package com.hospital.attendance.Repository;

import com.hospital.attendance.Entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByTenDangNhap(String tenDangNhap);
    Optional<User> findByEmail(String email);

    Page<User> findByTenDangNhapContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String tenDangNhap, String email, Pageable pageable);
}