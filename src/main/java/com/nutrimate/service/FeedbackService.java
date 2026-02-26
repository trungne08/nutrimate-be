package com.nutrimate.service;

import com.nutrimate.dto.FeedbackCreateRequest;
import com.nutrimate.entity.Booking;
import com.nutrimate.entity.Feedback;
import com.nutrimate.entity.User;
import com.nutrimate.exception.BadRequestException;
import com.nutrimate.exception.ForbiddenException;
import com.nutrimate.exception.ResourceNotFoundException;
import com.nutrimate.repository.BookingRepository;
import com.nutrimate.repository.FeedbackRepository;
import com.nutrimate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import java.util.HashMap;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    @Transactional
    public Feedback createFeedback(String memberId, FeedbackCreateRequest req) {
        // 1. Check User
        User member = userRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại!"));

        // 2. Check Booking
        Booking booking = bookingRepository.findById(req.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking không tồn tại!"));

        // 3. Chính chủ mới được đánh giá
        if (!booking.getMember().getId().equals(memberId)) {
            throw new ForbiddenException("Bạn không thể đánh giá lịch hẹn của người khác!");
        }

        // 4. Chỉ cho phép đánh giá khi đã hoàn thành (COMPLETED)
        if (booking.getStatus() != Booking.BookingStatus.COMPLETED) {
            throw new BadRequestException("Chỉ có thể đánh giá sau khi lịch hẹn đã hoàn thành!");
        }

        // 5. Mỗi lịch chỉ được đánh giá 1 lần (Check trùng lặp)
        if (feedbackRepository.existsByBookingId(booking.getId())) {
            throw new BadRequestException("Bạn đã đánh giá lịch hẹn này rồi!");
        }

        // 6. Kiểm tra số sao (1-5)
        if (req.getRating() == null || req.getRating() < 1 || req.getRating() > 5) {
            throw new BadRequestException("Điểm đánh giá phải từ 1 đến 5 sao!");
        }

        // 7. Tạo và lưu Feedback
        Feedback feedback = new Feedback();
        feedback.setBooking(booking);
        feedback.setMember(member);
        feedback.setExpert(booking.getExpert());
        feedback.setRating(req.getRating());
        feedback.setComment(req.getComment());

        return feedbackRepository.save(feedback);
    }

    public Map<String, Object> getExpertFeedbacks(String expertId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        
        // Lấy danh sách feedback
        Page<Feedback> feedbackPage = feedbackRepository.findByExpertId(expertId, pageable);
        
        // Lấy điểm trung bình
        Double avgRating = feedbackRepository.getAverageRatingByExpertId(expertId);
        
        // Đóng gói trả về cho Frontend
        Map<String, Object> response = new HashMap<>();
        response.put("feedbacks", feedbackPage.getContent());
        response.put("currentPage", feedbackPage.getNumber());
        response.put("totalItems", feedbackPage.getTotalElements());
        response.put("totalPages", feedbackPage.getTotalPages());
        
        // Làm tròn 1 chữ số thập phân (VD: 4.56 -> 4.6)
        response.put("averageRating", avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0);
        response.put("totalReviews", feedbackPage.getTotalElements());

        return response;
    }
}