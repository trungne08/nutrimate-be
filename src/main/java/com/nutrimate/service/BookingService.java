package com.nutrimate.service;

import com.nutrimate.dto.BookingRequestDTO;
import com.nutrimate.dto.PriceCheckResponseDTO;
import com.nutrimate.entity.*;
import com.nutrimate.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ExpertProfileRepository expertProfileRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final UserBenefitUsageRepository benefitUsageRepository;
    private final UserRepository userRepository;

    // 5.3 CHECK GIÁ (Quan trọng: Logic trừ lượt Free)
    public PriceCheckResponseDTO checkBookingPrice(String userId, String expertId) {
        ExpertProfile expert = expertProfileRepository.findById(expertId)
                .orElseThrow(() -> new RuntimeException("Expert not found"));

        // Lấy thông tin gói cước
        Optional<UserSubscription> subOpt = subscriptionRepository.findActiveSubscriptionByUserId(userId);
        
        boolean isFree = false;
        String msg = "Bạn sẽ trả phí gốc.";

        if (subOpt.isPresent()) {
            UserSubscription sub = subOpt.get();
            // Nếu gói có hỗ trợ Expert (isExpertPlan = true)
            if (Boolean.TRUE.equals(sub.getPlan().getIsExpertPlan())) {
                UserBenefitUsage usage = getOrCreateUsage(userId, sub);
                int limit = sub.getPlan().getFreeSessionsPerCycle();
                int used = usage.getSessionsUsed();

                if (used < limit) {
                    isFree = true;
                    msg = "Bạn được MIỄN PHÍ (Đã dùng " + used + "/" + limit + " lượt).";
                } else {
                    msg = "Bạn đã hết lượt miễn phí trong chu kỳ này.";
                }
            }
        }

        return PriceCheckResponseDTO.builder()
                .isFreeSession(isFree)
                .originalPrice(expert.getHourlyRate())
                .finalPrice(isFree ? BigDecimal.ZERO : expert.getHourlyRate())
                .message(msg)
                .build();
    }

    // 5.4 TẠO BOOKING
    @Transactional
    public Booking createBooking(String userId, BookingRequestDTO req) {
        User member = userRepository.findById(userId).orElseThrow();
        ExpertProfile expertProfile = expertProfileRepository.findById(req.getExpertId()).orElseThrow();
        User expertUser = expertProfile.getUser(); // Lấy User entity của Expert

        // 1. Tính toán giá lại (Double check để tránh hack API)
        PriceCheckResponseDTO priceCheck = checkBookingPrice(userId, req.getExpertId());

        // 2. Nếu Free -> Trừ lượt trong BenefitUsage
        if (priceCheck.isFreeSession()) {
            UserSubscription sub = subscriptionRepository.findActiveSubscriptionByUserId(userId).get();
            UserBenefitUsage usage = getOrCreateUsage(userId, sub);
            usage.setSessionsUsed(usage.getSessionsUsed() + 1);
            benefitUsageRepository.save(usage);
        }

        // 3. Lưu Booking
        Booking booking = new Booking();
        booking.setMember(member);
        booking.setExpert(expertUser);
        booking.setBookingTime(req.getBookingTime());
        booking.setOriginalPrice(priceCheck.getOriginalPrice());
        booking.setFinalPrice(priceCheck.getFinalPrice());
        booking.setIsFreeSession(priceCheck.isFreeSession());
        booking.setStatus(Booking.BookingStatus.Pending); // Mặc định là Pending
        booking.setMeetingLink(null); // Chưa có link

        return bookingRepository.save(booking);
    }

    // 5.5 LỊCH SỬ BOOKING
    public List<Booking> getMyBookings(String userId) {
        // Tìm xem user là Member hay Expert
        // Ở đây mình thử tìm cả 2, cái nào có dữ liệu thì trả về
        List<Booking> asMember = bookingRepository.findByMemberIdOrderByBookingTimeDesc(userId);
        if (!asMember.isEmpty()) return asMember;
        
        return bookingRepository.findByExpertIdOrderByBookingTimeDesc(userId);
    }

    // 5.6 UPDATE TRẠNG THÁI (Cho Expert)
    @Transactional
    public Booking updateStatus(String bookingId, String expertUserId, String statusStr) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // Check quyền: Chỉ Expert của booking này mới được sửa
        if (!booking.getExpert().getId().equals(expertUserId)) {
            throw new RuntimeException("Unauthorized: You are not the expert of this booking");
        }

        Booking.BookingStatus status = Booking.BookingStatus.valueOf(statusStr.toUpperCase());
        booking.setStatus(status);

        // Nếu Confirm -> Tạo link Google Meet (Giả lập)
        if (status == Booking.BookingStatus.Confirmed) {
            booking.setMeetingLink("https://meet.google.com/gen-link-" + booking.getId());
        }

        return bookingRepository.save(booking);
    }

    // 5.7 ADMIN XEM ALL
    public List<Booking> getAllBookings(LocalDate date) {
        return bookingRepository.findAllByDate(date);
    }

    // Helper: Lấy hoặc tạo Usage record
    private UserBenefitUsage getOrCreateUsage(String userId, UserSubscription sub) {
        return benefitUsageRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserBenefitUsage u = new UserBenefitUsage();
                    u.setUserId(userId);
                    u.setSubscription(sub);
                    u.setSessionsUsed(0);
                    return benefitUsageRepository.save(u);
                });
    }
}