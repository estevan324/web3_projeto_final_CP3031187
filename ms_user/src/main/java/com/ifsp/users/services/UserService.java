package com.ifsp.users.services;

import com.ifsp.users.dtos.*;
import com.ifsp.users.entities.Role;
import com.ifsp.users.entities.User;
import com.ifsp.users.enums.RoleName;
import com.ifsp.users.repositories.RoleRepository;
import com.ifsp.users.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Role getOrCreateRole(RoleName roleName) {
        return roleRepository.findByName(roleName)
                .orElseGet(() -> {
                    Role newRole = Role.builder().name(roleName).build();
                    return roleRepository.save(newRole);
                });
    }

    public RecoveryJwtTokenDto authenticateUser(LoginUserDto loginDto) {
        var authToken = new UsernamePasswordAuthenticationToken(loginDto.email(),
                loginDto.password());
        var authentication =
                authenticationManager.authenticate(authToken);
        UserDetailsImpl userDetails = (UserDetailsImpl)
                authentication.getPrincipal();
        String token = jwtTokenService.generateToken(userDetails);
        return new RecoveryJwtTokenDto(token);
    }

    public void createUser(CreateUserDto createDto) {
        Role role = getOrCreateRole(createDto.role());

        User newUser = User.builder()
                .email(createDto.email())
                .password(passwordEncoder.encode(createDto.password()))
                .roles(new ArrayList<>(List.of(role)))
                .build();

        userRepository.save(newUser);
    }

    public UserProfileDto getUserInformation(Authentication authentication) {
        String email = authentication.getName();

        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        var roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .toList();

        return new UserProfileDto(user.getId(), user.getEmail(),
                roles);
    }

    public User getOrCreateUserForCode(String email) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            var randomPassword = UUID.randomUUID().toString();
            Role customerRole = getOrCreateRole(RoleName.ROLE_CUSTOMER);

            var newUser = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(randomPassword))
                    .roles(new ArrayList<>(List.of(customerRole)))
                    .build();

            return userRepository.save(newUser);
        });
    }

    public User updateProfile(String email, UpdateProfileDto dto) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        user.setName(dto.name());
        Role role = getOrCreateRole(dto.role());

        user.setRoles(new ArrayList<>(List.of(role)));

        return userRepository.save(user);
    }

    public RecoveryJwtTokenDto generateTokenByEmail(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado após validação de código"));

        var userDetails = new UserDetailsImpl(user);

        var token = jwtTokenService.generateToken(userDetails);

        return new RecoveryJwtTokenDto(token);
    }
}
