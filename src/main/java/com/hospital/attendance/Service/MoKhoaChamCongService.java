package com.hospital.attendance.Service;

import com.hospital.attendance.Entity.KhoaPhong;
import com.hospital.attendance.Entity.MoKhoaChamCong;
import com.hospital.attendance.Entity.User;
import com.hospital.attendance.Repository.KhoaPhongRepository;
import com.hospital.attendance.Repository.MoKhoaChamCongRepository;
import com.hospital.attendance.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
public class MoKhoaChamCongService {

    @Autowired
    private MoKhoaChamCongRepository moKhoaChamCongRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KhoaPhongRepository khoaPhongRepository;

    public MoKhoaChamCong taoMoKhoa(String tenDangNhap, Long khoaPhongId,
                                    String tuNgayStr, String denNgayStr,
                                    String lyDo, String ghiChu) {

        // 1. Kiểm tra người tạo
        User nguoiTao = userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new IllegalStateException("Người dùng không tồn tại"));

        if (!"ADMIN".equals(nguoiTao.getRole().getTenVaiTro())) {
            throw new SecurityException("Chỉ ADMIN mới có quyền tạo mở khóa");
        }

        // 2. Kiểm tra khoa phòng
        KhoaPhong khoaPhong = khoaPhongRepository.findById(khoaPhongId)
                .orElseThrow(() -> new IllegalStateException("Khoa phòng không tồn tại"));

        // 3. Parse ngày với nhiều format
        Date tuNgay, denNgay;
        try {
            tuNgay = parseFlexibleDate(tuNgayStr);
            denNgay = parseFlexibleDate(denNgayStr);
        } catch (ParseException e) {
            throw new IllegalStateException("Định dạng ngày không hợp lệ. Sử dụng dd/MM/yyyy hoặc dd-MM-yyyy");
        }

        // 4. Kiểm tra logic ngày - CHỈ KIỂM TRA THỨ TỰ NGÀY
        if (tuNgay.after(denNgay)) {
            throw new IllegalStateException("Từ ngày không thể sau đến ngày");
        }

        // ✅ BỎ KIỂM TRA NGÀY QUÁ KHỨ - Cho phép tạo mở khóa cho bất kỳ thời gian nào
        // REMOVED: if (denNgay.before(hienTai)) { ... }

        // 5. Kiểm tra trùng lặp khoảng thời gian
        List<MoKhoaChamCong> trungLap = moKhoaChamCongRepository.findOverlappingUnlocks(
                khoaPhongId, tuNgay, denNgay);

        if (!trungLap.isEmpty()) {
            throw new IllegalStateException("Đã có mở khóa khác trong khoảng thời gian này");
        }

        // 6. Tạo mở khóa mới
        MoKhoaChamCong moKhoa = new MoKhoaChamCong();
        moKhoa.setKhoaPhong(khoaPhong);
        moKhoa.setTuNgay(tuNgay);
        moKhoa.setDenNgay(denNgay);
        moKhoa.setLyDo(lyDo);
        moKhoa.setGhiChu(ghiChu);
        moKhoa.setTrangThai(true);
        moKhoa.setNguoiTao(nguoiTao);
        moKhoa.setNgayTao(new Date());

        return moKhoaChamCongRepository.save(moKhoa);
    }

    public Page<MoKhoaChamCong> layDanhSachMoKhoa(String tenDangNhap, String role,
                                                  Long userKhoaPhongId, Long khoaPhongId,
                                                  int page, int size) {

        Pageable pageable = PageRequest.of(page, size);

        if ("ADMIN".equals(role)) {
            // ADMIN xem tất cả hoặc theo khoa phòng được chọn
            if (khoaPhongId != null) {
                return moKhoaChamCongRepository.findByKhoaPhongId(khoaPhongId, pageable);
            } else {
                return moKhoaChamCongRepository.findActiveMoKhoa(pageable);
            }
        } else {
            // NGUOICHAMCONG chỉ xem của khoa mình
            return moKhoaChamCongRepository.findByKhoaPhongId(userKhoaPhongId, pageable);
        }
    }

    public MoKhoaChamCong huyMoKhoa(String tenDangNhap, Long id, String lyDoHuy) {
        // 1. Kiểm tra người hủy
        User nguoiHuy = userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new IllegalStateException("Người dùng không tồn tại"));

        if (!"ADMIN".equals(nguoiHuy.getRole().getTenVaiTro())) {
            throw new SecurityException("Chỉ ADMIN mới có quyền hủy mở khóa");
        }

        // 2. Tìm mở khóa
        MoKhoaChamCong moKhoa = moKhoaChamCongRepository.findByIdAndTrangThai(id, true)
                .orElseThrow(() -> new IllegalStateException("Mở khóa không tồn tại hoặc đã bị hủy"));

        // 3. Hủy mở khóa
        moKhoa.setTrangThai(false);
        moKhoa.setNguoiHuy(nguoiHuy);
        moKhoa.setNgayHuy(new Date());

        if (lyDoHuy != null && !lyDoHuy.trim().isEmpty()) {
            String ghiChuCu = moKhoa.getGhiChu() != null ? moKhoa.getGhiChu() : "";
            moKhoa.setGhiChu(ghiChuCu + "\n[HỦY] " + lyDoHuy);
        }

        return moKhoaChamCongRepository.save(moKhoa);
    }

    public boolean kiemTraCoMoKhoa(Long khoaPhongId, String ngayKiemTraStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
            Date ngayKiemTra = sdf.parse(ngayKiemTraStr);

            List<MoKhoaChamCong> danhSachMoKhoa = moKhoaChamCongRepository
                    .findActiveUnlockForKhoaPhongAndDate(khoaPhongId, ngayKiemTra);

            return !danhSachMoKhoa.isEmpty();

        } catch (ParseException e) {
            throw new IllegalStateException("Định dạng ngày không hợp lệ. Sử dụng dd-MM-yyyy");
        }
    }

    public MoKhoaChamCong layChiTietMoKhoa(Long id) {
        return moKhoaChamCongRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Mở khóa không tồn tại"));
    }

    // *** THÊM METHOD QUAN TRỌNG: Kiểm tra mở khóa cho Date object ***
    public boolean kiemTraCoMoKhoa(Long khoaPhongId, Date ngayKiemTra) {
        List<MoKhoaChamCong> danhSachMoKhoa = moKhoaChamCongRepository
                .findActiveUnlockForKhoaPhongAndDate(khoaPhongId, ngayKiemTra);

        return !danhSachMoKhoa.isEmpty();
    }

    public List<MoKhoaChamCong> layDanhSachMoKhoaHienTai(Long khoaPhongId) {
        Date hienTai = new Date();
        return moKhoaChamCongRepository.findCurrentActiveUnlocks(khoaPhongId, hienTai);
    }

    // *** THÊM MỚI: Lấy danh sách mở khóa hiện đang hoạt động cho khoa phòng ***
    public List<MoKhoaChamCong> layMoKhoaHienTai(Long khoaPhongId) {
        return moKhoaChamCongRepository.findCurrentActiveUnlocksForKhoaPhong(khoaPhongId);
    }

    // *** THÊM MỚI: Gia hạn mở khóa - CẬP NHẬT để cho phép gia hạn về quá khứ ***
    public MoKhoaChamCong giaHanMoKhoa(String tenDangNhap, Long id, String denNgayMoiStr, String lyDoGiaHan) {
        User nguoiGiaHan = userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new IllegalStateException("Người dùng không tồn tại"));

        if (!"ADMIN".equals(nguoiGiaHan.getRole().getTenVaiTro())) {
            throw new SecurityException("Chỉ ADMIN mới có quyền gia hạn mở khóa");
        }

        MoKhoaChamCong moKhoa = moKhoaChamCongRepository.findByIdAndTrangThai(id, true)
                .orElseThrow(() -> new IllegalStateException("Mở khóa không tồn tại hoặc đã bị hủy"));

        try {
            Date denNgayMoi = parseFlexibleDate(denNgayMoiStr);

            // ✅ CHỈ KIỂM TRA LOGIC CƠ BẢN - BỎ KIỂM TRA QUÁ KHỨ
            // REMOVED: if (denNgayMoi.before(ngayHienTai)) { ... }
            // REMOVED: if (denNgayMoi.before(moKhoa.getDenNgay())) { ... }

            // Kiểm tra trùng lặp với khoảng thời gian mới
            List<MoKhoaChamCong> trungLap = moKhoaChamCongRepository.findOverlappingUnlocksWithExclusion(
                    moKhoa.getKhoaPhong().getId(), moKhoa.getTuNgay(), denNgayMoi, id);

            if (!trungLap.isEmpty()) {
                throw new IllegalStateException("Gia hạn này sẽ trùng với mở khóa khác");
            }

            // Cập nhật thông tin
            moKhoa.setDenNgay(denNgayMoi);
            String ghiChuCu = moKhoa.getGhiChu() != null ? moKhoa.getGhiChu() : "";
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            moKhoa.setGhiChu(ghiChuCu + "\n[GIA HẠN lúc " +
                    sdf.format(new Date()) + "] " + (lyDoGiaHan != null ? lyDoGiaHan : "Không có lý do"));

            return moKhoaChamCongRepository.save(moKhoa);

        } catch (ParseException e) {
            throw new IllegalStateException("Định dạng ngày không hợp lệ. Sử dụng dd/MM/yyyy");
        }
    }

    // *** HELPER METHOD: Parse ngày linh hoạt ***
    private Date parseFlexibleDate(String dateStr) throws ParseException {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            throw new ParseException("Ngày không được để trống", 0);
        }

        // Thử parse với format dd/MM/yyyy trước
        try {
            SimpleDateFormat sdf1 = new SimpleDateFormat("dd/MM/yyyy");
            sdf1.setLenient(false);
            return sdf1.parse(dateStr.trim());
        } catch (ParseException e1) {
            // Nếu thất bại, thử với format dd-MM-yyyy
            try {
                SimpleDateFormat sdf2 = new SimpleDateFormat("dd-MM-yyyy");
                sdf2.setLenient(false);
                return sdf2.parse(dateStr.trim());
            } catch (ParseException e2) {
                throw new ParseException("Định dạng ngày không hợp lệ: " + dateStr +
                        ". Vui lòng sử dụng dd/MM/yyyy hoặc dd-MM-yyyy", 0);
            }
        }
    }
}