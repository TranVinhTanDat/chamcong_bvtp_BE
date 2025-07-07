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

    @Autowired
    private KhoaPhongRepository khoaPhongRepository;


    private boolean isWithin7Days(Date checkDate) {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 23);
        today.set(Calendar.MINUTE, 59);
        today.set(Calendar.SECOND, 59);
        today.set(Calendar.MILLISECOND, 999);

        Calendar sevenDaysAgo = Calendar.getInstance();
        sevenDaysAgo.add(Calendar.DAY_OF_MONTH, -7);
        sevenDaysAgo.set(Calendar.HOUR_OF_DAY, 0);
        sevenDaysAgo.set(Calendar.MINUTE, 0);
        sevenDaysAgo.set(Calendar.SECOND, 0);
        sevenDaysAgo.set(Calendar.MILLISECOND, 0);

        return checkDate.after(sevenDaysAgo.getTime()) && !checkDate.after(today.getTime());
    }

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
    public ChamCong capNhatTrangThai(String tenDangNhap, Long id, String trangThai, String caLamViecId, String maKyHieuChamCong, String ghiChu) {
        // 1. Kiểm tra quyền người sửa
        User user = userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new SecurityException("Người dùng không tồn tại"));

        String role = user.getRole().getTenVaiTro();
        if (!role.equals("ADMIN") && !role.equals("NGUOICHAMCONG")) {
            throw new SecurityException("Bạn không có quyền sửa chấm công");
        }

        // 2. Lấy bản ghi chấm công
        ChamCong chamCong = chamCongRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Bản ghi chấm công với ID " + id + " không tồn tại"));

        // 3. Kiểm tra nhân viên còn hoạt động
        NhanVien nhanVien = nhanVienRepository.findByIdAndTrangThai(chamCong.getNhanVien().getId(), 1)
                .orElseThrow(() -> new IllegalStateException("Nhân viên đã bị vô hiệu hóa"));

        // 4. Kiểm tra quyền sửa cho nhân viên này
        if (role.equals("NGUOICHAMCONG") &&
                !user.getKhoaPhong().getId().equals(nhanVien.getKhoaPhong().getId())) {
            throw new SecurityException("Chỉ được sửa chấm công cho nhân viên cùng khoa/phòng");
        }

        // 5. Kiểm tra thời gian - NGUOICHAMCONG chỉ được sửa trong vòng 7 ngày
        if (role.equals("NGUOICHAMCONG")) {
            Date chamCongDate = chamCong.getThoiGianCheckIn();

            if (!isWithin7Days(chamCongDate)) {
                throw new SecurityException("NGUOICHAMCONG chỉ được sửa chấm công trong vòng 7 ngày gần nhất");
            }
        }

        // 6. Cập nhật bản ghi
        return capNhatBanGhiChamCong(chamCong, trangThai, caLamViecId, maKyHieuChamCong, ghiChu);
    }

    public Page<ChamCong> layLichSuChamCong(String tenDangNhap, Integer year, Integer month, Integer day, Long khoaPhongId, int page, int size) {
        User user = userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new SecurityException("Người dùng không tồn tại"));
        String role = user.getRole().getTenVaiTro();
        Long finalKhoaPhongId = khoaPhongId;

        if (role.equals("NGUOICHAMCONG") || role.equals("NGUOITONGHOP_1KP")) {
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


    public Map<String, Object> checkInBulk(String tenDangNhapChamCong, Long khoaPhongId,
                                           String trangThai, Integer shift, String caLamViecId,
                                           String maKyHieuChamCong, String ghiChu, String filterDate) {

        // 1. Kiểm tra quyền người chấm công
        User chamCongUser = userRepository.findByTenDangNhap(tenDangNhapChamCong)
                .orElseThrow(() -> new SecurityException("Người chấm công không tồn tại"));

        String vaiTroChamCong = chamCongUser.getRole().getTenVaiTro();
        if (!vaiTroChamCong.equals("ADMIN") && !vaiTroChamCong.equals("NGUOICHAMCONG") && !vaiTroChamCong.equals("NGUOITONGHOP")) {
            throw new SecurityException("Bạn không có quyền chấm công");
        }

        // 2. Kiểm tra quyền truy cập khoa phòng
        if ((vaiTroChamCong.equals("NGUOICHAMCONG") || vaiTroChamCong.equals("NGUOITONGHOP")) &&
                !chamCongUser.getKhoaPhong().getId().equals(khoaPhongId)) {
            throw new SecurityException("Chỉ được chấm công cho nhân viên cùng khoa/phòng");
        }

        // 3. Kiểm tra khoa phòng tồn tại
        if (!khoaPhongRepository.existsById(khoaPhongId)) {
            throw new IllegalStateException("Khoa phòng không tồn tại");
        }

        // 4. Lấy danh sách nhân viên trong khoa phòng (chỉ những người đang hoạt động)
        List<NhanVien> danhSachNhanVien = nhanVienRepository.findByKhoaPhongIdAndTrangThai(khoaPhongId, 1);

        if (danhSachNhanVien.isEmpty()) {
            throw new IllegalStateException("Không có nhân viên nào trong khoa phòng này");
        }

        // 5. Tạo khoảng thời gian dựa trên filterDate hoặc ngày hiện tại
        Date[] dateRange = getDateRange(filterDate);
        java.sql.Date startOfDay = new java.sql.Date(dateRange[0].getTime());
        java.sql.Date endOfDay = new java.sql.Date(dateRange[1].getTime());

        // 6. Xử lý chấm công cho từng nhân viên
        List<String> thanhCong = new ArrayList<>();
        List<String> thatBai = new ArrayList<>();

        for (NhanVien nhanVien : danhSachNhanVien) {
            try {
                // Kiểm tra đã chấm công tối đa 2 lần trong ngày chưa
                Long soLanChamCongTrongNgay = chamCongRepository.countByNhanVienAndThoiGianCheckInBetween(
                        nhanVien, startOfDay, endOfDay);

                if (soLanChamCongTrongNgay >= 2) {
                    thatBai.add(nhanVien.getHoTen() + " - Đã chấm công đủ 2 lần trong ngày");
                    continue;
                }

                // Kiểm tra xem shift này đã được chấm công chưa bằng cách đếm số lần chấm công
                List<ChamCong> danhSachChamCongTrongNgay = chamCongRepository.findByNhanVienAndDateRange(
                        nhanVien, startOfDay, endOfDay);

                // Nếu shift = 1 và đã có bản ghi nào, hoặc shift = 2 và đã có >= 2 bản ghi thì skip
                if ((shift == 1 && !danhSachChamCongTrongNgay.isEmpty()) ||
                        (shift == 2 && danhSachChamCongTrongNgay.size() >= 2)) {
                    thatBai.add(nhanVien.getHoTen() + " - Đã chấm công cho ca " + (shift == 1 ? "sáng" : "chiều"));
                    continue;
                }

                // Nếu shift = 2 nhưng chưa có shift 1, bắt buộc phải chấm shift 1 trước
                if (shift == 2 && danhSachChamCongTrongNgay.isEmpty()) {
                    thatBai.add(nhanVien.getHoTen() + " - Phải chấm công ca sáng trước khi chấm ca chiều");
                    continue;
                }

                // Tạo bản ghi chấm công
                ChamCong chamCong = taoMoiBanGhiChamCong(nhanVien, trangThai, caLamViecId,
                        maKyHieuChamCong, ghiChu, filterDate);

                thanhCong.add(nhanVien.getHoTen() + " - " + trangThai + " ca " + (shift == 1 ? "sáng" : "chiều"));

            } catch (Exception e) {
                thatBai.add(nhanVien.getHoTen() + " - Lỗi: " + e.getMessage());
            }
        }

        // 7. Tạo kết quả trả về
        Map<String, Object> result = new HashMap<>();
        result.put("tongSoNhanVien", danhSachNhanVien.size());
        result.put("soLuongThanhCong", thanhCong.size());
        result.put("soLuongThatBai", thatBai.size());
        result.put("chiTietThanhCong", thanhCong);
        result.put("chiTietThatBai", thatBai);

        String thongBaoTongKet = String.format("Chấm công hàng loạt hoàn tất: %d/%d thành công",
                thanhCong.size(), danhSachNhanVien.size());
        result.put("message", thongBaoTongKet);

        return result;
    }


    public Map<String, Object> updateBulk(String tenDangNhapChamCong, Long khoaPhongId,
                                          String trangThai, Integer shift, String caLamViecId,
                                          String maKyHieuChamCong, String ghiChu, String filterDate) {

        // 1. Kiểm tra quyền người chấm công
        User chamCongUser = userRepository.findByTenDangNhap(tenDangNhapChamCong)
                .orElseThrow(() -> new SecurityException("Người chấm công không tồn tại"));

        String vaiTroChamCong = chamCongUser.getRole().getTenVaiTro();
        if (!vaiTroChamCong.equals("ADMIN") && !vaiTroChamCong.equals("NGUOICHAMCONG")) {
            throw new SecurityException("Chỉ ADMIN và NGUOICHAMCONG mới có quyền cập nhật hàng loạt");
        }

// 1.1. Kiểm tra quyền truy cập khoa phòng cho NGUOICHAMCONG
        if (vaiTroChamCong.equals("NGUOICHAMCONG") &&
                !chamCongUser.getKhoaPhong().getId().equals(khoaPhongId)) {
            throw new SecurityException("Chỉ được cập nhật chấm công cho nhân viên cùng khoa/phòng");
        }

// 1.2. Kiểm tra thời gian cho NGUOICHAMCONG - trong vòng 7 ngày
        if (vaiTroChamCong.equals("NGUOICHAMCONG")) {
            if (filterDate == null || filterDate.isEmpty()) {
                throw new SecurityException("NGUOICHAMCONG phải cung cấp ngày cần cập nhật");
            }

            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                Date filterDateParsed = sdf.parse(filterDate);

                if (!isWithin7Days(filterDateParsed)) {
                    throw new SecurityException("NGUOICHAMCONG chỉ được sửa chấm công trong vòng 7 ngày gần nhất");
                }
            } catch (ParseException e) {
                throw new SecurityException("Định dạng ngày không hợp lệ");
            }
        }
        // 2. Kiểm tra khoa phòng tồn tại
        if (!khoaPhongRepository.existsById(khoaPhongId)) {
            throw new IllegalStateException("Khoa phòng không tồn tại");
        }

        // 3. Lấy danh sách nhân viên trong khoa phòng (chỉ những người đang hoạt động)
        List<NhanVien> danhSachNhanVien = nhanVienRepository.findByKhoaPhongIdAndTrangThai(khoaPhongId, 1);

        if (danhSachNhanVien.isEmpty()) {
            throw new IllegalStateException("Không có nhân viên nào trong khoa phòng này");
        }

        // 4. Tạo khoảng thời gian dựa trên filterDate
        Date[] dateRange = getDateRange(filterDate);
        java.sql.Date startOfDay = new java.sql.Date(dateRange[0].getTime());
        java.sql.Date endOfDay = new java.sql.Date(dateRange[1].getTime());

        // 5. Tìm và cập nhật các bản ghi chấm công
        List<String> thanhCong = new ArrayList<>();
        List<String> thatBai = new ArrayList<>();

        for (NhanVien nhanVien : danhSachNhanVien) {
            try {
                // Tìm bản ghi chấm công theo shift
                List<ChamCong> danhSachChamCongTrongNgay = chamCongRepository.findByNhanVienAndDateRange(
                        nhanVien, startOfDay, endOfDay);

                // Xác định bản ghi cần update theo shift
                ChamCong chamCongCanUpdate = null;
                if (shift == 1 && !danhSachChamCongTrongNgay.isEmpty()) {
                    chamCongCanUpdate = danhSachChamCongTrongNgay.get(0); // Bản ghi đầu tiên (ca sáng)
                } else if (shift == 2 && danhSachChamCongTrongNgay.size() >= 2) {
                    chamCongCanUpdate = danhSachChamCongTrongNgay.get(1); // Bản ghi thứ hai (ca chiều)
                }

                if (chamCongCanUpdate == null) {
                    thatBai.add(nhanVien.getHoTen() + " - Không tìm thấy bản ghi chấm công cho ca " +
                            (shift == 1 ? "sáng" : "chiều"));
                    continue;
                }

                // Cập nhật bản ghi
                capNhatBanGhiChamCong(chamCongCanUpdate, trangThai, caLamViecId, maKyHieuChamCong, ghiChu);

                thanhCong.add(nhanVien.getHoTen() + " - Cập nhật thành " + trangThai + " ca " +
                        (shift == 1 ? "sáng" : "chiều"));

            } catch (Exception e) {
                thatBai.add(nhanVien.getHoTen() + " - Lỗi: " + e.getMessage());
            }
        }

        // 6. Tạo kết quả trả về
        Map<String, Object> result = new HashMap<>();
        result.put("tongSoNhanVien", danhSachNhanVien.size());
        result.put("soLuongThanhCong", thanhCong.size());
        result.put("soLuongThatBai", thatBai.size());
        result.put("chiTietThanhCong", thanhCong);
        result.put("chiTietThatBai", thatBai);

        String thongBaoTongKet = String.format("Cập nhật hàng loạt hoàn tất: %d/%d thành công",
                thanhCong.size(), danhSachNhanVien.size());
        result.put("message", thongBaoTongKet);

        return result;
    }


    public ChamCong updateAttendanceSymbol(String tenDangNhap, Long nhanVienId,
                                           Integer day, Integer shift, Integer month,
                                           Integer year, String newSymbol) {

        // 1. Kiểm tra quyền ADMIN
        User user = userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new SecurityException("Người dùng không tồn tại"));

        if (!"ADMIN".equals(user.getRole().getTenVaiTro())) {
            throw new SecurityException("Chỉ ADMIN mới có quyền sửa ký hiệu chấm công");
        }

        // 2. Kiểm tra nhân viên tồn tại
        NhanVien nhanVien = nhanVienRepository.findByIdAndTrangThai(nhanVienId, 1)
                .orElseThrow(() -> new IllegalStateException("Nhân viên không tồn tại hoặc đã bị vô hiệu hóa"));

        // 3. Tạo khoảng thời gian cho ngày cụ thể
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, day, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date startOfDay = cal.getTime();

        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        Date endOfDay = cal.getTime();

        java.sql.Date sqlStartOfDay = new java.sql.Date(startOfDay.getTime());
        java.sql.Date sqlEndOfDay = new java.sql.Date(endOfDay.getTime());

        // 4. Lấy tất cả bản ghi chấm công trong ngày và sắp xếp theo thời gian
        List<ChamCong> existingRecords = chamCongRepository.findByNhanVienAndDateRange(
                nhanVien, sqlStartOfDay, sqlEndOfDay);

        // Sắp xếp theo thời gian check-in
        existingRecords.sort((a, b) -> {
            Date timeA = a.getThoiGianCheckIn();
            Date timeB = b.getThoiGianCheckIn();
            return timeA.compareTo(timeB);
        });

        ChamCong targetRecord = null;

        // 5. Xác định bản ghi cần cập nhật theo shift
        if (shift == 1 && existingRecords.size() >= 1) {
            targetRecord = existingRecords.get(0); // Ca sáng - bản ghi đầu tiên
        } else if (shift == 2 && existingRecords.size() >= 2) {
            targetRecord = existingRecords.get(1); // Ca chiều - bản ghi thứ hai
        }

        // 6. Xử lý theo ký hiệu mới
        if ("-".equals(newSymbol)) {
            // Xóa bản ghi nếu ký hiệu là "-"
            if (targetRecord != null) {
                chamCongRepository.delete(targetRecord);
                return null; // Trả về null để biết đã xóa
            }
            return null;
        } else {
            // Cập nhật hoặc tạo mới bản ghi
            if (targetRecord == null) {
                // Tạo mới nếu chưa có bản ghi
                targetRecord = new ChamCong();
                targetRecord.setNhanVien(nhanVien);

                // Set thời gian check-in dựa trên shift
                Calendar checkInCal = Calendar.getInstance();
                checkInCal.set(year, month - 1, day);

                if (shift == 1) {
                    checkInCal.set(Calendar.HOUR_OF_DAY, 7); // 7:00 AM cho ca sáng
                    checkInCal.set(Calendar.MINUTE, 0);
                } else {
                    checkInCal.set(Calendar.HOUR_OF_DAY, 13); // 1:00 PM cho ca chiều
                    checkInCal.set(Calendar.MINUTE, 0);
                }

                targetRecord.setThoiGianCheckIn(checkInCal.getTime());
            }

            // Cập nhật ký hiệu và trạng thái
            return updateRecordWithNewSymbol(targetRecord, newSymbol);
        }
    }

    private ChamCong updateRecordWithNewSymbol(ChamCong record, String newSymbol) {
        // Tìm ký hiệu chấm công
        Optional<KyHieuChamCong> kyHieuOpt = kyHieuChamCongRepository.findByMaKyHieu(newSymbol);

        if (kyHieuOpt.isEmpty()) {
            throw new IllegalStateException("Ký hiệu chấm công '" + newSymbol + "' không tồn tại");
        }

        KyHieuChamCong kyHieu = kyHieuOpt.get();

        if (!kyHieu.isTrangThai()) {
            throw new IllegalStateException("Ký hiệu chấm công '" + newSymbol + "' không được sử dụng");
        }

        record.setKyHieuChamCong(kyHieu);

        // Xác định trạng thái và ca làm việc dựa trên ký hiệu
        if (isWorkSymbol(newSymbol)) {
            // Trạng thái LÀM
            TrangThaiChamCong trangThaiLam = trangThaiChamCongRepository.findByTenTrangThai("LÀM")
                    .orElseThrow(() -> new IllegalStateException("Trạng thái LÀM không tồn tại"));
            record.setTrangThaiChamCong(trangThaiLam);

            // Tìm ca làm việc có ký hiệu này
            Optional<CaLamViec> caLamViecOpt = caLamViecRepository.findByKyHieuChamCong(kyHieu);
            if (caLamViecOpt.isPresent()) {
                record.setCaLamViec(caLamViecOpt.get());
            } else {
                // Nếu không tìm thấy ca cụ thể, dùng ca mặc định
                List<CaLamViec> allCa = caLamViecRepository.findAll();
                if (!allCa.isEmpty()) {
                    record.setCaLamViec(allCa.get(0)); // Ca đầu tiên làm mặc định
                }
            }

            record.setGhiChu(null);
        } else {
            // Trạng thái NGHỈ
            TrangThaiChamCong trangThaiNghi = trangThaiChamCongRepository.findByTenTrangThai("NGHỈ")
                    .orElseThrow(() -> new IllegalStateException("Trạng thái NGHỈ không tồn tại"));
            record.setTrangThaiChamCong(trangThaiNghi);

            // Đặt ca làm việc mặc định cho nghỉ
            List<CaLamViec> allCa = caLamViecRepository.findAll();
            if (!allCa.isEmpty()) {
                record.setCaLamViec(allCa.get(0));
            }

            record.setGhiChu("Cập nhật từ bảng chấm công");
        }

        return chamCongRepository.save(record);
    }

    private boolean isWorkSymbol(String symbol) {
        // Các ký hiệu làm việc
        return Arrays.asList("X", "VT", "RT", "S", "C", "T", "T12", "T16", "CT").contains(symbol);
    }
}

