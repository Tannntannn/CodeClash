package com.example.codeclash;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProblemManager {
    
    // Sample problems for different lessons
    private static final Map<String, LessonProblem> SAMPLE_PROBLEMS = new HashMap<>();
    
    static {
        initializeSampleProblems();
    }
    
    private static void initializeSampleProblems() {
        // Remove older generic problems to avoid confusion. Using explicit lesson tasks below.
        
        // Remove old sample problems not used by lessons 1-6
        
        
        
        
        
        // Problem 5: Arrays (Hard)
        List<LessonProblem.TestCase> arrayTests = Arrays.asList(
            new LessonProblem.TestCase("", "Sum of array: 15", "Array sum calculation")
        );
        
        SAMPLE_PROBLEMS.put("Arrays and Collections", new LessonProblem(
            "array_sum",
            "Array Sum Calculator",
            "Create an array with values {1, 2, 3, 4, 5}.\n" +
            "Calculate the sum of all elements and print: 'Sum of array: [result]'\n\n" +
            "Requirements:\n" +
            "• Create an integer array with the given values\n" +
            "• Use a loop to calculate the sum\n" +
            "• Print the result in the exact format",
            "Hard",
            "public class Main {\n" +
            "    public static void main(String[] args) {\n" +
            "        // Create your array here\n" +
            "        \n" +
            "        // Calculate sum using a loop\n" +
            "        \n" +
            "        // Print the result\n" +
            "        \n" +
            "    }\n" +
            "}",
            arrayTests,
            "Use: int[] numbers = {1, 2, 3, 4, 5}; and a for loop to sum them",
            30
        ));

        // ===================== NEW LESSON TASKS =====================
        // lesson 1: Welcome Message / Hello World
        List<LessonProblem.TestCase> l1Tests = Arrays.asList(
            new LessonProblem.TestCase("CodeClash\n", "Hello, CodeClash!", "Greets the provided name")
        );
        SAMPLE_PROBLEMS.put("lesson 1", new LessonProblem(
            "lesson1_hello",
            "Problem: Welcome Message / Hello World",
            "Students create a simple Java program that prints a personalized greeting.",
            "Easy",
            "import java.util.*;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        System.out.print(\"Enter your name: \");\n        String name = sc.nextLine();\n        // TODO: Print the greeting below\n        // System.out.println(\"Hello, \" + name + \"!\");\n    }\n}",
            l1Tests,
            "Use: String name = sc.nextLine(); then print using concatenation.",
            10
        ));

        // lesson 2: Digital ID Card
        List<LessonProblem.TestCase> l2Tests = Arrays.asList(
            new LessonProblem.TestCase("Alex\n21\nBSCS\n", "Name: Alex\nAge: 21\nCourse: BSCS", "Displays user details in three lines")
        );
        SAMPLE_PROBLEMS.put("lesson 2", new LessonProblem(
            "lesson2_id_card",
            "Problem: Digital ID Card",
            "Students store and display their name, age, and course using variables and strings.",
            "Easy",
            "import java.util.*;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        System.out.print(\"Enter name: \"); String name = sc.nextLine();\n        System.out.print(\"Enter age: \"); int age = sc.nextInt(); sc.nextLine();\n        System.out.print(\"Enter course: \"); String course = sc.nextLine();\n        // TODO: Print details exactly as shown in the example\n        // System.out.println(\"Name: \" + name);\n        // System.out.println(\"Age: \" + age);\n        // System.out.println(\"Course: \" + course);\n    }\n}",
            l2Tests,
            "Mind the newline after reading an int; call sc.nextLine() before reading course.",
            12
        ));

        // lesson 3: Bill Splitter / Simple Calculator (Bill Splitter version)
        List<LessonProblem.TestCase> l3Tests = Arrays.asList(
            new LessonProblem.TestCase("150\n3\n", "Per person: 50.00", "150 split among 3 -> 50.00 each")
        );
        SAMPLE_PROBLEMS.put("lesson 3", new LessonProblem(
            "lesson3_bill_splitter",
            "Problem: Bill Splitter / Simple Calculator",
            "Students build a program that computes total expenses and divides costs among friends.",
            "Easy",
            "import java.util.*;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        System.out.print(\"Total bill: \"); double total = sc.nextDouble();\n        System.out.print(\"Number of friends: \"); int friends = sc.nextInt();\n        // TODO: Compute share and print formatted to 2 decimals: Per person: <amount>\n        // double share = total / friends;\n        // System.out.printf(\"Per person: %.2f\", share);\n    }\n}",
            l3Tests,
            "Use System.out.printf with \"%.2f\" to format to two decimals.",
            15
        ));

        // lesson 4: Grade Classifier
        List<LessonProblem.TestCase> l4Tests = Arrays.asList(
            new LessonProblem.TestCase("85\n", "Grade: B", "85 corresponds to B"),
            new LessonProblem.TestCase("91\n", "Grade: A", "91 corresponds to A")
        );
        SAMPLE_PROBLEMS.put("lesson 4", new LessonProblem(
            "lesson4_grade_classifier",
            "Problem: Grade Classifier",
            "Students write a program that takes a numeric grade and outputs the letter equivalent (A–F).",
            "Medium",
            "import java.util.*;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        int g = sc.nextInt();\n        // TODO: if-else chain to determine letter grade\n        // Example: System.out.println(\"Grade: A\");\n    }\n}",
            l4Tests,
            "Use inclusive bounds and ensure single output line like 'Grade: B'.",
            18
        ));

        // lesson 5: Guess the Number Game
        List<LessonProblem.TestCase> l5aTests = Arrays.asList(
            new LessonProblem.TestCase("50\n50\n", "Correct!", "Guesses correct number immediately")
        );
        SAMPLE_PROBLEMS.put("lesson 5: guess the number game", new LessonProblem(
            "lesson5_guess_number",
            "Problem: Guess the Number Game",
            "Students implement a program where the user guesses a number until correct, using while/do-while loops.",
            "Medium",
            "import java.util.*;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        int target = 50; // You may randomize, but keep deterministic for tests\n        // TODO: Loop reading guesses until equal to target, printing feedback\n        // while (true) {\n        //   int guess = sc.nextInt();\n        //   if (guess == target) { System.out.println(\"Correct!\"); break; }\n        //   else if (guess < target) System.out.println(\"Too low\");\n        //   else System.out.println(\"Too high\");\n        // }\n    }\n}",
            l5aTests,
            "Use a while loop; print exactly 'Correct!' when matched.",
            22
        ));

        // lesson 5: Mini Data Analyzer
        List<LessonProblem.TestCase> l5bTests = Arrays.asList(
            new LessonProblem.TestCase("5\n10\n20\n5\n15\n0\n", "Sum: 50\nAverage: 12.50\nMax: 20\nMin: 0", "Aggregates multiple values" )
        );
        SAMPLE_PROBLEMS.put("lesson 5: mini data analyzer", new LessonProblem(
            "lesson5_data_analyzer",
            "Problem: Mini Data Analyzer",
            "Students create a loop-based program that accepts multiple inputs, then calculates sum, average, and highest/lowest values.",
            "Medium",
            "import java.util.*;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        long sum = 0; int count = 0;\n        int max = Integer.MIN_VALUE, min = Integer.MAX_VALUE;\n        // TODO: Read integers (you can stop on non-integer or sentinel 0) and update aggregates\n        // Print in this format:\n        // Sum: <sum>\n        // Average: <avg with 2 decimals>\n        // Max: <max>\n        // Min: <min>\n    }\n}",
            l5bTests,
            "Track count for average; use printf with %.2f for Average.",
            24
        ));

        // lesson 6: Student Grade Manager (arrays)
        List<LessonProblem.TestCase> l6Tests = Arrays.asList(
            new LessonProblem.TestCase("5\n72\n65\n90\n50\n80\n", "Average: 71.40\nHighest: 90\nLowest: 50\nPassed: 3\nFailed: 2", "Processes an array of grades")
        );
        SAMPLE_PROBLEMS.put("lesson 6", new LessonProblem(
            "lesson6_grade_manager",
            "Problem: Student Grade Manager",
            "Students use arrays to store multiple grades, then compute average, highest, lowest, and count of passed/failed students.",
            "Hard",
            "import java.util.*;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        int n = sc.nextInt();\n        int[] grades = new int[n];\n        for (int i = 0; i < n; i++) { grades[i] = sc.nextInt(); }\n        // TODO: compute average (double), highest, lowest, passed, failed and print:\n        // Average: <avg with 2 decimals>\n        // Highest: <max>\n        // Lowest: <min>\n        // Passed: <count>\n        // Failed: <count>\n    }\n}",
            l6Tests,
            "Use a single pass to compute aggregates; remember to cast for average.",
            30
        ));
    }
    
    /**
     * Get a problem for a specific lesson
     */
    public static LessonProblem getProblemForLesson(String lessonName) {
        return SAMPLE_PROBLEMS.get(lessonName);
    }
    
    /**
     * Check if a lesson has a problem
     */
    public static boolean hasProblem(String lessonName) {
        return SAMPLE_PROBLEMS.containsKey(lessonName);
    }
    
    /**
     * Get all available lesson names with problems
     */
    public static List<String> getAvailableLessons() {
        return new ArrayList<>(SAMPLE_PROBLEMS.keySet());
    }
    
    /**
     * Validate if the student's output matches expected output
     */
    public static boolean validateSolution(String actualOutput, String expectedOutput) {
        if (actualOutput == null || expectedOutput == null) {
            return false;
        }
        
        // Normalize whitespace and line endings
        String normalizedActual = actualOutput.trim().replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");
        String normalizedExpected = expectedOutput.trim().replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");
        
        return normalizedActual.equals(normalizedExpected);
    }
    
    /**
     * Get a default problem for lessons without specific problems
     */
    public static LessonProblem getDefaultProblem(String lessonName) {
        List<LessonProblem.TestCase> defaultTests = Arrays.asList(
            new LessonProblem.TestCase("", "Program completed successfully!", "Basic completion test")
        );
        
        return new LessonProblem(
            "default_" + lessonName.toLowerCase().replaceAll("\\s+", "_"),
            "Practice Exercise: " + lessonName,
            "Practice what you've learned in this lesson!\n\n" +
            "Write a Java program that demonstrates the concepts from: " + lessonName + "\n\n" +
            "Feel free to experiment with the code and try different approaches.\n" +
            "When you're done, make sure your program prints: 'Program completed successfully!'",
            "Practice",
            "public class Main {\n" +
            "    public static void main(String[] args) {\n" +
            "        // Practice " + lessonName + " concepts here\n" +
            "        \n" +
            "        System.out.println(\"Program completed successfully!\");\n" +
            "    }\n" +
            "}",
            defaultTests,
            "Experiment with the concepts you've learned and don't forget the success message!",
            5
        );
    }
}
