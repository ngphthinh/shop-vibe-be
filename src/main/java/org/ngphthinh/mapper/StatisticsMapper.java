package org.ngphthinh.mapper;

import org.mapstruct.Mapper;
import org.ngphthinh.dto.response.statistics.*;
import org.ngphthinh.repository.projection.*;
import org.ngphthinh.repository.projection.AllTimeStatsProjection;

@Mapper(componentModel = "spring")
public interface StatisticsMapper {
    RevenueDataResponse toRevenueDataResponse(RevenueProjection revenueProjection);

    ProductRankItem toProductRankItem(ProductRankItemProjection productRankItemProjection);

    CustomerRankItem toCustomerRankItem(CustomerRankItemProjection customerRankItemProjection);

    PeriodStats toPeriodStats(PeriodStatsProjection statsProjection);

    AllTimeStats toAllTimeStats(AllTimeStatsProjection allTimeStatsProjection);
}
