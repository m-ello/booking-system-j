package com.mello.bookingsystem.booking;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

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

    @Query("select b from Booking b join fetch b.table where b.user.id = :userId order by b.startTime desc")
    List<Booking> findByUser(@Param("userId") Long userId);

    @Override
    @EntityGraph(attributePaths = {"table", "user"})
    Page<Booking> findAll(Specification<Booking> spec, Pageable pageable);
}