package org.hear2read.indic.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.hear2read.indic.BuildConfig;
import org.hear2read.indic.R;

import java.util.Objects;

public class FliteInfoActivity extends AppCompatActivity {
    private ListAdapter mAdapter;
    private ListView mList;
    private Handler mHandler = new Handler();
    private boolean mFinishedStart = false;
    //private NativeFliteTTS mFliteEngine;

    private Runnable mRequestFocus = new Runnable() {
        public void run() {
            mList.focusableViewAvailable(mList);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flite_info);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.hear2read_logo);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        //mFliteEngine = new NativeFliteTTS(this, null);
        ProgressDialog progress = new ProgressDialog(this);
        //progress.setMessage("Benchmarking Flite. Wait a few seconds");
        //progress.setCancelable(false);
        new GetInformation(progress).execute();

    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        ensureList();
        super.onRestoreInstanceState(state);
    }

    private void ensureList() {
        if (mList != null) {
            return;
        }
        setContentView(android.R.layout.simple_list_item_1);
    }

    @Override
    protected void onDestroy() {
        mHandler.removeCallbacks(mRequestFocus);
        super.onDestroy();
    }

    /**
     * Updates the screen state (current list and other views) when the
     * content changes.
     *
     * @see Activity#onContentChanged()
     */
    @Override
    public void onContentChanged() {
        super.onContentChanged();
        mList = (ListView)findViewById(R.id.list_info);
        //mList.setOnItemClickListener(mOnClickListener);
        if (mFinishedStart) {
            setListAdapter(mAdapter);
        }
        mHandler.post(mRequestFocus);
        mFinishedStart = true;
    }

    private void setListAdapter(ListAdapter adapter) {
        synchronized (this) {
            ensureList();
            mAdapter = adapter;
            mList.setAdapter(adapter);
        }
    }

    private void populateInformation() {
//		if (mBenchmark <0) {
//			mBenchmark = mFliteEngine.getNativeBenchmark();
//		}
        final String[] Info = new String[] {
                "Copyright",
                "URL",
                "Android Version",
                "App Version",
                "Flite Release",
                "Device Model",
//				"Benchmark",
        };
        final String[] Data = new String[] {
                "© (1999–2018) Carnegie Mellon University" +
                        "\n© (2016–2021) Hear2Read",
                "www.cmuflite.org" +
                        "\nwww.hear2read.org",
                android.os.Build.VERSION.RELEASE,
                BuildConfig.VERSION_NAME,
                getString(R.string.flite_version),
                android.os.Build.MODEL,
//				mBenchmark + " times faster than real time",

        };

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                setListAdapter(new SettingsArrayAdapter(FliteInfoActivity.this, Info, Data));
            }
        });

    }

    private class SettingsArrayAdapter extends ArrayAdapter<String> {
        private final Context context;
        private final String[] values;
        private final String[] data;

        public SettingsArrayAdapter(Context context, String[] values, String[] data) {
            super(context, R.layout.list_item_info, values);
            this.context = context;
            this.values = values;
            this.data = data;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            if (Objects.equals(values[position], "RUNTIME_HEADER")) {
                return 0;
            }
            else return 1;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.list_item_info, parent, false);
            }

            TextView infoType = (TextView) convertView.findViewById(R.id.text_infotitle);
            TextView infoDetail = (TextView) convertView.findViewById(R.id.text_infodetail);

            if (Objects.equals(values[position], "RUNTIME_HEADER")) {
                infoType.setText("Runtime Information");
                infoType.setClickable(false);

                infoType.setTextColor(getResources().getColor(R.color.themeblue));
                infoType.setPadding(0,20,0,5);
                infoDetail.setVisibility(View.GONE);
            }
            else {
                infoType.setText(values[position]);
                infoDetail.setText(data[position]);
            }

            return convertView;
        }
    }

    private class GetInformation extends AsyncTask<Void, Void, Void> {

        private ProgressDialog progress;

        public GetInformation(ProgressDialog progress) {
            this.progress = progress;
        }

        @Override
        public void onPreExecute() {
            //progress.show();
        }

        @Override
        public Void doInBackground(Void... arg0) {
            populateInformation();
            return null;
        }

        @Override
        public void onPostExecute(Void unused) {
            progress.dismiss();
        }
    }
}
