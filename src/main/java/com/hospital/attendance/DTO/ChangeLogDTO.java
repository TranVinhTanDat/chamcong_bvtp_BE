package com.hospital.attendance.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
public class ChangeLogDTO {
    private Long id;
    private String tieuDe;
    private String moTa;
    private String ghiChu;
    private String loaiThayDoi;
    private String version;
    private String mucDoQuanTrong;

    // THÊM @JsonFormat cho date fields
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Date ngayTao;

    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Date ngayCapNhat;

    private String trangThai;

    // Người tạo và cập nhật
    private Long nguoiTaoId;
    private String tenNguoiTao;
    private Long nguoiCapNhatId;
    private String tenNguoiCapNhat;

    // Danh sách file
    private List<ChangeLogFileDTO> files;

    @Data
    @NoArgsConstructor
    public static class ChangeLogFileDTO {
        private Long id;
        private String tenFileGoc;
        private String tenFileLuu;
        private Long kichThuoc;
        private String loaiFile;
        private String versionFile;
        private String thuMucCha;

        // THÊM @JsonFormat cho date field
        @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
        private Date ngayUpload;

        private Long nguoiUploadId;
        private String tenNguoiUpload;
    }
}