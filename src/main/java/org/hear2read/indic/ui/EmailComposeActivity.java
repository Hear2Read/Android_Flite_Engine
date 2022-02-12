package org.hear2read.indic.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import org.hear2read.indic.BuildConfig;
import org.hear2read.indic.R;

import static org.hear2read.indic.util.Utility.sendEmail;

public class EmailComposeActivity extends AppCompatActivity {
    public static final String ACTIVITY_DETAILS_TAG = "activityDetails";

    private EditText mEmailComposeBox;
    private String mActivityDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        mActivityDetails = intent.getStringExtra(ACTIVITY_DETAILS_TAG);
        setContentView(R.layout.activity_email_compose);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.hear2read_logo);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        mEmailComposeBox = findViewById(R.id.email_compose_box);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_email, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == R.id.action_send_email) {
            String emailMessage = mEmailComposeBox.getText().toString();
            if ((mEmailComposeBox.getText() == null) ||
                    mEmailComposeBox.getText().toString().isEmpty()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
                String subjectLine = getString(R.string.email_subject) + " (v" +
                        BuildConfig.VERSION_NAME + ", " + mActivityDetails + ")" + " ["
                        + Build.MODEL + ", Android:" + Build.VERSION.RELEASE + "]";

                sendEmail("feedback@Hear2Read.org",
                        subjectLine,
                        mEmailComposeBox.getText().toString());
                finish();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void sendEmail(String emailTo, String emailSubject, String emailContent)
    {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto",emailTo, null));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, emailSubject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, emailContent);
        startActivity(Intent.createChooser(emailIntent, getString(R.string.email_client)));
    }
}
