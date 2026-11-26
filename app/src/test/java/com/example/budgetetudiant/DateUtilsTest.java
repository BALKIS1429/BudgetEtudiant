package com.example.budgetetudiant;

import org.junit.Test;

import static org.junit.Assert.*;

public class DateUtilsTest {

    @Test
    public void testMonthOf() {
        assertEquals("2026-11", DateUtils.monthOf("2026-11-27"));
    }

    @Test
    public void testAddMonths() {
        assertEquals("2026-12", DateUtils.addMonths("2026-11", 1));
        assertEquals("2026-10", DateUtils.addMonths("2026-11", -1));
        assertEquals("2027-01", DateUtils.addMonths("2026-11", 2));
    }

    @Test
    public void testMonthLabel() {
        assertNotNull(DateUtils.monthLabel("2026-11"));
        assertTrue(DateUtils.monthLabel("2026-11").contains("2026"));
    }

    @Test
    public void testLastMonthKeys() {
        String[] keys = DateUtils.lastMonthKeys(6);
        assertEquals(6, keys.length);
    }
}
