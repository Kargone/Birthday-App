package com.karson.birthday_app;

import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class BirthdayTexts {
    static final String ACTION_SEND_TEXT = "com.karson.birthday_app.SEND_TEXT";
    private static final String PHONE_PREFS = "PhonePrefs";
    private static final String MESSAGE_PREFS = "MessagePrefs";
    static final String CHANNEL_ID = "birthday_alerts";

    static String getPhone(Context context, String name) {
        return context.getSharedPreferences(PHONE_PREFS, Context.MODE_PRIVATE).getString(name, null);
    }

    static void savePhone(Context context, String name, String phone) {
        var prefs = context.getSharedPreferences(PHONE_PREFS, Context.MODE_PRIVATE).edit();
        if (phone == null || phone.isEmpty()) prefs.remove(name);
        else prefs.putString(name, phone);
        prefs.apply();
    }

    static String getMessage(Context context, String name) {
        return context.getSharedPreferences(MESSAGE_PREFS, Context.MODE_PRIVATE)
                .getString(name, "Happy birthday, " + name + "! 🎉");
    }

    static void saveMessage(Context context, String name, String message) {
        var prefs = context.getSharedPreferences(MESSAGE_PREFS, Context.MODE_PRIVATE).edit();
        if (message == null || message.isBlank()) prefs.remove(name);
        else prefs.putString(name, message);
        prefs.apply();
    }

    static void deleteMessage(Context context, String name) {
        context.getSharedPreferences(MESSAGE_PREFS, Context.MODE_PRIVATE).edit()
                .remove(name).apply();
    }

    static void showNotification(Context context, String name) {
        Intent sendIntent = new Intent(context, MainActivity.class).setAction(ACTION_SEND_TEXT)
                .putExtra("name", name).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent sendPending = PendingIntent.getActivity(context, name.hashCode(), sendIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification).setContentTitle("Birthday today!")
                .setContentText("It's " + name + "'s birthday").setContentIntent(sendPending)
                .addAction(0, "Send text", sendPending).setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        try {
            NotificationManagerCompat.from(context).notify(name.hashCode(), builder.build());
        } catch (SecurityException ignored) {}
    }

    static void openSmsApp(Context context, String phone, String message) {
        Intent sms = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(phone)));

        sms.putExtra("sms_body", message);
        try {
            context.startActivity(sms);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "No messaging app found", Toast.LENGTH_SHORT).show();
        }
    }
}
