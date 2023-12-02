package com.mlprograms.rechenmax;

import org.apache.commons.math3.util.FastMath;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CalculatorActivity {
    private static MainActivity mainActivity;
    public static void setMainActivity(MainActivity activity) {
        mainActivity = activity;
    }
    private static final MathContext MC = new MathContext(Integer.MAX_VALUE);
    private static final MathContext DIVIDEMC = new MathContext(10);
    public static final String ROOT = "√";
    public static BigDecimal applyOperator(final BigDecimal operand1, final BigDecimal operand2, final String operator) {
        switch (operator) {
            case "+":
                return operand1.add(operand2);
            case "-":
                return operand1.subtract(operand2);
            case "*":
                return operand1.multiply(operand2);
            case "/":
                if (operand2.compareTo(BigDecimal.ZERO) == 0) {
                    return new BigDecimal("Unendlich");
                } else {
                    return operand1.divide(operand2, DIVIDEMC);
                }
            case ROOT:
                if (operand2.compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("Nur reelle Zahlen");
                } else {
                    return new BigDecimal(Math.sqrt(operand2.doubleValue()));
                }
            case "!":
                return factorial(operand1);
            case "^":
                return pow(operand1, operand2);
            default:
                throw new IllegalArgumentException("Unbekannter Operator: " + operator);
        }
    }
    public static BigDecimal factorial(BigDecimal number) {
        if (number.compareTo(BigDecimal.ZERO) < 0) {
            number = number.negate();
        }
        if (number.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("Domainfehler");
        }
        BigDecimal result = BigDecimal.ONE;
        while (number.compareTo(BigDecimal.ONE) > 0) {
            result = result.multiply(number);
            number = number.subtract(BigDecimal.ONE);
        }
        return result;
    }
    public static boolean isFactorial(final String token) {
        return token.endsWith("!");
    }
    public static String calculate(final String calc) {
        try {
            String trim = calc.replace('×', '*')
                    .replace('÷', '/')
                    .replace("=", "")
                    .replace(".", "")
                    .replace(",", ".")
                    .replace("E", "e")
                    .trim();

            if (isScientificNotation(trim)) {
                mainActivity.setIsNotation(true);
                String result = convertScientificToDecimal(trim);
                return removeNonNumeric(result);
            }

            final String expression = convertScientificToDecimal(trim);
            final List<String> tokens = tokenize(expression);
            for (int i = 0; i < tokens.size() - 1; i++) {
                if (tokens.get(i).equals("/") && Double.parseDouble(tokens.get(i + 1)) <= 0) {
                    return "Unendlich";
                }
            }
            final BigDecimal result = evaluate(tokens);
            double resultDouble = result.doubleValue();
            if (Double.isInfinite(resultDouble)) {
                return "Wert zu groß";
            }
            if (result.compareTo(new BigDecimal("1000000000000000000")) >= 0) {
                return String.format(Locale.GERMANY, "%.10e", result);
            } else {
                return result.stripTrailingZeros().toPlainString().replace('.', ',');
            }
        } catch (ArithmeticException e) {
            if (e.getMessage().equals("Wert zu groß")) {
                return "Wert zu groß";
            } else {
                return e.getMessage();
            }
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        } catch (Exception e) {
            return "Syntax Fehler";
        }
    }
    public static boolean isScientificNotation(final String str) {
        final String formattedInput = str.replace(",", ".");
        final Pattern pattern = Pattern.compile("^([-+]?\\d+(\\.\\d+)?)(e[+-]\\d+)$");
        final Matcher matcher = pattern.matcher(formattedInput);
        return matcher.matches();
    }
    public static String convertScientificToDecimal(final String str) {
        final String formattedInput = str.replace(",", ".");
        final Pattern pattern = Pattern.compile("([-+]?\\d+(\\.\\d+)?)(e[+-]\\d+)");
        final Matcher matcher = pattern.matcher(formattedInput);
        final StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            final String numberPart = matcher.group(1);
            String exponentPart = matcher.group(3);
            if (exponentPart != null) {
                exponentPart = exponentPart.substring(1);
            }
            final int exponent = Integer.parseInt(exponentPart);
            final BigDecimal number = new BigDecimal(numberPart);
            final BigDecimal scaledNumber = number.scaleByPowerOfTen(exponent);
            matcher.appendReplacement(sb, scaledNumber.toPlainString());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
    public static String removeNonNumeric(final String str) {
        return str.replaceAll("[^0-9.,]", "");
    }
    public static BigDecimal pow(BigDecimal base, BigDecimal exponent) {
        double baseDouble = base.doubleValue();
        double exponentDouble = exponent.doubleValue();
        double resultDouble = Math.pow(baseDouble, exponentDouble);
        if (Double.isInfinite(resultDouble)) {
            throw new ArithmeticException("Wert zu groß");
        }
        return new BigDecimal(resultDouble, MC);
    }
    public static BigDecimal evaluate(final List<String> tokens) {
        final List<String> postfixTokens = infixToPostfix(tokens);
        return evaluatePostfix(postfixTokens);
    }
    public static BigDecimal evaluatePostfix(final List<String> postfixTokens) {
        final List<BigDecimal> stack = new ArrayList<>();
        for (final String token : postfixTokens) {
            if (isNumber(token)) {
                stack.add(new BigDecimal(token));
            } else if (isOperator(token)) {
                if (token.equals("!")) {
                    final BigDecimal operand1 = stack.remove(stack.size() - 1);
                    final BigDecimal result = applyOperator(operand1, BigDecimal.ZERO, token);
                    stack.add(result);
                } else {
                    final BigDecimal operand2 = stack.remove(stack.size() - 1);
                    if (!token.equals(ROOT)) {
                        final BigDecimal operand1 = stack.remove(stack.size() - 1);
                        final BigDecimal result = applyOperator(operand1, operand2, token);
                        stack.add(result);
                    } else {
                        final BigDecimal operand2SquareRoot = applyOperator(BigDecimal.ZERO, operand2, ROOT);
                        stack.add(operand2SquareRoot);
                    }
                }
            } else {
                throw new IllegalArgumentException("Syntax Fehler");
            }
        }
        if (stack.size() != 1) {
            throw new IllegalArgumentException("Syntax Fehler");
        }

        return stack.get(0);
    }
    public static List<String> infixToPostfix(final List<String> infixTokens) {
        final List<String> postfixTokens = new ArrayList<>();
        final Stack<String> stack = new Stack<>();

        for (final String token : infixTokens) {
            if (isNumber(token)) {
                postfixTokens.add(token);
            } else if (isOperator(token)) {
                while (!stack.isEmpty() && precedence(stack.peek()) >= precedence(token)) {
                    postfixTokens.add(stack.pop());
                }
                stack.push(token);
            } else if (token.equals("(")) {
                stack.push(token);
            } else if (token.equals(")")) {
                while (!stack.peek().equals("(")) {
                    postfixTokens.add(stack.pop());
                }
                stack.pop();
            }
        }
        while (!stack.isEmpty()) {
            postfixTokens.add(stack.pop());
        }

        return postfixTokens;
    }
    public static boolean isNumber(final String token) {
        try {
            new BigDecimal(token);
            return true;
        } catch (final NumberFormatException e) {
            return false;
        }
    }
    public static boolean isOperator(final String token) {
        return token.equals("+") || token.equals("-") || token.equals("*") || token.equals("/") || token.equals("^") || token.equals("√") || token.equals("!");
    }
    public static int precedence(final String operator) {
        if (operator.equals("(")) {
            return 0;
        } else if (operator.equals("+") || operator.equals("-")) {
            return 1;
        } else if (operator.equals("*") || operator.equals("/")) {
            return 2;
        } else if (operator.equals("^")) {
            return 3;
        } else if (operator.equals("√")) {
            return 4;
        } else if (operator.equals("!")) {
            return 5;
        } else {
            throw new IllegalArgumentException("Unbekannter Operator: " + operator);
        }
    }
    public static List<String> tokenize(final String expression) {
        final List<String> tokens = new ArrayList<>();
        final StringBuilder currentToken = new StringBuilder();
        for (int i = 0; i < expression.length(); i++) {
            final char c = expression.charAt(i);
            if (Character.isDigit(c) || c == '.' || (c == '-' && (i == 0 || !Character.isDigit(expression.charAt(i - 1))))) {
                currentToken.append(c);
            } else if (c == '+' || c == '*' || c == '/' || c == '-' || c == '^' || c == '√' || c == '(' || c == ')' || c == '!') {
                if (currentToken.length() > 0) {
                    tokens.add(currentToken.toString());
                    currentToken.setLength(0);
                }
                tokens.add(Character.toString(c));
            } else if (c == ' ') {
                if (currentToken.length() > 0) {
                    tokens.add(currentToken.toString());
                    currentToken.setLength(0);
                }
            }
        }
        if (currentToken.length() > 0) {
            tokens.add(currentToken.toString());
        }
        return tokens;
    }
}
