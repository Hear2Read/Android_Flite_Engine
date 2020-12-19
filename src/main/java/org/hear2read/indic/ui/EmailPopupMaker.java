package org.hear2read.indic.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroupOverlay;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;

import org.hear2read.indic.R;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;
import static org.hear2read.indic.util.Utility.sendEmail;

/**
 * Created by shyama on 17/1/18.
 * Helper class to show a PopupWindow for sending email feedback
 */

public class EmailPopupMaker {

    public static void popupEmail(PopupWindow popupWindow, final Activity parent, final String emailTo,
                                  final String emailSubject) {

        // Initialize a new instance of LayoutInflater service
        LayoutInflater inflater = (LayoutInflater) parent.getSystemService(LAYOUT_INFLATER_SERVICE);

        // Inflate the custom layout/view
        final ViewGroup root = (ViewGroup) parent.getWindow().getDecorView().getRootView();
        View customView = inflater.inflate(R.layout.popup_email,null);

        // Initialize a new instance of popup window
        popupWindow = new PopupWindow(
                customView,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
        );

        popupWindow.setBackgroundDrawable(new ColorDrawable());
        //applyDim(root, 0.5f);

        // Set an elevation value for popup window
        // Call requires API level 21
        if(Build.VERSION.SDK_INT>=21){
            popupWindow.setElevation(5.0f);
        }

        final EditText emailText = (EditText) customView.findViewById(R.id.text_feedback_content);
        popupWindow.setFocusable(true);
        emailText.getBackground().setColorFilter(parent.getResources().getColor(R.color.actBackgr),
                PorterDuff.Mode.SRC_ATOP);
        //popupWindow.update();


        Button emailButton = (Button) customView.findViewById(R.id.button_send_email);

        final PopupWindow finalPopupWindow = popupWindow;
        emailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Check if feedback is empty
                String emailMessage = emailText.getText().toString();
                if ((emailText.getText() == null) || emailText.getText().toString().isEmpty()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(parent);
                    builder.setMessage("Feedback empty!\n" +
                            "Please enter feedback before sending email");
                    builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    AlertDialog alert = builder.create();
                    alert.show();
                }
                else {
                    sendEmail(parent, emailTo, emailSubject, emailText.getText().toString());
                    finalPopupWindow.dismiss();
                }
            }
        });
/*
        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                //clearDim(root);
            }
        });
*/
        popupWindow.showAtLocation(root, Gravity.CENTER,0,0);
    }

/*
    // Dim background for popup
    private static void applyDim(@NonNull ViewGroup parent, float dimAmount){
        Drawable dim = new ColorDrawable(Color.BLACK);
        dim.setBounds(0, 0, parent.getWidth(), parent.getHeight());
        dim.setAlpha((int) (255 * dimAmount));

        ViewGroupOverlay overlay = parent.getOverlay();
        overlay.add(dim);
    }

    // Clear background for popup
    private static void clearDim(@NonNull ViewGroup parent) {
        ViewGroupOverlay overlay = parent.getOverlay();
        overlay.clear();
    }
*/
}
