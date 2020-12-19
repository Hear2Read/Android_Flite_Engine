package org.hear2read.indic.ui.manager;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.hear2read.indic.R;
import org.hear2read.indic.ui.TTSDemoActivity;

import java.util.List;

/**
 * Adapter for  the UI list of installed voices
 * Created by shyam on 16/12/17.
 */

public class LanguagesAdapter extends RecyclerView.Adapter<LanguagesAdapter.MyViewHolder> {
    private static final String LOG_TAG = "Flite_Java_" + LanguagesAdapter.class.getSimpleName();

    private List<LanguageListItem> mLanguagesList;
    private Context mContext;

    class MyViewHolder extends RecyclerView.ViewHolder {
        TextView icon, name, voices;
        RelativeLayout languageContainer;

        MyViewHolder(View view) {
            super(view);
            icon = (TextView) view.findViewById(R.id.icon_lang);
            name = (TextView) view.findViewById(R.id.text_lang_name);
            voices = (TextView) view.findViewById(R.id.text_lang_voices);
            languageContainer = (RelativeLayout) view.findViewById(R.id.layout_lang_container);
        }

        void bind(int position) {
            LanguageListItem language = mLanguagesList.get(position);
            icon.setText(language.getIcon());
            // The icon is text, so we don't want it read by Talkback
            icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            name.setText(language.getName());
            voices.setText(language.getVariant());

            addOnClick(language.getAndroidName());
        }

        void addOnClick(final String variant) {
            languageContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Intent intent = new Intent(mContext, TTSDemoActivity.class);
                    intent.putExtra("VoiceName", variant);
                    mContext.startActivity(intent);
                }
            });
        }
    }

    LanguagesAdapter(Context context, List<LanguageListItem> mLanguagesList) {
        this.mContext = context;
        this.mLanguagesList = mLanguagesList;
    }

    public LanguagesAdapter(List<LanguageListItem> mLanguagesList) {
        this.mLanguagesList = mLanguagesList;
    }

    @Override
    @NonNull
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View itemView = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.list_item_language, viewGroup, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return mLanguagesList.size();
    }
}