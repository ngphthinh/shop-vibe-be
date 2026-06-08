package org.ngphthinh.dto.request.review;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "Request body for creating or updating a review")
public class ReviewRequest {
    @Schema(description = "Rating given by the user", example = "4")
    private Integer rating;
    @Schema(description = "Comment about the product", example = "Great product!")
    private String comment;
}
