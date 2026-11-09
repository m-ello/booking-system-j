package com.mello.bookingsystem.diningtable;

import com.mello.bookingsystem.common.BadRequestException;
import com.mello.bookingsystem.common.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DiningTableService {

    private final DiningTableRepository repository;

    @Transactional(readOnly = true)
    public List<DiningTableResponse> findAll() {
        return repository.findAll().stream().map(DiningTableResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public DiningTableResponse findById(Long id) {
        return DiningTableResponse.from(getOrThrow(id));
    }

    @Transactional
    public DiningTableResponse create(DiningTableRequest request) {
        DiningTable table = new DiningTable();
        apply(table, request);
        return DiningTableResponse.from(repository.save(table));
    }

    @Transactional
    public DiningTableResponse update(Long id, DiningTableRequest request) {
        DiningTable table = getOrThrow(id);
        apply(table, request);
        return DiningTableResponse.from(table);
    }

    @Transactional
    public void deactivate(Long id) {
        getOrThrow(id).setActive(false);
    }

    @Transactional(readOnly = true)
    public List<DiningTableResponse> findAvailable(Instant start, Instant end, int partySize) {
        if (!end.isAfter(start)) throw new BadRequestException("End time must be after start time");
        return repository.findAvailable(start, end, partySize).stream().map(DiningTableResponse::from).toList();
    }

    private DiningTable getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Table " + id + " not found"));
    }

    private void apply(DiningTable table, DiningTableRequest r) {
        table.setLabel(r.label());
        table.setDescription(r.description());
        table.setSeats(r.seats());
        table.setZone(r.zone());
        if (r.active() != null) table.setActive(r.active());
    }
}