package org.ngphthinh.dto.response.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class AllTimeStats {
    private Integer totalProducts;
    private Integer totalCustomers;
    private Integer totalOrders;
}
