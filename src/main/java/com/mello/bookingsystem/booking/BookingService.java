package com.mello.bookingsystem.booking;

import com.mello.bookingsystem.common.BadRequestException;
import com.mello.bookingsystem.common.ConflictException;
import com.mello.bookingsystem.common.NotFoundException;
import com.mello.bookingsystem.common.PageResponse;
import com.mello.bookingsystem.diningtable.DiningTable;
import com.mello.bookingsystem.diningtable.DiningTableRepository;
import com.mello.bookingsystem.user.User;
import com.mello.bookingsystem.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.*;
import java.util.*;


import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final DiningTableRepository tableRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional
    public BookingResponse create(Long userId, BookingRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BadRequestException("End time must be after start time");
        }
        if (!request.startTime().isAfter(Instant.now(clock))) {
            throw new BadRequestException("Start time must be in the future");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User " + userId + " not found"));
        DiningTable table = tableRepository.findByIdForUpdate(request.tableId())
                .filter(DiningTable::isActive)
                .orElseThrow(() -> new NotFoundException("Table " + request.tableId() + " not found"));

        if (request.partySize() > table.getSeats()) {
            throw new BadRequestException(
                    "Party size " + request.partySize() + " exceeds table capacity of " + table.getSeats());
        }
        if (bookingRepository.existsOverlap(request.tableId(), request.startTime(), request.endTime())) {
            throw new ConflictException("Table is already booked for the requested time");
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setTable(table);
        booking.setStartTime(request.startTime());
        booking.setEndTime(request.endTime());
        booking.setPartySize(request.partySize());
        booking.setStatus(BookingStatus.CONFIRMED);

        try {
            return BookingResponse.from(bookingRepository.saveAndFlush(booking));
        } catch (DataIntegrityViolationException ex) {
            String cause = NestedExceptionUtils.getMostSpecificCause(ex).getMessage();
            if (cause != null && cause.contains("no_overlapping_bookings")) {
                throw new ConflictException("Table is already booked for the requested time");
            }
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public BookingResponse findById(Long id, Long requesterId, boolean admin) {
        return bookingRepository.findById(id)
                .filter(b -> admin || b.getUser().getId().equals(requesterId))
                .map(BookingResponse::from)
                .orElseThrow(() -> new NotFoundException("Booking " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> findMine(Long userId) {
        return bookingRepository.findByUser(userId).stream().map(BookingResponse::from).toList();
    }

    @Transactional
    public void cancel(Long id, Long requesterId, boolean admin) {
        Booking b = bookingRepository.findById(id)
                .filter(x -> admin || x.getUser().getId().equals(requesterId))
                .orElseThrow(() -> new NotFoundException("Booking " + id + " not found"));
        if (b.getStatus() == BookingStatus.CANCELLED) return;
        if (!admin && b.getStartTime().isBefore(Instant.now(clock).plus(Duration.ofHours(2)))) {
            throw new BadRequestException("Bookings can only be cancelled at least 2 hours before the start");
        }
        b.setStatus(BookingStatus.CANCELLED);
    }

    private static final Set<String> SORTABLE = Set.of("startTime", "createdAt");

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> search(BookingFilter f, int page, int size, String sort, String dir) {
        String property = SORTABLE.contains(sort) ? sort : "startTime";   // whitelist: never sort by client-chosen fields
        Sort order = "asc".equalsIgnoreCase(dir) ? Sort.by(property).ascending() : Sort.by(property).descending();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), order.and(Sort.by("id")));

        Specification<Booking> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (f.status() != null) p.add(cb.equal(root.get("status"), f.status()));
            if (f.tableId() != null) p.add(cb.equal(root.get("table").get("id"), f.tableId()));
            if (f.from() != null) p.add(cb.greaterThanOrEqualTo(root.<Instant>get("startTime"), f.from()));
            if (f.to() != null) p.add(cb.lessThan(root.<Instant>get("startTime"), f.to()));
            if (f.email() != null && !f.email().isBlank()) {
                String like = "%" + f.email().trim().toLowerCase()
                        .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                p.add(cb.like(cb.lower(root.get("user").get("email")), like, '\\'));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        return PageResponse.of(bookingRepository.findAll(spec, pageable).map(BookingResponse::from));
    }
}