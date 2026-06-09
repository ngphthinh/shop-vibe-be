package org.ngphthinh.repository.projection;

public interface ProductRankItemProjection {

    Integer getRank();

    String getProductId();

    String getProductName();

    String getThumbnail();

    Integer getTotalQuantitySold();

    Long getTotalRevenue();
}
