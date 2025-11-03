# 🐛 Debug Leaderboard Data Loading Issue

## 🔍 **Systematic Debugging Steps**

### **Step 1: Check Debug Logs**
Run the app and check Logcat for these specific logs:

#### **Expected Log Flow:**
```
🏆 Leaderboards: Loading user's classes...
🏆 Leaderboards: User is a teacher with X classes
🏆 Leaderboards: Loading leaderboards from X classes
🏆 Leaderboards: Loading leaderboards for class: CLASS123
🏆 Leaderboards: Loading CLASS123 Lesson 1 quiz
🏆 LeaderboardManager: getTopScores() called - Class: CLASS123, Lesson: Lesson 1, Activity: quiz
🏆 LeaderboardManager: Querying path: Classes/CLASS123/Leaderboards/Lesson 1_quiz/Scores
🏆 LeaderboardManager: Query successful - Found X documents
🏆 LeaderboardManager: Processing document: student123 - Score: 85, Student: John Doe
🏆 LeaderboardManager: Returning X entries
🏆 Leaderboards: Found X entries for CLASS123 Lesson 1 quiz
```

### **Step 2: Check Database Structure**
Go to Firebase Console and verify:

#### **Expected Database Path:**
```
Classes/
└── {classCode}/
    └── Leaderboards/
        └── {lessonName}_{activityType}/
            └── Scores/
                └── {studentId}/
                    ├── studentId: "student123"
                    ├── studentName: "John Doe"
                    ├── score: 85
                    ├── attemptsUsed: 2
                    └── timestamp: 1703123456789
```

### **Step 3: Common Issues to Check**

#### **Issue 1: Lesson Name Mismatch**
**Problem**: Database has "Lesson1" but code queries "Lesson 1"
**Check**: 
- What lesson names are in your class document?
- What lesson names are being queried in logs?

#### **Issue 2: Activity Type Mismatch**
**Problem**: Database has "quiz" but code queries "code_builder"
**Check**:
- What activity types are in your database?
- What activity types are being queried?

#### **Issue 3: Class Code Mismatch**
**Problem**: Wrong class code being used
**Check**:
- What class codes exist in your database?
- What class code is being queried?

#### **Issue 4: Collection Name Case Sensitivity**
**Problem**: Database has "leaderboards" but code queries "Leaderboards"
**Check**:
- Are collection names case-sensitive in your database?

### **Step 4: Manual Database Check**
1. **Go to Firebase Console**
2. **Navigate to**: `Classes → {your_class_code} → Leaderboards`
3. **Check what collections exist**:
   - `Lesson 1_quiz`?
   - `Lesson1_quiz`?
   - `Lesson 1_code_builder`?
   - `Lesson1_code_builder`?

### **Step 5: Test with Known Data**
1. **Complete a lesson** and check if score is recorded
2. **Check the exact path** where the score is stored
3. **Compare with what the leaderboard is querying**

---

## 🚨 **Most Likely Issues**

### **Issue 1: Lesson Name Format**
- **Database**: `Lesson1_quiz`
- **Code queries**: `Lesson 1_quiz`
- **Fix**: Update lesson names to match

### **Issue 2: Missing Scores Collection**
- **Problem**: Leaderboard document exists but no Scores subcollection
- **Fix**: Complete a lesson to generate scores

### **Issue 3: Wrong Class Code**
- **Problem**: Querying wrong class
- **Fix**: Check class code in logs vs database

---

## 🎯 **Quick Test**

1. **Run app with debug logs**
2. **Go to leaderboards**
3. **Check Logcat** for the exact paths being queried
4. **Compare with Firebase Console** structure
5. **Look for mismatches** in names/format

**Tell me what you see in the debug logs!** 🔍


