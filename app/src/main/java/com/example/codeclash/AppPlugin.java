package com.example.codeclash;

import org.godotengine.godot.Godot;
import org.godotengine.godot.plugin.GodotPlugin;
import org.godotengine.godot.plugin.SignalInfo;
    import org.godotengine.godot.plugin.UsedByGodot;

import java.util.HashSet;
import java.util.Set;

public class AppPlugin extends GodotPlugin {

    private static final SignalInfo OPEN_GAME_SIGNAL =
            new SignalInfo("open_game", String.class);

    public AppPlugin(Godot godot) {
        super(godot);
    }

    @Override
    public String getPluginName() {
        return "AppPlugin";
    }

    @Override
    public Set<SignalInfo> getPluginSignals() {
        Set<SignalInfo> signals = new HashSet<>();
        signals.add(OPEN_GAME_SIGNAL);
        return signals;
    }

        @UsedByGodot
    public void openGame(String sceneName) {
        emitSignal("open_game", sceneName);
    }

    // -------------------- Godot Launch Helpers --------------------
    // We emit a JSON payload over the existing "open_game" signal so the Godot
    // side can route to a lesson + mode (code_builder or quiz).

    private void emitLaunch(String mode, String lessonKey, String lessonTitle) {
        // Minimal JSON construction without adding dependencies
        String payload = "{\"mode\":\"" + mode + "\"," +
                "\"lessonKey\":\"" + lessonKey + "\"," +
                "\"lessonTitle\":\"" + lessonTitle + "\"}";
        emitSignal("open_game", payload);
    }

    // Generic entry points
        @UsedByGodot
    public void launchCodeBuilder(String lessonKey, String lessonTitle) {
        emitLaunch("code_builder", lessonKey, lessonTitle);
    }

        @UsedByGodot
    public void launchQuiz(String lessonKey, String lessonTitle) {
        emitLaunch("quiz", lessonKey, lessonTitle);
    }

    // Convenience methods per lesson (Code Builder)
        @UsedByGodot public void launchCodeBuilderLesson1() { emitLaunch("code_builder", "lesson 1", "Introduction to Java"); }
        @UsedByGodot public void launchCodeBuilderLesson2() { emitLaunch("code_builder", "lesson 2", "Variables and Data"); }
        @UsedByGodot public void launchCodeBuilderLesson3() { emitLaunch("code_builder", "lesson 3", "Operators and Expressions"); }
        @UsedByGodot public void launchCodeBuilderLesson4() { emitLaunch("code_builder", "lesson 4", "Conditional Statements"); }
        @UsedByGodot public void launchCodeBuilderLesson5() { emitLaunch("code_builder", "lesson 5", "Conditional Loops"); }
        @UsedByGodot public void launchCodeBuilderLesson6() { emitLaunch("code_builder", "lesson 6", "Arrays"); }

    // Convenience methods per lesson (Quiz)
        @UsedByGodot public void launchQuizLesson1() { emitLaunch("quiz", "lesson 1", "Introduction to Java"); }
        @UsedByGodot public void launchQuizLesson2() { emitLaunch("quiz", "lesson 2", "Variables and Data"); }
        @UsedByGodot public void launchQuizLesson3() { emitLaunch("quiz", "lesson 3", "Operators and Expressions"); }
        @UsedByGodot public void launchQuizLesson4() { emitLaunch("quiz", "lesson 4", "Conditional Statements"); }
        @UsedByGodot public void launchQuizLesson5() { emitLaunch("quiz", "lesson 5", "Conditional Loops"); }
        @UsedByGodot public void launchQuizLesson6() { emitLaunch("quiz", "lesson 6", "Arrays"); }

    // -------------------- Score Submission (Godot -> Android) --------------------
    // Godot should call: Engine.get_singleton("AppPlugin").submitScore(score, attemptsUsed)
        @UsedByGodot
        public void submitScore(int score, int attemptsUsed) {
        System.out.println("🎯 AppPlugin: submitScore() called - Score: " + score + ", Attempts: " + attemptsUsed);
        
        android.app.Activity activity = getActivity();
        if (activity == null) {
            System.out.println("❌ AppPlugin: Activity is null, cannot submit score");
            return;
        }

        android.content.Intent intent = activity.getIntent();
        if (intent == null) {
            System.out.println("❌ AppPlugin: Intent is null, cannot submit score");
            return;
        }

        String classCode = intent.getStringExtra("classCode");
        String lessonName = intent.getStringExtra("lessonName");
        String activityType = intent.getStringExtra("activityType");
        String studentId = intent.getStringExtra("studentId");

        System.out.println("🎯 AppPlugin: Context - Class: " + classCode + ", Lesson: " + lessonName + ", Activity: " + activityType + ", Student: " + studentId);

        // Guard: require minimum context
        if (classCode == null || lessonName == null || studentId == null) {
            System.out.println("❌ AppPlugin: Missing required context, cannot submit score");
            return;
        }

        // Default to quiz if not provided or invalid
        if (activityType == null ||
                !(LeaderboardManager.ACTIVITY_QUIZ.equals(activityType) ||
                  LeaderboardManager.ACTIVITY_CODE_BUILDER.equals(activityType))) {
            activityType = LeaderboardManager.ACTIVITY_QUIZ;
            System.out.println("🎯 AppPlugin: Defaulting to quiz activity type");
        }

        // Clamp attemptsUsed to >= 1
        int safeAttemptsUsed = attemptsUsed <= 0 ? 1 : attemptsUsed;

        System.out.println("🎯 AppPlugin: Submitting score to LessonManager - Score: " + score + ", Attempts: " + safeAttemptsUsed);
        
        // Convert Godot lesson name back to database format for storage
        String databaseLessonName = mapGodotNameToDatabase(lessonName);
        System.out.println("🎯 AppPlugin: Converted '" + lessonName + "' to '" + databaseLessonName + "' for database");
        
        // Persist score and progress
        LessonManager.markActivityCompleted(classCode, databaseLessonName, activityType, studentId, score, safeAttemptsUsed);
        
        System.out.println("✅ AppPlugin: Score submission completed");
    }
    
    // Helper method to convert Godot lesson names back to database format
    private String mapGodotNameToDatabase(String godotLessonName) {
        switch (godotLessonName) {
            case "lesson 1": return "INTRODUCTION TO JAVA";
            case "lesson 2": return "VARIABLES and DATA";
            case "lesson 3": return "OPERATORS and EXPRESSIONS";
            case "lesson 4": return "CONDITIONAL STATEMENTS";
            case "lesson 5": return "CONDITIONAL LOOPS";
            case "lesson 6": return "ARRAYS";
            default: return godotLessonName; // Fallback to original
        }
    }

    // -------------------- Navigation Helpers --------------------
    // Godot can call this to return to Android without killing the whole app
        @UsedByGodot
        public void navigateBack() {
        System.out.println("🔙 AppPlugin: navigateBack() called - IMMEDIATE EXIT");
        android.app.Activity activity = getActivity();
        if (activity == null) {
            System.out.println("❌ AppPlugin: Activity is null, cannot navigate back");
            return;
        }
        
        // FIXED: Use process kill for immediate exit to prevent hanging
        System.out.println("🔙 AppPlugin: IMMEDIATE EXIT - Killing process to ensure exit");
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}
