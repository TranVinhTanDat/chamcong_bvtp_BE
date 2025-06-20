package com.hospital.attendance.Controller;

import com.hospital.attendance.Entity.CaLamViec;
import com.hospital.attendance.Service.CaLamViecService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ca-lam-viec")
public class CaLamViecController {

    @Autowired
    private CaLamViecService caLamViecService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOICHAMCONG', 'NGUOITONGHOP')")
    public ResponseEntity<List<CaLamViec>> getAllCaLamViec() {
        List<CaLamViec> caLamViecs = caLamViecService.getAllCaLamViec();
        return ResponseEntity.ok(caLamViecs);
    }

    @GetMapping("/paged")
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOICHAMCONG', 'NGUOITONGHOP')")
    public ResponseEntity<Page<CaLamViec>> getPagedCaLamViec(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        Page<CaLamViec> caLamViecs = caLamViecService.getPagedCaLamViec(page, size, search);
        return ResponseEntity.ok(caLamViecs);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOICHAMCONG', 'NGUOITONGHOP')")
    public ResponseEntity<CaLamViec> getCaLamViecById(@PathVariable Long id) {
        return caLamViecService.getCaLamViecById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CaLamViec> createCaLamViec(@RequestBody CaLamViec caLamViec) {
        try {
            CaLamViec created = caLamViecService.createCaLamViec(caLamViec);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CaLamViec> updateCaLamViec(@PathVariable Long id, @RequestBody CaLamViec caLamViec) {
        try {
            CaLamViec updated = caLamViecService.updateCaLamViec(id, caLamViec);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCaLamViec(@PathVariable Long id) {
        try {
            caLamViecService.deleteCaLamViec(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}