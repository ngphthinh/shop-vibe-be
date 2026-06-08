package org.ngphthinh.mapper;

import org.mapstruct.Mapper;
import org.ngphthinh.dto.request.order.OrderCreateRequest;
import org.ngphthinh.dto.response.order.OrderItemResponse;
import org.ngphthinh.dto.response.order.OrderResponse;
import org.ngphthinh.dto.response.order.PaymentResponse;
import org.ngphthinh.entity.Order;
import org.ngphthinh.repository.projection.OrderProjection;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Mapper(componentModel = "spring")
public interface OrderMapper {

    Order toOrder(OrderCreateRequest request);

    OrderResponse toOrderResponse(Order order);


    default List<OrderResponse> mapToOrderResponses(List<OrderProjection> flatList) {
        if (flatList == null || flatList.isEmpty()) {
            return new ArrayList<>();
        }

        return flatList.stream().map(projection -> {
            OrderResponse response = new OrderResponse();
            response.setId(projection.getId());
            response.setOrderCode(projection.getOrderCode());
            response.setStatus(projection.getStatus().name());
            response.setTotalAmount(projection.getTotalAmount());
            response.setShippingAddress(projection.getShippingAddress());
            response.setNote(projection.getNote());
            response.setItemCount(projection.getItemCount());
            response.setCreatedAt(projection.getCreatedAt());

            PaymentResponse paymentResponse = new PaymentResponse();
            paymentResponse.setMethod(projection.getPaymentMethod());
            paymentResponse.setStatus(projection.getPaymentStatus().name());
            response.setPayment(paymentResponse);

            return response;
        }).collect(Collectors.toList());
    }

    default OrderResponse toOrderResponse(List<OrderProjection> order) {
        if (order == null || order.isEmpty()) {
            return null;
        }

        OrderProjection firstOrder = order.get(0);


        return OrderResponse.builder()
                .id(firstOrder.getId())
                .orderCode(firstOrder.getOrderCode())
                .status(firstOrder.getStatus().name())
                .totalAmount(firstOrder.getTotalAmount())
                .shippingAddress(firstOrder.getShippingAddress())
                .note(firstOrder.getNote())
                .createdAt(firstOrder.getCreatedAt())
                .payment(PaymentResponse.builder()
                        .method(firstOrder.getPaymentMethod())
                        .status(firstOrder.getPaymentStatus().name())
                        .build())
                .items(order.stream().map(projection -> OrderItemResponse.builder()
                        .productId(projection.getProductId())
                        .productName(projection.getProductName())
                        .thumbnailUrl(projection.getProductImageUrl())
                        .unitPrice(projection.getPrice())
                        .quantity(projection.getQuantity())
                        .subtotal(projection.getSubtotal())
                        .build()).collect(Collectors.toList()))
                .build();
    }
}

