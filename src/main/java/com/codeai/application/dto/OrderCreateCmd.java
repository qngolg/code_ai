package com.codeai.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateCmd {

    @NotBlank(message = "订单名称不能为空")
    private String orderName;

    @NotNull(message = "订单价格不能为空")
    @PositiveOrZero(message = "订单价格不能为负数")
    private BigDecimal price;
}