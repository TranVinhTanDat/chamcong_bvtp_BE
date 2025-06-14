package com.hospital.attendance.Controller;

import com.hospital.attendance.Entity.ChamCong;
import com.hospital.attendance.Entity.User;
import com.hospital.attendance.Service.ChamCongService;
import com.hospital.attendance.Service.JwtService;
import com.hospital.attendance.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chamcong")
public class ChamCongController {

    @Autowired
    private ChamCongService chamCongService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserService userService;

    @PostMapping("/checkin")
    public ResponseEntity<?> checkIn(@RequestHeader("Authorization") String token, @RequestBody Map<String, String> request) {
        String tenDangNhap = jwtService.extractUsername(token.substring(7));
        String nhanVienId = request.get("nhanVienId");
        String nhanVienHoTen = request.get("nhanVienHoTen");
        String emailNhanVien = request.get("emailNhanVien");
        String trangThai = request.get("trangThai");
        String maCa = request.get("maCa");
        String maLoaiNghi = request.get("maLoaiNghi");
        String ghiChu = request.get("ghiChu");

        if (nhanVienId == null && nhanVienHoTen == null && emailNhanVien == null) {
            return ResponseEntity.badRequest().body("{\"error\": \"Thiếu thông tin nhân viên (nhanVienId, nhanVienHoTen, hoặc emailNhanVien)\"}");
        }
        if (trangThai == null || (!trangThai.equals("LÀM") && !trangThai.equals("NGHỈ"))) {
            return ResponseEntity.badRequest().body("{\"error\": \"Trạng thái phải là LÀM hoặc NGHỈ\"}");
        }
        if (trangThai.equals("NGHỈ") && (maLoaiNghi == null || ghiChu == null)) {
            return ResponseEntity.badRequest().body("{\"error\": \"Phải cung cấp maLoaiNghi và ghiChu khi trạng thái là NGHỈ\"}");
        }

        try {
            ChamCong chamCong = chamCongService.checkIn(tenDangNhap, nhanVienId, nhanVienHoTen, emailNhanVien,
                    trangThai, maCa, maLoaiNghi, ghiChu);
            return ResponseEntity.ok("{\"message\": \"Check-in thành công\"}");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\": \"Lỗi hệ thống: " + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/lichsu")
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOICHAMCONG')")
    public ResponseEntity<?> layLichSuChamCong(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer day,
            @RequestParam(required = false) Long khoaPhongId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String tenDangNhap = jwtService.extractUsername(token.substring(7));
        try {
            User user = userService.getUserByTenDangNhap(tenDangNhap);
            String role = user.getRole().getTenVaiTro();
            Long finalKhoaPhongId = khoaPhongId;

            if (role.equals("NGUOICHAMCONG")) {
                finalKhoaPhongId = user.getKhoaPhong().getId();
            } else if (role.equals("ADMIN") && khoaPhongId == null) {
                finalKhoaPhongId = null; // ADMIN can view all if no khoaPhongId is provided
            }

            Page<ChamCong> chamCongs = chamCongService.layLichSuChamCong(tenDangNhap, year, month, day, finalKhoaPhongId, page, size);
            return ResponseEntity.ok(chamCongs);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\": \"Lỗi hệ thống: " + e.getMessage() + "\"}");
        }
    }

    @PutMapping("/{id}/trangthai")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> capNhatTrangThai(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String trangThai = request.get("trangThai");
        String maCa = request.get("maCa");
        String maLoaiNghi = request.get("maLoaiNghi");
        String ghiChu = request.get("ghiChu");

        if (trangThai == null || (!trangThai.equals("LÀM") && !trangThai.equals("NGHỈ"))) {
            return ResponseEntity.badRequest().body("{\"error\": \"Trạng thái phải là LÀM hoặc NGHỈ\"}");
        }
        if (trangThai.equals("NGHỈ") && (maLoaiNghi == null || ghiChu == null)) {
            return ResponseEntity.badRequest().body("{\"error\": \"Phải cung cấp maLoaiNghi và ghiChu khi trạng thái là NGHỈ\"}");
        }

        try {
            ChamCong chamCong = chamCongService.capNhatTrangThai(id, trangThai, maCa, maLoaiNghi, ghiChu);
            return ResponseEntity.ok(chamCong);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\": \"Lỗi hệ thống: " + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/tonghop/{khoaPhongId}")
    @PreAuthorize("hasRole('NGUOITONGHOP')")
    public ResponseEntity<?> tongHopChamCong(@PathVariable Long khoaPhongId) {
        try {
            List<ChamCong> chamCongs = chamCongService.tongHopChamCongTheoPhongBan(khoaPhongId);
            return ResponseEntity.ok(chamCongs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\": \"Lỗi hệ thống: " + e.getMessage() + "\"}");
        }
    }
}