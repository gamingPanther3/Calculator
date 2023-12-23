package com.mlprograms.rechenmax;

import android.annotation.SuppressLint;
import android.util.Log;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CalculatorActivity
 * @author Max Lemberg
 * @version 1.7.1
 * @date 23.12.2023
 */

public class CalculatorActivity {
    // Declaration of a static variable of type MainActivity. This variable is used to access the methods and variables of the MainActivity class.
    @SuppressLint("StaticFieldLeak")
    private static MainActivity mainActivity;

    // Method to set the MainActivity. This method is used to initialize the static variable mainActivity.
    public static void setMainActivity(MainActivity activity) {
        mainActivity = activity;
    }

    // Declaration of a constant of type MathContext with a precision of 10. This is used for division to ensure a precision of 10 decimal places.
    private static final MathContext MC = new MathContext(10);

    // Declaration of a constant for the root operation.
    public static final String ROOT = "√";

    /**
     * This method calculates the result of a mathematical expression. The expression is passed as a string parameter.
     * <p>
     * It first replaces all the special characters in the expression with their corresponding mathematical symbols.
     * <p>
     * If the expression is in scientific notation, it converts it to decimal notation.
     * <p>
     * It then tokenizes the expression and evaluates it.
     * <p>
     * If the result is too large, it returns "Wert zu groß" (Value too large).
     * If the result is in scientific notation, it formats it to decimal notation.
     * <p>
     * It handles various exceptions such as ArithmeticException, IllegalArgumentException, and other exceptions.
     *
     * @param calc The mathematical expression as a string to be calculated.
     * @return The result of the calculation as a string.
     * @throws ArithmeticException If there is an arithmetic error in the calculation.
     * @throws IllegalArgumentException If there is an illegal argument in the calculation.
     */
    public static String calculate(final String calc) {
        try {
            // Replace all the special characters in the expression with their corresponding mathematical symbols
            String trim = calc.replace('×', '*')
                    .replace('÷', '/')
                    .replace("=", "")
                    .replace(".", "")
                    .replace(",", ".")
                    .replace("E", "e")
                    .trim();

            // If the expression is in scientific notation, convert it to decimal notation
            if (isScientificNotation(trim)) {
                mainActivity.setIsNotation(true);
                String result = convertScientificToDecimal(trim);
                return removeNonNumeric(result);
            }

            // Tokenize the expression and evaluate it
            final String expression = convertScientificToDecimal(trim);
            final List<String> tokens = tokenize(expression);

            for (int i = 0; i < tokens.size() - 1; i++) {
                // If the expression contains division by zero, return "Infinity"
                try {
                    if (tokens.get(i).equals("/") && Double.parseDouble(tokens.get(i + 1)) <= 0) {
                        return "Unendlich";
                    }
                } catch (Exception e) {
                    // do nothing
                }
            }
            final BigDecimal result = evaluate(tokens);
            double resultDouble = result.doubleValue();
            // If the result is too large, return "Wert zu groß"
            if (Double.isInfinite(resultDouble)) {
                return "Wert zu groß";
            }
            // If the result is larger than a certain threshold, return it in scientific notation
            if (result.compareTo(new BigDecimal("1000000000000000000")) >= 0) {
                return String.format(Locale.GERMANY, "%.10e", result);
            } else {
                // Otherwise, return the result in decimal notation
                return result.stripTrailingZeros().toPlainString().replace('.', ',');
            }
        } catch (ArithmeticException e) {
            // Handle exceptions related to arithmetic errors
            if (Objects.equals(e.getMessage(), "Wert zu groß")) {
                return "Wert zu groß";
            } else {
                return e.getMessage();
            }
        } catch (IllegalArgumentException e) {
            // Handle exceptions related to illegal arguments
            return e.getMessage();
        } catch (Exception e) {
            // Handle all other exceptions
            return "Syntax Fehler";
        }
    }

    /**
     * This method checks if a string is in scientific notation. Scientific notation is a way of expressing numbers that are too large or too small to be conveniently written in decimal form.
     *
     * @param str The string to be checked.
     * @return True if the string is in scientific notation, false otherwise.
     */
    public static boolean isScientificNotation(final String str) {
        // The input string is formatted by replacing all commas with dots. This is because in some locales, a comma is used as the decimal separator.
        final String formattedInput = str.replace(",", ".");

        // A regular expression pattern is defined to match the scientific notation. The pattern is as follows:
        // "^([-+]?\\d+(\\.\\d+)?)(e[+-]\\d+)$"
        // Explanation of the pattern:
        // "^" - start of the line
        // "([-+]?\\d+(\\.\\d+)?)" - matches a number which may be negative or positive, and may have a decimal part
        // "(e[+-]\\d+)" - matches 'e' followed by an optional '+' or '-' sign, followed by one or more digits
        // "$" - end of the line
        final Pattern pattern = Pattern.compile("^([-+]?\\d+(\\.\\d+)?)(e[+-]\\d+)$");

        // The pattern is used to create a matcher for the formatted input string
        final Matcher matcher = pattern.matcher(formattedInput);

        // The method returns true if the matcher finds a match in the input string, indicating that the string is in scientific notation
        return matcher.matches();
    }

    /**
     * This method converts a string in scientific notation to decimal notation.
     *
     * @param str The string in scientific notation to be converted.
     * @return The string in decimal notation. If the exponent part of the scientific notation is greater than 1000, an IllegalArgumentException is thrown with the message "Wert zu groß".
     * @throws IllegalArgumentException if the exponent part of the scientific notation is greater than 1000.
     */
    public static String convertScientificToDecimal(final String str) {
        // The input string is formatted by replacing all commas with dots. This is because in some locales, a comma is used as the decimal separator.
        final String formattedInput = str.replace(",", ".");

        // A regular expression pattern is defined to match the scientific notation. The pattern is the same as in the isScientificNotation method.
        final Pattern pattern = Pattern.compile("([-+]?\\d+(\\.\\d+)?)(e[-+]?\\d+)");

        // The pattern is used to create a matcher for the formatted input string
        final Matcher matcher = pattern.matcher(formattedInput);

        // A StringBuffer is created to hold the result of the conversion
        final StringBuffer sb = new StringBuffer();

        // The matcher is used to find each match in the input string
        while (matcher.find()) {
            // The number part and the exponent part of the match are separated
            final String numberPart = matcher.group(1);
            String exponentPart = matcher.group(3);

            // The 'e' in the exponent part is removed
            if (exponentPart != null) {
                exponentPart = exponentPart.substring(1);
            }

            // The number part and the exponent part are converted to BigDecimal and integer respectively
            assert exponentPart != null;
            final int exponent = Integer.parseInt(exponentPart);

            // Check if exponent is greater than 1000 then return "Wert zu groß"
            if (exponent > 1000) {
                throw new IllegalArgumentException("Wert zu groß");
            }

            final BigDecimal number = new BigDecimal(numberPart);

            // The number is scaled by the power of ten of the exponent
            final BigDecimal scaledNumber = number.scaleByPowerOfTen(exponent);

            // The match in the input string is replaced with the scaled number
            matcher.appendReplacement(sb, scaledNumber.toPlainString());
        }

        // The remaining input string after the last match is appended to the result
        matcher.appendTail(sb);

        // The method returns the result of the conversion
        return sb.toString();
    }

    /**
     * This method removes all non-numeric characters from a string, except for the decimal point and comma.
     * It uses a regular expression to match all characters that are not digits, decimal points, or commas, and replaces them with an empty string.
     *
     * @param str The string to be processed.
     * @return The processed string with all non-numeric characters removed.
     */
    public static String removeNonNumeric(final String str) {
        // Replace all non-numeric and non-decimal point characters in the string with an empty string
        return str.replaceAll("[^0-9.,]", "");
    }

    /**
     * This method tokenizes a mathematical expression.
     * It separates the input string into meaningful units, such as numbers, operators, and parentheses.
     * The output is a list of tokens in the order they appear in the expression.
     *
     * @param expression The mathematical expression to be tokenized.
     * @return A list of tokens representing the components of the mathematical expression.
     */
    public static List<String> tokenize(final String expression) {
        // Debugging: Print input expression
        Log.i("tokenize","Input Expression: " + expression);

        // Replace all spaces in the expression
        String expressionWithoutSpaces = expression.replaceAll("\\s+", "");

        List<String> tokens = new ArrayList<>();
        StringBuilder currentToken = new StringBuilder();

        for (int i = 0; i < expressionWithoutSpaces.length(); i++) {
            char c = expressionWithoutSpaces.charAt(i);

            // If the character is a digit, period, or minus sign (if it's at the beginning or after an opening parenthesis),
            // add it to the current token
            if (Character.isDigit(c) || c == '.' || (c == '-' && (i == 0 || expressionWithoutSpaces.charAt(i - 1) == '('))) {
                currentToken.append(c);
            } else {
                // If the character is an operator, add the current token to the list and reset the current token
                if (currentToken.length() > 0) {
                    tokens.add(currentToken.toString());
                    currentToken.setLength(0);
                }
                tokens.add(Character.toString(c));
            }
        }

        // Add the last token if it exists
        if (currentToken.length() > 0) {
            tokens.add(currentToken.toString());
        }

        // Debugging: Print tokens
        Log.i("tokenize","Tokens: " + tokens);

        return tokens;
    }

    /**
     * This method evaluates a mathematical expression represented as a list of tokens.
     * It first converts the expression from infix notation to postfix notation, then evaluates the postfix expression.
     *
     * @param tokens The mathematical expression in infix notation.
     * @return The result of the expression.
     */
    public static BigDecimal evaluate(final List<String> tokens) {
        // Convert the infix expression to postfix
        final List<String> postfixTokens = infixToPostfix(tokens);

        // Evaluate the postfix expression and return the result
        return evaluatePostfix(postfixTokens);
    }

    /**
     * This method applies an operator to two operands. The operands and the operator are passed as parameters.
     * It supports the following operations: addition, subtraction, multiplication, division, square root, factorial, and power.
     * <p>
     * For each operation, it checks the operator and performs the corresponding operation.
     * <p>
     * For division, it checks if the second operand is zero to avoid division by zero. If it is, it returns "Unendlich" (Infinity).
     * <p>
     * For the square root operation, it checks if the second operand is negative as the root of a negative number is not defined. If it is, it throws an exception.
     * <p>
     * If the operator is not recognized, it throws an exception.
     *
     * @param operand1 The first operand for the operation.
     * @param operand2 The second operand for the operation.
     * @param operator The operator for the operation.
     * @return The result of the operation.
     * @throws IllegalArgumentException If the operator is not recognized or if the second operand for the square root operation is negative.
     */
    public static BigDecimal applyOperator(final BigDecimal operand1, final BigDecimal operand2, final String operator) {
        switch (operator) {
            case "+": // Case for addition
                return operand1.add(operand2);
            case "-": // Case for subtraction
                return operand1.subtract(operand2);
            case "*": // Case for multiplication
                return operand1.multiply(operand2);
            case "/": // Case for division
                if (operand2.compareTo(BigDecimal.ZERO) == 0) { // Check if the second operand is zero to avoid division by zero
                    return new BigDecimal("Unendlich"); // Return "Infinity" if the second operand is zero
                } else {
                    return operand1.divide(operand2, MC); // Perform the division if the second operand is not zero
                }
            case ROOT: // Case for the root operation
                if (operand2.compareTo(BigDecimal.ZERO) < 0) { // Check if the second operand is negative as the root of a negative number is not defined
                    throw new IllegalArgumentException("Nur reelle Zahlen"); // Throw exception if the second operand is negative
                } else {
                    return BigDecimal.valueOf(Math.sqrt(operand2.doubleValue())); // Calculate the square root if the second operand is not negative
                }
            case "!": // Case for factorial
                return factorial(operand1);
            case "^": // Case for power
                return pow(operand1, operand2);
            default: // Default case when the operator is not recognized
                throw new IllegalArgumentException("Unbekannter Operator: " + operator); // Throw exception when the operator is not recognized
        }
    }

    /**
     * This method calculates the factorial of a number. The factorial of a number is the product of all positive integers less than or equal to the number.
     * For example, the factorial of 5 (denoted as 5!) is 1*2*3*4*5 = 120.
     * <p>
     * The method takes a BigDecimal number as input. It first checks if the number is negative. If it is, the number is made positive for the calculation.
     * Then it checks if the number is a whole number because factorial is only defined for whole numbers.
     * <p>
     * It initializes the result to 1. This will hold the calculated factorial.
     * It calculates the factorial by multiplying the number with the result and then decrementing the number, until the number is greater than 1.
     * <p>
     * If the original number was negative, the result is negated. Otherwise, the result is returned as is.
     *
     * @param number The number for which the factorial is to be calculated.
     * @return The factorial of the number.
     * @throws IllegalArgumentException If the number is not a whole number.
     */
    public static BigDecimal factorial(BigDecimal number) {
        // Check if the number is negative
        boolean isNegative = number.compareTo(BigDecimal.ZERO) < 0;
        // If the number is negative, convert it to positive
        if (isNegative) {
            number = number.negate();
        }
        // Check if the number is an integer. If not, throw an exception
        if (number.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("Domainfehler");
        }
        // Initialize the result as 1
        BigDecimal result = BigDecimal.ONE;
        // Calculate the factorial of the number
        while (number.compareTo(BigDecimal.ONE) > 0) {
            result = result.multiply(number);
            number = number.subtract(BigDecimal.ONE);
        }
        // If the original number was negative, return the negative of the result. Otherwise, return the result.
        return isNegative ? result.negate() : result;
    }

    /**
     * This method calculates the power of a base number to an exponent.
     * It first converts the base and exponent to double values, then uses the Math.pow method to calculate the power.
     * If the result is infinite (which can happen if the base and exponent are too large), it throws an ArithmeticException.
     * If the result is not a valid number format, it throws a NumberFormatException.
     *
     * @param base The base number.
     * @param exponent The exponent.
     * @return The result of raising the base to the power of the exponent.
     * @throws ArithmeticException If the result is too large to be represented as a double.
     * @throws NumberFormatException If the result is not a valid number format.
     */
    public static BigDecimal pow(BigDecimal base, BigDecimal exponent) {
        // Convert the base and exponent to double values
        double baseDouble = base.doubleValue();
        double exponentDouble = exponent.doubleValue();

        // Calculate the power of the base to the exponent
        double resultDouble = Math.pow(baseDouble, exponentDouble);

        // If the result is too large to be represented as a double, throw an exception
        if (Double.isInfinite(resultDouble)) {
            throw new ArithmeticException("Wert zu groß");
        }

        // Convert the result back to a BigDecimal and return it
        try {
            return new BigDecimal(resultDouble, MC).stripTrailingZeros();
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Ungültiges Zahlenformat");
        }
    }

    /**
     * This method evaluates a postfix expression.
     * The input is a list of tokens in the postfix expression.
     * The output is the result of the expression as a BigDecimal.
     *
     * @param postfixTokens The list of tokens in the postfix expression.
     * @return The result of the expression as a BigDecimal.
     */
    public static BigDecimal evaluatePostfix(final List<String> postfixTokens) {
        // Create a stack to store numbers
        final List<BigDecimal> stack = new ArrayList<>();

        // Iterate through each token in the postfix list
        for (final String token : postfixTokens) {
            // Debugging: Print current token
            Log.i("evaluatePostfix","Token: " + token);

            // If the token is a number, add it to the stack
            if (isNumber(token)) {
                stack.add(new BigDecimal(token));
            }
            // If the token is an operator, apply the operator to the numbers in the stack
            else if (isOperator(token)) {
                // If the operator is "!", apply the operator to only one number
                if (token.equals("!")) {
                    final BigDecimal operand1 = stack.remove(stack.size() - 1);
                    final BigDecimal result = applyOperator(operand1, BigDecimal.ZERO, token);
                    stack.add(result);
                }
                // If the operator is not "!", apply the operator to two numbers
                else {
                    final BigDecimal operand2 = stack.remove(stack.size() - 1);
                    // If the operator is not ROOT, apply the operator to two numbers
                    if (!token.equals(ROOT)) {
                        final BigDecimal operand1 = stack.remove(stack.size() - 1);
                        final BigDecimal result = applyOperator(operand1, operand2, token);
                        stack.add(result);
                    }
                    // If the operator is ROOT, apply the operator to only one number
                    else {
                        final BigDecimal operand2SquareRoot = applyOperator(BigDecimal.ZERO, operand2, ROOT);
                        stack.add(operand2SquareRoot);
                    }
                }
            }
            // If the token is neither a number nor an operator, throw an exception
            else {
                Log.i("evaluatePostfix","Token is neither a number nor an operator");
                throw new IllegalArgumentException("Syntax Fehler");
            }

            // Debugging: Print current stack
            Log.i("evaluatePostfix","Stack: " + stack);
        }

        // If there is more than one number in the stack at the end, throw an exception
        if (stack.size() != 1) {
            Log.i("evaluatePostfix","Stacksize != 1");
            throw new IllegalArgumentException("Syntax Fehler");
        }

        // Return the result
        return stack.get(0);
    }

    /**
     * This method converts an infix expression to a postfix expression.
     * The input is a list of tokens in the infix expression.
     * The output is a list of tokens in the postfix expression.
     *
     * @param infixTokens The list of tokens in the infix expression.
     * @return The list of tokens in the postfix expression.
     */
    public static List<String> infixToPostfix(final List<String> infixTokens) {
        // Create a list to store the postfix tokens
        final List<String> postfixTokens = new ArrayList<>();
        // Create a stack to store the operators
        final Stack<String> stack = new Stack<>();

        // Iterate through each token in the infix list
        for (final String token : infixTokens) {
            // Debugging: Print current token and stack
            Log.i("infixToPostfix","Current Token: " + token);
            Log.i("infixToPostfix","Stack: " + stack);

            // If the token is a number, add it to the postfix list
            if (isNumber(token)) {
                postfixTokens.add(token);
            }
            // If the token is an operator, apply the operator to the numbers in the postfix list
            else if (isOperator(token)) {
                // While the stack is not empty and the operator on the stack has higher or equal precedence, add the operator to the postfix list
                while (!stack.isEmpty() && precedence(stack.peek()) >= precedence(token)) {
                    postfixTokens.add(stack.pop());
                }
                // Add the current operator to the stack
                stack.push(token);
            }
            // If the token is an opening parenthesis, add it to the stack
            else if (token.equals("(")) {
                stack.push(token);
            }
            // If the token is a closing parenthesis, add all operators to the postfix list until an opening parenthesis is found
            else if (token.equals(")")) {
                while (!stack.peek().equals("(")) {
                    postfixTokens.add(stack.pop());
                }
                // Remove the opening parenthesis from the stack
                stack.pop();
            }

            // Debugging: Print postfixTokens and stack after processing current token
            Log.i("infixToPostfix","Postfix Tokens: " + postfixTokens);
            Log.i("infixToPostfix","Stack after Token Processing: " + stack);
        }

        // Add all remaining operators on the stack to the postfix list
        while (!stack.isEmpty()) {
            postfixTokens.add(stack.pop());
        }

        // Debugging: Print final postfixTokens
        Log.i("infixToPostfix","Final Postfix Tokens: " + postfixTokens);

        // Return the postfix list
        return postfixTokens;
    }

    /**
     * This method checks if a token is a number.
     * It tries to create a BigDecimal from the token. If it succeeds, the token is a number. If it fails, the token is not a number.
     *
     * @param token The token to be checked.
     * @return True if the token is a number, false otherwise.
     */
    public static boolean isNumber(final String token) {
        // Try to create a new BigDecimal from the token
        try {
            new BigDecimal(token);
            // If successful, the token is a number
            return true;
        }
        // If a NumberFormatException is thrown, the token is not a number
        catch (final NumberFormatException e) {
            return false;
        }
    }

    /**
     * This method checks if a token is an operator.
     * It checks if the token is one of the following operators: "+", "-", "*", "/", "^", "√", "!".
     *
     * @param token The token to be checked.
     * @return True if the token is an operator, false otherwise.
     */
    public static boolean isOperator(final String token) {
        // Check if the token is one of the recognized operators
        return token.equals("+") || token.equals("-") || token.equals("*") || token.equals("/") || token.equals("^") || token.equals("√") || token.equals("!");
    }

    /**
     * This method determines the precedence of an operator.
     * Precedence rules determine the order in which expressions involving both unary and binary operators are evaluated.
     *
     * @param operator The operator to be checked.
     * @return The precedence of the operator.
     * @throws IllegalArgumentException If the operator is not recognized.
     */
    public static int precedence(final String operator) {
        // If the operator is an opening parenthesis, return 0
        switch (operator) {
            case "(":
                return 0;

            // If the operator is addition or subtraction, return 1
            case "+":
            case "-":
                return 1;

            // If the operator is multiplication or division, return 2
            case "*":
            case "/":
                return 2;

            // If the operator is exponentiation, return 3
            case "^":
                return 3;

            // If the operator is square root, return 4
            case "√":
                return 4;

            // If the operator is factorial, return 5
            case "!":
                return 5;

            // If the operator is not recognized, throw an exception
            default:
                throw new IllegalArgumentException("Unbekannter Operator: " + operator);
        }
    }
}
