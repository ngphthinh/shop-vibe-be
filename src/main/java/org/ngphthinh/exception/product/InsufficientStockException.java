package org.ngphthinh.exception.product;

import lombok.Getter;
import org.ngphthinh.exception.AppException;
import org.ngphthinh.exception.ErrorCode;

@Getter
public class InsufficientStockException extends AppException {

    private String keyProduct;
    private String keyAvailableStock;

    private String valueProduct;
    private Integer valueAvailableStock;

    public InsufficientStockException(String keyProduct, String valueProduct, String keyAvailableStock, Integer valueAvailableStock) {
        super(ErrorCode.INSUFFICIENT_PRODUCT_STOCK);
        this.keyProduct = keyProduct;
        this.keyAvailableStock = keyAvailableStock;
        this.valueProduct = valueProduct;
        this.valueAvailableStock = valueAvailableStock;

    }
}
