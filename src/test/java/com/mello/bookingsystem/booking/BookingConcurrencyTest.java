package com.mello.bookingsystem.booking;

import com.mello.bookingsystem.common.ConflictException;
import com.mello.bookingsystem.diningtable.DiningTable;
import com.mello.bookingsystem.diningtable.DiningTableRepository;
import com.mello.bookingsystem.user.User;
import com.mello.bookingsystem.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Testcontainers
class BookingConcurrencyTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

    @Autowired BookingService bookingService;
    @Autowired BookingRepository bookingRepository;
    @Autowired UserRepository userRepository;
    @Autowired DiningTableRepository tableRepository;

    @Test
    void onlyOneOfManyConcurrentBookingsSucceeds() throws Exception {
        User user = new User();
        user.setEmail("concurrent@test.com");
        user.setPasswordHash("x");
        user = userRepository.save(user);

        DiningTable table = new DiningTable();
        table.setLabel("C1");
        table.setSeats(4);
        table = tableRepository.save(table);

        Long userId = user.getId();

        Instant start = Instant.now().plus(Duration.ofDays(30));
        Instant end = start.plus(Duration.ofHours(2));
        BookingRequest request = new BookingRequest(table.getId(), start, end, 2);

        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();
        AtomicInteger others = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();               // all threads start at the same moment
                try {
                    bookingService.create(userId, request);
                    successes.incrementAndGet();
                } catch (ConflictException e) {
                    conflicts.incrementAndGet();
                } catch (Exception e) {
                    others.incrementAndGet();
                }
                return null;
            }));
        }

        ready.await();
        go.countDown();
        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdown();

        assertEquals(0, others.get());
        assertEquals(1, successes.get());
        assertEquals(threads - 1, conflicts.get());
        assertEquals(1, bookingRepository.count());
    }
}