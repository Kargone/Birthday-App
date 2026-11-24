package com.karson.birthday_app;


import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class BirthdayAdapter extends
        RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_PERSON = 1;

    interface OnPersonClickListener {
        void onPersonClick(String name);
    }

    static class Row {
        final String text;
        final String name;
        final int age;
        final boolean isBirthdayToday;

        private Row(String text, String name, int age, boolean isBirthdayToday) {
            this.text = text;
            this.name = name;
            this.age = age;
            this.isBirthdayToday = isBirthdayToday;
        }

        static Row header(String text) {
            return new Row(text, null, 0, false);
        }

        static Row person(String text, String name, int age, boolean isBirthdayToday) {
            return new Row(text, name, age, isBirthdayToday);
        }
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        final TextView monthText;

        HeaderHolder(View view) {
            super(view);
            monthText = view.findViewById(R.id.monthText);
        }
    }

    static class PersonHolder extends RecyclerView.ViewHolder {
        final TextView personText;
        final TextView ageText;

        PersonHolder(View view) {
            super(view);
            personText = view.findViewById(R.id.personText);
            ageText = view.findViewById(R.id.ageText);
        }
    }

    private final List<Row> rows = new ArrayList<>();
    private final OnPersonClickListener listener;

    BirthdayAdapter(OnPersonClickListener listener) {
        this.listener = listener;
    }

    void setRows(List<Row> newRows) {
        rows.clear();
        rows.addAll(newRows);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).name == null ? TYPE_HEADER : TYPE_PERSON;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderHolder(inflater.inflate(R.layout.item_month_header, parent, false));
        }
        return new PersonHolder(inflater.inflate(R.layout.item_person, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);

        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).monthText.setText(row.text);
        } else {
            PersonHolder personHolder = (PersonHolder) holder;
            personHolder.personText.setText(row.text);
            personHolder.ageText.setText("(" + row.age + ")");
            personHolder.itemView.setOnClickListener(v -> listener.onPersonClick(row.name));
            if (row.isBirthdayToday) {
                personHolder.personText.setTypeface(null, Typeface.BOLD);
                personHolder.ageText.setTypeface(null, Typeface.BOLD);
            } else {
                personHolder.personText.setTypeface(null, Typeface.NORMAL);
                personHolder.ageText.setTypeface(null, Typeface.NORMAL);
            }
        }
    }
}
