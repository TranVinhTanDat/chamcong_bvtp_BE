package com.hospital.attendance.Controller;

import com.hospital.attendance.Entity.KhoaPhong;
import com.hospital.attendance.Service.KhoaPhongService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/khoa-phong")
public class KhoaPhongController {

    private static final Logger logger = LoggerFactory.getLogger(KhoaPhongController.class);

    @Autowired
    private KhoaPhongService khoaPhongService;

    // *** CẬP NHẬT: Thêm NGUOITONGHOP_1KP ***
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOICHAMCONG', 'NGUOITONGHOP', 'NGUOITONGHOP_1KP')")
    public ResponseEntity<List<KhoaPhong>> getAllKhoaPhongs() {
        logger.info("GET /khoa-phong request received");
        List<KhoaPhong> khoaPhongs = khoaPhongService.getAllKhoaPhongs();
        return ResponseEntity.ok(khoaPhongs);
    }

    @GetMapping("/page")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<KhoaPhong>> getKhoaPhongsWithPagination(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String searchTerm) {
        logger.info("GET /khoa-phong/page request received with searchTerm: {}", searchTerm);
        Pageable pageable = PageRequest.of(page, size);
        Page<KhoaPhong> khoaPhongs = khoaPhongService.getKhoaPhongsWithPagination(pageable, searchTerm);
        return ResponseEntity.ok(khoaPhongs);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createKhoaPhong(@Valid @RequestBody KhoaPhong khoaPhong, BindingResult bindingResult) {
        logger.info("POST /khoa-phong request received");

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(bindingResult.getAllErrors().stream()
                    .map(ObjectError::getDefaultMessage)
                    .collect(Collectors.joining(", ")));
        }

        try {
            KhoaPhong savedKhoaPhong = khoaPhongService.saveKhoaPhong(khoaPhong);
            return ResponseEntity.ok("Thêm khoa/phòng thành công");
        } catch (IllegalStateException e) {
            logger.error("IllegalStateException: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateKhoaPhong(@PathVariable Long id, @Valid @RequestBody KhoaPhong khoaPhong, BindingResult bindingResult) {
        logger.info("PUT /khoa-phong/{} request received", id);

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(bindingResult.getAllErrors().stream()
                    .map(ObjectError::getDefaultMessage)
                    .collect(Collectors.joining(", ")));
        }

        try {
            KhoaPhong updatedKhoaPhong = khoaPhongService.updateKhoaPhong(id, khoaPhong);
            return ResponseEntity.ok("Cập nhật khoa/phòng thành công");
        } catch (IllegalStateException e) {
            logger.error("IllegalStateException: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteKhoaPhong(@PathVariable Long id) {
        logger.info("DELETE /khoa-phong/{} request received", id);

        try {
            khoaPhongService.deleteKhoaPhong(id);
            return ResponseEntity.ok("Xóa khoa/phòng thành công");
        } catch (IllegalStateException e) {
            logger.error("IllegalStateException: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Lỗi hệ thống: " + e.getMessage());
        }
    }
}