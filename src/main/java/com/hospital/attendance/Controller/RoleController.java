package com.hospital.attendance.Controller;

import com.hospital.attendance.Entity.Role;
import com.hospital.attendance.Repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    @Autowired
    private RoleRepository roleRepository;

    @PostMapping
    public ResponseEntity<?> createRole(@RequestBody Role role) {
        if (roleRepository.findByTenVaiTro(role.getTenVaiTro()).isPresent()) {
            return ResponseEntity.badRequest().body("Vai trò '" + role.getTenVaiTro() + "' đã tồn tại");
        }
        Role savedRole = roleRepository.save(role);
        return ResponseEntity.ok(savedRole);
    }

    @GetMapping
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @PutMapping("/{id}")
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
    public ResponseEntity<?> deleteRole(@PathVariable Long id) {
        Optional<Role> role = roleRepository.findById(id);
        if (role.isEmpty()) {
            return ResponseEntity.badRequest().body("Vai trò không tồn tại");
        }
        roleRepository.deleteById(id);
        return ResponseEntity.ok("Xóa vai trò thành công");
    }
}