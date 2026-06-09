package org.ngphthinh.dto.response.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class TopCustomersResponse {
    private Integer limit;

    private List<CustomerRankItem> data;
}
