package com.hospital.attendance.Service;

import com.hospital.attendance.Entity.ChamCong;
import com.hospital.attendance.Entity.NhanVien;
import com.hospital.attendance.Entity.TrangThaiChamCong;
import com.hospital.attendance.Entity.User;
import com.hospital.attendance.Repository.ChamCongRepository;
import com.hospital.attendance.Repository.NhanVienRepository;
import com.hospital.attendance.Repository.UserRepository;
import com.hospital.attendance.Repository.TrangThaiChamCongRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

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

    public ChamCong checkIn(String tenDangNhapChamCong, String maNhanVien, String hoTen, String emailNhanVien) {
        // Lấy thông tin người chấm công từ User
        User chamCong = userRepository.findByTenDangNhap(tenDangNhapChamCong)
                .orElseThrow(() -> new SecurityException("Người chấm công không tồn tại"));

        String vaiTroChamCong = chamCong.getRole().getTenVaiTro();
        if (!vaiTroChamCong.equals("ADMIN") && !vaiTroChamCong.equals("NGUOICHAMCONG") && !vaiTroChamCong.equals("NGUOITONGHOP")) {
            throw new SecurityException("Bạn không có quyền chấm công");
        }

        // Tìm NhanVien dựa trên ma, hoTen, hoặc email
        NhanVien nhanVien = null;
        if (maNhanVien != null) {
            try {
                Long ma = Long.parseLong(maNhanVien);
                nhanVien = nhanVienRepository.findById(ma)
                        .orElseThrow(() -> new SecurityException("Nhân viên với ID " + ma + " không tồn tại"));
            } catch (NumberFormatException e) {
                throw new SecurityException("maNhanVien phải là số hợp lệ");
            }
        } else if (hoTen != null) {
            nhanVien = nhanVienRepository.findByHoTen(hoTen)
                    .orElseThrow(() -> new SecurityException("Nhân viên với họ tên '" + hoTen + "' không tồn tại"));
        } else if (emailNhanVien != null) {
            nhanVien = nhanVienRepository.findByEmail(emailNhanVien)
                    .orElseThrow(() -> new SecurityException("Nhân viên với email '" + emailNhanVien + "' không tồn tại"));
        } else {
            throw new SecurityException("Thiếu thông tin nhân viên (maNhanVien, hoTen, hoặc emailNhanVien)");
        }

        // Kiểm tra nếu là NGUOICHAMCONG, phải cùng phòng ban
        if (vaiTroChamCong.equals("NGUOICHAMCONG") && !chamCong.getKhoaPhong().getId().equals(nhanVien.getKhoaPhong().getId())) {
            throw new SecurityException("Chỉ được chấm công cho nhân viên cùng khoa/phòng");
        }

        // Kiểm tra check-in trùng ngày
        java.sql.Date startOfDay = new java.sql.Date(getStartOfDay().getTime());
        java.sql.Date endOfDay = new java.sql.Date(getEndOfDay().getTime());
        if (chamCongRepository.findByNhanVienAndThoiGianCheckInBetween(nhanVien, startOfDay, endOfDay).isPresent()) {
            throw new IllegalStateException("Nhân viên này đã được check-in rồi"); // Cập nhật thông báo
        }

        ChamCong chamCongEntity = new ChamCong();
        chamCongEntity.setNhanVien(nhanVien);
        chamCongEntity.setThoiGianCheckIn(new java.util.Date());
        TrangThaiChamCong trangThaiChoDuyet = trangThaiChamCongRepository.findByTenTrangThai("CHO_DUYET")
                .orElseGet(() -> trangThaiChamCongRepository.save(new TrangThaiChamCong("CHO_DUYET")));
        chamCongEntity.setTrangThaiChamCong(trangThaiChoDuyet);
        return chamCongRepository.save(chamCongEntity);
    }

    public List<ChamCong> layLichSuChamCong(String tenDangNhap) {
        User user = userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new SecurityException("Người dùng không tồn tại"));
        return chamCongRepository.findByNhanVienKhoaPhongId(user.getKhoaPhong().getId());
    }

    public ChamCong capNhatTrangThai(Long ma, String tenTrangThai) {
        ChamCong chamCong = chamCongRepository.findById(ma)
                .orElseThrow(() -> new IllegalStateException("Bản ghi chấm công với ID " + ma + " không tồn tại"));
        TrangThaiChamCong trangThai = trangThaiChamCongRepository.findByTenTrangThai(tenTrangThai)
                .orElseThrow(() -> new IllegalStateException("Trạng thái '" + tenTrangThai + "' không tồn tại"));
        chamCong.setTrangThaiChamCong(trangThai);
        return chamCongRepository.save(chamCong);
    }

    public List<ChamCong> tongHopChamCongTheoPhongBan(Long khoaPhongId) {
        return chamCongRepository.findByNhanVienKhoaPhongId(khoaPhongId);
    }

    private java.util.Date getStartOfDay() {
        java.util.Date now = new java.util.Date();
        return new java.util.Date(now.getTime() - now.getTime() % (24 * 60 * 60 * 1000));
    }

    private java.util.Date getEndOfDay() {
        java.util.Date now = new java.util.Date();
        return new java.util.Date(now.getTime() - now.getTime() % (24 * 60 * 60 * 1000) + 24 * 60 * 60 * 1000 - 1);
    }
}