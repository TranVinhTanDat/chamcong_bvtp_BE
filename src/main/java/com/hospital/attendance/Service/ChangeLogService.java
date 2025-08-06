package com.hospital.attendance.Service;

import com.hospital.attendance.DTO.ChangeLogDTO;
import com.hospital.attendance.Entity.ChangeLog;
import com.hospital.attendance.Entity.ChangeLogFile;
import com.hospital.attendance.Entity.User;
import com.hospital.attendance.Repository.ChangeLogRepository;
import com.hospital.attendance.Repository.ChangeLogFileRepository;
import com.hospital.attendance.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

@Service
public class ChangeLogService {

    private static final Logger logger = LoggerFactory.getLogger(ChangeLogService.class);


    @Autowired
    private ChangeLogRepository changeLogRepository;

    @Autowired
    private ChangeLogFileRepository changeLogFileRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${file.upload.dir:uploads/changelog}")
    private String uploadDirectory;

    // THÊM METHOD NÀY: Convert Entity sang DTO
    public ChangeLogDTO convertToDTO(ChangeLog changeLog) {
        ChangeLogDTO dto = new ChangeLogDTO();
        dto.setId(changeLog.getId());
        dto.setTieuDe(changeLog.getTieuDe());
        dto.setMoTa(changeLog.getMoTa());
        dto.setGhiChu(changeLog.getGhiChu());
        dto.setLoaiThayDoi(changeLog.getLoaiThayDoi());
        dto.setVersion(changeLog.getVersion());
        dto.setMucDoQuanTrong(changeLog.getMucDoQuanTrong());
        dto.setNgayTao(changeLog.getNgayTao());
        dto.setNgayCapNhat(changeLog.getNgayCapNhat());
        dto.setTrangThai(changeLog.getTrangThai());

        // Thông tin người tạo
        if (changeLog.getNguoiTao() != null) {
            dto.setNguoiTaoId(changeLog.getNguoiTao().getId());
            dto.setTenNguoiTao(changeLog.getNguoiTao().getTenDangNhap());
        }

        // Thông tin người cập nhật
        if (changeLog.getNguoiCapNhat() != null) {
            dto.setNguoiCapNhatId(changeLog.getNguoiCapNhat().getId());
            dto.setTenNguoiCapNhat(changeLog.getNguoiCapNhat().getTenDangNhap());
        }

        // Convert files
        if (changeLog.getFiles() != null) {
            List<ChangeLogDTO.ChangeLogFileDTO> fileDTOs = changeLog.getFiles().stream()
                    .map(this::convertFileToDTO)
                    .collect(Collectors.toList());
            dto.setFiles(fileDTOs);
        } else {
            dto.setFiles(new ArrayList<>());
        }

        return dto;
    }

    // THÊM METHOD NÀY: Convert File Entity sang DTO
    public ChangeLogDTO.ChangeLogFileDTO convertFileToDTO(ChangeLogFile file) {
        ChangeLogDTO.ChangeLogFileDTO dto = new ChangeLogDTO.ChangeLogFileDTO();
        dto.setId(file.getId());
        dto.setTenFileGoc(file.getTenFileGoc());
        dto.setTenFileLuu(file.getTenFileLuu());
        dto.setKichThuoc(file.getKichThuoc());
        dto.setLoaiFile(file.getLoaiFile());
        dto.setVersionFile(file.getVersionFile());
        dto.setThuMucCha(file.getThuMucCha());
        dto.setNgayUpload(file.getNgayUpload());

        if (file.getNguoiUpload() != null) {
            dto.setNguoiUploadId(file.getNguoiUpload().getId());
            dto.setTenNguoiUpload(file.getNguoiUpload().getTenDangNhap());
        }

        return dto;
    }

    // SỬA METHOD searchChangeLogs để trả về DTO
    public Page<ChangeLogDTO> searchChangeLogsDTO(String tieuDe, String loaiThayDoi, String trangThai,
                                                  String version, Date tuNgay, Date denNgay, Pageable pageable) {
        Page<ChangeLog> changeLogs = changeLogRepository.searchChangeLogs(
                tieuDe, loaiThayDoi, trangThai, version, tuNgay, denNgay, pageable);

        return changeLogs.map(this::convertToDTO);
    }

    // GIỮ NGUYÊN method cũ cho backward compatibility
    public Page<ChangeLog> searchChangeLogs(String tieuDe, String loaiThayDoi, String trangThai,
                                            String version, Date tuNgay, Date denNgay, Pageable pageable) {
        return changeLogRepository.searchChangeLogs(tieuDe, loaiThayDoi, trangThai, version, tuNgay, denNgay, pageable);
    }

    // Các method khác giữ nguyên...
    @Transactional
    public ChangeLog createChangeLog(ChangeLog changeLog, String tenDangNhap) {
        User nguoiTao = userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy người dùng: " + tenDangNhap));

        changeLog.setNguoiTao(nguoiTao);

        if (changeLog.getVersion() == null || changeLog.getVersion().isEmpty()) {
            changeLog.setVersion(generateNextVersion());
        }

        return changeLogRepository.save(changeLog);
    }

    @Transactional
    public ChangeLog updateChangeLog(Long id, ChangeLog changeLogDetails, String tenDangNhap) {
        ChangeLog changeLog = changeLogRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy ChangeLog với ID: " + id));

        User nguoiCapNhat = userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy người dùng: " + tenDangNhap));

        changeLog.setTieuDe(changeLogDetails.getTieuDe());
        changeLog.setMoTa(changeLogDetails.getMoTa());
        changeLog.setGhiChu(changeLogDetails.getGhiChu());
        changeLog.setLoaiThayDoi(changeLogDetails.getLoaiThayDoi());
        changeLog.setMucDoQuanTrong(changeLogDetails.getMucDoQuanTrong());
        changeLog.setTrangThai(changeLogDetails.getTrangThai());
        changeLog.setNguoiCapNhat(nguoiCapNhat);

        return changeLogRepository.save(changeLog);
    }

    @Transactional
    public ChangeLogFile uploadFile(Long changeLogId, MultipartFile file, String thuMucCha, String tenDangNhap) throws IOException {
        ChangeLog changeLog = changeLogRepository.findById(changeLogId)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy ChangeLog với ID: " + changeLogId));

        User nguoiUpload = userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy người dùng: " + tenDangNhap));

        String folderPath = uploadDirectory + File.separator + thuMucCha;
        File folder = new File(folderPath);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        String newVersion = generateNextFileVersion(thuMucCha);
        String originalFileName = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFileName);
        String baseFileName = getBaseFileName(originalFileName);
        String newFileName = baseFileName + "_v" + newVersion + "." + fileExtension;

        Path filePath = Paths.get(folderPath, newFileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        ChangeLogFile changeLogFile = new ChangeLogFile();
        changeLogFile.setTenFileGoc(originalFileName);
        changeLogFile.setTenFileLuu(newFileName);
        changeLogFile.setDuongDan(filePath.toString());
        changeLogFile.setKichThuoc(file.getSize());
        changeLogFile.setLoaiFile(file.getContentType());
        changeLogFile.setVersionFile(newVersion);
        changeLogFile.setThuMucCha(thuMucCha);
        changeLogFile.setChangeLog(changeLog);
        changeLogFile.setNguoiUpload(nguoiUpload);

        return changeLogFileRepository.save(changeLogFile);
    }

    public ChangeLog getChangeLogById(Long id) {
        return changeLogRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy ChangeLog với ID: " + id));
    }

    @Transactional
    public void deleteChangeLog(Long id) {
        ChangeLog changeLog = changeLogRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy ChangeLog với ID: " + id));

        List<ChangeLogFile> files = changeLogFileRepository.findByChangeLogIdOrderByNgayUploadDesc(id);
        for (ChangeLogFile file : files) {
            deletePhysicalFile(file.getDuongDan());
        }

        changeLogRepository.delete(changeLog);
    }

    public List<ChangeLogFile> getFilesByChangeLogId(Long changeLogId) {
        return changeLogFileRepository.findByChangeLogIdOrderByNgayUploadDesc(changeLogId);
    }

    public List<ChangeLogFile> getFilesByThuMucCha(String thuMucCha) {
        return changeLogFileRepository.findByThuMucChaOrderByVersionFileDesc(thuMucCha);
    }

    public List<String> getAllThuMucCha() {
        return changeLogFileRepository.findDistinctThuMucCha();
    }

    @Transactional
    public void deleteFile(Long fileId) {
        ChangeLogFile file = changeLogFileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy file với ID: " + fileId));

        deletePhysicalFile(file.getDuongDan());
        changeLogFileRepository.delete(file);
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        List<Object[]> loaiThayDoiStats = changeLogRepository.getStatisticsByLoaiThayDoi();
        Map<String, Long> loaiThayDoiMap = new HashMap<>();
        for (Object[] stat : loaiThayDoiStats) {
            loaiThayDoiMap.put((String) stat[0], (Long) stat[1]);
        }
        stats.put("thongKeLoaiThayDoi", loaiThayDoiMap);

        List<Object[]> trangThaiStats = changeLogRepository.getStatisticsByTrangThai();
        Map<String, Long> trangThaiMap = new HashMap<>();
        for (Object[] stat : trangThaiStats) {
            trangThaiMap.put((String) stat[0], (Long) stat[1]);
        }
        stats.put("thongKeTrangThai", trangThaiMap);

        stats.put("tongSoChangeLog", changeLogRepository.count());
        stats.put("tongSoFile", changeLogFileRepository.count());

        return stats;
    }

    private String generateNextVersion() {
        Optional<String> latestVersion = changeLogRepository.findLatestVersion();
        if (latestVersion.isPresent()) {
            try {
                String version = latestVersion.get(); // "1.5"
                String[] parts = version.split("\\."); // ["1", "5"]

                if (parts.length == 2) {
                    int major = Integer.parseInt(parts[0]); // 1
                    int minor = Integer.parseInt(parts[1]); // 5
                    return major + "." + (minor + 1); // "1.6"
                } else {
                    // Fallback nếu format không đúng
                    double versionNum = Double.parseDouble(version);
                    return new DecimalFormat("0.0").format(versionNum + 0.1);
                }
            } catch (NumberFormatException e) {
                return "1.0";
            }
        }
        return "1.0";
    }

    private String generateNextFileVersion(String thuMucCha) {
        Optional<String> latestVersion = changeLogFileRepository.findLatestVersionByThuMucCha(thuMucCha);
        if (latestVersion.isPresent()) {
            try {
                String version = latestVersion.get(); // "1.5"
                String[] parts = version.split("\\."); // ["1", "5"]

                if (parts.length == 2) {
                    int major = Integer.parseInt(parts[0]); // 1
                    int minor = Integer.parseInt(parts[1]); // 5
                    return major + "." + (minor + 1); // "1.6"
                } else {
                    // Fallback nếu format không đúng
                    double versionNum = Double.parseDouble(version);
                    return new DecimalFormat("0.0").format(versionNum + 0.1);
                }
            } catch (NumberFormatException e) {
                return "1.0";
            }
        }
        return "1.0";
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    private String getBaseFileName(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return fileName;
        }
        return fileName.substring(0, fileName.lastIndexOf("."));
    }

    private void deletePhysicalFile(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            System.err.println("Không thể xóa file: " + filePath + " - " + e.getMessage());
        }
    }

    // Thêm vào ChangeLogService.java

    /**
     * Lấy thông tin file theo ID
     */
    public ChangeLogFile getFileById(Long fileId) {
        return changeLogFileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy file với ID: " + fileId));
    }

    /**
     * Kiểm tra file có tồn tại trên đĩa không
     */
    public boolean checkPhysicalFileExists(String filePath) {
        try {
            Path path = Paths.get(filePath);
            return Files.exists(path) && Files.isReadable(path);
        } catch (Exception e) {
            logger.error("Lỗi khi kiểm tra file tồn tại: {}", filePath, e);
            return false;
        }
    }

    /**
     * Lấy Resource cho download
     */
    public Resource getFileResource(Long fileId) throws IOException {
        ChangeLogFile file = getFileById(fileId);

        Path filePath = Paths.get(file.getDuongDan());
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            throw new IOException("File không tồn tại hoặc không thể đọc: " + file.getTenFileGoc());
        }

        return resource;
    }
}