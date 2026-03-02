package com.nutrimate.service;

import com.nutrimate.dto.BookingRequestDTO;
import com.nutrimate.dto.BookingStatusDTO;
import com.nutrimate.dto.PriceCheckResponseDTO;
import com.nutrimate.entity.*;
import com.nutrimate.entity.Booking.BookingStatus;
import com.nutrimate.exception.BadRequestException;
import com.nutrimate.exception.ForbiddenException;
import com.nutrimate.exception.ResourceNotFoundException;
import com.nutrimate.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ExpertProfileRepository expertProfileRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final UserBenefitUsageRepository benefitUsageRepository;
    private final UserRepository userRepository;

    private static final List<String> FIXED_TIME_SLOTS = List.of(
            // Sáng
            "08:00", "09:00", "10:00", "11:00",
            // Chiều
            "13:30", "14:30", "15:30", "16:30",
            // Tối
            "19:00", "20:00", "21:00"
    );

    // Helper nội bộ: thông tin free sessions còn lại cho user
    private static class FreeSessionInfo {
        private final boolean hasSubscription;
        private final int total;
        private final long used;
        private final long remaining;
        private final UserSubscription subscription; // để cập nhật usage khi create booking

        private FreeSessionInfo(boolean hasSubscription, int total, long used, long remaining, UserSubscription subscription) {
            this.hasSubscription = hasSubscription;
            this.total = total;
            this.used = used;
            this.remaining = remaining;
            this.subscription = subscription;
        }
    }

    private FreeSessionInfo calculateFreeSessions(String userId) {
        Optional<UserSubscription> subOpt = subscriptionRepository.findActiveSubscriptionByUserId(userId);

        if (subOpt.isEmpty()) {
            return new FreeSessionInfo(false, 0, 0, 0, null);
        }

        UserSubscription sub = subOpt.get();
        if (!Boolean.TRUE.equals(sub.getPlan().getIsExpertPlan())) {
            return new FreeSessionInfo(false, 0, 0, 0, null);
        }

        int total = Optional.ofNullable(sub.getPlan().getFreeSessionsPerCycle()).orElse(0);
        if (total <= 0) {
            return new FreeSessionInfo(true, 0, 0, 0, sub);
        }

        List<BookingStatus> statuses = List.of(
                BookingStatus.PENDING,
                BookingStatus.CONFIRMED,
                BookingStatus.COMPLETED,
                BookingStatus.DONE
        );
        long used = bookingRepository.countUsedFreeSessions(
                userId,
                statuses,
                sub.getStartDate(),
                sub.getEndDate()
        );
        long remaining = Math.max(0, total - used);
        return new FreeSessionInfo(true, total, used, remaining, sub);
    }

    // 5.3 CHECK GIÁ (dùng helper free sessions)
    public PriceCheckResponseDTO checkBookingPrice(String userId, String expertId) {
        ExpertProfile expert = expertProfileRepository.findById(expertId)
                .orElseThrow(() -> new ResourceNotFoundException("Expert not found"));
        FreeSessionInfo info = calculateFreeSessions(userId);

        boolean isFree = info.hasSubscription && info.remaining > 0 && info.total > 0;
        String msg = isFree
                ? "FREE SESSION APPLIED (Used " + info.used + "/" + info.total + ")."
                : (info.hasSubscription ? "You have used all free sessions for this cycle." : "Standard price applied.");

        return PriceCheckResponseDTO.builder()
                .isFreeSession(isFree)
                .originalPrice(expert.getHourlyRate())
                .finalPrice(isFree ? BigDecimal.ZERO : expert.getHourlyRate())
                .message(msg)
                .build();
    }

    @Transactional
    public Booking createBooking(String userId, BookingRequestDTO req) {
        User member = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        ExpertProfile expertProfile = expertProfileRepository.findById(req.getExpertId())
                .orElseThrow(() -> new ResourceNotFoundException("Expert not found"));
        BigDecimal basePrice = expertProfile.getHourlyRate();
        FreeSessionInfo info = calculateFreeSessions(userId);
        boolean wantFree = Boolean.TRUE.equals(req.getUseFreeSession());

        Booking booking = new Booking();
        booking.setMember(member);
        booking.setExpert(expertProfile);
        booking.setBookingTime(req.getBookingTime());
        booking.setNote(req.getNote());

        if (wantFree && info.hasSubscription && info.remaining > 0 && info.total > 0) {
            booking.setIsFreeSession(true);
            booking.setOriginalPrice(basePrice);
            booking.setFinalPrice(BigDecimal.ZERO);
            booking.setStatus(BookingStatus.CONFIRMED); // Free session: auto-confirm, không cần PayOS

            // Đồng bộ bảng usage cũ nếu đang dùng
            if (info.subscription != null) {
                UserBenefitUsage usage = getOrCreateUsage(userId, info.subscription);
                usage.setSessionsUsed(Optional.ofNullable(usage.getSessionsUsed()).orElse(0) + 1);
                benefitUsageRepository.save(usage);
            }
        } else {
            booking.setIsFreeSession(false);
            booking.setOriginalPrice(basePrice);
            booking.setFinalPrice(basePrice);
            booking.setStatus(BookingStatus.PENDING); // Cần thanh toán PayOS
        }

        booking.setMeetingLink(null);

        return bookingRepository.save(booking);
    }

    // Lịch sử booking của Expert (chỉ những booking được assign cho expert này)
    public List<Booking> getMyExpertBookings(String expertUserId) {
        return bookingRepository.findByExpertIdOrderByBookingTimeDesc(expertUserId);
    }

    // 5.5 LỊCH SỬ BOOKING (Member - những booking mình đặt)
    public List<Booking> getMyBookings(String userId) {
        // Tìm xem user là Member hay Expert
        // Ở đây mình thử tìm cả 2, cái nào có dữ liệu thì trả về
        List<Booking> asMember = bookingRepository.findByMemberIdOrderByBookingTimeDesc(userId);
        if (!asMember.isEmpty()) return asMember;
        
        return bookingRepository.findByExpertIdOrderByBookingTimeDesc(userId);
    }

    // 5.6 UPDATE TRẠNG THÁI (Cho Expert)
    @Transactional
    // Sửa tham số nhận vào: Dùng DTO để nhận cả Status và Note
    public Booking updateStatus(String userId, String bookingId, BookingStatusDTO req) {
        
        // 1. Tìm Booking
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        // 2. TỪ USER ID -> TÌM RA EXPERT ID (Fix lỗi so sánh ID sai)
        ExpertProfile expert = expertProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Bạn không phải là Expert!"));

        // 3. Check quyền (Chỉ Expert chủ sở hữu mới được sửa)
        if (!booking.getExpert().getId().equals(expert.getId())) {
             // Dùng Custom Exception của bạn
            throw new ForbiddenException("Unauthorized: You are not the expert assigned to this booking");
        }

        Booking.BookingStatus currentStatus = booking.getStatus();
        Booking.BookingStatus newStatus = req.getStatus(); // Lấy từ DTO

        // 4. LOGIC STATE MACHINE (Chặn đổi trạng thái lung tung)
        switch (newStatus) {
            case CONFIRMED:
                if (currentStatus != Booking.BookingStatus.PENDING) {
                    throw new BadRequestException("Chỉ có thể chấp nhận lịch đang chờ (Pending).");
                }
                // 👇 GIỮ LẠI LOGIC TẠO LINK CỦA BẠN (Rất hay)
                booking.setMeetingLink("https://meet.google.com/gen-link-" + booking.getId());
                break;

            case REJECTED:
                if (currentStatus != Booking.BookingStatus.PENDING) {
                    throw new BadRequestException("Lịch đã xử lý rồi, không thể từ chối nữa.");
                }
                // Check lý do
                if (req.getNote() == null || req.getNote().trim().isEmpty()) {
                    throw new BadRequestException("Vui lòng nhập lý do từ chối!");
                }
                booking.setNote(req.getNote());
                break;

            case COMPLETED:
            case DONE:
                if (currentStatus != Booking.BookingStatus.CONFIRMED) {
                    throw new BadRequestException("Chỉ có thể hoàn thành lịch đã được xác nhận.");
                }
                break;

            case CANCELLED:
                // Expert hủy kèo
                if (currentStatus == Booking.BookingStatus.COMPLETED
                        || currentStatus == Booking.BookingStatus.DONE
                        || currentStatus == Booking.BookingStatus.REJECTED) {
                    throw new BadRequestException("Lịch đã kết thúc, không thể hủy.");
                }
                if (req.getNote() == null || req.getNote().trim().isEmpty()) {
                    throw new BadRequestException("Vui lòng nhập lý do hủy!");
                }
                booking.setNote(req.getNote());
                break;

            default:
                throw new BadRequestException("Trạng thái không hợp lệ.");
        }

        // 5. Cập nhật trạng thái và lưu
        booking.setStatus(newStatus);
        return bookingRepository.save(booking);
    }

    // 5.7 ADMIN XEM ALL
    public List<Booking> getAllBookings(LocalDate date) {
        return bookingRepository.findAllByDate(date);
    }

    /**
     * Lấy danh sách khung giờ còn trống của một Expert trong ngày.
     */
    public List<String> getExpertAvailableSlots(String expertId, LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        List<Booking> bookings = bookingRepository.findByExpertAndDateWithActiveStatus(expertId, date);

        Set<String> busySlots = new LinkedHashSet<>();
        for (Booking b : bookings) {
            LocalTime time = b.getBookingTime().toLocalTime();
            busySlots.add(time.format(formatter));
        }

        return FIXED_TIME_SLOTS.stream()
                .filter(slot -> !busySlots.contains(slot))
                .toList();
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

    /**
     * API helper cho FE: Thống kê lượt free sessions của user trong chu kỳ gói hiện tại.
     */
    public Map<String, Object> getMyFreeSessionsSummary(String userId) {
        FreeSessionInfo info = calculateFreeSessions(userId);

        Map<String, Object> result = new java.util.HashMap<>();

        if (!info.hasSubscription) {
            result.put("hasSubscription", false);
            result.put("totalFreeSessions", 0);
            result.put("used", 0);
            result.put("remainingFreeSessions", 0);
            return result;
        }

        result.put("hasSubscription", true);
        result.put("totalFreeSessions", info.total);
        result.put("used", info.used);
        result.put("remainingFreeSessions", info.remaining);
        return result;
    }
}