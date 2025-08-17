package com.hospital.attendance.Entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@Entity
@Table(name = "mo_khoa_cham_cong")
public class MoKhoaChamCong {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "khoa_phong_id", nullable = false)
    private KhoaPhong khoaPhong;

    @JsonFormat(pattern = "dd-MM-yyyy", timezone = "Asia/Ho_Chi_Minh")
    @Column(name = "tu_ngay", nullable = false)
    private Date tuNgay;

    @JsonFormat(pattern = "dd-MM-yyyy", timezone = "Asia/Ho_Chi_Minh")
    @Column(name = "den_ngay", nullable = false)
    private Date denNgay;

    @Column(name = "ly_do", length = 500)
    private String lyDo;

    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThai = true; // true = đang hoạt động, false = đã hủy

    @ManyToOne
    @JoinColumn(name = "nguoi_tao_id", nullable = false)
    private User nguoiTao;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    @Column(name = "ngay_tao", nullable = false)
    private Date ngayTao;

    @ManyToOne
    @JoinColumn(name = "nguoi_huy_id")
    private User nguoiHuy;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    @Column(name = "ngay_huy")
    private Date ngayHuy;

    @Column(name = "ghi_chu", length = 1000)
    private String ghiChu;
}