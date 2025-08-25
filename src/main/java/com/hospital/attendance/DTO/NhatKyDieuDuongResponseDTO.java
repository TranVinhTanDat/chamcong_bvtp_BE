package com.hospital.attendance.DTO;

import com.hospital.attendance.Entity.NhatKyDieuDuong.LoaiMauNhatKy;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class NhatKyDieuDuongResponseDTO {

    private Long id;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate ngay;

    private Long khoaPhongId;
    private String tenKhoaPhong;
    private String maKhoaPhong;

    private LoaiMauNhatKy loaiMau;
    private String moTaLoaiMau;

    // === THÔNG TIN BỆNH NHÂN (MẪU 1) ===
    private Integer giuongThucKe;
    private Integer giuongChiTieu;
    private Integer tongBenhCu;
    private Integer bnVaoVien;
    private Integer tongXuatVien;
    private Integer chuyenVien;
    private Integer chuyenKhoa;
    private Integer tronVien;
    private Integer xinVe;
    private Integer tuVong;
    private Integer benhHienCo;

    // Tình hình sản phụ
    private Integer sanhThuong;
    private Integer sanhMo;
    private Integer moPhuKhoa;

    // Tình hình phẫu thuật - thủ thuật
    private Integer capCuu;
    private Integer chuongTrinh;
    private Integer thuThuat;
    private Integer tieuPhau;
    private Integer phauThuat;
    private Integer ptLoaiI;
    private Integer ptLoaiII;
    private Integer ptLoaiIII;

    // Chăm sóc điều dưỡng
    private Integer thoCpap;
    private Integer thoMay;
    private Integer thoOxy;
    private Integer bopBong;
    private Integer monitor;
    private Integer cvp;
    private Integer noiKhiQuan;
    private Integer noiSoi;
    private Integer sondeDaDay;
    private Integer sondeTieu;
    private Integer hutDamNhot;

    // Phân cấp chăm sóc
    private Integer csCapI;
    private Integer csCapII;
    private Integer csCapIII;

    // Tình hình KCB
    private Integer tsNbKcb;
    private Integer tsNbCapCuu;
    private Integer ngoaiVien;
    private Integer chuyenNoiTru;
    private Integer chuyenCapCuu;
    private Integer chuyenVienKcb;
    private Integer chuyenPkKNgoai;
    private Integer tuVongKcb;
    private Integer tongNbDoDienTim;
    private Integer tongNbDoDienCo;
    private Integer tongNbDoChucNangHoHap;

    // === THÔNG TIN NHÂN SỰ (MẪU 2) ===
    // Tình hình nhân sự
    private Integer dieuDuong;
    private Integer hoSinh;
    private Integer kyThuatVien;
    private Integer ySi;
    private Integer nhanSuKhac;
    private Integer hoLyNhanSu;
    private Integer tongNhanSu;

    // Hiện diện
    private Integer ddtKhoa;
    private Integer ddhc;
    private Integer phongKham;
    private Integer tourSang;
    private Integer tourChieu;
    private Integer tourDem;
    private Integer truc2424;
    private Integer hoLyHienDien;
    private Integer tongHienDien;

    // Vắng
    private Integer raTruc;
    private Integer buTruc;
    private Integer nghiPhep;
    private Integer nghiOm;
    private Integer nghiHauSan;
    private Integer nghiKhac;
    private Integer diHoc;
    private Integer congTac;
    private Integer hoLyVang;
    private Integer tongVang;

    // Đào tạo
    private Integer nhanVienThuViec;
    private Integer thucHanhKLuong;
    private Integer nhanSuTangCuong;
    private Integer svDdHs;
    private Integer svYSi;
    private Integer svKtv;
    private Integer svDuoc;


    // === THÔNG TIN MẪU 3 - KHỐI CẬN LÂM SÀNG ===
// Thêm vào sau phần Mẫu 2, trước phần Metadata

    // Khoa Xét nghiệm - Mẫu xét nghiệm
    private Integer xnTongSoMau;
    private Integer xnMauNgoaiTru;
    private Integer xnMauNoiTru;
    private Integer xnMauCapCuu;

    // Khoa Xét nghiệm - Người bệnh
    private Integer xnNbTongSo;
    private Integer xnNbNgoaiTru;
    private Integer xnNbNoiTru;
    private Integer xnNbCapCuu;

    // Khoa Xét nghiệm - Các loại xét nghiệm
    private Integer xnHuyetHoc;
    private Integer xnSinhHoa;
    private Integer xnViSinh;
    private Integer xnGiaiPhauBenh;

    // Khoa CĐHA - X-quang
    private Integer cdhaXqTongNb;
    private Integer cdhaXqTongPhim;

    // Khoa CĐHA - CT Scanner
    private Integer cdhaCTTongNb;
    private Integer cdhaCTTongPhim;

    // Khoa CĐHA - Siêu âm
    private Integer cdhaSATongNb;
    private Integer cdhaSATongSo;

    // Metadata
    private String ghiChu;
    private String nguoiTao;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private LocalDateTime ngayTao;

    private String nguoiCapNhat;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private LocalDateTime ngayCapNhat;

    private Integer trangThai;

    // Tính toán tự động (có thể thêm)
    private Integer tongBenhNhan; // = tongBenhCu + bnVaoVien
    private Integer tyLeLapGiuong; // = (giuongThucKe / giuongChiTieu) * 100
    private Integer tongNhanSuHienDien; // Tổng nhân sự có mặt
    private Integer tongNhanSuVang; // Tổng nhân sự vắng

    // Constructor từ Entity
    public static NhatKyDieuDuongResponseDTO fromEntity(com.hospital.attendance.Entity.NhatKyDieuDuong entity) {
        NhatKyDieuDuongResponseDTO dto = new NhatKyDieuDuongResponseDTO();

        // Basic info
        dto.setId(entity.getId());
        dto.setNgay(entity.getNgay());
        dto.setKhoaPhongId(entity.getKhoaPhong().getId());
        dto.setTenKhoaPhong(entity.getKhoaPhong().getTenKhoaPhong());
        dto.setMaKhoaPhong(entity.getKhoaPhong().getMaKhoaPhong());
        dto.setLoaiMau(entity.getLoaiMau());
        dto.setMoTaLoaiMau(entity.getLoaiMau().getMoTa());

        // Mẫu 1 - Thông tin bệnh nhân
        dto.setGiuongThucKe(entity.getGiuongThucKe());
        dto.setGiuongChiTieu(entity.getGiuongChiTieu());
        dto.setTongBenhCu(entity.getTongBenhCu());
        dto.setBnVaoVien(entity.getBnVaoVien());
        dto.setTongXuatVien(entity.getTongXuatVien());
        dto.setChuyenVien(entity.getChuyenVien());
        dto.setChuyenKhoa(entity.getChuyenKhoa());
        dto.setTronVien(entity.getTronVien());
        dto.setXinVe(entity.getXinVe());
        dto.setTuVong(entity.getTuVong());
        dto.setBenhHienCo(entity.getBenhHienCo());

        // Tình hình sản phụ
        dto.setSanhThuong(entity.getSanhThuong());
        dto.setSanhMo(entity.getSanhMo());
        dto.setMoPhuKhoa(entity.getMoPhuKhoa());

        // Tình hình phẫu thuật
        dto.setCapCuu(entity.getCapCuu());
        dto.setChuongTrinh(entity.getChuongTrinh());
        dto.setThuThuat(entity.getThuThuat());
        dto.setTieuPhau(entity.getTieuPhau());
        dto.setPhauThuat(entity.getPhauThuat());
        dto.setPtLoaiI(entity.getPtLoaiI());
        dto.setPtLoaiII(entity.getPtLoaiII());
        dto.setPtLoaiIII(entity.getPtLoaiIII());

        // Chăm sóc điều dưỡng
        dto.setThoCpap(entity.getThoCpap());
        dto.setThoMay(entity.getThoMay());
        dto.setThoOxy(entity.getThoOxy());
        dto.setBopBong(entity.getBopBong());
        dto.setMonitor(entity.getMonitor());
        dto.setCvp(entity.getCvp());
        dto.setNoiKhiQuan(entity.getNoiKhiQuan());
        dto.setNoiSoi(entity.getNoiSoi());
        dto.setSondeDaDay(entity.getSondeDaDay());
        dto.setSondeTieu(entity.getSondeTieu());
        dto.setHutDamNhot(entity.getHutDamNhot());

        // Phân cấp chăm sóc
        dto.setCsCapI(entity.getCsCapI());
        dto.setCsCapII(entity.getCsCapII());
        dto.setCsCapIII(entity.getCsCapIII());

        // Tình hình KCB
        dto.setTsNbKcb(entity.getTsNbKcb());
        dto.setTsNbCapCuu(entity.getTsNbCapCuu());
        dto.setNgoaiVien(entity.getNgoaiVien());
        dto.setChuyenNoiTru(entity.getChuyenNoiTru());
        dto.setChuyenCapCuu(entity.getChuyenCapCuu());
        dto.setChuyenVienKcb(entity.getChuyenVienKcb());
        dto.setChuyenPkKNgoai(entity.getChuyenPkKNgoai());
        dto.setTuVongKcb(entity.getTuVongKcb());
        dto.setTongNbDoDienTim(entity.getTongNbDoDienTim());
        dto.setTongNbDoDienCo(entity.getTongNbDoDienCo());
        dto.setTongNbDoChucNangHoHap(entity.getTongNbDoChucNangHoHap());

        // Mẫu 2 - Thông tin nhân sự
        dto.setDieuDuong(entity.getDieuDuong());
        dto.setHoSinh(entity.getHoSinh());
        dto.setKyThuatVien(entity.getKyThuatVien());
        dto.setYSi(entity.getYSi());
        dto.setNhanSuKhac(entity.getNhanSuKhac());
        dto.setHoLyNhanSu(entity.getHoLyNhanSu());
        dto.setTongNhanSu(entity.getTongNhanSu());

        // Hiện diện
        dto.setDdtKhoa(entity.getDdtKhoa());
        dto.setDdhc(entity.getDdhc());
        dto.setPhongKham(entity.getPhongKham());
        dto.setTourSang(entity.getTourSang());
        dto.setTourChieu(entity.getTourChieu());
        dto.setTourDem(entity.getTourDem());
        dto.setTruc2424(entity.getTruc2424());
        dto.setHoLyHienDien(entity.getHoLyHienDien());
        dto.setTongHienDien(entity.getTongHienDien());

        // Vắng
        dto.setRaTruc(entity.getRaTruc());
        dto.setBuTruc(entity.getBuTruc());
        dto.setNghiPhep(entity.getNghiPhep());
        dto.setNghiOm(entity.getNghiOm());
        dto.setNghiHauSan(entity.getNghiHauSan());
        dto.setNghiKhac(entity.getNghiKhac());
        dto.setDiHoc(entity.getDiHoc());
        dto.setCongTac(entity.getCongTac());
        dto.setHoLyVang(entity.getHoLyVang());
        dto.setTongVang(entity.getTongVang());

        // Đào tạo
        dto.setNhanVienThuViec(entity.getNhanVienThuViec());
        dto.setThucHanhKLuong(entity.getThucHanhKLuong());
        dto.setNhanSuTangCuong(entity.getNhanSuTangCuong());
        dto.setSvDdHs(entity.getSvDdHs());
        dto.setSvYSi(entity.getSvYSi());
        dto.setSvKtv(entity.getSvKtv());
        dto.setSvDuoc(entity.getSvDuoc());


        // Mẫu 3 - Khoa Xét nghiệm
        dto.setXnTongSoMau(entity.getXnTongSoMau());
        dto.setXnMauNgoaiTru(entity.getXnMauNgoaiTru());
        dto.setXnMauNoiTru(entity.getXnMauNoiTru());
        dto.setXnMauCapCuu(entity.getXnMauCapCuu());
        dto.setXnNbTongSo(entity.getXnNbTongSo());
        dto.setXnNbNgoaiTru(entity.getXnNbNgoaiTru());
        dto.setXnNbNoiTru(entity.getXnNbNoiTru());
        dto.setXnNbCapCuu(entity.getXnNbCapCuu());
        dto.setXnHuyetHoc(entity.getXnHuyetHoc());
        dto.setXnSinhHoa(entity.getXnSinhHoa());
        dto.setXnViSinh(entity.getXnViSinh());
        dto.setXnGiaiPhauBenh(entity.getXnGiaiPhauBenh());

// Mẫu 3 - Khoa CĐHA
        dto.setCdhaXqTongNb(entity.getCdhaXqTongNb());
        dto.setCdhaXqTongPhim(entity.getCdhaXqTongPhim());
        dto.setCdhaCTTongNb(entity.getCdhaCTTongNb());
        dto.setCdhaCTTongPhim(entity.getCdhaCTTongPhim());
        dto.setCdhaSATongNb(entity.getCdhaSATongNb());
        dto.setCdhaSATongSo(entity.getCdhaSATongSo());

        // Metadata
        dto.setGhiChu(entity.getGhiChu());
        dto.setNguoiTao(entity.getNguoiTao());
        dto.setNgayTao(entity.getNgayTao());
        dto.setNguoiCapNhat(entity.getNguoiCapNhat());
        dto.setNgayCapNhat(entity.getNgayCapNhat());
        dto.setTrangThai(entity.getTrangThai());

        // Tính toán
        dto.calculateDerivedFields();

        return dto;
    }

    // Tính toán các trường phụ
    private void calculateDerivedFields() {
        // Tổng bệnh nhân
        if (tongBenhCu != null && bnVaoVien != null) {
            tongBenhNhan = tongBenhCu + bnVaoVien;
        }

        // Tỷ lệ lấp giường
        if (giuongThucKe != null && giuongChiTieu != null && giuongChiTieu > 0) {
            tyLeLapGiuong = Math.round((giuongThucKe * 100.0f) / giuongChiTieu);
        }

        // Tổng nhân sự hiện diện (nếu có dữ liệu mẫu 2)
        if (tongHienDien != null) {
            tongNhanSuHienDien = tongHienDien;
        }

        // Tổng nhân sự vắng (nếu có dữ liệu mẫu 2)
        if (tongVang != null) {
            tongNhanSuVang = tongVang;
        }
    }
}