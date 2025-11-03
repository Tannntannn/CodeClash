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

public class JoinRequestAdapter extends RecyclerView.Adapter<JoinRequestAdapter.JoinRequestViewHolder> {
    private List<TeacherApprovalActivity.JoinRequest> joinRequests;
    private OnRequestActionListener actionListener;
    
    public interface OnRequestActionListener {
        void onRequestAction(TeacherApprovalActivity.JoinRequest request, String action);
    }
    
    public JoinRequestAdapter(List<TeacherApprovalActivity.JoinRequest> joinRequests, OnRequestActionListener actionListener) {
        this.joinRequests = joinRequests;
        this.actionListener = actionListener;
    }
    
    @NonNull
    @Override
    public JoinRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_join_request, parent, false);
        return new JoinRequestViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull JoinRequestViewHolder holder, int position) {
        TeacherApprovalActivity.JoinRequest request = joinRequests.get(position);
        holder.bind(request);
    }
    
    @Override
    public int getItemCount() {
        return joinRequests.size();
    }
    
    class JoinRequestViewHolder extends RecyclerView.ViewHolder {
        private TextView studentNameText;
        private TextView requestTimeText;
        private Button approveButton;
        private Button rejectButton;
        
        public JoinRequestViewHolder(@NonNull View itemView) {
            super(itemView);
            studentNameText = itemView.findViewById(R.id.studentNameText);
            requestTimeText = itemView.findViewById(R.id.requestTimeText);
            approveButton = itemView.findViewById(R.id.approveButton);
            rejectButton = itemView.findViewById(R.id.rejectButton);
        }
        
        public void bind(TeacherApprovalActivity.JoinRequest request) {
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
