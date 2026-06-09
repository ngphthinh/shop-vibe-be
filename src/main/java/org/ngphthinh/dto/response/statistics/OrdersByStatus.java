package org.ngphthinh.dto.response.statistics;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrdersByStatus {
    private Long pending;
    private Long confirmed;
    private Long shipping;
    private Long delivered;
    private Long cancelled;
}
