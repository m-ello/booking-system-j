package com.mello.bookingsystem.booking;

import com.mello.bookingsystem.common.PageResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@Tag(name = "Bookings")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService service;

    @PostMapping
    public ResponseEntity<BookingResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                  @Valid @RequestBody BookingRequest request) {
        BookingResponse created = service.create(userId(jwt), request);
        return ResponseEntity.created(URI.create("/api/bookings/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public BookingResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.findById(id, userId(jwt), "ADMIN".equals(jwt.getClaimAsString("role")));
    }

    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }

    @GetMapping("/my")
    public List<BookingResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return service.findMine(userId(jwt));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.cancel(id, userId(jwt), isAdmin(jwt));
        return ResponseEntity.noContent().build();
    }

    private static boolean isAdmin(Jwt jwt) {
        return "ADMIN".equals(jwt.getClaimAsString("role"));
    }

    @GetMapping
    public PageResponse<BookingResponse> search(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) Long tableId,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "startTime") String sort,
            @RequestParam(defaultValue = "desc") String dir) {
        return service.search(new BookingFilter(status, tableId, email, from, to), page, size, sort, dir);
    }
}