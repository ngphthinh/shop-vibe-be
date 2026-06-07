package org.ngphthinh.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface ProductProjection {
    Long getId();

    String getName();

    String getSlug();

    String getDescription();

    BigDecimal getPrice();

    Integer getStockQuantity();

    String getImageUrl();

    Long getCategoryId();

    String getCategoryName();

    Double getAverageRating();

    Long getReviewCount();

    LocalDateTime getCreatedAt();


}