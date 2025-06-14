package com.hospital.attendance.Service;

import com.hospital.attendance.Entity.NhanVien;
import com.hospital.attendance.Repository.NhanVienRepository;
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

    @Transactional
    public NhanVien saveNhanVien(NhanVien nhanVien) {
        Optional<NhanVien> existingByEmail = nhanVienRepository.findByEmail(nhanVien.getEmail());
        if (existingByEmail.isPresent()) {
            throw new IllegalStateException("Email '" + nhanVien.getEmail() + "' đã tồn tại");
        }
        return nhanVienRepository.save(nhanVien);
    }

    public Page<NhanVien> getAllNhanVien(int page, int size, Long khoaPhongId) {
        Pageable pageable = PageRequest.of(page, size);
        return nhanVienRepository.findByKhoaPhongId(khoaPhongId, pageable);
    }

    public Optional<NhanVien> getNhanVienById(Long id) {
        return nhanVienRepository.findById(id);
    }

    @Transactional
    public NhanVien updateNhanVien(Long id, NhanVien nhanVienDetails) {
        NhanVien nhanVien = nhanVienRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Nhân viên với ID " + id + " không tồn tại"));
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
    public void deleteNhanVien(Long id) {
        NhanVien nhanVien = nhanVienRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Nhân viên với ID " + id + " không tồn tại"));
        nhanVienRepository.delete(nhanVien);
    }

    public List<NhanVien> getNhanVienByPhongBanId(Long khoaPhongId) {
        return nhanVienRepository.findByKhoaPhongId(khoaPhongId);
    }
}