package com.nutrimate.repository;

import com.nutrimate.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, String> {
    // Lịch sử đặt lịch của Member
    List<Booking> findByMemberIdOrderByBookingTimeDesc(String memberId);
    
    // Lịch làm việc của Expert
    List<Booking> findByExpertIdOrderByBookingTimeDesc(String expertId);

    @Query("SELECT b FROM Booking b WHERE :date IS NULL OR DATE(b.bookingTime) = :date")
    List<Booking> findAllByDate(LocalDate date);

    List<Booking> findByExpertId(String expertId);
}