package org.ngphthinh.dto.request.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Schema(description = "Request object for updating user information - all fields are optional, only provide the fields that need to be updated")
public class UserUpdateRequest {
    @Schema(description = "Full name of the user", example = "John Doe")
    private String fullName;
    @Schema(description = "Phone number of the user", example = "123-456-7890")
    private String phone;
    @Schema(description = "Address of the user", example = "123 Main St, City, State 12345")
    private String address;
}
