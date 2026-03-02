package com.nutrimate.service;

import com.nutrimate.dto.CreatePaymentLinkRequestDTO;
import com.nutrimate.entity.Booking;
import com.nutrimate.entity.Booking.BookingStatus;
import com.nutrimate.exception.BadRequestException;
import com.nutrimate.exception.ResourceNotFoundException;
import com.nutrimate.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.exception.PayOSException;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.webhooks.WebhookData;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    private final PayOS payOS;
    private final BookingRepository bookingRepository;

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

    public String createPaymentLink(CreatePaymentLinkRequestDTO request) throws PayOSException {
        long orderCode = generateOrderCode();
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

    public void processPayOSWebhook(Object webhookBody) {
        WebhookData data = payOS.webhooks().verify(webhookBody);
        if (!"00".equals(data.getCode())) {
            log.warn("Thanh toán PayOS thất bại hoặc không thành công, code={}", data.getCode());
            return;
        }

        Long orderCode = data.getOrderCode();
        log.info("PayOS webhook success, orderCode={}", orderCode);

        if (orderCode != null) {
            bookingRepository.findByOrderCode(orderCode).ifPresent(booking -> {
                if (booking.getStatus() == BookingStatus.PENDING) {
                    booking.setStatus(BookingStatus.CONFIRMED);
                    bookingRepository.save(booking);
                    log.info("Đã cập nhật Booking {} sang trạng thái CONFIRMED từ PayOS webhook", booking.getId());
                }
            });
        }
    }
}
