package org.ngphthinh.controller;

import lombok.RequiredArgsConstructor;
import org.ngphthinh.dto.request.category.CategoryRequest;
import org.ngphthinh.dto.response.ApiResponse;
import org.ngphthinh.dto.response.category.CategoryResponse;
import org.ngphthinh.enums.ResponseCode;
import org.ngphthinh.service.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/categories")
public class CategoryController {


    private final CategoryService categoryService;

    @GetMapping
    public ApiResponse<List<CategoryResponse>> getCategories() {

        return ApiResponse.<List<CategoryResponse>>builder()
                .code(ResponseCode.CATEGORY_GET_SUCCESS.getCode())
                .message(ResponseCode.CATEGORY_GET_SUCCESS.getMessage())
                .data(categoryService.getAllCategories())
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<CategoryResponse> getCategoryById(@PathVariable Long id) {
        {
            return ApiResponse.<CategoryResponse>builder()
                    .code(ResponseCode.CATEGORY_GET_BY_ID_SUCCESS.getCode())
                    .message(ResponseCode.CATEGORY_GET_BY_ID_SUCCESS.getMessage())
                    .data(categoryService.getCategoryById(id))
                    .build();
        }
    }


    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ApiResponse<CategoryResponse> createCategory(@RequestBody CategoryRequest request) {
        return ApiResponse.<CategoryResponse>builder()
                .code(ResponseCode.CATEGORY_CREATE_SUCCESS.getCode())
                .message(ResponseCode.CATEGORY_CREATE_SUCCESS.getMessage())
                .data(categoryService.createCategory(request))
                .build();
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
    }


    @PutMapping("/{id}")
    public ApiResponse<CategoryResponse> updateCategory(@PathVariable Long id, @RequestBody CategoryRequest request) {
        return ApiResponse.<CategoryResponse>builder()
                .code(ResponseCode.CATEGORY_UPDATE_SUCCESS.getCode())
                .message(ResponseCode.CATEGORY_UPDATE_SUCCESS.getMessage())
                .data(categoryService.updateCategory(id, request))
                .build();
    }
}