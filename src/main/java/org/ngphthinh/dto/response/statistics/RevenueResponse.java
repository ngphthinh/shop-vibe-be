package org.ngphthinh.dto.response.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class RevenueResponse {

    private LocalDate from;
    private LocalDate to;
    private BigDecimal totalRevenue;
    private List<RevenueDataResponse> data;
}
