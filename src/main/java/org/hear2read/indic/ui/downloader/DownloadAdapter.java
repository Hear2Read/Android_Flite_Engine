package org.hear2read.indic.ui.downloader;

/**
 * Adapter for the DownloadActivity
 * on the phone
 * Created by shyam on 23/12/17.
 */

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.hear2read.indic.R;

import java.util.List;

public class DownloadAdapter extends RecyclerView.Adapter<DownloadAdapter.MyViewHolder> {
    private static final String LOG_TAG = "Flite_Java_" + DownloadAdapter.class.getSimpleName();


    private List<DownloadListItem> mDownloadsList;
    private AppCompatActivity mActivity;

    class MyViewHolder extends RecyclerView.ViewHolder {
        TextView icon, name, voices;
        LinearLayout downloadContainer;

        MyViewHolder(View view) {
            super(view);
            name = (TextView) view.findViewById(R.id.text_download_name);
            downloadContainer = (LinearLayout) view.findViewById(R.id.layout_download_container);
        }
        void bind(int position) {

            DownloadListItem downloadItem = mDownloadsList.get(position);
            name.setText(downloadItem.getName());

            addOnClick(downloadItem.getPackageName());

        }

        void addOnClick(final String packageName) {
            downloadContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {Intent intent = new Intent(Intent.ACTION_VIEW);
            //intent.setData(Uri.parse("market://details?id=" + packageName));
            intent.setData(Uri.parse("http://play.google.com/store/apps/details?id=" + packageName));
            mActivity.startActivity(intent);
            mActivity.finish();
                }
            });
        }
    }

    DownloadAdapter(AppCompatActivity activity, List<DownloadListItem> downloadsList) {
        this.mActivity = activity;
        this.mDownloadsList = downloadsList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View itemView = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.list_item_download, viewGroup, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
/*
        LanguageListItem language = mLanguagesList.get(position);
        holder.icon.setText(language.getIcon());
        holder.name.setText(language.getName());
        //holder.voices.setText(language.getVoices());

        addOnClick(holder, language.getISO3());
*/
        holder.bind(position);
    }

/*
    private void addOnClick(MyViewHolder holder, final String iso3) {
        holder.languageContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(mActivity, TTSDemoActivity.class);
                intent.putExtra("ISO3", iso3);
                mActivity.startActivity(intent);
            }
        });
    }
*/

    @Override
    public int getItemCount() {
        return mDownloadsList.size();
    }
}