package com.hospital.attendance.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "ca_lam_viec")
public class CaLamViec {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ky_hieu_cham_cong_id", nullable = false)
    private KyHieuChamCong kyHieuChamCong;

    @Column(name = "ten_ca_lam_viec", nullable = false)
    private String tenCaLamViec;
}