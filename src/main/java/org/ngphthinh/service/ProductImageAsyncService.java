package org.ngphthinh.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ngphthinh.entity.Product;
import org.ngphthinh.entity.ProductImage;
import org.ngphthinh.exception.AppException;
import org.ngphthinh.exception.ErrorCode;
import org.ngphthinh.repository.ProductImageRepository;
import org.ngphthinh.repository.ProductRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProductImageAsyncService {
    private static final String SECURE_URL = "secure_url";
    private static final String PUBLIC_ID = "public_id";
    private final ProductImageRepository productImageRepository;
    private final Cloudinary cloudinary;
    private final ProductRepository productRepository;

    @Async("taskExecutor") // Cấu hình một ThreadPool riêng cho tác vụ upload ảnh
    public CompletableFuture<ProductImage> uploadSingleImage(byte[] imageBytes, Long productId,boolean isPrimary) {
        Map<?, ?> uploadParams = ObjectUtils.asMap(
                "folder", "shop-vibe",
                "transformation", new Transformation<>().width(640).height(480).crop("fill").gravity("auto")
        );

        try {
            // 1. Upload lên Cloudinary (Tác vụ này tốn thời gian nhất sẽ được đẩy xuống Thread ngầm)
            Map<?, ?> uploadResult = cloudinary.uploader().upload(imageBytes, uploadParams);

            // 2. Lấy Proxy của Product (Không tốn câu lệnh Select nhờ getReferenceById)
            Product product = productRepository.getReferenceById(productId);

            ProductImage productImage = ProductImage.builder()
                    .imageUrl(uploadResult.get(SECURE_URL).toString())
                    .publicId(uploadResult.get(PUBLIC_ID).toString())
                    .product(product)
                    .isPrimary(isPrimary)
                    .build();

            // 3. Lưu vào DB và trả về kết quả thành công trong Future
            return CompletableFuture.completedFuture(productImageRepository.save(productImage));

        } catch (IOException e) {
            // Trong môi trường Async, nên bắt exception và chuyển thành CompleteExceptionally
            CompletableFuture<ProductImage> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new AppException(ErrorCode.IMAGE_UPLOAD_FAILED));
            return failedFuture;
        }
    }


    @Transactional
    @Async
    public void deleteImage(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            productImageRepository.deleteByPublicId(publicId);
        } catch (IOException e) {
            // Vì đây là Thread ngầm kiểu 'void', thay vì throw Exception (sẽ bị nuốt mất),
            //  ghi Log ERROR rõ ràng để hệ thống giám sát có thể bắt được.
            log.error("Failed to delete image from Cloudinary for publicId: {}", publicId, e);
        }
    }
}
