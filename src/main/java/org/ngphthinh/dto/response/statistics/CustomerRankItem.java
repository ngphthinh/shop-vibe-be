package org.ngphthinh.dto.response.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class CustomerRankItem {

    private Integer rank;
    private String customerId;
    private String fullName;
    private String email;
    private Long totalSpent;
}
