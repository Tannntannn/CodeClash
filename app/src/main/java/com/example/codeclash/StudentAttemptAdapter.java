package com.example.codeclash;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class StudentAttemptAdapter extends RecyclerView.Adapter<StudentAttemptAdapter.StudentViewHolder> {
    
    private List<StudentAttemptManagementActivity.StudentAttemptInfo> students;
    private OnStudentClickListener listener;
    private StudentAttemptManagementActivity activity;
    
    public interface OnStudentClickListener {
        void onStudentClick(StudentAttemptManagementActivity.StudentAttemptInfo student);
    }
    
    public StudentAttemptAdapter(List<StudentAttemptManagementActivity.StudentAttemptInfo> students, OnStudentClickListener listener, StudentAttemptManagementActivity activity) {
        this.students = students;
        this.listener = listener;
        this.activity = activity;
    }
    
    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student_attempt, parent, false);
        return new StudentViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        StudentAttemptManagementActivity.StudentAttemptInfo student = students.get(position);
        
        holder.studentNameText.setText(student.studentName);
        holder.attemptsText.setText(student.attemptsUsed + "/3 attempts used");
        
        // Setup attempt management buttons
        setupAttemptButtons(student, holder);
        
        // Remove the old click listener since we now have direct buttons
        holder.itemView.setOnClickListener(null);
    }
    
    private void setupAttemptButtons(StudentAttemptManagementActivity.StudentAttemptInfo student, StudentViewHolder holder) {
        // Add attempt button
        holder.btnAddAttempt.setOnClickListener(v -> {
            System.out.println("🔍 StudentAttemptAdapter: Adding attempt for " + student.studentName);
            activity.addAttemptForStudent(student);
        });
        
        // Minus attempt button
        holder.btnMinusAttempt.setOnClickListener(v -> {
            System.out.println("🔍 StudentAttemptAdapter: Subtracting attempt for " + student.studentName);
            activity.subtractAttemptForStudent(student);
        });
        
        // Reset attempts button
        holder.btnResetAttempts.setOnClickListener(v -> {
            System.out.println("🔍 StudentAttemptAdapter: Resetting attempts for " + student.studentName);
            activity.resetAttemptsForStudent(student);
        });
    }
    
    @Override
    public int getItemCount() {
        return students.size();
    }
    
    public static class StudentViewHolder extends RecyclerView.ViewHolder {
        TextView studentNameText;
        TextView attemptsText;
        Button btnAddAttempt;
        Button btnMinusAttempt;
        Button btnResetAttempts;
        
        public StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            studentNameText = itemView.findViewById(R.id.textStudentName);
            attemptsText = itemView.findViewById(R.id.textAttempts);
            btnAddAttempt = itemView.findViewById(R.id.btnAddAttempt);
            btnMinusAttempt = itemView.findViewById(R.id.btnMinusAttempt);
            btnResetAttempts = itemView.findViewById(R.id.btnResetAttempts);
        }
    }
}
