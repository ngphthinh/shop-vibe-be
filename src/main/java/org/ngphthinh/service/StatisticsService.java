package org.ngphthinh.service;

import lombok.RequiredArgsConstructor;
import org.ngphthinh.dto.response.statistics.*;
import org.ngphthinh.enums.OrderStatus;
import org.ngphthinh.enums.PeriodStatistics;
import org.ngphthinh.exception.AppException;
import org.ngphthinh.exception.ErrorCode;
import org.ngphthinh.mapper.StatisticsMapper;
import org.ngphthinh.repository.OrderRepository;
import org.ngphthinh.repository.ProductRepository;
import org.ngphthinh.repository.UserRepository;
import org.ngphthinh.repository.projection.*;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
@Service
public class StatisticsService {

    private final OrderRepository orderRepository;
    private final StatisticsMapper statisticsMapper;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public RevenueResponse getRevenueStatistics(LocalDate from, LocalDate to) {
        List<RevenueProjection> revenueProjections = orderRepository.findRevenueByDateRange(from.atStartOfDay(), to.atTime(LocalTime.MAX));

        List<RevenueDataResponse> revenueData = revenueProjections.stream()
                .map(statisticsMapper::toRevenueDataResponse)
                .toList();

        BigDecimal totalRevenue = revenueProjections.stream()
                .map(RevenueProjection::getRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return RevenueResponse.builder()
                .from(from)
                .to(to)
                .totalRevenue(totalRevenue)
                .data(revenueData)
                .build();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public TopProductsResponse getTopProducts(Integer limit) {

        Pageable pageable = Pageable.ofSize(limit);
        List<ProductRankItemProjection> topProducts = productRepository.findTopProducts(pageable);

        List<ProductRankItem> productRankItems = topProducts.stream()
                .map(statisticsMapper::toProductRankItem)
                .toList();

        return TopProductsResponse.builder()
                .limit(limit)
                .data(productRankItems)
                .build();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public TopCustomersResponse getTopCustomers(Integer limit) {
        Pageable pageable = Pageable.ofSize(limit);
        List<CustomerRankItemProjection> topCustomers = userRepository.findTopCustomers(pageable);
        List<CustomerRankItem> customerRankItems = topCustomers.stream()
                .map(statisticsMapper::toCustomerRankItem)
                .toList();

        return TopCustomersResponse.builder()
                .limit(limit)
                .data(customerRankItems)
                .build();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public OverviewResponse getOverview() {

        PeriodStats today = getPeriodStats(PeriodStatistics.TODAY);
        PeriodStats thisMonth = getPeriodStats(PeriodStatistics.THIS_MONTH);

        List<OrdersByStatusProjection> activeOrders = orderRepository.statisticOrderByByWithoutStatus(
                List.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED),
                false,
                LocalDate.now().atStartOfDay(),
                LocalDate.now().atTime(LocalTime.MAX));

        AllTimeStatsProjection allTimeStatsProjection = orderRepository.findAllTimeStats();

        return OverviewResponse.builder()
                .generatedAt(OffsetDateTime.now())
                .today(today)
                .thisMonth(thisMonth)
                .activeOrders(getOrdersByStatus(activeOrders))
                .allTime(statisticsMapper.toAllTimeStats(allTimeStatsProjection))
                .build();
    }

    private PeriodStats getPeriodStats(PeriodStatistics periodStatistics) {
        LocalDate now = LocalDate.now();
        LocalDate start;
        LocalDate end;

        switch (periodStatistics) {
            case TODAY -> {
                start = now;
                end = now;
            }
            case THIS_MONTH -> {
                start = now.withDayOfMonth(1);
                end = now.withDayOfMonth(now.lengthOfMonth());
            }
            default -> throw new AppException(ErrorCode.INVALID_PERIOD_STATISTICS);
        }

        PeriodStatsProjection statsProjection = orderRepository.findPeriodStats(start.atStartOfDay(), end.atTime(LocalTime.MAX));


        PeriodStats periodStats = statisticsMapper.toPeriodStats(statsProjection);


        if (periodStatistics.equals(PeriodStatistics.THIS_MONTH)) {
            List<OrdersByStatusProjection> ordersByStatusProjections = orderRepository.statisticOrderByByWithoutStatus(Collections.emptyList(), true, start.atStartOfDay(), end.atTime(LocalTime.MAX));
            OrdersByStatus ordersByStatus = getOrdersByStatus(ordersByStatusProjections);
            periodStats.setOrdersByStatus(ordersByStatus);
        }
        return periodStats;

    }

    private static OrdersByStatus getOrdersByStatus(List<OrdersByStatusProjection> ordersByStatusProjections) {
        OrdersByStatus ordersByStatus = new OrdersByStatus();
        for (OrdersByStatusProjection ordersByStatusProjection : ordersByStatusProjections) {
            switch (ordersByStatusProjection.getStatus()) {
                case OrderStatus.PENDING -> ordersByStatus.setPending(ordersByStatusProjection.getCount());
                case OrderStatus.CONFIRMED -> ordersByStatus.setConfirmed(ordersByStatusProjection.getCount());
                case OrderStatus.SHIPPING -> ordersByStatus.setShipping(ordersByStatusProjection.getCount());
                case OrderStatus.DELIVERED -> ordersByStatus.setDelivered(ordersByStatusProjection.getCount());
                case OrderStatus.CANCELLED -> ordersByStatus.setCancelled(ordersByStatusProjection.getCount());
            }
        }
        return ordersByStatus;
    }
}
