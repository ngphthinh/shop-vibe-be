package org.ngphthinh.repository.projection;


import org.ngphthinh.enums.OrderStatus;
import org.ngphthinh.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface OrderProjection {
    Long getId();

    String getOrderCode();

    OrderStatus getStatus();

    Long getProductId();

    String getProductName();

    String getProductImageUrl();

    BigDecimal getPrice();

    Integer getQuantity();

    BigDecimal getSubtotal();

    BigDecimal getTotalAmount();

    String getShippingAddress();

    String getPaymentMethod();

    PaymentStatus getPaymentStatus();

    String getNote();

    LocalDateTime getCreatedAt();

    String getCustomerName  ();

    Long getItemCount();
}
