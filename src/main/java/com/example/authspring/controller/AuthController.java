package com.example.authspring.controller;


import com.example.authspring.dtos.request.LoginRequestDto;
import com.example.authspring.dtos.request.RegisterRequestDto;
import com.example.authspring.dtos.response.LoginResponseDto;
import com.example.authspring.dtos.response.RegisterResponseDto;
import com.example.authspring.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/auth")
public class AuthController {

    private AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDto> register(@RequestBody RegisterRequestDto registerRequestDto) {

        RegisterResponseDto response = authService.register (registerRequestDto);

        // to visualize in the terminal
        // System.out.println("Register Success\n" + response);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public  ResponseEntity<LoginResponseDto>  login(@RequestBody LoginRequestDto loginRequestDto) {
       LoginResponseDto response = authService.login(loginRequestDto);
       // to visualize in the terminal
       //  System.out.println("Login Success\n" + response);
       return ResponseEntity.ok(response);
    }

    }

