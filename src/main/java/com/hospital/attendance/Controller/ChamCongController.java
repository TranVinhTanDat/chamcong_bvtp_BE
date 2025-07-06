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
        String caLamViecId = request.get("caLamViecId");
        String maKyHieuChamCong = request.get("maKyHieuChamCong");
        String ghiChu = request.get("ghiChu");
        String filterDate = request.get("filterDate");

        if (nhanVienId == null && nhanVienHoTen == null && emailNhanVien == null) {
            return ResponseEntity.badRequest().body("{\"error\": \"Thiếu thông tin nhân viên (nhanVienId, nhanVienHoTen, hoặc emailNhanVien)\"}");
        }
        if (trangThai == null || (!trangThai.equals("LÀM") && !trangThai.equals("NGHỈ"))) {
            return ResponseEntity.badRequest().body("{\"error\": \"Trạng thái phải là LÀM hoặc NGHỈ\"}");
        }
        // UPDATED: Yêu cầu caLamViecId cho cả LÀM và NGHỈ
        if (caLamViecId == null) {
            return ResponseEntity.badRequest().body("{\"error\": \"Phải cung cấp caLamViecId\"}");
        }
        // UPDATED: Chỉ yêu cầu maKyHieuChamCong khi trạng thái là NGHỈ, ghiChu có thể null
        if (trangThai.equals("NGHỈ") && maKyHieuChamCong == null) {
            return ResponseEntity.badRequest().body("{\"error\": \"Phải cung cấp maKyHieuChamCong khi trạng thái là NGHỈ\"}");
        }

        try {
            ChamCong chamCong = chamCongService.checkIn(tenDangNhap, nhanVienId, nhanVienHoTen, emailNhanVien,
                    trangThai, caLamViecId, maKyHieuChamCong, ghiChu, filterDate);
            return ResponseEntity.ok("{\"message\": \"Check-in thành công\"}");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\": \"Lỗi hệ thống: " + e.getMessage() + "\"}");
        }
    }

    // Chỉ ADMIN có quyền sửa trạng thái chấm công
    // ADMIN và NGUOICHAMCONG có quyền sửa trạng thái chấm công
    @PutMapping("/{id}/trangthai")
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOICHAMCONG')")
    public ResponseEntity<?> capNhatTrangThai(@RequestHeader("Authorization") String token,
                                              @PathVariable Long id, @RequestBody Map<String, String> request) {
        String tenDangNhap = jwtService.extractUsername(token.substring(7));
        String trangThai = request.get("trangThai");
        String caLamViecId = request.get("caLamViecId");
        String maKyHieuChamCong = request.get("maKyHieuChamCong");
        String ghiChu = request.get("ghiChu");

        if (trangThai == null || (!trangThai.equals("LÀM") && !trangThai.equals("NGHỈ"))) {
            return ResponseEntity.badRequest().body("{\"error\": \"Trạng thái phải là LÀM hoặc NGHỈ\"}");
        }
        // UPDATED: Yêu cầu caLamViecId cho cả LÀM và NGHỈ
        if (caLamViecId == null) {
            return ResponseEntity.badRequest().body("{\"error\": \"Phải cung cấp caLamViecId\"}");
        }
        // UPDATED: Chỉ yêu cầu maKyHieuChamCong khi trạng thái là NGHỈ, ghiChu có thể null
        if (trangThai.equals("NGHỈ") && maKyHieuChamCong == null) {
            return ResponseEntity.badRequest().body("{\"error\": \"Phải cung cấp maKyHieuChamCong khi trạng thái là NGHỈ\"}");
        }

        try {
            ChamCong chamCong = chamCongService.capNhatTrangThai(tenDangNhap, id, trangThai, caLamViecId, maKyHieuChamCong, ghiChu);
            return ResponseEntity.ok(chamCong);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\": \"Lỗi hệ thống: " + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/lichsu")
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOICHAMCONG', 'NGUOITONGHOP')")
    public ResponseEntity<?> layLichSuChamCong(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer day,
            @RequestParam(required = false) Long khoaPhongId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String tenDangNhap = jwtService.extractUsername(token.substring(7));
        Long userKhoaPhongId = jwtService.extractKhoaPhongId(token.substring(7));
        try {
            String role = jwtService.extractRole(token.substring(7));
            Long finalKhoaPhongId = khoaPhongId;

            if (role.equals("NGUOICHAMCONG") || role.equals("NGUOITONGHOP_1KP")) {
                finalKhoaPhongId = userKhoaPhongId;
            } else if (role.equals("ADMIN") && khoaPhongId == null) {
                finalKhoaPhongId = null;
            }

            Page<ChamCong> chamCongs = chamCongService.layLichSuChamCong(tenDangNhap, year, month, day, finalKhoaPhongId, page, size);
            return ResponseEntity.ok(chamCongs);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\": \"Lỗi hệ thống: " + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/trangthai-ngay")
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOICHAMCONG', 'NGUOITONGHOP')")
    public ResponseEntity<?> kiemTraTrangThaiChamCongTrongNgay(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String nhanVienId,
            @RequestParam(required = false) String nhanVienHoTen,
            @RequestParam(required = false) String emailNhanVien,
            @RequestParam(required = false) String filterDate) { // THÊM MỚI

        String tenDangNhap = jwtService.extractUsername(token.substring(7));

        if (nhanVienId == null && nhanVienHoTen == null && emailNhanVien == null) {
            return ResponseEntity.badRequest().body("{\"error\": \"Thiếu thông tin nhân viên (nhanVienId, nhanVienHoTen, hoặc emailNhanVien)\"}");
        }

        try {
            Map<String, Object> trangThai = chamCongService.kiemTraTrangThaiChamCongTrongNgay(
                    tenDangNhap, nhanVienId, nhanVienHoTen, emailNhanVien, filterDate);
            return ResponseEntity.ok(trangThai);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\": \"Lỗi hệ thống: " + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/chitiet-homnay")
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOICHAMCONG', 'NGUOITONGHOP')")
    public ResponseEntity<?> layChiTietChamCongHomNay(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String nhanVienId,
            @RequestParam(required = false) String nhanVienHoTen,
            @RequestParam(required = false) String emailNhanVien,
            @RequestParam(required = false) String filterDate) { // THÊM MỚI

        String tenDangNhap = jwtService.extractUsername(token.substring(7));

        if (nhanVienId == null && nhanVienHoTen == null && emailNhanVien == null) {
            return ResponseEntity.badRequest().body("{\"error\": \"Thiếu thông tin nhân viên (nhanVienId, nhanVienHoTen, hoặc emailNhanVien)\"}");
        }

        try {
            List<ChamCong> chiTiet = chamCongService.layChiTietChamCongHomNay(
                    tenDangNhap, nhanVienId, nhanVienHoTen, emailNhanVien, filterDate);
            return ResponseEntity.ok(chiTiet);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\": \"Lỗi hệ thống: " + e.getMessage() + "\"}");
        }
    }

    @PostMapping("/checkin-bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOICHAMCONG', 'NGUOITONGHOP')")
    public ResponseEntity<?> checkInBulk(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request) {

        String tenDangNhap = jwtService.extractUsername(token.substring(7));

        // Parse parameters
        Long khoaPhongId = request.get("khoaPhongId") != null ?
                Long.parseLong(request.get("khoaPhongId").toString()) : null;
        String trangThai = (String) request.get("trangThai");
        Integer shift = request.get("shift") != null ?
                Integer.parseInt(request.get("shift").toString()) : null;
        String caLamViecId = (String) request.get("caLamViecId");
        String maKyHieuChamCong = (String) request.get("maKyHieuChamCong");
        String ghiChu = (String) request.get("ghiChu");
        String filterDate = (String) request.get("filterDate");

        // Validation
        if (khoaPhongId == null) {
            return ResponseEntity.badRequest().body("{\"error\": \"Phải chọn khoa phòng\"}");
        }
        if (trangThai == null || (!trangThai.equals("LÀM") && !trangThai.equals("NGHỈ"))) {
            return ResponseEntity.badRequest().body("{\"error\": \"Trạng thái phải là LÀM hoặc NGHỈ\"}");
        }
        if (shift == null || (shift < 1 || shift > 3)) {
            return ResponseEntity.badRequest().body("{\"error\": \"Ca phải là 1 (sáng), 2 (chiều), hoặc 3 (tối)\"}");
        }
        if (caLamViecId == null) {
            return ResponseEntity.badRequest().body("{\"error\": \"Phải cung cấp caLamViecId\"}");
        }
        if (trangThai.equals("NGHỈ") && maKyHieuChamCong == null) {
            return ResponseEntity.badRequest().body("{\"error\": \"Phải cung cấp maKyHieuChamCong khi trạng thái là NGHỈ\"}");
        }

        try {
            Map<String, Object> result = chamCongService.checkInBulk(
                    tenDangNhap, khoaPhongId, trangThai, shift, caLamViecId,
                    maKyHieuChamCong, ghiChu, filterDate
            );
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Lỗi hệ thống: " + e.getMessage() + "\"}");
        }
    }


    @PutMapping("/update-bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateBulk(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request) {

        String tenDangNhap = jwtService.extractUsername(token.substring(7));

        // Parse parameters
        Long khoaPhongId = request.get("khoaPhongId") != null ?
                Long.parseLong(request.get("khoaPhongId").toString()) : null;
        String trangThai = (String) request.get("trangThai");
        Integer shift = request.get("shift") != null ?
                Integer.parseInt(request.get("shift").toString()) : null;
        String caLamViecId = (String) request.get("caLamViecId");
        String maKyHieuChamCong = (String) request.get("maKyHieuChamCong");
        String ghiChu = (String) request.get("ghiChu");
        String filterDate = (String) request.get("filterDate");

        // Validation
        if (khoaPhongId == null) {
            return ResponseEntity.badRequest().body("{\"error\": \"Phải chọn khoa phòng\"}");
        }
        if (trangThai == null || (!trangThai.equals("LÀM") && !trangThai.equals("NGHỈ"))) {
            return ResponseEntity.badRequest().body("{\"error\": \"Trạng thái phải là LÀM hoặc NGHỈ\"}");
        }
        if (shift == null || (shift < 1 || shift > 3)) {
            return ResponseEntity.badRequest().body("{\"error\": \"Ca phải là 1 (sáng), 2 (chiều), hoặc 3 (tối)\"}");
        }
        if (caLamViecId == null) {
            return ResponseEntity.badRequest().body("{\"error\": \"Phải cung cấp caLamViecId\"}");
        }
        if (trangThai.equals("NGHỈ") && maKyHieuChamCong == null) {
            return ResponseEntity.badRequest().body("{\"error\": \"Phải cung cấp maKyHieuChamCong khi trạng thái là NGHỈ\"}");
        }

        try {
            Map<String, Object> result = chamCongService.updateBulk(
                    tenDangNhap, khoaPhongId, trangThai, shift, caLamViecId,
                    maKyHieuChamCong, ghiChu, filterDate
            );
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Lỗi hệ thống: " + e.getMessage() + "\"}");
        }
    }

    @PutMapping("/update-symbol")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateAttendanceSymbol(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request) {

        try {
            Long nhanVienId = Long.parseLong(request.get("nhanVienId").toString());
            Integer day = Integer.parseInt(request.get("day").toString());
            Integer shift = Integer.parseInt(request.get("shift").toString());
            Integer month = Integer.parseInt(request.get("month").toString());
            Integer year = Integer.parseInt(request.get("year").toString());
            String newSymbol = (String) request.get("newSymbol");

            if (shift < 1 || shift > 3) {
                return ResponseEntity.badRequest()
                        .body("{\"error\": \"Ca phải là 1 (sáng), 2 (chiều), hoặc 3 (tối)\"}");
            }

            if (day < 1 || day > 31) {
                return ResponseEntity.badRequest()
                        .body("{\"error\": \"Ngày không hợp lệ\"}");
            }

            if (month < 1 || month > 12) {
                return ResponseEntity.badRequest()
                        .body("{\"error\": \"Tháng không hợp lệ\"}");
            }

            String tenDangNhap = jwtService.extractUsername(token.substring(7));

            ChamCong updatedRecord = chamCongService.updateAttendanceSymbol(
                    tenDangNhap, nhanVienId, day, shift, month, year, newSymbol);

            return ResponseEntity.ok(updatedRecord);

        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"Dữ liệu đầu vào không hợp lệ\"}");
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