package org.ngphthinh.controller;

import lombok.RequiredArgsConstructor;
import org.ngphthinh.dto.request.order.OrderCancelRequest;
import org.ngphthinh.dto.request.order.OrderCreateRequest;
import org.ngphthinh.dto.request.order.OrderUpdateStatusRequest;
import org.ngphthinh.dto.response.ApiResponse;
import org.ngphthinh.dto.response.PagingResponse;
import org.ngphthinh.dto.response.order.OrderResponse;
import org.ngphthinh.enums.ResponseCode;
import org.ngphthinh.service.OrderService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/orders")
public class OrderController {

    private final OrderService orderService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ApiResponse<OrderResponse> createOrder(@RequestBody OrderCreateRequest request) {
        return ApiResponse.<OrderResponse>builder()
                .code(ResponseCode.ORDER_CREATE_SUCCESS.getCode())
                .message(ResponseCode.ORDER_CREATE_SUCCESS.getMessage())
                .data(orderService.createOrder(request))
                .build();
    }

    @GetMapping
    public ApiResponse<PagingResponse<OrderResponse>> getOrders(@PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
                                                                @RequestParam(required = false) String status,
                                                                @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate from,
                                                                @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate to
    ) {
        return ApiResponse.<PagingResponse<OrderResponse>>builder()
                .code(ResponseCode.ORDER_GET_SUCCESS.getCode())
                .message(ResponseCode.ORDER_GET_SUCCESS.getMessage())
                .data(orderService.getOrders(pageable, status, from, to))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getOrderById(@PathVariable Long id) {
        return ApiResponse.<OrderResponse>builder()
                .code(ResponseCode.ORDER_GET_BY_ID_SUCCESS.getCode())
                .message(ResponseCode.ORDER_GET_BY_ID_SUCCESS.getMessage())
                .data(orderService.getOrderById(id))
                .build();
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<OrderResponse> cancelOrder(@PathVariable Long id, @RequestBody OrderCancelRequest request) {
        return ApiResponse.<OrderResponse>builder()
                .code(ResponseCode.ORDER_CANCEL_SUCCESS.getCode())
                .message(ResponseCode.ORDER_CANCEL_SUCCESS.getMessage())
                .data(orderService.cancelOrder(id, request))
                .build();
    }

    @GetMapping("/admin/all")
    public ApiResponse<PagingResponse<OrderResponse>> getAllOrders(@PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
                                                                   @RequestParam(required = false) String status,
                                                                   @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate from,
                                                                   @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate to,
                                                                   @RequestParam String keyword
    ) {
        return ApiResponse.<PagingResponse<OrderResponse>>builder()
                .code(ResponseCode.ORDER_GET_SUCCESS.getCode())
                .message(ResponseCode.ORDER_GET_SUCCESS.getMessage())
                .data(orderService.getAllOrders(pageable, status, from, to, keyword))
                .build();
    }

    @PutMapping("/admin/{id}/status")
    public ApiResponse<OrderResponse> updateOrderStatus(@PathVariable Long id, @RequestBody OrderUpdateStatusRequest status) {
        return ApiResponse.<OrderResponse>builder()
                .code(ResponseCode.ORDER_STATUS_UPDATE_SUCCESS.getCode())
                .message(ResponseCode.ORDER_STATUS_UPDATE_SUCCESS.getMessage())
                .data(orderService.updateOrderStatus(id, status))
                .build();
    }
    @GetMapping("/admin/{id}")
    public ApiResponse<OrderResponse> getAdminOrderById(@PathVariable Long id) {
        return ApiResponse.<OrderResponse>builder()
                .code(ResponseCode.ORDER_GET_BY_ID_SUCCESS.getCode())
                .message(ResponseCode.ORDER_GET_BY_ID_SUCCESS.getMessage())
                .data(orderService.getOrderByIdWithRoleAdmin(id))
                .build();
    }
}