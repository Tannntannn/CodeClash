package com.example.codeclash;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.codeclash.Fragments.Leaderboards;

public class StudentLeaderboardsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_leaderboards);

        if (savedInstanceState == null) {
            Leaderboards fragment = new Leaderboards();
            fragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.leaderboardsContainer, fragment)
                    .commit();
        }
    }
}


