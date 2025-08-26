package com.hospital.attendance.Controller;

import com.hospital.attendance.Entity.NhatKyDieuDuong.LoaiMauNhatKy;
import com.hospital.attendance.Service.JwtService;
import com.hospital.attendance.Service.NhatKyDieuDuongExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/nhat-ky-dieu-duong/export")
public class NhatKyDieuDuongExportController {

    private static final Logger logger = LoggerFactory.getLogger(NhatKyDieuDuongExportController.class);

    @Autowired
    private NhatKyDieuDuongExportService exportService;

    @Autowired
    private JwtService jwtService;

    /**
     * Export báo cáo tháng ra Excel
     */
    @GetMapping("/bao-cao-thang")
    @PreAuthorize("hasAnyRole('ADMIN', 'NGUOIDIENNHATKYDD', 'NGUOITONGHOP_NHATKYDD')")
    public ResponseEntity<byte[]> exportBaoCaoThang(
            @RequestHeader("Authorization") String token,
            @RequestParam Long khoaPhongId,
            @RequestParam LoaiMauNhatKy loaiMau,
            @RequestParam Integer thang,
            @RequestParam Integer nam) {

        logger.info("GET /nhat-ky-dieu-duong/export/bao-cao-thang request received - khoaPhongId: {}, loaiMau: {}, thang: {}, nam: {}",
                khoaPhongId, loaiMau, thang, nam);

        // Validate tháng và năm
        if (thang < 1 || thang > 12) {
            return ResponseEntity.badRequest().build();
        }
        if (nam < 2020 || nam > 2030) {
            return ResponseEntity.badRequest().build();
        }

        String tenDangNhap = jwtService.extractUsername(token.substring(7));
        String role = jwtService.extractRole(token.substring(7));
        Long userKhoaPhongId = jwtService.extractKhoaPhongId(token.substring(7));

        logger.info("User: {}, Role: {}, UserKhoaPhongId: {}", tenDangNhap, role, userKhoaPhongId);

        try {
            byte[] excelData = exportService.exportMonthlyReport(tenDangNhap, khoaPhongId, loaiMau, thang, nam);

            // Tạo tên file
            String fileName = generateFileName(loaiMau, thang, nam, khoaPhongId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setContentLength(excelData.length);

            logger.info("Export successful, file size: {} bytes", excelData.length);
            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);

        } catch (SecurityException e) {
            logger.error("SecurityException: {}", e.getMessage());
            return ResponseEntity.status(403).build();
        } catch (IllegalStateException e) {
            logger.error("IllegalStateException: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            logger.error("IOException during export: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        } catch (Exception e) {
            logger.error("Unexpected error during export: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Tạo tên file cho export
     */
    private String generateFileName(LoaiMauNhatKy loaiMau, Integer thang, Integer nam, Long khoaPhongId) {
        String prefix;
        switch (loaiMau) {
            case MAU_1:
                prefix = "NhatKy_QuanLy_Khoa";
                break;
            case MAU_2:
                prefix = "NhatKy_NhanSu_Khoa";
                break;
            case MAU_3:
                prefix = "NhatKy_CanLamSang_Khoa";
                break;
            default:
                prefix = "NhatKy_Khoa";
                break;
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return String.format("%s_Khoa%d_T%02d_%d_%s.xlsx", prefix, khoaPhongId, thang, nam, timestamp);
    }
}