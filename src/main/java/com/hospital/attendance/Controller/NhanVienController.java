package com.hospital.attendance.Controller;

import com.hospital.attendance.Entity.NhanVien;
import com.hospital.attendance.Service.NhanVienService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/nhanvien")
public class NhanVienController {

    @Autowired
    private NhanVienService nhanVienService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOICHAMCONG', 'NGUOITONGHOP')")
    public ResponseEntity<?> createNhanVien(@RequestBody NhanVien nhanVien) {
        try {
            NhanVien savedNhanVien = nhanVienService.saveNhanVien(nhanVien);
            return ResponseEntity.ok("Thêm nhân viên thành công");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOICHAMCONG', 'NGUOITONGHOP')")
    public ResponseEntity<List<NhanVien>> getAllNhanVien() {
        List<NhanVien> nhanViens = nhanVienService.getAllNhanVien();
        return ResponseEntity.ok(nhanViens);
    }

    @GetMapping("/{ma}")
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOICHAMCONG', 'NGUOITONGHOP')")
    public ResponseEntity<NhanVien> getNhanVienById(@PathVariable Long ma) {
        return nhanVienService.getNhanVienById(ma)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.badRequest().body(null)); // Trả về null với status 400 nếu không tìm thấy
    }

    @PutMapping("/{ma}")
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOICHAMCONG', 'NGUOITONGHOP')")
    public ResponseEntity<?> updateNhanVien(@PathVariable Long ma, @RequestBody NhanVien nhanVienDetails) {
        try {
            NhanVien updatedNhanVien = nhanVienService.updateNhanVien(ma, nhanVienDetails);
            return ResponseEntity.ok("Cập nhật nhân viên thành công");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @DeleteMapping("/{ma}")
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOICHAMCONG', 'NGUOITONGHOP')")
    public ResponseEntity<?> deleteNhanVien(@PathVariable Long ma) {
        try {
            nhanVienService.deleteNhanVien(ma);
            return ResponseEntity.ok("Xóa nhân viên thành công");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @GetMapping("/khoaPhong/{khoaPhongId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOICHAMCONG', 'NGUOITONGHOP')")
    public ResponseEntity<List<NhanVien>> getNhanVienByKhoaPhongId(@PathVariable Long khoaPhongId) {
        List<NhanVien> nhanViens = nhanVienService.getNhanVienByPhongBanId(khoaPhongId);
        return ResponseEntity.ok(nhanViens);
    }
}