package com.example.authspring.service.impl;

import com.example.authspring.dtos.request.LoginRequestDto;
import com.example.authspring.dtos.request.RegisterRequestDto;
import com.example.authspring.dtos.response.LoginResponseDto;
import com.example.authspring.dtos.response.RegisterResponseDto;
import com.example.authspring.entity.User;
import com.example.authspring.entity.enums.UserRole;
import com.example.authspring.repository.UserRepository;
import com.example.authspring.security.JwtService;
import com.example.authspring.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service

public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;


    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    public RegisterResponseDto register(RegisterRequestDto reqDto){

        if (userRepository.findByUsername(reqDto.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.findByEmail(reqDto.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }
        User user = new User();
        user.setUsername(reqDto.getUsername());
        user.setEmail(reqDto.getEmail());
        user.setPassword(passwordEncoder.encode(reqDto.getPassword()));
        user.setRole(UserRole.CLIENT);
        userRepository.save(user);

        return  new RegisterResponseDto(user.getUsername(), user.getEmail());
    }

    @Override
    public LoginResponseDto login(LoginRequestDto resDto){
        if(resDto == null || resDto.getUsername() == null){
            throw new RuntimeException("Usernamae is Empty!");
        }
        User user = userRepository.findByUsername(resDto.getUsername()).orElseThrow(

                ()-> new RuntimeException("Username not found!")
        ) ;

        Boolean valid = passwordEncoder.matches(resDto.getPassword(), user.getPassword());

        if(!valid ){
            throw new RuntimeException("Invalid password!");
        }
        String token = jwtService.generateToken(user);
        return new LoginResponseDto(user.getUsername(),token);
    }
}
