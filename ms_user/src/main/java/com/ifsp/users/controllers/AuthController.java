package com.ifsp.users.controllers;

import com.ifsp.users.dtos.*;
import com.ifsp.users.producers.UserProducer;
import com.ifsp.users.services.CodigoCacheService;
import com.ifsp.users.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final CodigoCacheService codigoCacheService;
    private final UserService userService;
    private final UserProducer userProducer;

    public AuthController(CodigoCacheService codigoCacheService, UserService userService, UserProducer userProducer) {
        this.codigoCacheService = codigoCacheService;
        this.userService = userService;
        this.userProducer = userProducer;
    }

    @PostMapping("/login")
    public ResponseEntity<RecoveryJwtTokenDto> login(@RequestBody LoginUserDto
                                                             dto) {
        RecoveryJwtTokenDto token = userService.authenticateUser(dto);
        return ResponseEntity.ok(token);
    }

    @PostMapping("/request-code")
    public ResponseEntity<Void> requestCode(@RequestBody EmailRequest request) {
        var email = request.email();
        var codigo = String.format("%06d", new Random().nextInt(999999));

        var user = userService.getOrCreateUserForCode(email);

        codigoCacheService.salvarCodigo(email, codigo);

        var emailDto = new EmailDto(
                email,
                "Seu código de acesso",
                "Seu código de verificação é: " + codigo + ". Ele expira em 5 minutos.",
                user.getId()
        );

        userProducer.sendEmail(emailDto);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@RequestBody ValidateCodeRequest request) {
        var cachedCode = codigoCacheService.obterCodigoValido(request.email());

        if (cachedCode == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Código expirado ou não solicitado."));
        }

        if (!cachedCode.equals(request.code())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Código inválido."));
        }

        codigoCacheService.removerCodigo(request.email());

        var tokenDto = userService.generateTokenByEmail(request.email());

        return ResponseEntity.ok(tokenDto);
    }
}
