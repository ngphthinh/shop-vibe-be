package org.ngphthinh.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.ngphthinh.dto.response.category.CategoryResponse;
import org.ngphthinh.entity.Category;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    default CategoryResponse toCategoryResponse(Category category) {
        if (category == null) {
            return null;
        }

        CategoryResponse categoryResponse = new CategoryResponse();
        categoryResponse.setId(category.getId());
        categoryResponse.setName(category.getName());

        if (category.getSubCategories() != null) {
            categoryResponse.setSubCategories(
                    category.getSubCategories().stream()
                            .map(this::toCategoryResponse)
                            .toList()
            );
        }

        return categoryResponse;
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "products", ignore = true)
    @Mapping(target = "parentCategory", ignore = true)
    @Mapping(target = "subCategories", ignore = true)
    Category toCategory(String name);
}

