package com.mello.bookingsystem.diningtable;

import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;



public interface DiningTableRepository extends JpaRepository<DiningTable, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from DiningTable t where t.id = :id")
    Optional<DiningTable> findByIdForUpdate(@Param("id") Long id);

    @Query("""
        select t from DiningTable t
        where t.active = true and t.seats >= :partySize
          and not exists (
            select 1 from Booking b
            where b.table = t
              and b.status <> com.mello.bookingsystem.booking.BookingStatus.CANCELLED
              and b.startTime < :end and b.endTime > :start)
        order by t.seats, t.label
        """)
    List<DiningTable> findAvailable(@Param("start") Instant start,
                                    @Param("end") Instant end,
                                    @Param("partySize") int partySize);
}