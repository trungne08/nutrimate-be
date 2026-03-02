package com.nutrimate.service;

import com.nutrimate.entity.Booking;
import com.nutrimate.entity.Booking.BookingStatus;
import com.nutrimate.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingCleanupService {

    private final BookingRepository bookingRepository;

    @Scheduled(fixedRate = 60000)
    public void cleanupExpiredPendingBookings() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(15);
        List<Booking> expired = bookingRepository.findByStatusAndCreatedAtBefore(BookingStatus.PENDING, cutoff);

        if (expired.isEmpty()) {
            return;
        }

        expired.forEach(b -> b.setStatus(BookingStatus.CANCELLED));
        bookingRepository.saveAll(expired);

        log.info("BookingCleanup: Đã hủy {} booking PENDING quá hạn 15 phút", expired.size());
    }
}

