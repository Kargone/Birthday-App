package com.karson.birthday_app;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class BirthdayWorker extends Worker {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/uuuu");

    public  BirthdayWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        Map<String, ?> all = context.getSharedPreferences("BirthdayPrefs", Context.MODE_PRIVATE).getAll();
        MonthDay today = MonthDay.now();

        for (Map.Entry<String, ?> e : all.entrySet()) {
            LocalDate date = LocalDate.parse(e.getValue().toString(), DATE_FORMAT);
            if (MonthDay.from(date).equals(today)) {
                BirthdayTexts.showNotification(context, e.getKey());
            }
        }
        return Result.success();
    }
}
