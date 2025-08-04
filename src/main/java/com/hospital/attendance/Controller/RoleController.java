package com.hospital.attendance.Controller;

import com.hospital.attendance.Entity.Role;
import com.hospital.attendance.Repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/role") // *** SỬA TỪ "/api/roles" THÀNH "/role" ***
public class RoleController {

    @Autowired
    private RoleRepository roleRepository;

    // *** THÊM PHÂN QUYỀN CHO TẤT CẢ ENDPOINT ***
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')") // Chỉ ADMIN mới được xem roles
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')") // Chỉ ADMIN mới được tạo role
    public ResponseEntity<?> createRole(@RequestBody Role role) {
        if (roleRepository.findByTenVaiTro(role.getTenVaiTro()).isPresent()) {
            return ResponseEntity.badRequest().body("Vai trò '" + role.getTenVaiTro() + "' đã tồn tại");
        }
        Role savedRole = roleRepository.save(role);
        return ResponseEntity.ok(savedRole);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')") // Chỉ ADMIN mới được sửa role
    public ResponseEntity<?> updateRole(@PathVariable Long id, @RequestBody Role role) {
        Optional<Role> existingRole = roleRepository.findById(id);
        if (existingRole.isEmpty()) {
            return ResponseEntity.badRequest().body("Vai trò không tồn tại");
        }
        if (roleRepository.findByTenVaiTro(role.getTenVaiTro()).isPresent() &&
                !roleRepository.findByTenVaiTro(role.getTenVaiTro()).get().getId().equals(id)) {
            return ResponseEntity.badRequest().body("Tên vai trò '" + role.getTenVaiTro() + "' đã được sử dụng");
        }
        role.setId(id);
        Role updatedRole = roleRepository.save(role);
        return ResponseEntity.ok(updatedRole);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')") // Chỉ ADMIN mới được xóa role
    public ResponseEntity<?> deleteRole(@PathVariable Long id) {
        Optional<Role> role = roleRepository.findById(id);
        if (role.isEmpty()) {
            return ResponseEntity.badRequest().body("Vai trò không tồn tại");
        }

        // *** THÊM: Kiểm tra xem role có đang được sử dụng không ***
        // Uncomment dòng dưới nếu bạn muốn kiểm tra
        // if (userRepository.existsByRoleId(id)) {
        //     return ResponseEntity.badRequest().body("Không thể xóa vai trò đang được sử dụng");
        // }

        roleRepository.deleteById(id);
        return ResponseEntity.ok("Xóa vai trò thành công");
    }
}