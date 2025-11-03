package com.example.codeclash;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LeaveRequestAdapter extends RecyclerView.Adapter<LeaveRequestAdapter.LeaveRequestViewHolder> {
    private List<TeacherApprovalActivity.LeaveRequest> leaveRequests;
    private OnRequestActionListener actionListener;
    
    public interface OnRequestActionListener {
        void onRequestAction(TeacherApprovalActivity.LeaveRequest request, String action);
    }
    
    public LeaveRequestAdapter(List<TeacherApprovalActivity.LeaveRequest> leaveRequests, OnRequestActionListener actionListener) {
        this.leaveRequests = leaveRequests;
        this.actionListener = actionListener;
    }
    
    @NonNull
    @Override
    public LeaveRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_leave_request, parent, false);
        return new LeaveRequestViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull LeaveRequestViewHolder holder, int position) {
        TeacherApprovalActivity.LeaveRequest request = leaveRequests.get(position);
        holder.bind(request);
    }
    
    @Override
    public int getItemCount() {
        return leaveRequests.size();
    }
    
    class LeaveRequestViewHolder extends RecyclerView.ViewHolder {
        private TextView studentNameText;
        private TextView requestTimeText;
        private Button approveButton;
        private Button rejectButton;
        
        public LeaveRequestViewHolder(@NonNull View itemView) {
            super(itemView);
            studentNameText = itemView.findViewById(R.id.studentNameText);
            requestTimeText = itemView.findViewById(R.id.requestTimeText);
            approveButton = itemView.findViewById(R.id.approveButton);
            rejectButton = itemView.findViewById(R.id.rejectButton);
        }
        
        public void bind(TeacherApprovalActivity.LeaveRequest request) {
            studentNameText.setText(request.getStudentName());
            
            // Format request time
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            String formattedTime = sdf.format(new Date(request.getRequestTime()));
            requestTimeText.setText("Requested: " + formattedTime);
            
            // Set button listeners
            approveButton.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onRequestAction(request, "approve");
                }
            });
            
            rejectButton.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onRequestAction(request, "reject");
                }
            });
        }
    }
}
