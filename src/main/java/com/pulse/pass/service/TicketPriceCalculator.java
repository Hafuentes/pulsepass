package com.pulse.pass.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.pulse.pass.domain.TicketType;
import com.pulse.pass.exception.BusinessRuleException;

/**
 * Estrategia de precios encapsulada (BR-TICKET-009). El cliente nunca envia el precio.
 * <ul>
 *   <li>GENERAL: precio base</li>
 *   <li>STUDENT: 50 % del precio base</li>
 *   <li>VIP: 2.5 x precio base</li>
 *   <li>BACKSTAGE: 5 x precio base</li>
 * </ul>
 * El precio base es configurable con {@code pulsepass.pricing.base-price}.
 */
@Component
public class TicketPriceCalculator {

    private static final int SCALE = 2;

    private final BigDecimal basePrice;

    public TicketPriceCalculator(@Value("${pulsepass.pricing.base-price:100.00}") BigDecimal basePrice) {
        if (basePrice == null || basePrice.signum() < 0) {
            throw new IllegalArgumentException("Base price must be zero or positive.");
        }
        this.basePrice = basePrice;
    }

    public BigDecimal calculate(TicketType type) {
        if (type == null) {
            throw new BusinessRuleException("Ticket type is required.");
        }
        BigDecimal price = basePrice
                .multiply(multiplierFor(type))
                .setScale(SCALE, RoundingMode.HALF_UP);
        if (price.signum() < 0) {
            throw new BusinessRuleException("Ticket price cannot be negative.");
        }
        return price;
    }

    private static BigDecimal multiplierFor(TicketType type) {
        return switch (type) {
            case GENERAL -> BigDecimal.ONE;
            case STUDENT -> new BigDecimal("0.50");
            case VIP -> new BigDecimal("2.50");
            case BACKSTAGE -> new BigDecimal("5.00");
        };
    }
}
