package com.example.codeclash;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

/**
 * Dialog for showing connection status and handling poor connections
 */
public class ConnectionStatusDialog extends DialogFragment {
    
    public interface ConnectionDialogListener {
        void onRetry();
        void onCancel();
        void onGoOffline();
    }
    
    private static final String ARG_TITLE = "title";
    private static final String ARG_MESSAGE = "message";
    private static final String ARG_TYPE = "type";
    private static final String ARG_SHOW_OFFLINE_OPTION = "show_offline_option";
    
    public static final String TYPE_CONNECTION_LOST = "connection_lost";
    public static final String TYPE_TIMEOUT = "timeout";
    public static final String TYPE_POOR_CONNECTION = "poor_connection";
    public static final String TYPE_CONNECTION_RESTORED = "connection_restored";
    
    private ConnectionDialogListener listener;
    
    public static ConnectionStatusDialog newInstance(String title, String message, String type, boolean showOfflineOption) {
        ConnectionStatusDialog dialog = new ConnectionStatusDialog();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_MESSAGE, message);
        args.putString(ARG_TYPE, type);
        args.putBoolean(ARG_SHOW_OFFLINE_OPTION, showOfflineOption);
        dialog.setArguments(args);
        return dialog;
    }
    
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof ConnectionDialogListener) {
            listener = (ConnectionDialogListener) context;
        }
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        String title = args.getString(ARG_TITLE, "Connection Issue");
        String message = args.getString(ARG_MESSAGE, "Please check your connection");
        String type = args.getString(ARG_TYPE, TYPE_CONNECTION_LOST);
        boolean showOfflineOption = args.getBoolean(ARG_SHOW_OFFLINE_OPTION, false);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_connection_status, null);
        
        // Set up views
        ImageView iconView = dialogView.findViewById(R.id.connectionIcon);
        TextView titleView = dialogView.findViewById(R.id.connectionTitle);
        TextView messageView = dialogView.findViewById(R.id.connectionMessage);
        Button retryButton = dialogView.findViewById(R.id.retryButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        Button offlineButton = dialogView.findViewById(R.id.offlineButton);
        
                   // Set content based on type
                   switch (type) {
                       case TYPE_CONNECTION_LOST:
                           iconView.setImageResource(R.drawable.baseline_wifi_off_24);
                           titleView.setText("No Internet Connection");
                           messageView.setText("Please check your internet connection and try again.");
                           // Set red background for connection lost
                           ((View) iconView.getParent()).setBackgroundResource(R.drawable.connection_icon_background_lost);
                           break;
                       case TYPE_TIMEOUT:
                           iconView.setImageResource(R.drawable.baseline_refresh_24);
                           titleView.setText("Request Timed Out");
                           messageView.setText("The operation took too long to complete. This might be due to a poor connection.");
                           // Set orange background for timeout
                           ((View) iconView.getParent()).setBackgroundResource(R.drawable.connection_icon_background_timeout);
                           break;
                       case TYPE_POOR_CONNECTION:
                           iconView.setImageResource(R.drawable.baseline_wifi_off_24);
                           titleView.setText("Poor Connection");
                           messageView.setText("Your connection is slow. Some features may not work properly.");
                           // Set orange background for poor connection
                           ((View) iconView.getParent()).setBackgroundResource(R.drawable.connection_icon_background_poor);
                           break;
                       case TYPE_CONNECTION_RESTORED:
                           iconView.setImageResource(R.drawable.baseline_sync_lock_24);
                           titleView.setText("Connection Restored");
                           messageView.setText("Your internet connection has been restored. Syncing data...");
                           retryButton.setText("Continue");
                           cancelButton.setVisibility(View.GONE);
                           offlineButton.setVisibility(View.GONE);
                           // Set green background for connection restored
                           ((View) iconView.getParent()).setBackgroundResource(R.drawable.connection_icon_background_restored);
                           break;
                   }
        
        // Set up buttons
        retryButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRetry();
            }
            dismiss();
        });
        
        cancelButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCancel();
            }
            dismiss();
        });
        
        if (showOfflineOption) {
            offlineButton.setVisibility(View.VISIBLE);
            offlineButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onGoOffline();
                }
                dismiss();
            });
        } else {
            offlineButton.setVisibility(View.GONE);
        }
        
        builder.setView(dialogView);
        return builder.create();
    }
    
    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        if (listener != null) {
            listener.onCancel();
        }
    }
}
