package org.ngphthinh.dto.response.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ProductRankItem {
    private Integer rank;
    private String productId;
    private String productName;
    private String thumbnail;
    private Integer totalQuantitySold;
    private Long totalRevenue;

}
