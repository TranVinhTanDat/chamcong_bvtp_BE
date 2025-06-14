package com.hospital.attendance.Service;

import com.hospital.attendance.Entity.User;
import com.hospital.attendance.Entity.Role;
import com.hospital.attendance.Repository.UserRepository;
import com.hospital.attendance.Repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public UserDetails loadUserByUsername(String tenDangNhap) throws UsernameNotFoundException {
        User user = userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với tên đăng nhập: " + tenDangNhap));

        String vaiTro = user.getRole().getTenVaiTro();
        return org.springframework.security.core.userdetails.User.withUsername(user.getTenDangNhap())
                .password(user.getMatKhau())
                .roles(vaiTro)
                .build();
    }

    public User findByUsername(String tenDangNhap) {
        return userRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với tên đăng nhập: " + tenDangNhap));
    }

    @Transactional
    public User saveEmployee(User user) {
        Optional<User> existingByUsername = userRepository.findByTenDangNhap(user.getTenDangNhap());
        if (existingByUsername.isPresent()) {
            throw new IllegalStateException("Tên đăng nhập '" + user.getTenDangNhap() + "' đã tồn tại");
        }
        Optional<User> existingByEmail = userRepository.findByEmail(user.getEmail());
        if (existingByEmail.isPresent()) {
            throw new IllegalStateException("Email '" + user.getEmail() + "' đã tồn tại");
        }
        if (user.getKhoaPhong() == null || user.getKhoaPhong().getId() == null) {
            Role defaultRole = roleRepository.findByTenVaiTro("NGUOICHAMCONG")
                    .orElseThrow(() -> new IllegalStateException("Vai trò mặc định NGUOICHAMCONG không tồn tại"));
            user.setRole(defaultRole);
        } else {
            Role role = roleRepository.findByTenVaiTro(user.getRole().getTenVaiTro())
                    .orElseThrow(() -> new IllegalStateException("Vai trò '" + user.getRole().getTenVaiTro() + "' không tồn tại"));
            user.setRole(role);
        }
        return userRepository.save(user);
    }
}