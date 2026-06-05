package org.ngphthinh.dto.request.category;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Schema(description = "Request object for creating or updating a category")
public class CategoryRequest {
    @Schema(description = "Name of the category", example = "Electronics")
    private String name;

    @Schema(description = "The ID of the parent category (optional) must be the current parent category when updating the category.", example = "1")
    private Long parentCategoryId;
}
