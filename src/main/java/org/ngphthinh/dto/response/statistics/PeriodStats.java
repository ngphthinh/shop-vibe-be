package org.ngphthinh.dto.response.statistics;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.JoinColumn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class PeriodStats {
    private Long revenue;
    private Integer orders;
    private Integer newCustomers;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private OrdersByStatus ordersByStatus;
}
