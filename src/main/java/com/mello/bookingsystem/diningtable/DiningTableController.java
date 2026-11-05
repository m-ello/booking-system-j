package com.mello.bookingsystem.diningtable;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/tables")
@RequiredArgsConstructor
public class DiningTableController {

    private final DiningTableService service;

    @GetMapping
    public List<DiningTableResponse> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public DiningTableResponse get(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<DiningTableResponse> create(@Valid @RequestBody DiningTableRequest request) {
        DiningTableResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/tables/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public DiningTableResponse update(@PathVariable Long id, @Valid @RequestBody DiningTableRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}