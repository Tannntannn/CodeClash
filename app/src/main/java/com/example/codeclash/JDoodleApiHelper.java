package com.example.codeclash;

import java.util.HashMap;
import java.util.Map;

public class JDoodleApiHelper {
    
    // JDoodle API Configuration
    public static final String JDOODLE_API_URL = "https://api.jdoodle.com/v1/execute";
    
    // JDoodle API Credentials
    // Get your free API credentials from: https://www.jdoodle.com/compiler-api
    public static final String CLIENT_ID = "c55284e02d516e41c649ff1e55787168";
    public static final String CLIENT_SECRET = "cccfcf24ff82c94b3b914a0f2ab2d4d13d61d892b0bb02ca781813e49cdaa3fc";
    
    // Language IDs for JDoodle
    public static final Map<String, String> LANGUAGE_IDS = new HashMap<>();
    static {
        LANGUAGE_IDS.put("java", "java");      // Java (JDK 1.8)
        LANGUAGE_IDS.put("python", "python3"); // Python 3
        LANGUAGE_IDS.put("cpp", "cpp");        // C++ (GCC 5.3.0)
        LANGUAGE_IDS.put("javascript", "nodejs"); // Node.js
    }
    
    // Demo mode - set to false to use the real API
    public static final boolean DEMO_MODE = false;
    
    /**
     * Creates a submission request for JDoodle
     */
    public static String createSubmissionRequest(String code, String language) {
        return createSubmissionRequest(code, language, null);
    }

    /**
     * Overload with explicit stdin support
     */
    public static String createSubmissionRequest(String code, String language, String stdin) {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("clientId", CLIENT_ID);
        requestMap.put("clientSecret", CLIENT_SECRET);
        requestMap.put("script", code);
        requestMap.put("language", LANGUAGE_IDS.getOrDefault(language, "java"));
        requestMap.put("versionIndex", "0");
        
        if (stdin != null && !stdin.trim().isEmpty()) {
            requestMap.put("stdin", stdin);
        }
        
        return new org.json.JSONObject(requestMap).toString();
    }
    
    /**
     * Get API headers for JDoodle
     */
    public static Map<String, String> getApiHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");
        return headers;
    }
    
    /**
     * Get mock success response for demo mode
     */
    public static String getMockSuccessResponse(String code, String stdin) {
        String output = "Hello, World!";
        String error = "";
        
        // Check if it's a calculator program
        if (code.contains("Scanner") && code.contains("nextInt") || code.contains("nextDouble")) {
            if (stdin != null && !stdin.trim().isEmpty()) {
                output = "Interactive program executed successfully!\nInput: " + stdin + "\nOutput: Calculation completed.";
            } else {
                output = "Interactive program ready for input.\nPlease provide input values.";
            }
        } else if (code.contains("System.out.println")) {
            output = "Program executed successfully!\nOutput: Hello, World!";
        } else if (code.contains("public static void main")) {
            output = "Java program compiled and executed successfully!";
        }
        
        // Check for common errors
        if (code.contains("System.out.println") && !code.contains(";")) {
            error = "Compilation Error: Missing semicolon";
            output = "";
        } else if (code.contains("public class") && !code.contains("{")) {
            error = "Compilation Error: Missing opening brace";
            output = "";
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("output", output);
        response.put("statusCode", error.isEmpty() ? "200" : "400");
        response.put("memory", "12345");
        response.put("cpuTime", "0.123");
        response.put("error", error);
        
        return new org.json.JSONObject(response).toString();
    }
    
    /**
     * Get mock error response for demo mode
     */
    public static String getMockErrorResponse(String code, String errorType) {
        String error = "";
        String output = "";
        
        switch (errorType) {
            case "syntax":
                error = "Compilation Error: Syntax error in your code";
                break;
            case "runtime":
                error = "Runtime Error: Exception in thread \"main\" java.lang.ArithmeticException: / by zero";
                output = "Exception occurred during execution";
                break;
            case "timeout":
                error = "Time Limit Exceeded: Your program took too long to execute";
                break;
            default:
                error = "Compilation Error: Please check your code syntax";
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("output", output);
        response.put("statusCode", "400");
        response.put("memory", "0");
        response.put("cpuTime", "0");
        response.put("error", error);
        
        return new org.json.JSONObject(response).toString();
    }
    
    /**
     * Check if code contains interactive elements (Scanner, input, etc.)
     */
    public static boolean isInteractiveCode(String code) {
        String lowerCode = code.toLowerCase();
        return lowerCode.contains("scanner") || 
               lowerCode.contains("nextint") || 
               lowerCode.contains("nextdouble") || 
               lowerCode.contains("nextline") ||
               lowerCode.contains("input") ||
               lowerCode.contains("readline");
    }
    
    /**
     * Get interactive input prompt based on code content
     */
    public static String getInteractivePrompt(String code) {
        if (code.contains("nextInt")) {
            return "Enter integer values (separated by spaces or newlines):";
        } else if (code.contains("nextDouble")) {
            return "Enter decimal numbers (separated by spaces or newlines):";
        } else if (code.contains("nextLine")) {
            return "Enter text input:";
        } else {
            return "Enter input values:";
        }
    }
}
