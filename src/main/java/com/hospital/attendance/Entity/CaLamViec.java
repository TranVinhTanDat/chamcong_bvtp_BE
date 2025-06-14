package com.hospital.attendance.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "ca_lam_viec")
public class CaLamViec {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_ca", nullable = false, unique = true)
    private String maCa;

    @Column(name = "ten_ca", nullable = false)
    private String tenCa;

    @Column(name = "thoi_gian_bat_dau", nullable = false)
    private LocalTime thoiGianBatDau;

    @Column(name = "thoi_gian_ket_thuc", nullable = false)
    private LocalTime thoiGianKetThuc;
}