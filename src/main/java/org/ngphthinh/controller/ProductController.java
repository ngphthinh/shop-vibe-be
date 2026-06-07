package org.ngphthinh.controller;

import jakarta.servlet.annotation.HttpConstraint;
import lombok.RequiredArgsConstructor;
import org.ngphthinh.dto.request.product.ProductCreateRequest;
import org.ngphthinh.dto.request.product.ProductUpdateRequest;
import org.ngphthinh.dto.response.ApiResponse;
import org.ngphthinh.dto.response.PagingResponse;
import org.ngphthinh.dto.response.product.ProductDetailResponse;
import org.ngphthinh.dto.response.product.ProductImageResponse;
import org.ngphthinh.dto.response.product.ProductResponse;
import org.ngphthinh.enums.ResponseCode;
import org.ngphthinh.service.ProductService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/products")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ApiResponse<PagingResponse<ProductResponse>> getProducts(@RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "10") int size,
                                                                    @RequestParam(defaultValue = "newest") String sort) {
        return ApiResponse.<PagingResponse<ProductResponse>>builder()
                .code(ResponseCode.PRODUCT_GET_SUCCESS.getCode())
                .message(ResponseCode.PRODUCT_GET_SUCCESS.getMessage())
                .data(productService.getAllProducts(page, size, sort))
                .build();
    }

    @GetMapping("/search")
    public ApiResponse<PagingResponse<ProductResponse>> searchProducts(@RequestParam String keyword,
                                                                       @RequestParam(defaultValue = "0") int page,
                                                                       @RequestParam(defaultValue = "10") int size,
                                                                       @RequestParam(defaultValue = "newest") String sort) {
        return ApiResponse.<PagingResponse<ProductResponse>>builder()
                .code(ResponseCode.PRODUCT_GET_SUCCESS.getCode())
                .message(ResponseCode.PRODUCT_GET_SUCCESS.getMessage())
                .data(productService.getProductsByCategoryId(keyword, page, size, sort))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductDetailResponse> getProductById(@PathVariable Long id) {
        return ApiResponse.<ProductDetailResponse>builder()
                .code(ResponseCode.PRODUCT_GET_BY_ID_SUCCESS.getCode())
                .message(ResponseCode.PRODUCT_GET_BY_ID_SUCCESS.getMessage())
                .data(productService.getProductById(id))
                .build();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductDetailResponse> createProduct(@RequestBody ProductCreateRequest request) {
        return ApiResponse.<ProductDetailResponse>builder()
                .code(ResponseCode.PRODUCT_CREATE_SUCCESS.getCode())
                .message(ResponseCode.PRODUCT_CREATE_SUCCESS.getMessage())
                .data(productService.createProduct(request))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductDetailResponse> updateProduct(@PathVariable Long id, @RequestBody ProductUpdateRequest request) {
        return ApiResponse.<ProductDetailResponse>builder()
                .code(ResponseCode.PRODUCT_UPDATE_SUCCESS.getCode())
                .message(ResponseCode.PRODUCT_UPDATE_SUCCESS.getMessage())
                .data(productService.updateProduct(id, request))
                .build();
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
    }

    @DeleteMapping("/{id}/images/{imgId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProductImage(@PathVariable Long id, @PathVariable Long imgId) {
        productService.deleteProductImage(id, imgId);
    }


    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/{id}/images")
    public ApiResponse<List<ProductImageResponse>> addProductImage(@PathVariable("id") Long productId,
                                                                   @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        return ApiResponse.<List<ProductImageResponse>>builder()
                .code(ResponseCode.PRODUCT_UPDATE_SUCCESS.getCode())
                .message(ResponseCode.PRODUCT_UPDATE_SUCCESS.getMessage())
                .data(productService.saveProductImages(images, productId))
                .build();
    }
}