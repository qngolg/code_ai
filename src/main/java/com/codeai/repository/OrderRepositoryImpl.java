package com.codeai.repository;

import com.codeai.domain.aggregate.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderMapper orderMapper;

    @Override
    public Order save(Order order) {
        if (order.getId() == null) {
            orderMapper.insert(order);
        }
        return order;
    }

    @Override
    public Optional<Order> findById(Long id) {
        return Optional.ofNullable(orderMapper.findById(id));
    }

    @Override
    public void deleteById(Long id) {
        orderMapper.deleteById(id);
    }
}