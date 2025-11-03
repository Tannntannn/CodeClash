# JDoodle Setup Guide

## ✅ **Perfect for Your Classroom!**

### **Why JDoodle?**
- **40-50 students** - Well within free limits (200-1000/day)
- **Interactive input** - Scanner, user input, real stdin/stdout
- **Educational focus** - Better for learning programming
- **Cost-effective** - No paid plans needed

## 🚀 **Setup Steps**

### **1. Get JDoodle API Credentials**
1. Visit: https://www.jdoodle.com/compiler-api
2. Sign up for free account
3. Get your `CLIENT_ID` and `CLIENT_SECRET`

### **2. Update Configuration**
**File**: `app/src/main/java/com/example/codeclash/JDoodleApiHelper.java`

```java
// Replace with your actual credentials
public static final String CLIENT_ID = "your_client_id_here";
public static final String CLIENT_SECRET = "your_client_secret_here";

// Set to false for production
public static final boolean DEMO_MODE = false;
```

## 📚 **Interactive Examples**

### **Calculator Program**
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
        scanner.close();
    }
}
```

**Input**: `10 5`

### **Student Grade Calculator**
```java
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.print("Enter student name: ");
        String name = scanner.nextLine();
        
        System.out.print("Enter test score: ");
        double score = scanner.nextDouble();
        
        String grade = (score >= 90) ? "A" : (score >= 80) ? "B" : "C";
        System.out.println(name + " got a " + grade);
        
        scanner.close();
    }
}
```

## 🎓 **Classroom Benefits**

### **Educational Advantages**
- **Real programming experience** - Interactive programs
- **Immediate feedback** - See results with different inputs
- **Engaging content** - Students love interactive programs
- **Practical skills** - Learn input/output handling

### **Technical Benefits**
- **Reliable service** - Stable and fast
- **Good error messages** - Clear debugging feedback
- **Multiple languages** - Java, Python, C++, JavaScript
- **Free tier** - Covers classroom needs

## 🎉 **Ready to Use!**

Your students can now write programs that:
- Ask for user input
- Process real data
- Provide interactive experiences
- Work like real development environments

**Perfect for learning programming fundamentals!** 🚀
