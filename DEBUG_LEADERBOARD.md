# 🐛 Debug Leaderboard Issue

## 🔍 **Step-by-Step Debugging**

### **Step 1: Check What You Completed**
When you completed Lesson 1, did you:
- ✅ **Quiz Mode** (multiple choice questions with timer)
- ✅ **Code Builder** (drag and drop blocks)

### **Step 2: Check Debug Logs**
1. **Open Android Studio**
2. **Run the app in debug mode**
3. **Complete Lesson 1 again** (either quiz or code builder)
4. **Check Logcat** for these logs:

#### **Expected Logs:**
```
🎯 Lesson1: Submitting score to Android - Score: 85
🎯 Lesson1: Calling AppPlugin.submitScore()
🎯 AppPlugin: submitScore() called - Score: 85, Attempts: 1
🎯 AppPlugin: Context - Class: CLASS123, Lesson: Lesson 1, Activity: quiz, Student: student123
🎯 AppPlugin: Submitting score to LessonManager - Score: 85, Attempts: 1
🏆 LeaderboardManager: recordScore() called
🏆 LeaderboardManager: Class: CLASS123, Lesson: Lesson 1, Activity: quiz
✅ Score recorded successfully
```

#### **If Missing Context:**
```
❌ AppPlugin: Missing required context, cannot submit score
```

### **Step 3: Check Firebase Console**
1. **Go to Firebase Console** → Firestore Database
2. **Navigate to**: `Classes → {your_class_code} → Leaderboards`
3. **Look for**:
   - `Lesson 1_quiz` (for quiz scores)
   - `Lesson 1_code_builder` (for drag & drop scores)

### **Step 4: Check Which Leaderboard You're Viewing**
In the app, make sure you're checking the correct leaderboard:
- **Quiz Leaderboard** → Shows quiz scores
- **Code Builder Leaderboard** → Shows drag & drop scores

---

## 🚨 **Common Issues**

### **Issue 1: Wrong Activity Type**
**Problem**: Completed quiz but checking code builder leaderboard (or vice versa)
**Solution**: Check the correct leaderboard tab

### **Issue 2: Missing Intent Extras**
**Problem**: Intent extras not set when launching lesson
**Solution**: Check debug logs for "Missing required context"

### **Issue 3: Firebase Connection**
**Problem**: Scores not reaching Firebase
**Solution**: Check internet connection and Firebase rules

### **Issue 4: Wrong Class Code**
**Problem**: Scores recorded in wrong class
**Solution**: Verify class code in debug logs

---

## 🎯 **Quick Test**

1. **Complete Lesson 1 Quiz** (not code builder)
2. **Check Quiz Leaderboard** (not code builder leaderboard)
3. **Look for debug logs** in Logcat
4. **Check Firebase Console** for `Lesson 1_quiz` collection

**Tell me what you see in the debug logs!** 🔍


