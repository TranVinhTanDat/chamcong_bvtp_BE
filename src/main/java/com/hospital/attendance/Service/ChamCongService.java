package com.hospital.attendance.Service;

import com.hospital.attendance.Entity.*;
import com.hospital.attendance.Repository.*;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class ChamCongService {

    @Autowired
    private ChamCongRepository chamCongRepository;

    @Autowired
    private NhanVienRepository nhanVienRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TrangThaiChamCongRepository trangThaiChamCongRepository;

    @Autowired
    private CaLamViecRepository caLamViecRepository;

    @Autowired
    private KyHieuChamCongRepository kyHieuChamCongRepository;

    /**
     * UPDATED: Loại bỏ kiểm tra trùng ca làm việc - Cho phép chấm công cùng ca nhiều lần
     */
    public ChamCong checkIn(String tenDangNhapChamCong, String nhanVienId, String nhanVienHoTen, String emailNhanVien,
                            String trangThai, String caLamViecId, String maKyHieuChamCong, String ghiChu, String filterDate) {

        // 1. Kiểm tra quyền người chấm công
        User chamCongUser = userRepository.findByTenDangNhap(tenDangNhapChamCong)
                .orElseThrow(() -> new SecurityException("Người chấm công không tồn tại"));

        String vaiTroChamCong = chamCongUser.getRole().getTenVaiTro();
        if (!vaiTroChamCong.equals("ADMIN") && !vaiTroChamCong.equals("NGUOICHAMCONG") && !vaiTroChamCong.equals("NGUOITONGHOP")) {
            throw new SecurityException("Bạn không có quyền chấm công");
        }

        // 2. Tìm nhân viên cần chấm công
        NhanVien nhanVien = timNhanVien(nhanVienId, nhanVienHoTen, emailNhanVien);

        // 3. Kiểm tra quyền chấm công cho nhân viên này
        kiemTraQuyenChamCongChoNhanVien(chamCongUser, nhanVien, vaiTroChamCong);

        // 4. Tạo khoảng thời gian dựa trên filterDate hoặc ngày hiện tại
        Date[] dateRange = getDateRange(filterDate);
        java.sql.Date startOfDay = new java.sql.Date(dateRange[0].getTime());
        java.sql.Date endOfDay = new java.sql.Date(dateRange[1].getTime());

        // 5. KIỂM TRA GIỚI HẠN 2 LẦN CHẤM CÔNG TRONG NGÀY được lọc
        Long soLanChamCongTrongNgay = chamCongRepository.countByNhanVienAndThoiGianCheckInBetween(
                nhanVien, startOfDay, endOfDay);

        if (soLanChamCongTrongNgay >= 2) {
            // Lấy thông tin chi tiết để thông báo
            List<ChamCong> danhSachChamCongTrongNgay = chamCongRepository.findByNhanVienAndDateRange(
                    nhanVien, startOfDay, endOfDay);

            String ngayHienThi;
            if (filterDate != null && !filterDate.isEmpty()) {
                ngayHienThi = "ngày " + filterDate;
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                ngayHienThi = "ngày " + sdf.format(new Date()) + " (hôm nay)";
            }

            StringBuilder thongBao = new StringBuilder("Nhân viên đã chấm công đủ 2 lần trong " + ngayHienThi + ": ");
            for (int i = 0; i < danhSachChamCongTrongNgay.size(); i++) {
                ChamCong cc = danhSachChamCongTrongNgay.get(i);
                thongBao.append("Lần ").append(i + 1).append(": ")
                        .append(cc.getTrangThaiChamCong().getTenTrangThai());
                if (cc.getCaLamViec() != null) {
                    thongBao.append(" - ").append(cc.getCaLamViec().getTenCaLamViec());
                }
                if (i < danhSachChamCongTrongNgay.size() - 1) {
                    thongBao.append(", ");
                }
            }
            throw new IllegalStateException(thongBao.toString());
        }

        // 6. *** REMOVED: Kiểm tra trùng lặp ca cụ thể ***
        // Đã loại bỏ hoàn toàn logic kiểm tra trùng ca làm việc
        // Cho phép nhân viên chấm công nhiều lần với cùng một ca trong ngày

        // 7. Tạo bản ghi chấm công mới với thời gian hiện tại hoặc thời gian được chỉ định
        return taoMoiBanGhiChamCong(nhanVien, trangThai, caLamViecId, maKyHieuChamCong, ghiChu, filterDate);
    }

    /**
     * UPDATED: Loại bỏ kiểm tra trùng ca khi cập nhật
     */
    public ChamCong capNhatTrangThai(Long id, String trangThai, String caLamViecId, String maKyHieuChamCong, String ghiChu) {
        ChamCong chamCong = chamCongRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Bản ghi chấm công với ID " + id + " không tồn tại"));

        // Kiểm tra nhân viên còn hoạt động
        NhanVien nhanVien = nhanVienRepository.findByIdAndTrangThai(chamCong.getNhanVien().getId(), 1)
                .orElseThrow(() -> new IllegalStateException("Nhân viên đã bị vô hiệu hóa"));

        // *** REMOVED: Kiểm tra trùng lặp ca cụ thể khi cập nhật ***
        // Đã loại bỏ logic kiểm tra trùng ca khi cập nhật
        // Cho phép cập nhật thành bất kỳ ca nào

        // Cập nhật bản ghi
        return capNhatBanGhiChamCong(chamCong, trangThai, caLamViecId, maKyHieuChamCong, ghiChu);
    }

    public Page<ChamCong> layLichSuChamCong(String tenDangNhap, Integer year, Integer month, Integer day, Long khoaPhongId, int page, int size) {
        User user = userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new SecurityException("Người dùng không tồn tại"));
        String role = user.getRole().getTenVaiTro();
        Long finalKhoaPhongId = khoaPhongId;

        if (role.equals("NGUOICHAMCONG") || role.equals("NGUOITONGHOP")) {
            finalKhoaPhongId = user.getKhoaPhong().getId();
        } else if (role.equals("ADMIN") && khoaPhongId == null) {
            finalKhoaPhongId = null;
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<ChamCong> chamCongs = chamCongRepository.findByKhoaPhongAndDateFilters(finalKhoaPhongId, year, month, day, pageable);
        chamCongs.forEach(chamCong -> Hibernate.initialize(chamCong.getNhanVien()));
        return chamCongs;
    }

    // ===== PRIVATE HELPER METHODS =====

    private NhanVien timNhanVien(String nhanVienId, String nhanVienHoTen, String emailNhanVien) {
        if (nhanVienId != null) {
            try {
                Long id = Long.parseLong(nhanVienId);
                return nhanVienRepository.findByIdAndTrangThai(id, 1)
                        .orElseThrow(() -> new SecurityException("Nhân viên với ID " + id + " không tồn tại hoặc đã bị vô hiệu hóa"));
            } catch (NumberFormatException e) {
                throw new SecurityException("nhanVienId phải là số hợp lệ (ID)");
            }
        } else if (nhanVienHoTen != null) {
            return nhanVienRepository.findByHoTenAndTrangThai(nhanVienHoTen, 1)
                    .orElseThrow(() -> new SecurityException("Nhân viên với họ tên '" + nhanVienHoTen + "' không tồn tại hoặc đã bị vô hiệu hóa"));
        } else if (emailNhanVien != null) {
            return nhanVienRepository.findByEmailAndTrangThai(emailNhanVien, 1)
                    .orElseThrow(() -> new SecurityException("Nhân viên với email '" + emailNhanVien + "' không tồn tại hoặc đã bị vô hiệu hóa"));
        } else {
            throw new SecurityException("Thiếu thông tin nhân viên (nhanVienId, nhanVienHoTen, hoặc emailNhanVien)");
        }
    }

    private void kiemTraQuyenChamCongChoNhanVien(User chamCongUser, NhanVien nhanVien, String vaiTroChamCong) {
        if ((vaiTroChamCong.equals("NGUOICHAMCONG") || vaiTroChamCong.equals("NGUOITONGHOP")) &&
                !chamCongUser.getKhoaPhong().getId().equals(nhanVien.getKhoaPhong().getId())) {
            throw new SecurityException("Chỉ được chấm công cho nhân viên cùng khoa/phòng");
        }
    }

    private ChamCong taoMoiBanGhiChamCong(NhanVien nhanVien, String trangThai, String caLamViecId, String maKyHieuChamCong, String ghiChu, String filterDate) {
        ChamCong chamCong = new ChamCong();
        chamCong.setNhanVien(nhanVien);

        // Nếu có filterDate, set thời gian theo ngày đó, nếu không thì dùng thời gian hiện tại
        if (filterDate != null && !filterDate.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                Date filterDateParsed = sdf.parse(filterDate);
                // Set thời gian hiện tại nhưng ngày theo filter
                Calendar cal = Calendar.getInstance();
                Calendar filterCal = Calendar.getInstance();
                filterCal.setTime(filterDateParsed);

                cal.set(Calendar.YEAR, filterCal.get(Calendar.YEAR));
                cal.set(Calendar.MONTH, filterCal.get(Calendar.MONTH));
                cal.set(Calendar.DAY_OF_MONTH, filterCal.get(Calendar.DAY_OF_MONTH));

                chamCong.setThoiGianCheckIn(cal.getTime());
            } catch (ParseException e) {
                // Nếu parse lỗi, dùng thời gian hiện tại
                chamCong.setThoiGianCheckIn(new Date());
            }
        } else {
            chamCong.setThoiGianCheckIn(new Date());
        }

        // Set trạng thái chấm công
        TrangThaiChamCong trangThaiChamCongEntity = trangThaiChamCongRepository.findByTenTrangThai(trangThai)
                .orElseThrow(() -> new IllegalStateException("Trạng thái '" + trangThai + "' không tồn tại"));
        chamCong.setTrangThaiChamCong(trangThaiChamCongEntity);

        if ("LÀM".equals(trangThai)) {
            xetCaLamViecChoTrangThaiLam(chamCong, caLamViecId);
        } else if ("NGHỈ".equals(trangThai)) {
            xetThongTinChoTrangThaiNghi(chamCong, caLamViecId, maKyHieuChamCong, ghiChu, nhanVien);
        }

        return chamCongRepository.save(chamCong);
    }

    private ChamCong capNhatBanGhiChamCong(ChamCong chamCong, String trangThai, String caLamViecId, String maKyHieuChamCong, String ghiChu) {
        // Set trạng thái chấm công
        TrangThaiChamCong trangThaiChamCongEntity = trangThaiChamCongRepository.findByTenTrangThai(trangThai)
                .orElseThrow(() -> new IllegalStateException("Trạng thái '" + trangThai + "' không tồn tại"));
        chamCong.setTrangThaiChamCong(trangThaiChamCongEntity);

        if ("LÀM".equals(trangThai)) {
            xetCaLamViecChoTrangThaiLam(chamCong, caLamViecId);
            chamCong.setGhiChu(null); // Clear ghi chú khi chuyển sang trạng thái LÀM
            chamCong.setKyHieuChamCong(null); // Clear ký hiệu chấm công riêng lẻ
        } else if ("NGHỈ".equals(trangThai)) {
            xetThongTinChoTrangThaiNghi(chamCong, caLamViecId, maKyHieuChamCong, ghiChu, chamCong.getNhanVien());
        }

        return chamCongRepository.save(chamCong);
    }

    private void xetCaLamViecChoTrangThaiLam(ChamCong chamCong, String caLamViecId) {
        if (caLamViecId == null) {
            throw new IllegalStateException("Phải cung cấp caLamViecId khi trạng thái là LÀM");
        }
        try {
            Long caId = Long.parseLong(caLamViecId);
            CaLamViec caLamViec = caLamViecRepository.findById(caId)
                    .orElseThrow(() -> new IllegalStateException("Ca làm việc với ID " + caLamViecId + " không tồn tại"));
            chamCong.setCaLamViec(caLamViec);
            chamCong.setKyHieuChamCong(caLamViec.getKyHieuChamCong());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("caLamViecId phải là số hợp lệ");
        }
    }

    private void xetThongTinChoTrangThaiNghi(ChamCong chamCong, String caLamViecId, String maKyHieuChamCong, String ghiChu, NhanVien nhanVien) {
        // UPDATED: Chỉ yêu cầu maKyHieuChamCong, ghiChu có thể null
        if (maKyHieuChamCong == null) {
            throw new IllegalStateException("Phải cung cấp maKyHieuChamCong khi trạng thái là NGHỈ");
        }

        // Bắt buộc phải có caLamViecId cho trạng thái NGHỈ
        if (caLamViecId == null) {
            throw new IllegalStateException("Phải cung cấp caLamViecId khi trạng thái là NGHỈ");
        }

        // Set ký hiệu chấm công
        KyHieuChamCong kyHieuChamCong = kyHieuChamCongRepository.findByMaKyHieu(maKyHieuChamCong)
                .orElseThrow(() -> new IllegalStateException("Ký hiệu chấm công '" + maKyHieuChamCong + "' không tồn tại"));
        if (!kyHieuChamCong.isTrangThai()) {
            throw new IllegalStateException("Ký hiệu chấm công '" + maKyHieuChamCong + "' không được sử dụng");
        }
        chamCong.setKyHieuChamCong(kyHieuChamCong);

        // UPDATED: ghiChu có thể null, không cần kiểm tra
        chamCong.setGhiChu(ghiChu); // Cho phép null

        // Set ca làm việc bắt buộc từ payload
        try {
            Long caId = Long.parseLong(caLamViecId);
            CaLamViec caLamViec = caLamViecRepository.findById(caId)
                    .orElseThrow(() -> new IllegalStateException("Ca làm việc với ID " + caLamViecId + " không tồn tại"));
            chamCong.setCaLamViec(caLamViec);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("caLamViecId phải là số hợp lệ");
        }
    }

    private Date[] getDateRange(String filterDate) {
        if (filterDate != null && !filterDate.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                Date filterDateParsed = sdf.parse(filterDate);
                return getDateRangeFromDate(filterDateParsed);
            } catch (ParseException e) {
                // Nếu parse lỗi, dùng ngày hiện tại
                return getDateRangeFromDate(new Date());
            }
        } else {
            return getDateRangeFromDate(new Date());
        }
    }

    private Date[] getDateRangeFromDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        // Start of day
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date startOfDay = cal.getTime();

        // End of day
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        Date endOfDay = cal.getTime();

        return new Date[]{startOfDay, endOfDay};
    }

    private Date getStartOfDay() {
        Date now = new Date();
        return new Date(now.getTime() - now.getTime() % (24 * 60 * 60 * 1000));
    }

    private Date getEndOfDay() {
        Date now = new Date();
        return new Date(now.getTime() - now.getTime() % (24 * 60 * 60 * 1000) + 24 * 60 * 60 * 1000 - 1);
    }

    // ===== ADDITIONAL HELPER METHODS =====

    /**
     * UPDATED: Kiểm tra trạng thái chấm công của nhân viên trong ngày được lọc
     * Loại bỏ logic kiểm tra ca đã chấm công để cho phép trùng ca
     */
    public Map<String, Object> kiemTraTrangThaiChamCongTrongNgay(String tenDangNhap, String nhanVienId, String nhanVienHoTen, String emailNhanVien, String filterDate) {
        // Kiểm tra quyền
        User chamCongUser = userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new SecurityException("Người dùng không tồn tại"));

        // Tìm nhân viên
        NhanVien nhanVien = timNhanVien(nhanVienId, nhanVienHoTen, emailNhanVien);

        // Kiểm tra quyền chấm công cho nhân viên này
        String vaiTro = chamCongUser.getRole().getTenVaiTro();
        kiemTraQuyenChamCongChoNhanVien(chamCongUser, nhanVien, vaiTro);

        // Lấy thông tin chấm công trong ngày được lọc
        Date[] dateRange = getDateRange(filterDate);
        java.sql.Date startOfDay = new java.sql.Date(dateRange[0].getTime());
        java.sql.Date endOfDay = new java.sql.Date(dateRange[1].getTime());

        Long soLanChamCong = chamCongRepository.countByNhanVienAndThoiGianCheckInBetween(
                nhanVien, startOfDay, endOfDay);

        List<ChamCong> danhSachChamCong = chamCongRepository.findByNhanVienAndDateRange(
                nhanVien, startOfDay, endOfDay);

        Map<String, Object> result = new HashMap<>();
        result.put("nhanVienId", nhanVien.getId());
        result.put("nhanVienHoTen", nhanVien.getHoTen());
        result.put("soLanChamCongTrongNgay", soLanChamCong);
        result.put("coTheCheck", soLanChamCong < 2);
        result.put("danhSachChamCong", danhSachChamCong);

        // *** UPDATED: Loại bỏ logic kiểm tra ca đã chấm công ***
        // Để trống để cho phép chấm công nhiều lần với cùng ca
        Set<Long> cacCaDaChamCong = new HashSet<>(); // Luôn trống để không chặn
        result.put("cacCaDaChamCong", cacCaDaChamCong);

        return result;
    }

    /**
     * Lấy chi tiết chấm công của nhân viên trong ngày được lọc
     */
    public List<ChamCong> layChiTietChamCongHomNay(String tenDangNhap, String nhanVienId, String nhanVienHoTen, String emailNhanVien, String filterDate) {
        // Kiểm tra quyền
        User chamCongUser = userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new SecurityException("Người dùng không tồn tại"));

        // Tìm nhân viên
        NhanVien nhanVien = timNhanVien(nhanVienId, nhanVienHoTen, emailNhanVien);

        // Kiểm tra quyền chấm công cho nhân viên này
        String vaiTro = chamCongUser.getRole().getTenVaiTro();
        kiemTraQuyenChamCongChoNhanVien(chamCongUser, nhanVien, vaiTro);

        // Lấy chi tiết chấm công trong ngày được lọc
        Date[] dateRange = getDateRange(filterDate);
        java.sql.Date startOfDay = new java.sql.Date(dateRange[0].getTime());
        java.sql.Date endOfDay = new java.sql.Date(dateRange[1].getTime());

        return chamCongRepository.findByNhanVienAndDateRange(nhanVien, startOfDay, endOfDay);
    }
}