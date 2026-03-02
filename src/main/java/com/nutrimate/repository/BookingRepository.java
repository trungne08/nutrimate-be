package com.nutrimate.repository;

import com.nutrimate.entity.Booking;
import com.nutrimate.entity.Booking.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, String> {
    // Lịch sử đặt lịch của Member
    List<Booking> findByMemberIdOrderByBookingTimeDesc(String memberId);
    
    // Lịch làm việc của Expert
    List<Booking> findByExpertIdOrderByBookingTimeDesc(String expertId);

    @Query("SELECT b FROM Booking b WHERE :date IS NULL OR DATE(b.bookingTime) = :date")
    List<Booking> findAllByDate(LocalDate date);

    List<Booking> findByExpertId(String expertId);

    @Query("SELECT SUM(b.finalPrice) FROM Booking b WHERE b.status = 'COMPLETED' OR b.status = 'CONFIRMED'")
    BigDecimal calculateTotalRevenue();

    @Query("SELECT b FROM Booking b " +
            "WHERE b.expert.id = :expertId " +
            "AND DATE(b.bookingTime) = :date " +
            "AND (b.status = 'PENDING' OR b.status = 'CONFIRMED')")
    List<Booking> findByExpertAndDateWithActiveStatus(@Param("expertId") String expertId,
                                                      @Param("date") LocalDate date);

    Optional<Booking> findByOrderCode(Long orderCode);

    List<Booking> findByStatusAndCreatedAtBefore(BookingStatus status, LocalDateTime createdAtBefore);
}