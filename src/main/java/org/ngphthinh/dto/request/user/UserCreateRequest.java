package org.ngphthinh.dto.request.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Schema(description = "Request object for creating a new user")
public class UserCreateRequest {
    @Schema(description = "Full name of the user", example = "John Doe")
    @NotBlank(message = "Full name is required")
    private String fullName;
    @Schema(description = "Email of the user", example = "john.doe@example.com")
    @NotBlank(message = "Email is required")
    private String email;

    @Schema(description = "Password of the user", example = "password123")
    @NotBlank(message = "Password is required")
    private String password;

    @Schema(description = "Phone number of the user", example = "123-456-7890")
    @NotBlank(message = "Phone number is required")
    private String phone;
}
