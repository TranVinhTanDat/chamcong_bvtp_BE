package com.hospital.attendance.Controller;

import com.hospital.attendance.Entity.KhoaPhong;
import com.hospital.attendance.Service.KhoaPhongService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/khoa-phong")
public class KhoaPhongController {

    @Autowired
    private KhoaPhongService khoaPhongService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOICHAMCONG', 'NGUOITONGHOP')")
    public ResponseEntity<List<KhoaPhong>> getAllKhoaPhongs() {
        List<KhoaPhong> khoaPhongs = khoaPhongService.getAllKhoaPhongs();
        return ResponseEntity.ok(khoaPhongs);
    }

    @GetMapping("/page")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<KhoaPhong>> getKhoaPhongsWithPagination(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String searchTerm) {
        Pageable pageable = PageRequest.of(page, size);
        Page<KhoaPhong> khoaPhongs = khoaPhongService.getKhoaPhongsWithPagination(pageable, searchTerm);
        return ResponseEntity.ok(khoaPhongs);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<KhoaPhong> createKhoaPhong(@RequestBody KhoaPhong khoaPhong) {
        KhoaPhong savedKhoaPhong = khoaPhongService.saveKhoaPhong(khoaPhong);
        return ResponseEntity.ok(savedKhoaPhong);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<KhoaPhong> updateKhoaPhong(@PathVariable Long id, @RequestBody KhoaPhong khoaPhong) {
        KhoaPhong updatedKhoaPhong = khoaPhongService.updateKhoaPhong(id, khoaPhong);
        return ResponseEntity.ok(updatedKhoaPhong);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteKhoaPhong(@PathVariable Long id) {
        khoaPhongService.deleteKhoaPhong(id);
        return ResponseEntity.noContent().build();
    }
}