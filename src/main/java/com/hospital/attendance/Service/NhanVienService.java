package com.hospital.attendance.Service;

import com.hospital.attendance.Entity.NhanVien;
import com.hospital.attendance.Entity.User;
import com.hospital.attendance.Repository.ChucVuRepository;
import com.hospital.attendance.Repository.KhoaPhongRepository;
import com.hospital.attendance.Repository.NhanVienRepository;
import com.hospital.attendance.Repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(NhanVienService.class);

    @Autowired
    private NhanVienRepository nhanVienRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KhoaPhongRepository khoaPhongRepository;

    @Autowired
    private ChucVuRepository chucVuRepository;

    @Transactional
    public NhanVien saveNhanVien(NhanVien nhanVien, String tenDangNhap) {
        logger.info("Saving NhanVien with email: {}", nhanVien.getEmail());
        User user = userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new SecurityException("Người dùng không tồn tại"));
        logger.info("User: {}, Role: {}, UserKhoaPhongId: {}", tenDangNhap, user.getRole().getTenVaiTro(), user.getKhoaPhong().getId());

        // Kiểm tra khoa/phòng
        if (nhanVien.getKhoaPhong() == null || nhanVien.getKhoaPhong().getId() == null) {
            throw new IllegalStateException("Phải cung cấp thông tin khoa/phòng hợp lệ (khoa_phong_id)");
        }
        Long khoaPhongId = nhanVien.getKhoaPhong().getId();
        logger.info("Requested KhoaPhongId: {}", khoaPhongId);
        if (!khoaPhongRepository.existsById(khoaPhongId)) {
            throw new IllegalStateException("Khoa/phòng với ID " + khoaPhongId + " không tồn tại");
        }

        // Kiểm tra chức vụ (nếu có)
        if (nhanVien.getChucVu() != null && nhanVien.getChucVu().getId() != null) {
            Long chucVuId = nhanVien.getChucVu().getId();
            logger.info("Requested ChucVuId: {}", chucVuId);
            if (!chucVuRepository.existsById(chucVuId)) {
                throw new IllegalStateException("Chức vụ với ID " + chucVuId + " không tồn tại");
            }
            nhanVien.setChucVu(chucVuRepository.findById(chucVuId).get());
        } else {
            nhanVien.setChucVu(null);
            logger.info("ChucVu set to null");
        }

        // Kiểm tra quyền NGUOICHAMCONG
        if (user.getRole().getTenVaiTro().equals("NGUOICHAMCONG") &&
                !user.getKhoaPhong().getId().equals(khoaPhongId)) {
            logger.error("NGUOICHAMCONG permission check failed: UserKhoaPhongId: {}, RequestedKhoaPhongId: {}",
                    user.getKhoaPhong().getId(), khoaPhongId);
            throw new SecurityException("Chỉ được thêm nhân viên thuộc khoa/phòng của bạn");
        }

        // Kiểm tra email trùng lặp
        Optional<NhanVien> existingByEmail = nhanVienRepository.findByEmailAndTrangThai(nhanVien.getEmail(), 1);
        if (existingByEmail.isPresent()) {
            throw new IllegalStateException("Email '" + nhanVien.getEmail() + "' đã tồn tại");
        }

        // Kiểm tra mã NV trùng lặp
        if (nhanVien.getMaNV() != null && !nhanVien.getMaNV().trim().isEmpty()) {
            Optional<NhanVien> existingByMaNV = nhanVienRepository.findByMaNVAndTrangThai(nhanVien.getMaNV().trim(), 1);
            if (existingByMaNV.isPresent()) {
                throw new IllegalStateException("Mã nhân viên '" + nhanVien.getMaNV() + "' đã tồn tại");
            }
            nhanVien.setMaNV(nhanVien.getMaNV().trim()); // Trim whitespace
        }

        // THÊM MỚI: Kiểm tra số điện thoại trùng lặp
        if (nhanVien.getSoDienThoai() != null && !nhanVien.getSoDienThoai().trim().isEmpty()) {
            Optional<NhanVien> existingBySDT = nhanVienRepository.findBySoDienThoaiAndTrangThai(nhanVien.getSoDienThoai().trim(), 1);
            if (existingBySDT.isPresent()) {
                throw new IllegalStateException("Số điện thoại '" + nhanVien.getSoDienThoai() + "' đã tồn tại");
            }
            nhanVien.setSoDienThoai(nhanVien.getSoDienThoai().trim()); // Trim whitespace
        }

        // Gán khoa/phòng và trạng thái
        nhanVien.setKhoaPhong(khoaPhongRepository.findById(khoaPhongId).get());
        nhanVien.setTrangThai(1); // Nhân viên mới luôn hoạt động
        logger.info("Saving NhanVien to database");
        return nhanVienRepository.save(nhanVien);
    }

    @Transactional
    public NhanVien updateNhanVien(Long id, NhanVien nhanVienDetails, String tenDangNhap) {
        logger.info("Updating NhanVien with ID: {}", id);
        User user = userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new SecurityException("Người dùng không tồn tại"));
        NhanVien nhanVien = nhanVienRepository.findByIdAndTrangThai(id, 1)
                .orElseThrow(() -> new IllegalStateException("Nhân viên với ID " + id + " không tồn tại hoặc đã bị vô hiệu hóa"));

        // Kiểm tra khoa/phòng
        if (nhanVienDetails.getKhoaPhong() == null || nhanVienDetails.getKhoaPhong().getId() == null) {
            throw new IllegalStateException("Phải cung cấp thông tin khoa/phòng hợp lệ (khoa_phong_id)");
        }
        Long khoaPhongId = nhanVienDetails.getKhoaPhong().getId();
        logger.info("Requested KhoaPhongId: {}", khoaPhongId);
        if (!khoaPhongRepository.existsById(khoaPhongId)) {
            throw new IllegalStateException("Khoa/phòng với ID " + khoaPhongId + " không tồn tại");
        }

        // Kiểm tra chức vụ (nếu có)
        if (nhanVienDetails.getChucVu() != null && nhanVienDetails.getChucVu().getId() != null) {
            Long chucVuId = nhanVienDetails.getChucVu().getId();
            logger.info("Requested ChucVuId: {}", chucVuId);
            if (!chucVuRepository.existsById(chucVuId)) {
                throw new IllegalStateException("Chức vụ với ID " + chucVuId + " không tồn tại");
            }
            nhanVien.setChucVu(chucVuRepository.findById(chucVuId).get());
        } else {
            nhanVien.setChucVu(null);
            logger.info("ChucVu set to null");
        }

        // Kiểm tra quyền NGUOICHAMCONG
        if (user.getRole().getTenVaiTro().equals("NGUOICHAMCONG") &&
                !user.getKhoaPhong().getId().equals(khoaPhongId)) {
            logger.error("NGUOICHAMCONG permission check failed: UserKhoaPhongId: {}, RequestedKhoaPhongId: {}",
                    user.getKhoaPhong().getId(), khoaPhongId);
            throw new SecurityException("Chỉ được cập nhật nhân viên thuộc khoa/phòng của bạn");
        }

        // Kiểm tra email trùng lặp nếu thay đổi
        if (!nhanVien.getEmail().equals(nhanVienDetails.getEmail())) {
            Optional<NhanVien> existingByEmail = nhanVienRepository.findByEmailAndTrangThai(nhanVienDetails.getEmail(), 1);
            if (existingByEmail.isPresent()) {
                throw new IllegalStateException("Email '" + nhanVienDetails.getEmail() + "' đã tồn tại");
            }
        }

        // Kiểm tra mã NV trùng lặp nếu thay đổi
        if (nhanVienDetails.getMaNV() != null && !nhanVienDetails.getMaNV().trim().isEmpty()) {
            String newMaNV = nhanVienDetails.getMaNV().trim();
            if (nhanVien.getMaNV() == null || !nhanVien.getMaNV().equals(newMaNV)) {
                Optional<NhanVien> existingByMaNV = nhanVienRepository.findByMaNVAndTrangThai(newMaNV, 1);
                if (existingByMaNV.isPresent()) {
                    throw new IllegalStateException("Mã nhân viên '" + newMaNV + "' đã tồn tại");
                }
            }
            nhanVien.setMaNV(newMaNV);
        } else {
            nhanVien.setMaNV(null); // Cho phép để trống
        }

        // THÊM MỚI: Kiểm tra số điện thoại trùng lặp nếu thay đổi
        if (nhanVienDetails.getSoDienThoai() != null && !nhanVienDetails.getSoDienThoai().trim().isEmpty()) {
            String newSDT = nhanVienDetails.getSoDienThoai().trim();
            if (nhanVien.getSoDienThoai() == null || !nhanVien.getSoDienThoai().equals(newSDT)) {
                Optional<NhanVien> existingBySDT = nhanVienRepository.findBySoDienThoaiAndTrangThai(newSDT, 1);
                if (existingBySDT.isPresent()) {
                    throw new IllegalStateException("Số điện thoại '" + newSDT + "' đã tồn tại");
                }
            }
            nhanVien.setSoDienThoai(newSDT);
        } else {
            nhanVien.setSoDienThoai(null); // Cho phép để trống
        }

        nhanVien.setHoTen(nhanVienDetails.getHoTen());
        nhanVien.setEmail(nhanVienDetails.getEmail());
        nhanVien.setNgayThangNamSinh(nhanVienDetails.getNgayThangNamSinh());
        nhanVien.setKhoaPhong(khoaPhongRepository.findById(khoaPhongId).get());
        logger.info("Updating NhanVien in database");
        return nhanVienRepository.save(nhanVien);
    }

    public Page<NhanVien> getAllNhanVien(String tenDangNhap, int page, int size, Long khoaPhongId) {
        User user = userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new SecurityException("Người dùng không tồn tại"));
        Long finalKhoaPhongId = khoaPhongId;

        logger.info("Fetching NhanVien for User: {}, Role: {}, KhoaPhongId: {}",
                tenDangNhap, user.getRole().getTenVaiTro(), finalKhoaPhongId);
        if (user.getRole().getTenVaiTro().equals("NGUOICHAMCONG")) {
            finalKhoaPhongId = user.getKhoaPhong().getId();
        } else if (user.getRole().getTenVaiTro().equals("ADMIN") && khoaPhongId == null) {
            finalKhoaPhongId = null;
        }

        Pageable pageable = PageRequest.of(page, size);
        return nhanVienRepository.findByKhoaPhongIdAndTrangThai(finalKhoaPhongId, 1, pageable);
    }

    public Optional<NhanVien> getNhanVienById(Long id) {
        logger.info("Fetching NhanVien with ID: {}", id);
        return nhanVienRepository.findByIdAndTrangThai(id, 1);
    }

    @Transactional
    public void disableNhanVien(Long id, String tenDangNhap) {
        User user = userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new SecurityException("Người dùng không tồn tại"));
        NhanVien nhanVien = nhanVienRepository.findByIdAndTrangThai(id, 1)
                .orElseThrow(() -> new IllegalStateException("Nhân viên với ID " + id + " không tồn tại hoặc đã bị vô hiệu hóa"));

        logger.info("Disabling NhanVien with ID: {}, User Role: {}, User KhoaPhongId: {}",
                id, user.getRole().getTenVaiTro(), user.getKhoaPhong().getId());
        if (user.getRole().getTenVaiTro().equals("NGUOICHAMCONG") &&
                !user.getKhoaPhong().getId().equals(nhanVien.getKhoaPhong().getId())) {
            throw new SecurityException("Chỉ được vô hiệu hóa nhân viên thuộc khoa/phòng của bạn");
        }
        nhanVien.setTrangThai(0);
        nhanVienRepository.save(nhanVien);
    }

    public List<NhanVien> getNhanVienByPhongBanId(String tenDangNhap, Long khoaPhongId) {
        User user = userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new SecurityException("Người dùng không tồn tại"));
        logger.info("Fetching NhanVien for KhoaPhongId: {}, User Role: {}, User KhoaPhongId: {}",
                khoaPhongId, user.getRole().getTenVaiTro(), user.getKhoaPhong().getId());
        if (user.getRole().getTenVaiTro().equals("NGUOICHAMCONG") &&
                !user.getKhoaPhong().getId().equals(khoaPhongId)) {
            throw new SecurityException("Chỉ được xem nhân viên thuộc khoa/phòng của bạn");
        }
        return nhanVienRepository.findByKhoaPhongIdAndTrangThai(khoaPhongId, 1);
    }
}