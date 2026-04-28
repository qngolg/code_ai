package com.codeai.domain.aggregate;

import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@ToString
public class Order extends AggregateRoot {

    private Long id;
    private String orderName;
    private BigDecimal price;
    private LocalDateTime createTime;

    public Order(Long id, String orderName, BigDecimal price, LocalDateTime createTime) {
        this.id = id;
        this.orderName = orderName;
        this.price = price;
        this.createTime = createTime;
    }

    public static Order create(String orderName, BigDecimal price) {
        if (orderName == null || orderName.isBlank()) {
            throw new IllegalArgumentException("订单名称不能为空");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("订单价格不能为空且不能为负数");
        }
        return new Order(null, orderName, price, LocalDateTime.now());
    }

    public void updateOrderName(String orderName) {
        if (orderName == null || orderName.isBlank()) {
            throw new IllegalArgumentException("订单名称不能为空");
        }
        this.orderName = orderName;
    }

    public void updatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("订单价格不能为空且不能为负数");
        }
        this.price = price;
    }
}