package org.ngphthinh.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface RevenueProjection {
    LocalDate getDate();
    BigDecimal getRevenue();
    BigDecimal getTotalRevenue();
}
