package com.example.authspring.dtos.request;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequestDto {

   private String username;
   private String password;
   private String email;
}
