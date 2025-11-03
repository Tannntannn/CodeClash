package com.example.codeclash;

import java.util.HashMap;
import java.util.Map;

public class    Judge0ApiHelper {
    
    // Judge0 API Configuration
    public static final String JUDGE0_API_URL = "https://judge0-ce.p.rapidapi.com";
    public static final String SUBMISSIONS_ENDPOINT = "/submissions";
    public static final String RESULTS_ENDPOINT = "/submissions/";
    
    // RapidAPI Configuration (required for Judge0)
    // Get your free API key from: https://rapidapi.com/judge0-official/api/judge0-ce/
    public static final String RAPIDAPI_KEY = "861842b357msh35f30dd9a48e32bp1fd0b4jsn68781a5c9ac0";
    public static final String RAPIDAPI_HOST = "judge0-ce.p.rapidapi.com";
    
    // Language IDs for Judge0
    public static final Map<String, Integer> LANGUAGE_IDS = new HashMap<>();
    static {
        LANGUAGE_IDS.put("java", 62);      // Java (OpenJDK 13.0.1)
        LANGUAGE_IDS.put("python", 71);    // Python (3.8.1)
        LANGUAGE_IDS.put("cpp", 54);       // C++ (GCC 9.2.0)
        LANGUAGE_IDS.put("javascript", 63); // JavaScript (Node.js 12.14.0)
    }
    
    // Demo mode - set to false to use the real API
    public static final boolean DEMO_MODE = false;
    
    /**
     * Creates a submission request for Judge0
     */
    public static String createSubmissionRequest(String code, String language) {
        return createSubmissionRequest(code, language, null);
    }

    /**
     * Overload with explicit stdin support
     */
    public static String createSubmissionRequest(String code, String language, String explicitStdin) {
        int languageId = LANGUAGE_IDS.getOrDefault(language.toLowerCase(), 62); // Default to Java
        
        // Prefer caller-provided stdin; otherwise try to infer
        String stdin = (explicitStdin != null) ? explicitStdin : extractInputFromCode(code);
        
        return "{\n" +
                "  \"source_code\": \"" + escapeJson(code) + "\",\n" +
                "  \"language_id\": " + languageId + ",\n" +
                "  \"stdin\": \"" + escapeJson(stdin) + "\",\n" +
                "  \"expected_output\": \"\",\n" +
                "  \"cpu_time_limit\": 5,\n" +
                "  \"memory_limit\": 512000\n" +
                "}";
    }
    
    /**
     * Extracts input from code that uses Scanner
     */
    private static String extractInputFromCode(String code) {
        StringBuilder input = new StringBuilder();
        
        // Count different types of inputs to provide realistic sequences
        int intCount = countOccurrences(code, "nextInt()");
        int doubleCount = countOccurrences(code, "nextDouble()");
        int stringCount = countOccurrences(code, "nextLine()");
        int wordCount = countOccurrences(code, "next()");
        int charCount = countOccurrences(code, "charAt(0)");
        
        // Generate realistic input sequences
        for (int i = 0; i < intCount; i++) {
            input.append((10 + i * 5)).append("\n"); // 10, 15, 20, etc.
        }
        
        for (int i = 0; i < doubleCount; i++) {
            input.append(String.format("%.2f", 1.5 + i * 0.5)).append("\n"); // 1.50, 2.00, 2.50, etc.
        }
        
        for (int i = 0; i < stringCount; i++) {
            input.append("User Input ").append(i + 1).append("\n");
        }
        
        for (int i = 0; i < wordCount; i++) {
            input.append("word").append(i + 1).append("\n");
        }
        
        for (int i = 0; i < charCount; i++) {
            input.append("y\n"); // Default to 'y' for yes/no questions
        }
        
        // Special handling for calculator-like programs
        if (code.contains("calculator") || code.contains("Calculator")) {
            input = new StringBuilder();
            input.append("10.5\n"); // First number
            input.append("+\n");    // Operator
            input.append("5.2\n");  // Second number
            input.append("y\n");    // Continue (yes)
            input.append("20\n");   // Another calculation
            input.append("*\n");    // Operator
            input.append("3\n");    // Second number
            input.append("n\n");    // Stop (no)
        }
        
        // If no specific input found, provide some default input
        if (input.length() == 0 && code.contains("Scanner")) {
            input.append("10\n5\nHello\ny\n"); // Multiple inputs with continue
        }
        
        return input.toString();
    }
    
    /**
     * Generates realistic calculator output for interactive programs
     */
    private static String generateCalculatorOutput(String input) {
        StringBuilder output = new StringBuilder();
        String[] inputs = input.split("\n");
        
        output.append("=== Simple Java Calculator ===\n\n");
        
        // Process the first calculation
        if (inputs.length >= 4) {
            double num1 = Double.parseDouble(inputs[0]);
            String operator = inputs[1];
            double num2 = Double.parseDouble(inputs[2]);
            String choice = inputs[3];
            
            output.append("Enter first number: ").append(num1).append("\n");
            output.append("Enter an operator (+, -, *, /): ").append(operator).append("\n");
            output.append("Enter second number: ").append(num2).append("\n");
            
            double result = 0;
            switch (operator) {
                case "+":
                    result = num1 + num2;
                    break;
                case "-":
                    result = num1 - num2;
                    break;
                case "*":
                    result = num1 * num2;
                    break;
                case "/":
                    if (num2 != 0) {
                        result = num1 / num2;
                    } else {
                        output.append("Error: Division by zero is not allowed.\n");
                        return output.toString();
                    }
                    break;
                default:
                    output.append("Error: Invalid operator.\n");
                    return output.toString();
            }
            
            output.append("Result: ").append(result).append("\n\n");
            output.append("Do you want to calculate again? (y/n): ").append(choice).append("\n\n");
            
            // If user wants to continue, show second calculation
            if (choice.equalsIgnoreCase("y") && inputs.length >= 8) {
                double num3 = Double.parseDouble(inputs[4]);
                String operator2 = inputs[5];
                double num4 = Double.parseDouble(inputs[6]);
                String choice2 = inputs[7];
                
                output.append("Enter first number: ").append(num3).append("\n");
                output.append("Enter an operator (+, -, *, /): ").append(operator2).append("\n");
                output.append("Enter second number: ").append(num4).append("\n");
                
                double result2 = 0;
                switch (operator2) {
                    case "+":
                        result2 = num3 + num4;
                        break;
                    case "-":
                        result2 = num3 - num4;
                        break;
                    case "*":
                        result2 = num3 * num4;
                        break;
                    case "/":
                        if (num4 != 0) {
                            result2 = num3 / num4;
                        } else {
                            output.append("Error: Division by zero is not allowed.\n");
                            return output.toString();
                        }
                        break;
                    default:
                        output.append("Error: Invalid operator.\n");
                        return output.toString();
                }
                
                output.append("Result: ").append(result2).append("\n\n");
                output.append("Do you want to calculate again? (y/n): ").append(choice2).append("\n\n");
                
                if (choice2.equalsIgnoreCase("n")) {
                    output.append("Calculator closed. Goodbye!\n");
                }
            } else if (choice.equalsIgnoreCase("n")) {
                output.append("Calculator closed. Goodbye!\n");
            }
        }
        
        return output.toString();
    }
    
    /**
     * Counts occurrences of a substring in a string
     */
    private static int countOccurrences(String text, String substring) {
        int count = 0;
        int lastIndex = 0;
        while (lastIndex != -1) {
            lastIndex = text.indexOf(substring, lastIndex);
            if (lastIndex != -1) {
                count++;
                lastIndex += substring.length();
            }
        }
        return count;
    }
    
    /**
     * Escapes JSON string for API request
     */
    private static String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
    
    /**
     * Returns a mock successful response for demo purposes
     * Dynamically generates output based on the code content
     */
    public static String getMockSuccessResponse(String code) {
        return getMockSuccessResponse(code, null);
    }

    public static String getMockSuccessResponse(String code, String explicitStdin) {
        String output = extractExpectedOutput(code);
        String input = explicitStdin != null ? explicitStdin : extractInputFromCode(code);
        
        // If code uses Scanner, show input/output format
        if (code.contains("Scanner")) {
            // For calculator programs, show a more realistic interaction
            if (code.contains("calculator") || code.contains("Calculator")) {
                output = generateCalculatorOutput(input);
            } else {
                output = "Input:\n" + input + "\nOutput:\n" + output;
            }
        }
        
        return "{\n" +
                "  \"stdout\": \"" + escapeJson(output) + "\",\n" +
                "  \"time\": \"" + String.format("%.3f", Math.random() * 0.5 + 0.1) + "\",\n" +
                "  \"memory\": " + (12000 + (int)(Math.random() * 5000)) + ",\n" +
                "  \"stderr\": \"\",\n" +
                "  \"token\": \"demo_token_" + System.currentTimeMillis() + "\",\n" +
                "  \"compile_output\": \"\",\n" +
                "  \"message\": \"\",\n" +
                "  \"status\": {\n" +
                "    \"id\": 3,\n" +
                "    \"description\": \"Accepted\"\n" +
                "  }\n" +
                "}";
    }
    
    /**
     * Returns a mock error response for demo purposes
     */
    public static String getMockErrorResponse(String code) {
        String errorMessage = generateRealisticError(code);
        return "{\n" +
                "  \"stdout\": \"\",\n" +
                "  \"time\": \"0.000\",\n" +
                "  \"memory\": 0,\n" +
                "  \"stderr\": \"" + escapeJson(errorMessage) + "\",\n" +
                "  \"token\": \"demo_token_" + System.currentTimeMillis() + "\",\n" +
                "  \"compile_output\": \"\",\n" +
                "  \"message\": \"\",\n" +
                "  \"status\": {\n" +
                "    \"id\": 4,\n" +
                "    \"description\": \"Wrong Answer\"\n" +
                "  }\n" +
                "}";
    }
    
    /**
     * Extracts expected output from System.out.println statements in the code
     * Enhanced to handle arithmetic operations, loops, conditionals, Scanner input, and more
     */
    private static String extractExpectedOutput(String code) {
        StringBuilder output = new StringBuilder();
        String[] lines = code.split("\n");
        
        // Track variables for arithmetic operations
        java.util.Map<String, Object> variables = new java.util.HashMap<>();
        
        // Track Scanner input variables
        java.util.Map<String, Object> inputVariables = new java.util.HashMap<>();
        
        for (String line : lines) {
            line = line.trim();
            
            // Handle Scanner input assignments
            if (line.contains("Scanner") && line.contains("nextInt()")) {
                processScannerInput(line, inputVariables, "int");
            } else if (line.contains("Scanner") && line.contains("nextDouble()")) {
                processScannerInput(line, inputVariables, "double");
            } else if (line.contains("Scanner") && line.contains("nextLine()")) {
                processScannerInput(line, inputVariables, "String");
            } else if (line.contains("Scanner") && line.contains("next()")) {
                processScannerInput(line, inputVariables, "String");
            }
            // Look for System.out.println statements
            else if (line.contains("System.out.println(")) {
                String content = extractPrintContent(line, variables, inputVariables);
                if (!content.isEmpty()) {
                    output.append(content).append("\n");
                }
            }
            // Look for System.out.print statements
            else if (line.contains("System.out.print(")) {
                String content = extractPrintContent(line, variables, inputVariables);
                if (!content.isEmpty()) {
                    output.append(content);
                }
            }
            // Handle variable assignments
            else if (line.contains("=") && !line.contains("==")) {
                processVariableAssignment(line, variables);
            }
            // Handle arithmetic operations
            else if (isArithmeticOperation(line)) {
                processArithmeticOperation(line, variables);
            }
        }
        
        // If no print statements found, return default success message
        if (output.length() == 0) {
            return "Program executed successfully.";
        }
        
        return output.toString().trim();
    }
    
    /**
     * Processes Scanner input assignments
     */
    private static void processScannerInput(String line, java.util.Map<String, Object> inputVariables, String type) {
        try {
            // Extract variable name from patterns like: int num = scanner.nextInt();
            String[] parts = line.split("=");
            if (parts.length == 2) {
                String varDeclaration = parts[0].trim();
                String varName = varDeclaration.substring(varDeclaration.lastIndexOf(" ") + 1);
                
                // Assign default values based on type
                switch (type) {
                    case "int":
                        inputVariables.put(varName, 42);
                        break;
                    case "double":
                        inputVariables.put(varName, 3.14);
                        break;
                    case "String":
                        inputVariables.put(varName, "Hello World");
                        break;
                }
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
    }
    
    /**
     * Extracts the content from a print statement
     * Enhanced to handle variables, expressions, arithmetic, and Scanner input
     */
    private static String extractPrintContent(String line, java.util.Map<String, Object> variables, java.util.Map<String, Object> inputVariables) {
        try {
            // Find the content between quotes
            int start = line.indexOf("\"");
            int end = line.lastIndexOf("\"");
            
            if (start != -1 && end != -1 && start < end) {
                String content = line.substring(start + 1, end);
                
                // Handle string concatenation with variables
                if (content.contains("\" + ")) {
                    content = processStringConcatenation(content, variables);
                }
                
                return content;
            }
            
            // If no quotes found, might be a variable or expression
            if (line.contains("System.out.print")) {
                return processExpression(line, variables, inputVariables);
            }
            
        } catch (Exception e) {
            // If parsing fails, return generic output
            return "[output]";
        }
        
        return "";
    }
    
    /**
     * Processes variable assignments (e.g., int x = 5;)
     */
    private static void processVariableAssignment(String line, java.util.Map<String, Object> variables) {
        try {
            // Remove semicolon and trim
            line = line.replace(";", "").trim();
            
            // Find the equals sign
            int equalsIndex = line.indexOf("=");
            if (equalsIndex == -1) return;
            
            // Extract variable name (before equals)
            String beforeEquals = line.substring(0, equalsIndex).trim();
            String varName = beforeEquals.substring(beforeEquals.lastIndexOf(" ") + 1);
            
            // Extract value (after equals)
            String valueStr = line.substring(equalsIndex + 1).trim();
            
            // Parse the value
            Object value = parseValue(valueStr);
            if (value != null) {
                variables.put(varName, value);
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
    }
    
    /**
     * Checks if a line contains arithmetic operations
     */
    private static boolean isArithmeticOperation(String line) {
        return line.contains("+") || line.contains("-") || line.contains("*") || 
               line.contains("/") || line.contains("%") || line.contains("++") || 
               line.contains("--");
    }
    
    /**
     * Processes arithmetic operations
     */
    private static void processArithmeticOperation(String line, java.util.Map<String, Object> variables) {
        try {
            // Handle increment/decrement
            if (line.contains("++") || line.contains("--")) {
                processIncrementDecrement(line, variables);
            }
            // Handle compound assignments
            else if (line.contains("+=") || line.contains("-=") || line.contains("*=") || line.contains("/=")) {
                processCompoundAssignment(line, variables);
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
    }
    
    /**
     * Processes increment/decrement operations
     */
    private static void processIncrementDecrement(String line, java.util.Map<String, Object> variables) {
        line = line.replace(";", "").trim();
        
        if (line.contains("++")) {
            String varName = line.replace("++", "").trim();
            Object currentValue = variables.get(varName);
            if (currentValue instanceof Integer) {
                variables.put(varName, (Integer) currentValue + 1);
            } else if (currentValue instanceof Double) {
                variables.put(varName, (Double) currentValue + 1.0);
            }
        } else if (line.contains("--")) {
            String varName = line.replace("--", "").trim();
            Object currentValue = variables.get(varName);
            if (currentValue instanceof Integer) {
                variables.put(varName, (Integer) currentValue - 1);
            } else if (currentValue instanceof Double) {
                variables.put(varName, (Double) currentValue - 1.0);
            }
        }
    }
    
    /**
     * Processes compound assignments
     */
    private static void processCompoundAssignment(String line, java.util.Map<String, Object> variables) {
        line = line.replace(";", "").trim();
        
        if (line.contains("+=")) {
            String[] parts = line.split("\\+=");
            String varName = parts[0].trim();
            String valueStr = parts[1].trim();
            
            Object currentValue = variables.get(varName);
            Object newValue = parseValue(valueStr);
            
            if (currentValue instanceof Integer && newValue instanceof Integer) {
                variables.put(varName, (Integer) currentValue + (Integer) newValue);
            } else if (currentValue instanceof Double || newValue instanceof Double) {
                double current = currentValue instanceof Double ? (Double) currentValue : (Integer) currentValue;
                double newVal = newValue instanceof Double ? (Double) newValue : (Integer) newValue;
                variables.put(varName, current + newVal);
            }
        } else if (line.contains("-=")) {
            String[] parts = line.split("-=");
            String varName = parts[0].trim();
            String valueStr = parts[1].trim();
            
            Object currentValue = variables.get(varName);
            Object newValue = parseValue(valueStr);
            
            if (currentValue instanceof Integer && newValue instanceof Integer) {
                variables.put(varName, (Integer) currentValue - (Integer) newValue);
            } else if (currentValue instanceof Double || newValue instanceof Double) {
                double current = currentValue instanceof Double ? (Double) currentValue : (Integer) currentValue;
                double newVal = newValue instanceof Double ? (Double) newValue : (Integer) newValue;
                variables.put(varName, current - newVal);
            }
        }
        // Add similar logic for *=, /= if needed
    }
    
    /**
     * Processes string concatenation
     */
    private static String processStringConcatenation(String content, java.util.Map<String, Object> variables) {
        // Handle patterns like "Hello " + name + "!"
        String[] parts = content.split("\\+");
        StringBuilder result = new StringBuilder();
        
        for (String part : parts) {
            part = part.trim();
            
            if (part.startsWith("\"") && part.endsWith("\"")) {
                // It's a string literal
                result.append(part.substring(1, part.length() - 1));
            } else {
                // It's a variable
                Object value = variables.get(part);
                if (value != null) {
                    result.append(value.toString());
                } else {
                    result.append("[undefined]");
                }
            }
        }
        
        return result.toString();
    }
    
    /**
     * Processes expressions in print statements
     */
    private static String processExpression(String line, java.util.Map<String, Object> variables, java.util.Map<String, Object> inputVariables) {
        // Extract the expression between parentheses
        int start = line.indexOf("(");
        int end = line.lastIndexOf(")");
        
        if (start != -1 && end != -1) {
            String expression = line.substring(start + 1, end).trim();
            
            // Handle simple variable printing (check both regular and input variables)
            if (variables.containsKey(expression)) {
                return variables.get(expression).toString();
            } else if (inputVariables.containsKey(expression)) {
                return inputVariables.get(expression).toString();
            }
            
            // Handle arithmetic expressions
            if (expression.contains("+") || expression.contains("-") || 
                expression.contains("*") || expression.contains("/")) {
                return evaluateArithmeticExpression(expression, variables, inputVariables);
            }
            
            // Handle method calls
            if (expression.contains("(")) {
                return processMethodCall(expression, variables, inputVariables);
            }
        }
        
        return "[output]";
    }
    
    /**
     * Evaluates arithmetic expressions
     */
    private static String evaluateArithmeticExpression(String expression, java.util.Map<String, Object> variables, java.util.Map<String, Object> inputVariables) {
        try {
            // Replace variables with their values (check both maps)
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                expression = expression.replace(entry.getKey(), entry.getValue().toString());
            }
            for (Map.Entry<String, Object> entry : inputVariables.entrySet()) {
                expression = expression.replace(entry.getKey(), entry.getValue().toString());
            }
            
            // Handle different arithmetic operations
            if (expression.contains("+")) {
                String[] parts = expression.split("\\+");
                double sum = 0;
                for (String part : parts) {
                    sum += Double.parseDouble(part.trim());
                }
                return String.valueOf(sum);
            } else if (expression.contains("-")) {
                String[] parts = expression.split("-");
                if (parts.length == 2) {
                    double result = Double.parseDouble(parts[0].trim()) - Double.parseDouble(parts[1].trim());
                    return String.valueOf(result);
                }
            } else if (expression.contains("*")) {
                String[] parts = expression.split("\\*");
                double product = 1;
                for (String part : parts) {
                    product *= Double.parseDouble(part.trim());
                }
                return String.valueOf(product);
            } else if (expression.contains("/")) {
                String[] parts = expression.split("/");
                if (parts.length == 2) {
                    double result = Double.parseDouble(parts[0].trim()) / Double.parseDouble(parts[1].trim());
                    return String.valueOf(result);
                }
            } else if (expression.contains("%")) {
                String[] parts = expression.split("%");
                if (parts.length == 2) {
                    double result = Double.parseDouble(parts[0].trim()) % Double.parseDouble(parts[1].trim());
                    return String.valueOf(result);
                }
            }
            
            return expression;
        } catch (Exception e) {
            return "[calculation error]";
        }
    }
    
    /**
     * Processes method calls
     */
    private static String processMethodCall(String expression, java.util.Map<String, Object> variables, java.util.Map<String, Object> inputVariables) {
        if (expression.contains("length()")) {
            String varName = expression.replace(".length()", "");
            Object value = variables.get(varName);
            if (value == null) {
                value = inputVariables.get(varName);
            }
            if (value instanceof String) {
                return String.valueOf(((String) value).length());
            }
        }
        
        return "[method result]";
    }
    
    /**
     * Parses a value from a string
     */
    private static Object parseValue(String valueStr) {
        valueStr = valueStr.trim();
        
        // Remove quotes for strings
        if (valueStr.startsWith("\"") && valueStr.endsWith("\"")) {
            return valueStr.substring(1, valueStr.length() - 1);
        }
        
        // Try to parse as integer
        try {
            return Integer.parseInt(valueStr);
        } catch (NumberFormatException e) {
            // Try to parse as double
            try {
                return Double.parseDouble(valueStr);
            } catch (NumberFormatException e2) {
                // Return as string
                return valueStr;
            }
        }
    }
    
    /**
     * Generates realistic compiler error messages based on the code
     */
    private static String generateRealisticError(String code) {
        if (code == null || code.trim().isEmpty()) {
            return "Error: No source code provided.";
        }
        
        String cleanCode = code.trim();
        
        // Check for obvious structural issues
        if (!cleanCode.contains("class")) {
            return "Main.java:1: error: class, interface, or enum expected\n^";
        }
        
        if (!cleanCode.contains("main")) {
            return "Error: Main method not found in class Main, please define the main method as:\n   public static void main(String[] args)";
        }
        
        // Check for common syntax errors
        if (cleanCode.contains("System.out.println") && !cleanCode.contains(";")) {
            return "Main.java: error: ';' expected\n        System.out.println(\"Hello World\")\n                                    ^";
        }
        
        if (cleanCode.contains("int ") && cleanCode.contains("=") && !cleanCode.contains(";")) {
            return "Main.java: error: ';' expected\n        int x = 5\n               ^";
        }
        
        if (cleanCode.contains("for(") && !cleanCode.contains(")")) {
            return "Main.java: error: ')' expected\n        for(int i = 0; i < 10\n                           ^";
        }
        
        if (cleanCode.contains("if(") && !cleanCode.contains(")")) {
            return "Main.java: error: ')' expected\n        if(x > 5\n           ^";
        }
        
        if (cleanCode.contains("while(") && !cleanCode.contains(")")) {
            return "Main.java: error: ')' expected\n        while(i < 10\n              ^";
        }
        
        // Check for undefined variables (only if x is actually used in println)
        if (cleanCode.contains("System.out.println(x)") && 
            !cleanCode.contains("int x") && 
            !cleanCode.contains("double x") && 
            !cleanCode.contains("String x") &&
            !cleanCode.contains("char x") &&
            !cleanCode.contains("boolean x")) {
            return "Main.java: error: cannot find symbol\n  symbol:   variable x\n  location: class Main\n        System.out.println(x);\n                           ^";
        }
        
        // Check for missing braces
        if (cleanCode.contains("if(") && !cleanCode.contains("{")) {
            return "Main.java: error: '{' expected\n        if(x > 5)\n            System.out.println(\"Greater\");\n        ^";
        }
        
        // For any other case, return a generic compilation error
        return "Main.java: compilation failed due to syntax errors.";
    }
    
    /**
     * Checks if the code should show an error (like a real compiler)
     */
    public static boolean shouldShowError(String code) {
        if (code == null || code.trim().isEmpty()) {
            return true; // Empty code
        }
        
        String cleanCode = code.trim();
        
        // Check for basic structural issues
        if (!cleanCode.contains("class") || !cleanCode.contains("main")) {
            return true; // Missing basic structure
        }
        
        // Check for common syntax errors
        if (cleanCode.contains("System.out.println") && !cleanCode.contains(";")) {
            return true; // Missing semicolon
        }
        
        if (cleanCode.contains("int ") && cleanCode.contains("=") && !cleanCode.contains(";")) {
            return true; // Missing semicolon in variable declaration
        }
        
        if (cleanCode.contains("for(") && !cleanCode.contains(")")) {
            return true; // Incomplete for loop
        }
        
        if (cleanCode.contains("if(") && !cleanCode.contains(")")) {
            return true; // Incomplete if statement
        }
        
        if (cleanCode.contains("while(") && !cleanCode.contains(")")) {
            return true; // Incomplete while loop
        }
        
        // Only check for undefined variable x if it's actually used in println
        if (cleanCode.contains("System.out.println(x)") && 
            !cleanCode.contains("int x") && 
            !cleanCode.contains("double x") && 
            !cleanCode.contains("String x") &&
            !cleanCode.contains("char x") &&
            !cleanCode.contains("boolean x")) {
            return true; // Undefined variable x
        }
        
        if (cleanCode.contains("if(") && !cleanCode.contains("{")) {
            return true; // Missing braces
        }
        
        // For demo purposes, assume most code compiles successfully
        return false;
    }
    
    /**
     * Get the API headers for Judge0 requests
     */
    public static Map<String, String> getApiHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-RapidAPI-Key", RAPIDAPI_KEY);
        headers.put("X-RapidAPI-Host", RAPIDAPI_HOST);
        return headers;
    }
}
