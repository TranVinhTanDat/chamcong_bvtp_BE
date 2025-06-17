package com.hospital.attendance.Service;

import com.hospital.attendance.Entity.*;
import com.hospital.attendance.Repository.*;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Date;
import java.util.List;

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
        User chamCongUser = userRepository.findByTenDangNhap(tenDangNhapChamCong)
                .orElseThrow(() -> new SecurityException("Người chấm công không tồn tại"));

        String vaiTroChamCong = chamCongUser.getRole().getTenVaiTro();
        if (!vaiTroChamCong.equals("ADMIN") && !vaiTroChamCong.equals("NGUOICHAMCONG") && !vaiTroChamCong.equals("NGUOITONGHOP")) { // Thêm NGUOITONGHOP
            throw new SecurityException("Bạn không có quyền chấm công");
        }

        NhanVien nhanVien = null;
        if (nhanVienId != null) {
            try {
                Long id = Long.parseLong(nhanVienId);
                nhanVien = nhanVienRepository.findById(id)
                        .orElseThrow(() -> new SecurityException("Nhân viên với ID " + id + " không tồn tại"));
            } catch (NumberFormatException e) {
                throw new SecurityException("nhanVienId phải là số hợp lệ (ID)");
            }
        } else if (nhanVienHoTen != null) {
            nhanVien = nhanVienRepository.findByHoTen(nhanVienHoTen)
                    .orElseThrow(() -> new SecurityException("Nhân viên với họ tên '" + nhanVienHoTen + "' không tồn tại"));
        } else if (emailNhanVien != null) {
            nhanVien = nhanVienRepository.findByEmail(emailNhanVien)
                    .orElseThrow(() -> new SecurityException("Nhân viên với email '" + emailNhanVien + "' không tồn tại"));
        } else {
            throw new SecurityException("Thiếu thông tin nhân viên (nhanVienId, nhanVienHoTen, hoặc emailNhanVien)");
        }

        if ((vaiTroChamCong.equals("NGUOICHAMCONG") || vaiTroChamCong.equals("NGUOITONGHOP")) && // Thêm NGUOITONGHOP
                !chamCongUser.getKhoaPhong().getId().equals(nhanVien.getKhoaPhong().getId())) {
            throw new SecurityException("Chỉ được chấm công cho nhân viên cùng khoa/phòng");
        }

        java.sql.Date startOfDay = new java.sql.Date(getStartOfDay().getTime());
        java.sql.Date endOfDay = new java.sql.Date(getEndOfDay().getTime());
        if (chamCongRepository.findByNhanVienAndThoiGianCheckInBetween(nhanVien, startOfDay, endOfDay).isPresent()) {
            throw new IllegalStateException("Nhân viên này đã được check-in trong ngày hôm nay");
        }

        ChamCong chamCong = new ChamCong();
        chamCong.setNhanVien(nhanVien);
        chamCong.setThoiGianCheckIn(new Date());

        TrangThaiChamCong trangThaiChamCong = trangThaiChamCongRepository.findByTenTrangThai(trangThai)
                .orElseThrow(() -> new IllegalStateException("Trạng thái '" + trangThai + "' không tồn tại"));
        chamCong.setTrangThaiChamCong(trangThaiChamCong);

        if (trangThai.equals("LÀM")) {
            if (caLamViecId == null) {
                throw new IllegalStateException("Phải cung cấp caLamViecId khi trạng thái là LÀM");
            }
            try {
                Long id = Long.parseLong(caLamViecId);
                CaLamViec caLamViec = caLamViecRepository.findById(id)
                        .orElseThrow(() -> new IllegalStateException("Ca làm việc với ID " + caLamViecId + " không tồn tại"));
                chamCong.setCaLamViec(caLamViec);
                chamCong.setKyHieuChamCong(caLamViec.getKyHieuChamCong());
            } catch (NumberFormatException e) {
                throw new IllegalStateException("caLamViecId phải là số hợp lệ");
            }
        } else if (trangThai.equals("NGHỈ")) {
            if (maKyHieuChamCong == null || ghiChu == null) {
                throw new IllegalStateException("Phải cung cấp maKyHieuChamCong và ghiChu khi trạng thái là NGHỈ");
            }
            KyHieuChamCong kyHieuChamCong = kyHieuChamCongRepository.findByMaKyHieu(maKyHieuChamCong)
                    .orElseThrow(() -> new IllegalStateException("Ký hiệu chấm công '" + maKyHieuChamCong + "' không tồn tại"));
            if (!kyHieuChamCong.isTrangThai()) {
                throw new IllegalStateException("Ký hiệu chấm công '" + maKyHieuChamCong + "' không được sử dụng");
            }
            chamCong.setKyHieuChamCong(kyHieuChamCong);
            chamCong.setGhiChu(ghiChu);

            // Ưu tiên sử dụng caLamViecId từ payload nếu có
            if (caLamViecId != null) {
                try {
                    Long id = Long.parseLong(caLamViecId);
                    CaLamViec caLamViec = caLamViecRepository.findById(id)
                            .orElseThrow(() -> new IllegalStateException("Ca làm việc với ID " + caLamViecId + " không tồn tại"));
                    chamCong.setCaLamViec(caLamViec);
                } catch (NumberFormatException e) {
                    throw new IllegalStateException("caLamViecId phải là số hợp lệ");
                }
            } else {
                // Nếu không có caLamViecId, lấy ca làm việc gần nhất
                ChamCong lastChamCong = chamCongRepository.findLatestWithCaLamViecByNhanVienId(nhanVien.getId())
                        .orElse(null);
                chamCong.setCaLamViec(lastChamCong != null ? lastChamCong.getCaLamViec() : null);
                System.out.println("CaLamViec for NGHỈ: " + (lastChamCong != null ? lastChamCong.getCaLamViec() : "null"));
            }
        }

        return chamCongRepository.save(chamCong);
    }

    // Các phương thức khác giữ nguyên
    public ChamCong capNhatTrangThai(Long id, String trangThai, String caLamViecId, String maKyHieuChamCong, String ghiChu) {
        ChamCong chamCong = chamCongRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Bản ghi chấm công với ID " + id + " không tồn tại"));
        TrangThaiChamCong trangThaiChamCong = trangThaiChamCongRepository.findByTenTrangThai(trangThai)
                .orElseThrow(() -> new IllegalStateException("Trạng thái '" + trangThai + "' không tồn tại"));
        chamCong.setTrangThaiChamCong(trangThaiChamCong);

        if (trangThai.equals("LÀM")) {
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
            chamCong.setGhiChu(null);
        } else if (trangThai.equals("NGHỈ")) {
            if (maKyHieuChamCong == null || ghiChu == null) {
                throw new IllegalStateException("Phải cung cấp maKyHieuChamCong và ghiChu khi trạng thái là NGHỈ");
            }
            KyHieuChamCong kyHieuChamCong = kyHieuChamCongRepository.findByMaKyHieu(maKyHieuChamCong)
                    .orElseThrow(() -> new IllegalStateException("Ký hiệu chấm công '" + maKyHieuChamCong + "' không tồn tại"));
            if (!kyHieuChamCong.isTrangThai()) {
                throw new IllegalStateException("Ký hiệu chấm công '" + maKyHieuChamCong + "' không được sử dụng");
            }
            chamCong.setKyHieuChamCong(kyHieuChamCong);
            chamCong.setGhiChu(ghiChu);

            // Ưu tiên sử dụng caLamViecId từ payload nếu có
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
                // Nếu không có caLamViecId, lấy ca làm việc gần nhất
                ChamCong lastChamCong = chamCongRepository.findLatestWithCaLamViecByNhanVienId(chamCong.getNhanVien().getId())
                        .orElse(null);
                chamCong.setCaLamViec(lastChamCong != null ? lastChamCong.getCaLamViec() : null);
                System.out.println("CaLamViec for NGHỈ (update): " + (lastChamCong != null ? lastChamCong.getCaLamViec() : "null"));
            }
        }

        return chamCongRepository.save(chamCong);
    }

    public Page<ChamCong> layLichSuChamCong(String tenDangNhap, Integer year, Integer month, Integer day, Long khoaPhongId, int page, int size) {
        User user = userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new SecurityException("Người dùng không tồn tại"));
        String role = user.getRole().getTenVaiTro();
        Long finalKhoaPhongId = khoaPhongId;

        if (role.equals("NGUOICHAMCONG") || role.equals("NGUOITONGHOP")) { // Đã có NGUOITONGHOP
            finalKhoaPhongId = user.getKhoaPhong().getId();
        } else if (role.equals("ADMIN") && khoaPhongId == null) {
            finalKhoaPhongId = null;
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<ChamCong> chamCongs = chamCongRepository.findByKhoaPhongAndDateFilters(finalKhoaPhongId, year, month, day, pageable);
        chamCongs.forEach(chamCong -> Hibernate.initialize(chamCong.getNhanVien()));
        return chamCongs;
    }


    private Date getStartOfDay() {
        Date now = new Date();
        return new Date(now.getTime() - now.getTime() % (24 * 60 * 60 * 1000));
    }

    private Date getEndOfDay() {
        Date now = new Date();
        return new Date(now.getTime() - now.getTime() % (24 * 60 * 60 * 1000) + 24 * 60 * 60 * 1000 - 1);
    }
}