package com.hospital.attendance.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@Entity
@Table(name = "nhanvien", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"email"})
})
public class NhanVien {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ho_ten", nullable = false)
    private String hoTen;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "ma_nv")
    private String maNV;

    @Temporal(TemporalType.DATE)
    @Column(name = "ngay_thang_nam_sinh")
    private Date ngayThangNamSinh;

    @Column(name = "so_dien_thoai")
    private String soDienThoai;

    @ManyToOne
    @JoinColumn(name = "chuc_vu_id")
    private ChucVu chucVu;

    @ManyToOne
    @JoinColumn(name = "khoa_phong_id", nullable = false)
    private KhoaPhong khoaPhong;
}