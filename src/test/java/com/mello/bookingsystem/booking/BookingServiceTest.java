package com.mello.bookingsystem.booking;

import com.mello.bookingsystem.common.BadRequestException;
import com.mello.bookingsystem.common.ConflictException;
import com.mello.bookingsystem.common.NotFoundException;
import com.mello.bookingsystem.diningtable.DiningTable;
import com.mello.bookingsystem.diningtable.DiningTableRepository;
import com.mello.bookingsystem.user.User;
import com.mello.bookingsystem.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-05T10:00:00Z");
    private static final Instant START = Instant.parse("2026-12-20T18:00:00Z");
    private static final Instant END = Instant.parse("2026-12-20T20:00:00Z");

    @Mock BookingRepository bookingRepository;
    @Mock DiningTableRepository tableRepository;
    @Mock UserRepository userRepository;

    BookingService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new BookingService(bookingRepository, tableRepository, userRepository, clock);
    }

    private void givenUserAndTable(int seats) {
        DiningTable table = new DiningTable();
        table.setSeats(seats);
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));
        when(tableRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(table));
    }

    @Test
    void rejectsEndBeforeStart() {
        var request = new BookingRequest(1L, END, START, 2);
        assertThrows(BadRequestException.class, () -> service.create(1L, request));
    }

    @Test
    void rejectsEqualStartAndEnd() {
        var request = new BookingRequest( 1L, START, START, 2);
        assertThrows(BadRequestException.class, () -> service.create(1L, request));
    }

    @Test
    void rejectsStartInThePast() {
        var request = new BookingRequest( 1L, NOW.minusSeconds(3600), END, 2);
        assertThrows(BadRequestException.class, () -> service.create(1L, request));
    }

    @Test
    void rejectsPartyLargerThanTable() {
        givenUserAndTable(4);
        var request = new BookingRequest( 1L, START, END, 5);
        assertThrows(BadRequestException.class, () -> service.create(1L, request));
        verify(bookingRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsOverlappingBooking() {
        givenUserAndTable(4);
        when(bookingRepository.existsOverlap(1L, START, END)).thenReturn(true);
        var request = new BookingRequest( 1L, START, END, 2);
        assertThrows(ConflictException.class, () -> service.create(1L, request));
        verify(bookingRepository, never()).saveAndFlush(any());
    }

    @Test
    void savesConfirmedBookingWhenAllRulesPass() {
        givenUserAndTable(4);
        when(bookingRepository.existsOverlap(1L, START, END)).thenReturn(false);
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse response = service.create(1L,new BookingRequest( 1L, START, END, 4));

        assertEquals(BookingStatus.CONFIRMED, response.status());
        assertEquals(4, response.partySize());
    }

    private Booking bookingOf(long ownerId, Instant start) {
        User owner = new User();
        owner.setId(ownerId);
        Booking b = new Booking();
        b.setUser(owner);
        b.setStartTime(start);
        b.setStatus(BookingStatus.CONFIRMED);
        return b;
    }

    @Test
    void ownerCancelsBookingFarInTheFuture() {
        Booking b = bookingOf(1L, NOW.plus(Duration.ofDays(2)));
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(b));
        service.cancel(5L, 1L, false);
        assertEquals(BookingStatus.CANCELLED, b.getStatus());
    }

    @Test
    void userCannotCancelWithinTwoHoursOfStart() {
        Booking b = bookingOf(1L, NOW.plus(Duration.ofHours(1)));
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(b));
        assertThrows(BadRequestException.class, () -> service.cancel(5L, 1L, false));
        assertEquals(BookingStatus.CONFIRMED, b.getStatus());
    }

    @Test
    void adminCanCancelWithinTwoHoursOfStart() {
        Booking b = bookingOf(1L, NOW.plus(Duration.ofHours(1)));
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(b));
        service.cancel(5L, 99L, true);
        assertEquals(BookingStatus.CANCELLED, b.getStatus());
    }

    @Test
    void otherUsersBookingLooksNonExistent() {
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(bookingOf(1L, NOW.plus(Duration.ofDays(2)))));
        assertThrows(NotFoundException.class, () -> service.cancel(5L, 2L, false));
    }
}