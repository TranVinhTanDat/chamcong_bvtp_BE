package com.hospital.attendance.Service;

import com.hospital.attendance.Entity.NhatKyDieuDuong;
import com.hospital.attendance.Entity.NhatKyDieuDuong.LoaiMauNhatKy;
import com.hospital.attendance.Entity.User;
import com.hospital.attendance.Repository.NhatKyDieuDuongRepository;
import com.hospital.attendance.Repository.KhoaPhongRepository;
import com.hospital.attendance.Repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class NhatKyDieuDuongService {

    private static final Logger logger = LoggerFactory.getLogger(NhatKyDieuDuongService.class);

    @Autowired
    private NhatKyDieuDuongRepository nhatKyDieuDuongRepository;

    @Autowired
    private KhoaPhongRepository khoaPhongRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Tạo mới nhật ký điều dưỡng
     */
    /**
     * Tạo mới nhật ký điều dưỡng - CẢI TIẾN VỚI AUTO RESTORE
     */
    @Transactional
    public NhatKyDieuDuong createNhatKyDieuDuong(NhatKyDieuDuong nhatKyDieuDuong, String tenDangNhap) {
        logger.info("Creating NhatKyDieuDuong for date: {}, khoaPhongId: {}, loaiMau: {}",
                nhatKyDieuDuong.getNgay(), nhatKyDieuDuong.getKhoaPhong().getId(), nhatKyDieuDuong.getLoaiMau());

        // Lấy thông tin user
        User user = userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new SecurityException("Người dùng không tồn tại"));

        // Validate khoa phòng
        if (nhatKyDieuDuong.getKhoaPhong() == null || nhatKyDieuDuong.getKhoaPhong().getId() == null) {
            throw new IllegalStateException("Phải cung cấp thông tin khoa/phòng hợp lệ");
        }

        Long khoaPhongId = nhatKyDieuDuong.getKhoaPhong().getId();
        if (!khoaPhongRepository.existsById(khoaPhongId)) {
            throw new IllegalStateException("Khoa/phòng với ID " + khoaPhongId + " không tồn tại");
        }

        // Kiểm tra quyền truy cập khoa phòng
        validateKhoaPhongAccess(user, khoaPhongId);

        // ✅ KIỂM TRA BẢN GHI ACTIVE
        boolean activeExists = nhatKyDieuDuongRepository.existsByNgayAndKhoaPhongIdAndLoaiMauAndTrangThai(
                nhatKyDieuDuong.getNgay(), khoaPhongId, nhatKyDieuDuong.getLoaiMau(), 1);

        if (activeExists) {
            throw new IllegalStateException("Nhật ký cho ngày " + nhatKyDieuDuong.getNgay() +
                    " với loại mẫu " + nhatKyDieuDuong.getLoaiMau().getMoTa() + " đã tồn tại");
        }

        // ✅ TỰ ĐỘNG PHỤC HỒI BẢN GHI ĐÃ XÓA NẾU CÓ
        Optional<NhatKyDieuDuong> restoredRecord = checkAndRestoreIfDeleted(
                nhatKyDieuDuong.getNgay(), khoaPhongId, nhatKyDieuDuong.getLoaiMau(), tenDangNhap);

        if (restoredRecord.isPresent()) {
            logger.info("🔄 Found and restored deleted record, updating with new data...");
            NhatKyDieuDuong existing = restoredRecord.get();

            // Cập nhật với dữ liệu mới
            updateNhatKyFields(existing, nhatKyDieuDuong);
            existing.setNguoiCapNhat(tenDangNhap);
            existing.setNgayCapNhat(LocalDateTime.now());
            existing.setGhiChu(nhatKyDieuDuong.getGhiChu());

            return nhatKyDieuDuongRepository.save(existing);
        }

        // Validate ngày không được trong tương lai
        if (nhatKyDieuDuong.getNgay().isAfter(LocalDate.now())) {
            throw new IllegalStateException("Ngày nhật ký không được là tương lai");
        }

        // Set thông tin khoa phòng và metadata
        nhatKyDieuDuong.setKhoaPhong(khoaPhongRepository.findById(khoaPhongId).get());
        nhatKyDieuDuong.setNguoiTao(tenDangNhap);
        nhatKyDieuDuong.setNgayTao(LocalDateTime.now());
        nhatKyDieuDuong.setTrangThai(1);

        // Validate dữ liệu theo loại mẫu
        validateDataByMauType(nhatKyDieuDuong);

        logger.info("💾 Saving new NhatKyDieuDuong to database");
        return nhatKyDieuDuongRepository.save(nhatKyDieuDuong);
    }

    /**
     * Cập nhật nhật ký điều dưỡng
     */
    @Transactional
    public NhatKyDieuDuong updateNhatKyDieuDuong(Long id, NhatKyDieuDuong nhatKyDetails, String tenDangNhap) {
        logger.info("Updating NhatKyDieuDuong with ID: {}", id);

        // Lấy thông tin user
        User user = userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new SecurityException("Người dùng không tồn tại"));

        // Tìm bản ghi hiện tại
        NhatKyDieuDuong existingNhatKy = nhatKyDieuDuongRepository.findByIdAndTrangThai(id, 1)
                .orElseThrow(() -> new IllegalStateException("Nhật ký với ID " + id + " không tồn tại hoặc đã bị xóa"));

        // Kiểm tra quyền truy cập khoa phòng
        validateKhoaPhongAccess(user, existingNhatKy.getKhoaPhong().getId());

        // Không cho phép thay đổi ngày, khoa phòng, loại mẫu
        if (!existingNhatKy.getNgay().equals(nhatKyDetails.getNgay())) {
            throw new IllegalStateException("Không được thay đổi ngày của nhật ký");
        }

        if (!existingNhatKy.getKhoaPhong().getId().equals(nhatKyDetails.getKhoaPhong().getId())) {
            throw new IllegalStateException("Không được thay đổi khoa phòng của nhật ký");
        }

        if (!existingNhatKy.getLoaiMau().equals(nhatKyDetails.getLoaiMau())) {
            throw new IllegalStateException("Không được thay đổi loại mẫu của nhật ký");
        }

        // Validate dữ liệu theo loại mẫu
        validateDataByMauType(nhatKyDetails);

        // Cập nhật các trường dữ liệu
        updateNhatKyFields(existingNhatKy, nhatKyDetails);

        // Cập nhật metadata
        existingNhatKy.setNguoiCapNhat(tenDangNhap);
        existingNhatKy.setNgayCapNhat(LocalDateTime.now());
        existingNhatKy.setGhiChu(nhatKyDetails.getGhiChu());

        logger.info("Updating NhatKyDieuDuong in database");
        return nhatKyDieuDuongRepository.save(existingNhatKy);
    }

    /**
     * Lấy danh sách nhật ký với phân trang và filter
     */
    public Page<NhatKyDieuDuong> getNhatKyDieuDuongWithFilters(String tenDangNhap, int page, int size,
                                                               Long khoaPhongId, LoaiMauNhatKy loaiMau, LocalDate tuNgay, LocalDate denNgay) {

        User user = userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new SecurityException("Người dùng không tồn tại"));

        // Xác định khoa phòng có thể truy cập
        Long finalKhoaPhongId = determineAccessibleKhoaPhong(user, khoaPhongId);

        Pageable pageable = PageRequest.of(page, size);

        // Nếu có khoảng thời gian
        if (tuNgay != null && denNgay != null) {
            return nhatKyDieuDuongRepository.findByDateRangeAndFilters(finalKhoaPhongId, loaiMau, tuNgay, denNgay, 1, pageable);
        }

        // Không có khoảng thời gian
        return nhatKyDieuDuongRepository.findByFiltersWithPagination(finalKhoaPhongId, loaiMau, 1, pageable);
    }

    /**
     * Lấy nhật ký theo ID
     */
    public Optional<NhatKyDieuDuong> getNhatKyDieuDuongById(Long id, String tenDangNhap) {
        User user = userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new SecurityException("Người dùng không tồn tại"));

        Optional<NhatKyDieuDuong> nhatKy = nhatKyDieuDuongRepository.findByIdAndTrangThai(id, 1);

        if (nhatKy.isPresent()) {
            // Kiểm tra quyền truy cập khoa phòng
            validateKhoaPhongAccess(user, nhatKy.get().getKhoaPhong().getId());
        }

        return nhatKy;
    }

    /**
     * Xóa mềm nhật ký điều dưỡng - CHO PHÉP NGUOIDIENNHATKYDD XÓA NHẬT KÝ CỦA KHOA MÌNH
     */
    @Transactional
    public void deleteNhatKyDieuDuong(Long id, String tenDangNhap) {
        logger.info("Soft deleting NhatKyDieuDuong with ID: {}", id);

        User user = userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new SecurityException("Người dùng không tồn tại"));

        NhatKyDieuDuong nhatKy = nhatKyDieuDuongRepository.findByIdAndTrangThai(id, 1)
                .orElseThrow(() -> new IllegalStateException("Nhật ký với ID " + id + " không tồn tại hoặc đã bị xóa"));

        // Kiểm tra quyền truy cập khoa phòng
        validateKhoaPhongAccess(user, nhatKy.getKhoaPhong().getId());

        // ✅ CHO PHÉP CẢ ADMIN VÀ NGUOIDIENNHATKYDD XÓA
        String userRole = user.getRole().getTenVaiTro();
        if (!"ADMIN".equals(userRole) && !"NGUOIDIENNHATKYDD".equals(userRole)) {
            throw new SecurityException("Chỉ có ADMIN và NGUOIDIENNHATKYDD mới được phép xóa nhật ký điều dưỡng");
        }

        // ✅ NGUOIDIENNHATKYDD chỉ được xóa nhật ký của khoa phòng mình
        if ("NGUOIDIENNHATKYDD".equals(userRole)) {
            Long userKhoaPhongId = user.getKhoaPhong() != null ? user.getKhoaPhong().getId() : null;
            if (userKhoaPhongId == null) {
                throw new SecurityException("Người điền nhật ký điều dưỡng chưa được phân công khoa phòng");
            }
            if (!userKhoaPhongId.equals(nhatKy.getKhoaPhong().getId())) {
                throw new SecurityException("Bạn chỉ có thể xóa nhật ký của khoa phòng được phân công");
            }
            logger.info("✅ NGUOIDIENNHATKYDD {} deleting diary from their department: {}",
                    tenDangNhap, userKhoaPhongId);
        }

        // ✅ ADMIN có thể xóa bất kỳ nhật ký nào
        if ("ADMIN".equals(userRole)) {
            logger.info("✅ ADMIN {} deleting diary from department: {}",
                    tenDangNhap, nhatKy.getKhoaPhong().getId());
        }

        nhatKy.setTrangThai(0);
        nhatKy.setNguoiCapNhat(tenDangNhap);
        nhatKy.setNgayCapNhat(LocalDateTime.now());
        nhatKyDieuDuongRepository.save(nhatKy);

        logger.info("✅ Soft deleted NhatKyDieuDuong successfully by {} ({})", tenDangNhap, userRole);
    }

    /**
     * Lấy nhật ký theo tháng để tạo báo cáo
     */
    public List<NhatKyDieuDuong> getNhatKyForMonthlyReport(String tenDangNhap, Long khoaPhongId,
                                                           LoaiMauNhatKy loaiMau, Integer thang, Integer nam) {

        User user = userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new SecurityException("Người dùng không tồn tại"));

        // Xác định khoa phòng có thể truy cập
        Long finalKhoaPhongId = determineAccessibleKhoaPhong(user, khoaPhongId);
        if (finalKhoaPhongId == null) {
            throw new IllegalStateException("Không thể xác định khoa phòng để tạo báo cáo");
        }

        return nhatKyDieuDuongRepository.findForMonthlyReport(finalKhoaPhongId, loaiMau, thang, nam, 1);
    }

    /**
     * Lấy bản ghi gần nhất để làm template
     */
    public Optional<NhatKyDieuDuong> getLatestTemplate(String tenDangNhap, Long khoaPhongId, LoaiMauNhatKy loaiMau) {
        User user = userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new SecurityException("Người dùng không tồn tại"));

        Long finalKhoaPhongId = determineAccessibleKhoaPhong(user, khoaPhongId);
        if (finalKhoaPhongId == null) {
            return Optional.empty();
        }

        List<NhatKyDieuDuong> latest = nhatKyDieuDuongRepository.findLatestByKhoaPhongAndLoaiMau(
                finalKhoaPhongId, loaiMau, 1, PageRequest.of(0, 1));

        return latest.isEmpty() ? Optional.empty() : Optional.of(latest.get(0));
    }

    // *** PRIVATE HELPER METHODS ***

    private void validateKhoaPhongAccess(User user, Long khoaPhongId) {
        String userRole = user.getRole().getTenVaiTro();
        Long userKhoaPhongId = user.getKhoaPhong() != null ? user.getKhoaPhong().getId() : null;

        logger.info("🔐 Validating access - User: {}, Role: {}, UserKhoaPhongId: {}, RequestedKhoaPhongId: {}",
                user.getTenDangNhap(), userRole, userKhoaPhongId, khoaPhongId);

        switch (userRole) {
            case "ADMIN":
                // ADMIN có thể truy cập tất cả khoa phòng
                logger.info("✅ ADMIN access granted for all departments");
                break;

            case "NGUOIDIENNHATKYDD":
                // NGUOIDIENNHATKYDD chỉ có thể truy cập khoa phòng của mình
                if (userKhoaPhongId == null) {
                    throw new SecurityException("Người điền nhật ký điều dưỡng chưa được phân công khoa phòng");
                }
                if (khoaPhongId != null && !userKhoaPhongId.equals(khoaPhongId)) {
                    throw new SecurityException("Bạn chỉ có thể truy cập nhật ký của khoa phòng được phân công");
                }
                logger.info("✅ NGUOIDIENNHATKYDD access granted for khoa phong: {}", userKhoaPhongId);
                break;

            default:
                // ❌ LOẠI BỎ TẤT CẢ CÁC ROLE KHÁC
                logger.error("❌ Unauthorized role for Nhat Ky Dieu Duong: {}", userRole);
                throw new SecurityException("Chỉ có ADMIN và NGUOIDIENNHATKYDD mới được truy cập nhật ký điều dưỡng");
        }
    }

    private Long determineAccessibleKhoaPhong(User user, Long requestedKhoaPhongId) {
        String userRole = user.getRole().getTenVaiTro();
        Long userKhoaPhongId = user.getKhoaPhong() != null ? user.getKhoaPhong().getId() : null;

        logger.info("🎯 Determining accessible khoa phong - User: {}, Role: {}, UserKhoaPhongId: {}, RequestedKhoaPhongId: {}",
                user.getTenDangNhap(), userRole, userKhoaPhongId, requestedKhoaPhongId);

        switch (userRole) {
            case "ADMIN":
                // ADMIN có thể truy cập tất cả hoặc theo filter
                logger.info("✅ ADMIN can access all departments");
                return requestedKhoaPhongId; // null = tất cả, có giá trị = filter theo yêu cầu

            case "NGUOIDIENNHATKYDD":
                // NGUOIDIENNHATKYDD chỉ được truy cập khoa phòng của mình
                if (userKhoaPhongId == null) {
                    throw new SecurityException("Người điền nhật ký điều dưỡng chưa được phân công khoa phòng");
                }
                logger.info("✅ NGUOIDIENNHATKYDD restricted to khoa phong: {}", userKhoaPhongId);
                return userKhoaPhongId; // Bắt buộc phải là khoa phòng của user

            default:
                // ❌ LOẠI BỎ TẤT CẢ CÁC ROLE KHÁC
                logger.error("❌ Unauthorized role for Nhat Ky Dieu Duong: {}", userRole);
                throw new SecurityException("Chỉ có ADMIN và NGUOIDIENNHATKYDD mới được truy cập nhật ký điều dưỡng");
        }
    }

    private void validateDataByMauType(NhatKyDieuDuong nhatKy) {
        // Tùy theo loại mẫu có thể có validation khác nhau
        if (nhatKy.getLoaiMau() == LoaiMauNhatKy.MAU_1) {
            // Validate dữ liệu cho mẫu 1 (quản lý khoa khối lâm sàng)
            // Ví dụ: giường thực kê không được lớn hơn giường chỉ tiêu
            if (nhatKy.getGiuongThucKe() != null && nhatKy.getGiuongChiTieu() != null &&
                    nhatKy.getGiuongThucKe() > nhatKy.getGiuongChiTieu()) {
                logger.warn("Giường thực kê ({}) > giường chỉ tiêu ({})",
                        nhatKy.getGiuongThucKe(), nhatKy.getGiuongChiTieu());
            }
        } else if (nhatKy.getLoaiMau() == LoaiMauNhatKy.MAU_2) {
            // Validate dữ liệu cho mẫu 2 (tình hình nhân sự)
            // Ví dụ: tổng nhân sự = điều dưỡng + hộ sinh + ...
            // (có thể bỏ qua validation này nếu frontend đã tính toán)
        } else if (nhatKy.getLoaiMau() == LoaiMauNhatKy.MAU_3) {
            // Validate dữ liệu cho mẫu 3 (khối cận lâm sàng)
            // Kiểm tra tổng mẫu XN = ngoại trú + nội trú + cấp cứu
            if (nhatKy.getXnTongSoMau() != null && nhatKy.getXnMauNgoaiTru() != null &&
                    nhatKy.getXnMauNoiTru() != null && nhatKy.getXnMauCapCuu() != null) {
                int tongTinhToan = nhatKy.getXnMauNgoaiTru() + nhatKy.getXnMauNoiTru() + nhatKy.getXnMauCapCuu();
                if (Math.abs(nhatKy.getXnTongSoMau() - tongTinhToan) > nhatKy.getXnTongSoMau() * 0.1) { // Cho phép sai lệch 10%
                    logger.warn("Tổng mẫu XN ({}) khác tổng tính toán ({})",
                            nhatKy.getXnTongSoMau(), tongTinhToan);
                }
            }

            // Kiểm tra tổng NB XN = ngoại trú + nội trú + cấp cứu
            if (nhatKy.getXnNbTongSo() != null && nhatKy.getXnNbNgoaiTru() != null &&
                    nhatKy.getXnNbNoiTru() != null && nhatKy.getXnNbCapCuu() != null) {
                int tongNbTinhToan = nhatKy.getXnNbNgoaiTru() + nhatKy.getXnNbNoiTru() + nhatKy.getXnNbCapCuu();
                if (Math.abs(nhatKy.getXnNbTongSo() - tongNbTinhToan) > nhatKy.getXnNbTongSo() * 0.1) {
                    logger.warn("Tổng NB XN ({}) khác tổng tính toán ({})",
                            nhatKy.getXnNbTongSo(), tongNbTinhToan);
                }
            }
        }
    }

    private void updateNhatKyFields(NhatKyDieuDuong existing, NhatKyDieuDuong details) {
        // Cập nhật tất cả các trường dữ liệu từ details sang existing
        // Mẫu 1 - Thông tin bệnh nhân
        existing.setGiuongThucKe(details.getGiuongThucKe());
        existing.setGiuongChiTieu(details.getGiuongChiTieu());
        existing.setTongBenhCu(details.getTongBenhCu());
        existing.setBnVaoVien(details.getBnVaoVien());
        existing.setTongXuatVien(details.getTongXuatVien());
        existing.setChuyenVien(details.getChuyenVien());
        existing.setChuyenKhoa(details.getChuyenKhoa());
        existing.setTronVien(details.getTronVien());
        existing.setXinVe(details.getXinVe());
        existing.setTuVong(details.getTuVong());
        existing.setBenhHienCo(details.getBenhHienCo());

        // Tình hình sản phụ
        existing.setSanhThuong(details.getSanhThuong());
        existing.setSanhMo(details.getSanhMo());
        existing.setMoPhuKhoa(details.getMoPhuKhoa());

        // Tình hình phẫu thuật
        existing.setCapCuu(details.getCapCuu());
        existing.setChuongTrinh(details.getChuongTrinh());
        existing.setThuThuat(details.getThuThuat());
        existing.setTieuPhau(details.getTieuPhau());
        existing.setPhauThuat(details.getPhauThuat());
        existing.setPtLoaiI(details.getPtLoaiI());
        existing.setPtLoaiII(details.getPtLoaiII());
        existing.setPtLoaiIII(details.getPtLoaiIII());

        // Chăm sóc điều dưỡng
        existing.setThoCpap(details.getThoCpap());
        existing.setThoMay(details.getThoMay());
        existing.setThoOxy(details.getThoOxy());
        existing.setBopBong(details.getBopBong());
        existing.setMonitor(details.getMonitor());
        existing.setCvp(details.getCvp());
        existing.setNoiKhiQuan(details.getNoiKhiQuan());
        existing.setNoiSoi(details.getNoiSoi());
        existing.setSondeDaDay(details.getSondeDaDay());
        existing.setSondeTieu(details.getSondeTieu());
        existing.setHutDamNhot(details.getHutDamNhot());

        // Phân cấp chăm sóc
        existing.setCsCapI(details.getCsCapI());
        existing.setCsCapII(details.getCsCapII());
        existing.setCsCapIII(details.getCsCapIII());

        // Tình hình KCB
        existing.setTsNbKcb(details.getTsNbKcb());
        existing.setTsNbCapCuu(details.getTsNbCapCuu());
        existing.setNgoaiVien(details.getNgoaiVien());
        existing.setChuyenNoiTru(details.getChuyenNoiTru());
        existing.setChuyenCapCuu(details.getChuyenCapCuu());
        existing.setChuyenVienKcb(details.getChuyenVienKcb());
        existing.setChuyenPkKNgoai(details.getChuyenPkKNgoai());
        existing.setTuVongKcb(details.getTuVongKcb());
        existing.setTongNbDoDienTim(details.getTongNbDoDienTim());
        existing.setTongNbDoDienCo(details.getTongNbDoDienCo());
        existing.setTongNbDoChucNangHoHap(details.getTongNbDoChucNangHoHap());

        // Mẫu 2 - Thông tin nhân sự
        existing.setDieuDuong(details.getDieuDuong());
        existing.setHoSinh(details.getHoSinh());
        existing.setKyThuatVien(details.getKyThuatVien());
        existing.setYSi(details.getYSi());
        existing.setNhanSuKhac(details.getNhanSuKhac());
        existing.setHoLyNhanSu(details.getHoLyNhanSu());
        existing.setTongNhanSu(details.getTongNhanSu());

        // Hiện diện
        existing.setDdtKhoa(details.getDdtKhoa());
        existing.setDdhc(details.getDdhc());
        existing.setPhongKham(details.getPhongKham());
        existing.setTourSang(details.getTourSang());
        existing.setTourChieu(details.getTourChieu());
        existing.setTourDem(details.getTourDem());
        existing.setTruc2424(details.getTruc2424());
        existing.setHoLyHienDien(details.getHoLyHienDien());
        existing.setTongHienDien(details.getTongHienDien());

        // Vắng
        existing.setRaTruc(details.getRaTruc());
        existing.setBuTruc(details.getBuTruc());
        existing.setNghiPhep(details.getNghiPhep());
        existing.setNghiOm(details.getNghiOm());
        existing.setNghiHauSan(details.getNghiHauSan());
        existing.setNghiKhac(details.getNghiKhac());
        existing.setDiHoc(details.getDiHoc());
        existing.setCongTac(details.getCongTac());
        existing.setHoLyVang(details.getHoLyVang());
        existing.setTongVang(details.getTongVang());

        // Đào tạo
        existing.setNhanVienThuViec(details.getNhanVienThuViec());
        existing.setThucHanhKLuong(details.getThucHanhKLuong());
        existing.setNhanSuTangCuong(details.getNhanSuTangCuong());
        existing.setSvDdHs(details.getSvDdHs());
        existing.setSvYSi(details.getSvYSi());
        existing.setSvKtv(details.getSvKtv());
        existing.setSvDuoc(details.getSvDuoc());


        // Mẫu 3 - Khoa Xét nghiệm
        existing.setXnTongSoMau(details.getXnTongSoMau());
        existing.setXnMauNgoaiTru(details.getXnMauNgoaiTru());
        existing.setXnMauNoiTru(details.getXnMauNoiTru());
        existing.setXnMauCapCuu(details.getXnMauCapCuu());
        existing.setXnNbTongSo(details.getXnNbTongSo());
        existing.setXnNbNgoaiTru(details.getXnNbNgoaiTru());
        existing.setXnNbNoiTru(details.getXnNbNoiTru());
        existing.setXnNbCapCuu(details.getXnNbCapCuu());
        existing.setXnHuyetHoc(details.getXnHuyetHoc());
        existing.setXnSinhHoa(details.getXnSinhHoa());
        existing.setXnViSinh(details.getXnViSinh());
        existing.setXnGiaiPhauBenh(details.getXnGiaiPhauBenh());

        // Mẫu 3 - Khoa CĐHA
        existing.setCdhaXqTongNb(details.getCdhaXqTongNb());
        existing.setCdhaXqTongPhim(details.getCdhaXqTongPhim());
        existing.setCdhaCTTongNb(details.getCdhaCTTongNb());
        existing.setCdhaCTTongPhim(details.getCdhaCTTongPhim());
        existing.setCdhaSATongNb(details.getCdhaSATongNb());
        existing.setCdhaSATongSo(details.getCdhaSATongSo());
    }

    // Thêm method này vào NhatKyDieuDuongService

    /**
     * Kiểm tra nhật ký đã tồn tại
     */
    /**
     * Kiểm tra nhật ký đã tồn tại (CHỈ BẢN GHI ACTIVE)
     */
    public boolean kiemTraTonTai(String tenDangNhap, LocalDate ngay, Long khoaPhongId, LoaiMauNhatKy loaiMau) {
        User user = userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new SecurityException("Người dùng không tồn tại"));

        // Kiểm tra quyền truy cập khoa phòng
        validateKhoaPhongAccess(user, khoaPhongId);

        // ✅ CHỈ KIỂM TRA BẢN GHI ACTIVE (trangThai = 1)
        boolean activeExists = nhatKyDieuDuongRepository.existsByNgayAndKhoaPhongIdAndLoaiMauAndTrangThai(
                ngay, khoaPhongId, loaiMau, 1);

        // ✅ KIỂM TRA BẢN GHI ĐÃ BỊ XÓA MỀM
        boolean deletedExists = nhatKyDieuDuongRepository.existsByNgayAndKhoaPhongIdAndLoaiMauAndTrangThai(
                ngay, khoaPhongId, loaiMau, 0);

        // ✅ THÊM LOG DEBUG
        logger.info("🔍 Checking existence for {}/{}/{}: activeExists={}, deletedExists={}",
                ngay, khoaPhongId, loaiMau, activeExists, deletedExists);

        return activeExists; // ✅ CHỈ TRẢ VỀ TRẠNG THÁI ACTIVE
    }

    /**
     * Kiểm tra và tự động phục hồi bản ghi đã xóa nếu cần
     */
    @Transactional
    public Optional<NhatKyDieuDuong> checkAndRestoreIfDeleted(LocalDate ngay, Long khoaPhongId,
                                                              LoaiMauNhatKy loaiMau, String tenDangNhap) {
        logger.info("🔄 Checking for deleted record to restore: {}/{}/{}", ngay, khoaPhongId, loaiMau);

        // Tìm bản ghi đã bị xóa mềm
        Optional<NhatKyDieuDuong> deletedRecord = nhatKyDieuDuongRepository
                .findByNgayAndKhoaPhongIdAndLoaiMauAndTrangThai(ngay, khoaPhongId, loaiMau, 0);

        if (deletedRecord.isPresent()) {
            logger.info("📋 Found deleted record with ID: {}, restoring...", deletedRecord.get().getId());

            NhatKyDieuDuong restoredRecord = deletedRecord.get();
            restoredRecord.setTrangThai(1);
            restoredRecord.setNguoiCapNhat(tenDangNhap);
            restoredRecord.setNgayCapNhat(LocalDateTime.now());

            NhatKyDieuDuong saved = nhatKyDieuDuongRepository.save(restoredRecord);
            logger.info("✅ Successfully restored record with ID: {}", saved.getId());

            return Optional.of(saved);
        }

        return Optional.empty();
    }

    /**
     * Phục hồi thủ công bản ghi đã xóa - CHO PHÉP NGUOIDIENNHATKYDD PHỤC HỒI NHẬT KÝ CỦA KHOA MÌNH
     */
    @Transactional
    public NhatKyDieuDuong restoreNhatKyDieuDuong(Long id, String tenDangNhap) {
        logger.info("🔄 Manual restore NhatKyDieuDuong with ID: {}", id);

        User user = userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new SecurityException("Người dùng không tồn tại"));

        // ✅ CHO PHÉP CẢ ADMIN VÀ NGUOIDIENNHATKYDD PHỤC HỒI
        String userRole = user.getRole().getTenVaiTro();
        if (!"ADMIN".equals(userRole) && !"NGUOIDIENNHATKYDD".equals(userRole)) {
            throw new SecurityException("Chỉ có ADMIN và NGUOIDIENNHATKYDD mới được phép phục hồi nhật ký điều dưỡng");
        }

        // Tìm bản ghi đã bị xóa
        NhatKyDieuDuong nhatKy = nhatKyDieuDuongRepository.findByIdAndTrangThai(id, 0)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy nhật ký đã bị xóa với ID: " + id));

        // Kiểm tra quyền truy cập khoa phòng
        validateKhoaPhongAccess(user, nhatKy.getKhoaPhong().getId());

        // ✅ NGUOIDIENNHATKYDD chỉ được phục hồi nhật ký của khoa phòng mình
        if ("NGUOIDIENNHATKYDD".equals(userRole)) {
            Long userKhoaPhongId = user.getKhoaPhong() != null ? user.getKhoaPhong().getId() : null;
            if (userKhoaPhongId == null) {
                throw new SecurityException("Người điền nhật ký điều dưỡng chưa được phân công khoa phòng");
            }
            if (!userKhoaPhongId.equals(nhatKy.getKhoaPhong().getId())) {
                throw new SecurityException("Bạn chỉ có thể phục hồi nhật ký của khoa phòng được phân công");
            }
            logger.info("✅ NGUOIDIENNHATKYDD {} restoring diary from their department: {}",
                    tenDangNhap, userKhoaPhongId);
        }

        // ✅ ADMIN có thể phục hồi bất kỳ nhật ký nào
        if ("ADMIN".equals(userRole)) {
            logger.info("✅ ADMIN {} restoring diary from department: {}",
                    tenDangNhap, nhatKy.getKhoaPhong().getId());
        }

        // Kiểm tra có conflict với bản ghi active không
        boolean activeExists = nhatKyDieuDuongRepository.existsByNgayAndKhoaPhongIdAndLoaiMauAndTrangThai(
                nhatKy.getNgay(), nhatKy.getKhoaPhong().getId(), nhatKy.getLoaiMau(), 1);

        if (activeExists) {
            throw new IllegalStateException("Đã có nhật ký active cho ngày " + nhatKy.getNgay() +
                    ", không thể phục hồi. Vui lòng xóa nhật ký hiện tại trước.");
        }




        // Phục hồi
        nhatKy.setTrangThai(1);
        nhatKy.setNguoiCapNhat(tenDangNhap);
        nhatKy.setNgayCapNhat(LocalDateTime.now());

        NhatKyDieuDuong restored = nhatKyDieuDuongRepository.save(nhatKy);
        logger.info("✅ Successfully restored NhatKyDieuDuong with ID: {} by {} ({})",
                restored.getId(), tenDangNhap, userRole);

        return restored;
    }
}