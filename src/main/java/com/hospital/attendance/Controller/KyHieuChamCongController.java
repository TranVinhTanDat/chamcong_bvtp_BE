package com.hospital.attendance.Controller;

import com.hospital.attendance.Entity.KyHieuChamCong;
import com.hospital.attendance.Service.KyHieuChamCongService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ky-hieu-cham-cong")
public class KyHieuChamCongController {

    @Autowired
    private KyHieuChamCongService kyHieuChamCongService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOICHAMCONG', 'NGUOITONGHOP')")
    public ResponseEntity<List<KyHieuChamCong>> getAllKyHieuChamCong() {
        List<KyHieuChamCong> kyHieuChamCongs = kyHieuChamCongService.getAllKyHieuChamCong();
        return ResponseEntity.ok(kyHieuChamCongs);
    }

    // Thêm endpoint phân trang và tìm kiếm
    @GetMapping("/paged")
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOICHAMCONG', 'NGUOITONGHOP')")
    public ResponseEntity<Page<KyHieuChamCong>> getPagedKyHieuChamCong(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        Page<KyHieuChamCong> kyHieuChamCongs = kyHieuChamCongService.getPagedKyHieuChamCong(page, size, search);
        return ResponseEntity.ok(kyHieuChamCongs);
    }

    // Thêm endpoint lấy chi tiết theo ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOICHAMCONG', 'NGUOITONGHOP')")
    public ResponseEntity<KyHieuChamCong> getKyHieuChamCongById(@PathVariable Long id) {
        return kyHieuChamCongService.getKyHieuChamCongById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Thêm endpoint tạo mới
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<KyHieuChamCong> createKyHieuChamCong(@RequestBody KyHieuChamCong kyHieuChamCong) {
        try {
            KyHieuChamCong created = kyHieuChamCongService.createKyHieuChamCong(kyHieuChamCong);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // Thêm endpoint cập nhật
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<KyHieuChamCong> updateKyHieuChamCong(@PathVariable Long id, @RequestBody KyHieuChamCong kyHieuChamCong) {
        try {
            KyHieuChamCong updated = kyHieuChamCongService.updateKyHieuChamCong(id, kyHieuChamCong);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Thêm endpoint xóa
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteKyHieuChamCong(@PathVariable Long id) {
        try {
            kyHieuChamCongService.deleteKyHieuChamCong(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}