package com.ifsp.users.controllers;

import com.ifsp.users.dtos.*;
import com.ifsp.users.entities.User;
import com.ifsp.users.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<Void> createUser(@RequestBody CreateUserDto dto) {
        userService.createUser(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/update-profile")
    public ResponseEntity<UserProfileDto> updateProfile(Authentication authentication, @RequestBody UpdateProfileDto dto) {
        String email = authentication.getName();
        User updated = userService.updateProfile(email, dto);

        List<String> stringRoles = updated.getRoles().stream()
                .map(role -> role.getName().name())
                .toList();

        var userProfile = new UserProfileDto(updated.getId(), updated.getEmail(), stringRoles);

        return ResponseEntity.ok(userProfile);
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(userService.getUserInformation(authentication));
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Autenticado com sucesso!");
    }

    @GetMapping("/test/customer")
    public ResponseEntity<String> customerTest() {
        return ResponseEntity.ok("Acesso de CUSTOMER autorizado!");
    }

    @GetMapping("/test/administrator")
    public ResponseEntity<String> adminTest() {
        return ResponseEntity.ok("Acesso de ADMINISTRATOR autorizado!");
    }
}