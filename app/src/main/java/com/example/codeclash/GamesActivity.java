package com.example.codeclash;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.ViewGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.android.material.button.MaterialButton;
import java.util.List;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.widget.LinearLayout;

public class GamesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_games);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        // 🟪 Get class details from intent
        String classCode = getIntent().getStringExtra("classCode");
        String yearLevel = getIntent().getStringExtra("yearLevel");
        String block = getIntent().getStringExtra("block");

        // 🟪 Update the class title at the top
        TextView classTitle = findViewById(R.id.classTitle);
        if (yearLevel != null && block != null) {
            String title = "Year " + yearLevel + " - Block " + block;
            if (classCode != null) {
                title += "\n(" + classCode + ")";
            }
            classTitle.setText(title);
        }

        // 🟪 Dynamically load lessons for this class
        if (classCode != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("Classes").document(classCode).get()
                .addOnSuccessListener(documentSnapshot -> {
                    List<String> lessons = (List<String>) documentSnapshot.get("lessons");
                    showLessonsDynamically(lessons);
                });
        }
    }

    private void showLessonsDynamically(List<String> lessons) {
        LinearLayout buttonContainer = findViewById(R.id.buttonContainer);
        buttonContainer.removeAllViews();
        if (lessons == null || lessons.isEmpty()) {
            TextView noLessons = new TextView(this);
            noLessons.setText("No lessons available.");
            buttonContainer.addView(noLessons);
            return;
        }
        for (String lesson : lessons) {
            MaterialButton btn = new MaterialButton(this);
            btn.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            btn.setText(lesson);
            btn.setTextColor(getResources().getColor(android.R.color.white));
            btn.setBackgroundTintList(getResources().getColorStateList(R.color.purple_200));
            btn.setCornerRadius(100);
            btn.setStrokeWidth(2);
            btn.setStrokeColorResource(R.color.purple_700);
            btn.setIconResource(R.drawable.baseline_person_24);
            btn.setIconTintResource(android.R.color.white);
            btn.setIconPadding(8);
            btn.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 120);
            params.setMargins(0, 0, 0, 24);
            btn.setLayoutParams(params);
            buttonContainer.addView(btn);
        }
    }
}
