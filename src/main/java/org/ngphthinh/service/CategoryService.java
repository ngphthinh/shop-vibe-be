package org.ngphthinh.service;

import lombok.RequiredArgsConstructor;
import org.ngphthinh.dto.request.category.CategoryRequest;
import org.ngphthinh.dto.response.category.CategoryResponse;
import org.ngphthinh.entity.Category;
import org.ngphthinh.exception.AppException;
import org.ngphthinh.exception.ErrorCode;
import org.ngphthinh.mapper.CategoryMapper;
import org.ngphthinh.repository.CategoryRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findByParentCategoryIsNull().stream()
                .map(categoryMapper::toCategoryResponse)
                .toList();
    }

    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        return categoryMapper.toCategoryResponse(category);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public CategoryResponse createCategory(CategoryRequest request) {
        Category category = categoryMapper.toCategory(request.getName());
        if (request.getParentCategoryId() != null) {
            Category parentCategory = categoryRepository.findById(request.getParentCategoryId()).orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
            category.setParentCategory(parentCategory);
        }
        Category savedCategory = categoryRepository.save(category);
        return categoryMapper.toCategoryResponse(savedCategory);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void deleteCategory(Long id) {
        {
            Category category = categoryRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

            if (!categoryRepository.existsByIdAndSubCategoriesIsEmpty(id)) {
                throw new AppException(ErrorCode.CATEGORY_HAS_SUBCATEGORIES);
            }

            if (!categoryRepository.existsByIdAndProductsIsEmpty(id)) {
                throw new AppException(ErrorCode.CATEGORY_HAS_PRODUCTS);
            }

            categoryRepository.delete(category);
        }
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        category.setName(request.getName());
        if (request.getParentCategoryId() != null) {
            Category parentCategory = categoryRepository.findById(request.getParentCategoryId()).orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
            category.setParentCategory(parentCategory);
        } else {
            category.setParentCategory(null);
        }
        return categoryMapper.toCategoryResponse(categoryRepository.save(category));
    }
}