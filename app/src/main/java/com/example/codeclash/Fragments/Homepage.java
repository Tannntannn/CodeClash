package com.example.codeclash.Fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.SearchView;


import com.example.codeclash.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.ArrayList;

public class Homepage extends Fragment {

    private View view;
    private LinearLayout classListContainer;
    private FirebaseFirestore db;
    private SearchView searchView;
    private final List<ClassItem> allClasses = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_homepage, container, false);
        classListContainer = view.findViewById(R.id.classListContainer);
        searchView = view.findViewById(R.id.searchView);
        db = FirebaseFirestore.getInstance();

        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    renderClasses(query);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    renderClasses(newText);
                    return true;
                }
            });
        }

        loadClasses();

        return view;
    }

    private void loadClasses() {
        String currentUserUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (currentUserUid == null) {
            if (isAdded() && getContext() != null) {
                Toast.makeText(getContext(), "Not signed in", Toast.LENGTH_SHORT).show();
            }
            System.out.println("⚠️ Homepage: currentUserUid is null, aborting loadClasses()");
            return;
        }

        System.out.println("🔐 Homepage: loading classes for uid=" + currentUserUid);

        db.collection("Classes").whereEqualTo("createdBy", currentUserUid).get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!isAdded() || getActivity() == null) return;
                
                allClasses.clear();

                View emptyView = view.findViewById(R.id.emptyMessage);
                System.out.println("📦 Homepage: classes found=" + queryDocumentSnapshots.size());
                
                // 🎮 Update class count badge
                TextView classCountBadge = view.findViewById(R.id.classCountBadge);
                if (classCountBadge != null) {
                    classCountBadge.setText(String.valueOf(queryDocumentSnapshots.size()));
                }
                
                if (queryDocumentSnapshots.isEmpty()) {
                    emptyView.setVisibility(View.VISIBLE);
                } else {
                    emptyView.setVisibility(View.GONE);
                }

                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                    String classCode = doc.getId();
                    String year = doc.getString("yearLevel");
                    String block = doc.getString("block");
                    List<String> lessons = (List<String>) doc.get("lessons");

                    allClasses.add(new ClassItem(classCode, year, block, lessons));
                }

                String q = searchView != null ? String.valueOf(searchView.getQuery()) : "";
                renderClasses(q);

            }).addOnFailureListener(e -> {
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load classes.", Toast.LENGTH_SHORT).show();
                }
                System.out.println("❌ Homepage: loadClasses failed: " + e.getMessage());
            });
    }

    private void addClassCard(String classCode, String year, String block, List<String> lessons) {
        Context context = getContext();
        if (context == null) return;
        CardView cardView = new CardView(context);
        cardView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        cardView.setCardElevation(8f);
        cardView.setRadius(12f);
        cardView.setUseCompatPadding(true);
        cardView.setPadding(16, 16, 16, 16);

        LinearLayout outerLayout = new LinearLayout(context);
        outerLayout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout innerLayout = new LinearLayout(context);
        innerLayout.setOrientation(LinearLayout.VERTICAL);
        innerLayout.setPadding(24, 24, 24, 24);

        TextView titleText = new TextView(context);
        titleText.setText("Class Code: " + classCode);
        titleText.setTextSize(18f);
        titleText.setTextColor(getResources().getColor(R.color.black));
        titleText.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView subtitleText = new TextView(context);
        subtitleText.setText("Year: " + year + " | Block: " + block);
        subtitleText.setTextSize(16f);
        subtitleText.setTextColor(getResources().getColor(R.color.black));

        // Create modern delete button with icon
        LinearLayout deleteButtonContainer = new LinearLayout(context);
        deleteButtonContainer.setOrientation(LinearLayout.HORIZONTAL);
        deleteButtonContainer.setGravity(android.view.Gravity.CENTER_VERTICAL);
        deleteButtonContainer.setPadding(16, 12, 16, 12);
        deleteButtonContainer.setBackgroundResource(R.drawable.delete_button_background);
        deleteButtonContainer.setElevation(4f);
        
        // Delete icon
        TextView deleteIcon = new TextView(context);
        deleteIcon.setText("🗑️");
        deleteIcon.setTextSize(16f);
        deleteIcon.setPadding(0, 0, 8, 0);
        
        // Delete text
        TextView deleteText = new TextView(context);
        deleteText.setText("Delete Class");
        deleteText.setTextColor(getResources().getColor(android.R.color.white));
        deleteText.setTextSize(14f);
        deleteText.setTypeface(null, android.graphics.Typeface.BOLD);
        
        deleteButtonContainer.addView(deleteIcon);
        deleteButtonContainer.addView(deleteText);
        
        // Add margin to the delete button
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        deleteParams.setMargins(0, 16, 0, 0);
        deleteButtonContainer.setLayoutParams(deleteParams);

        deleteButtonContainer.setOnClickListener(v -> {
            // Add subtle animation
            deleteButtonContainer.setScaleX(0.95f);
            deleteButtonContainer.setScaleY(0.95f);
            deleteButtonContainer.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(150)
                    .start();
            // Modern confirmation dialog
            new AlertDialog.Builder(context)
                    .setTitle("🗑️ Delete Class")
                    .setMessage("Are you sure you want to delete this class?\n\n" +
                               "Class: " + classCode + "\n" +
                               "Year: " + year + " | Block: " + block + "\n\n" +
                               "This action cannot be undone.")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        // Show loading state
                        deleteText.setText("Deleting...");
                        deleteButtonContainer.setEnabled(false);
                        // Verify ownership before deletion
                        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
                        if (uid == null) {
                            Toast.makeText(context, "Not signed in", Toast.LENGTH_SHORT).show();
                            deleteText.setText("Delete Class");
                            deleteButtonContainer.setEnabled(true);
                            return;
                        }
                        db.collection("Classes").document(classCode).get()
                                .addOnSuccessListener(doc -> {
                                    String owner = doc.getString("createdBy");
                                    if (owner != null && owner.equals(uid)) {
                                        db.collection("Classes").document(classCode)
                                                .delete()
                                                .addOnSuccessListener(unused -> {
                                                    Toast.makeText(context, "✅ Class deleted successfully", Toast.LENGTH_SHORT).show();
                                                    loadClasses();
                                                })
                                                .addOnFailureListener(e2 -> {
                                                    Toast.makeText(context, "❌ Failed to delete class", Toast.LENGTH_SHORT).show();
                                                    deleteText.setText("Delete Class");
                                                    deleteButtonContainer.setEnabled(true);
                                                });
                                    } else {
                                        Toast.makeText(context, "You don't own this class", Toast.LENGTH_SHORT).show();
                                        deleteText.setText("Delete Class");
                                        deleteButtonContainer.setEnabled(true);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(context, "❌ Failed to verify ownership", Toast.LENGTH_SHORT).show();
                                    deleteText.setText("Delete Class");
                                    deleteButtonContainer.setEnabled(true);
                                });
                    })
                    .setNegativeButton("Cancel", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        });

        innerLayout.addView(titleText);
        innerLayout.addView(subtitleText);
        innerLayout.addView(deleteButtonContainer);

        outerLayout.addView(innerLayout);
        cardView.addView(outerLayout);

        // Handle card click for opening class details
        cardView.setOnClickListener(v -> {
            Intent intent = new Intent(context, com.example.codeclash.TeacherClassDetailActivity.class);
            intent.putExtra("classCode", classCode);
            intent.putExtra("yearLevel", year);
            intent.putExtra("block", block);
            startActivity(intent);
        });
        
        // Handle long press for quick delete option
        cardView.setOnLongClickListener(v -> {
            // Show quick delete confirmation
            new AlertDialog.Builder(context)
                    .setTitle("🗑️ Quick Delete")
                    .setMessage("Delete class " + classCode + "?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        db.collection("Classes").document(classCode)
                                .delete()
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(context, "✅ Class deleted", Toast.LENGTH_SHORT).show();
                                    loadClasses();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(context, "❌ Delete failed", Toast.LENGTH_SHORT).show();
                                });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true; // Consume the long press
        });

        classListContainer.addView(cardView);
    }

    private void renderClasses(String query) {
        if (!isAdded()) return;
        classListContainer.removeAllViews();
        String q = query == null ? "" : query.trim().toLowerCase();

        for (ClassItem item : allClasses) {
            String code = item.classCode != null ? item.classCode.toLowerCase() : "";
            String year = item.year != null ? item.year.toLowerCase() : "";
            String block = item.block != null ? item.block.toLowerCase() : "";

            if (q.isEmpty() || code.contains(q) || year.contains(q) || block.contains(q)) {
                addClassCard(item.classCode, item.year, item.block, item.lessons);
            }
        }
    }

    private static class ClassItem {
        final String classCode;
        final String year;
        final String block;
        final List<String> lessons;

        ClassItem(String classCode, String year, String block, List<String> lessons) {
            this.classCode = classCode;
            this.year = year;
            this.block = block;
            this.lessons = lessons;
        }
    }
}
