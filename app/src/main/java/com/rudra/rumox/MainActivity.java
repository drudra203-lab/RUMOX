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
                respond("Always listen mode activated. I'm always here, boss.");
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
        if (cmd.contains("whatsapp") || cmd.contains("whats") || cmd.contains(" wa ")) {
            if (cmd.contains("message") || cmd.contains("send")) {
                handleWhatsAppMessage(cmd);
            } else {
                openApp("com.whatsapp", "WhatsApp");
            }
        }
        // === INSTAGRAM ===
        else if (cmd.contains("instagram") || cmd.contains("insta") || cmd.contains(" ig ")) {
            openApp("com.instagram.android", "Instagram");
        }
        // === YOUTUBE ===
        else if (cmd.contains("youtube") || cmd.contains(" yt ")) {
            openApp("com.google.android.youtube", "YouTube");
        }
        // === SPOTIFY ===
        else if (cmd.contains("spotify") || (cmd.contains("play") && cmd.contains("music"))) {
            if (cmd.contains("play") && !cmd.contains("open")) {
                String song = cmd.replace("play", "").replace("on spotify", "")
                               .replace("spotify", "").trim();
                playOnSpotify(song);
            } else {
                openApp("com.spotify.music", "Spotify");
            }
        }
        // === SNAPCHAT ===
        else if (cmd.contains("snapchat") || cmd.contains("snap")) {
            openApp("com.snapchat.android", "Snapchat");
        }
        // === CAMERA ===
        else if (cmd.contains("camera") || cmd.contains("cam")) {
            try {
                Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivity(i);
                respond("Opening camera. Say cheese, boss!");
            } catch (Exception e) {
                openApp("com.android.camera", "Camera");
            }
        }
        // === SETTINGS ===
        else if (cmd.contains("settings")) {
            startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
            respond("Opening settings for you.");
        }
        // === CALCULATOR ===
        else if (cmd.contains("calculator") || cmd.contains("calc")) {
            if (!openAppSilent("com.google.android.calculator"))
                if (!openAppSilent("com.android.calculator2"))
                    openApp("com.miui.calculator", "Calculator");
            respond("Opening calculator.");
        }
        // === CLAUDE AI ===
        else if (cmd.contains("claude")) {
            if (!openAppSilent("com.anthropic.claude"))
                openApp("com.anthropic.claudeai", "Claude AI");
            respond("Opening Claude AI.");
        }
        // === CHATGPT ===
        else if (cmd.contains("chatgpt") || cmd.contains("gpt") || cmd.contains("ai chat")) {
            if (!openAppSilent("com.openai.chatgpt"))
                openApp("com.openai.chatgpt", "ChatGPT");
            respond("Opening ChatGPT.");
        }
        // === GOOGLE ===
        else if (cmd.contains("open google") || cmd.contains("google search")) {
            if (!openAppSilent("com.google.android.googlequicksearchbox"))
                openApp("com.google.android.gm", "Google");
            respond("Opening Google.");
        }
        // === COMET ===
        else if (cmd.contains("comet")) {
            openApp("com.cometapp.comet", "Comet");
        }
        // === TELEGRAM ===
        else if (cmd.contains("telegram")) {
            openApp("org.telegram.messenger", "Telegram");
        }
        // === NETFLIX ===
        else if (cmd.contains("netflix")) {
            openApp("com.netflix.mediaclient", "Netflix");
        }
        // === GMAIL ===
        else if (cmd.contains("gmail")) {
            openApp("com.google.android.gm", "Gmail");
        }
        // === MAPS ===
        else if (cmd.contains("maps") || cmd.contains("navigation")) {
            openApp("com.google.android.apps.maps", "Google Maps");
        }
        // === PLAY STORE ===
        else if (cmd.contains("play store")) {
            openApp("com.android.vending", "Play Store");
        }

        // === CALLS ===
        else if (cmd.contains("call ") || cmd.startsWith("call")) {
            String contact = cmd.replaceAll("(?i)(call to|call|please|now)", "").trim();
            if (!contact.isEmpty()) makeCall(contact);
            else respond("Who should I call, boss?");
        }

        // === ANSWER CALL ===
        else if (cmd.contains("answer") || cmd.contains("pick up") || cmd.contains("accept call")) {
            respond("Answering the call!");
            Intent i = new Intent(Intent.ACTION_MEDIA_BUTTON);
            i.putExtra(Intent.EXTRA_KEY_EVENT,
                new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN,
                    android.view.KeyEvent.KEYCODE_HEADSETHOOK));
            sendOrderedBroadcast(i, null);
        }

        // === REJECT CALL ===
        else if (cmd.contains("reject") || cmd.contains("hang up") || cmd.contains("decline")) {
            respond("Call rejected.");
            Intent i = new Intent(Intent.ACTION_MEDIA_BUTTON);
            i.putExtra(Intent.EXTRA_KEY_EVENT,
                new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN,
                    android.view.KeyEvent.KEYCODE_HEADSETHOOK));
            sendOrderedBroadcast(i, null);
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
            respond("It's " + time + " boss.");
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
        else if (cmd.contains("note") || cmd.contains("take a note")) {
            String note = cmd.replace("take a note", "").replace("note", "").trim();
            respond("Got it! Note saved: " + note);
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
            respond("I am RUMOX, your personal AI assistant. Built to serve, boss.");
        }
        else if (cmd.contains("joke")) {
            respond("Why don't scientists trust atoms? Because they make up everything! Just like my confidence, boss.");
        }
        else if (cmd.contains("thank")) {
            respond("Always a pleasure, boss. That's what I'm here for.");
        }
        else if (cmd.contains("bye") || cmd.contains("goodbye")) {
            respond("Goodbye boss. RUMOX standing by whenever you need me.");
        }
        else if (cmd.contains("what can you do") || cmd.contains("help")) {
            respond("I can open apps, make calls, send messages, play music, tell time, set reminders and much more. Just ask, boss!");
        }

        // === UNKNOWN ===
        else {
            respond("I heard: " + cmd + ". I'm not sure how to help with that yet, boss. Try saying open WhatsApp or call someone.");
        }
    }

    private void openApp(String pkg, String name) {
        try {
            Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
            if (i != null) {
                startActivity(i);
                respond("Opening " + name + " for you!");
            } else {
                respond(name + " is not installed. Want me to search the Play Store?");
            }
        } catch (Exception e) {
            respond("Couldn't open " + name + ".");
        }
    }

    private boolean openAppSilent(String pkg) {
        try {
            Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
            if (i != null) { startActivity(i); return true; }
        } catch (Exception ignored) {}
        return false;
    }

    private void makeCall(String contactName) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            respond("I need call permission to do that.");
            return;
        }
        String number = getContactNumber(contactName);
        if (number != null) {
            respond("Calling " + contactName + " now!");
            startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number)));
        } else {
            respond("I couldn't find " + contactName + " in your contacts, boss.");
        }
    }

    private String getContactNumber(String name) {
        try {
            ContentResolver cr = getContentResolver();
            Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null,
                ContactsContract.Contacts.DISPLAY_NAME + " LIKE ?",
                new String[]{"%" + name + "%"}, null);
            if (cursor != null && cursor.moveToFirst()) {
                String id = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                cursor.close();
                Cursor phones = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + id, null, null);
                if (phones != null && phones.moveToFirst()) {
                    String number = phones.getString(
                        phones.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    phones.close();
                    return number;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    private void handleWhatsAppMessage(String cmd) {
        try {
            String cleaned = cmd.replace("send whatsapp message to", "")
                               .replace("whatsapp message to", "")
                               .replace("send message to", "").trim();
            String[] parts = cleaned.split(" ", 2);
            if (parts.length >= 2) {
                String contact = parts[0];
                String message = parts[1];
                String number = getContactNumber(contact);
                if (number != null) {
                    number = number.replaceAll("[^0-9+]", "");
                    Intent i = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://api.whatsapp.com/send?phone=" + number + "&text=" + Uri.encode(message)));
                    startActivity(i);
                    respond("Opening WhatsApp to message " + contact + "!");
                } else {
                    respond("Couldn't find " + contact + " in your contacts.");
                }
            }
        } catch (Exception e) {
            respond("Something went wrong with the message.");
        }
    }

    private void handleSMS(String cmd) {
        try {
            String cleaned = cmd.replace("send message to", "").trim();
            String[] parts = cleaned.split(" ", 2);
            if (parts.length >= 2) {
                String contact = parts[0];
                String message = parts[1];
                String number = getContactNumber(contact);
                if (number != null) {
                    Intent i = new Intent(Intent.ACTION_SENDTO);
                    i.setData(Uri.parse("smsto:" + number));
                    i.putExtra("sms_body", message);
                    startActivity(i);
                    respond("Opening messages to " + contact + "!");
                } else {
                    respond("Couldn't find " + contact + " in your contacts.");
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
            openApp("com.spotify.music", "Spotify");
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
