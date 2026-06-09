package org.ngphthinh.repository.projection;

public interface CustomerRankItemProjection {
    Integer getRank();
    String getCustomerId();
    String getFullName();
    String getEmail();
    Integer getTotalOrders();
    Long getTotalSpent();
}
