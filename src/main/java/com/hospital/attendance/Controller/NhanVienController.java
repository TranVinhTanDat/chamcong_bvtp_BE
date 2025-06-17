package com.hospital.attendance.Controller;

import com.hospital.attendance.Entity.NhanVien;
import com.hospital.attendance.Service.JwtService;
import com.hospital.attendance.Service.NhanVienService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/nhanvien")
public class NhanVienController {

    @Autowired
    private NhanVienService nhanVienService;

    @Autowired
    private JwtService jwtService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOICHAMCONG')")
    public ResponseEntity<?> createNhanVien(@RequestHeader("Authorization") String token, @RequestBody NhanVien nhanVien) {
        String tenDangNhap = jwtService.extractUsername(token.substring(7));
        try {
            NhanVien savedNhanVien = nhanVienService.saveNhanVien(nhanVien, tenDangNhap);
            return ResponseEntity.ok("Thêm nhân viên thành công");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOICHAMCONG')")
    public ResponseEntity<Page<NhanVien>> getAllNhanVien(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long khoaPhongId) {
        String tenDangNhap = jwtService.extractUsername(token.substring(7));
        Long userKhoaPhongId = jwtService.extractKhoaPhongId(token.substring(7));
        Long finalKhoaPhongId = khoaPhongId;
        String role = jwtService.extractRole(token.substring(7));
        if (role.equals("NGUOICHAMCONG")) {
            finalKhoaPhongId = userKhoaPhongId;
        } else if (role.equals("ADMIN") && khoaPhongId == null) {
            finalKhoaPhongId = null;
        }
        Page<NhanVien> nhanViens = nhanVienService.getAllNhanVien(tenDangNhap, page, size, finalKhoaPhongId);
        return ResponseEntity.ok(nhanViens);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOICHAMCONG')")
    public ResponseEntity<NhanVien> getNhanVienById(@PathVariable Long id) {
        return nhanVienService.getNhanVienById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.badRequest().body(null));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOICHAMCONG')")
    public ResponseEntity<?> updateNhanVien(@RequestHeader("Authorization") String token, @PathVariable Long id, @RequestBody NhanVien nhanVienDetails) {
        String tenDangNhap = jwtService.extractUsername(token.substring(7));
        try {
            NhanVien updatedNhanVien = nhanVienService.updateNhanVien(id, nhanVienDetails, tenDangNhap);
            return ResponseEntity.ok("Cập nhật nhân viên thành công");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOICHAMCONG')")
    public ResponseEntity<?> deleteNhanVien(@RequestHeader("Authorization") String token, @PathVariable Long id) {
        String tenDangNhap = jwtService.extractUsername(token.substring(7));
        try {
            nhanVienService.deleteNhanVien(id, tenDangNhap);
            return ResponseEntity.ok("Xóa nhân viên thành công");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @GetMapping("/khoaPhong/{khoaPhongId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOICHAMCONG')")
    public ResponseEntity<List<NhanVien>> getNhanVienByKhoaPhongId(@RequestHeader("Authorization") String token, @PathVariable Long khoaPhongId) {
        String tenDangNhap = jwtService.extractUsername(token.substring(7));
        List<NhanVien> nhanViens = nhanVienService.getNhanVienByPhongBanId(tenDangNhap, khoaPhongId);
        return ResponseEntity.ok(nhanViens);
    }
}