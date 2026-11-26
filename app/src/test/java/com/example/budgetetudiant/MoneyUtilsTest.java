package com.example.budgetetudiant;

import org.junit.Test;

import static org.junit.Assert.*;

public class MoneyUtilsTest {

    @Test
    public void testParseAmountValid() {
        assertEquals(15.5, MoneyUtils.parseAmount("15.5"), 0.001);
        assertEquals(15.5, MoneyUtils.parseAmount("15,5"), 0.001);
        assertEquals(1234.567, MoneyUtils.parseAmount("1 234,567"), 0.001);
    }

    @Test
    public void testParseAmountInvalid() {
        assertTrue(Double.isNaN(MoneyUtils.parseAmount("")));
        assertTrue(Double.isNaN(MoneyUtils.parseAmount(null)));
        assertTrue(Double.isNaN(MoneyUtils.parseAmount("abc")));
    }

    @Test
    public void testIsValidAmount() {
        assertTrue(MoneyUtils.isValidAmount(10.0));
        assertFalse(MoneyUtils.isValidAmount(0.0));
        assertFalse(MoneyUtils.isValidAmount(-5.0));
        assertFalse(MoneyUtils.isValidAmount(Double.NaN));
    }

    @Test
    public void testPercentCalculation() {
        assertEquals(50, MoneyUtils.percent(50, 100));
        assertEquals(80, MoneyUtils.percent(800, 1000));
        assertEquals(0, MoneyUtils.percent(10, 0));
    }
}
