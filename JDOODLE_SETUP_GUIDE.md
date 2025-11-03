# JDoodle Integration Setup Guide

## 🎯 **Why JDoodle for Your Classroom?**

### **Perfect for Your Use Case** ✅
- **40-50 students** - Well within free tier limits (200-1000 compilations/day)
- **Interactive input support** - Scanner, user input, real stdin/stdout
- **Educational focus** - Better for learning programming concepts
- **Cost-effective** - No paid plans needed for classroom size

### **Interactive Features** 🎮
- **Scanner input** - `nextInt()`, `nextDouble()`, `nextLine()`
- **User prompts** - Students can write interactive programs
- **Real programming experience** - Like actual development environment
- **Calculator programs** - Students can build functional applications

---

## 🚀 **Setup Instructions**

### **Step 1: Get JDoodle API Credentials**

1. **Visit JDoodle API**: https://www.jdoodle.com/compiler-api
2. **Sign up for free account** (no credit card required)
3. **Get your credentials**:
   - `CLIENT_ID`
   - `CLIENT_SECRET`
4. **Free tier includes**:
   - 200-1000 compilations per day
   - Multiple programming languages
   - Interactive input support

### **Step 2: Update Configuration**

**File**: `app/src/main/java/com/example/codeclash/JDoodleApiHelper.java`

```java
// Update these lines with your actual credentials
public static final String CLIENT_ID = "your_actual_client_id_here";
public static final String CLIENT_SECRET = "your_actual_client_secret_here";

// Set to false for production use
public static final boolean DEMO_MODE = false;
```

### **Step 3: Test the Integration**

1. **Build and run** your app
2. **Open Compiler Mode** in any lesson
3. **Try the interactive example** (already included)
4. **Test with custom input** in the input field

---

## 📚 **Interactive Programming Examples**

### **Example 1: Simple Calculator**
```java
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.print("Enter first number: ");
        int a = scanner.nextInt();
        
        System.out.print("Enter second number: ");
        int b = scanner.nextInt();
        
        System.out.println("Sum: " + (a + b));
        System.out.println("Difference: " + (a - b));
        System.out.println("Product: " + (a * b));
        System.out.println("Quotient: " + (a / b));
        
        scanner.close();
    }
}
```

**Input**: `10 5`

### **Example 2: Student Grade Calculator**
```java
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.print("Enter student name: ");
        String name = scanner.nextLine();
        
        System.out.print("Enter test score: ");
        double score = scanner.nextDouble();
        
        String grade;
        if (score >= 90) grade = "A";
        else if (score >= 80) grade = "B";
        else if (score >= 70) grade = "C";
        else if (score >= 60) grade = "D";
        else grade = "F";
        
        System.out.println(name + " got a " + grade);
        
        scanner.close();
    }
}
```

**Input**: `John Smith 85`

### **Example 3: Number Guessing Game**
```java
import java.util.Scanner;
import java.util.Random;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Random random = new Random();
        
        int secretNumber = random.nextInt(100) + 1;
        int attempts = 0;
        
        System.out.println("I'm thinking of a number between 1 and 100!");
        
        while (true) {
            System.out.print("Enter your guess: ");
            int guess = scanner.nextInt();
            attempts++;
            
            if (guess == secretNumber) {
                System.out.println("Correct! You got it in " + attempts + " attempts!");
                break;
            } else if (guess < secretNumber) {
                System.out.println("Too low! Try again.");
            } else {
                System.out.println("Too high! Try again.");
            }
        }
        
        scanner.close();
    }
}
```

---

## 🎓 **Classroom Integration**

### **Lesson Ideas with Interactive Programming**

#### **1. Input/Output Fundamentals**
- **Objective**: Learn Scanner and basic input/output
- **Activity**: Write programs that ask for user information
- **Example**: Name, age, favorite color input program

#### **2. Mathematical Operations**
- **Objective**: Practice arithmetic with user input
- **Activity**: Calculator programs, area/perimeter calculators
- **Example**: Rectangle area calculator

#### **3. Conditional Statements**
- **Objective**: Learn if/else with real data
- **Activity**: Grade calculators, age verification
- **Example**: Movie ticket price calculator

#### **4. Loops and Repetition**
- **Objective**: Practice while and for loops
- **Activity**: Number guessing games, multiplication tables
- **Example**: Countdown timer

### **Student Engagement Benefits**
- **Real-world applications** - Programs that actually do something useful
- **Immediate feedback** - See results instantly with different inputs
- **Creative freedom** - Students can experiment with different inputs
- **Problem-solving** - Debugging interactive programs

---

## 🔧 **Technical Details**

### **API Response Format**
```json
{
  "output": "Hello John, you are 25 years old!",
  "statusCode": "200",
  "memory": "12345",
  "cpuTime": "0.123",
  "error": ""
}
```

### **Error Handling**
- **Compilation errors** - Syntax issues, missing imports
- **Runtime errors** - Division by zero, null pointer exceptions
- **Input errors** - Invalid data types, missing input
- **Timeout errors** - Programs that run too long

### **Performance Metrics**
- **Memory usage** - Shows how much memory the program used
- **CPU time** - Shows execution time
- **Status codes** - 200 for success, 400 for errors

---

## 📊 **Usage Limits & Monitoring**

### **Free Tier Limits**
- **Daily compilations**: 200-1000 per day
- **Concurrent requests**: Limited but sufficient for classroom
- **Language support**: Java, Python, C++, JavaScript, and more

### **For 40-50 Students**
- **Average usage**: ~10-20 compilations per student per day
- **Total daily usage**: 400-1000 compilations
- **Safety margin**: Well within free tier limits

### **Monitoring Usage**
- **JDoodle dashboard** - Track daily compilation count
- **Error monitoring** - Check for failed requests
- **Performance tracking** - Monitor response times

---

## 🚨 **Troubleshooting**

### **Common Issues**

#### **1. "Invalid credentials" error**
- **Solution**: Double-check CLIENT_ID and CLIENT_SECRET
- **Check**: Ensure no extra spaces or characters

#### **2. "Daily limit exceeded" error**
- **Solution**: Wait until next day or upgrade plan
- **Prevention**: Monitor usage in JDoodle dashboard

#### **3. "Compilation timeout" error**
- **Solution**: Check for infinite loops in student code
- **Prevention**: Set reasonable time limits for assignments

#### **4. "Input not found" error**
- **Solution**: Ensure input field is not empty for Scanner programs
- **Check**: Verify input format matches program expectations

### **Demo Mode Testing**
- **Enable demo mode** for testing without API calls
- **Test interactive features** with mock responses
- **Verify UI behavior** before going live

---

## 🎉 **Benefits for Your Classroom**

### **Educational Advantages**
✅ **Real programming experience** - Students write actual interactive programs
✅ **Immediate feedback** - See results with different inputs instantly
✅ **Engaging content** - Interactive programs are more interesting
✅ **Practical skills** - Learn input/output handling early
✅ **Problem-solving** - Debug real programs with real inputs

### **Technical Advantages**
✅ **Reliable service** - JDoodle is stable and well-maintained
✅ **Fast execution** - Quick compilation and execution times
✅ **Good error messages** - Clear feedback for debugging
✅ **Multiple languages** - Support for Java, Python, C++, etc.
✅ **Cost-effective** - Free tier covers classroom needs

### **Integration Benefits**
✅ **Seamless integration** - Works with existing leaderboard system
✅ **Score tracking** - Can track compilation success/failure
✅ **Progress monitoring** - Teachers can see student progress
✅ **Attempt management** - Integrates with 3-attempt system

---

## 🚀 **Next Steps**

1. **Get JDoodle credentials** from their website
2. **Update the configuration** in JDoodleApiHelper.java
3. **Test with interactive examples** provided above
4. **Create lesson plans** using interactive programming
5. **Monitor usage** in JDoodle dashboard
6. **Enjoy enhanced student engagement**! 🎓

**Your students will love writing programs that actually interact with users!** 🎉
