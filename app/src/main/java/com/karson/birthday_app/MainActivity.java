package com.karson.birthday_app;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.MonthDay;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private Map<String, String> birthdayInfo = new HashMap<>();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/uuuu");
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("MM/dd");
    private EditText nameInput;
    private EditText dateInput;
    private BirthdayAdapter adapter;
    private boolean showThisMonthOnly = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        NotificationChannel channel = new NotificationChannel(BirthdayTexts.CHANNEL_ID, "Birthday reminders", NotificationManager.IMPORTANCE_HIGH);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);

        addPadding();
        setupListeners();
        setupSharedPreferences();
        setupBirthdayList();
        updateBirthdayText();
        handleIntent(getIntent());
        dailyBirthdayCheck();

        getSystemService(NotificationManager.class).deleteNotificationChannel("birthdays");
    }

    private void addPadding() {
        View root = findViewById(R.id.main);
        int padLeft = root.getPaddingLeft();
        int padTop = root.getPaddingTop();
        int padRight = root.getPaddingRight();
        int padBottom = root.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets keyboard = insets.getInsets(WindowInsetsCompat.Type.ime());

            v.setPadding(
                padLeft + systemBars.left,
                padTop + systemBars.top,
                padRight + systemBars.right,
                padBottom + Math.max(systemBars.bottom, keyboard.bottom)
            );

            return insets;
        });
    }

    private void setupBirthdayList() {
        RecyclerView list = findViewById(R.id.birthdayList);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BirthdayAdapter(this::showEditPersonDialog);
        list.setAdapter(adapter);
    }

    private void updateBirthdayText() {
        List<Map.Entry<String, String>> sorted = sortBirthdays(false);

        List<Month> months = new ArrayList<>();
        for (Map.Entry<String, String> entry : sorted) {
            months.add(Month.from(nextBirthday(entry.getValue())));
        }

        List<BirthdayAdapter.Row> rows = new ArrayList<>();
        Month previousMonth = null;

        for (int i = 0; i < sorted.size(); i++) {
            Map.Entry<String, String> entry = sorted.get(i);
            Month month = months.get(i);


            if (!month.equals(previousMonth)) {
                int count = Collections.frequency(months, month);
                rows.add(BirthdayAdapter.Row.header(
                    month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                        + " (" + count + "):"
                ));
                previousMonth = month;
            }

            LocalDate birthday = LocalDate.parse(entry.getValue(), DATE_FORMAT);
            MonthDay today = MonthDay.now();
            String weekday = nextBirthday(entry.getValue())
                    .getDayOfWeek()
                    .getDisplayName(TextStyle.NARROW, Locale.getDefault());
            String text = birthday.format(DISPLAY_FORMAT) + " (" + weekday + ") " + entry.getKey();
            int age = Period.between(birthday, LocalDate.now()).getYears();
            boolean isBirthdayToday = MonthDay.from(birthday).equals(today);

            rows.add(BirthdayAdapter.Row.person(text, entry.getKey(), age, isBirthdayToday));
        }

        adapter.setRows(rows);
        findViewById(R.id.birthdayList).setVisibility(View.VISIBLE);
        findViewById(R.id.birthdayText).setVisibility(View.GONE);
    }

    private void showStats() {
        TextView birthdayTextView = findViewById(R.id.birthdayText);
        birthdayTextView.setMovementMethod(new ScrollingMovementMethod());
        birthdayTextView.setVisibility(View.VISIBLE);
        findViewById(R.id.birthdayList).setVisibility(View.GONE);

        if (birthdayInfo.isEmpty()) {
            birthdayTextView.setText("No birthdays yet");
            return;
        }

        String youngestName = null, oldestName = null;
        LocalDate youngestDate = null, oldestDate = null;
        Map<MonthDay, List<String>> peopleByDay = new HashMap<>();

        for (Map.Entry<String, String> entry : birthdayInfo.entrySet()) {
            LocalDate date = LocalDate.parse(entry.getValue(), DATE_FORMAT);

            if (youngestDate == null || date.isAfter(youngestDate)) {
                youngestDate = date;
                youngestName = entry.getKey();
            }
            if (oldestDate == null || date.isBefore(oldestDate)) {
                oldestDate = date;
                oldestName = entry.getKey();
            }

            peopleByDay.computeIfAbsent(MonthDay.from(date), k -> new ArrayList<>()).add(entry.getKey());
        }

        List<String> doubles = new ArrayList<>();
        List<String> triples = new ArrayList<>();
        for (Map.Entry<MonthDay, List<String>> group : peopleByDay.entrySet()) {
            String text = String.join(" & ", group.getValue()) + " (" +
                    group.getKey().format(DISPLAY_FORMAT) + ")";

            if (group.getValue().size() == 2) doubles.add(text);
            else if (group.getValue().size() == 3) triples.add(text);
        }

        int youngestAge = Period.between(youngestDate, LocalDate.now()).getYears();
        int oldestAge = Period.between(oldestDate, LocalDate.now()).getYears();

        List<String> lines = new ArrayList<>();
        lines.add("People: " + birthdayInfo.size());
        lines.add("Youngest: " + youngestName + " (" + youngestAge + ")");
        lines.add("Oldest: " + oldestName + " (" + oldestAge + ")");
        lines.add("");
        lines.add("Doubles: " + (doubles.isEmpty() ? "none" : ""));
        lines.addAll(doubles);
        lines.add("");
        lines.add("Triples: " + (triples.isEmpty() ? "none" : ""));
        lines.addAll(triples);
        birthdayTextView.setText(String.join("\n", lines));

    }

    List<Map.Entry<String, String>> sortBirthdays(boolean reversed) {
        List<Map.Entry<String, String>> entries = new ArrayList<>(birthdayInfo.entrySet());
        if (showThisMonthOnly) {
            Month thisMonth = LocalDate.now().getMonth();
            entries.removeIf(e -> LocalDate.parse(e.getValue(), DATE_FORMAT).getMonth() != thisMonth);
        }
        Comparator<Map.Entry<String, String>> byDate = Comparator.comparing((Map.Entry<String, String> e) -> monthsFromNow(e.getValue()))
                .thenComparing(e -> LocalDate.parse(e.getValue(), DATE_FORMAT).getDayOfMonth());
        entries.sort(reversed ? byDate.reversed() : byDate);
        return entries;
    }

    private LocalDate nextBirthday(String date) {
        LocalDate today = LocalDate.now();
        LocalDate next = MonthDay.from(LocalDate.parse(date, DATE_FORMAT)).atYear(today.getYear());
        return next.isBefore(today) ? next.plusYears(1) : next;
    }

    private int monthsFromNow(String date) {
        int birthMonth = LocalDate.parse(date, DATE_FORMAT).getMonthValue();
        int currentMonth = LocalDate.now().getMonthValue();
        return (birthMonth - currentMonth + 12) % 12;
    }

    private void setupListeners() {
        nameInput = findViewById(R.id.nameInput);
        dateInput = findViewById(R.id.dateInput);

        dateInput.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting = false;
            @Override
            public void afterTextChanged(Editable editable) {
                if (isFormatting) return;
                isFormatting = true;

                String digits = editable.toString().replaceAll("[^0-9]", "");
                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < digits.length() && i < 8; i++) {
                    if (i == 2 || i == 4) formatted.append('/');
                    formatted.append(digits.charAt(i));
                }
                editable.replace(0, editable.length(), formatted);

                isFormatting = false;
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
        });

        dateInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            private boolean isValidDate(String date) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/uuuu")
                        .withResolverStyle(ResolverStyle.STRICT);
                try {
                    LocalDate parsed = LocalDate.parse(date, formatter);
                    return !parsed.isAfter(LocalDate.now());
                } catch (DateTimeParseException e) {
                    return false;
                }
            }
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                        (keyEvent != null && keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER && keyEvent.getAction() == KeyEvent.ACTION_DOWN)) {
                    String name = nameInput.getText().toString().trim();
                    String date = dateInput.getText().toString();

                    if (!name.isEmpty() && isValidDate(date)) {
                        birthdayInfo.put(name, date);

                        SharedPreferences sharedPreferences = getSharedPreferences("BirthdayPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString(name, date);
                        editor.apply();

                        updateBirthdayText();

                        nameInput.setText("");
                        dateInput.setText("");
                        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(dateInput.getWindowToken(), 0);
                        dateInput.clearFocus();
                    } else if (!isValidDate(date)) {
                        dateInput.setError("Use MM/DD/YYYY");
                    } else {
                        nameInput.setError("Empty name");
                    }

                    return true;
                }
                return false;
            }
        });

        findViewById(R.id.all_birthdays).setOnClickListener(v -> {
            showThisMonthOnly = false;
            updateBirthdayText();
        });

        findViewById(R.id.month_birthdays).setOnClickListener(v -> {
            showThisMonthOnly = true;
            updateBirthdayText();
        });

        findViewById(R.id.stats_button).setOnClickListener(v -> showStats());

        findViewById(R.id.notification_setup).setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 33 &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        });
    }

    private void setupSharedPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("BirthdayPrefs", MODE_PRIVATE);
        Map<String, ?> allEntries = sharedPreferences.getAll();

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String name = entry.getKey();
            String birthday = entry.getValue().toString();
            birthdayInfo.put(name, birthday);
        }
    }
    private void showEditPersonDialog(String name) {
        EditText phoneInput = new EditText(this);
        phoneInput.setHint("Phone number");
        phoneInput.setInputType(InputType.TYPE_CLASS_PHONE);
        phoneInput.setText(BirthdayTexts.getPhone(this, name));

        EditText messageInput = new EditText(this);
        messageInput.setHint("Birthday Message");
        messageInput.setText(BirthdayTexts.getMessage(this, name));

        new AlertDialog.Builder(this).setTitle(name).setView(dialogLayout(phoneInput, messageInput))
                .setPositiveButton("Save", (dialog, which) -> {
                    BirthdayTexts.savePhone(this, name, phoneInput.getText().toString().trim());
                    BirthdayTexts.saveMessage(this, name, messageInput.getText().toString().trim());
                }).setNeutralButton("Send now", (dialog, which) -> sendTextTo(name))
                .setNegativeButton("Delete", (dialog, which) -> confirmDelete(name)).show();
    }

    private void confirmDelete(String name) {
        new AlertDialog.Builder(this).setTitle("Delete " + name + "?")
                .setMessage("This removes their birthday, phone number, and message.")
                .setPositiveButton("Delete", (dialog, which) -> deletePerson(name))
                .setNegativeButton("Cancel", null).show();
    }

    private void deletePerson(String name) {
        birthdayInfo.remove(name);
        getSharedPreferences("BirthdayPrefs", MODE_PRIVATE).edit().remove(name).apply();

        BirthdayTexts.savePhone(this, name, null);
        BirthdayTexts.deleteMessage(this, name);
        NotificationManagerCompat.from(this).cancel(name.hashCode());

        updateBirthdayText();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }
    void handleIntent(Intent intent) {
        if (intent == null || !BirthdayTexts.ACTION_SEND_TEXT.equals(intent.getAction())) return;

        String name = intent.getStringExtra("name");
        if (name == null) return;

        intent.setAction(null);
        NotificationManagerCompat.from(this).cancel(name.hashCode());
        sendTextTo(name);
    }

    private void dailyBirthdayCheck() {
        // Wait until the next 12:00 AM before the first run, then repeat every day
        LocalDateTime nextMidnight = LocalDate.now().plusDays(1).atStartOfDay().plusMinutes(1);
        long delaySeconds = Duration.between(LocalDateTime.now(), nextMidnight).getSeconds();

        PeriodicWorkRequest daily = new PeriodicWorkRequest.Builder(BirthdayWorker.class, 1, TimeUnit.DAYS)
                .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("birthday_check", ExistingPeriodicWorkPolicy.KEEP, daily);
    }
    void sendTextTo(String name) {
        String phone = BirthdayTexts.getPhone(this, name);
        String message = BirthdayTexts.getMessage(this, name);

        if (phone != null) {
            BirthdayTexts.openSmsApp(this, phone, message);
            return;
        }

        EditText phoneInput = new EditText(this);
        phoneInput.setHint("Phone number");
        phoneInput.setInputType(InputType.TYPE_CLASS_PHONE);

        CheckBox saveBox = new CheckBox(this);
        saveBox.setText("Save this number for " + name);
        saveBox.setChecked(true);

        new AlertDialog.Builder(this).setTitle("No number for " + name)
                .setView(dialogLayout(phoneInput, saveBox))
                .setPositiveButton("Send", (dialog, which) -> {
                    String entered = phoneInput.getText().toString().trim();
                    if (entered.isEmpty()) return;
                    if (saveBox.isChecked()) BirthdayTexts.savePhone(this, name, entered);
                    BirthdayTexts.openSmsApp(this, entered, message);
                }).setNegativeButton("Cancel", null).show();
    }

    private LinearLayout dialogLayout(View... views) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density); // 20dp in pixels
        layout.setPadding(pad, pad / 2, pad, 0);
        for (View view : views) layout.addView(view);
        return layout;
    }
}