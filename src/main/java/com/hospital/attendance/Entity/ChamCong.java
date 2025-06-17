package com.hospital.attendance.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@Entity
@Table(name = "chamcong")
public class ChamCong {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ma_nhan_vien", nullable = false)
    @JsonManagedReference
    private NhanVien nhanVien;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    @Column(name = "thoi_gian_check_in")
    private Date thoiGianCheckIn;

    @ManyToOne
    @JoinColumn(name = "trang_thai_cham_cong_id", nullable = false)
    private TrangThaiChamCong trangThaiChamCong;

    @ManyToOne
    @JoinColumn(name = "ca_lam_viec_id")
    private CaLamViec caLamViec;

    @ManyToOne
    @JoinColumn(name = "ky_hieu_cham_cong_id")
    private KyHieuChamCong kyHieuChamCong;

    @Column(name = "ghi_chu")
    private String ghiChu;
}