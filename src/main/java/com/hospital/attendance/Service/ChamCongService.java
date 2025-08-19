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

    @Autowired
    private MoKhoaChamCongService moKhoaChamCongService;


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
                            String trangThai, String caLamViecId, String maKyHieuChamCong, String ghiChu, String filterDate, Integer shift) {

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

        // 6. *** THÊM MỚI: Kiểm tra trùng shift trong ngày (nếu có shift) ***
        if (shift != null) {
            List<ChamCong> existingRecords = chamCongRepository.findByNhanVienAndDateRange(nhanVien, startOfDay, endOfDay);
            for (ChamCong existing : existingRecords) {
                if (shift.equals(existing.getShift())) {
                    String caName = shift == 1 ? "sáng" : "chiều";
                    throw new IllegalStateException("Nhân viên đã chấm công ca " + caName + " trong ngày này");
                }
            }
        }

        // 7. Tạo bản ghi chấm công mới với shift
        return taoMoiBanGhiChamCong(nhanVien, trangThai, caLamViecId, maKyHieuChamCong, ghiChu, filterDate, shift);
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

        // 5. Kiểm tra thời gian - SỬ DỤNG LOGIC MỞ KHÓA MỚI
        Date chamCongDate = chamCong.getThoiGianCheckIn();
        if (!isAllowedToEdit(chamCongDate, nhanVien.getKhoaPhong().getId(), role)) {
            if (role.equals("NGUOICHAMCONG")) {
                throw new SecurityException("NGUOICHAMCONG chỉ được sửa chấm công trong vòng 7 ngày gần nhất hoặc trong thời gian được mở khóa bởi ADMIN");
            } else {
                throw new SecurityException("Không được phép sửa chấm công trong thời gian này");
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

    // CẬP NHẬT method taoMoiBanGhiChamCong trong ChamCongService
    private ChamCong taoMoiBanGhiChamCong(NhanVien nhanVien, String trangThai, String caLamViecId, String maKyHieuChamCong, String ghiChu, String filterDate, Integer shift) {
        ChamCong chamCong = new ChamCong();
        chamCong.setNhanVien(nhanVien);

        // *** THÊM MỚI: Lưu shift ***
        chamCong.setShift(shift);

        // Nếu có filterDate, set thời gian theo ngày đó, nếu không thì dùng thời gian hiện tại
        if (filterDate != null && !filterDate.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                Date filterDateParsed = sdf.parse(filterDate);
                Calendar cal = Calendar.getInstance();
                cal.setTime(filterDateParsed);

                // *** THÊM MỚI: SET THỜI GIAN DỰA TRÊN SHIFT ***
                if (shift != null) {
                    if (shift == 1) {
                        cal.set(Calendar.HOUR_OF_DAY, 7);  // Ca sáng: 7:00 AM
                        cal.set(Calendar.MINUTE, 0);
                    } else if (shift == 2) {
                        cal.set(Calendar.HOUR_OF_DAY, 13); // Ca chiều: 1:00 PM
                        cal.set(Calendar.MINUTE, 0);
                    }
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                } else {
                    // Fallback: Dùng thời gian hiện tại nhưng với ngày từ filter
                    Calendar currentTime = Calendar.getInstance();
                    cal.set(Calendar.HOUR_OF_DAY, currentTime.get(Calendar.HOUR_OF_DAY));
                    cal.set(Calendar.MINUTE, currentTime.get(Calendar.MINUTE));
                    cal.set(Calendar.SECOND, currentTime.get(Calendar.SECOND));
                }

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
            // CẬP NHẬT: Truyền thêm ghiChu cho method xetCaLamViecChoTrangThaiLam
            xetCaLamViecChoTrangThaiLam(chamCong, caLamViecId, ghiChu);
        } else if ("NGHỈ".equals(trangThai)) {
            xetThongTinChoTrangThaiNghi(chamCong, caLamViecId, maKyHieuChamCong, ghiChu, nhanVien);
        }

        return chamCongRepository.save(chamCong);
    }

    // CẬP NHẬT method capNhatBanGhiChamCong trong ChamCongService
    private ChamCong capNhatBanGhiChamCong(ChamCong chamCong, String trangThai, String caLamViecId, String maKyHieuChamCong, String ghiChu) {
        // Set trạng thái chấm công
        TrangThaiChamCong trangThaiChamCongEntity = trangThaiChamCongRepository.findByTenTrangThai(trangThai)
                .orElseThrow(() -> new IllegalStateException("Trạng thái '" + trangThai + "' không tồn tại"));
        chamCong.setTrangThaiChamCong(trangThaiChamCongEntity);

        if ("LÀM".equals(trangThai)) {
            // CẬP NHẬT: Truyền thêm ghiChu cho method xetCaLamViecChoTrangThaiLam
            xetCaLamViecChoTrangThaiLam(chamCong, caLamViecId, ghiChu);
            chamCong.setKyHieuChamCong(null); // Clear ký hiệu chấm công riêng lẻ cho trạng thái LÀM
        } else if ("NGHỈ".equals(trangThai)) {
            xetThongTinChoTrangThaiNghi(chamCong, caLamViecId, maKyHieuChamCong, ghiChu, chamCong.getNhanVien());
        }

        return chamCongRepository.save(chamCong);
    }

    private void xetCaLamViecChoTrangThaiLam(ChamCong chamCong, String caLamViecId, String ghiChu) {
        if (caLamViecId == null) {
            throw new IllegalStateException("Phải cung cấp caLamViecId khi trạng thái là LÀM");
        }
        try {
            Long caId = Long.parseLong(caLamViecId);
            CaLamViec caLamViec = caLamViecRepository.findById(caId)
                    .orElseThrow(() -> new IllegalStateException("Ca làm việc với ID " + caLamViecId + " không tồn tại"));

            chamCong.setCaLamViec(caLamViec);
            chamCong.setKyHieuChamCong(caLamViec.getKyHieuChamCong());

            // THÊM MỚI: Kiểm tra nếu là ca công tác/CSSKCBĐDL (ID = 9) thì cho phép ghi chú
            if (caId == 9L) {
                // Ca công tác/CSSKCBĐDL - cho phép ghi chú (có thể null)
                chamCong.setGhiChu(ghiChu); // Có thể null, không ép buộc
            } else {
                // Các ca khác - xóa ghi chú để tránh nhầm lẫn
                chamCong.setGhiChu(null);
            }

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

                // *** SỬA ĐỔI: Kiểm tra trùng shift thay vì theo thứ tự ***
                List<ChamCong> danhSachChamCongTrongNgay = chamCongRepository.findByNhanVienAndDateRange(
                        nhanVien, startOfDay, endOfDay);

                // Kiểm tra xem shift này đã được chấm công chưa
                boolean shiftExists = danhSachChamCongTrongNgay.stream()
                        .anyMatch(cc -> shift.equals(cc.getShift()));

                if (shiftExists) {
                    thatBai.add(nhanVien.getHoTen() + " - Đã chấm công cho ca " + (shift == 1 ? "sáng" : "chiều"));
                    continue;
                }

                // *** SỬA ĐỔI: Truyền thêm tham số shift vào method taoMoiBanGhiChamCong ***
                ChamCong chamCong = taoMoiBanGhiChamCong(nhanVien, trangThai, caLamViecId,
                        maKyHieuChamCong, ghiChu, filterDate, shift);

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

        // 1.2. Kiểm tra thời gian cho NGUOICHAMCONG - SỬ DỤNG LOGIC MỞ KHÓA MỚI
        if (vaiTroChamCong.equals("NGUOICHAMCONG")) {
            if (filterDate == null || filterDate.isEmpty()) {
                throw new SecurityException("NGUOICHAMCONG phải cung cấp ngày cần cập nhật");
            }

            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                Date filterDateParsed = sdf.parse(filterDate);

                if (!isAllowedToEdit(filterDateParsed, khoaPhongId, vaiTroChamCong)) {
                    throw new SecurityException("NGUOICHAMCONG chỉ được sửa chấm công trong vòng 7 ngày gần nhất hoặc trong thời gian được mở khóa bởi ADMIN");
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


    public void xoaChamCong(String tenDangNhap, Long id) {
        // 1. Kiểm tra quyền người xóa
        User user = userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new SecurityException("Người dùng không tồn tại"));

        String role = user.getRole().getTenVaiTro();
        if (!role.equals("ADMIN") && !role.equals("NGUOICHAMCONG")) {
            throw new SecurityException("Bạn không có quyền xóa chấm công");
        }

        // 2. Lấy bản ghi chấm công
        ChamCong chamCong = chamCongRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Bản ghi chấm công với ID " + id + " không tồn tại"));

        // 3. Kiểm tra nhân viên còn hoạt động
        NhanVien nhanVien = nhanVienRepository.findByIdAndTrangThai(chamCong.getNhanVien().getId(), 1)
                .orElseThrow(() -> new IllegalStateException("Nhân viên đã bị vô hiệu hóa"));

        // 4. Kiểm tra quyền xóa cho nhân viên này
        if (role.equals("NGUOICHAMCONG") &&
                !user.getKhoaPhong().getId().equals(nhanVien.getKhoaPhong().getId())) {
            throw new SecurityException("Chỉ được xóa chấm công cho nhân viên cùng khoa/phòng");
        }

        // 5. Kiểm tra thời gian - SỬ DỤNG LOGIC MỞ KHÓA MỚI
        Date chamCongDate = chamCong.getThoiGianCheckIn();
        if (!isAllowedToEdit(chamCongDate, nhanVien.getKhoaPhong().getId(), role)) {
            if (role.equals("NGUOICHAMCONG")) {
                throw new SecurityException("NGUOICHAMCONG chỉ được xóa chấm công trong vòng 7 ngày gần nhất hoặc trong thời gian được mở khóa bởi ADMIN");
            } else {
                throw new SecurityException("Không được phép xóa chấm công trong thời gian này");
            }
        }

        // 6. Xóa bản ghi
        chamCongRepository.delete(chamCong);
    }


    public Map<String, Object> xoaChamCongHangLoat(String tenDangNhap, Long khoaPhongId, Integer shift, String filterDate) {
        // 1. Kiểm tra quyền người xóa
        User user = userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new SecurityException("Người dùng không tồn tại"));

        String role = user.getRole().getTenVaiTro();
        if (!role.equals("ADMIN") && !role.equals("NGUOICHAMCONG")) {
            throw new SecurityException("Bạn không có quyền xóa chấm công");
        }

        // 2. Kiểm tra quyền truy cập khoa phòng cho NGUOICHAMCONG
        if (role.equals("NGUOICHAMCONG") &&
                !user.getKhoaPhong().getId().equals(khoaPhongId)) {
            throw new SecurityException("Chỉ được xóa chấm công cho nhân viên cùng khoa/phòng");
        }

        // 3. Kiểm tra thời gian cho NGUOICHAMCONG - SỬ DỤNG LOGIC MỞ KHÓA MỚI
        if (role.equals("NGUOICHAMCONG")) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                Date filterDateParsed = sdf.parse(filterDate);

                if (!isAllowedToEdit(filterDateParsed, khoaPhongId, role)) {
                    throw new SecurityException("NGUOICHAMCONG chỉ được xóa chấm công trong vòng 7 ngày gần nhất hoặc trong thời gian được mở khóa bởi ADMIN");
                }
            } catch (ParseException e) {
                throw new SecurityException("Định dạng ngày không hợp lệ");
            }
        }

        // 4. Kiểm tra khoa phòng tồn tại
        if (!khoaPhongRepository.existsById(khoaPhongId)) {
            throw new IllegalStateException("Khoa phòng không tồn tại");
        }

        // 5. Lấy danh sách nhân viên trong khoa phòng (chỉ những người đang hoạt động)
        List<NhanVien> danhSachNhanVien = nhanVienRepository.findByKhoaPhongIdAndTrangThai(khoaPhongId, 1);

        if (danhSachNhanVien.isEmpty()) {
            throw new IllegalStateException("Không có nhân viên nào trong khoa phòng này");
        }

        // 6. Tạo khoảng thời gian dựa trên filterDate
        Date[] dateRange = getDateRange(filterDate);
        java.sql.Date startOfDay = new java.sql.Date(dateRange[0].getTime());
        java.sql.Date endOfDay = new java.sql.Date(dateRange[1].getTime());

        // 7. LOGIC ĐƠN GIẢN: Xóa theo index giống checkInBulk
        List<String> thanhCong = new ArrayList<>();
        List<String> thatBai = new ArrayList<>();

        for (NhanVien nhanVien : danhSachNhanVien) {
            try {
                // Lấy tất cả bản ghi chấm công trong ngày, sắp xếp theo thời gian
                List<ChamCong> danhSachChamCongTrongNgay = chamCongRepository.findByNhanVienAndDateRange(
                        nhanVien, startOfDay, endOfDay);

                if (danhSachChamCongTrongNgay.isEmpty()) {
                    thatBai.add(nhanVien.getHoTen() + " - Không có bản ghi chấm công nào trong ngày");
                    continue;
                }

                // *** LOGIC ĐƠN GIẢN GIỐNG checkInBulk - Xóa theo index ***
                ChamCong recordToDelete = null;

                if (shift == 1 && !danhSachChamCongTrongNgay.isEmpty()) {
                    // Ca sáng: Xóa bản ghi đầu tiên
                    recordToDelete = danhSachChamCongTrongNgay.get(0);
                } else if (shift == 2) {
                    if (danhSachChamCongTrongNgay.size() >= 2) {
                        // Ca chiều: Xóa bản ghi thứ hai (nếu có)
                        recordToDelete = danhSachChamCongTrongNgay.get(1);
                    } else if (danhSachChamCongTrongNgay.size() == 1) {
                        // Chỉ có 1 bản ghi: Xóa luôn (coi như ca chiều)
                        recordToDelete = danhSachChamCongTrongNgay.get(0);
                    }
                }

                if (recordToDelete != null) {
                    // Thực hiện xóa
                    chamCongRepository.delete(recordToDelete);

                    String caInfo = recordToDelete.getCaLamViec() != null ?
                            recordToDelete.getCaLamViec().getTenCaLamViec() : "N/A";

                    thanhCong.add(nhanVien.getHoTen() + " - Đã xóa chấm công ca " +
                            (shift == 1 ? "sáng" : "chiều") +
                            " (ID: " + recordToDelete.getId() + ", Ca: " + caInfo + ")");
                } else {
                    thatBai.add(nhanVien.getHoTen() + " - Không có bản ghi để xóa cho ca " +
                            (shift == 1 ? "sáng" : "chiều"));
                }

            } catch (Exception e) {
                thatBai.add(nhanVien.getHoTen() + " - Lỗi: " + e.getMessage());
            }
        }

        // 8. Tạo kết quả trả về
        Map<String, Object> result = new HashMap<>();
        result.put("tongSoNhanVien", danhSachNhanVien.size());
        result.put("soLuongThanhCong", thanhCong.size());
        result.put("soLuongThatBai", thatBai.size());
        result.put("chiTietThanhCong", thanhCong);
        result.put("chiTietThatBai", thatBai);

        String thongBaoTongKet = String.format("Xóa chấm công hàng loạt hoàn tất: %d/%d thành công",
                thanhCong.size(), danhSachNhanVien.size());
        result.put("message", thongBaoTongKet);

        return result;
    }

    /**
     * *** THÊM METHOD MỚI: Tìm bản ghi tốt nhất cho shift được chọn ***
     */
    private ChamCong findBestMatchForShift(List<ChamCong> records, Integer shift) {
        if (records.isEmpty()) {
            return null;
        }

        // Sắp xếp theo thời gian (cũ nhất đầu tiên)
        records.sort((a, b) -> a.getThoiGianCheckIn().compareTo(b.getThoiGianCheckIn()));

        if (shift == 1) {
            // *** XÓA CA SÁNG: Áp dụng nhiều tiêu chí ưu tiên ***

            // Tiêu chí 1: Ca Sáng chính thức (ID = 11)
            for (ChamCong cc : records) {
                if (cc.getCaLamViec() != null && cc.getCaLamViec().getId() == 11L) {
                    System.out.println("🌅 [Ca Sáng] Tìm thấy ca chính thức ID=11: " + cc.getCaLamViec().getTenCaLamViec());
                    return cc;
                }
            }

            // Tiêu chí 2: Ca có tên chứa "sáng"
            for (ChamCong cc : records) {
                if (cc.getCaLamViec() != null &&
                        cc.getCaLamViec().getTenCaLamViec() != null &&
                        cc.getCaLamViec().getTenCaLamViec().toLowerCase().contains("sáng")) {
                    System.out.println("🌅 [Ca Sáng] Tìm thấy ca tên chứa 'sáng': " + cc.getCaLamViec().getTenCaLamViec());
                    return cc;
                }
            }

            // Tiêu chí 3: Ca theo thời gian (trước 12h trưa)
            for (ChamCong cc : records) {
                if (isMorningRecord(cc)) {
                    System.out.println("🌅 [Ca Sáng] Tìm thấy ca theo thời gian sáng: " +
                            (cc.getCaLamViec() != null ? cc.getCaLamViec().getTenCaLamViec() : "N/A"));
                    return cc;
                }
            }

            // Tiêu chí 4: Fallback - bản ghi đầu tiên
            ChamCong firstRecord = records.get(0);
            System.out.println("🌅 [Ca Sáng] Fallback - lấy bản ghi đầu tiên: " +
                    (firstRecord.getCaLamViec() != null ? firstRecord.getCaLamViec().getTenCaLamViec() : "N/A"));
            return firstRecord;

        } else if (shift == 2) {
            // *** XÓA CA CHIỀU: Áp dụng nhiều tiêu chí ưu tiên ***

            // Tiêu chí 1: Ca Chiều chính thức (ID = 12)
            for (ChamCong cc : records) {
                if (cc.getCaLamViec() != null && cc.getCaLamViec().getId() == 12L) {
                    System.out.println("🌆 [Ca Chiều] Tìm thấy ca chính thức ID=12: " + cc.getCaLamViec().getTenCaLamViec());
                    return cc;
                }
            }

            // Tiêu chí 2: Ca có tên chứa "chiều"
            for (ChamCong cc : records) {
                if (cc.getCaLamViec() != null &&
                        cc.getCaLamViec().getTenCaLamViec() != null &&
                        cc.getCaLamViec().getTenCaLamViec().toLowerCase().contains("chiều")) {
                    System.out.println("🌆 [Ca Chiều] Tìm thấy ca tên chứa 'chiều': " + cc.getCaLamViec().getTenCaLamViec());
                    return cc;
                }
            }

            // Tiêu chí 3: Ca theo thời gian (sau 12h trưa)
            for (ChamCong cc : records) {
                if (isAfternoonRecord(cc)) {
                    System.out.println("🌆 [Ca Chiều] Tìm thấy ca theo thời gian chiều: " +
                            (cc.getCaLamViec() != null ? cc.getCaLamViec().getTenCaLamViec() : "N/A"));
                    return cc;
                }
            }

            // Tiêu chí 4: Bản ghi thứ 2 nếu có >= 2 bản ghi
            if (records.size() >= 2) {
                ChamCong secondRecord = records.get(1);
                System.out.println("🌆 [Ca Chiều] Lấy bản ghi thứ 2: " +
                        (secondRecord.getCaLamViec() != null ? secondRecord.getCaLamViec().getTenCaLamViec() : "N/A"));
                return secondRecord;
            }

            // Tiêu chí 5: Nếu chỉ có 1 bản ghi và không phải ca sáng rõ ràng
            if (records.size() == 1) {
                ChamCong onlyRecord = records.get(0);

                // Kiểm tra xem có phải ca sáng rõ ràng không
                boolean isClearMorning = (onlyRecord.getCaLamViec() != null &&
                        (onlyRecord.getCaLamViec().getId() == 11L ||
                                (onlyRecord.getCaLamViec().getTenCaLamViec() != null &&
                                        onlyRecord.getCaLamViec().getTenCaLamViec().toLowerCase().contains("sáng"))));

                if (!isClearMorning) {
                    System.out.println("🌆 [Ca Chiều] Lấy bản ghi duy nhất (không phải ca sáng rõ ràng): " +
                            (onlyRecord.getCaLamViec() != null ? onlyRecord.getCaLamViec().getTenCaLamViec() : "N/A"));
                    return onlyRecord;
                }
            }

            System.out.println("🌆 [Ca Chiều] Không tìm thấy bản ghi phù hợp");
            return null;
        }

        return null;
    }

    /**
     * *** THÊM METHOD MỚI: Kiểm tra bản ghi có phải ca sáng theo thời gian ***
     */
    private boolean isMorningRecord(ChamCong chamCong) {
        try {
            String timeStr = chamCong.getThoiGianCheckIn().toString();
            if (timeStr.contains(" ")) {
                String timePart = timeStr.split(" ")[1];
                if (timePart != null && timePart.contains(":")) {
                    int hour = Integer.parseInt(timePart.split(":")[0]);
                    return hour < 12; // Trước 12h trưa = ca sáng
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi parse thời gian: " + e.getMessage());
        }
        return false; // Mặc định không phải ca sáng
    }

    // *** THÊM HELPER METHOD MỚI để xác định ca chiều ***
    private boolean isAfternoonRecord(ChamCong chamCong) {
        // Kiểm tra theo ca làm việc trước
        if (chamCong.getCaLamViec() != null) {
            Long caId = chamCong.getCaLamViec().getId();
            String tenCa = chamCong.getCaLamViec().getTenCaLamViec();

            // Ca Chiều có ID = 12 hoặc tên chứa "chiều"
            if (caId == 12 || (tenCa != null && tenCa.toLowerCase().contains("chiều"))) {
                return true;
            }

            // Ca Sáng có ID = 11 hoặc tên chứa "sáng"
            if (caId == 11 || (tenCa != null && tenCa.toLowerCase().contains("sáng"))) {
                return false;
            }
        }

        // Fallback: Kiểm tra theo thời gian (sau 12h trưa = ca chiều)
        try {
            String timeStr = chamCong.getThoiGianCheckIn().toString();
            if (timeStr.contains(" ")) {
                String timePart = timeStr.split(" ")[1];
                if (timePart != null && timePart.contains(":")) {
                    int hour = Integer.parseInt(timePart.split(":")[0]);
                    return hour >= 12; // Sau 12h trưa = ca chiều
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi parse thời gian: " + e.getMessage());
        }

        // Mặc định: coi là ca sáng
        return false;
    }

    // *** HELPER METHOD CŨ (giữ nguyên) ***
    private Date parseDate(Date sqlDate) {
        return sqlDate != null ? sqlDate : new Date();
    }

    // THÊM METHOD MỚI: Kiểm tra có được phép sửa chấm công không (bao gồm mở khóa)
    private boolean isAllowedToEdit(Date checkDate, Long khoaPhongId, String role) {
        // ADMIN luôn được phép
        if ("ADMIN".equals(role)) {
            return true;
        }

        // NGUOICHAMCONG: Kiểm tra trong vòng 7 ngày HOẶC có mở khóa
        if ("NGUOICHAMCONG".equals(role)) {
            // Trước tiên kiểm tra trong vòng 7 ngày
            if (isWithin7Days(checkDate)) {
                return true;
            }

            // Nếu quá 7 ngày, kiểm tra có mở khóa không
            return moKhoaChamCongService.kiemTraCoMoKhoa(khoaPhongId, checkDate);
        }

        return false;
    }


    // THÊM VÀO ChamCongService.java

    /**
     * Chấm công cho 1 nhân viên cụ thể trong khoảng thời gian từ ngày này đến ngày khác
     */
    public Map<String, Object> checkInRange(String tenDangNhapChamCong, String nhanVienId, String nhanVienHoTen,
                                            String emailNhanVien, String tuNgay, String denNgay, String trangThai,
                                            List<Integer> shifts, String caLamViecId, String maKyHieuChamCong, String ghiChu) {

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

        // 4. Parse và validate ngày tháng
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        Date tuNgayDate, denNgayDate;

        try {
            tuNgayDate = sdf.parse(tuNgay);
            denNgayDate = sdf.parse(denNgay);
        } catch (ParseException e) {
            throw new IllegalStateException("Định dạng ngày không hợp lệ. Vui lòng sử dụng dd-MM-yyyy");
        }

        // Kiểm tra tuNgay <= denNgay
        if (tuNgayDate.after(denNgayDate)) {
            throw new IllegalStateException("Ngày bắt đầu phải nhỏ hơn hoặc bằng ngày kết thúc");
        }

        // Kiểm tra khoảng thời gian không quá 31 ngày (để tránh spam)
        long diffInMillies = Math.abs(denNgayDate.getTime() - tuNgayDate.getTime());
        long diffInDays = diffInMillies / (24 * 60 * 60 * 1000);

        if (diffInDays > 31) {
            throw new IllegalStateException("Khoảng thời gian chấm công không được vượt quá 31 ngày");
        }

        // 5. Tạo danh sách tất cả các ngày trong khoảng thời gian
        List<Date> danhSachNgay = taoKhoangNgay(tuNgayDate, denNgayDate);

        // 6. Thực hiện chấm công cho từng ngày
        List<String> thanhCong = new ArrayList<>();
        List<String> thatBai = new ArrayList<>();
        List<String> boQua = new ArrayList<>();

        for (Date ngayHienTai : danhSachNgay) {
            String ngayStr = sdf.format(ngayHienTai);

            // Kiểm tra có được phép chấm công cho ngày này không (đối với NGUOICHAMCONG)
            if (vaiTroChamCong.equals("NGUOICHAMCONG")) {
                if (!isAllowedToEdit(ngayHienTai, nhanVien.getKhoaPhong().getId(), vaiTroChamCong)) {
                    boQua.add(ngayStr + " - Không có quyền chấm công cho ngày này (quá 7 ngày và không có mở khóa)");
                    continue;
                }
            }

            // Lấy thông tin chấm công hiện có trong ngày
            Date[] dateRange = getDateRangeFromDate(ngayHienTai);
            java.sql.Date startOfDay = new java.sql.Date(dateRange[0].getTime());
            java.sql.Date endOfDay = new java.sql.Date(dateRange[1].getTime());

            List<ChamCong> existingRecords = chamCongRepository.findByNhanVienAndDateRange(
                    nhanVien, startOfDay, endOfDay);

            // Chấm công cho từng ca được yêu cầu
            for (Integer shift : shifts) {
                String caStr = (shift == 1) ? "sáng" : "chiều";

                try {
                    // Kiểm tra đã có đủ 2 lần chấm công chưa
                    if (existingRecords.size() >= 2) {
                        thatBai.add(ngayStr + " - Ca " + caStr + ": Đã chấm công đủ 2 lần trong ngày");
                        continue;
                    }

                    // Kiểm tra logic ca: nếu chấm ca chiều mà chưa có ca sáng
                    if (shift == 2 && existingRecords.isEmpty()) {
                        thatBai.add(ngayStr + " - Ca " + caStr + ": Phải chấm công ca sáng trước");
                        continue;
                    }

                    // Kiểm tra đã chấm ca này chưa dựa trên số lượng bản ghi
                    if ((shift == 1 && !existingRecords.isEmpty()) ||
                            (shift == 2 && existingRecords.size() >= 2)) {
                        boQua.add(ngayStr + " - Ca " + caStr + ": Đã được chấm công");
                        continue;
                    }

                    // Tạo bản ghi chấm công với thời gian phù hợp cho ca
                    ChamCong newRecord = taoMoiBanGhiChamCongChoKhoangThoiGian(
                            nhanVien, trangThai, caLamViecId, maKyHieuChamCong, ghiChu, ngayHienTai, shift);

                    thanhCong.add(ngayStr + " - Ca " + caStr + ": " + trangThai + " thành công");

                    // Cập nhật danh sách bản ghi để kiểm tra ca tiếp theo
                    existingRecords.add(newRecord);

                } catch (Exception e) {
                    thatBai.add(ngayStr + " - Ca " + caStr + ": Lỗi - " + e.getMessage());
                }
            }
        }

        // 7. Tạo kết quả trả về
        Map<String, Object> result = new HashMap<>();
        result.put("nhanVien", nhanVien.getHoTen());
        result.put("tuNgay", tuNgay);
        result.put("denNgay", denNgay);
        result.put("tongSoNgay", danhSachNgay.size());
        result.put("tongSoCaYeuCau", danhSachNgay.size() * shifts.size());
        result.put("soLuongThanhCong", thanhCong.size());
        result.put("soLuongThatBai", thatBai.size());
        result.put("soLuongBoQua", boQua.size());
        result.put("chiTietThanhCong", thanhCong);
        result.put("chiTietThatBai", thatBai);
        result.put("chiTietBoQua", boQua);

        String thongBaoTongKet = String.format(
                "Chấm công khoảng thời gian hoàn tất cho %s từ %s đến %s: %d/%d ca thành công",
                nhanVien.getHoTen(), tuNgay, denNgay, thanhCong.size(),
                danhSachNgay.size() * shifts.size());
        result.put("message", thongBaoTongKet);

        return result;
    }

    /**
     * Helper method: Tạo danh sách tất cả các ngày trong khoảng từ tuNgay đến denNgay
     */
    private List<Date> taoKhoangNgay(Date tuNgay, Date denNgay) {
        List<Date> danhSachNgay = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.setTime(tuNgay);

        while (!cal.getTime().after(denNgay)) {
            danhSachNgay.add(new Date(cal.getTimeInMillis()));
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        return danhSachNgay;
    }

    /**
     * Helper method: Tạo bản ghi chấm công với thời gian phù hợp cho ca làm việc
     */
    private ChamCong taoMoiBanGhiChamCongChoKhoangThoiGian(NhanVien nhanVien, String trangThai,
                                                           String caLamViecId, String maKyHieuChamCong,
                                                           String ghiChu, Date ngayMuc, Integer shift) {
        ChamCong chamCong = new ChamCong();
        chamCong.setNhanVien(nhanVien);

        // Set thời gian dựa trên ca làm việc
        Calendar cal = Calendar.getInstance();
        cal.setTime(ngayMuc);

        if (shift == 1) {
            // Ca sáng: 7:00 AM
            cal.set(Calendar.HOUR_OF_DAY, 7);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
        } else {
            // Ca chiều: 1:00 PM
            cal.set(Calendar.HOUR_OF_DAY, 13);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
        }

        chamCong.setThoiGianCheckIn(cal.getTime());

        // Set trạng thái chấm công
        TrangThaiChamCong trangThaiChamCongEntity = trangThaiChamCongRepository.findByTenTrangThai(trangThai)
                .orElseThrow(() -> new IllegalStateException("Trạng thái '" + trangThai + "' không tồn tại"));
        chamCong.setTrangThaiChamCong(trangThaiChamCongEntity);

        if ("LÀM".equals(trangThai)) {
            xetCaLamViecChoTrangThaiLam(chamCong, caLamViecId, ghiChu);
        } else if ("NGHỈ".equals(trangThai)) {
            xetThongTinChoTrangThaiNghi(chamCong, caLamViecId, maKyHieuChamCong, ghiChu, nhanVien);
        }

        return chamCongRepository.save(chamCong);
    }


    // Thêm vào ChamCongService.java

    /**
     * Chấm công cho cả tháng - tất cả nhân viên trong khoa phòng
     */
    public Map<String, Object> checkInMonthly(String tenDangNhapChamCong, Long khoaPhongId,
                                              Integer year, Integer month) {

        // 1. Kiểm tra quyền người chấm công
        User chamCongUser = userRepository.findByTenDangNhap(tenDangNhapChamCong)
                .orElseThrow(() -> new SecurityException("Người chấm công không tồn tại"));

        String vaiTroChamCong = chamCongUser.getRole().getTenVaiTro();
        if (!vaiTroChamCong.equals("ADMIN") && !vaiTroChamCong.equals("NGUOICHAMCONG") &&
                !vaiTroChamCong.equals("NGUOITONGHOP")) {
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

        // 5. Tạo danh sách tất cả các ngày trong tháng
        List<Date> danhSachNgayTrongThang = taoKhoangNgayTrongThang(year, month);

        // 6. Thực hiện chấm công cho từng nhân viên và từng ngày
        List<String> thanhCong = new ArrayList<>();
        List<String> thatBai = new ArrayList<>();
        List<String> boQua = new ArrayList<>();
        int tongSoXuLy = 0;

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

        for (NhanVien nhanVien : danhSachNhanVien) {
            for (Date ngayHienTai : danhSachNgayTrongThang) {
                String ngayStr = sdf.format(ngayHienTai);

                // Kiểm tra có được phép chấm công cho ngày này không (đối với NGUOICHAMCONG)
                if (vaiTroChamCong.equals("NGUOICHAMCONG")) {
                    if (!isAllowedToEdit(ngayHienTai, khoaPhongId, vaiTroChamCong)) {
                        boQua.add(nhanVien.getHoTen() + " - " + ngayStr +
                                " - Không có quyền (quá 7 ngày và không có mở khóa)");
                        continue;
                    }
                }

                try {
                    // Kiểm tra xem đã có chấm công nào trong ngày này chưa
                    Date[] dateRange = getDateRangeFromDate(ngayHienTai);
                    java.sql.Date startOfDay = new java.sql.Date(dateRange[0].getTime());
                    java.sql.Date endOfDay = new java.sql.Date(dateRange[1].getTime());

                    List<ChamCong> existingRecords = chamCongRepository.findByNhanVienAndDateRange(
                            nhanVien, startOfDay, endOfDay);

                    if (!existingRecords.isEmpty()) {
                        boQua.add(nhanVien.getHoTen() + " - " + ngayStr + " - Đã có chấm công");
                        continue;
                    }

                    // Xác định loại ngày (thứ 7, chủ nhật hay ngày thường)
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(ngayHienTai);
                    int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

                    if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                        // Thứ 7, Chủ nhật: Nghỉ phép N1 - TẠO 2 BẢN GHI
                        List<ChamCong> danhSachChamCongNghi = taoMoiBanGhiChamCongChoThangN1(nhanVien, ngayHienTai);
                        for (int i = 0; i < danhSachChamCongNghi.size(); i++) {
                            String caInfo = (i == 0) ? "ca sáng" : "ca chiều";
                            thanhCong.add(nhanVien.getHoTen() + " - " + ngayStr + " - Nghỉ N1 (" + caInfo + " - cuối tuần)");
                            tongSoXuLy++;
                        }
                    } else {
                        // Ngày thường: Ca sáng + Ca chiều với ký hiệu X (GIỮ NGUYÊN)
                        ChamCong chamCongSang = taoMoiBanGhiChamCongChoThangX(nhanVien, ngayHienTai, 1);
                        ChamCong chamCongChieu = taoMoiBanGhiChamCongChoThangX(nhanVien, ngayHienTai, 2);

                        thanhCong.add(nhanVien.getHoTen() + " - " + ngayStr + " - Ca sáng (X)");
                        thanhCong.add(nhanVien.getHoTen() + " - " + ngayStr + " - Ca chiều (X)");
                        tongSoXuLy += 2;
                    }

                } catch (Exception e) {
                    thatBai.add(nhanVien.getHoTen() + " - " + ngayStr + " - Lỗi: " + e.getMessage());
                }
            }
        }

        // 7. Tạo kết quả trả về
        Map<String, Object> result = new HashMap<>();
        result.put("thang", month);
        result.put("nam", year);
        result.put("tongSoNhanVien", danhSachNhanVien.size());
        result.put("tongSoNgayTrongThang", danhSachNgayTrongThang.size());
        result.put("tongSoXuLy", tongSoXuLy);
        result.put("soLuongThanhCong", thanhCong.size());
        result.put("soLuongThatBai", thatBai.size());
        result.put("soLuongBoQua", boQua.size());
        result.put("chiTietThanhCong", thanhCong);
        result.put("chiTietThatBai", thatBai);
        result.put("chiTietBoQua", boQua);

        String thongBaoTongKet = String.format(
                "Chấm công tháng %d/%d hoàn tất cho %d nhân viên: %d ca thành công, %d thất bại, %d bỏ qua",
                month, year, danhSachNhanVien.size(), thanhCong.size(), thatBai.size(), boQua.size());
        result.put("message", thongBaoTongKet);

        return result;
    }

    /**
     * Xóa chấm công cho cả tháng - tất cả nhân viên trong khoa phòng
     */
    public Map<String, Object> deleteMonthly(String tenDangNhap, Long khoaPhongId,
                                             Integer year, Integer month) {

        // 1. Kiểm tra quyền người xóa
        User user = userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new SecurityException("Người dùng không tồn tại"));

        String role = user.getRole().getTenVaiTro();
        if (!role.equals("ADMIN") && !role.equals("NGUOICHAMCONG")) {
            throw new SecurityException("Bạn không có quyền xóa chấm công");
        }

        // 2. Kiểm tra quyền truy cập khoa phòng cho NGUOICHAMCONG
        if (role.equals("NGUOICHAMCONG") && !user.getKhoaPhong().getId().equals(khoaPhongId)) {
            throw new SecurityException("Chỉ được xóa chấm công cho nhân viên cùng khoa/phòng");
        }

        // 3. Kiểm tra khoa phòng tồn tại
        if (!khoaPhongRepository.existsById(khoaPhongId)) {
            throw new IllegalStateException("Khoa phòng không tồn tại");
        }

        // 4. Lấy danh sách nhân viên trong khoa phòng
        List<NhanVien> danhSachNhanVien = nhanVienRepository.findByKhoaPhongIdAndTrangThai(khoaPhongId, 1);
        if (danhSachNhanVien.isEmpty()) {
            throw new IllegalStateException("Không có nhân viên nào trong khoa phòng này");
        }

        // 5. Tạo khoảng thời gian cho tháng
        Calendar calStart = Calendar.getInstance();
        calStart.set(year, month - 1, 1, 0, 0, 0);
        calStart.set(Calendar.MILLISECOND, 0);
        Date startOfMonth = calStart.getTime();

        Calendar calEnd = Calendar.getInstance();
        calEnd.set(year, month - 1, calStart.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
        calEnd.set(Calendar.MILLISECOND, 999);
        Date endOfMonth = calEnd.getTime();

        java.sql.Date sqlStartOfMonth = new java.sql.Date(startOfMonth.getTime());
        java.sql.Date sqlEndOfMonth = new java.sql.Date(endOfMonth.getTime());

        // 6. Xóa chấm công cho từng nhân viên
        List<String> thanhCong = new ArrayList<>();
        List<String> thatBai = new ArrayList<>();
        List<String> boQua = new ArrayList<>();
        int tongSoXoa = 0;

        for (NhanVien nhanVien : danhSachNhanVien) {
            try {
                // Lấy tất cả bản ghi chấm công trong tháng
                List<ChamCong> recordsInMonth = chamCongRepository.findByNhanVienAndDateRange(
                        nhanVien, sqlStartOfMonth, sqlEndOfMonth);

                if (recordsInMonth.isEmpty()) {
                    boQua.add(nhanVien.getHoTen() + " - Không có bản ghi chấm công nào trong tháng");
                    continue;
                }

                // *** THAY ĐỔI: BỎ QUA KIỂM TRA THỜI GIAN CHO XÓA THÁNG ***
                // NGUOICHAMCONG có thể xóa toàn bộ tháng giống ADMIN
                List<ChamCong> recordsToDelete = new ArrayList<>(recordsInMonth);

                // Thực hiện xóa toàn bộ
                chamCongRepository.deleteAll(recordsToDelete);

                thanhCong.add(nhanVien.getHoTen() + " - Đã xóa " + recordsToDelete.size() + " bản ghi chấm công");
                tongSoXoa += recordsToDelete.size();

            } catch (Exception e) {
                thatBai.add(nhanVien.getHoTen() + " - Lỗi: " + e.getMessage());
            }
        }

        // 7. Tạo kết quả trả về
        Map<String, Object> result = new HashMap<>();
        result.put("thang", month);
        result.put("nam", year);
        result.put("tongSoNhanVien", danhSachNhanVien.size());
        result.put("tongSoXoa", tongSoXoa);
        result.put("soLuongThanhCong", thanhCong.size());
        result.put("soLuongThatBai", thatBai.size());
        result.put("soLuongBoQua", boQua.size());
        result.put("chiTietThanhCong", thanhCong);
        result.put("chiTietThatBai", thatBai);
        result.put("chiTietBoQua", boQua);

        String thongBaoTongKet = String.format(
                "Xóa chấm công tháng %d/%d hoàn tất: %d bản ghi đã xóa cho %d/%d nhân viên",
                month, year, tongSoXoa, thanhCong.size(), danhSachNhanVien.size());
        result.put("message", thongBaoTongKet);

        return result;
    }

    // Helper methods
    private List<Date> taoKhoangNgayTrongThang(Integer year, Integer month) {
        List<Date> danhSachNgay = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, 1); // Ngày đầu tháng

        int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int day = 1; day <= maxDay; day++) {
            cal.set(Calendar.DAY_OF_MONTH, day);
            danhSachNgay.add(new Date(cal.getTimeInMillis()));
        }

        return danhSachNgay;
    }

    private List<ChamCong> taoMoiBanGhiChamCongChoThangN1(NhanVien nhanVien, Date ngayMuc) {
        List<ChamCong> danhSachChamCong = new ArrayList<>();

        // TẠO 2 BẢN GHI: CA SÁNG VÀ CA CHIỀU
        for (int shift = 1; shift <= 2; shift++) {
            ChamCong chamCong = new ChamCong();
            chamCong.setNhanVien(nhanVien);

            // Set thời gian theo ca
            Calendar cal = Calendar.getInstance();
            cal.setTime(ngayMuc);

            if (shift == 1) {
                // Ca sáng: 8:00 AM
                cal.set(Calendar.HOUR_OF_DAY, 8);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                chamCong.setCaLamViec(caLamViecRepository.findById(11L)
                        .orElse(caLamViecRepository.findAll().get(0))); // Ca sáng
            } else {
                // Ca chiều: 2:00 PM
                cal.set(Calendar.HOUR_OF_DAY, 14);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                chamCong.setCaLamViec(caLamViecRepository.findById(12L)
                        .orElse(caLamViecRepository.findAll().get(0))); // Ca chiều
            }

            chamCong.setThoiGianCheckIn(cal.getTime());

            // Trạng thái NGHỈ
            TrangThaiChamCong trangThaiNghi = trangThaiChamCongRepository.findByTenTrangThai("NGHỈ")
                    .orElseThrow(() -> new IllegalStateException("Trạng thái NGHỈ không tồn tại"));
            chamCong.setTrangThaiChamCong(trangThaiNghi);

            // Ký hiệu N1 (Ngày nghỉ không làm việc) - KHÔNG CẦN GHI CHÚ
            KyHieuChamCong kyHieuN1 = kyHieuChamCongRepository.findByMaKyHieu("N1")
                    .orElseThrow(() -> new IllegalStateException("Ký hiệu N1 không tồn tại"));
            chamCong.setKyHieuChamCong(kyHieuN1);

            // KHÔNG SET GHI CHÚ cho nghỉ cuối tuần
            chamCong.setGhiChu(null);

            ChamCong savedChamCong = chamCongRepository.save(chamCong);
            danhSachChamCong.add(savedChamCong);
        }

        return danhSachChamCong;
    }

    private ChamCong taoMoiBanGhiChamCongChoThangX(NhanVien nhanVien, Date ngayMuc, Integer shift) {
        ChamCong chamCong = new ChamCong();
        chamCong.setNhanVien(nhanVien);

        // Set thời gian dựa trên ca
        Calendar cal = Calendar.getInstance();
        cal.setTime(ngayMuc);

        if (shift == 1) {
            // Ca sáng: 7:00 AM
            cal.set(Calendar.HOUR_OF_DAY, 7);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            chamCong.setCaLamViec(caLamViecRepository.findById(11L)
                    .orElse(caLamViecRepository.findAll().get(0))); // Ca sáng (ID=11)
        } else {
            // Ca chiều: 1:00 PM
            cal.set(Calendar.HOUR_OF_DAY, 13);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            chamCong.setCaLamViec(caLamViecRepository.findById(12L)
                    .orElse(caLamViecRepository.findAll().get(0))); // Ca chiều (ID=12)
        }

        chamCong.setThoiGianCheckIn(cal.getTime());

        // Trạng thái LÀM
        TrangThaiChamCong trangThaiLam = trangThaiChamCongRepository.findByTenTrangThai("LÀM")
                .orElseThrow(() -> new IllegalStateException("Trạng thái LÀM không tồn tại"));
        chamCong.setTrangThaiChamCong(trangThaiLam);

        // Ký hiệu X (Ngày làm việc hành chính)
        KyHieuChamCong kyHieuX = kyHieuChamCongRepository.findByMaKyHieu("X")
                .orElseThrow(() -> new IllegalStateException("Ký hiệu X không tồn tại"));
        chamCong.setKyHieuChamCong(kyHieuX);


        return chamCongRepository.save(chamCong);
    }


}



