package org.ngphthinh.repository.projection;

import java.math.BigDecimal;

public interface CartProjection {

    Long getId();

    BigDecimal getTotalAmount();

    Integer getTotalItems();

    Long getItemId();

    Integer getQuantity();

    BigDecimal getUnitPrice();

    BigDecimal getSubtotal();

    Long getProductId();

    String getProductName();

    BigDecimal getProductPrice();

    String getProductPrimaryImageUrl();


}
