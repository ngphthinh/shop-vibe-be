package org.ngphthinh.dto.response.statistics;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class OverviewResponse {
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime generatedAt;
    private PeriodStats today;
    private PeriodStats thisMonth;
    private AllTimeStats allTime;
    private OrdersByStatus activeOrders;

}