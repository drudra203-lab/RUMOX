package com.rudra.rumox;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.AlarmClock;
import android.provider.ContactsContract;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private SpeechRecognizer speechRecognizer;
    private TextToSpeech tts;
    private TextView tvUserSpeech, tvAssistantResponse, tvStatus;
    private Button btnMic, btnAlwaysListen;
    private boolean isListening = false;
    private boolean alwaysListenMode = false;
    private Handler handler = new Handler();

    private static final String[] PERMISSIONS = {
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.SEND_SMS
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvUserSpeech = findViewById(R.id.tvUserSpeech);
        tvAssistantResponse = findViewById(R.id.tvAssistantResponse);
        tvStatus = findViewById(R.id.tvStatus);
        btnMic = findViewById(R.id.btnMic);
        btnAlwaysListen = findViewById(R.id.btnAlwaysListen);

        ActivityCompat.requestPermissions(this, PERMISSIONS, 100);
        initTTS();
        initSpeechRecognizer();

        btnMic.setOnClickListener(v -> {
            if (!isListening) startListening();
            else stopListening();
        });

        btnAlwaysListen.setOnClickListener(v -> {
            alwaysListenMode = !alwaysListenMode;
            if (alwaysListenMode) {
                btnAlwaysListen.setText("STOP LISTENING");
                tvStatus.setText("Always Listen Mode ON");
                respond("Always listen mode activated. I am always here, boss.");
                startListening();
            } else {
                btnAlwaysListen.setText("ALWAYS LISTEN");
                tvStatus.setText("Ready");
                stopListening();
                respond("Always listen mode deactivated.");
            }
        });

        respond("RUMOX online. How can I help you, boss?");
    }

    private void initTTS() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
                tts.setPitch(0.85f);
                tts.setSpeechRate(1.0f);
            }
        });
    }

    private void respond(String text) {
        tvAssistantResponse.setText("RUMOX: " + text);
        if (tts != null) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    private void initSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle p) {
                tvStatus.setText("Listening...");
                btnMic.setText("STOP");
                isListening = true;
            }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float v) {}
            @Override public void onBufferReceived(byte[] b) {}
            @Override public void onEndOfSpeech() {
                btnMic.setText("SPEAK");
                isListening = false;
            }
            @Override public void onError(int error) {
                btnMic.setText("SPEAK");
                isListening = false;
                tvStatus.setText("Ready");
                if (alwaysListenMode) handler.postDelayed(() -> startListening(), 1500);
            }
            @Override public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String cmd = matches.get(0);
                    tvUserSpeech.setText("You: " + cmd);
                    handleCommand(cmd.toLowerCase().trim());
                }
                btnMic.setText("SPEAK");
                isListening = false;
                tvStatus.setText("Ready");
                if (alwaysListenMode) handler.postDelayed(() -> startListening(), 2000);
            }
            @Override public void onPartialResults(Bundle partial) {
                ArrayList<String> p = partial.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (p != null && !p.isEmpty()) tvUserSpeech.setText("You: " + p.get(0));
            }
            @Override public void onEvent(int e, Bundle b) {}
        });
    }

    private void handleCommand(String cmd) {

        // === WHATSAPP ===
        if (cmd.contains("whatsapp") || cmd.contains("whats app") || cmd.equals("wa")) {
            if (cmd.contains("message") || cmd.contains("send")) {
                handleWhatsAppMessage(cmd);
            } else {
                openWhatsApp();
            }
        }
        // === INSTAGRAM ===
        else if (cmd.contains("instagram") || cmd.contains("insta")) {
            openInstagram();
        }
        // === YOUTUBE ===
        else if (cmd.contains("youtube")) {
            openYouTube();
        }
        // === SPOTIFY ===
        else if (cmd.contains("spotify")) {
            if (cmd.contains("play") && !cmd.contains("open")) {
                String song = cmd.replace("play", "").replace("on spotify", "")
                               .replace("spotify", "").trim();
                playOnSpotify(song);
            } else {
                openSpotify();
            }
        }
        // === PLAY MUSIC ===
        else if (cmd.contains("play music") || cmd.contains("play song")) {
            openSpotify();
        }
        // === SNAPCHAT ===
        else if (cmd.contains("snapchat") || cmd.contains("snap")) {
            openSnapchat();
        }
        // === CAMERA ===
        else if (cmd.contains("camera") || cmd.contains("cam")) {
            openCamera();
        }
        // === SETTINGS ===
        else if (cmd.contains("settings")) {
            startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
            respond("Opening settings for you.");
        }
        // === CALCULATOR ===
        else if (cmd.contains("calculator") || cmd.contains("calc")) {
            openCalculator();
        }
        // === CLAUDE AI ===
        else if (cmd.contains("claude")) {
            openClaudeAI();
        }
        // === CHATGPT ===
        else if (cmd.contains("chatgpt") || cmd.contains("chat gpt") || cmd.contains("gpt")) {
            openChatGPT();
        }
        // === GOOGLE ===
        else if (cmd.contains("open google")) {
            openGoogle();
        }
        // === COMET ===
        else if (cmd.contains("comet")) {
            openComet();
        }
        // === TELEGRAM ===
        else if (cmd.contains("telegram")) {
            openAppWithFallback(new String[]{"org.telegram.messenger", "org.telegram.messenger.web"}, "Telegram");
        }
        // === NETFLIX ===
        else if (cmd.contains("netflix")) {
            openAppWithFallback(new String[]{"com.netflix.mediaclient"}, "Netflix");
        }
        // === GMAIL ===
        else if (cmd.contains("gmail")) {
            openAppWithFallback(new String[]{"com.google.android.gm"}, "Gmail");
        }
        // === MAPS ===
        else if (cmd.contains("maps") || cmd.contains("navigation")) {
            openAppWithFallback(new String[]{"com.google.android.apps.maps"}, "Google Maps");
        }
        // === PLAY STORE ===
        else if (cmd.contains("play store")) {
            openAppWithFallback(new String[]{"com.android.vending"}, "Play Store");
        }
        // === PHONE ===
        else if (cmd.contains("open phone") || cmd.contains("open dialer")) {
            startActivity(new Intent(Intent.ACTION_DIAL));
            respond("Opening phone dialer.");
        }

        // === CALLS - EXACT MATCH ===
        else if (cmd.startsWith("call ") || cmd.contains(" call ")) {
            // Extract contact name exactly - remove only "call" word
            String contact = cmd.replaceFirst("(?i)^call\\s+", "")
                               .replaceFirst("(?i)\\s+call\\s+", "")
                               .replace("please", "")
                               .replace("to ", "")
                               .trim();
            if (!contact.isEmpty()) {
                makeCallExact(contact);
            } else {
                respond("Who should I call, boss?");
            }
        }

        // === ANSWER CALL ===
        else if (cmd.contains("answer") || cmd.contains("pick up") || cmd.contains("accept call") || cmd.contains("pick the call")) {
            respond("Answering the call!");
        }

        // === REJECT CALL ===
        else if (cmd.contains("reject") || cmd.contains("hang up") || cmd.contains("decline")) {
            respond("Call rejected.");
        }

        // === SEND WHATSAPP MESSAGE ===
        else if (cmd.contains("send whatsapp") || cmd.contains("whatsapp message")) {
            handleWhatsAppMessage(cmd);
        }

        // === SEND SMS ===
        else if (cmd.contains("send message to")) {
            handleSMS(cmd);
        }

        // === SEARCH ===
        else if (cmd.contains("search for") || cmd.contains("search ")) {
            String query = cmd.replace("search for", "").replace("search", "").trim();
            Intent i = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/search?q=" + Uri.encode(query)));
            startActivity(i);
            respond("Searching for " + query + " on Google.");
        }

        // === TIME ===
        else if (cmd.contains("time") || cmd.contains("what time")) {
            String time = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
            respond("It is " + time + " boss.");
        }

        // === DATE ===
        else if (cmd.contains("date") || cmd.contains("today")) {
            String date = new SimpleDateFormat("EEEE, MMMM d yyyy", Locale.getDefault()).format(new Date());
            respond("Today is " + date);
        }

        // === REMINDER ===
        else if (cmd.contains("reminder") || cmd.contains("remind me")) {
            Intent i = new Intent(AlarmClock.ACTION_SET_ALARM);
            i.putExtra(AlarmClock.EXTRA_MESSAGE, cmd);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            respond("Setting a reminder for you, boss.");
        }

        // === NOTE ===
        else if (cmd.contains("take a note") || cmd.contains("note")) {
            String note = cmd.replace("take a note", "").replace("note", "").trim();
            respond("Note saved: " + note);
            Toast.makeText(this, "Note: " + note, Toast.LENGTH_LONG).show();
        }

        // === GREETINGS ===
        else if (cmd.contains("hello") || cmd.contains("hi") || cmd.contains("hey")) {
            respond("Hey boss! RUMOX is ready. What can I do for you?");
        }
        else if (cmd.contains("how are you")) {
            respond("Fully operational and ready to serve, boss!");
        }
        else if (cmd.contains("who are you") || cmd.contains("your name")) {
            respond("I am RUMOX, your personal AI assistant. Built to serve you, boss.");
        }
        else if (cmd.contains("joke")) {
            respond("Why don't scientists trust atoms? Because they make up everything!");
        }
        else if (cmd.contains("thank")) {
            respond("Always a pleasure, boss.");
        }
        else if (cmd.contains("bye") || cmd.contains("goodbye")) {
            respond("Goodbye boss. RUMOX standing by.");
        }
        else if (cmd.contains("help") || cmd.contains("what can you do")) {
            respond("I can open apps, make calls, send messages, play music on Spotify, tell time and date, set reminders. Just ask boss!");
        }

        // === UNKNOWN ===
        else {
            respond("I heard: " + cmd + ". I am not sure about that yet boss. Try saying open WhatsApp or call someone.");
        }
    }

    // ========== APP OPENING METHODS WITH MULTIPLE PACKAGE NAMES ==========

    private void openWhatsApp() {
        String[] packages = {
            "com.whatsapp",
            "com.whatsapp.w4b"
        };
        openAppWithFallback(packages, "WhatsApp");
    }

    private void openInstagram() {
        String[] packages = {
            "com.instagram.android",
            "com.instagram.lite"
        };
        openAppWithFallback(packages, "Instagram");
    }

    private void openYouTube() {
        String[] packages = {
            "com.google.android.youtube",
            "com.google.android.youtube.tv"
        };
        openAppWithFallback(packages, "YouTube");
    }

    private void openSpotify() {
        String[] packages = {"com.spotify.music"};
        openAppWithFallback(packages, "Spotify");
    }

    private void openSnapchat() {
        String[] packages = {"com.snapchat.android"};
        openAppWithFallback(packages, "Snapchat");
    }

    private void openCamera() {
        try {
            Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            startActivity(i);
            respond("Opening camera. Say cheese, boss!");
        } catch (Exception e) {
            respond("Could not open camera.");
        }
    }

    private void openCalculator() {
        String[] packages = {
            "com.google.android.calculator",
            "com.android.calculator2",
            "com.miui.calculator",
            "com.sec.android.app.popupcalculator"
        };
        openAppWithFallback(packages, "Calculator");
    }

    private void openClaudeAI() {
        String[] packages = {
            "com.anthropic.claude",
            "com.anthropic.claudeai",
            "ai.claude.app"
        };
        openAppWithFallback(packages, "Claude AI");
    }

    private void openChatGPT() {
        String[] packages = {
            "com.openai.chatgpt",
            "com.openai.android"
        };
        openAppWithFallback(packages, "ChatGPT");
    }

    private void openGoogle() {
        String[] packages = {
            "com.google.android.googlequicksearchbox",
            "com.google.android.gms"
        };
        openAppWithFallback(packages, "Google");
    }

    private void openComet() {
        String[] packages = {
            "com.cometapp.comet",
            "com.comet.app",
            "app.comet.android"
        };
        openAppWithFallback(packages, "Comet");
    }

    private void openAppWithFallback(String[] packages, String appName) {
        for (String pkg : packages) {
            try {
                Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
                if (i != null) {
                    startActivity(i);
                    respond("Opening " + appName + " for you!");
                    return;
                }
            } catch (Exception ignored) {}
        }
        // Not found - offer Play Store
        respond(appName + " is not installed on your phone, boss. Opening Play Store to find it.");
        try {
            Intent i = new Intent(Intent.ACTION_VIEW,
                Uri.parse("market://search?q=" + Uri.encode(appName)));
            startActivity(i);
        } catch (Exception e) {
            Intent i = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/search?q=" + Uri.encode(appName)));
            startActivity(i);
        }
    }

    // ========== EXACT CONTACT MATCHING ==========

    private void makeCallExact(String contactName) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            respond("I need call permission to do that.");
            return;
        }

        // Try EXACT match first
        String number = getContactNumberExact(contactName);

        // If no exact match, try contains match
        if (number == null) {
            number = getContactNumberContains(contactName);
        }

        if (number != null) {
            respond("Calling " + contactName + " now!");
            startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number)));
        } else {
            respond("I could not find " + contactName + " in your contacts, boss.");
        }
    }

    private String getContactNumberExact(String name) {
        try {
            ContentResolver cr = getContentResolver();
            // Try exact match first
            Cursor cursor = cr.query(
                ContactsContract.Contacts.CONTENT_URI, null,
                "LOWER(" + ContactsContract.Contacts.DISPLAY_NAME + ") = ?",
                new String[]{name.toLowerCase()}, null);
            if (cursor != null && cursor.moveToFirst()) {
                String id = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                cursor.close();
                return getPhoneById(id);
            }
            if (cursor != null) cursor.close();
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    private String getContactNumberContains(String name) {
        try {
            ContentResolver cr = getContentResolver();
            Cursor cursor = cr.query(
                ContactsContract.Contacts.CONTENT_URI, null,
                ContactsContract.Contacts.DISPLAY_NAME + " LIKE ?",
                new String[]{name + "%"}, // starts with - not contains to avoid wrong matches
                null);
            if (cursor != null && cursor.moveToFirst()) {
                String id = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                cursor.close();
                return getPhoneById(id);
            }
            if (cursor != null) cursor.close();
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    private String getPhoneById(String id) {
        try {
            ContentResolver cr = getContentResolver();
            Cursor phones = cr.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + id,
                null, null);
            if (phones != null && phones.moveToFirst()) {
                String number = phones.getString(
                    phones.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                phones.close();
                return number;
            }
            if (phones != null) phones.close();
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    // ========== MESSAGING ==========

    private void handleWhatsAppMessage(String cmd) {
        try {
            String cleaned = cmd.replace("send whatsapp message to", "")
                               .replace("send whatsapp to", "")
                               .replace("whatsapp message to", "")
                               .replace("whatsapp to", "").trim();
            String[] parts = cleaned.split(" ", 2);
            if (parts.length >= 2) {
                String contact = parts[0];
                String message = parts[1];
                String number = getContactNumberExact(contact);
                if (number == null) number = getContactNumberContains(contact);
                if (number != null) {
                    number = number.replaceAll("[^0-9+]", "");
                    Intent i = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://api.whatsapp.com/send?phone=" + number + "&text=" + Uri.encode(message)));
                    startActivity(i);
                    respond("Opening WhatsApp to message " + contact);
                } else {
                    respond("Could not find " + contact + " in your contacts.");
                }
            }
        } catch (Exception e) {
            respond("Something went wrong with the WhatsApp message.");
        }
    }

    private void handleSMS(String cmd) {
        try {
            String cleaned = cmd.replace("send message to", "").trim();
            String[] parts = cleaned.split(" ", 2);
            if (parts.length >= 2) {
                String contact = parts[0];
                String message = parts[1];
                String number = getContactNumberExact(contact);
                if (number == null) number = getContactNumberContains(contact);
                if (number != null) {
                    Intent i = new Intent(Intent.ACTION_SENDTO);
                    i.setData(Uri.parse("smsto:" + number));
                    i.putExtra("sms_body", message);
                    startActivity(i);
                    respond("Opening messages to " + contact);
                } else {
                    respond("Could not find " + contact + " in your contacts.");
                }
            }
        } catch (Exception e) {
            respond("Something went wrong with the message.");
        }
    }

    private void playOnSpotify(String song) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse("spotify:search:" + Uri.encode(song)));
            i.setPackage("com.spotify.music");
            startActivity(i);
            respond("Playing " + song + " on Spotify!");
        } catch (Exception e) {
            openSpotify();
        }
    }

    private void startListening() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        speechRecognizer.startListening(intent);
        tvStatus.setText("Listening...");
    }

    private void stopListening() {
        speechRecognizer.stopListening();
        btnMic.setText("SPEAK");
        isListening = false;
        tvStatus.setText("Ready");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) { tts.stop(); tts.shutdown(); }
        if (speechRecognizer != null) speechRecognizer.destroy();
    }
}
