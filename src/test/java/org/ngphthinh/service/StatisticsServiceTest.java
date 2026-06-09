package org.ngphthinh.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ngphthinh.dto.response.statistics.*;
import org.ngphthinh.enums.OrderStatus;
import org.ngphthinh.exception.AppException;
import org.ngphthinh.exception.ErrorCode;
import org.ngphthinh.mapper.StatisticsMapper;
import org.ngphthinh.repository.OrderRepository;
import org.ngphthinh.repository.ProductRepository;
import org.ngphthinh.repository.UserRepository;
import org.ngphthinh.repository.projection.*;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private StatisticsMapper statisticsMapper;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StatisticsService statisticsService;

    // Khởi tạo các mẫu Mock Projection bằng anonymous class hoặc Mockito mock
    private RevenueProjection mockRevenueProjection;
    private ProductRankItemProjection mockProductProjection;
    private CustomerRankItemProjection mockCustomerProjection;
    private PeriodStatsProjection mockPeriodStatsProjection;
    private AllTimeStatsProjection mockAllTimeStatsProjection;

    @BeforeEach
    void setUp() {
        mockRevenueProjection = mock(RevenueProjection.class);
        mockProductProjection = mock(ProductRankItemProjection.class);
        mockCustomerProjection = mock(CustomerRankItemProjection.class);
        mockPeriodStatsProjection = mock(PeriodStatsProjection.class);
        mockAllTimeStatsProjection = mock(AllTimeStatsProjection.class);
    }

    @Nested
    @DisplayName("Test hàm getRevenueStatistics")
    class RevenueStatisticsTest {

        @Test
        @DisplayName("Nên trả về RevenueResponse thành công khi dữ liệu hợp lệ")
        void shouldReturnRevenueResponse_WhenValidDateRange() {
            // Given
            LocalDate from = LocalDate.now().minusDays(7);
            LocalDate to = LocalDate.now();
            when(mockRevenueProjection.getRevenue())
                    .thenReturn(BigDecimal.valueOf(1500000));
            when(orderRepository.findRevenueByDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(List.of(mockRevenueProjection));

            RevenueDataResponse mockDataResponse = new RevenueDataResponse();
            when(statisticsMapper.toRevenueDataResponse(any(RevenueProjection.class))).thenReturn(mockDataResponse);

            // When
            RevenueResponse response = statisticsService.getRevenueStatistics(from, to);

            // Then
            assertNotNull(response);
            assertEquals(from, response.getFrom());
            assertEquals(BigDecimal.valueOf(1500000), response.getTotalRevenue());
            assertEquals(1, response.getData().size());

            verify(orderRepository, times(1)).findRevenueByDateRange(from.atStartOfDay(), to.atTime(LocalTime.MAX));
        }
    }

    @Nested
    @DisplayName("Test hàm getTopProducts")
    class TopProductsTest {

        @Test
        @DisplayName("Nên trả về danh sách sản phẩm bán chạy đúng giới hạn limit")
        void shouldReturnTopProducts_WithGivenLimit() {
            // Given
            Integer limit = 5;
            when(productRepository.findTopProducts(any(Pageable.class))).thenReturn(List.of(mockProductProjection));
            when(statisticsMapper.toProductRankItem(any(ProductRankItemProjection.class))).thenReturn(new ProductRankItem());

            // When
            TopProductsResponse response = statisticsService.getTopProducts(limit);

            // Then
            assertNotNull(response);
            assertEquals(limit, response.getLimit());
            assertEquals(1, response.getData().size());

            verify(productRepository, times(1)).findTopProducts(Pageable.ofSize(limit));
        }
    }

    @Nested
    @DisplayName("Test hàm getTopCustomers")
    class TopCustomersTest {

        @Test
        @DisplayName("Nên trả về danh sách khách hàng hàng đầu đúng giới hạn limit")
        void shouldReturnTopCustomers_WithGivenLimit() {
            // Given
            Integer limit = 10;
            when(userRepository.findTopCustomers(any(Pageable.class))).thenReturn(List.of(mockCustomerProjection));
            when(statisticsMapper.toCustomerRankItem(any(CustomerRankItemProjection.class))).thenReturn(new CustomerRankItem());

            // When
            TopCustomersResponse response = statisticsService.getTopCustomers(limit);

            // Then
            assertNotNull(response);
            assertEquals(limit, response.getLimit());
            assertEquals(1, response.getData().size());

            verify(userRepository, times(1)).findTopCustomers(Pageable.ofSize(limit));
        }
    }

    @Nested
    @DisplayName("Test hàm getOverview")
    class OverviewTest {

        @Test
        @DisplayName("Nên trả về tổng quan OverviewResponse chính xác dữ liệu ngày, tháng và all-time")
        void shouldReturnOverviewResponse_Success() {
            // Given
            // 1. Khởi tạo DTO sạch để hứng dữ liệu giả lập
            PeriodStats todayStats = new PeriodStats();
            PeriodStats thisMonthStats = new PeriodStats();

            // Giả lập trả về dữ liệu period (Lần 1 cho TODAY, Lần 2 cho THIS_MONTH)
            when(orderRepository.findPeriodStats(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(mockPeriodStatsProjection);
            when(statisticsMapper.toPeriodStats(mockPeriodStatsProjection))
                    .thenReturn(todayStats, thisMonthStats);

            // 2. Giả lập trạng thái đơn hàng của THÁNG (Trong hàm getPeriodStats - Lần gọi đầu tiên của statisticOrderByByWithoutStatus)
            OrdersByStatusProjection monthPending = mock(OrdersByStatusProjection.class);
            when(monthPending.getStatus()).thenReturn(OrderStatus.PENDING);
            when(monthPending.getCount()).thenReturn(10L);

            // 3. Giả lập đơn hàng ACTIVE hiện tại (Trong hàm getOverview - Lần gọi thứ hai của statisticOrderByByWithoutStatus)
            OrdersByStatusProjection activeShipping = mock(OrdersByStatusProjection.class);
            when(activeShipping.getStatus()).thenReturn(OrderStatus.SHIPPING);
            when(activeShipping.getCount()).thenReturn(3L);

            // Sử dụng `thenReturn` chuỗi liên tiếp để trả về đúng danh sách theo thứ tự thực tế hệ thống gọi:
            // - Lần gọi đầu tiên (Tháng): Trả về danh sách chứa monthPending
            // - Lần gọi thứ hai (Active): Trả về danh sách chứa activeShipping
            when(orderRepository.statisticOrderByByWithoutStatus(anyList(), anyBoolean(), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(List.of(monthPending), List.of(activeShipping));

            // 4. Giả lập All time stats
            when(orderRepository.findAllTimeStats()).thenReturn(mockAllTimeStatsProjection);
            AllTimeStats allTimeStats = new AllTimeStats();
            when(statisticsMapper.toAllTimeStats(mockAllTimeStatsProjection)).thenReturn(allTimeStats);

            // When
            OverviewResponse response = statisticsService.getOverview();

            // Then
            assertNotNull(response);
            assertNotNull(response.getGeneratedAt());
            assertEquals(todayStats, response.getToday());
            assertEquals(thisMonthStats, response.getThisMonth());

            // Kiểm tra phân tích trạng thái đơn hàng Active
            assertNotNull(response.getActiveOrders());
            assertEquals(3L, response.getActiveOrders().getShipping());
            assertNull(response.getActiveOrders().getPending()); // Hợp lệ với thuộc tính kiểu Long (mặc định ban đầu null)

            // Kiểm tra phân tích trạng thái của tháng
            assertNotNull(response.getThisMonth().getOrdersByStatus());
            assertEquals(10L, response.getThisMonth().getOrdersByStatus().getPending());
            assertNull(response.getThisMonth().getOrdersByStatus().getShipping());

            // Xác minh số lần tương tác chuẩn xác với Repository
            verify(orderRepository, times(2)).findPeriodStats(any(), any());
            verify(orderRepository, times(2)).statisticOrderByByWithoutStatus(anyList(), anyBoolean(), any(), any());
            verify(orderRepository, times(1)).findAllTimeStats();
        }
    }
}