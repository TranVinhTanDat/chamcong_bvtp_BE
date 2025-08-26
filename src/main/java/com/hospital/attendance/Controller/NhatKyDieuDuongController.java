package com.hospital.attendance.Controller;

import com.hospital.attendance.Entity.NhatKyDieuDuong;
import com.hospital.attendance.Entity.NhatKyDieuDuong.LoaiMauNhatKy;
import com.hospital.attendance.DTO.NhatKyDieuDuongResponseDTO;
import com.hospital.attendance.DTO.NhatKyDieuDuongRequestDTO;
import com.hospital.attendance.Repository.NhatKyDieuDuongRepository;
import com.hospital.attendance.Service.JwtService;
import com.hospital.attendance.Service.NhatKyDieuDuongService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/nhat-ky-dieu-duong")
public class NhatKyDieuDuongController {

    private static final Logger logger = LoggerFactory.getLogger(NhatKyDieuDuongController.class);

    @Autowired
    private NhatKyDieuDuongService nhatKyDieuDuongService;

    @Autowired
    private NhatKyDieuDuongRepository nhatKyDieuDuongRepository;

    @Autowired
    private JwtService jwtService;

    /**
     * Tạo mới nhật ký điều dưỡng
     */
    // ✅ Trong NhatKyDieuDuongController, sửa exception handling:

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOIDIENNHATKYDD')")
    public ResponseEntity<?> createNhatKyDieuDuong(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody NhatKyDieuDuongRequestDTO requestDTO,
            BindingResult bindingResult,
            HttpServletRequest request) {

        logger.info("🚀 POST /nhat-ky-dieu-duong request received");
        logger.info("📝 DTO: ngay={}, khoaPhongId={}, loaiMau={}",
                requestDTO.getNgay(), requestDTO.getKhoaPhongId(), requestDTO.getLoaiMau());

        if (bindingResult.hasErrors()) {
            logger.error("❌ Validation errors: {}", bindingResult.getAllErrors());
            return ResponseEntity.badRequest().body(bindingResult.getAllErrors().stream()
                    .map(ObjectError::getDefaultMessage)
                    .collect(Collectors.joining(", ")));
        }

        String tenDangNhap = jwtService.extractUsername(token.substring(7));

        try {
            NhatKyDieuDuong nhatKyDieuDuong = requestDTO.toEntity();
            NhatKyDieuDuong savedNhatKy = nhatKyDieuDuongService.createNhatKyDieuDuong(nhatKyDieuDuong, tenDangNhap);
            NhatKyDieuDuongResponseDTO responseDTO = NhatKyDieuDuongResponseDTO.fromEntity(savedNhatKy);

            logger.info("✅ Successfully created/restored NhatKy with ID: {}", savedNhatKy.getId());
            return ResponseEntity.ok(responseDTO);

        } catch (IllegalStateException e) {
            logger.error("❌ IllegalStateException: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            logger.error("❌ SecurityException: {}", e.getMessage());
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (DataIntegrityViolationException e) {
            // ✅ CẢI THIỆN XỬ LÝ DUPLICATE ERROR
            logger.error("❌ DataIntegrityViolationException: {}", e.getMessage());

            if (e.getMessage().contains("Duplicate entry") && e.getMessage().contains("UKkxo45m3ss3x84povbkjume1i8")) {
                String loaiMauText = requestDTO.getLoaiMau() == LoaiMauNhatKy.MAU_1 ? "Mẫu 1 - Quản lý khoa" : "Mẫu 2 - Nhân sự";
                String errorMessage = String.format("Nhật ký cho ngày %s với %s đã tồn tại!",
                        requestDTO.getNgay(), loaiMauText);

                // ✅ THÊM THÔNG TIN DEBUG
                logger.error("🔍 Duplicate detected but should have been handled by service layer. " +
                        "This suggests a race condition or constraint issue.");

                return ResponseEntity.badRequest().body(errorMessage);
            }

            return ResponseEntity.badRequest().body("Lỗi ràng buộc dữ liệu: Có thể dữ liệu đã tồn tại");
        } catch (Exception e) {
            logger.error("❌ Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Lỗi hệ thống: " + e.getMessage());
        }
    }
    /**
     * Lấy danh sách nhật ký với phân trang và filter - FIXED
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOIDIENNHATKYDD', 'NGUOITONGHOP_NHATKYDD')")
    public ResponseEntity<Page<NhatKyDieuDuongResponseDTO>> getNhatKyDieuDuong(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long khoaPhongId,
            @RequestParam(required = false) LoaiMauNhatKy loaiMau,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate tuNgay,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate denNgay) {

        logger.info("GET /nhat-ky-dieu-duong request received with filters - khoaPhongId: {}, loaiMau: {}, tuNgay: {}, denNgay: {}",
                khoaPhongId, loaiMau, tuNgay, denNgay);

        String tenDangNhap = jwtService.extractUsername(token.substring(7));

        try {
            Page<NhatKyDieuDuong> nhatKyPage = nhatKyDieuDuongService.getNhatKyDieuDuongWithFilters(
                    tenDangNhap, page, size, khoaPhongId, loaiMau, tuNgay, denNgay);

            // Convert Entity Page to DTO Page - THIS FIXES THE HIBERNATE PROXY ERROR
            Page<NhatKyDieuDuongResponseDTO> dtoPage = nhatKyPage.map(NhatKyDieuDuongResponseDTO::fromEntity);

            logger.info("Returning {} nhật ký điều dưỡng for user {}", dtoPage.getContent().size(), tenDangNhap);
            return ResponseEntity.ok(dtoPage);
        } catch (SecurityException e) {
            logger.error("SecurityException: {}", e.getMessage());
            return ResponseEntity.status(403).body(Page.empty());
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Page.empty());
        }
    }

    /**
     * Lấy nhật ký theo ID - FIXED
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOIDIENNHATKYDD', 'NGUOITONGHOP_NHATKYDD')")
    public ResponseEntity<NhatKyDieuDuongResponseDTO> getNhatKyDieuDuongById(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {

        logger.info("GET /nhat-ky-dieu-duong/{} request received", id);

        String tenDangNhap = jwtService.extractUsername(token.substring(7));

        try {
            Optional<NhatKyDieuDuong> nhatKy = nhatKyDieuDuongService.getNhatKyDieuDuongById(id, tenDangNhap);

            if (nhatKy.isPresent()) {
                NhatKyDieuDuongResponseDTO responseDTO = NhatKyDieuDuongResponseDTO.fromEntity(nhatKy.get());
                return ResponseEntity.ok(responseDTO);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (SecurityException e) {
            logger.error("SecurityException: {}", e.getMessage());
            return ResponseEntity.status(403).build();
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Cập nhật nhật ký điều dưỡng - FIXED
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOIDIENNHATKYDD')")
    public ResponseEntity<?> updateNhatKyDieuDuong(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id,
            @Valid @RequestBody NhatKyDieuDuongRequestDTO requestDTO,
            BindingResult bindingResult) {

        logger.info("PUT /nhat-ky-dieu-duong/{} request received", id);

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(bindingResult.getAllErrors().stream()
                    .map(ObjectError::getDefaultMessage)
                    .collect(Collectors.joining(", ")));
        }

        String tenDangNhap = jwtService.extractUsername(token.substring(7));

        try {
            // Convert DTO to Entity
            NhatKyDieuDuong nhatKyDetails = requestDTO.toEntity();

            NhatKyDieuDuong updatedNhatKy = nhatKyDieuDuongService.updateNhatKyDieuDuong(id, nhatKyDetails, tenDangNhap);

            // Return DTO instead of Entity
            NhatKyDieuDuongResponseDTO responseDTO = NhatKyDieuDuongResponseDTO.fromEntity(updatedNhatKy);

            return ResponseEntity.ok(responseDTO);
        } catch (IllegalStateException e) {
            logger.error("IllegalStateException: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            logger.error("SecurityException: {}", e.getMessage());
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    /**
     * Xóa nhật ký điều dưỡng (chỉ ADMIN)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOIDIENNHATKYDD')")
    public ResponseEntity<?> deleteNhatKyDieuDuong(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {

        logger.info("DELETE /nhat-ky-dieu-duong/{} request received", id);

        String tenDangNhap = jwtService.extractUsername(token.substring(7));

        try {
            nhatKyDieuDuongService.deleteNhatKyDieuDuong(id, tenDangNhap);
            return ResponseEntity.ok("Xóa nhật ký điều dưỡng thành công");
        } catch (IllegalStateException e) {
            logger.error("IllegalStateException: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            logger.error("SecurityException: {}", e.getMessage());
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    /**
     * Lấy nhật ký theo tháng để tạo báo cáo - FIXED
     */
    @GetMapping("/bao-cao-thang")
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOIDIENNHATKYDD', 'NGUOITONGHOP_NHATKYDD')")
    public ResponseEntity<List<NhatKyDieuDuongResponseDTO>> getBaoCaoThang(
            @RequestHeader("Authorization") String token,
            @RequestParam Long khoaPhongId,
            @RequestParam LoaiMauNhatKy loaiMau,
            @RequestParam Integer thang,
            @RequestParam Integer nam) {

        logger.info("GET /nhat-ky-dieu-duong/bao-cao-thang request received - khoaPhongId: {}, loaiMau: {}, thang: {}, nam: {}",
                khoaPhongId, loaiMau, thang, nam);

        // Validate tháng và năm
        if (thang < 1 || thang > 12) {
            return ResponseEntity.badRequest().build();
        }
        if (nam < 2020 || nam > 2030) {
            return ResponseEntity.badRequest().build();
        }

        String tenDangNhap = jwtService.extractUsername(token.substring(7));

        try {
            List<NhatKyDieuDuong> nhatKyList = nhatKyDieuDuongService.getNhatKyForMonthlyReport(
                    tenDangNhap, khoaPhongId, loaiMau, thang, nam);

            // Convert Entity List to DTO List
            List<NhatKyDieuDuongResponseDTO> responseDTOList = nhatKyList.stream()
                    .map(NhatKyDieuDuongResponseDTO::fromEntity)
                    .collect(Collectors.toList());

            logger.info("Returning {} nhật ký for monthly report", responseDTOList.size());
            return ResponseEntity.ok(responseDTOList);
        } catch (SecurityException e) {
            logger.error("SecurityException: {}", e.getMessage());
            return ResponseEntity.status(403).build();
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Lấy template từ bản ghi gần nhất - FIXED
     */
    @GetMapping("/template")
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOIDIENNHATKYDD' , 'NGUOITONGHOP_NHATKYDD')")
    public ResponseEntity<NhatKyDieuDuongRequestDTO> getTemplate(
            @RequestHeader("Authorization") String token,
            @RequestParam Long khoaPhongId,
            @RequestParam LoaiMauNhatKy loaiMau) {

        logger.info("GET /nhat-ky-dieu-duong/template request received - khoaPhongId: {}, loaiMau: {}", khoaPhongId, loaiMau);

        String tenDangNhap = jwtService.extractUsername(token.substring(7));

        try {
            Optional<NhatKyDieuDuong> template = nhatKyDieuDuongService.getLatestTemplate(tenDangNhap, khoaPhongId, loaiMau);

            if (template.isPresent()) {
                // Convert to RequestDTO for template (suitable for form filling)
                NhatKyDieuDuongRequestDTO templateDTO = NhatKyDieuDuongRequestDTO.fromEntity(template.get());

                // Reset fields not needed for template
                templateDTO.setNgay(LocalDate.now()); // Set current date as default
                templateDTO.setGhiChu(null);

                return ResponseEntity.ok(templateDTO);
            } else {
                // Return empty template
                NhatKyDieuDuongRequestDTO emptyTemplate = NhatKyDieuDuongRequestDTO.createEmptyTemplate(loaiMau, khoaPhongId);
                return ResponseEntity.ok(emptyTemplate);
            }
        } catch (SecurityException e) {
            logger.error("SecurityException: {}", e.getMessage());
            return ResponseEntity.status(403).build();
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Lấy danh sách loại mẫu nhật ký
     */
    @GetMapping("/loai-mau")
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOIDIENNHATKYDD', 'NGUOITONGHOP_NHATKYDD')")
    public ResponseEntity<LoaiMauNhatKy[]> getLoaiMauNhatKy() {
        logger.info("GET /nhat-ky-dieu-duong/loai-mau request received");
        return ResponseEntity.ok(LoaiMauNhatKy.values());
    }

    /**
     * Kiểm tra nhật ký đã tồn tại cho ngày cụ thể
     */
    @GetMapping("/kiem-tra-ton-tai")
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOIDIENNHATKYDD', 'NGUOITONGHOP_NHATKYDD')")
    public ResponseEntity<Boolean> kiemTraTonTai(
            @RequestHeader("Authorization") String token,
            @RequestParam @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate ngay,
            @RequestParam Long khoaPhongId,
            @RequestParam LoaiMauNhatKy loaiMau) {

        logger.info("GET /nhat-ky-dieu-duong/kiem-tra-ton-tai request received - ngay: {}, khoaPhongId: {}, loaiMau: {}",
                ngay, khoaPhongId, loaiMau);

        String tenDangNhap = jwtService.extractUsername(token.substring(7));

        try {
            boolean exists = nhatKyDieuDuongService.kiemTraTonTai(tenDangNhap, ngay, khoaPhongId, loaiMau);
            return ResponseEntity.ok(exists);
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(false);
        }
    }

    /**
     * Phục hồi nhật ký điều dưỡng đã bị xóa (chỉ ADMIN)
     */
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOIDIENNHATKYDD')")
    public ResponseEntity<?> restoreNhatKyDieuDuong(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {

        logger.info("POST /nhat-ky-dieu-duong/{}/restore request received", id);

        String tenDangNhap = jwtService.extractUsername(token.substring(7));

        try {
            NhatKyDieuDuong restoredNhatKy = nhatKyDieuDuongService.restoreNhatKyDieuDuong(id, tenDangNhap);
            NhatKyDieuDuongResponseDTO responseDTO = NhatKyDieuDuongResponseDTO.fromEntity(restoredNhatKy);

            return ResponseEntity.ok(responseDTO);
        } catch (IllegalStateException e) {
            logger.error("IllegalStateException: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            logger.error("SecurityException: {}", e.getMessage());
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    /**
     * Lấy danh sách nhật ký đã bị xóa (chỉ ADMIN)
     */
    @GetMapping("/deleted")
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOIDIENNHATKYDD')")
    public ResponseEntity<Page<NhatKyDieuDuongResponseDTO>> getDeletedNhatKyDieuDuong(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long khoaPhongId,
            @RequestParam(required = false) LoaiMauNhatKy loaiMau) {

        logger.info("GET /nhat-ky-dieu-duong/deleted request received");

        String tenDangNhap = jwtService.extractUsername(token.substring(7));

        try {
            // Sử dụng existing method nhưng với trangThai = 0
            Pageable pageable = PageRequest.of(page, size);

            Page<NhatKyDieuDuong> deletedPage = nhatKyDieuDuongRepository.findByFiltersWithPagination(
                    khoaPhongId, loaiMau, 0, pageable); // trangThai = 0 (deleted)

            Page<NhatKyDieuDuongResponseDTO> dtoPage = deletedPage.map(NhatKyDieuDuongResponseDTO::fromEntity);

            logger.info("Returning {} deleted nhật ký điều dưỡng", dtoPage.getContent().size());
            return ResponseEntity.ok(dtoPage);
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Page.empty());
        }
    }
}