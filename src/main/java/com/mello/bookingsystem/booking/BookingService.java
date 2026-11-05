package com.mello.bookingsystem.booking;

import com.mello.bookingsystem.common.BadRequestException;
import com.mello.bookingsystem.common.ConflictException;
import com.mello.bookingsystem.common.NotFoundException;
import com.mello.bookingsystem.diningtable.DiningTable;
import com.mello.bookingsystem.diningtable.DiningTableRepository;
import com.mello.bookingsystem.user.User;
import com.mello.bookingsystem.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final DiningTableRepository tableRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional
    public BookingResponse create(BookingRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BadRequestException("End time must be after start time");
        }
        if (!request.startTime().isAfter(Instant.now(clock))) {
            throw new BadRequestException("Start time must be in the future");
        }

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new NotFoundException("User " + request.userId() + " not found"));
        DiningTable table = tableRepository.findById(request.tableId())
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

        return BookingResponse.from(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public BookingResponse findById(Long id) {
        return bookingRepository.findById(id)
                .map(BookingResponse::from)
                .orElseThrow(() -> new NotFoundException("Booking " + id + " not found"));
    }
}