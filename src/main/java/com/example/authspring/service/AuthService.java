package com.example.authspring.service;

import com.example.authspring.dtos.request.LoginRequestDto;
import com.example.authspring.dtos.request.RegisterRequestDto;
import com.example.authspring.dtos.response.LoginResponseDto;
import com.example.authspring.dtos.response.RegisterResponseDto;

public interface AuthService {

    RegisterResponseDto register(RegisterRequestDto registerRequestDto);
    LoginResponseDto login(LoginRequestDto loginRequestDto);
}
