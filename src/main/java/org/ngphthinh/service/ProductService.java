package org.ngphthinh.service;

import jakarta.persistence.Entity;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.ngphthinh.dto.request.product.ProductCreateRequest;
import org.ngphthinh.dto.request.product.ProductUpdateRequest;
import org.ngphthinh.dto.response.PagingResponse;
import org.ngphthinh.dto.response.category.CategoryResponse;
import org.ngphthinh.dto.response.product.ProductDetailResponse;
import org.ngphthinh.dto.response.product.ProductImageResponse;
import org.ngphthinh.dto.response.product.ProductResponse;
import org.ngphthinh.entity.Category;
import org.ngphthinh.entity.Product;
import org.ngphthinh.entity.ProductImage;
import org.ngphthinh.exception.AppException;
import org.ngphthinh.exception.ErrorCode;
import org.ngphthinh.mapper.ProductImageMapper;
import org.ngphthinh.mapper.ProductMapper;
import org.ngphthinh.repository.ProductImageRepository;
import org.ngphthinh.repository.ProductRepository;
import org.ngphthinh.repository.projection.ProductImageProjection;
import org.ngphthinh.repository.projection.ProductProjection;
import org.ngphthinh.util.AppUtil;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final ProductImageRepository productImageRepository;
    private final CategoryService categoryService;
    private final ProductImageAsyncService productImageAsyncService;
    private final ProductImageMapper productImageMapper;

    @Cacheable(value = "products", key = "#page + '-' + #size + '-' + #sort")
    public PagingResponse<ProductResponse> getAllProducts(int page, int size, String sort) {

        Pageable pageable = AppUtil.buildPageable(page, size, sort);


        Page<ProductProjection> productPage = productRepository.findAllProducts(pageable);

        return getProductResponsePagingResponse(productPage);
    }

    public ProductDetailResponse getProductById(Long id) {
        ProductProjection productProjection = productRepository.findByIdWithProjection(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        List<ProductImageProjection> imgUrls = productImageRepository.findImageUrlsByProductId(id);
        return productMapper.toProductDetailResponse(productProjection, imgUrls);
    }

    public PagingResponse<ProductResponse> getProductsByCategoryId(String keyword, int page, int size, String sort) {
        Pageable pageable = AppUtil.buildPageable(page, size, sort);
        Page<ProductProjection> productPage = productRepository.findProductsByKeyword(keyword, pageable);

        return getProductResponsePagingResponse(productPage);
    }

    private PagingResponse<ProductResponse> getProductResponsePagingResponse(Page<ProductProjection> productPage) {
        List<ProductResponse> productResponses = productPage.getContent().stream()
                .map(productMapper::toProductResponse)
                .collect(Collectors.toList());

        return PagingResponse.<ProductResponse>builder()
                .content(productResponses)
                .size(productPage.getSize())
                .totalElements(productPage.getTotalElements())
                .page(productPage.getNumber())
                .last(productPage.isLast())
                .totalPages(productPage.getTotalPages())
                .build();
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @CacheEvict(value = "products", allEntries = true)
    @Transactional
    public ProductDetailResponse createProduct(ProductCreateRequest request) {
        Category category = categoryService.findCategoryById(request.getCategoryId());
        if (category == null) {
            throw new AppException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        // Chỉ cho phép tạo sản phẩm trong danh mục lá (leaf category)
        if (!category.getSubCategories().isEmpty()) {
            throw new AppException(ErrorCode.CATEGORY_NOT_LEAF);
        }

        Product product = productMapper.toProduct(request);

        product.setCategory(category);

        String slug = request.getName().toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "-") + "-" + System.currentTimeMillis();
        product.setSlug(slug);

        return productMapper.toProductDetailResponse(productRepository.save(product));
    }


    @Transactional
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @CacheEvict(value = "products", allEntries = true)
    public ProductDetailResponse updateProduct(Long id, ProductUpdateRequest request) {
        Product product = productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        if (request.getCategoryId() != null) {
            Category category = categoryService.findCategoryById(request.getCategoryId());
            if (category == null) {
                throw new AppException(ErrorCode.CATEGORY_NOT_FOUND);
            } else if (!category.getSubCategories().isEmpty()) {
                throw new AppException(ErrorCode.CATEGORY_NOT_LEAF);
            }
            product.setCategory(category);
        }
        // chỉ những trường thông tin nào được cung cấp trong request mới được cập nhật
        productMapper.updateProductFromRequest(request, product);

        return productMapper.toProductDetailResponse(productRepository.save(product));
    }


    @Transactional
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @CacheEvict(value = "products", allEntries = true)
    public void deleteProduct(Long id) {
        Product product = productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        product.setIsDeleted(true);
        productRepository.save(product);
    }

    @Transactional
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @CacheEvict(value = "products", allEntries = true)
    public void deleteProductImage(Long id, Long imgId) {
        // Kiểm tra sản phẩm có tồn tại và chưa bị xóa
        productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        // Kiểm tra ảnh có tồn tại và thuộc về sản phẩm này
        if (!productImageRepository.existsByIdAndProductId(imgId, id)) {
            throw new AppException(ErrorCode.PRODUCT_IMAGE_NOT_FOUND);
        }

        // Nếu ảnh này là ảnh chính, cần tìm một ảnh khác để làm ảnh chính mới
        if (productImageRepository.existsByIdAndProductIdAndIsPrimaryTrue(imgId, id)) {
            List<ProductImage> otherImages = productImageRepository.findByProductIdAndIsPrimaryFalseAndIdNot(id, imgId);
            if (!otherImages.isEmpty()) {
                ProductImage newPrimary = otherImages.get(0);
                newPrimary.setIsPrimary(true);
                productImageRepository.save(newPrimary);
            } else {
                //TODO: Nếu không còn ảnh nào khác, có thể để sản phẩm không có ảnh chính
            }
        }

        productImageRepository.deleteById(imgId);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public List<ProductImageResponse> saveProductImages(List<MultipartFile> imageBytesList, Long productId) {

        productRepository.findByIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));


        if (imageBytesList == null || imageBytesList.isEmpty()) {
            throw new AppException(ErrorCode.NO_IMAGES_PROVIDED);
        }

        List<byte[]> bytesImg = buildImageBytes(imageBytesList);

        // 1. Kích hoạt upload song song tất cả các ảnh cùng một lúc
        List<CompletableFuture<ProductImage>> futures = bytesImg.stream()
                .map(bytes -> productImageAsyncService.uploadSingleImage(bytes, productId))
                .toList();

        // 2. Chờ cho đến khi TẤT CẢ các ảnh được upload và lưu DB thành công
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        try {
            // Block tại đây để đợi kết quả cuối cùng từ tất cả các Thread ngầm
            allOf.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new AppException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }

        // 3. Gom kết quả trả về List<ProductImage> sạch sẽ để hiển thị cho Client
        return futures.stream()
                .map(CompletableFuture::join)
                .map(productImageMapper::toProductImageResponse)
                .toList();
    }

    private List<byte[]> buildImageBytes(List<MultipartFile> images) {
        return images.stream()
                .map(image -> {
                    try {
                        return image.getBytes();
                    } catch (IOException e) {
                        throw new AppException(ErrorCode.IMAGE_PROCESSING_FAILED);
                    }
                })
                .toList();
    }
}