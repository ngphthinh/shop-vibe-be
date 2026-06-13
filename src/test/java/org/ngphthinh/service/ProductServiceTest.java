package org.ngphthinh.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ngphthinh.dto.request.product.ProductCreateRequest;
import org.ngphthinh.dto.request.product.ProductUpdateRequest;
import org.ngphthinh.dto.response.PagingResponse;
import org.ngphthinh.dto.response.product.ProductDetailResponse;
import org.ngphthinh.dto.response.product.ProductImageResponse;
import org.ngphthinh.dto.response.product.ProductResponse;
import org.ngphthinh.entity.Category;
import org.ngphthinh.entity.Product;
import org.ngphthinh.entity.ProductImage;
import org.ngphthinh.exception.AppException;
import org.ngphthinh.exception.ErrorCode;
import org.ngphthinh.mapper.ProductMapper;
import org.ngphthinh.mapper.ProductImageMapper;
import org.ngphthinh.repository.ProductImageRepository;
import org.ngphthinh.repository.ProductRepository;
import org.ngphthinh.repository.projection.ProductProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private ProductImageAsyncService productImageAsyncService;

    @Mock
    private ProductImageMapper productImageMapper;

    @InjectMocks
    private ProductService productService;

    @Test
      void getProductById_whenNotFound_shouldThrow() {
        when(productRepository.findByIdWithProjection(1L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> productService.getProductById(1L));
        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void getProductsByCategoryId_shouldReturnPagingResponse_whenDataExists() {
        String keyword = "laptop";
        int page = 0;
        int size = 10;
        String sort = "id:desc";

        ProductProjection projectionMock = mock(ProductProjection.class);
        ProductResponse responseMock = new ProductResponse();

        List<ProductProjection> projections =
                Collections.singletonList(projectionMock);

        Page<ProductProjection> productPage =
                new PageImpl<>(projections, Pageable.ofSize(size), 1);

        when(productRepository.findProductsByKeyword(
                eq(keyword),
                isNull(),
                any(Pageable.class)
        )).thenReturn(productPage);

        when(productMapper.toProductResponse(any(ProductProjection.class)))
                .thenReturn(responseMock);

        PagingResponse<ProductResponse> actualResponse =
                productService.searchProducts(
                        keyword,
                        null,
                        page,
                        size,
                        sort
                );

        assertNotNull(actualResponse);
        assertEquals(1, actualResponse.getContent().size());
        assertEquals(size, actualResponse.getSize());
        assertEquals(1, actualResponse.getTotalElements());
        assertEquals(0, actualResponse.getPage());
        assertTrue(actualResponse.isLast());
        assertEquals(1, actualResponse.getTotalPages());

        verify(productRepository, times(1))
                .findProductsByKeyword(
                        eq(keyword),
                        isNull(),
                        any(Pageable.class)
                );

        verify(productMapper, times(1))
                .toProductResponse(any(ProductProjection.class));
    }

    @Test
      void getProductById_whenFound_shouldReturnDetail() {
        // use mocked projection and mapper to return a DTO
        var proj = org.mockito.Mockito.mock(org.ngphthinh.repository.projection.ProductProjection.class);
        var imgProj = org.mockito.Mockito.mock(org.ngphthinh.repository.projection.ProductImageProjection.class);

        when(productRepository.findByIdWithProjection(1L)).thenReturn(Optional.of(proj));
        when(productImageRepository.findImageUrlsByProductId(1L)).thenReturn(List.of(imgProj));

        ProductDetailResponse expected = ProductDetailResponse.builder().id(1L).name("p1").build();
        when(productMapper.toProductDetailResponse(proj, List.of(imgProj))).thenReturn(expected);

        ProductDetailResponse actual = productService.getProductById(1L);
        assertNotNull(actual);
        assertEquals(1L, actual.getId());
        assertEquals("p1", actual.getName());
    }

    @Test
      void createProduct_whenCategoryNull_shouldThrow() {
        ProductCreateRequest req = ProductCreateRequest.builder()
                .name("n")
                .categoryId(5L)
                .build();

        when(categoryService.findCategoryById(5L)).thenReturn(null);

        AppException ex = assertThrows(AppException.class, () -> productService.createProduct(req));
        assertEquals(ErrorCode.CATEGORY_NOT_FOUND, ex.getErrorCode());
    }

    @Test
      void createProduct_whenCategoryNotLeaf_shouldThrow() {
        ProductCreateRequest req = ProductCreateRequest.builder()
                .name("n")
                .categoryId(5L)
                .build();

        Category cat = new Category();
        cat.setSubCategories(List.of(new Category()));

        when(categoryService.findCategoryById(5L)).thenReturn(cat);

        AppException ex = assertThrows(AppException.class, () -> productService.createProduct(req));
        assertEquals(ErrorCode.CATEGORY_NOT_LEAF, ex.getErrorCode());
    }

    @Test
      void createProduct_whenValid_shouldReturnDetail() {
        ProductCreateRequest req = ProductCreateRequest.builder()
                .name("Test Product")
                .categoryId(5L)
                .build();

        Category cat = new Category();
        cat.setSubCategories(List.of());

        when(categoryService.findCategoryById(5L)).thenReturn(cat);

        Product product = new Product();
        product.setId(10L);
        when(productMapper.toProduct(any())).thenReturn(product);
        when(productRepository.save(product)).thenReturn(product);

        ProductDetailResponse expected = ProductDetailResponse.builder().id(10L).name("Test Product").build();
        when(productMapper.toProductDetailResponse(product)).thenReturn(expected);

        ProductDetailResponse actual = productService.createProduct(req);
        assertNotNull(actual);
        assertEquals(10L, actual.getId());
        assertEquals("Test Product", actual.getName());
    }

    @Test
    void updateProduct_shouldSuccess_whenRequestIsValid() {
        // 1. SETUP
        Long productId = 1L;
        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setCategoryId(1L);

        Product existingProduct = new Product();
        Category leafCategory = new Category();
        // Danh mục lá thì danh sách subCategories phải trống
        leafCategory.setSubCategories(Collections.emptyList());

        Product updatedProduct = new Product();
        ProductDetailResponse expectedResponse = new ProductDetailResponse();

        when(productRepository.findByIdAndIsDeletedFalse(productId)).thenReturn(Optional.of(existingProduct));
        when(categoryService.findCategoryById(1L)).thenReturn(leafCategory);

        // Mock hành vi void của Mapstruct (tiến hành update đè lên object)
        doNothing().when(productMapper).updateProductFromRequest(request, existingProduct);

        when(productRepository.save(existingProduct)).thenReturn(updatedProduct);
        when(productMapper.toProductDetailResponse(updatedProduct)).thenReturn(expectedResponse);

        // 2. ACT
        ProductDetailResponse actualResponse = productService.updateProduct(productId, request);

        // 3. ASSERT
        assertNotNull(actualResponse);
        assertSame(expectedResponse, actualResponse);

        // 4. VERIFY
        verify(productRepository).findByIdAndIsDeletedFalse(productId);
        verify(categoryService).findCategoryById(1L);
        verify(productRepository).save(existingProduct);
    }

    @Test
    void updateProduct_shouldThrowAppException_whenCategoryNotFound() {
        // 1. SETUP
        Long productId = 1L;
        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setCategoryId(1L);

        Product existingProduct = new Product();

        when(productRepository.findByIdAndIsDeletedFalse(productId)).thenReturn(Optional.of(existingProduct));
        // Giả lập không tìm thấy Category (trả về null)
        when(categoryService.findCategoryById(1L)).thenReturn(null);

        // 2. ACT & ASSERT
        AppException exception = assertThrows(AppException.class, () ->
                productService.updateProduct(productId, request)
        );

        assertEquals(ErrorCode.CATEGORY_NOT_FOUND, exception.getErrorCode());

        // 3. VERIFY
        verify(productRepository, never()).save(any(Product.class));
        verify(productMapper, never()).updateProductFromRequest(any(), any());
    }

    @Test
    void updateProduct_shouldThrowAppException_whenCategoryIsNotLeaf() {
        // 1. SETUP
        Long productId = 1L;
        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setCategoryId(1L);

        Product existingProduct = new Product();
        Category parentCategory = new Category();
        // Giả lập danh mục cha có chứa 1 danh mục con bên trong
        parentCategory.setSubCategories(Collections.singletonList(new Category()));

        when(productRepository.findByIdAndIsDeletedFalse(productId)).thenReturn(Optional.of(existingProduct));
        when(categoryService.findCategoryById(1L)).thenReturn(parentCategory);

        // 2. ACT & ASSERT
        AppException exception = assertThrows(AppException.class, () ->
                productService.updateProduct(productId, request)
        );

        assertEquals(ErrorCode.CATEGORY_NOT_LEAF, exception.getErrorCode());

        // 3. VERIFY
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void deleteProduct_shouldSuccess_whenProductExists() {
        // 1. SETUP
        Long productId = 1L;
        Product existingProduct = new Product();
        existingProduct.setId(productId);
        existingProduct.setIsDeleted(false); // Trạng thái ban đầu chưa xóa

        // Giả lập tìm thấy sản phẩm
        when(productRepository.findByIdAndIsDeletedFalse(productId))
                .thenReturn(Optional.of(existingProduct));

        // Giả lập hàm save trả về chính đối tượng đã chỉnh sửa
        when(productRepository.save(any(Product.class))).thenReturn(existingProduct);

        // 2. ACT
        assertDoesNotThrow(() -> productService.deleteProduct(productId));

        // 3. ASSERT & VERIFY
        // Kiểm tra xem trường dữ liệu đã được chuyển sang true trước khi lưu chưa
        assertTrue(existingProduct.getIsDeleted(), "Trường isDeleted phải được chuyển thành true");

        // Xác nhận repository thực hiện đúng nhiệm vụ
        verify(productRepository, times(1)).findByIdAndIsDeletedFalse(productId);
        verify(productRepository, times(1)).save(existingProduct);
    }

    @Test
    void deleteProduct_shouldThrowAppException_whenProductNotFound() {
        // 1. SETUP
        Long productId = 99L;

        // Giả lập không tìm thấy sản phẩm (trả về Optional rỗng)
        when(productRepository.findByIdAndIsDeletedFalse(productId))
                .thenReturn(Optional.empty());

        // 2. ACT & ASSERT
        AppException exception = assertThrows(AppException.class, () ->
                productService.deleteProduct(productId)
        );

        // Kiểm tra đúng mã lỗi mong muốn
        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, exception.getErrorCode());

        // 3. VERIFY
        // Đảm bảo hệ thống KHÔNG bao giờ gọi tới hàm lưu dữ liệu nếu không tìm thấy gốc
        verify(productRepository, times(1)).findByIdAndIsDeletedFalse(productId);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void getAllProducts_shouldReturnPagingResponse_whenProductsExist() {
        // 1. SETUP DATA & MOCKS
        int page = 0;
        int size = 5;
        String sort = "createdAt:desc";

        // Giả lập dữ liệu Projection và Response mẫu
        ProductProjection projectionMock = mock(ProductProjection.class);
        ProductResponse responseMock = new ProductResponse();

        List<ProductProjection> projections = Collections.singletonList(projectionMock);

        // Dùng PageImpl thực tế của Spring Data để gói danh sách dữ liệu giả lập
        Page<ProductProjection> productPage = new PageImpl<>(projections, Pageable.ofSize(size), 1);

        // Giả lập hành vi cho Repository và Mapper
        when(productRepository.findAllProducts(any(Pageable.class))).thenReturn(productPage);
        when(productMapper.toProductResponse(any(ProductProjection.class))).thenReturn(responseMock);

        // 2. ACT
        PagingResponse<ProductResponse> actualResponse = productService.getAllProducts(page, size, sort);

        // 3. ASSERT (Kiểm tra trọn vẹn cấu trúc phân trang trả về)
        assertNotNull(actualResponse);
        assertEquals(1, actualResponse.getContent().size());
        assertEquals(size, actualResponse.getSize());
        assertEquals(1, actualResponse.getTotalElements());
        assertEquals(0, actualResponse.getPage());
        assertTrue(actualResponse.isLast());
        assertEquals(1, actualResponse.getTotalPages());

        // 4. VERIFY (Đảm bảo các dependency được gọi đúng luồng)
        verify(productRepository, times(1)).findAllProducts(any(Pageable.class));
        verify(productMapper, times(1)).toProductResponse(any(ProductProjection.class));
    }

    @Test
    void deleteProductImage_shouldSuccessAndChangePrimary_whenDeletedImageIsPrimary() {
        Long productId = 1L;
        Long imgId = 10L;
        Long nextImgId = 11L;

        ProductImage nextImage = new ProductImage();
        nextImage.setId(nextImgId);
        nextImage.setIsPrimary(false);

        // Giả lập luồng: Sản phẩm có tồn tại -> Ảnh thuộc về sản phẩm -> Ảnh bị xóa ĐANG LÀ ẢNH CHÍNH
        when(productRepository.existsByIdAndIsDeletedFalse(productId)).thenReturn(true);
        when(productImageRepository.existsByIdAndProductId(imgId, productId)).thenReturn(true);
        when(productImageRepository.existsByIdAndProductIdAndIsPrimaryTrue(imgId, productId)).thenReturn(true);

        // Tìm thấy danh sách ảnh phụ còn lại để đôn lên làm ảnh chính mới
        when(productImageRepository.findByProductIdAndIsPrimaryFalseAndIdNot(productId, imgId))
                .thenReturn(Collections.singletonList(nextImage));
        when(productImageRepository.save(nextImage)).thenReturn(nextImage);

        when(productImageRepository.findById(imgId)).thenReturn(Optional.of(new ProductImage()));

          // Chạy hàm
        assertDoesNotThrow(() -> productService.deleteProductImage(productId, imgId));

        // Kiểm tra xem ảnh phụ đã được chuyển thành ảnh chính chưa
        assertTrue(nextImage.getIsPrimary());

        // Xác nhận đã gọi xóa ảnh cũ
        verify(productImageRepository).deleteById(imgId);
    }

    @Test
    void deleteProductImage_shouldThrowException_whenProductImageNotFound() {
        Long productId = 1L;
        Long imgId = 99L;

        when(productRepository.existsByIdAndIsDeletedFalse(productId)).thenReturn(true);
        // Giả lập ảnh không thuộc sản phẩm này hoặc không tồn tại
        when(productImageRepository.existsByIdAndProductId(imgId, productId)).thenReturn(false);

        AppException exception = assertThrows(AppException.class, () ->
                productService.deleteProductImage(productId, imgId)
        );

        assertEquals(ErrorCode.PRODUCT_IMAGE_NOT_FOUND, exception.getErrorCode());
        verify(productImageRepository, never()).deleteById(anyLong());
    }

    // ==========================================
    // TẬP TEST CASES CHO: saveProductImages
    // ==========================================

    @Test
    void saveProductImages_shouldSuccess_whenAllImagesUploadSuccessfully() throws IOException {
        Long productId = 1L;

        // Mock các file ảnh multipart truyền lên
        MultipartFile file1 = mock(MultipartFile.class);
        MultipartFile file2 = mock(MultipartFile.class);
        when(file1.getBytes()).thenReturn(new byte[]{1, 2});
        when(file2.getBytes()).thenReturn(new byte[]{3, 4});
        List<MultipartFile> imageFiles = Arrays.asList(file1, file2);

        ProductImage imgResult1 = new ProductImage();
        ProductImage imgResult2 = new ProductImage();
        ProductImageResponse resp1 = new ProductImageResponse();
        ProductImageResponse resp2 = new ProductImageResponse();

        when(productRepository.existsByIdAndIsDeletedFalse(productId)).thenReturn(true);

        // Giả lập tính năng xử lý song song trả về các CompletableFuture hoàn thành thành công
        when(productImageAsyncService.uploadSingleImage(any(byte[].class), eq(productId), anyBoolean()))
                .thenReturn(CompletableFuture.completedFuture(imgResult1))
                .thenReturn(CompletableFuture.completedFuture(imgResult2));

        when(productImageMapper.toProductImageResponse(imgResult1)).thenReturn(resp1);
        when(productImageMapper.toProductImageResponse(imgResult2)).thenReturn(resp2);

        // Chạy hàm
        List<ProductImageResponse> responses = productService.saveProductImages(imageFiles, productId);

        // Kiểm tra kết quả gom dữ liệu sau bất đồng bộ
        assertNotNull(responses);
        assertEquals(2, responses.size());
        verify(productImageAsyncService, times(2)).uploadSingleImage(any(byte[].class), eq(productId), anyBoolean());
    }

    @Test
    void saveProductImages_shouldThrowAppException_whenAsyncUploadFails() throws IOException {
        Long productId = 1L;
        MultipartFile file = mock(MultipartFile.class);
        when(file.getBytes()).thenReturn(new byte[]{1, 2});

        when(productRepository.existsByIdAndIsDeletedFalse(productId)).thenReturn(true);

        // Giả lập luồng phụ chạy ngầm bị crash (bắn ra lỗi lồng trong ExecutionException)
        CompletableFuture<ProductImage> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Cloudinary upload error"));

        when(productImageAsyncService.uploadSingleImage(any(byte[].class), eq(productId), anyBoolean()))
                .thenReturn(failedFuture);

        // Đoạn code logic get() sẽ ném ra ExecutionException, và hàm của bạn bọc lại thành IMAGE_UPLOAD_FAILED
        AppException exception = assertThrows(AppException.class, () ->
                productService.saveProductImages(Collections.singletonList(file), productId)
        );

        assertEquals(ErrorCode.IMAGE_UPLOAD_FAILED, exception.getErrorCode());
    }

    @Test
    void saveProductImages_shouldThrowAppException_whenBuildImageBytesFails() throws IOException {
        Long productId = 1L;
        MultipartFile file = mock(MultipartFile.class);

        when(productRepository.existsByIdAndIsDeletedFalse(productId)).thenReturn(true);
        // Giả lập file lỗi IO khi đọc mảng byte
        when(file.getBytes()).thenThrow(new IOException("Read error"));

        AppException exception = assertThrows(AppException.class, () ->
                productService.saveProductImages(Collections.singletonList(file), productId)
        );

        assertEquals(ErrorCode.IMAGE_PROCESSING_FAILED, exception.getErrorCode());
    }
}
