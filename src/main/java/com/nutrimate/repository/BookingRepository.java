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

        @Query("SELECT SUM(b.finalPrice) FROM Booking b " +
                        "WHERE b.status = 'COMPLETED' OR b.status = 'DONE' OR b.status = 'CONFIRMED'")
        BigDecimal calculateTotalRevenue();

        @Query("SELECT b FROM Booking b " +
                        "WHERE b.expert.id = :expertId " +
                        "AND DATE(b.bookingTime) = :date " +
                        "AND (b.status = 'PENDING' OR b.status = 'CONFIRMED')")
        List<Booking> findByExpertAndDateWithActiveStatus(@Param("expertId") String expertId,
                        @Param("date") LocalDate date);

        Optional<Booking> findByOrderCode(Long orderCode);

        List<Booking> findByStatusAndCreatedAtBefore(BookingStatus status, LocalDateTime createdAtBefore);

        @Query("SELECT b FROM Booking b WHERE b.bookingTime BETWEEN :start AND :end " +
                "AND (b.isReminded IS NULL OR b.isReminded = false) " +
                "AND b.status IN ('PENDING', 'CONFIRMED')")
        List<Booking> findUpcomingToRemind(@Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);

        @Query("SELECT COUNT(b) FROM Booking b " +
                        "WHERE b.member.id = :memberId " +
                        "AND b.isFreeSession = true " +
                        "AND b.status IN :statuses " +
                        "AND b.bookingTime BETWEEN :startTime AND :endTime")
        long countUsedFreeSessions(@Param("memberId") String memberId,
                        @Param("statuses") List<BookingStatus> statuses,
                        @Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime);

        // Dùng cho Biểu đồ doanh thu: Gom tiền Booking theo Tuần/Tháng/Năm (Chỉ tính
        // đơn đã trả tiền/xong)
        @Query(value = "SELECT DATE_FORMAT(booking_time, '%Y-%m') as label, SUM(final_price) FROM Bookings WHERE status IN ('CONFIRMED', 'COMPLETED') GROUP BY label ORDER BY label", nativeQuery = true)
        List<Object[]> getRevenueByMonth();

        @Query(value = "SELECT YEAR(booking_time) as label, SUM(final_price) FROM Bookings WHERE status IN ('CONFIRMED', 'COMPLETED') GROUP BY label ORDER BY label", nativeQuery = true)
        List<Object[]> getRevenueByYear();

        @Query(value = "SELECT DATE_FORMAT(booking_time, '%x-W%v') as label, SUM(final_price) FROM Bookings WHERE status IN ('CONFIRMED', 'COMPLETED') GROUP BY label ORDER BY label", nativeQuery = true)
        List<Object[]> getRevenueByWeek();
}