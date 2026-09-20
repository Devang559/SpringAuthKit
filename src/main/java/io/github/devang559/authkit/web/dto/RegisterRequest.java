package io.github.devang559.authkit.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @Email(message = "Email must be valid")
    private String email;

    private String username;

    @Size(min = 7, max = 30, message = "Phone must be between 7 and 30 characters")
    private String phone;

    @NotBlank(message = "Password is required")
    private String password;

    private String firstName;

    private String lastName;

    private String displayName;

    private String avatar;
}
