package com.project.queue_service.converter;

import com.project.queue_service.model.TicketStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TicketStatusConverter implements AttributeConverter<TicketStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(TicketStatus attribute) {
        return attribute.getId();
    }

    @Override
    public TicketStatus convertToEntityAttribute(Integer dbData) {
        return TicketStatus.fromId(dbData);
    }
}
