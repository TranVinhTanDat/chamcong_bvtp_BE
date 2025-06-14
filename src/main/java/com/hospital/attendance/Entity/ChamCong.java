package com.hospital.attendance.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
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
    private Long ma;

    @ManyToOne
    @JoinColumn(name = "ma_nhan_vien", nullable = false)
    @JsonBackReference
    private NhanVien nhanVien;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    @Column(name = "thoi_gian_check_in", nullable = false)
    private Date thoiGianCheckIn;

    @ManyToOne
    @JoinColumn(name = "trang_thai_cham_cong_id", nullable = false)
    private TrangThaiChamCong trangThaiChamCong;
}