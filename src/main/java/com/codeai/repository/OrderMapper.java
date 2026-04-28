package com.codeai.repository;

import com.codeai.domain.aggregate.Order;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OrderMapper {

    @Insert("INSERT INTO orders (order_name, price, create_time) VALUES (#{orderName}, #{price}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Order order);

    @Select("SELECT * FROM orders WHERE id = #{id}")
    Order findById(Long id);

    @Delete("DELETE FROM orders WHERE id = #{id}")
    void deleteById(Long id);

    @Select("SELECT * FROM orders")
    List<Order> findAll();
}