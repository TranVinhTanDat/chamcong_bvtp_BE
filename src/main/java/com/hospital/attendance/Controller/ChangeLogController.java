package com.hospital.attendance.Controller;

import com.hospital.attendance.DTO.ChangeLogDTO;
import com.hospital.attendance.Entity.ChangeLog;
import com.hospital.attendance.Entity.ChangeLogFile;
import com.hospital.attendance.Service.ChangeLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/change-log")
public class ChangeLogController {

    private static final Logger logger = LoggerFactory.getLogger(ChangeLogController.class);

    @Autowired
    private ChangeLogService changeLogService;

    @PostMapping
    public ResponseEntity<?> createChangeLog(@RequestBody ChangeLog changeLog, Authentication authentication) {
        try {
            String tenDangNhap = authentication.getName();
            ChangeLog created = changeLogService.createChangeLog(changeLog, tenDangNhap);
            logger.info("Tạo ChangeLog thành công - ID: {}, Người tạo: {}", created.getId(), tenDangNhap);

            // Trả về DTO thay vì Entity
            ChangeLogDTO createdDTO = changeLogService.convertToDTO(created);
            return ResponseEntity.ok(createdDTO);
        } catch (Exception e) {
            logger.error("Lỗi khi tạo ChangeLog", e);
            return ResponseEntity.badRequest().body("Lỗi khi tạo ChangeLog: " + e.getMessage());
        }
    }

    // THAY ĐỔI METHOD NÀY - sử dụng DTO
    @GetMapping
    public ResponseEntity<?> searchChangeLogs(
            @RequestParam(required = false) String tieuDe,
            @RequestParam(required = false) String loaiThayDoi,
            @RequestParam(required = false) String trangThai,
            @RequestParam(required = false) String version,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date tuNgay,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date denNgay,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "ngayTao") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        try {
            Sort sort = sortDirection.equalsIgnoreCase("desc") ?
                    Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);

            // SỬ DỤNG METHOD MỚI TRẢ VỀ DTO
            Page<ChangeLogDTO> result = changeLogService.searchChangeLogsDTO(
                    tieuDe, loaiThayDoi, trangThai, version, tuNgay, denNgay, pageable);

            logger.info("Tìm kiếm ChangeLog thành công - Trang: {}, Kích thước: {}, Tổng: {}",
                    page, size, result.getTotalElements());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Lỗi khi tìm kiếm ChangeLog", e);
            return ResponseEntity.badRequest().body("Lỗi khi tìm kiếm ChangeLog: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getChangeLogById(@PathVariable Long id) {
        try {
            ChangeLog changeLog = changeLogService.getChangeLogById(id);
            // Trả về DTO thay vì Entity
            ChangeLogDTO changeLogDTO = changeLogService.convertToDTO(changeLog);
            return ResponseEntity.ok(changeLogDTO);
        } catch (Exception e) {
            logger.error("Lỗi khi lấy ChangeLog với ID: {}", id, e);
            return ResponseEntity.badRequest().body("Lỗi khi lấy ChangeLog: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateChangeLog(@PathVariable Long id,
                                             @RequestBody ChangeLog changeLogDetails,
                                             Authentication authentication) {
        try {
            String tenDangNhap = authentication.getName();
            ChangeLog updated = changeLogService.updateChangeLog(id, changeLogDetails, tenDangNhap);
            logger.info("Cập nhật ChangeLog thành công - ID: {}, Người cập nhật: {}", id, tenDangNhap);

            // Trả về DTO thay vì Entity
            ChangeLogDTO updatedDTO = changeLogService.convertToDTO(updated);
            return ResponseEntity.ok(updatedDTO);
        } catch (Exception e) {
            logger.error("Lỗi khi cập nhật ChangeLog với ID: {}", id, e);
            return ResponseEntity.badRequest().body("Lỗi khi cập nhật ChangeLog: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteChangeLog(@PathVariable Long id) {
        try {
            changeLogService.deleteChangeLog(id);
            logger.info("Xóa ChangeLog thành công - ID: {}", id);
            return ResponseEntity.ok("Xóa ChangeLog thành công");
        } catch (Exception e) {
            logger.error("Lỗi khi xóa ChangeLog với ID: {}", id, e);
            return ResponseEntity.badRequest().body("Lỗi khi xóa ChangeLog: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/upload")
    public ResponseEntity<?> uploadFile(@PathVariable Long id,
                                        @RequestParam("file") MultipartFile file,
                                        @RequestParam("thuMucCha") String thuMucCha,
                                        Authentication authentication) {
        try {
            String tenDangNhap = authentication.getName();
            ChangeLogFile uploadedFile = changeLogService.uploadFile(id, file, thuMucCha, tenDangNhap);
            logger.info("Upload file thành công - ChangeLog ID: {}, File: {}, Người upload: {}",
                    id, file.getOriginalFilename(), tenDangNhap);

            // Trả về DTO thay vì Entity
            ChangeLogDTO.ChangeLogFileDTO fileDTO = changeLogService.convertFileToDTO(uploadedFile);
            return ResponseEntity.ok(fileDTO);
        } catch (IOException e) {
            logger.error("Lỗi khi upload file cho ChangeLog ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi upload file: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Lỗi khi upload file cho ChangeLog ID: {}", id, e);
            return ResponseEntity.badRequest().body("Lỗi khi upload file: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/files")
    public ResponseEntity<?> getFilesByChangeLogId(@PathVariable Long id) {
        try {
            List<ChangeLogFile> files = changeLogService.getFilesByChangeLogId(id);
            // Convert sang DTO
            List<ChangeLogDTO.ChangeLogFileDTO> fileDTOs = files.stream()
                    .map(changeLogService::convertFileToDTO)
                    .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(fileDTOs);
        } catch (Exception e) {
            logger.error("Lỗi khi lấy danh sách file cho ChangeLog ID: {}", id, e);
            return ResponseEntity.badRequest().body("Lỗi khi lấy danh sách file: " + e.getMessage());
        }
    }

    @GetMapping("/files/{fileId}/download")
    public ResponseEntity<?> downloadFile(@PathVariable Long fileId) {
        try {
            logger.info("🔽 Download request for file ID: {}", fileId);

            // Sử dụng service method
            ChangeLogFile file = changeLogService.getFileById(fileId);
            logger.info("📋 Found file: {} (path: {})", file.getTenFileGoc(), file.getDuongDan());

            // Kiểm tra file tồn tại trên đĩa
            if (!changeLogService.checkPhysicalFileExists(file.getDuongDan())) {
                logger.error("❌ Physical file not found: {}", file.getDuongDan());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("File không tồn tại trên server: " + file.getTenFileGoc());
            }

            // Lấy Resource
            Resource resource = changeLogService.getFileResource(fileId);

            logger.info("✅ File ready for download: {} (size: {} bytes)",
                    file.getTenFileGoc(), file.getKichThuoc());

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + file.getTenFileGoc() + "\"")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(file.getKichThuoc()))
                    .body(resource);

        } catch (IllegalStateException e) {
            // File không tồn tại trong database
            logger.error("❌ File not found in database: {}", fileId);
            return ResponseEntity.notFound().build();

        } catch (IOException e) {
            // Lỗi đọc file
            logger.error("❌ IO error when reading file ID {}: {}", fileId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi đọc file: " + e.getMessage());

        } catch (Exception e) {
            // Lỗi không xác định
            logger.error("❌ Unexpected error downloading file ID {}: {}", fileId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi server khi tải file: " + e.getMessage());
        }
    }

    // Thêm endpoint để kiểm tra file tồn tại
    @GetMapping("/files/{fileId}/exists")
    public ResponseEntity<?> checkFileExists(@PathVariable Long fileId) {
        try {
            ChangeLogFile file = changeLogService.getFileById(fileId);
            boolean physicalExists = changeLogService.checkPhysicalFileExists(file.getDuongDan());

            Map<String, Object> result = Map.of(
                    "exists", true,
                    "physicalExists", physicalExists,
                    "fileName", file.getTenFileGoc(),
                    "filePath", file.getDuongDan(),
                    "fileSize", file.getKichThuoc()
            );

            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(Map.of("exists", false));
        }
    }

    // Thêm endpoint để lấy thông tin file
    @GetMapping("/files/{fileId}")
    public ResponseEntity<?> getFileDetails(@PathVariable Long fileId) {
        try {
            ChangeLogFile file = changeLogService.getFileById(fileId);
            ChangeLogDTO.ChangeLogFileDTO fileDTO = changeLogService.convertFileToDTO(file);
            return ResponseEntity.ok(fileDTO);
        } catch (IllegalStateException e) {
            logger.error("❌ File not found: {}", fileId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("❌ Error getting file details: {}", fileId, e);
            return ResponseEntity.badRequest().body("Lỗi khi lấy thông tin file: " + e.getMessage());
        }
    }

    @DeleteMapping("/files/{fileId}")
    public ResponseEntity<?> deleteFile(@PathVariable Long fileId) {
        try {
            changeLogService.deleteFile(fileId);
            logger.info("Xóa file thành công - File ID: {}", fileId);
            return ResponseEntity.ok("Xóa file thành công");
        } catch (Exception e) {
            logger.error("Lỗi khi xóa file ID: {}", fileId, e);
            return ResponseEntity.badRequest().body("Lỗi khi xóa file: " + e.getMessage());
        }
    }

    @GetMapping("/folders/{thuMucCha}/files")
    public ResponseEntity<?> getFilesByThuMucCha(@PathVariable String thuMucCha) {
        try {
            List<ChangeLogFile> files = changeLogService.getFilesByThuMucCha(thuMucCha);
            // Convert sang DTO
            List<ChangeLogDTO.ChangeLogFileDTO> fileDTOs = files.stream()
                    .map(changeLogService::convertFileToDTO)
                    .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(fileDTOs);
        } catch (Exception e) {
            logger.error("Lỗi khi lấy danh sách file cho thư mục: {}", thuMucCha, e);
            return ResponseEntity.badRequest().body("Lỗi khi lấy danh sách file: " + e.getMessage());
        }
    }

    @GetMapping("/folders")
    public ResponseEntity<?> getAllThuMucCha() {
        try {
            List<String> folders = changeLogService.getAllThuMucCha();
            return ResponseEntity.ok(folders);
        } catch (Exception e) {
            logger.error("Lỗi khi lấy danh sách thư mục", e);
            return ResponseEntity.badRequest().body("Lỗi khi lấy danh sách thư mục: " + e.getMessage());
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics() {
        try {
            Map<String, Object> stats = changeLogService.getStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Lỗi khi lấy thống kê", e);
            return ResponseEntity.badRequest().body("Lỗi khi lấy thống kê: " + e.getMessage());
        }
    }

    @GetMapping("/enums")
    public ResponseEntity<?> getEnumValues() {
        try {
            Map<String, String[]> enums = Map.of(
                    "loaiThayDoi", new String[]{"THONG_SO", "PHAN_MEM", "DATABASE", "UI", "BUG_FIX", "FEATURE"},
                    "mucDoQuanTrong", new String[]{"THAP", "TRUNG_BINH", "CAO", "KHONG_CAN"},
                    "trangThai", new String[]{"DANG_TRIEN_KHAI", "HOAN_THANH", "BI_LOI", "CANCEL"}
            );
            return ResponseEntity.ok(enums);
        } catch (Exception e) {
            logger.error("Lỗi khi lấy enum values", e);
            return ResponseEntity.badRequest().body("Lỗi khi lấy enum values: " + e.getMessage());
        }
    }

}