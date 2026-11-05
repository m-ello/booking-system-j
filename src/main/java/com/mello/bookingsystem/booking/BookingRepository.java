package com.mello.bookingsystem.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("""
            select count(b) > 0 from Booking b
            where b.table.id = :tableId
              and b.status <> com.mello.bookingsystem.booking.BookingStatus.CANCELLED
              and b.startTime < :end
              and b.endTime > :start
            """)
    boolean existsOverlap(@Param("tableId") Long tableId,
                          @Param("start") Instant start,
                          @Param("end") Instant end);
}