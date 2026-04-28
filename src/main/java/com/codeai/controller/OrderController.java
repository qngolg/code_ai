package com.codeai.controller;

import com.codeai.application.OrderApplicationService;
import com.codeai.application.dto.OrderCreateCmd;
import com.codeai.application.dto.OrderDTO;
import com.codeai.common.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderApplicationService orderApplicationService;

    @PostMapping
    public ResponseEntity<Result<OrderDTO>> createOrder(@Valid @RequestBody OrderCreateCmd cmd) {
        OrderDTO orderDTO = orderApplicationService.createOrder(cmd);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Result.success(orderDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Result<OrderDTO>> getOrderById(@PathVariable Long id) {
        return orderApplicationService.getOrderById(id)
                .map(orderDTO -> ResponseEntity.ok(Result.success(orderDTO)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Result.error(404, "订单不存在")));
    }
}