package org.ngphthinh.service;

import lombok.RequiredArgsConstructor;
import org.ngphthinh.dto.request.category.CategoryRequest;
import org.ngphthinh.dto.response.category.CategoryResponse;
import org.ngphthinh.entity.Category;
import org.ngphthinh.exception.AppException;
import org.ngphthinh.exception.ErrorCode;
import org.ngphthinh.mapper.CategoryMapper;
import org.ngphthinh.repository.CategoryRepository;
import org.ngphthinh.repository.ProductRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "'categories:all'")
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findByParentCategoryIsNull().stream()
                .map(categoryMapper::toCategoryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        return categoryMapper.toCategoryResponse(category);
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public CategoryResponse createCategory(CategoryRequest request) {
        Category category = categoryMapper.toCategory(request.getName());
        if (request.getParentCategoryId() != null) {
            Category parentCategory = categoryRepository.findById(request.getParentCategoryId()).orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

            if (productRepository.existsByCategoryIdAndIsDeletedFalse(request.getParentCategoryId())) {
                throw new AppException(ErrorCode.PARENT_CATEGORY_HAS_PRODUCTS);
            }

            category.setParentCategory(parentCategory);
        }
        Category savedCategory = categoryRepository.save(category);
        return categoryMapper.toCategoryResponse(savedCategory);
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
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

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        // Chỉ cho phép đổi tên tự do
        category.setName(request.getName());

        if (request.getParentCategoryId() != null) {
            // ── Đổi parent ──────────────────────────────────────

            Category parentCategory = categoryRepository
                    .findById(request.getParentCategoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.PARENT_CATEGORY_NOT_FOUND));

            // Check circular reference
            if (isCircularReference(id, request.getParentCategoryId())) {
                throw new AppException(ErrorCode.CATEGORY_CIRCULAR_REFERENCE);
            }



            category.setParentCategory(parentCategory);

        } else {
            // parentCategoryId = null => muốn làm category này thành category gốc, không có cha

            // Kiểm tra nếu có product thì không được làm cha
            if (productRepository.existsByCategoryIdAndIsDeletedFalse(id)) {
                throw new AppException(ErrorCode.CATEGORY_HAS_PRODUCTS);
            }

            category.setParentCategory(null);
        }

        return categoryMapper.toCategoryResponse(categoryRepository.save(category));
    }


    public Category findCategoryById(Long id) {
        return categoryRepository.findById(id).orElse(null);
    }

    private boolean isCircularReference(Long categoryId, Long newParentId) {
        Long current = newParentId;
        while (current != null) {
            if (current.equals(categoryId)) return true;
            current = categoryRepository.findParentIdById(current);
        }
        return false;
    }
}