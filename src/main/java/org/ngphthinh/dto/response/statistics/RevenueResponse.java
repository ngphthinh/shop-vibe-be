package org.ngphthinh.dto.response.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class RevenueResponse {

    private String from;
    private String to;
    private BigDecimal totalRevenue;
    private List<RevenueDataResponse> data;
}
