package org.ngphthinh.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ngphthinh.dto.request.product.ProductCreateRequest;
import org.ngphthinh.dto.response.product.ProductDetailResponse;
import org.ngphthinh.entity.Category;
import org.ngphthinh.entity.Product;
import org.ngphthinh.exception.AppException;
import org.ngphthinh.exception.ErrorCode;
import org.ngphthinh.mapper.ProductMapper;
import org.ngphthinh.mapper.ProductImageMapper;
import org.ngphthinh.repository.ProductImageRepository;
import org.ngphthinh.repository.ProductRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

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
	public void getProductById_whenNotFound_shouldThrow() {
		when(productRepository.findByIdWithProjection(1L)).thenReturn(Optional.empty());

		AppException ex = assertThrows(AppException.class, () -> productService.getProductById(1L));
		assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
	}

	@Test
	public void getProductById_whenFound_shouldReturnDetail() {
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
	public void createProduct_whenCategoryNull_shouldThrow() {
		ProductCreateRequest req = ProductCreateRequest.builder()
				.name("n")
				.categoryId(5L)
				.build();

		when(categoryService.findCategoryById(5L)).thenReturn(null);

		AppException ex = assertThrows(AppException.class, () -> productService.createProduct(req));
		assertEquals(ErrorCode.CATEGORY_NOT_FOUND, ex.getErrorCode());
	}

	@Test
	public void createProduct_whenCategoryNotLeaf_shouldThrow() {
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
	public void createProduct_whenValid_shouldReturnDetail() {
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

}
