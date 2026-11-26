package com.example.budgetetudiant;

/**
 * Objectif d'épargne unique de l'utilisateur.
 */
public class SavingsGoal {

    public int id;
    public String name;
    public double targetAmount;
    public double savedAmount;

    public SavingsGoal(int id, String name, double targetAmount, double savedAmount) {
        this.id = id;
        this.name = name;
        this.targetAmount = targetAmount;
        this.savedAmount = savedAmount;
    }

    public double remainingAmount() {
        return Math.max(0, targetAmount - savedAmount);
    }

    public int progressPercent() {
        if (targetAmount <= 0) {
            return 0;
        }
        return (int) Math.round((savedAmount / targetAmount) * 100);
    }

    public boolean isReached() {
        return targetAmount > 0 && savedAmount >= targetAmount;
    }
}
