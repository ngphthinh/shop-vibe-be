package org.ngphthinh.dto.response.category;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<CategoryResponse> subCategories;

    public CategoryResponse(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}
