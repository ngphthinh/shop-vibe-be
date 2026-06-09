package org.ngphthinh.controller;

import lombok.RequiredArgsConstructor;
import org.ngphthinh.dto.response.ApiResponse;
import org.ngphthinh.dto.response.statistics.*;
import org.ngphthinh.enums.ResponseCode;
import org.ngphthinh.service.StatisticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/admin/statistics")
public class StatisticsController {
    private final StatisticsService statisticsService;

    @GetMapping("/revenue")
    public ApiResponse<RevenueResponse> getRevenueStatistics(
            @RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate from,
            @RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate to) {
        return ApiResponse.<RevenueResponse>builder()
                .code(ResponseCode.STATISTICS_REVENUE_SUCCESS.getCode())
                .message(ResponseCode.STATISTICS_REVENUE_SUCCESS.getMessage())
                .data(statisticsService.getRevenueStatistics(from, to))
                .build();

    }

    @GetMapping("/top-products")
    public ApiResponse<TopProductsResponse> getTopProducts(@RequestParam(defaultValue = "10") Integer limit) {
        return ApiResponse.<TopProductsResponse>builder()
                .code(ResponseCode.STATISTICS_TOP_PRODUCTS_SUCCESS.getCode())
                .message(ResponseCode.STATISTICS_TOP_PRODUCTS_SUCCESS.getMessage())
                .data(statisticsService.getTopProducts(limit))
                .build();
    }

    @GetMapping("/top-customers")
    public ApiResponse<TopCustomersResponse> getTopCustomers(@RequestParam(defaultValue = "10") Integer limit) {
        return ApiResponse.<TopCustomersResponse>builder()
                .code(ResponseCode.STATISTICS_TOP_CUSTOMERS_SUCCESS.getCode())
                .message(ResponseCode.STATISTICS_TOP_CUSTOMERS_SUCCESS.getMessage())
                .data(statisticsService.getTopCustomers(limit))
                .build();
    }

    @GetMapping("/overview")
    public ApiResponse<OverviewResponse> getOverview() {
        return ApiResponse.<OverviewResponse>builder()
                .code(ResponseCode.STATISTICS_OVERVIEW_SUCCESS.getCode())
                .message(ResponseCode.STATISTICS_OVERVIEW_SUCCESS.getMessage())
                .data(statisticsService.getOverview())
                .build();
    }
}
