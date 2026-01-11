package com.example.calculator_vault_androidapp.models;

/**
 * Model class representing a calculation history entry.
 */
public class CalculationHistory {
    private int id;
    private String expression;
    private String result;
    private String calculatedAt;

    public CalculationHistory() {}

    public CalculationHistory(int id, String expression, String result, String calculatedAt) {
        this.id = id;
        this.expression = expression;
        this.result = result;
        this.calculatedAt = calculatedAt;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getExpression() { return expression; }
    public void setExpression(String expression) { this.expression = expression; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(String calculatedAt) { this.calculatedAt = calculatedAt; }
}
