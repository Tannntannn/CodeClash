# 🐛 Debug Empty Leaderboard

## 🔍 **Step-by-Step Debugging**

### **Step 1: Check Debug Logs**
1. **Open Android Studio**
2. **Run the app in debug mode**
3. **Go to Leaderboards**
4. **Check Logcat** for these logs:

#### **Expected Logs:**
```
🏆 Leaderboards: Class: null, Lesson: null
🏆 Leaderboards: Loading student's classes...
🏆 Leaderboards: Loading leaderboards from X classes
```

#### **If No Classes Found:**
```
🏆 Leaderboards: Loading student's classes...
(No further logs - means no classes found)
```

### **Step 2: Check Student's Joined Classes**
1. **Go to Firebase Console** → Firestore Database
2. **Navigate to**: `Users → {your_student_id} → MyJoinedClasses`
3. **Check if you have any classes listed**

### **Step 3: Check Class Students Collection**
1. **Go to Firebase Console** → Firestore Database
2. **Navigate to**: `Classes → {class_code} → Students`
3. **Check if your student ID is listed there**

### **Step 4: Check Scores in Firebase**
1. **Go to Firebase Console** → Firestore Database
2. **Navigate to**: `Classes → {class_code} → Leaderboards`
3. **Check if there are any leaderboard collections** (e.g., `Lesson 1_quiz`, `Lesson 1_code_builder`)

---

## 🚨 **Common Issues**

### **Issue 1: Student Not in Classes Collection**
**Problem**: Student joined class but not added to `Students` collection
**Solution**: Check if student is in `Classes/{classCode}/Students/{studentId}`

### **Issue 2: Wrong Collection Name**
**Problem**: Student added to `students` (lowercase) but code looks in `Students` (uppercase)
**Solution**: We fixed this, but existing students might still be in wrong collection

### **Issue 3: No Scores Recorded**
**Problem**: Lessons completed but scores not submitted to Firebase
**Solution**: Check if `submit_score_to_android()` is being called

### **Issue 4: Wrong Class Code**
**Problem**: Scores recorded in different class than student is in
**Solution**: Verify class codes match

---

## 🎯 **Quick Test Steps**

1. **Check Logcat** for debug messages
2. **Check Firebase Console** for student's joined classes
3. **Check Firebase Console** for scores in leaderboards
4. **Complete a lesson** and check if score appears

**Tell me what you see in the debug logs and Firebase Console!** 🔍


