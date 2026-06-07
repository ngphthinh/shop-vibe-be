package org.ngphthinh.repository;

import org.ngphthinh.entity.ProductImage;
import org.ngphthinh.repository.projection.ProductImageProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    @Query("select pi.imageUrl as imageUrl, pi.id as id from ProductImage pi where pi.product.id = :productId")
    List<ProductImageProjection> findImageUrlsByProductId(@Param("productId") Long productId);


    boolean existsByIdAndProductId(Long id, Long productId);


    boolean existsByIdAndProductIdAndIsPrimaryTrue(Long id, Long productId);

    List<ProductImage> findByIdAndProductIdAndIsPrimaryFalse(Long id, Long productId);


    List<ProductImage> findByProductIdAndIsPrimaryFalseAndIdNot(Long productId, Long id);

    void deleteByPublicId(String publicId);
}

