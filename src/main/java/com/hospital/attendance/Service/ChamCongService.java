// ===== ChamCongService.java - Version hoàn chỉnh =====
package com.hospital.attendance.Service;

import com.hospital.attendance.Entity.*;
import com.hospital.attendance.Repository.*;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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

    public ChamCong checkIn(String tenDangNhapChamCong, String nhanVienId, String nhanVienHoTen, String emailNhanVien,
                            String trangThai, String caLamViecId, String maKyHieuChamCong, String ghiChu) {

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

        // 4. Tạo khoảng thời gian trong ngày
        java.sql.Date startOfDay = new java.sql.Date(getStartOfDay().getTime());
        java.sql.Date endOfDay = new java.sql.Date(getEndOfDay().getTime());

        // 5. KIỂM TRA GIỚI HẠN 2 LẦN CHẤM CÔNG TRONG NGÀY
        Long soLanChamCongTrongNgay = chamCongRepository.countByNhanVienAndThoiGianCheckInBetween(
                nhanVien, startOfDay, endOfDay);

        if (soLanChamCongTrongNgay >= 2) {
            // Lấy thông tin chi tiết để thông báo
            List<ChamCong> danhSachChamCongTrongNgay = chamCongRepository.findByNhanVienAndDateRange(
                    nhanVien, startOfDay, endOfDay);

            StringBuilder thongBao = new StringBuilder("Nhân viên đã chấm công đủ 2 lần trong ngày hôm nay: ");
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

        // 6. KIỂM TRA TRÙNG LẶP CA CỤTHỂ (chỉ với trạng thái "LÀM")
        if ("LÀM".equals(trangThai) && caLamViecId != null) {
            try {
                Long caId = Long.parseLong(caLamViecId);
                if (chamCongRepository.findByNhanVienAndCaLamViecAndThoiGianCheckInBetween(
                        nhanVien, caId, startOfDay, endOfDay).isPresent()) {
                    throw new IllegalStateException("Nhân viên này đã được check-in cho ca làm việc này trong ngày hôm nay");
                }
            } catch (NumberFormatException e) {
                throw new IllegalStateException("caLamViecId phải là số hợp lệ");
            }
        }

        // 7. Tạo bản ghi chấm công mới
        return taoMoiBanGhiChamCong(nhanVien, trangThai, caLamViecId, maKyHieuChamCong, ghiChu);
    }

    public ChamCong capNhatTrangThai(Long id, String trangThai, String caLamViecId, String maKyHieuChamCong, String ghiChu) {
        ChamCong chamCong = chamCongRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Bản ghi chấm công với ID " + id + " không tồn tại"));

        // Kiểm tra nhân viên còn hoạt động
        NhanVien nhanVien = nhanVienRepository.findByIdAndTrangThai(chamCong.getNhanVien().getId(), 1)
                .orElseThrow(() -> new IllegalStateException("Nhân viên đã bị vô hiệu hóa"));

        // Tạo khoảng thời gian trong ngày
        java.sql.Date startOfDay = new java.sql.Date(getStartOfDay().getTime());
        java.sql.Date endOfDay = new java.sql.Date(getEndOfDay().getTime());

        // KIỂM TRA TRÙNG LẶP CA CỤTHỂ khi cập nhật thành "LÀM"
        if ("LÀM".equals(trangThai) && caLamViecId != null) {
            try {
                Long caId = Long.parseLong(caLamViecId);
                // Chỉ kiểm tra nếu không phải bản ghi đang cập nhật
                if (chamCongRepository.findByNhanVienAndCaLamViecAndThoiGianCheckInBetween(
                                nhanVien, caId, startOfDay, endOfDay)
                        .filter(c -> !c.getId().equals(id))
                        .isPresent()) {
                    throw new IllegalStateException("Nhân viên này đã được check-in cho ca làm việc này trong ngày hôm nay");
                }
            } catch (NumberFormatException e) {
                throw new IllegalStateException("caLamViecId phải là số hợp lệ");
            }
        }

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

    private ChamCong taoMoiBanGhiChamCong(NhanVien nhanVien, String trangThai, String caLamViecId, String maKyHieuChamCong, String ghiChu) {
        ChamCong chamCong = new ChamCong();
        chamCong.setNhanVien(nhanVien);
        chamCong.setThoiGianCheckIn(new Date());

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
        if (maKyHieuChamCong == null || ghiChu == null) {
            throw new IllegalStateException("Phải cung cấp maKyHieuChamCong và ghiChu khi trạng thái là NGHỈ");
        }

        // Set ký hiệu chấm công
        KyHieuChamCong kyHieuChamCong = kyHieuChamCongRepository.findByMaKyHieu(maKyHieuChamCong)
                .orElseThrow(() -> new IllegalStateException("Ký hiệu chấm công '" + maKyHieuChamCong + "' không tồn tại"));
        if (!kyHieuChamCong.isTrangThai()) {
            throw new IllegalStateException("Ký hiệu chấm công '" + maKyHieuChamCong + "' không được sử dụng");
        }
        chamCong.setKyHieuChamCong(kyHieuChamCong);
        chamCong.setGhiChu(ghiChu);

        // Set ca làm việc (ưu tiên từ payload, nếu không có thì lấy ca gần nhất)
        if (caLamViecId != null) {
            try {
                Long caId = Long.parseLong(caLamViecId);
                CaLamViec caLamViec = caLamViecRepository.findById(caId)
                        .orElseThrow(() -> new IllegalStateException("Ca làm việc với ID " + caLamViecId + " không tồn tại"));
                chamCong.setCaLamViec(caLamViec);
            } catch (NumberFormatException e) {
                throw new IllegalStateException("caLamViecId phải là số hợp lệ");
            }
        } else {
            // Lấy ca làm việc gần nhất của nhân viên
            ChamCong lastChamCong = chamCongRepository.findLatestWithCaLamViecByNhanVienId(nhanVien.getId())
                    .orElse(null);
            chamCong.setCaLamViec(lastChamCong != null ? lastChamCong.getCaLamViec() : null);
        }
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
     * Kiểm tra trạng thái chấm công của nhân viên trong ngày
     */
    public Map<String, Object> kiemTraTrangThaiChamCongTrongNgay(String tenDangNhap, String nhanVienId, String nhanVienHoTen, String emailNhanVien) {
        // Kiểm tra quyền
        User chamCongUser = userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new SecurityException("Người dùng không tồn tại"));

        // Tìm nhân viên
        NhanVien nhanVien = timNhanVien(nhanVienId, nhanVienHoTen, emailNhanVien);

        // Kiểm tra quyền chấm công cho nhân viên này
        String vaiTro = chamCongUser.getRole().getTenVaiTro();
        kiemTraQuyenChamCongChoNhanVien(chamCongUser, nhanVien, vaiTro);

        // Lấy thông tin chấm công trong ngày
        java.sql.Date startOfDay = new java.sql.Date(getStartOfDay().getTime());
        java.sql.Date endOfDay = new java.sql.Date(getEndOfDay().getTime());

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

        // Kiểm tra ca nào đã chấm công
        Set<Long> cacCaDaChamCong = new HashSet<>();
        for (ChamCong cc : danhSachChamCong) {
            if (cc.getCaLamViec() != null) {
                cacCaDaChamCong.add(cc.getCaLamViec().getId());
            }
        }
        result.put("cacCaDaChamCong", cacCaDaChamCong);

        return result;
    }

    /**
     * Lấy chi tiết chấm công của nhân viên trong ngày hôm nay
     */
    public List<ChamCong> layChiTietChamCongHomNay(String tenDangNhap, String nhanVienId, String nhanVienHoTen, String emailNhanVien) {
        // Kiểm tra quyền
        User chamCongUser = userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new SecurityException("Người dùng không tồn tại"));

        // Tìm nhân viên
        NhanVien nhanVien = timNhanVien(nhanVienId, nhanVienHoTen, emailNhanVien);

        // Kiểm tra quyền chấm công cho nhân viên này
        String vaiTro = chamCongUser.getRole().getTenVaiTro();
        kiemTraQuyenChamCongChoNhanVien(chamCongUser, nhanVien, vaiTro);

        // Lấy chi tiết chấm công trong ngày
        java.sql.Date startOfDay = new java.sql.Date(getStartOfDay().getTime());
        java.sql.Date endOfDay = new java.sql.Date(getEndOfDay().getTime());

        return chamCongRepository.findByNhanVienAndDateRange(nhanVien, startOfDay, endOfDay);
    }
}