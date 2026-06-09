package org.ngphthinh.repository.projection;

import org.ngphthinh.enums.OrderStatus;

public interface OrdersByStatusProjection {
    OrderStatus getStatus();

    Long getCount();
}
