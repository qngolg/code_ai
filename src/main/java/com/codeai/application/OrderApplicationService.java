package com.codeai.application;

import com.codeai.application.assembler.OrderAssembler;
import com.codeai.application.dto.OrderCreateCmd;
import com.codeai.application.dto.OrderDTO;
import com.codeai.domain.aggregate.Order;
import com.codeai.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderApplicationService {

    private final OrderRepository orderRepository;
    private final OrderAssembler orderAssembler;

    @Transactional(readOnly = true)
    public Optional<OrderDTO> getOrderById(Long id) {
        return orderRepository.findById(id)
                .map(orderAssembler::toDTO);
    }

    @Transactional(readOnly = false)
    public OrderDTO createOrder(OrderCreateCmd cmd) {
        Order order = Order.create(cmd.getOrderName(), cmd.getPrice());
        Order savedOrder = orderRepository.save(order);
        log.info("创建订单成功, orderId={}", savedOrder.getId());
        return orderAssembler.toDTO(savedOrder);
    }
}