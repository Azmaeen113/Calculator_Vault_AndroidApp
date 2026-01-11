package com.example.calculator_vault_androidapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.calculator_vault_androidapp.database.DatabaseHelper;
import com.example.calculator_vault_androidapp.database.FirebaseHelper;
import com.example.calculator_vault_androidapp.models.CalculationHistory;
import com.example.calculator_vault_androidapp.utils.CryptoUtils;
import com.google.android.material.button.MaterialButton;

import java.util.Stack;

/**
 * Main calculator activity that also serves as the entry point to the hidden vault.
 * Features: Basic operations, square root, square, percentage, parentheses, +/-, decimal
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView tvDisplay;
    private TextView tvExpression;
    private DatabaseHelper dbHelper;
    
    private StringBuilder expression = new StringBuilder();
    private StringBuilder currentNumber = new StringBuilder();
    private boolean hasDecimal = false;
    private boolean lastWasOperator = false;
    private boolean lastWasEquals = false;
    private int openParenCount = 0;

    // Store current PIN for vault access (from user input sequence)
    private StringBuilder pinAttempt = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = DatabaseHelper.getInstance(this);

        // Check if first time - need to set up PIN
        if (dbHelper.isFirstTime()) {
            Intent intent = new Intent(this, PinSetupActivity.class);
            intent.putExtra("mode", "setup");
            startActivity(intent);
        }

        // Initialize UI
        initializeUI();
    }

    private void initializeUI() {
        tvDisplay = findViewById(R.id.tvDisplay);
        tvExpression = findViewById(R.id.tvExpression);

        // Number buttons
        int[] numberIds = {R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
                          R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9};
        for (int id : numberIds) {
            findViewById(id).setOnClickListener(this);
        }
        
        // 00 button
        findViewById(R.id.btn00).setOnClickListener(this);

        // Operator buttons
        findViewById(R.id.btnAdd).setOnClickListener(this);
        findViewById(R.id.btnSubtract).setOnClickListener(this);
        findViewById(R.id.btnMultiply).setOnClickListener(this);
        findViewById(R.id.btnDivide).setOnClickListener(this);
        findViewById(R.id.btnEquals).setOnClickListener(this);
        findViewById(R.id.btnClear).setOnClickListener(this);
        
        // Advanced function buttons
        findViewById(R.id.btnDot).setOnClickListener(this);
        findViewById(R.id.btnSqrt).setOnClickListener(this);
        findViewById(R.id.btnSquare).setOnClickListener(this);
        findViewById(R.id.btnPercent).setOnClickListener(this);
        findViewById(R.id.btnBackspace).setOnClickListener(this);
        findViewById(R.id.btnOpenParen).setOnClickListener(this);
        findViewById(R.id.btnCloseParen).setOnClickListener(this);
        findViewById(R.id.btnPlusMinus).setOnClickListener(this);

        // History button
        findViewById(R.id.btnHistory).setOnClickListener(v -> {
            Intent intent = new Intent(this, HistoryActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.btn0) onNumberClick("0");
        else if (id == R.id.btn00) onDoubleZeroClick();
        else if (id == R.id.btn1) onNumberClick("1");
        else if (id == R.id.btn2) onNumberClick("2");
        else if (id == R.id.btn3) onNumberClick("3");
        else if (id == R.id.btn4) onNumberClick("4");
        else if (id == R.id.btn5) onNumberClick("5");
        else if (id == R.id.btn6) onNumberClick("6");
        else if (id == R.id.btn7) onNumberClick("7");
        else if (id == R.id.btn8) onNumberClick("8");
        else if (id == R.id.btn9) onNumberClick("9");
        else if (id == R.id.btnAdd) onOperatorClick("+");
        else if (id == R.id.btnSubtract) onOperatorClick("-");
        else if (id == R.id.btnMultiply) onOperatorClick("×");
        else if (id == R.id.btnDivide) onOperatorClick("÷");
        else if (id == R.id.btnEquals) onEqualsClick();
        else if (id == R.id.btnClear) onClearClick();
        else if (id == R.id.btnDot) onDotClick();
        else if (id == R.id.btnSqrt) onSqrtClick();
        else if (id == R.id.btnSquare) onSquareClick();
        else if (id == R.id.btnPercent) onPercentClick();
        else if (id == R.id.btnBackspace) onBackspaceClick();
        else if (id == R.id.btnOpenParen) onOpenParenClick();
        else if (id == R.id.btnCloseParen) onCloseParenClick();
        else if (id == R.id.btnPlusMinus) onPlusMinusClick();
    }

    private void onNumberClick(String digit) {
        // If last action was equals, start fresh
        if (lastWasEquals) {
            expression.setLength(0);
            currentNumber.setLength(0);
            hasDecimal = false;
            lastWasEquals = false;
            pinAttempt.setLength(0);
        }

        // Prevent leading zeros
        if (digit.equals("0") && currentNumber.toString().equals("0")) {
            return;
        }
        if (!digit.equals("0") && currentNumber.toString().equals("0")) {
            currentNumber.setLength(0);
        }

        currentNumber.append(digit);
        expression.append(digit);
        lastWasOperator = false;
        
        // Track PIN attempt (only track consecutive digits)
        pinAttempt.append(digit);
        
        // Check for PIN match (exactly 5 digits entered consecutively)
        if (pinAttempt.length() == 5) {
            String potentialPin = pinAttempt.toString();
            String storedHash = dbHelper.getPinHash();
            
            if (storedHash != null && CryptoUtils.verifyPin(potentialPin, storedHash)) {
                // PIN matched! Open vault
                openVault(potentialPin);
                return;
            }
        }

        updateDisplay();
    }

    private void onOperatorClick(String op) {
        // Reset PIN attempt when operator is pressed
        pinAttempt.setLength(0);

        // If last action was equals, use the result as the starting point
        if (lastWasEquals) {
            String result = currentNumber.toString();
            expression.setLength(0);
            expression.append(result);
            lastWasEquals = false;
        }

        // Allow minus for negative numbers at start or after open paren
        if (op.equals("-") && (expression.length() == 0 || 
            expression.charAt(expression.length() - 1) == '(')) {
            currentNumber.append("-");
            expression.append("-");
            updateDisplay();
            return;
        }

        if (expression.length() == 0) return;

        // Replace operator if last was operator
        if (lastWasOperator) {
            expression.deleteCharAt(expression.length() - 1);
        }

        expression.append(op);
        currentNumber.setLength(0);
        hasDecimal = false;
        lastWasOperator = true;
        
        updateDisplay();
    }

    private void onDotClick() {
        if (hasDecimal) return;
        
        if (lastWasEquals) {
            expression.setLength(0);
            currentNumber.setLength(0);
            lastWasEquals = false;
        }
        
        if (currentNumber.length() == 0) {
            currentNumber.append("0");
            expression.append("0");
        }
        
        currentNumber.append(".");
        expression.append(".");
        hasDecimal = true;
        lastWasOperator = false;
        pinAttempt.setLength(0);
        
        updateDisplay();
    }

    private void onSqrtClick() {
        pinAttempt.setLength(0);
        
        if (currentNumber.length() > 0) {
            try {
                double value = Double.parseDouble(currentNumber.toString());
                if (value < 0) {
                    Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show();
                    return;
                }
                double result = Math.sqrt(value);
                String resultStr = formatNumber(result);
                
                // Replace current number in expression
                int numLen = currentNumber.length();
                expression.delete(expression.length() - numLen, expression.length());
                expression.append("√").append(currentNumber).append("→").append(resultStr);
                
                currentNumber.setLength(0);
                currentNumber.append(resultStr);
                hasDecimal = resultStr.contains(".");
                
                updateDisplay();
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void onSquareClick() {
        pinAttempt.setLength(0);
        
        if (currentNumber.length() > 0) {
            try {
                double value = Double.parseDouble(currentNumber.toString());
                double result = value * value;
                String resultStr = formatNumber(result);
                
                // Replace current number in expression
                int numLen = currentNumber.length();
                expression.delete(expression.length() - numLen, expression.length());
                expression.append(currentNumber).append("²→").append(resultStr);
                
                currentNumber.setLength(0);
                currentNumber.append(resultStr);
                hasDecimal = resultStr.contains(".");
                
                updateDisplay();
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void onPercentClick() {
        pinAttempt.setLength(0);
        
        if (currentNumber.length() > 0) {
            try {
                double value = Double.parseDouble(currentNumber.toString());
                double result = value / 100.0;
                String resultStr = formatNumber(result);
                
                // Replace current number in expression
                int numLen = currentNumber.length();
                expression.delete(expression.length() - numLen, expression.length());
                expression.append(resultStr);
                
                currentNumber.setLength(0);
                currentNumber.append(resultStr);
                hasDecimal = resultStr.contains(".");
                
                updateDisplay();
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void onBackspaceClick() {
        if (expression.length() > 0) {
            char lastChar = expression.charAt(expression.length() - 1);
            expression.deleteCharAt(expression.length() - 1);
            
            if (Character.isDigit(lastChar) || lastChar == '.') {
                if (currentNumber.length() > 0) {
                    char removed = currentNumber.charAt(currentNumber.length() - 1);
                    currentNumber.deleteCharAt(currentNumber.length() - 1);
                    if (removed == '.') hasDecimal = false;
                    if (pinAttempt.length() > 0) {
                        pinAttempt.deleteCharAt(pinAttempt.length() - 1);
                    }
                }
                lastWasOperator = false;
            } else if (lastChar == '(') {
                openParenCount--;
                lastWasOperator = false;
            } else if (lastChar == ')') {
                openParenCount++;
                lastWasOperator = false;
            } else {
                // Removed an operator
                lastWasOperator = false;
                // Rebuild currentNumber from expression
                rebuildCurrentNumber();
            }
            
            updateDisplay();
        }
        lastWasEquals = false;
    }
    
    private void rebuildCurrentNumber() {
        currentNumber.setLength(0);
        hasDecimal = false;
        
        for (int i = expression.length() - 1; i >= 0; i--) {
            char c = expression.charAt(i);
            if (Character.isDigit(c) || c == '.') {
                currentNumber.insert(0, c);
                if (c == '.') hasDecimal = true;
            } else if (c == '-' && (i == 0 || !Character.isDigit(expression.charAt(i-1)))) {
                currentNumber.insert(0, c);
                break;
            } else {
                break;
            }
        }
    }

    private void onOpenParenClick() {
        pinAttempt.setLength(0);
        
        if (lastWasEquals) {
            expression.setLength(0);
            currentNumber.setLength(0);
            hasDecimal = false;
            lastWasEquals = false;
        }
        
        // Add multiplication if there's a number before
        if (currentNumber.length() > 0 || 
            (expression.length() > 0 && expression.charAt(expression.length() - 1) == ')')) {
            expression.append("×");
        }
        
        expression.append("(");
        openParenCount++;
        currentNumber.setLength(0);
        hasDecimal = false;
        lastWasOperator = false;
        
        updateDisplay();
    }

    private void onCloseParenClick() {
        pinAttempt.setLength(0);
        
        if (openParenCount > 0 && !lastWasOperator) {
            expression.append(")");
            openParenCount--;
            currentNumber.setLength(0);
            lastWasOperator = false;
            
            updateDisplay();
        }
    }

    private void onPlusMinusClick() {
        pinAttempt.setLength(0);
        
        if (currentNumber.length() > 0) {
            int numLen = currentNumber.length();
            String numStr = currentNumber.toString();
            
            if (numStr.startsWith("-")) {
                // Remove the negative
                currentNumber.deleteCharAt(0);
                expression.delete(expression.length() - numLen, expression.length());
                expression.append(currentNumber);
            } else {
                // Add negative
                currentNumber.insert(0, "-");
                expression.delete(expression.length() - numLen, expression.length());
                expression.append(currentNumber);
            }
            
            updateDisplay();
        }
    }

    private void onEqualsClick() {
        // Reset PIN attempt
        pinAttempt.setLength(0);

        if (expression.length() == 0) return;
        
        // Store the original expression BEFORE closing parentheses for display
        String originalExpression = expression.toString().replaceAll("→[0-9.-]+", "");
        
        // Close any open parentheses
        while (openParenCount > 0) {
            expression.append(")");
            openParenCount--;
        }

        try {
            String expressionStr = expression.toString();
            // Clean up expression for display (remove internal arrows)
            String displayExpression = expressionStr.replaceAll("→[0-9.-]+", "");
            
            // Prepare for evaluation
            String evalExpression = expressionStr
                .replaceAll("√[0-9.-]+→", "")
                .replaceAll("[0-9.-]+²→", "")
                .replace("×", "*")
                .replace("÷", "/");
            
            double result = evaluateExpression(evalExpression);
            String resultStr = formatNumber(result);

            // Save to local database
            long historyId = dbHelper.saveCalculation(displayExpression, resultStr);

            // Backup to Firebase if user is signed in
            if (historyId != -1 && FirebaseHelper.getInstance().isAuthenticated()) {
                CalculationHistory history = new CalculationHistory();
                history.setId((int) historyId);
                history.setExpression(displayExpression);
                history.setResult(resultStr);
                history.setCalculatedAt(new java.text.SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));
                
                FirebaseHelper.getInstance().backupCalculationHistory(history, 
                    new FirebaseHelper.SyncCallback() {
                        @Override
                        public void onSuccess() {
                            // Successfully synced to Firebase
                        }
                        @Override
                        public void onFailure(String error) {
                            // Failed to sync - data is still saved locally
                        }
                    });
            }

            // Show result - top line shows original expression (no =), main display shows result
            tvExpression.setText(originalExpression);
            tvDisplay.setText(resultStr);

            // Reset for next calculation - but DON'T put result in expression yet
            // This way originalExpression won't be corrupted if user types more
            expression.setLength(0);
            currentNumber.setLength(0);
            currentNumber.append(resultStr);  // Store result for potential continued calculation
            hasDecimal = resultStr.contains(".");
            lastWasOperator = false;
            lastWasEquals = true;
            openParenCount = 0;
            
        } catch (Exception e) {
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
        }
    }

    private double evaluateExpression(String expr) {
        // Simple expression evaluator using two stacks
        Stack<Double> numbers = new Stack<>();
        Stack<Character> operators = new Stack<>();
        
        int i = 0;
        while (i < expr.length()) {
            char c = expr.charAt(i);
            
            if (c == ' ') {
                i++;
                continue;
            }
            
            if (Character.isDigit(c) || (c == '-' && (i == 0 || !Character.isDigit(expr.charAt(i-1)) && expr.charAt(i-1) != ')'))) {
                StringBuilder num = new StringBuilder();
                if (c == '-') {
                    num.append(c);
                    i++;
                }
                while (i < expr.length() && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) {
                    num.append(expr.charAt(i));
                    i++;
                }
                numbers.push(Double.parseDouble(num.toString()));
                continue;
            }
            
            if (c == '(') {
                operators.push(c);
            } else if (c == ')') {
                while (!operators.isEmpty() && operators.peek() != '(') {
                    numbers.push(applyOp(operators.pop(), numbers.pop(), numbers.pop()));
                }
                if (!operators.isEmpty()) operators.pop();
            } else if (isOperator(c)) {
                while (!operators.isEmpty() && precedence(c) <= precedence(operators.peek())) {
                    numbers.push(applyOp(operators.pop(), numbers.pop(), numbers.pop()));
                }
                operators.push(c);
            }
            i++;
        }
        
        while (!operators.isEmpty()) {
            numbers.push(applyOp(operators.pop(), numbers.pop(), numbers.pop()));
        }
        
        return numbers.isEmpty() ? 0 : numbers.pop();
    }
    
    private boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/';
    }
    
    private int precedence(char op) {
        if (op == '(' || op == ')') return 0;
        if (op == '+' || op == '-') return 1;
        if (op == '*' || op == '/') return 2;
        return 0;
    }
    
    private double applyOp(char op, double b, double a) {
        switch (op) {
            case '+': return a + b;
            case '-': return a - b;
            case '*': return a * b;
            case '/': 
                if (b == 0) throw new ArithmeticException("Division by zero");
                return a / b;
        }
        return 0;
    }

    private void onClearClick() {
        expression.setLength(0);
        currentNumber.setLength(0);
        hasDecimal = false;
        lastWasOperator = false;
        lastWasEquals = false;
        openParenCount = 0;
        pinAttempt.setLength(0);
        tvExpression.setText("");
        tvDisplay.setText("0");
    }

    private String formatNumber(double number) {
        if (Double.isNaN(number) || Double.isInfinite(number)) {
            return "Error";
        }
        if (number == (long) number && Math.abs(number) < 1e15) {
            return String.valueOf((long) number);
        } else {
            // Use maximum 10 decimal places, trim trailing zeros
            String formatted = String.format("%.10f", number)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
            
            // If result is too long, use scientific notation
            if (formatted.length() > 15) {
                formatted = String.format("%.6E", number);
            }
            return formatted;
        }
    }

    private void updateDisplay() {
        // Show full expression in top line (e.g., "2+3")
        String expressionText = expression.length() > 0 ? expression.toString() : "";
        // Clean up internal markers from display
        expressionText = expressionText.replaceAll("→[0-9.-]+", "");
        tvExpression.setText(expressionText);
        
        // Main display always shows the full expression being built
        tvDisplay.setText(expressionText.isEmpty() ? "0" : expressionText);
    }
    
    private void onDoubleZeroClick() {
        // Add 00 (two zeros)
        if (lastWasEquals) {
            expression.setLength(0);
            currentNumber.setLength(0);
            hasDecimal = false;
            lastWasEquals = false;
            pinAttempt.setLength(0);
        }
        
        // Don't add 00 if current number is just "0"
        if (currentNumber.toString().equals("0")) {
            return;
        }
        
        currentNumber.append("00");
        expression.append("00");
        lastWasOperator = false;
        pinAttempt.append("00");
        
        updateDisplay();
    }

    private void openVault(String pin) {
        // Clear display and state
        onClearClick();
        
        // Open vault activity with PIN
        Intent intent = new Intent(this, VaultActivity.class);
        intent.putExtra("pin", pin);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Clear any sensitive state when returning to calculator
        pinAttempt.setLength(0);
    }
}