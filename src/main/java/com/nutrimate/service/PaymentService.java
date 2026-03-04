package com.nutrimate.service;

import com.nutrimate.dto.CreatePaymentLinkRequestDTO;
import com.nutrimate.entity.Booking;
import com.nutrimate.entity.Booking.BookingStatus;
import com.nutrimate.entity.PaymentOrderMapping;
import com.nutrimate.entity.SubscriptionPlan;
import com.nutrimate.entity.User;
import com.nutrimate.entity.UserSubscription;
import com.nutrimate.entity.Payment;
import com.nutrimate.entity.Payment.PaymentMethod;
import com.nutrimate.entity.Payment.PaymentStatus;
import com.nutrimate.entity.Payment.PaymentType;
import com.nutrimate.entity.UserSubscription.SubscriptionStatus;
import com.nutrimate.exception.BadRequestException;
import com.nutrimate.exception.ResourceNotFoundException;
import com.nutrimate.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.exception.PayOSException;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.webhooks.WebhookData;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    private final PayOS payOS;
    private final BookingRepository bookingRepository;
    private final PaymentOrderMappingRepository paymentOrderMappingRepository;
    private final UserRepository userRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final PaymentRepository paymentRepository;

    /**
     * Tạo orderCode duy nhất (timestamp ms + nano suffix).
     */
    private long generateOrderCode() {
        return System.currentTimeMillis() * 1000 + (System.nanoTime() % 1000);
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }

    /** PayOS giới hạn description tối đa 25 ký tự. */
    private static final int MAX_DESCRIPTION_LENGTH = 25;

    @Transactional
    public String createPaymentLink(CreatePaymentLinkRequestDTO request) throws PayOSException {
        long orderCode = generateOrderCode();

        // Lưu mapping orderCode -> userId, planId để webhook xử lý subscription
        PaymentOrderMapping mapping = new PaymentOrderMapping();
        mapping.setOrderCode(orderCode);
        mapping.setUserId(request.getUserId());
        mapping.setPlanId(request.getPlanId());
        mapping.setAmount(request.getAmount().longValue());
        paymentOrderMappingRepository.save(mapping);

        String desc = "NUTRI " + truncate(request.getUserId(), 8) + " " + truncate(request.getPlanId(), 8);
        String description = desc.length() <= MAX_DESCRIPTION_LENGTH ? desc : desc.substring(0, MAX_DESCRIPTION_LENGTH);

        String returnUrl = frontendUrl.replaceAll("/$", "") + "/payment/success";
        String cancelUrl = frontendUrl.replaceAll("/$", "") + "/payment/cancel";

        CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                .orderCode(orderCode)
                .amount(request.getAmount().longValue())
                .description(description)
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl)
                .build();

        CreatePaymentLinkResponse response = payOS.paymentRequests().create(paymentData);
        return response.getCheckoutUrl();
    }

    /**
     * Tạo link thanh toán cho Booking cụ thể.
     */
    public String createBookingPaymentLink(String bookingId) throws PayOSException {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking không tồn tại"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Chỉ tạo link thanh toán cho Booking đang ở trạng thái PENDING");
        }
        if (booking.getFinalPrice() == null) {
            throw new BadRequestException("Booking chưa có finalPrice để thanh toán");
        }

        // Nếu booking chưa có orderCode thì generate và lưu lại
        if (booking.getOrderCode() == null) {
            booking.setOrderCode(generateOrderCode());
            bookingRepository.save(booking);
        }

        String description = ("BOOKING " + truncate(booking.getId(), 16));
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            description = description.substring(0, MAX_DESCRIPTION_LENGTH);
        }

        String returnUrl = frontendUrl.replaceAll("/$", "") + "/payment/success";
        String cancelUrl = frontendUrl.replaceAll("/$", "") + "/payment/cancel";

        CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                .orderCode(booking.getOrderCode())
                .amount(booking.getFinalPrice().longValue())
                .description(description)
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl)
                .build();

        CreatePaymentLinkResponse response = payOS.paymentRequests().create(paymentData);
        return response.getCheckoutUrl();
    }

    @Transactional
    public void processPayOSWebhook(Object webhookBody) {
        WebhookData data = payOS.webhooks().verify(webhookBody);
        if (!"00".equals(data.getCode())) {
            log.warn("Thanh toán PayOS thất bại hoặc không thành công, code={}", data.getCode());
            return;
        }

        Long orderCode = data.getOrderCode();
        log.info("PayOS webhook success, orderCode={}", orderCode);

        if (orderCode == null) return;

        // 1. Thử xử lý Booking trước
        var bookingOpt = bookingRepository.findByOrderCode(orderCode);
        if (bookingOpt.isPresent()) {
            Booking booking = bookingOpt.get();
            if (booking.getStatus() == BookingStatus.PENDING) {
                booking.setStatus(BookingStatus.CONFIRMED);
                bookingRepository.save(booking);
                log.info("Đã cập nhật Booking {} sang trạng thái CONFIRMED từ PayOS webhook", booking.getId());
            }
            return;
        }

        // 2. Nếu không phải Booking thì xử lý Subscription
        paymentOrderMappingRepository.findByOrderCode(orderCode).ifPresent(mapping -> {
            try {
                User user = userRepository.findById(mapping.getUserId())
                        .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại: " + mapping.getUserId()));
                SubscriptionPlan plan = subscriptionPlanRepository.findById(mapping.getPlanId())
                        .orElseThrow(() -> new ResourceNotFoundException("Plan không tồn tại: " + mapping.getPlanId()));

                LocalDateTime now = LocalDateTime.now();
                LocalDateTime endDate = now.plusDays(plan.getDurationDays() != null ? plan.getDurationDays() : 30);

                UserSubscription sub = new UserSubscription();
                sub.setUser(user);
                sub.setPlan(plan);
                sub.setStartDate(now);
                sub.setEndDate(endDate);
                sub.setStatus(SubscriptionStatus.Active);
                sub.setAutoRenew(false);
                userSubscriptionRepository.save(sub);

                Payment payment = new Payment();
                payment.setUser(user);
                payment.setRelatedId(sub.getId());
                payment.setPaymentType(PaymentType.SUBSCRIPTION);
                payment.setAmount(BigDecimal.valueOf(mapping.getAmount() != null ? mapping.getAmount() : 0));
                payment.setPaymentMethod(PaymentMethod.BANK);
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setPaymentDate(now);
                paymentRepository.save(payment);

                paymentOrderMappingRepository.delete(mapping);

                log.info("Đã tạo UserSubscription {} cho user {} plan {} từ PayOS webhook orderCode={}", sub.getId(), user.getId(), plan.getPlanName(), orderCode);
            } catch (Exception e) {
                log.error("Lỗi xử lý webhook Subscription orderCode={}", orderCode, e);
                throw e;
            }
        });
    }
}
