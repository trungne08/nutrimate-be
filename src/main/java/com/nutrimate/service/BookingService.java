package com.nutrimate.service;

import com.nutrimate.dto.BookingRequestDTO;
import com.nutrimate.dto.BookingStatusDTO;
import com.nutrimate.dto.PriceCheckResponseDTO;
import com.nutrimate.entity.*;
import com.nutrimate.entity.Booking.BookingStatus;
import com.nutrimate.exception.ForbiddenException;
import com.nutrimate.exception.ResourceNotFoundException;
import com.nutrimate.exception.BadRequestException;
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
                .orElseThrow(() -> new ResourceNotFoundException("Expert not found"));
        
        Optional<UserSubscription> subOpt = subscriptionRepository.findActiveSubscriptionByUserId(userId);
        
        boolean isFree = false;
        String msg = "Standard price applied.";

        if (subOpt.isPresent()) {
            UserSubscription sub = subOpt.get();
            if (Boolean.TRUE.equals(sub.getPlan().getIsExpertPlan())) {
                UserBenefitUsage usage = getOrCreateUsage(userId, sub);
                int limit = sub.getPlan().getFreeSessionsPerCycle();
                int used = usage.getSessionsUsed();

                if (used < limit) {
                    isFree = true;
                    msg = "FREE SESSION APPLIED (Used " + used + "/" + limit + ").";
                } else {
                    msg = "You have used all free sessions for this cycle.";
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

    @Transactional
    public Booking createBooking(String userId, BookingRequestDTO req) {
        User member = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        ExpertProfile expertProfile = expertProfileRepository.findById(req.getExpertId())
                .orElseThrow(() -> new ResourceNotFoundException("Expert not found"));
        User expertUser = expertProfile.getUser();

        PriceCheckResponseDTO priceCheck = checkBookingPrice(userId, req.getExpertId());

        if (priceCheck.isFreeSession()) {
            UserSubscription sub = subscriptionRepository.findActiveSubscriptionByUserId(userId).get();
            UserBenefitUsage usage = getOrCreateUsage(userId, sub);
            usage.setSessionsUsed(usage.getSessionsUsed() + 1);
            benefitUsageRepository.save(usage);
        }

        Booking booking = new Booking();
        booking.setMember(member);
        booking.setExpert(expertUser);
        booking.setBookingTime(req.getBookingTime());
        booking.setOriginalPrice(priceCheck.getOriginalPrice());
        booking.setFinalPrice(priceCheck.getFinalPrice());
        booking.setIsFreeSession(priceCheck.isFreeSession());
        booking.setStatus(Booking.BookingStatus.PENDING);
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
                if (currentStatus != Booking.BookingStatus.CONFIRMED) {
                    throw new BadRequestException("Chỉ có thể hoàn thành lịch đã được xác nhận.");
                }
                break;

            case CANCELLED:
                // Expert hủy kèo
                if (currentStatus == Booking.BookingStatus.COMPLETED || currentStatus == Booking.BookingStatus.REJECTED) {
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