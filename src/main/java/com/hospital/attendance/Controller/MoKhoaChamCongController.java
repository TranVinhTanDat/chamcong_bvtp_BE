package com.hospital.attendance.Controller;

import com.hospital.attendance.Entity.MoKhoaChamCong;
import com.hospital.attendance.Service.JwtService;
import com.hospital.attendance.Service.MoKhoaChamCongService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mo-khoa-cham-cong")
public class MoKhoaChamCongController {

    @Autowired
    private MoKhoaChamCongService moKhoaChamCongService;

    @Autowired
    private JwtService jwtService;

    // Tạo mở khóa mới - Chỉ ADMIN
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> taoMoKhoa(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request) {

        String tenDangNhap = jwtService.extractUsername(token.substring(7));

        try {
            Long khoaPhongId = Long.parseLong(request.get("khoaPhongId").toString());
            String tuNgay = (String) request.get("tuNgay"); // format: dd/MM/yyyy hoặc dd-MM-yyyy
            String denNgay = (String) request.get("denNgay"); // format: dd/MM/yyyy hoặc dd-MM-yyyy
            String lyDo = (String) request.get("lyDo");
            String ghiChu = (String) request.get("ghiChu");

            if (khoaPhongId == null || tuNgay == null || denNgay == null) {
                return ResponseEntity.badRequest()
                        .body("{\"error\": \"Thiếu thông tin bắt buộc: khoaPhongId, tuNgay, denNgay\"}");
            }

            MoKhoaChamCong moKhoa = moKhoaChamCongService.taoMoKhoa(
                    tenDangNhap, khoaPhongId, tuNgay, denNgay, lyDo, ghiChu);

            return ResponseEntity.ok(moKhoa);

        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"khoaPhongId phải là số hợp lệ\"}");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Lỗi hệ thống: " + e.getMessage() + "\"}");
        }
    }

    // Lấy danh sách mở khóa - ADMIN xem tất cả, NGUOICHAMCONG chỉ xem của khoa mình
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOICHAMCONG')")
    public ResponseEntity<?> layDanhSachMoKhoa(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) Long khoaPhongId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        String tenDangNhap = jwtService.extractUsername(token.substring(7));
        String role = jwtService.extractRole(token.substring(7));
        Long userKhoaPhongId = jwtService.extractKhoaPhongId(token.substring(7));

        try {
            Page<MoKhoaChamCong> result = moKhoaChamCongService.layDanhSachMoKhoa(
                    tenDangNhap, role, userKhoaPhongId, khoaPhongId, page, size);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Lỗi hệ thống: " + e.getMessage() + "\"}");
        }
    }

    // Hủy mở khóa - Chỉ ADMIN
    @PutMapping("/{id}/huy")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> huyMoKhoa(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> request) {

        String tenDangNhap = jwtService.extractUsername(token.substring(7));
        String lyDoHuy = request != null ? request.get("lyDoHuy") : null;

        try {
            MoKhoaChamCong moKhoa = moKhoaChamCongService.huyMoKhoa(tenDangNhap, id, lyDoHuy);
            return ResponseEntity.ok(moKhoa);

        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Lỗi hệ thống: " + e.getMessage() + "\"}");
        }
    }

    // Kiểm tra khoa phòng có được mở khóa cho ngày cụ thể không
    @GetMapping("/kiem-tra")
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOICHAMCONG')")
    public ResponseEntity<?> kiemTraMoKhoa(
            @RequestParam Long khoaPhongId,
            @RequestParam String ngayKiemTra) { // format: dd-MM-yyyy

        try {
            boolean coMoKhoa = moKhoaChamCongService.kiemTraCoMoKhoa(khoaPhongId, ngayKiemTra);

            return ResponseEntity.ok(Map.of(
                    "khoaPhongId", khoaPhongId,
                    "ngayKiemTra", ngayKiemTra,
                    "coMoKhoa", coMoKhoa
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Lỗi hệ thống: " + e.getMessage() + "\"}");
        }
    }

    // Lấy chi tiết mở khóa
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOICHAMCONG')")
    public ResponseEntity<?> layChiTietMoKhoa(@PathVariable Long id) {
        try {
            MoKhoaChamCong moKhoa = moKhoaChamCongService.layChiTietMoKhoa(id);
            return ResponseEntity.ok(moKhoa);

        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Lỗi hệ thống: " + e.getMessage() + "\"}");
        }
    }

    // ✅ FIXED: Chỉ giữ lại 1 method cho endpoint này
    // Lấy danh sách mở khóa hiện đang hoạt động cho khoa phòng
    @GetMapping("/khoa-phong/{khoaPhongId}/hien-tai")
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOICHAMCONG')")
    public ResponseEntity<?> layMoKhoaHienTai(
            @RequestHeader("Authorization") String token,
            @PathVariable Long khoaPhongId) {

        String role = jwtService.extractRole(token.substring(7));
        Long userKhoaPhongId = jwtService.extractKhoaPhongId(token.substring(7));

        try {
            // Kiểm tra quyền truy cập khoa phòng
            if ("NGUOICHAMCONG".equals(role) && !userKhoaPhongId.equals(khoaPhongId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("{\"error\": \"Không có quyền xem mở khóa của khoa phòng này\"}");
            }

            List<MoKhoaChamCong> danhSach = moKhoaChamCongService.layMoKhoaHienTai(khoaPhongId);
            return ResponseEntity.ok(danhSach);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Lỗi hệ thống: " + e.getMessage() + "\"}");
        }
    }

    // Gia hạn mở khóa - Chỉ ADMIN
    @PutMapping("/{id}/gia-han")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> giaHanMoKhoa(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {

        String tenDangNhap = jwtService.extractUsername(token.substring(7));
        String denNgayMoi = request.get("denNgayMoi"); // format: dd/MM/yyyy
        String lyDoGiaHan = request.get("lyDoGiaHan");

        try {
            if (denNgayMoi == null || denNgayMoi.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("{\"error\": \"Thiếu thông tin ngày gia hạn mới\"}");
            }

            MoKhoaChamCong moKhoa = moKhoaChamCongService.giaHanMoKhoa(
                    tenDangNhap, id, denNgayMoi, lyDoGiaHan);

            return ResponseEntity.ok(moKhoa);

        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Lỗi hệ thống: " + e.getMessage() + "\"}");
        }
    }
}