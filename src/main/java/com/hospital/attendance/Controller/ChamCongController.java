package com.hospital.attendance.Controller;

import com.hospital.attendance.Entity.ChamCong;
import com.hospital.attendance.Service.ChamCongService;
import com.hospital.attendance.Service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
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

    @PostMapping("/checkin")
    public ResponseEntity<?> checkIn(@RequestHeader("Authorization") String token, @RequestBody Map<String, String> request) {
        String tenDangNhapChamCong = jwtService.extractUsername(token.substring(7));
        String maNhanVien = request.get("maNhanVien");
        String hoTen = request.get("hoTen");
        String emailNhanVien = request.get("emailNhanVien");

        if (maNhanVien == null && hoTen == null && emailNhanVien == null) {
            return ResponseEntity.badRequest().body("{\"error\": \"Thiếu thông tin nhân viên (maNhanVien, hoTen, hoặc emailNhanVien)\"}");
        }

        try {
            ChamCong chamCong = chamCongService.checkIn(tenDangNhapChamCong, maNhanVien, hoTen, emailNhanVien);
            return ResponseEntity.ok("{\"message\": \"Check-in thành công, chờ duyệt\"}");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\": \"Lỗi hệ thống: " + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/lichsu")
    public ResponseEntity<?> layLichSuChamCong(@RequestHeader("Authorization") String token) {
        String tenDangNhap = jwtService.extractUsername(token.substring(7));
        try {
            List<ChamCong> chamCongs = chamCongService.layLichSuChamCong(tenDangNhap);
            return ResponseEntity.ok(chamCongs);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\": \"Lỗi hệ thống: " + e.getMessage() + "\"}");
        }
    }

    @PutMapping("/{ma}/trangthai")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> capNhatTrangThai(@PathVariable Long ma, @RequestBody Map<String, String> request) {
        String trangThai = request.get("trangThai");
        if (trangThai == null) {
            return ResponseEntity.badRequest().body("{\"error\": \"Thiếu thông tin trạng thái (trangThai)\"}");
        }
        try {
            ChamCong chamCong = chamCongService.capNhatTrangThai(ma, trangThai);
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