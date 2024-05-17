package praktikum2;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
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
 * @version 1.8.7
 * @date 18.01.2023
 */

public class RechenMaxCalculator {

//Declaration of a constant of type MathContext with a precision of 10. This is used for division to ensure a precision of 10 decimal places.
  private static final MathContext MC = new MathContext(11, RoundingMode.HALF_UP);

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
                  .replace("π", "3.1415926535897932384626433832")
                  .trim();

          // If the expression is in scientific notation, convert it to decimal notation
          if (isScientificNotation(trim)) {
              String result = convertScientificToDecimal(trim);
              return removeNonNumeric(result);
          }

          // Tokenize the expression and handle negative exponent in division

          // final String expression = convertScientificToDecimal(trim);
          // final List<String> tokens = tokenize(expression);
          final List<String> tokens = tokenize(trim);

          for (int i = 0; i < tokens.size() - 1; i++) {
              try {
                  if (tokens.get(i).equals("/") && tokens.get(i + 1).equals("-")) {
                      // Handle negative exponent in division
                      tokens.remove(i + 1);
                      tokens.add(i + 1, "NEG_EXPONENT");
                  }
              } catch (Exception e) {
                  // do nothing
              }
          }

          // Evaluate the expression and handle exceptions
          final BigDecimal result = evaluate(tokens);

          double resultDouble = result.doubleValue();
          // If the result is too large, return "Wert zu groß"
          if (Double.isInfinite(resultDouble)) {
              return "Wert zu groß";
          }
          // If the result is larger than a certain threshold, return it in scientific notation
          if (result.compareTo(new BigDecimal("1000000000000000000")) >= 0 || result.precision() > 17) {
              return String.format(Locale.GERMAN, "%.10e", result);
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
          return "Syntax Fehler3";
      }
  }

  public static boolean isScientificNotation(final String str) {
      // The input string is formatted by replacing all commas with dots. This is because in some locales, a comma is used as the decimal separator.
      final String formattedInput = str.replace(",", ".");

      // A regular expression pattern is defined to match the scientific notation. The pattern is as follows:
      // "^([-+]?\\d+(\\.\\d+)?)([eE][-+]?\\d+)$"
      // Explanation of the pattern:
      // "^" - start of the line
      // "([-+]?\\d+(\\.\\d+)?)"" - matches a number which may be negative or positive, and may have a decimal part
      // "([eE][-+]?\\d+)" - matches 'e' or 'E' followed by an optional '+' or '-' sign, followed by one or more digits
      // "$" - end of the line
      final Pattern pattern = Pattern.compile("^([-+]?\\d+(\\.\\d+)?)([eE][-+]?\\d+)$");

      // The pattern is used to create a matcher for the formatted input string
      final Matcher matcher = pattern.matcher(formattedInput);

      // The method returns true if the matcher finds a match in the input string, indicating that the string is in scientific notation
      return matcher.matches();
  }

  public static String convertScientificToDecimal(final String str) {
    // Replace commas with dots for proper decimal representation
    final String formattedInput = str;

    // Define the pattern for scientific notation
    final Pattern pattern = Pattern.compile("([-+]?\\d+(\\.\\d+)?)([eE][-+]?\\d+)");
    final Matcher matcher = pattern.matcher(formattedInput);
    final StringBuffer sb = new StringBuffer();

    // Process all matches found in the input string
    while (matcher.find()) {
        // Extract number and exponent parts from the match
        final String numberPart = matcher.group(1);
        String exponentPart = matcher.group(3);

        // Remove the 'e' or 'E' from the exponent part
        if (exponentPart != null) {
            exponentPart = exponentPart.substring(1);
        }

        // Check and handle the case where the exponent is too large
        if (exponentPart != null) {
            final int exponent = Integer.parseInt(exponentPart);

            // Determine the sign of the number and create a BigDecimal object
            final String sign = numberPart.startsWith("-") ? "-" : "";
            BigDecimal number = new BigDecimal(numberPart);

            // Negate the number if the input starts with a minus sign
            if (numberPart.startsWith("-")) {
                number = number.negate();
            }

            // Scale the number by the power of ten specified by the exponent
            BigDecimal scaledNumber;
            if (exponent >= 0) {
                scaledNumber = number.scaleByPowerOfTen(exponent);
            } else {
                scaledNumber = number.divide(BigDecimal.TEN.pow(-exponent));
            }

            // Remove trailing zeros and append the scaled number to the result buffer
            String result = sign + scaledNumber.stripTrailingZeros().toPlainString();
            if (result.startsWith(".")) {
                result = "0" + result;
            }
            matcher.appendReplacement(sb, result);
        }
    }

    // Append the remaining part of the input string to the result buffer
    matcher.appendTail(sb);

    // Check if the result buffer contains two consecutive minus signs and remove one if necessary
    if (sb.indexOf("--") != -1) {
        sb.replace(sb.indexOf("--"), sb.indexOf("--") + 2, "-");
    }

    // Return the final result as a string
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
      return str.replaceAll("[^0-9.,\\-]", "");
  }
  
  /**
   * Tokenizes a mathematical expression, breaking it into individual components such as numbers, operators, and functions.
   *
   * @param expression The input mathematical expression to be tokenized.
   * @return A list of tokens extracted from the expression.
   */
  public static List<String> tokenize(final String expression) {
      // Remove all spaces from the expression
      String expressionWithoutSpaces = expression.replaceAll("\\s+", "");
  
      List<String> tokens = new ArrayList<>();
      StringBuilder currentToken = new StringBuilder();
  
      for (int i = 0; i < expressionWithoutSpaces.length(); i++) {
          char c = expressionWithoutSpaces.charAt(i);
  
          // If the character is a digit, period, or minus sign (if it's at the beginning, after an opening parenthesis, or after an operator),
          // add it to the current token
          if (Character.isDigit(c) || c == '.' || (c == '-' && (i == 0 || expressionWithoutSpaces.charAt(i - 1) == '(' || isOperator(String.valueOf(expressionWithoutSpaces.charAt(i - 1)))))) {
              currentToken.append(c);
          } else {
              // If the character is an operator or a parenthesis, add the current token to the list and reset the current token
              if (currentToken.length() > 0) {
                  tokens.add(currentToken.toString());
                  currentToken.setLength(0);
              }
  
              if (i + 4 <= expressionWithoutSpaces.length()) {
                  String trigFunction = expressionWithoutSpaces.substring(i, i + 4);
                  if (trigFunction.equals("sin(") || trigFunction.equals("cos(") || trigFunction.equals("tan(")) {
                      tokens.add(trigFunction); // Add the full function name
                      i += 3; // Skip the next three characters (already processed)
                      continue;
                  }
              }
              if (i + 6 <= expressionWithoutSpaces.length()) {
                  String trigFunction = expressionWithoutSpaces.substring(i, i + 6);
                   if (trigFunction.equals("sin⁻¹(") || trigFunction.equals("cos⁻¹(") || trigFunction.equals("tan⁻¹(")) {
                      tokens.add(trigFunction); // Add the full function name
                      i += 5; // Skip the next five characters (already processed)
                      continue;
                  }
              }
  
              tokens.add(Character.toString(c));
          }
      }
  
      // Add the last token if it exists
      if (currentToken.length() > 0) {
          tokens.add(currentToken.toString());
      }
  
      return tokens;
  }
  
  /**
   * Evaluates a mathematical expression represented as a list of tokens.
   * Converts the expression from infix notation to postfix notation, then evaluates the postfix expression.
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
   * Applies an operator to two operands. Supports addition, subtraction, multiplication, division, square root, factorial, and power operations.
   * Checks the operator and performs the corresponding operation.
   *
   * @param operand1 The first operand for the operation.
   * @param operand2 The second operand for the operation.
   * @param operator The operator for the operation.
   * @return The result of the operation.
   * @throws IllegalArgumentException If the operator is not recognized or if the second operand for the square root operation is negative.
   */
  public static BigDecimal applyOperator(final BigDecimal operand1, final BigDecimal operand2, final String operator) {
    final String mode = "Deg";
      switch (operator) {
          case "+":
              return operand1.add(operand2);
          case "-":
              return operand1.subtract(operand2);
          case "*":
              return operand1.multiply(operand2);
          case "/":
              if (operand2.compareTo(BigDecimal.ZERO) == 0) {
                  throw new ArithmeticException("Kein Teilen durch 0");
              } else {
                  return operand1.divide(operand2, MC);
              }
          case ROOT:
              if (operand2.compareTo(BigDecimal.ZERO) < 0) {
                  throw new IllegalArgumentException("Nur reelle Zahlen");
              } else {
                  return BigDecimal.valueOf(Math.sqrt(operand2.doubleValue()));
              }
          case "!":
              return factorial(operand1);
          case "^":
              return pow(operand1, operand2);
          case "sin(":
              if (mode != null && mode.equals("Rad")) {
                  return BigDecimal.valueOf(Math.sin(operand2.doubleValue())).setScale(10, RoundingMode.DOWN);
              } else { // if mode equals 'Deg'
                  return BigDecimal.valueOf(Math.sin(Math.toRadians(operand2.doubleValue()))).setScale(10, RoundingMode.DOWN);
              }
          case "sin⁻¹(":
              if (mode != null && mode.equals("Rad")) {
                  return BigDecimal.valueOf(Math.asin(operand2.doubleValue())).setScale(10, RoundingMode.DOWN);
              } else { // if mode equals 'Deg'
                  return BigDecimal.valueOf(Math.toDegrees(Math.asin(operand2.doubleValue()))).setScale(10, RoundingMode.DOWN);
              }
          case "cos(":
              if (mode != null && mode.equals("Rad")) {
                  return BigDecimal.valueOf(Math.cos(operand2.doubleValue())).setScale(10, RoundingMode.DOWN);
              } else { // if mode equals 'Deg'
                  return BigDecimal.valueOf(Math.cos(Math.toRadians(operand2.doubleValue()))).setScale(10, RoundingMode.DOWN);
              }
          case "cos⁻¹(":
              if (mode != null && mode.equals("Rad")) {
                  return BigDecimal.valueOf(Math.acos(operand2.doubleValue())).setScale(10, RoundingMode.DOWN);
              } else { // if mode equals 'Deg'
                  return BigDecimal.valueOf(Math.toDegrees(Math.acos(operand2.doubleValue()))).setScale(10, RoundingMode.DOWN);
              }
          case "tan(":
              if (mode != null && mode.equals("Rad")) {
                  return BigDecimal.valueOf(Math.tan(operand2.doubleValue())).setScale(10, RoundingMode.DOWN);
              } else { // if mode equals 'Deg'
                  return BigDecimal.valueOf(Math.tan(Math.toRadians(operand2.doubleValue()))).setScale(10, RoundingMode.DOWN);
              }
          case "tan⁻¹(":
              if (mode != null && mode.equals("Rad")) {
                  return BigDecimal.valueOf(Math.atan(operand2.doubleValue())).setScale(10, RoundingMode.DOWN);
              } else { // if mode equals 'Deg'
                  return BigDecimal.valueOf(Math.toDegrees(Math.atan(operand2.doubleValue()))).setScale(10, RoundingMode.DOWN);
              }
          default:
              throw new IllegalArgumentException("Unbekannter Operator: '" + operator + "'");
      }
  }
  
  /**
   * Calculates the factorial of a number.
   * <p>
   * The factorial of a number is the product of all positive integers less than or equal to the number.
   * For example, the factorial of 5 (denoted as 5!) is 1*2*3*4*5 = 120.
   * <p>
   * The method takes a BigDecimal number as input. It first checks if the number is negative. If it is,
   * the number is made positive for the calculation. Then it checks if the number is a whole number because
   * factorial is only defined for whole numbers.
   * <p>
   * It initializes the result to 1. This will hold the calculated factorial.
   * It calculates the factorial by multiplying the number with the result and then decrementing the number,
   * until the number is greater than 1.
   * <p>
   * If the original number was negative, the result is negated. Otherwise, the result is returned as is.
   *
   * @param number The number for which the factorial is to be calculated.
   * @return The factorial of the number.
   * @throws IllegalArgumentException If the number is not a whole number or if it's greater than 170.
   */
  public static BigDecimal factorial(BigDecimal number) {
      // Check if the number is greater than 170
      if (number.compareTo(new BigDecimal("170")) > 0) {
          throw new IllegalArgumentException("Wert zu groß");
      }
  
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
  
      // Check if the base is zero and the exponent is negative
      if (baseDouble == 0 && exponentDouble < 0) {
          throw new ArithmeticException("Kein Teilen durch 0");
      }
  
      // Check if the base is negative and the exponent is an integer
      double resultDouble;
      if (baseDouble < 0 && exponentDouble == (int) exponentDouble) {
          baseDouble = -baseDouble;
          resultDouble = -Math.pow(baseDouble, exponentDouble);
      } else {
          resultDouble = Math.pow(baseDouble, exponentDouble);
      }
  
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
   * Evaluates a mathematical expression represented in postfix notation.
   *
   * @param postfixTokens The list of tokens in postfix notation.
   * @return The result of the expression.
   * @throws IllegalArgumentException If there is a syntax error in the expression or the stack size is not 1 at the end.
   */
  public static BigDecimal evaluatePostfix(final List<String> postfixTokens) {
      // Create a stack to store numbers
      final List<BigDecimal> stack = new ArrayList<>();
  
      // Iterate through each token in the postfix list
      for (final String token : postfixTokens) {
          // If the token is a number, add it to the stack
          if (isNumber(token)) {
              stack.add(new BigDecimal(token));
          } else if (isOperator(token)) {
              // If the token is an operator, apply the operator to the numbers in the stack
              applyOperatorToStack(token, stack);
          } else if (isFunction(token)) {
              // If the token is a function, evaluate the function and add the result to the stack
              evaluateFunction(token, stack);
          } else {
              // If the token is neither a number, operator, nor function, throw an exception
              throw new IllegalArgumentException("Syntax Fehler1");
          }
 
      }
  
      // If there is more than one number in the stack at the end, throw an exception
      if (stack.size() != 1) {
          throw new IllegalArgumentException("Syntax Fehler2");
      }
  
      // Return the result
      return stack.get(0);
  }
  
  /**
   * Applies an operator to numbers in the stack based on the given operator.
   *
   * @param operator The operator to be applied.
   * @param stack The stack containing numbers.
   */
  private static void applyOperatorToStack(String operator, List<BigDecimal> stack) {
      // If the operator is "!", apply the operator to only one number
      if (operator.equals("!")) {
          final BigDecimal operand1 = stack.remove(stack.size() - 1);
          final BigDecimal result = applyOperator(operand1, BigDecimal.ZERO, operator);
          stack.add(result);
      }
      // If the operator is not "!", apply the operator to two numbers
      else {
          final BigDecimal operand2 = stack.remove(stack.size() - 1);
          // If the operator is not ROOT, apply the operator to two numbers
          if (!operator.equals(ROOT)) {
              final BigDecimal operand1 = stack.remove(stack.size() - 1);
              final BigDecimal result = applyOperator(operand1, operand2, operator);
              stack.add(result);
          }
          // If the operator is ROOT, apply the operator to only one number
          else {
              final BigDecimal operand2SquareRoot;
              if (operand2.compareTo(BigDecimal.ZERO) < 0) {
                  // If the operand is negative, throw an exception or handle it as needed
                  throw new IllegalArgumentException("Nur reelle Zahlen");
              } else {
                  operand2SquareRoot = BigDecimal.valueOf(Math.sqrt(operand2.doubleValue()));
              }
              stack.add(operand2SquareRoot);
          }
      }
  }
  
  /**
   * Evaluates a mathematical function and adds the result to the stack.
   *
   * @param function The function to be evaluated.
   * @param stack The stack containing numbers.
   */
  private static void evaluateFunction(String function, List<BigDecimal> stack) {
      // Implement the evaluation of functions like sin, cos, tan.
      // You can use BigDecimalMath library or Java Math class for standard functions
      // Add the result of the function evaluation to the stack
      final String mode = "Deg";
      switch (function) {
          case "sin(": {
              BigDecimal operand = stack.remove(stack.size() - 1);
              BigDecimal result;
              if (mode != null && mode.equals("Rad")) {
                  result = BigDecimal.valueOf(Math.sin(operand.doubleValue())).setScale(10, RoundingMode.DOWN);
              } else { // if mode equals 'Deg'
                  result = BigDecimal.valueOf(Math.sin(Math.toRadians(operand.doubleValue()))).setScale(10, RoundingMode.DOWN);
              }
              stack.add(result);
              break;
          }
          case "sin⁻¹(": {
              BigDecimal operand = stack.remove(stack.size() - 1);
              BigDecimal result;
              if (operand.doubleValue() < -1 || operand.doubleValue() > 1) {
                  throw new ArithmeticException("Ungültiger Wert");
              }
              if (mode != null && mode.equals("Rad")) {
                  result = BigDecimal.valueOf(Math.asin(operand.doubleValue())).setScale(10, RoundingMode.DOWN);
              } else { // if mode equals 'Deg'
                  result = BigDecimal.valueOf(Math.toDegrees(Math.asin(operand.doubleValue()))).setScale(10, RoundingMode.DOWN);
              }
              stack.add(result);
              break;
          }
  
          case "cos(": {
              BigDecimal operand = stack.remove(stack.size() - 1);
              BigDecimal result;
              if (mode != null && mode.equals("Rad")) {
                  result = BigDecimal.valueOf(Math.cos(operand.doubleValue())).setScale(10, RoundingMode.DOWN);
              } else { // if mode equals 'Deg'
                  result = BigDecimal.valueOf(Math.cos(Math.toRadians(operand.doubleValue()))).setScale(10, RoundingMode.DOWN);
              }
              stack.add(result);
              break;
          }
          case "cos⁻¹(": {
              BigDecimal operand = stack.remove(stack.size() - 1);
              BigDecimal result;
              if (operand.doubleValue() < -1 || operand.doubleValue() > 1) {
                  throw new ArithmeticException("Ungültiger Wert");
              }
              if (mode != null && mode.equals("Rad")) {
                  result = BigDecimal.valueOf(Math.acos(operand.doubleValue())).setScale(10, RoundingMode.DOWN);
              } else { // if mode equals 'Deg'
                  result = BigDecimal.valueOf(Math.toDegrees(Math.acos(operand.doubleValue()))).setScale(10, RoundingMode.DOWN);
              }
              stack.add(result);
              break;
          }
          case "tan(": {
              BigDecimal operand = stack.remove(stack.size() - 1);
              BigDecimal result;
              if (mode != null && mode.equals("Rad")) {
                  result = BigDecimal.valueOf(Math.tan(operand.doubleValue())).setScale(10, RoundingMode.DOWN);
              } else { // if mode equals 'Deg'
                  double degrees = operand.doubleValue();
                  if (isMultipleOf90(degrees)) {
                      // Check if the tangent of multiples of 90 degrees is being calculated
                      throw new ArithmeticException("Nicht definiert");
                  }
                  result = BigDecimal.valueOf(Math.tan(Math.toRadians(degrees))).setScale(10, RoundingMode.DOWN);
              }
              stack.add(result);
              break;
          }
          case "tan⁻¹(": {
              BigDecimal operand = stack.remove(stack.size() - 1);
              BigDecimal result;
              if (mode != null && mode.equals("Rad")) {
                  result = BigDecimal.valueOf(Math.atan(operand.doubleValue())).setScale(10, RoundingMode.DOWN);
              } else { // if mode equals 'Deg'
                  result = BigDecimal.valueOf(Math.toDegrees(Math.atan(operand.doubleValue()))).setScale(10, RoundingMode.DOWN);
              }
              stack.add(result);
              break;
          }
  
      }
  }
  
  /**
   * Checks if a given angle in degrees is a multiple of 90.
   *
   * @param degrees The angle in degrees to be checked.
   * @return true if the angle is a multiple of 90, false otherwise.
   */
  private static boolean isMultipleOf90(double degrees) {
      // Check if degrees is a multiple of 90
      return Math.abs(degrees % 90) == 0;
  }
  
  /**
   * Converts a mathematical expression from infix notation to postfix notation.
   *
   * @param infixTokens The list of tokens in infix notation.
   * @return The list of tokens in postfix notation.
   */
  public static List<String> infixToPostfix(final List<String> infixTokens) {
      final List<String> postfixTokens = new ArrayList<>();
      final Stack<String> stack = new Stack<>();
  
      for (final String token : infixTokens) {
  
          if (isNumber(token)) {
              postfixTokens.add(token);
          } else if (isFunction(token)) {
              stack.push(token);
          } else if (isOperator(token) && token.equals("-")) {
              while (!stack.isEmpty() && precedence(stack.peek()) >= precedence(token) && !isFunction(stack.peek())) {
                  postfixTokens.add(stack.pop());
              }
              stack.push(token);
          } else if (isOperator(token)) {
              while (!stack.isEmpty() && precedence(stack.peek()) >= precedence(token) && !isFunction(stack.peek())) {
                  postfixTokens.add(stack.pop());
              }
              stack.push(token);
          } else if (token.equals("(")) {
              stack.push(token);
          } else if (token.equals(")")) {
              while (!stack.isEmpty() && !stack.peek().equals("(")) {
                  postfixTokens.add(stack.pop());
              }
              if (!stack.isEmpty() && stack.peek().equals("(")) {
                  stack.pop(); // Remove the opening parenthesis
                  if (!stack.isEmpty() && isFunction(stack.peek())) {
                      postfixTokens.add(stack.pop());
                  }
              }
          }
      }
  
      while (!stack.isEmpty()) {
          postfixTokens.add(stack.pop());
      }
  
      return postfixTokens;
  }
  
  /**
   * Checks if the given token represents a recognized trigonometric function.
   *
   * @param token The token to be checked.
   * @return true if the token represents a trigonometric function, false otherwise.
   */
  public static boolean isFunction(final String token) {
      // Check if the token is one of the recognized trigonometric functions
      return token.equals("sin(") || token.equals("cos(") || token.equals("tan(") ||
              token.equals("sin⁻¹(") || token.equals("cos⁻¹(") || token.equals("tan⁻¹(");
  }
  
  /**
   * Checks if a token is a number.
   * It attempts to create a BigDecimal from the token. If successful, the token is considered a number; otherwise, it is not.
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
   * Checks if the given token represents a recognized non-functional operator.
   *
   * @param token The token to be checked.
   * @return true if the token represents a non-functional operator, false otherwise.
   */
  public static boolean isOperator(final String token) {
      // Check if the token is one of the recognized non-functional operators
      return token.equals("+") || token.equals("-") || token.equals("*") || token.equals("/") ||
              token.equals("^") || token.equals("√") || token.equals("!");
  }
  
  /**
   * Determines the precedence of an operator.
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
  
          // If the operator is sine, cosine, or tangent, return 6
          case "sin(":
          case "cos(":
          case "tan(":
          case "sin⁻¹(":
          case "cos⁻¹(":
          case "tan⁻¹(":
              return 6;
  
          // If the operator is not recognized, throw an exception
          default:
              throw new IllegalArgumentException("Unbekannter Operator: " + operator);
      }
  }
}
