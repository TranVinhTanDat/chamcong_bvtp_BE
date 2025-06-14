package com.hospital.attendance.Service;


import com.hospital.attendance.Entity.ChamCong;
import com.hospital.attendance.Entity.NhanVien;
import com.hospital.attendance.Entity.TrangThaiChamCong;
import com.hospital.attendance.Entity.CaLamViec;
import com.hospital.attendance.Entity.LoaiNghi;
import com.hospital.attendance.Entity.User;
import com.hospital.attendance.Repository.ChamCongRepository;
import com.hospital.attendance.Repository.NhanVienRepository;
import com.hospital.attendance.Repository.UserRepository;
import com.hospital.attendance.Repository.TrangThaiChamCongRepository;
import com.hospital.attendance.Repository.CaLamViecRepository;
import com.hospital.attendance.Repository.LoaiNghiRepository;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

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
    private LoaiNghiRepository loaiNghiRepository;

    public ChamCong checkIn(String tenDangNhapChamCong, String nhanVienId, String nhanVienHoTen, String emailNhanVien,
                            String trangThai, String maCa, String maLoaiNghi, String ghiChu) {
        User chamCongUser = userRepository.findByTenDangNhap(tenDangNhapChamCong)
                .orElseThrow(() -> new SecurityException("Người chấm công không tồn tại"));

        String vaiTroChamCong = chamCongUser.getRole().getTenVaiTro();
        if (!vaiTroChamCong.equals("ADMIN") && !vaiTroChamCong.equals("NGUOICHAMCONG") && !vaiTroChamCong.equals("NGUOITONGHOP")) {
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

        if (vaiTroChamCong.equals("NGUOICHAMCONG") && !chamCongUser.getKhoaPhong().getId().equals(nhanVien.getKhoaPhong().getId())) {
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
            chamCong.setCaLamViec(null);
        } else if (trangThai.equals("NGHỈ")) {
            if (maLoaiNghi == null || ghiChu == null) {
                throw new IllegalStateException("Phải cung cấp maLoaiNghi và ghiChu khi trạng thái là NGHỈ");
            }
            LoaiNghi loaiNghi = loaiNghiRepository.findByMaLoaiNghi(maLoaiNghi)
                    .orElseThrow(() -> new IllegalStateException("Loại nghỉ '" + maLoaiNghi + "' không tồn tại"));
            if (!loaiNghi.isTrangThai()) {
                throw new IllegalStateException("Loại nghỉ '" + maLoaiNghi + "' không được sử dụng");
            }
            chamCong.setLoaiNghi(loaiNghi);
            chamCong.setGhiChu(ghiChu);
        } else {
            throw new IllegalStateException("Trạng thái phải là LÀM hoặc NGHỈ");
        }

        return chamCongRepository.save(chamCong);
    }

    public Page<ChamCong> layLichSuChamCong(String tenDangNhap, Integer year, Integer month, Integer day, Long khoaPhongId, int page, int size) {
        User user = userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new SecurityException("Người dùng không tồn tại"));
        Pageable pageable = PageRequest.of(page, size);
        Page<ChamCong> chamCongs = chamCongRepository.findByKhoaPhongAndDateFilters(khoaPhongId, year, month, day, pageable);
        chamCongs.forEach(chamCong -> Hibernate.initialize(chamCong.getNhanVien()));
        return chamCongs;
    }

    public ChamCong capNhatTrangThai(Long id, String trangThai, String maCa, String maLoaiNghi, String ghiChu) {
        ChamCong chamCong = chamCongRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Bản ghi chấm công với ID " + id + " không tồn tại"));
        TrangThaiChamCong trangThaiChamCong = trangThaiChamCongRepository.findByTenTrangThai(trangThai)
                .orElseThrow(() -> new IllegalStateException("Trạng thái '" + trangThai + "' không tồn tại"));
        chamCong.setTrangThaiChamCong(trangThaiChamCong);

        if (trangThai.equals("LÀM")) {
            chamCong.setCaLamViec(null);
            chamCong.setLoaiNghi(null);
            chamCong.setGhiChu(null);
        } else if (trangThai.equals("NGHỈ")) {
            if (maLoaiNghi == null || ghiChu == null) {
                throw new IllegalStateException("Phải cung cấp maLoaiNghi và ghiChu khi trạng thái là NGHỈ");
            }
            LoaiNghi loaiNghi = loaiNghiRepository.findByMaLoaiNghi(maLoaiNghi)
                    .orElseThrow(() -> new IllegalStateException("Loại nghỉ '" + maLoaiNghi + "' không tồn tại"));
            if (!loaiNghi.isTrangThai()) {
                throw new IllegalStateException("Loại nghỉ '" + maLoaiNghi + "' không được sử dụng");
            }
            chamCong.setLoaiNghi(loaiNghi);
            chamCong.setGhiChu(ghiChu);
            chamCong.setCaLamViec(null);
        } else {
            throw new IllegalStateException("Trạng thái phải là LÀM hoặc NGHỈ");
        }

        return chamCongRepository.save(chamCong);
    }

    public List<ChamCong> tongHopChamCongTheoPhongBan(Long khoaPhongId) {
        return chamCongRepository.findByNhanVienKhoaPhongId(khoaPhongId);
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