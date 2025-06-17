package com.hospital.attendance.Service;

import com.hospital.attendance.Entity.NhanVien;
import com.hospital.attendance.Entity.User;
import com.hospital.attendance.Repository.NhanVienRepository;
import com.hospital.attendance.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class NhanVienService {

    @Autowired
    private NhanVienRepository nhanVienRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public NhanVien saveNhanVien(NhanVien nhanVien, String tenDangNhap) {
        User user = userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new SecurityException("Người dùng không tồn tại"));
        if (user.getRole().getTenVaiTro().equals("NGUOICHAMCONG") &&
                !user.getKhoaPhong().getId().equals(nhanVien.getKhoaPhong().getId())) {
            throw new SecurityException("Chỉ được thêm nhân viên thuộc khoa/phòng của bạn");
        }
        Optional<NhanVien> existingByEmail = nhanVienRepository.findByEmail(nhanVien.getEmail());
        if (existingByEmail.isPresent()) {
            throw new IllegalStateException("Email '" + nhanVien.getEmail() + "' đã tồn tại");
        }
        return nhanVienRepository.save(nhanVien);
    }

    public Page<NhanVien> getAllNhanVien(String tenDangNhap, int page, int size, Long khoaPhongId) {
        User user = userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new SecurityException("Người dùng không tồn tại"));
        Long finalKhoaPhongId = khoaPhongId;
        if (user.getRole().getTenVaiTro().equals("NGUOICHAMCONG")) {
            finalKhoaPhongId = user.getKhoaPhong().getId();
        } else if (user.getRole().getTenVaiTro().equals("ADMIN") && khoaPhongId == null) {
            finalKhoaPhongId = null;
        }
        Pageable pageable = PageRequest.of(page, size);
        return nhanVienRepository.findByKhoaPhongId(finalKhoaPhongId, pageable);
    }

    public Optional<NhanVien> getNhanVienById(Long id) {
        return nhanVienRepository.findById(id);
    }

    @Transactional
    public NhanVien updateNhanVien(Long id, NhanVien nhanVienDetails, String tenDangNhap) {
        User user = userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new SecurityException("Người dùng không tồn tại"));
        NhanVien nhanVien = nhanVienRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Nhân viên với ID " + id + " không tồn tại"));
        if (user.getRole().getTenVaiTro().equals("NGUOICHAMCONG") &&
                !user.getKhoaPhong().getId().equals(nhanVienDetails.getKhoaPhong().getId())) {
            throw new SecurityException("Chỉ được cập nhật nhân viên thuộc khoa/phòng của bạn");
        }
        nhanVien.setHoTen(nhanVienDetails.getHoTen());
        nhanVien.setEmail(nhanVienDetails.getEmail());
        nhanVien.setMaNV(nhanVienDetails.getMaNV());
        nhanVien.setNgayThangNamSinh(nhanVienDetails.getNgayThangNamSinh());
        nhanVien.setSoDienThoai(nhanVienDetails.getSoDienThoai());
        nhanVien.setChucVu(nhanVienDetails.getChucVu());
        nhanVien.setKhoaPhong(nhanVienDetails.getKhoaPhong());
        return nhanVienRepository.save(nhanVien);
    }

    @Transactional
    public void deleteNhanVien(Long id, String tenDangNhap) {
        User user = userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new SecurityException("Người dùng không tồn tại"));
        NhanVien nhanVien = nhanVienRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Nhân viên với ID " + id + " không tồn tại"));
        if (user.getRole().getTenVaiTro().equals("NGUOICHAMCONG") &&
                !user.getKhoaPhong().getId().equals(nhanVien.getKhoaPhong().getId())) {
            throw new SecurityException("Chỉ được xóa nhân viên thuộc khoa/phòng của bạn");
        }
        nhanVienRepository.delete(nhanVien);
    }

    public List<NhanVien> getNhanVienByPhongBanId(String tenDangNhap, Long khoaPhongId) {
        User user = userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new SecurityException("Người dùng không tồn tại"));
        if (user.getRole().getTenVaiTro().equals("NGUOICHAMCONG") &&
                !user.getKhoaPhong().getId().equals(khoaPhongId)) {
            throw new SecurityException("Chỉ được xem nhân viên thuộc khoa/phòng của bạn");
        }
        return nhanVienRepository.findByKhoaPhongId(khoaPhongId);
    }
}