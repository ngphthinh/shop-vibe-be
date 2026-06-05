package org.ngphthinh.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ngphthinh.dto.request.category.CategoryRequest;
import org.ngphthinh.dto.response.category.CategoryResponse;
import org.ngphthinh.entity.Category;
import org.ngphthinh.exception.AppException;
import org.ngphthinh.exception.ErrorCode;
import org.ngphthinh.mapper.CategoryMapper;
import org.ngphthinh.repository.CategoryRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceTest {

    @InjectMocks
    private CategoryService categoryService;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    private Category parentCategory;
    private Category childCategory;
    private CategoryRequest requestWithParent;
    private CategoryResponse categoryResponseElectronics;
    private CategoryResponse categoryResponseNam;

    @BeforeEach
    void setUp() {
        parentCategory = Category.builder().id(1L).name("Thời trang").build();
        childCategory = Category.builder().id(2L).name("Nam").build();

        requestWithParent = new CategoryRequest();
        requestWithParent.setName("Nam");
        requestWithParent.setParentCategoryId(1L);

        // Tách biệt dữ liệu phản hồi cho màn Electronics
        categoryResponseElectronics = CategoryResponse.builder()
                .id(1L)
                .name("Electronics")
                .subCategories(List.of(
                        CategoryResponse.builder().id(2L).name("Mobile Phones").build(),
                        CategoryResponse.builder().id(3L).name("Laptops").build()
                ))
                .build();

        // Tách biệt dữ liệu phản hồi cho màn Thời trang/Nam để tránh đá nhau dữ liệu
        categoryResponseNam = CategoryResponse.builder()
                .id(2L)
                .name("Nam")
                .build();
    }

    // =========================================================================
    // 1. TEST CASES FOR: getCategoryById
    // =========================================================================
    @Test
    void getCategoryById_Success() {
        // GIVEN: Giả lập đúng tầng Repository và Mapper thay vì mock Service
        Long categoryId = 1L;
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(parentCategory));
        when(categoryMapper.toCategoryResponse(parentCategory)).thenReturn(categoryResponseElectronics);

        // WHEN
        CategoryResponse response = categoryService.getCategoryById(categoryId);

        // THEN
        assertNotNull(response);
        assertEquals("Electronics", response.getName());
        assertEquals(2, response.getSubCategories().size());
        verify(categoryRepository, times(1)).findById(categoryId);
    }

    @Test
    void getCategoryById_NotFound() {
        // GIVEN
        Long categoryId = 0L;
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        // WHEN & THEN
        AppException exception = assertThrows(AppException.class, () -> {
            categoryService.getCategoryById(categoryId);
        });

        assertEquals(ErrorCode.CATEGORY_NOT_FOUND, exception.getErrorCode());
        verify(categoryRepository, times(1)).findById(categoryId);
        verify(categoryMapper, never()).toCategoryResponse(any(Category.class));
    }

    // =========================================================================
    // 2. TEST CASES FOR: createCategory
    // =========================================================================
    @Test
    void createCategory_success_withParentCategory() {
        // GIVEN
        when(categoryMapper.toCategory(requestWithParent.getName())).thenReturn(childCategory);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(parentCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(childCategory);
        when(categoryMapper.toCategoryResponse(childCategory)).thenReturn(categoryResponseNam);

        // WHEN
        CategoryResponse result = categoryService.createCategory(requestWithParent);

        // THEN
        assertNotNull(result);
        assertEquals("Nam", result.getName());
        verify(categoryRepository, times(1)).findById(1L);
        verify(categoryRepository, times(1)).save(childCategory);
    }

    @Test
    void createCategory_failed_parentNotFound() {
        // GIVEN
        when(categoryMapper.toCategory(requestWithParent.getName())).thenReturn(childCategory);
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        // WHEN & THEN
        AppException exception = assertThrows(AppException.class, () -> {
            categoryService.createCategory(requestWithParent);
        });

        assertEquals(ErrorCode.CATEGORY_NOT_FOUND, exception.getErrorCode());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    // =========================================================================
    // 3. TEST CASES FOR: deleteCategory
    // =========================================================================
    @Test
    void deleteCategory_success() {
        Long catId = 2L;
        when(categoryRepository.findById(catId)).thenReturn(Optional.of(childCategory));
        when(categoryRepository.existsByIdAndSubCategoriesIsEmpty(catId)).thenReturn(true);
        when(categoryRepository.existsByIdAndProductsIsEmpty(catId)).thenReturn(true);
        doNothing().when(categoryRepository).delete(childCategory);

        assertDoesNotThrow(() -> categoryService.deleteCategory(catId));

        verify(categoryRepository, times(1)).delete(childCategory);
    }

    @Test
    void deleteCategory_failed_hasSubCategories() {
        Long catId = 2L;
        when(categoryRepository.findById(catId)).thenReturn(Optional.of(childCategory));
        when(categoryRepository.existsByIdAndSubCategoriesIsEmpty(catId)).thenReturn(false);

        AppException exception = assertThrows(AppException.class, () -> {
            categoryService.deleteCategory(catId);
        });

        assertEquals(ErrorCode.CATEGORY_HAS_SUBCATEGORIES, exception.getErrorCode());
        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    void deleteCategory_failed_hasProducts() {
        Long catId = 2L;
        when(categoryRepository.findById(catId)).thenReturn(Optional.of(childCategory));
        when(categoryRepository.existsByIdAndSubCategoriesIsEmpty(catId)).thenReturn(true);
        when(categoryRepository.existsByIdAndProductsIsEmpty(catId)).thenReturn(false);

        AppException exception = assertThrows(AppException.class, () -> {
            categoryService.deleteCategory(catId);
        });

        assertEquals(ErrorCode.CATEGORY_HAS_PRODUCTS, exception.getErrorCode());
        verify(categoryRepository, never()).delete(any(Category.class));
    }

    // =========================================================================
    // 4. TEST CASES FOR: updateCategory
    // =========================================================================
    @Test
    void updateCategory_success_changeToRootCategory() {
        Long catId = 2L;
        CategoryRequest requestToRoot = new CategoryRequest();
        requestToRoot.setName("Thời trang mới");
        requestToRoot.setParentCategoryId(null);

        // Gán sẵn cha để xem sau khi update có bị xóa gán thành null thật không
        childCategory.setParentCategory(parentCategory);

        when(categoryRepository.findById(catId)).thenReturn(Optional.of(childCategory));
        when(categoryRepository.save(childCategory)).thenReturn(childCategory);
        when(categoryMapper.toCategoryResponse(childCategory)).thenReturn(categoryResponseNam);

        categoryService.updateCategory(catId, requestToRoot);

        assertNull(childCategory.getParentCategory());
        assertEquals("Thời trang mới", childCategory.getName());
        verify(categoryRepository, times(1)).save(childCategory);
    }
}