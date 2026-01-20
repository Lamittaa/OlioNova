package com.project.order.service;

import com.project.order.dto.OrderStatusResponse;
import com.project.order.exception.ResourceNotFoundException;
import com.project.order.mapper.OrderStatusMapper;
import com.project.order.model.OrderStatus;
import com.project.order.repository.OrderStatusRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderStatusService {

    private final OrderStatusRepo statusRepo;
    private final OrderStatusMapper statusMapper;

    @Transactional(readOnly = true)
    public OrderStatusResponse getStatusById(Long id) {
        OrderStatus status = statusRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order status not found with id: " + id));
        return statusMapper.toOrderStatusResponse(status);
    }

    @Transactional(readOnly = true)
    public List<OrderStatusResponse> getAllStatuses() {
        return statusRepo.findAll()
                .stream()
                .map(statusMapper::toOrderStatusResponse)
                .toList();
    }
}
