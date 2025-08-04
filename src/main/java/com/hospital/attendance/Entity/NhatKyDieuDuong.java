package com.hospital.attendance.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "nhat_ky_dieu_duong", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"ngay", "khoa_phong_id", "loai_mau"})
})
public class NhatKyDieuDuong {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ngay", nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    @NotNull(message = "Ngày không được để trống")
    private LocalDate ngay;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khoa_phong_id", nullable = false)
    @NotNull(message = "Khoa phòng không được để trống")
    private KhoaPhong khoaPhong;

    // Trong NhatKyDieuDuong.java
    @Column(name = "loai_mau", nullable = false, length = 20) // ✅ Thêm length = 20
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Loại mẫu không được để trống")
    private LoaiMauNhatKy loaiMau;

    // === THÔNG TIN BỆNH NHÂN (MẪU 1) ===
    @Column(name = "giuong_thuc_ke")
    @Min(value = 0, message = "Giường thực kê không được âm")
    private Integer giuongThucKe = 0;

    @Column(name = "giuong_chi_tieu")
    @Min(value = 0, message = "Giường chỉ tiêu không được âm")
    private Integer giuongChiTieu = 0;

    @Column(name = "tong_benh_cu")
    @Min(value = 0, message = "Tổng bệnh cũ không được âm")
    private Integer tongBenhCu = 0;

    @Column(name = "bn_vao_vien")
    @Min(value = 0, message = "BN vào viện không được âm")
    private Integer bnVaoVien = 0;

    @Column(name = "tong_xuat_vien")
    @Min(value = 0, message = "Tổng xuất viện không được âm")
    private Integer tongXuatVien = 0;

    @Column(name = "chuyen_vien")
    @Min(value = 0, message = "Chuyển viện không được âm")
    private Integer chuyenVien = 0;

    @Column(name = "chuyen_khoa")
    @Min(value = 0, message = "Chuyển khoa không được âm")
    private Integer chuyenKhoa = 0;

    @Column(name = "tron_vien")
    @Min(value = 0, message = "Trốn viện không được âm")
    private Integer tronVien = 0;

    @Column(name = "xin_ve")
    @Min(value = 0, message = "Xin về không được âm")
    private Integer xinVe = 0;

    @Column(name = "tu_vong")
    @Min(value = 0, message = "Tử vong không được âm")
    private Integer tuVong = 0;

    @Column(name = "benh_hien_co")
    @Min(value = 0, message = "Bệnh hiện có không được âm")
    private Integer benhHienCo = 0;

    // Tình hình sản phụ
    @Column(name = "sanh_thuong")
    @Min(value = 0, message = "Sanh thường không được âm")
    private Integer sanhThuong = 0;

    @Column(name = "sanh_mo")
    @Min(value = 0, message = "Sanh mổ không được âm")
    private Integer sanhMo = 0;

    @Column(name = "mo_phu_khoa")
    @Min(value = 0, message = "Mổ phụ khoa không được âm")
    private Integer moPhuKhoa = 0;

    // Tình hình phẫu thuật - thủ thuật
    @Column(name = "cap_cuu")
    @Min(value = 0, message = "Cấp cứu không được âm")
    private Integer capCuu = 0;

    @Column(name = "chuong_trinh")
    @Min(value = 0, message = "Chương trình không được âm")
    private Integer chuongTrinh = 0;

    @Column(name = "thu_thuat")
    @Min(value = 0, message = "Thủ thuật không được âm")
    private Integer thuThuat = 0;

    @Column(name = "tieu_phau")
    @Min(value = 0, message = "Tiểu phẫu không được âm")
    private Integer tieuPhau = 0;

    @Column(name = "phau_thuat")
    @Min(value = 0, message = "Phẫu thuật không được âm")
    private Integer phauThuat = 0;

    @Column(name = "pt_loai_i")
    @Min(value = 0, message = "PT loại I không được âm")
    private Integer ptLoaiI = 0;

    @Column(name = "pt_loai_ii")
    @Min(value = 0, message = "PT loại II không được âm")
    private Integer ptLoaiII = 0;

    @Column(name = "pt_loai_iii")
    @Min(value = 0, message = "PT loại III không được âm")
    private Integer ptLoaiIII = 0;

    // Chăm sóc điều dưỡng
    @Column(name = "tho_cpap")
    @Min(value = 0, message = "Thở CPAP không được âm")
    private Integer thoCpap = 0;

    @Column(name = "tho_may")
    @Min(value = 0, message = "Thở máy không được âm")
    private Integer thoMay = 0;

    @Column(name = "tho_oxy")
    @Min(value = 0, message = "Thở Oxy không được âm")
    private Integer thoOxy = 0;

    @Column(name = "bop_bong")
    @Min(value = 0, message = "Bóp bóng không được âm")
    private Integer bopBong = 0;

    @Column(name = "monitor")
    @Min(value = 0, message = "Monitor không được âm")
    private Integer monitor = 0;

    @Column(name = "cvp")
    @Min(value = 0, message = "CVP không được âm")
    private Integer cvp = 0;

    @Column(name = "noi_khi_quan")
    @Min(value = 0, message = "Nội khí quản không được âm")
    private Integer noiKhiQuan = 0;

    @Column(name = "noi_soi")
    @Min(value = 0, message = "Nội soi không được âm")
    private Integer noiSoi = 0;

    @Column(name = "sonde_da_day")
    @Min(value = 0, message = "Sonde dạ dày không được âm")
    private Integer sondeDaDay = 0;

    @Column(name = "sonde_tieu")
    @Min(value = 0, message = "Sonde tiểu không được âm")
    private Integer sondeTieu = 0;

    @Column(name = "hut_dam_nhot")
    @Min(value = 0, message = "Hút đàm nhớt không được âm")
    private Integer hutDamNhot = 0;

    // Phân cấp chăm sóc
    @Column(name = "cs_cap_i")
    @Min(value = 0, message = "CS cấp I không được âm")
    private Integer csCapI = 0;

    @Column(name = "cs_cap_ii")
    @Min(value = 0, message = "CS cấp II không được âm")
    private Integer csCapII = 0;

    @Column(name = "cs_cap_iii")
    @Min(value = 0, message = "CS cấp III không được âm")
    private Integer csCapIII = 0;

    // Tình hình KCB
    @Column(name = "ts_nb_kcb")
    @Min(value = 0, message = "TS NB KCB không được âm")
    private Integer tsNbKcb = 0;

    @Column(name = "ts_nb_cap_cuu")
    @Min(value = 0, message = "TS NB cấp cứu không được âm")
    private Integer tsNbCapCuu = 0;

    @Column(name = "ngoai_vien")
    @Min(value = 0, message = "Ngoại viện không được âm")
    private Integer ngoaiVien = 0;

    @Column(name = "chuyen_noi_tru")
    @Min(value = 0, message = "Chuyển nội trú không được âm")
    private Integer chuyenNoiTru = 0;

    @Column(name = "chuyen_cap_cuu")
    @Min(value = 0, message = "Chuyển cấp cứu không được âm")
    private Integer chuyenCapCuu = 0;

    @Column(name = "chuyen_vien_kcb")
    @Min(value = 0, message = "Chuyển viện KCB không được âm")
    private Integer chuyenVienKcb = 0;

    @Column(name = "chuyen_pk_k_ngoai")
    @Min(value = 0, message = "Chuyển PK K.Ngoại không được âm")
    private Integer chuyenPkKNgoai = 0;

    @Column(name = "tu_vong_kcb")
    @Min(value = 0, message = "Tử vong KCB không được âm")
    private Integer tuVongKcb = 0;

    @Column(name = "tong_nb_do_dien_tim")
    @Min(value = 0, message = "Tổng NB đo điện tim không được âm")
    private Integer tongNbDoDienTim = 0;

    @Column(name = "tong_nb_do_dien_co")
    @Min(value = 0, message = "Tổng NB đo điện cơ không được âm")
    private Integer tongNbDoDienCo = 0;

    @Column(name = "tong_nb_do_chuc_nang_ho_hap")
    @Min(value = 0, message = "Tổng NB đo chức năng hô hấp không được âm")
    private Integer tongNbDoChucNangHoHap = 0;

    // === THÔNG TIN NHÂN SỰ (MẪU 2) ===
    // Tình hình nhân sự
    @Column(name = "dieu_duong")
    @Min(value = 0, message = "Điều dưỡng không được âm")
    private Integer dieuDuong = 0;

    @Column(name = "ho_sinh")
    @Min(value = 0, message = "Hộ sinh không được âm")
    private Integer hoSinh = 0;

    @Column(name = "ky_thuat_vien")
    @Min(value = 0, message = "Kỹ thuật viên không được âm")
    private Integer kyThuatVien = 0;

    @Column(name = "y_si")
    @Min(value = 0, message = "Y sĩ không được âm")
    private Integer ySi = 0;

    @Column(name = "nhan_su_khac")
    @Min(value = 0, message = "Nhân sự khác không được âm")
    private Integer nhanSuKhac = 0;

    @Column(name = "ho_ly_nhan_su")
    @Min(value = 0, message = "Hộ lý không được âm")
    private Integer hoLyNhanSu = 0;

    @Column(name = "tong_nhan_su")
    @Min(value = 0, message = "Tổng nhân sự không được âm")
    private Integer tongNhanSu = 0;

    // Hiện diện
    @Column(name = "ddt_khoa")
    @Min(value = 0, message = "ĐDT khoa không được âm")
    private Integer ddtKhoa = 0;

    @Column(name = "ddhc")
    @Min(value = 0, message = "ĐDHC không được âm")
    private Integer ddhc = 0;

    @Column(name = "phong_kham")
    @Min(value = 0, message = "Phòng khám không được âm")
    private Integer phongKham = 0;

    @Column(name = "tour_sang")
    @Min(value = 0, message = "Tour sáng không được âm")
    private Integer tourSang = 0;

    @Column(name = "tour_chieu")
    @Min(value = 0, message = "Tour chiều không được âm")
    private Integer tourChieu = 0;

    @Column(name = "tour_dem")
    @Min(value = 0, message = "Tour đêm không được âm")
    private Integer tourDem = 0;

    @Column(name = "truc_24_24")
    @Min(value = 0, message = "Trực 24/24 không được âm")
    private Integer truc2424 = 0;

    @Column(name = "ho_ly_hien_dien")
    @Min(value = 0, message = "Hộ lý hiện diện không được âm")
    private Integer hoLyHienDien = 0;

    @Column(name = "tong_hien_dien")
    @Min(value = 0, message = "Tổng hiện diện không được âm")
    private Integer tongHienDien = 0;

    // Vắng
    @Column(name = "ra_truc")
    @Min(value = 0, message = "Ra trực không được âm")
    private Integer raTruc = 0;

    @Column(name = "bu_truc")
    @Min(value = 0, message = "Bù trực không được âm")
    private Integer buTruc = 0;

    @Column(name = "nghi_phep")
    @Min(value = 0, message = "Nghỉ phép không được âm")
    private Integer nghiPhep = 0;

    @Column(name = "nghi_om")
    @Min(value = 0, message = "Nghỉ ốm không được âm")
    private Integer nghiOm = 0;

    @Column(name = "nghi_hau_san")
    @Min(value = 0, message = "Nghỉ hậu sản không được âm")
    private Integer nghiHauSan = 0;

    @Column(name = "nghi_khac")
    @Min(value = 0, message = "Nghỉ khác không được âm")
    private Integer nghiKhac = 0;

    @Column(name = "di_hoc")
    @Min(value = 0, message = "Đi học không được âm")
    private Integer diHoc = 0;

    @Column(name = "cong_tac")
    @Min(value = 0, message = "Công tác không được âm")
    private Integer congTac = 0;

    @Column(name = "ho_ly_vang")
    @Min(value = 0, message = "Hộ lý vắng không được âm")
    private Integer hoLyVang = 0;

    @Column(name = "tong_vang")
    @Min(value = 0, message = "Tổng vắng không được âm")
    private Integer tongVang = 0;

    // Đào tạo
    @Column(name = "nhan_vien_thu_viec")
    @Min(value = 0, message = "Nhân viên thử việc không được âm")
    private Integer nhanVienThuViec = 0;

    @Column(name = "thuc_hanh_k_luong")
    @Min(value = 0, message = "Thực hành không lương không được âm")
    private Integer thucHanhKLuong = 0;

    @Column(name = "nhan_su_tang_cuong")
    @Min(value = 0, message = "Nhân sự tăng cường không được âm")
    private Integer nhanSuTangCuong = 0;

    @Column(name = "sv_dd_hs")
    @Min(value = 0, message = "SV ĐD-HS không được âm")
    private Integer svDdHs = 0;

    @Column(name = "sv_y_si")
    @Min(value = 0, message = "SV Y sĩ không được âm")
    private Integer svYSi = 0;

    @Column(name = "sv_ktv")
    @Min(value = 0, message = "SV KTV không được âm")
    private Integer svKtv = 0;

    @Column(name = "sv_duoc")
    @Min(value = 0, message = "SV Dược không được âm")
    private Integer svDuoc = 0;


    // === THÔNG TIN MẪU 3 - KHỐI CẬN LÂM SÀNG ===
// Thêm vào cuối phần khai báo fields, trước metadata

    // Khoa Xét nghiệm - Mẫu xét nghiệm
    @Column(name = "xn_tong_so_mau")
    @Min(value = 0, message = "XN tổng số mẫu không được âm")
    private Integer xnTongSoMau = 0;

    @Column(name = "xn_mau_ngoai_tru")
    @Min(value = 0, message = "XN mẫu ngoại trú không được âm")
    private Integer xnMauNgoaiTru = 0;

    @Column(name = "xn_mau_noi_tru")
    @Min(value = 0, message = "XN mẫu nội trú không được âm")
    private Integer xnMauNoiTru = 0;

    @Column(name = "xn_mau_cap_cuu")
    @Min(value = 0, message = "XN mẫu cấp cứu không được âm")
    private Integer xnMauCapCuu = 0;

    // Khoa Xét nghiệm - Người bệnh
    @Column(name = "xn_nb_tong_so")
    @Min(value = 0, message = "XN NB tổng số không được âm")
    private Integer xnNbTongSo = 0;

    @Column(name = "xn_nb_ngoai_tru")
    @Min(value = 0, message = "XN NB ngoại trú không được âm")
    private Integer xnNbNgoaiTru = 0;

    @Column(name = "xn_nb_noi_tru")
    @Min(value = 0, message = "XN NB nội trú không được âm")
    private Integer xnNbNoiTru = 0;

    @Column(name = "xn_nb_cap_cuu")
    @Min(value = 0, message = "XN NB cấp cứu không được âm")
    private Integer xnNbCapCuu = 0;

    // Khoa Xét nghiệm - Các loại xét nghiệm
    @Column(name = "xn_huyet_hoc")
    @Min(value = 0, message = "XN huyết học không được âm")
    private Integer xnHuyetHoc = 0;

    @Column(name = "xn_sinh_hoa")
    @Min(value = 0, message = "XN sinh hóa không được âm")
    private Integer xnSinhHoa = 0;

    @Column(name = "xn_vi_sinh")
    @Min(value = 0, message = "XN vi sinh không được âm")
    private Integer xnViSinh = 0;

    @Column(name = "xn_giai_phau_benh")
    @Min(value = 0, message = "XN giải phẫu bệnh không được âm")
    private Integer xnGiaiPhauBenh = 0;

    // Khoa CĐHA - X-quang
    @Column(name = "cdha_xq_tong_nb")
    @Min(value = 0, message = "CĐHA X-quang tổng NB không được âm")
    private Integer cdhaXqTongNb = 0;

    @Column(name = "cdha_xq_tong_phim")
    @Min(value = 0, message = "CĐHA X-quang tổng phim không được âm")
    private Integer cdhaXqTongPhim = 0;

    // Khoa CĐHA - CT Scanner
    @Column(name = "cdha_ct_tong_nb")
    @Min(value = 0, message = "CĐHA CT tổng NB không được âm")
    private Integer cdhaCTTongNb = 0;

    @Column(name = "cdha_ct_tong_phim")
    @Min(value = 0, message = "CĐHA CT tổng phim không được âm")
    private Integer cdhaCTTongPhim = 0;

    // Khoa CĐHA - Siêu âm
    @Column(name = "cdha_sa_tong_nb")
    @Min(value = 0, message = "CĐHA siêu âm tổng NB không được âm")
    private Integer cdhaSATongNb = 0;

    @Column(name = "cdha_sa_tong_so")
    @Min(value = 0, message = "CĐHA siêu âm tổng số không được âm")
    private Integer cdhaSATongSo = 0;

    // Thông tin metadata
    @Column(name = "ghi_chu", columnDefinition = "TEXT")
    private String ghiChu;

    @Column(name = "nguoi_tao", nullable = false)
    private String nguoiTao;

    @Column(name = "ngay_tao", nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime ngayTao;

    @Column(name = "nguoi_cap_nhat")
    private String nguoiCapNhat;

    @Column(name = "ngay_cap_nhat")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime ngayCapNhat;

    @Column(name = "trang_thai", nullable = false)
    private Integer trangThai = 1; // 1: Active, 0: Inactive




    // Enum cho loại mẫu
    // Enum cho loại mẫu - THÊM MAU_3
    public enum LoaiMauNhatKy {
        MAU_1("Nhật ký quản lý khoa khối lâm sàng"),
        MAU_2("Tình hình nhân sự khối lâm sàng"),
        MAU_3("Nhật ký quản lý khoa khối cận lâm sàng"); // ✅ THÊM MẪU 3

        private final String moTa;

        LoaiMauNhatKy(String moTa) {
            this.moTa = moTa;
        }

        public String getMoTa() {
            return moTa;
        }
    }

    @PrePersist
    protected void onCreate() {
        ngayTao = LocalDateTime.now();
        if (trangThai == null) {
            trangThai = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        ngayCapNhat = LocalDateTime.now();
    }
}