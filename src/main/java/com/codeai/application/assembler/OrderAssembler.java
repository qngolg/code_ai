package com.codeai.application.assembler;

import com.codeai.domain.aggregate.Order;
import com.codeai.application.dto.OrderCreateCmd;
import com.codeai.application.dto.OrderDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderAssembler {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    Order toEntity(OrderCreateCmd cmd);

    OrderDTO toDTO(Order order);
}