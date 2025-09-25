package com.hospital.attendance.DTO;

import com.hospital.attendance.Entity.NhatKyDieuDuong;
import com.hospital.attendance.Entity.NhatKyDieuDuong.LoaiMauNhatKy;
import com.hospital.attendance.Entity.KhoaPhong;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NhatKyDieuDuongRequestDTO {

    @NotNull(message = "Ngày không được để trống")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate ngay;

    @NotNull(message = "Khoa phòng không được để trống")
    private Long khoaPhongId;

    @NotNull(message = "Loại mẫu không được để trống")
    private LoaiMauNhatKy loaiMau;

    // === THÔNG TIN BỆNH NHÂN (MẪU 1) ===
    @Min(value = 0, message = "Giường thực kê không được âm")
    private Integer giuongThucKe = 0;

    @Min(value = 0, message = "Giường chỉ tiêu không được âm")
    private Integer giuongChiTieu = 0;

    @Min(value = 0, message = "Tổng bệnh cũ không được âm")
    private Integer tongBenhCu = 0;

    @Min(value = 0, message = "BN vào viện không được âm")
    private Integer bnVaoVien = 0;

    @Min(value = 0, message = "Tổng xuất viện không được âm")
    private Integer tongXuatVien = 0;

    @Min(value = 0, message = "Chuyển viện không được âm")
    private Integer chuyenVien = 0;

    @Min(value = 0, message = "Chuyển khoa không được âm")
    private Integer chuyenKhoa = 0;

    @Min(value = 0, message = "Trốn viện không được âm")
    private Integer tronVien = 0;

    @Min(value = 0, message = "Xin về không được âm")
    private Integer xinVe = 0;

    @Min(value = 0, message = "Tử vong không được âm")
    private Integer tuVong = 0;

    @Min(value = 0, message = "Bệnh hiện có không được âm")
    private Integer benhHienCo = 0;

    // Tình hình sản phụ
    @Min(value = 0, message = "Sanh thường không được âm")
    private Integer sanhThuong = 0;

    @Min(value = 0, message = "Sanh mổ không được âm")
    private Integer sanhMo = 0;

    @Min(value = 0, message = "Mổ phụ khoa không được âm")
    private Integer moPhuKhoa = 0;

    // Tình hình phẫu thuật - thủ thuật
    @Min(value = 0, message = "Cấp cứu không được âm")
    private Integer capCuu = 0;

    @Min(value = 0, message = "Chương trình không được âm")
    private Integer chuongTrinh = 0;

    @Min(value = 0, message = "Thủ thuật không được âm")
    private Integer thuThuat = 0;

    @Min(value = 0, message = "Tiểu phẫu không được âm")
    private Integer tieuPhau = 0;

    @Min(value = 0, message = "Phẫu thuật không được âm")
    private Integer phauThuat = 0;

    @Min(value = 0, message = "PT loại I không được âm")
    private Integer ptLoaiI = 0;

    @Min(value = 0, message = "PT loại II không được âm")
    private Integer ptLoaiII = 0;

    @Min(value = 0, message = "PT loại III không được âm")
    private Integer ptLoaiIII = 0;

    // Chăm sóc điều dưỡng
    @Min(value = 0, message = "Thở CPAP không được âm")
    private Integer thoCpap = 0;

    @Min(value = 0, message = "Thở máy không được âm")
    private Integer thoMay = 0;

    @Min(value = 0, message = "Thở Oxy không được âm")
    private Integer thoOxy = 0;

    @Min(value = 0, message = "Bóp bóng không được âm")
    private Integer bopBong = 0;

    @Min(value = 0, message = "Monitor không được âm")
    private Integer monitor = 0;

    @Min(value = 0, message = "CVP không được âm")
    private Integer cvp = 0;

    @Min(value = 0, message = "Nội khí quản không được âm")
    private Integer noiKhiQuan = 0;

    @Min(value = 0, message = "Nội soi không được âm")
    private Integer noiSoi = 0;

    @Min(value = 0, message = "Sonde dạ dày không được âm")
    private Integer sondeDaDay = 0;

    @Min(value = 0, message = "Sonde tiểu không được âm")
    private Integer sondeTieu = 0;

    @Min(value = 0, message = "Hút đàm nhớt không được âm")
    private Integer hutDamNhot = 0;

    @Min(value = 0, message = "Truyền máu không được âm")
    private Integer truyenMau = 0;

    // Phân cấp chăm sóc
    @Min(value = 0, message = "Tổng CS cấp I không được âm")
    private Integer tongCsCapI = 0;

    @Min(value = 0, message = "CS cấp I mới không được âm")
    private Integer csCapIMoi = 0;

    @Min(value = 0, message = "CS cấp II không được âm")
    private Integer csCapII = 0;

    @Min(value = 0, message = "CS cấp III không được âm")
    private Integer csCapIII = 0;

    // Tình hình KCB
    @Min(value = 0, message = "TS NB KCB không được âm")
    private Integer tsNbKcb = 0;

    @Min(value = 0, message = "TS NB cấp cứu không được âm")
    private Integer tsNbCapCuu = 0;

    @Min(value = 0, message = "Ngoại viện không được âm")
    private Integer ngoaiVien = 0;

    @Min(value = 0, message = "Chuyển nội trú không được âm")
    private Integer chuyenNoiTru = 0;

    @Min(value = 0, message = "Chuyển cấp cứu không được âm")
    private Integer chuyenCapCuu = 0;

    @Min(value = 0, message = "Chuyển viện KCB không được âm")
    private Integer chuyenVienKcb = 0;

    @Min(value = 0, message = "Chuyển PK K.Ngoại không được âm")
    private Integer chuyenPkKNgoai = 0;

    @Min(value = 0, message = "Tử vong KCB không được âm")
    private Integer tuVongKcb = 0;

    @Min(value = 0, message = "Tổng NB đo điện tim không được âm")
    private Integer tongNbDoDienTim = 0;

    @Min(value = 0, message = "Tổng NB đo điện cơ không được âm")
    private Integer tongNbDoDienCo = 0;

    @Min(value = 0, message = "Tổng NB đo chức năng hô hấp không được âm")
    private Integer tongNbDoChucNangHoHap = 0;

    // === THÔNG TIN NHÂN SỰ (MẪU 2) ===
    // Tình hình nhân sự
    @Min(value = 0, message = "Điều dưỡng không được âm")
    private Integer dieuDuong = 0;

    @Min(value = 0, message = "Hộ sinh không được âm")
    private Integer hoSinh = 0;

    @Min(value = 0, message = "Kỹ thuật y không được âm")
    private Integer kyThuatY = 0;

//    @Min(value = 0, message = "Y sĩ không được âm")
//    private Integer ySi = 0;

    @Min(value = 0, message = "Nhân sự khác không được âm")
    private Integer nhanSuKhac = 0;

    @Min(value = 0, message = "Hộ lý không được âm")
    private Integer hoLyNhanSu = 0;

    @Min(value = 0, message = "Tổng nhân sự không được âm")
    private Integer tongNhanSu = 0;

    // Hiện diện
    @Min(value = 0, message = "ĐDT khoa không được âm")
    private Integer ddtKhoa = 0;

    @Min(value = 0, message = "ĐDHC không được âm")
    private Integer ddhc = 0;

    @Min(value = 0, message = "Phòng khám không được âm")
    private Integer phongKham = 0;

    @Min(value = 0, message = "Tour sáng không được âm")
    private Integer tourSang = 0;

    @Min(value = 0, message = "Tour chiều không được âm")
    private Integer tourChieu = 0;

    @Min(value = 0, message = "Tour đêm không được âm")
    private Integer tourDem = 0;

    @Min(value = 0, message = "Trực 24/24 không được âm")
    private Integer truc2424 = 0;

    @Min(value = 0, message = "Hộ lý hiện diện không được âm")
    private Integer hoLyHienDien = 0;

    @Min(value = 0, message = "Tổng hiện diện không được âm")
    private Integer tongHienDien = 0;

    // Vắng
    @Min(value = 0, message = "Ra trực không được âm")
    private Integer raTruc = 0;

    @Min(value = 0, message = "Bù trực không được âm")
    private Integer buTruc = 0;

    @Min(value = 0, message = "Nghỉ phép không được âm")
    private Integer nghiPhep = 0;

    @Min(value = 0, message = "Nghỉ ốm không được âm")
    private Integer nghiOm = 0;

    @Min(value = 0, message = "Nghỉ hậu sản không được âm")
    private Integer nghiHauSan = 0;

    @Min(value = 0, message = "Nghỉ khác không được âm")
    private Integer nghiKhac = 0;

    @Min(value = 0, message = "Đi học không được âm")
    private Integer diHoc = 0;

    @Min(value = 0, message = "Công tác không được âm")
    private Integer congTac = 0;

    @Min(value = 0, message = "Hộ lý vắng không được âm")
    private Integer hoLyVang = 0;

    @Min(value = 0, message = "Tổng vắng không được âm")
    private Integer tongVang = 0;

    // Đào tạo
    @Min(value = 0, message = "Nhân viên thử việc không được âm")
    private Integer nhanVienThuViec = 0;

    @Min(value = 0, message = "Thực hành không lương không được âm")
    private Integer thucHanhKLuong = 0;

    @Min(value = 0, message = "Nhân sự tăng cường không được âm")
    private Integer nhanSuTangCuong = 0;

    @Min(value = 0, message = "SV ĐD-HS không được âm")
    private Integer svDdHs = 0;

//    @Min(value = 0, message = "SV Y sĩ không được âm")
//    private Integer svYSi = 0;

    @Min(value = 0, message = "SV KTY không được âm")
    private Integer svKty = 0;

//    @Min(value = 0, message = "SV Dược không được âm")
//    private Integer svDuoc = 0;


    // === THÔNG TIN MẪU 3 - KHỐI CẬN LÂM SÀNG ===
// Thêm vào sau phần Mẫu 2, trước phần Ghi chú

    // Khoa Xét nghiệm - Mẫu xét nghiệm
    @Min(value = 0, message = "XN tổng số mẫu không được âm")
    private Integer xnTongSoMau = 0;

    @Min(value = 0, message = "XN mẫu ngoại trú không được âm")
    private Integer xnMauNgoaiTru = 0;

    @Min(value = 0, message = "XN mẫu nội trú không được âm")
    private Integer xnMauNoiTru = 0;

    @Min(value = 0, message = "XN mẫu cấp cứu không được âm")
    private Integer xnMauCapCuu = 0;

    // Khoa Xét nghiệm - Người bệnh
    @Min(value = 0, message = "XN NB tổng số không được âm")
    private Integer xnNbTongSo = 0;

    @Min(value = 0, message = "XN NB ngoại trú không được âm")
    private Integer xnNbNgoaiTru = 0;

    @Min(value = 0, message = "XN NB nội trú không được âm")
    private Integer xnNbNoiTru = 0;

    @Min(value = 0, message = "XN NB cấp cứu không được âm")
    private Integer xnNbCapCuu = 0;

    // Khoa Xét nghiệm - Các loại xét nghiệm
    @Min(value = 0, message = "XN huyết học không được âm")
    private Integer xnHuyetHoc = 0;

    @Min(value = 0, message = "XN sinh hóa không được âm")
    private Integer xnSinhHoa = 0;

    @Min(value = 0, message = "XN vi sinh không được âm")
    private Integer xnViSinh = 0;

    @Min(value = 0, message = "XN giải phẫu bệnh không được âm")
    private Integer xnGiaiPhauBenh = 0;

    // Khoa CĐHA - X-quang
    @Min(value = 0, message = "CĐHA X-quang tổng NB không được âm")
    private Integer cdhaXqTongNb = 0;

    @Min(value = 0, message = "CĐHA X-quang tổng phim không được âm")
    private Integer cdhaXqTongPhim = 0;

    // Khoa CĐHA - CT Scanner
    @Min(value = 0, message = "CĐHA CT có cản quang NB không được âm")
    private Integer cdhaCTCoCanQuangNb = 0;

    @Min(value = 0, message = "CĐHA CT không cản quang NB không được âm")
    private Integer cdhaCTKhongCanQuangNb = 0;

    @Min(value = 0, message = "CĐHA CT tổng phim không được âm")
    private Integer cdhaCTTongPhim = 0;

    // Khoa CĐHA - Siêu âm
    @Min(value = 0, message = "CĐHA siêu âm tổng NB không được âm")
    private Integer cdhaSATongNb = 0;

    @Min(value = 0, message = "CĐHA siêu âm tổng số không được âm")
    private Integer cdhaSATongSo = 0;

    // Ghi chú
    private String ghiChu;

    // *** UTILITY METHODS ***

    /**
     * Convert DTO to Entity
     */
    public NhatKyDieuDuong toEntity() {
        NhatKyDieuDuong entity = new NhatKyDieuDuong();

        // Basic info
        entity.setNgay(this.ngay);
        entity.setLoaiMau(this.loaiMau);
        entity.setGhiChu(this.ghiChu);

        // Khoa phòng sẽ được set trong Service
        KhoaPhong khoaPhong = new KhoaPhong();
        khoaPhong.setId(this.khoaPhongId);
        entity.setKhoaPhong(khoaPhong);

        // Mẫu 1 - Thông tin bệnh nhân
        entity.setGiuongThucKe(this.giuongThucKe);
        entity.setGiuongChiTieu(this.giuongChiTieu);
        entity.setTongBenhCu(this.tongBenhCu);
        entity.setBnVaoVien(this.bnVaoVien);
        entity.setTongXuatVien(this.tongXuatVien);
        entity.setChuyenVien(this.chuyenVien);
        entity.setChuyenKhoa(this.chuyenKhoa);
        entity.setTronVien(this.tronVien);
        entity.setXinVe(this.xinVe);
        entity.setTuVong(this.tuVong);
        entity.setBenhHienCo(this.benhHienCo);

        // Tình hình sản phụ
        entity.setSanhThuong(this.sanhThuong);
        entity.setSanhMo(this.sanhMo);
        entity.setMoPhuKhoa(this.moPhuKhoa);

        // Tình hình phẫu thuật
        entity.setCapCuu(this.capCuu);
        entity.setChuongTrinh(this.chuongTrinh);
        entity.setThuThuat(this.thuThuat);
        entity.setTieuPhau(this.tieuPhau);
        entity.setPhauThuat(this.phauThuat);
        entity.setPtLoaiI(this.ptLoaiI);
        entity.setPtLoaiII(this.ptLoaiII);
        entity.setPtLoaiIII(this.ptLoaiIII);

        // Chăm sóc điều dưỡng
        entity.setThoCpap(this.thoCpap);
        entity.setThoMay(this.thoMay);
        entity.setThoOxy(this.thoOxy);
        entity.setBopBong(this.bopBong);
        entity.setMonitor(this.monitor);
        entity.setCvp(this.cvp);
        entity.setNoiKhiQuan(this.noiKhiQuan);
        entity.setNoiSoi(this.noiSoi);
        entity.setSondeDaDay(this.sondeDaDay);
        entity.setSondeTieu(this.sondeTieu);
        entity.setHutDamNhot(this.hutDamNhot);
        entity.setTruyenMau(this.truyenMau);

        // Phân cấp chăm sóc
        entity.setTongCsCapI(this.tongCsCapI);
        entity.setCsCapIMoi(this.csCapIMoi);
        entity.setCsCapII(this.csCapII);
        entity.setCsCapIII(this.csCapIII);

        // Tình hình KCB
        entity.setTsNbKcb(this.tsNbKcb);
        entity.setTsNbCapCuu(this.tsNbCapCuu);
        entity.setNgoaiVien(this.ngoaiVien);
        entity.setChuyenNoiTru(this.chuyenNoiTru);
        entity.setChuyenCapCuu(this.chuyenCapCuu);
        entity.setChuyenVienKcb(this.chuyenVienKcb);
        entity.setChuyenPkKNgoai(this.chuyenPkKNgoai);
        entity.setTuVongKcb(this.tuVongKcb);
        entity.setTongNbDoDienTim(this.tongNbDoDienTim);
        entity.setTongNbDoDienCo(this.tongNbDoDienCo);
        entity.setTongNbDoChucNangHoHap(this.tongNbDoChucNangHoHap);

        // Mẫu 2 - Thông tin nhân sự
        entity.setDieuDuong(this.dieuDuong);
        entity.setHoSinh(this.hoSinh);
        entity.setKyThuatY(this.kyThuatY);
        entity.setNhanSuKhac(this.nhanSuKhac);
        entity.setHoLyNhanSu(this.hoLyNhanSu);
        entity.setTongNhanSu(this.tongNhanSu);

        // Hiện diện
        entity.setDdtKhoa(this.ddtKhoa);
        entity.setDdhc(this.ddhc);
        entity.setPhongKham(this.phongKham);
        entity.setTourSang(this.tourSang);
        entity.setTourChieu(this.tourChieu);
        entity.setTourDem(this.tourDem);
        entity.setTruc2424(this.truc2424);
        entity.setHoLyHienDien(this.hoLyHienDien);
        entity.setTongHienDien(this.tongHienDien);

        // Vắng
        entity.setRaTruc(this.raTruc);
        entity.setBuTruc(this.buTruc);
        entity.setNghiPhep(this.nghiPhep);
        entity.setNghiOm(this.nghiOm);
        entity.setNghiHauSan(this.nghiHauSan);
        entity.setNghiKhac(this.nghiKhac);
        entity.setDiHoc(this.diHoc);
        entity.setCongTac(this.congTac);
        entity.setHoLyVang(this.hoLyVang);
        entity.setTongVang(this.tongVang);

        // Đào tạo
        entity.setNhanVienThuViec(this.nhanVienThuViec);
        entity.setThucHanhKLuong(this.thucHanhKLuong);
        entity.setNhanSuTangCuong(this.nhanSuTangCuong);
        entity.setSvDdHs(this.svDdHs);
        entity.setSvKty(this.svKty);


        // Mẫu 3 - Khoa Xét nghiệm
        entity.setXnTongSoMau(this.xnTongSoMau);
        entity.setXnMauNgoaiTru(this.xnMauNgoaiTru);
        entity.setXnMauNoiTru(this.xnMauNoiTru);
        entity.setXnMauCapCuu(this.xnMauCapCuu);
        entity.setXnNbTongSo(this.xnNbTongSo);
        entity.setXnNbNgoaiTru(this.xnNbNgoaiTru);
        entity.setXnNbNoiTru(this.xnNbNoiTru);
        entity.setXnNbCapCuu(this.xnNbCapCuu);
        entity.setXnHuyetHoc(this.xnHuyetHoc);
        entity.setXnSinhHoa(this.xnSinhHoa);
        entity.setXnViSinh(this.xnViSinh);
        entity.setXnGiaiPhauBenh(this.xnGiaiPhauBenh);

// Mẫu 3 - Khoa CĐHA
        entity.setCdhaXqTongNb(this.cdhaXqTongNb);
        entity.setCdhaXqTongPhim(this.cdhaXqTongPhim);
        entity.setCdhaCTCoCanQuangNb(this.cdhaCTCoCanQuangNb);
        entity.setCdhaCTKhongCanQuangNb(this.cdhaCTKhongCanQuangNb);
        entity.setCdhaCTTongPhim(this.cdhaCTTongPhim);
        entity.setCdhaSATongNb(this.cdhaSATongNb);
        entity.setCdhaSATongSo(this.cdhaSATongSo);

        return entity;
    }

    /**
     * Validate business logic
     */
    public boolean isValid() {
        // Kiểm tra logic nghiệp vụ
        if (loaiMau == LoaiMauNhatKy.MAU_1) {
            // Mẫu 1: Giường thực kê không nên vượt quá giường chỉ tiêu quá nhiều
            if (giuongThucKe != null && giuongChiTieu != null && giuongThucKe > giuongChiTieu * 1.2) {
                return false; // Vượt quá 120% chỉ tiêu
            }
        } else if (loaiMau == LoaiMauNhatKy.MAU_2) {
            // Mẫu 2: Tổng hiện diện + tổng vắng không nên vượt quá tổng nhân sự quá nhiều
            if (tongHienDien != null && tongVang != null && tongNhanSu != null) {
                int total = tongHienDien + tongVang;
                if (total > tongNhanSu * 1.1) { // Cho phép 10% sai lệch
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Tạo template trống cho loại mẫu
     */
    public static NhatKyDieuDuongRequestDTO createEmptyTemplate(LoaiMauNhatKy loaiMau, Long khoaPhongId) {
        NhatKyDieuDuongRequestDTO template = new NhatKyDieuDuongRequestDTO();
        template.setLoaiMau(loaiMau);
        template.setKhoaPhongId(khoaPhongId);
        template.setNgay(LocalDate.now());

        // Tất cả các trường số đã có default value = 0
        return template;
    }

    /**
     * Copy từ Entity sang DTO (để edit)
     */
    public static NhatKyDieuDuongRequestDTO fromEntity(NhatKyDieuDuong entity) {
        NhatKyDieuDuongRequestDTO dto = new NhatKyDieuDuongRequestDTO();

        // Basic info
        dto.setNgay(entity.getNgay());
        dto.setKhoaPhongId(entity.getKhoaPhong().getId());
        dto.setLoaiMau(entity.getLoaiMau());
        dto.setGhiChu(entity.getGhiChu());

        // Copy tất cả các trường dữ liệu
        // Mẫu 1
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

        dto.setSanhThuong(entity.getSanhThuong());
        dto.setSanhMo(entity.getSanhMo());
        dto.setMoPhuKhoa(entity.getMoPhuKhoa());

        dto.setCapCuu(entity.getCapCuu());
        dto.setChuongTrinh(entity.getChuongTrinh());
        dto.setThuThuat(entity.getThuThuat());
        dto.setTieuPhau(entity.getTieuPhau());
        dto.setPhauThuat(entity.getPhauThuat());
        dto.setPtLoaiI(entity.getPtLoaiI());
        dto.setPtLoaiII(entity.getPtLoaiII());
        dto.setPtLoaiIII(entity.getPtLoaiIII());

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
        dto.setTruyenMau(entity.getTruyenMau());

        dto.setTongCsCapI(entity.getTongCsCapI());
        dto.setCsCapIMoi(entity.getCsCapIMoi());
        dto.setCsCapII(entity.getCsCapII());
        dto.setCsCapIII(entity.getCsCapIII());

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

        // Mẫu 2
        dto.setDieuDuong(entity.getDieuDuong());
        dto.setHoSinh(entity.getHoSinh());
        dto.setKyThuatY(entity.getKyThuatY());
        dto.setNhanSuKhac(entity.getNhanSuKhac());
        dto.setHoLyNhanSu(entity.getHoLyNhanSu());
        dto.setTongNhanSu(entity.getTongNhanSu());

        dto.setDdtKhoa(entity.getDdtKhoa());
        dto.setDdhc(entity.getDdhc());
        dto.setPhongKham(entity.getPhongKham());
        dto.setTourSang(entity.getTourSang());
        dto.setTourChieu(entity.getTourChieu());
        dto.setTourDem(entity.getTourDem());
        dto.setTruc2424(entity.getTruc2424());
        dto.setHoLyHienDien(entity.getHoLyHienDien());
        dto.setTongHienDien(entity.getTongHienDien());

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

        dto.setNhanVienThuViec(entity.getNhanVienThuViec());
        dto.setThucHanhKLuong(entity.getThucHanhKLuong());
        dto.setNhanSuTangCuong(entity.getNhanSuTangCuong());
        dto.setSvDdHs(entity.getSvDdHs());
        dto.setSvKty(entity.getSvKty());

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
        dto.setCdhaCTCoCanQuangNb(entity.getCdhaCTCoCanQuangNb());
        dto.setCdhaCTKhongCanQuangNb(entity.getCdhaCTKhongCanQuangNb());
        dto.setCdhaCTTongPhim(entity.getCdhaCTTongPhim());
        dto.setCdhaSATongNb(entity.getCdhaSATongNb());
        dto.setCdhaSATongSo(entity.getCdhaSATongSo());

        return dto;
    }
}