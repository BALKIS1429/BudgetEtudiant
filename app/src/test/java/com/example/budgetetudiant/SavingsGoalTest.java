package com.example.budgetetudiant;

import org.junit.Test;

import static org.junit.Assert.*;

public class SavingsGoalTest {

    @Test
    public void testSavingsGoalProgressAndRemaining() {
        SavingsGoal goal = new SavingsGoal(1, "Voyage", 1000.0, 350.0);

        assertEquals(650.0, goal.remainingAmount(), 0.001);
        assertEquals(35, goal.progressPercent());
        assertFalse(goal.isReached());

        goal.savedAmount = 1000.0;
        assertEquals(0.0, goal.remainingAmount(), 0.001);
        assertEquals(100, goal.progressPercent());
        assertTrue(goal.isReached());
    }
}
