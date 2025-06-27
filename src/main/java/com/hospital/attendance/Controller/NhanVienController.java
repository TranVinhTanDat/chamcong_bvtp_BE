package com.hospital.attendance.Controller;

import com.hospital.attendance.Entity.NhanVien;
import com.hospital.attendance.Service.JwtService;
import com.hospital.attendance.Service.NhanVienService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/nhanvien")
public class NhanVienController {

    private static final Logger logger = LoggerFactory.getLogger(NhanVienController.class);

    @Autowired
    private NhanVienService nhanVienService;

    @Autowired
    private JwtService jwtService;

    // *** CẬP NHẬT: Thêm NGUOITONGHOP_1KP và cải thiện logic phân quyền ***
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOICHAMCONG', 'NGUOITONGHOP', 'NGUOITONGHOP_1KP')")
    public ResponseEntity<?> createNhanVien(@RequestHeader("Authorization") String token, @Valid @RequestBody NhanVien nhanVien, BindingResult bindingResult) {
        logger.info("POST /nhanvien request received");
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(bindingResult.getAllErrors().stream()
                    .map(ObjectError::getDefaultMessage)
                    .collect(Collectors.joining(", ")));
        }
        String tenDangNhap = jwtService.extractUsername(token.substring(7));
        String role = jwtService.extractRole(token.substring(7));
        Long userKhoaPhongId = jwtService.extractKhoaPhongId(token.substring(7));
        logger.info("User: {}, Role: {}, UserKhoaPhongId: {}", tenDangNhap, role, userKhoaPhongId);
        try {
            NhanVien savedNhanVien = nhanVienService.saveNhanVien(nhanVien, tenDangNhap);
            return ResponseEntity.ok("Thêm nhân viên thành công");
        } catch (IllegalStateException e) {
            logger.error("IllegalStateException: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            logger.error("SecurityException: {}", e.getMessage());
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    // *** CẬP NHẬT: Logic phân quyền rõ ràng hơn ***
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOICHAMCONG', 'NGUOITONGHOP', 'NGUOITONGHOP_1KP')")
    public ResponseEntity<Page<NhanVien>> getAllNhanVien(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long khoaPhongId,
            @RequestParam(required = false) String search) {

        logger.info("GET /nhanvien request received with search: {}", search);
        String tenDangNhap = jwtService.extractUsername(token.substring(7));
        Long userKhoaPhongId = jwtService.extractKhoaPhongId(token.substring(7));
        String role = jwtService.extractRole(token.substring(7));

        logger.info("User: {}, Role: {}, RequestedKhoaPhongId: {}, UserKhoaPhongId: {}, Search: {}",
                tenDangNhap, role, khoaPhongId, userKhoaPhongId, search);

        // *** LOGIC PHÂN QUYỀN ĐƯỢC CẢI THIỆN ***
        Long finalKhoaPhongId = null;

        switch (role) {
            case "ADMIN":
                // ADMIN: Có thể xem tất cả hoặc filter theo khoa phòng cụ thể
                finalKhoaPhongId = khoaPhongId; // null = tất cả, có giá trị = filter
                logger.info("ADMIN access: viewing {} employees",
                        khoaPhongId == null ? "ALL" : "khoaPhongId=" + khoaPhongId);
                break;

            case "NGUOITONGHOP":
                // NGUOITONGHOP: Có thể xem tất cả hoặc filter (tương tự ADMIN)
                finalKhoaPhongId = khoaPhongId; // null = tất cả, có giá trị = filter
                logger.info("NGUOITONGHOP access: viewing {} employees",
                        khoaPhongId == null ? "ALL" : "khoaPhongId=" + khoaPhongId);
                break;

            case "NGUOITONGHOP_1KP":
            case "NGUOICHAMCONG":
                // Các role này chỉ được xem khoa phòng của mình, BỎ QUA tham số khoaPhongId
                finalKhoaPhongId = userKhoaPhongId;
                logger.info("{} access: restricted to userKhoaPhongId={}", role, userKhoaPhongId);

                // *** SECURITY CHECK: Cảnh báo nếu có người cố gắng truy cập khoa phòng khác ***
                if (khoaPhongId != null && !khoaPhongId.equals(userKhoaPhongId)) {
                    logger.warn("SECURITY ALERT: User {} (role: {}) attempted to access khoaPhongId={} but restricted to userKhoaPhongId={}",
                            tenDangNhap, role, khoaPhongId, userKhoaPhongId);
                }
                break;

            default:
                logger.error("Unknown role: {}", role);
                return ResponseEntity.status(403).body(Page.empty());
        }

        // Gọi service với finalKhoaPhongId đã được xác định
        Page<NhanVien> nhanViens = nhanVienService.searchNhanVien(tenDangNhap, page, size, finalKhoaPhongId, search);

        logger.info("Returning {} employees for user {} (role: {})",
                nhanViens.getContent().size(), tenDangNhap, role);

        return ResponseEntity.ok(nhanViens);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOICHAMCONG', 'NGUOITONGHOP', 'NGUOITONGHOP_1KP')")
    public ResponseEntity<NhanVien> getNhanVienById(@PathVariable Long id) {
        logger.info("GET /nhanvien/{} request received", id);
        return nhanVienService.getNhanVienById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.badRequest().body(null));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOICHAMCONG', 'NGUOITONGHOP', 'NGUOITONGHOP_1KP')")
    public ResponseEntity<?> updateNhanVien(@RequestHeader("Authorization") String token, @PathVariable Long id, @Valid @RequestBody NhanVien nhanVienDetails, BindingResult bindingResult) {
        logger.info("PUT /nhanvien/{} request received", id);
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(bindingResult.getAllErrors().stream()
                    .map(ObjectError::getDefaultMessage)
                    .collect(Collectors.joining(", ")));
        }
        String tenDangNhap = jwtService.extractUsername(token.substring(7));
        String role = jwtService.extractRole(token.substring(7));
        logger.info("User: {}, Role: {}", tenDangNhap, role);
        try {
            NhanVien updatedNhanVien = nhanVienService.updateNhanVien(id, nhanVienDetails, tenDangNhap);
            return ResponseEntity.ok("Cập nhật nhân viên thành công");
        } catch (IllegalStateException e) {
            logger.error("IllegalStateException: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            logger.error("SecurityException: {}", e.getMessage());
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOICHAMCONG', 'NGUOITONGHOP', 'NGUOITONGHOP_1KP')")
    public ResponseEntity<?> disableNhanVien(@RequestHeader("Authorization") String token, @PathVariable Long id) {
        logger.info("DELETE /nhanvien/{} request received", id);
        String tenDangNhap = jwtService.extractUsername(token.substring(7));
        String role = jwtService.extractRole(token.substring(7));
        logger.info("User: {}, Role: {}", tenDangNhap, role);
        try {
            nhanVienService.disableNhanVien(id, tenDangNhap);
            return ResponseEntity.ok("Vô hiệu hóa nhân viên thành công");
        } catch (IllegalStateException e) {
            logger.error("IllegalStateException: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            logger.error("SecurityException: {}", e.getMessage());
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @GetMapping("/khoaPhong/{khoaPhongId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOICHAMCONG', 'NGUOITONGHOP', 'NGUOITONGHOP_1KP')")
    public ResponseEntity<List<NhanVien>> getNhanVienByKhoaPhongId(@RequestHeader("Authorization") String token, @PathVariable Long khoaPhongId) {
        logger.info("GET /nhanvien/khoaPhong/{} request received", khoaPhongId);
        String tenDangNhap = jwtService.extractUsername(token.substring(7));
        String role = jwtService.extractRole(token.substring(7));
        logger.info("User: {}, Role: {}", tenDangNhap, role);
        List<NhanVien> nhanViens = nhanVienService.getNhanVienByPhongBanId(tenDangNhap, khoaPhongId);
        return ResponseEntity.ok(nhanViens);
    }
}
