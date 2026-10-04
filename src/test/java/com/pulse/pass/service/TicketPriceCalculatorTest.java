package com.pulse.pass.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.pulse.pass.domain.TicketType;
import com.pulse.pass.exception.BusinessRuleException;

class TicketPriceCalculatorTest {

    private final TicketPriceCalculator calculator = new TicketPriceCalculator(new BigDecimal("100.00"));

    @ParameterizedTest
    @CsvSource({
            "GENERAL,100.00",
            "STUDENT,50.00",
            "VIP,250.00",
            "BACKSTAGE,500.00"
    })
    void calculate_appliesMultiplierPerTicketType(TicketType type, String expected) {
        // ACT
        BigDecimal price = calculator.calculate(type);

        // ASSERT
        assertThat(price).isEqualByComparingTo(expected);
        assertThat(price.scale()).isEqualTo(2);
    }

    @Test
    void calculate_roundsHalfUpToTwoDecimals() {
        // ARRANGE
        TicketPriceCalculator odd = new TicketPriceCalculator(new BigDecimal("33.33"));

        // ACT
        BigDecimal price = odd.calculate(TicketType.STUDENT); // 16.665

        // ASSERT
        assertThat(price).isEqualByComparingTo("16.67");
    }

    @Test
    void calculate_nullType_throwsBusinessRuleException() {
        assertThatThrownBy(() -> calculator.calculate(null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void constructor_negativeBasePrice_isRejected() {
        assertThatThrownBy(() -> new TicketPriceCalculator(new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void calculate_zeroBasePrice_isAllowedAndNeverNegative() {
        TicketPriceCalculator free = new TicketPriceCalculator(BigDecimal.ZERO);

        assertThat(free.calculate(TicketType.VIP)).isEqualByComparingTo("0.00");
    }
}
