package com.hospital.attendance.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.PastOrPresent;

import java.util.Date;

@Data
@NoArgsConstructor
@Entity
@Table(name = "nhanvien", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"email"}),
        @UniqueConstraint(columnNames = {"ma_nv"}, name = "UK_ma_nv_unique"),
        @UniqueConstraint(columnNames = {"so_dien_thoai"}, name = "UK_so_dien_thoai_unique") // THÊM MỚI
})
public class NhanVien {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ho_ten", nullable = false)
    private String hoTen;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "ma_nv", unique = true)
    private String maNV;

    @Temporal(TemporalType.DATE)
    @Column(name = "ngay_thang_nam_sinh")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    @PastOrPresent(message = "Ngày sinh không được là tương lai")
    private Date ngayThangNamSinh;

    @Column(name = "so_dien_thoai", unique = true) // THÊM unique = true
    private String soDienThoai;

    @ManyToOne
    @JoinColumn(name = "chuc_vu_id")
    private ChucVu chucVu;

    @ManyToOne
    @JoinColumn(name = "khoa_phong_id", nullable = false)
    private KhoaPhong khoaPhong;

    @Column(name = "trang_thai", nullable = false)
    private Integer trangThai = 1;
}