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
import android.telecom.TelecomManager;
import android.view.KeyEvent;
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
        Manifest.permission.SEND_SMS,
        Manifest.permission.ANSWER_PHONE_CALLS,
        Manifest.permission.READ_PHONE_STATE
    };

    // ===== EXACT PACKAGE NAMES FROM YOUR PHONE =====
    private void openWhatsApp() {
        if (!tryOpen("com.whatsapp")) tryOpen("com.whatsapp.w4b");
        else return;
        respond("Opening WhatsApp for you boss!");
    }

    private void openInstagram() {
        tryOpenAndRespond("com.instagram.android", "Instagram");
    }

    private void openYouTube() {
        tryOpenAndRespond("com.google.android.youtube", "YouTube");
    }

    private void openSpotify() {
        tryOpenAndRespond("com.spotify.music", "Spotify");
    }

    private void openSnapchat() {
        if (!tryOpen("com.snapchat.android")) {
            respond("Snapchat is not installed boss.");
        } else {
            respond("Opening Snapchat!");
        }
    }

    private void openTelegram() {
        tryOpenAndRespond("org.telegram.messenger", "Telegram");
    }

    private void openChrome() {
        tryOpenAndRespond("com.android.chrome", "Chrome");
    }

    private void openGoogle() {
        tryOpenAndRespond("com.google.android.googlequicksearchbox", "Google");
    }

    private void openGmail() {
        tryOpenAndRespond("com.google.android.gm", "Gmail");
    }

    private void openMaps() {
        tryOpenAndRespond("com.google.android.apps.maps", "Google Maps");
    }

    private void openYouTubeMusic() {
        tryOpenAndRespond("com.google.android.apps.youtube.music", "YouTube Music");
    }

    private void openCalculator() {
        if (!tryOpen("com.coloros.calculator"))
            tryOpen("com.android.calculator2");
        respond("Opening Calculator!");
    }

    private void openCamera() {
        try {
            Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            startActivity(i);
            respond("Opening Camera. Say cheese boss!");
        } catch (Exception e) {
            respond("Could not open camera.");
        }
    }

    private void openPlayStore() {
        tryOpenAndRespond("com.android.vending", "Play Store");
    }

    private void openPhone() {
        tryOpenAndRespond("com.google.android.dialer", "Phone");
    }

    private void openMessages() {
        tryOpenAndRespond("com.google.android.apps.messaging", "Messages");
    }

    private void openPhotos() {
        if (!tryOpen("com.google.android.apps.photos"))
            tryOpen("com.coloros.gallery3d");
        respond("Opening Photos!");
    }

    private void openFiles() {
        if (!tryOpen("com.coloros.filemanager"))
            tryOpen("com.google.android.documentsui");
        respond("Opening Files!");
    }

    private void openGemini() {
        tryOpenAndRespond("com.google.android.apps.bard", "Gemini AI");
    }

    private void openPhonePe() {
        tryOpenAndRespond("com.phonepe.app", "PhonePe");
    }

    private void openGPay() {
        tryOpenAndRespond("com.google.android.apps.nbu.paisa.user", "Google Pay");
    }

    private void openUber() {
        tryOpenAndRespond("com.ubercab", "Uber");
    }

    private void openRapido() {
        tryOpenAndRespond("com.rapido.passenger", "Rapido");
    }

    private void openHotstar() {
        tryOpenAndRespond("in.startv.hotstar", "JioHotstar");
    }

    private void openMXPlayer() {
        tryOpenAndRespond("com.mxtech.videoplayer.ad", "MX Player");
    }

    private void openMusic() {
        if (!tryOpen("com.heytap.music"))
            if (!tryOpen("com.spotify.music"))
                tryOpen("com.google.android.apps.youtube.music");
        respond("Opening Music!");
    }

    private boolean tryOpen(String pkg) {
        try {
            Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
            if (i != null) {
                startActivity(i);
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private void tryOpenAndRespond(String pkg, String name) {
        if (tryOpen(pkg)) {
            respond("Opening " + name + " for you boss!");
        } else {
            respond(name + " is not installed boss. Opening Play Store.");
            try {
                Intent i = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://search?q=" + Uri.encode(name)));
                startActivity(i);
            } catch (Exception e) {
                Intent i = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/search?q=" + Uri.encode(name)));
                startActivity(i);
            }
        }
    }

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
                btnAlwaysListen.setText("■ STOP LISTENING");
                btnAlwaysListen.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFFFF4444));
                tvStatus.setText("● Always Listen ON");
                respond("Always listen mode activated. I am always here boss.");
                startListening();
            } else {
                btnAlwaysListen.setText("● ALWAYS LISTEN");
                btnAlwaysListen.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFF00E5FF));
                tvStatus.setText("Ready");
                stopListening();
                respond("Always listen mode deactivated.");
            }
        });

        respond("RUMOX online. How can I help you boss?");
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
        runOnUiThread(() -> tvAssistantResponse.setText("RUMOX: " + text));
        if (tts != null) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    private void initSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle p) {
                runOnUiThread(() -> {
                    tvStatus.setText("● Listening...");
                    btnMic.setText("■ STOP");
                });
                isListening = true;
            }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float v) {}
            @Override public void onBufferReceived(byte[] b) {}
            @Override public void onEndOfSpeech() {
                runOnUiThread(() -> {
                    btnMic.setText("● SPEAK");
                    tvStatus.setText("Processing...");
                });
                isListening = false;
            }
            @Override public void onError(int error) {
                runOnUiThread(() -> {
                    btnMic.setText("● SPEAK");
                    tvStatus.setText("Ready");
                });
                isListening = false;
                if (alwaysListenMode) handler.postDelayed(() -> startListening(), 1500);
            }
            @Override public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String cmd = matches.get(0);
                    runOnUiThread(() -> tvUserSpeech.setText("You: " + cmd));
                    handleCommand(cmd.toLowerCase().trim());
                }
                runOnUiThread(() -> {
                    btnMic.setText("● SPEAK");
                    tvStatus.setText("Ready");
                });
                isListening = false;
                if (alwaysListenMode) handler.postDelayed(() -> startListening(), 2000);
            }
            @Override public void onPartialResults(Bundle partial) {
                ArrayList<String> p = partial.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (p != null && !p.isEmpty()) {
                    runOnUiThread(() -> tvUserSpeech.setText("You: " + p.get(0)));
                }
            }
            @Override public void onEvent(int e, Bundle b) {}
        });
    }

    private void handleCommand(String cmd) {

        // === WAKE WORD CHECK - buddy ===
        boolean hasBuddy = cmd.contains("buddy");
        if (hasBuddy) {
            cmd = cmd.replace("buddy", "").trim();
        }

        // === ANSWER CALL ===
        if (cmd.contains("answer") || cmd.contains("pick up") || cmd.contains("pick the call")
                || cmd.contains("accept call") || cmd.contains("answer the call")) {
            answerCall();
            return;
        }

        // === REJECT CALL ===
        if (cmd.contains("reject") || cmd.contains("hang up") || cmd.contains("decline")
                || cmd.contains("end call")) {
            rejectCall();
            return;
        }

        // === WEATHER ===
        if (cmd.contains("weather")) {
            Intent i = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/search?q=weather+today"));
            startActivity(i);
            respond("Opening weather for you boss!");
            return;
        }

        // === WHATSAPP MESSAGE ===
        if ((cmd.contains("whatsapp") || cmd.contains("whats app")) &&
            (cmd.contains("message") || cmd.contains("send") || cmd.contains("msg"))) {
            handleWhatsAppMessage(cmd);
            return;
        }

        // === OPEN APPS ===
        if (cmd.contains("open ") || cmd.contains("launch ") || cmd.contains("start ")) {

            if (cmd.contains("whatsapp") || cmd.contains("whats app")) { openWhatsApp(); }
            else if (cmd.contains("instagram") || cmd.contains("insta")) { openInstagram(); }
            else if (cmd.contains("youtube") || cmd.contains("you tube")) { openYouTube(); }
            else if (cmd.contains("spotify")) { openSpotify(); }
            else if (cmd.contains("snapchat") || cmd.contains("snap")) { openSnapchat(); }
            else if (cmd.contains("telegram")) { openTelegram(); }
            else if (cmd.contains("chrome")) { openChrome(); }
            else if (cmd.contains("google")) { openGoogle(); }
            else if (cmd.contains("gmail")) { openGmail(); }
            else if (cmd.contains("maps") || cmd.contains("navigation")) { openMaps(); }
            else if (cmd.contains("calculator") || cmd.contains("calc")) { openCalculator(); }
            else if (cmd.contains("camera") || cmd.contains("cam")) { openCamera(); }
            else if (cmd.contains("play store")) { openPlayStore(); }
            else if (cmd.contains("phone") || cmd.contains("dialer")) { openPhone(); }
            else if (cmd.contains("messages") || cmd.contains("sms")) { openMessages(); }
            else if (cmd.contains("photos") || cmd.contains("gallery")) { openPhotos(); }
            else if (cmd.contains("files") || cmd.contains("file manager")) { openFiles(); }
            else if (cmd.contains("gemini")) { openGemini(); }
            else if (cmd.contains("phonepe") || cmd.contains("phone pe")) { openPhonePe(); }
            else if (cmd.contains("gpay") || cmd.contains("google pay")) { openGPay(); }
            else if (cmd.contains("uber")) { openUber(); }
            else if (cmd.contains("rapido")) { openRapido(); }
            else if (cmd.contains("hotstar") || cmd.contains("jio")) { openHotstar(); }
            else if (cmd.contains("mx player") || cmd.contains("mx")) { openMXPlayer(); }
            else if (cmd.contains("music")) { openMusic(); }
            else if (cmd.contains("settings")) {
                startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
                respond("Opening Settings!");
            }
            else if (cmd.contains("claude")) { openGemini(); }
            else if (cmd.contains("chatgpt") || cmd.contains("gpt")) { openChatGPT(); }
            else {
                respond("I could not find that app boss. Please check if it is installed.");
            }
            return;
        }

        // === WITHOUT "OPEN" KEYWORD ===
        if (cmd.contains("whatsapp") || cmd.contains("whats app")) { openWhatsApp(); return; }
        if (cmd.contains("instagram") || cmd.contains("insta")) { openInstagram(); return; }
        if (cmd.contains("youtube")) { openYouTube(); return; }
        if (cmd.contains("snapchat")) { openSnapchat(); return; }
        if (cmd.contains("telegram")) { openTelegram(); return; }
        if (cmd.contains("spotify")) { openSpotify(); return; }
        if (cmd.contains("netflix")) { tryOpenAndRespond("com.netflix.mediaclient", "Netflix"); return; }
        if (cmd.contains("gemini")) { openGemini(); return; }
        if (cmd.contains("chatgpt") || cmd.contains("gpt")) { openChatGPT(); return; }

        // === CALLS - EXACT MATCH ===
        if (cmd.startsWith("call ") || cmd.contains("call ")) {
            String contact = cmd
                .replaceFirst("(?i)call to\\s+", "")
                .replaceFirst("(?i)call\\s+", "")
                .replace("please", "")
                .replace("now", "")
                .trim();
            if (!contact.isEmpty()) makeCallExact(contact);
            else respond("Who should I call boss?");
            return;
        }

        // === PLAY MUSIC ===
        if (cmd.contains("play")) {
            if (cmd.contains("spotify")) {
                String song = cmd.replace("play", "").replace("on spotify", "")
                               .replace("spotify", "").trim();
                playOnSpotify(song.isEmpty() ? "top hits" : song);
            } else {
                openMusic();
            }
            return;
        }

        // === SEARCH ===
        if (cmd.contains("search for") || cmd.contains("search ")) {
            String query = cmd.replace("search for", "").replace("search", "").trim();
            Intent i = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/search?q=" + Uri.encode(query)));
            startActivity(i);
            respond("Searching for " + query);
            return;
        }

        // === SEND MESSAGE ===
        if (cmd.contains("send message to")) {
            handleSMS(cmd);
            return;
        }

        // === TIME ===
        if (cmd.contains("time") || cmd.contains("what time")) {
            String time = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
            respond("It is " + time + " boss.");
            return;
        }

        // === DATE ===
        if (cmd.contains("date") || cmd.contains("today")) {
            String date = new SimpleDateFormat("EEEE, MMMM d yyyy", Locale.getDefault()).format(new Date());
            respond("Today is " + date);
            return;
        }

        // === REMINDER ===
        if (cmd.contains("reminder") || cmd.contains("remind me")) {
            Intent i = new Intent(AlarmClock.ACTION_SET_ALARM);
            i.putExtra(AlarmClock.EXTRA_MESSAGE, cmd);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            respond("Setting a reminder for you boss.");
            return;
        }

        // === NOTE ===
        if (cmd.contains("take a note") || cmd.contains("note")) {
            String note = cmd.replace("take a note", "").replace("note", "").trim();
            respond("Note saved: " + note);
            Toast.makeText(this, "Note: " + note, Toast.LENGTH_LONG).show();
            return;
        }

        // === GREETINGS ===
        if (cmd.contains("hello") || cmd.contains("hi") || cmd.contains("hey")) {
            respond("Hey boss! RUMOX is ready. What can I do for you?");
        } else if (cmd.contains("how are you")) {
            respond("Fully operational and ready to serve boss!");
        } else if (cmd.contains("who are you") || cmd.contains("your name")) {
            respond("I am RUMOX, your personal AI assistant. Built to serve you boss.");
        } else if (cmd.contains("joke")) {
            respond("Why do programmers prefer dark mode? Because light attracts bugs!");
        } else if (cmd.contains("thank")) {
            respond("Always a pleasure boss.");
        } else if (cmd.contains("bye") || cmd.contains("goodbye")) {
            respond("Goodbye boss. RUMOX standing by.");
        } else if (cmd.contains("help") || cmd.contains("what can you do")) {
            respond("I can open any app, make calls, send messages, play music, tell weather, time and date. Just ask boss!");
        } else {
            respond("I heard: " + cmd + ". Try saying open WhatsApp or call someone boss.");
        }
    }

    private void openChatGPT() {
        if (!tryOpen("com.openai.chatgpt")) {
            respond("ChatGPT app not installed. Opening Play Store.");
            tryOpenAndRespond("com.openai.chatgpt", "ChatGPT");
        } else {
            respond("Opening ChatGPT!");
        }
    }

    // ===== EXACT CONTACT MATCHING =====
    private void makeCallExact(String contactName) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            respond("I need call permission boss.");
            return;
        }

        // Step 1: Try EXACT match (case insensitive)
        String number = findContactExact(contactName);

        // Step 2: Try starts-with match only if no exact match
        if (number == null) {
            number = findContactStartsWith(contactName);
        }

        if (number != null) {
            respond("Calling " + contactName + " now boss!");
            startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number)));
        } else {
            respond("I could not find " + contactName + " in your contacts boss.");
        }
    }

    private String findContactExact(String name) {
        try {
            ContentResolver cr = getContentResolver();
            Cursor cursor = cr.query(
                ContactsContract.Contacts.CONTENT_URI, null,
                "LOWER(" + ContactsContract.Contacts.DISPLAY_NAME + ") = LOWER(?)",
                new String[]{name}, null);
            if (cursor != null && cursor.moveToFirst()) {
                String id = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                cursor.close();
                return getPhoneById(id);
            }
            if (cursor != null) cursor.close();
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    private String findContactStartsWith(String name) {
        try {
            ContentResolver cr = getContentResolver();
            // Only match if name STARTS WITH the search term - prevents wrong matches
            Cursor cursor = cr.query(
                ContactsContract.Contacts.CONTENT_URI, null,
                "LOWER(" + ContactsContract.Contacts.DISPLAY_NAME + ") LIKE LOWER(?)",
                new String[]{name + " %"}, // adds space after to prevent partial word matches
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

    // ===== ANSWER / REJECT CALL =====
    private void answerCall() {
        respond("Answering the call boss!");
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                TelecomManager tm = (TelecomManager) getSystemService(TELECOM_SERVICE);
                if (tm != null && ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
                    tm.acceptRingingCall();
                }
            }
        } catch (Exception e) {
            // Fallback using headset hook
            Intent i = new Intent(Intent.ACTION_MEDIA_BUTTON);
            i.putExtra(Intent.EXTRA_KEY_EVENT,
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HEADSETHOOK));
            sendOrderedBroadcast(i, null);
        }
    }

    private void rejectCall() {
        respond("Call rejected boss!");
        try {
            TelecomManager tm = (TelecomManager) getSystemService(TELECOM_SERVICE);
            if (tm != null && ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
                tm.endCall();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===== MESSAGING =====
    private void handleWhatsAppMessage(String cmd) {
        try {
            String cleaned = cmd
                .replace("send whatsapp message to", "")
                .replace("send whatsapp to", "")
                .replace("whatsapp message to", "")
                .replace("open whatsapp message to", "")
                .replace("whatsapp", "")
                .replace("message to", "")
                .replace("send", "")
                .trim();
            String[] parts = cleaned.split(" ", 2);
            if (parts.length >= 2) {
                String contact = parts[0];
                String message = parts[1];
                String number = findContactExact(contact);
                if (number == null) number = findContactStartsWith(contact);
                if (number != null) {
                    number = number.replaceAll("[^0-9+]", "");
                    Intent i = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://api.whatsapp.com/send?phone=" + number
                            + "&text=" + Uri.encode(message)));
                    startActivity(i);
                    respond("Opening WhatsApp to message " + contact + " boss!");
                } else {
                    respond("Could not find " + contact + " in contacts boss.");
                }
            } else {
                respond("Please say: send WhatsApp message to [name] [message]");
            }
        } catch (Exception e) {
            respond("Something went wrong with WhatsApp message.");
        }
    }

    private void handleSMS(String cmd) {
        try {
            String cleaned = cmd.replace("send message to", "").trim();
            String[] parts = cleaned.split(" ", 2);
            if (parts.length >= 2) {
                String contact = parts[0];
                String message = parts[1];
                String number = findContactExact(contact);
                if (number == null) number = findContactStartsWith(contact);
                if (number != null) {
                    Intent i = new Intent(Intent.ACTION_SENDTO);
                    i.setData(Uri.parse("smsto:" + number));
                    i.putExtra("sms_body", message);
                    startActivity(i);
                    respond("Opening messages to " + contact + " boss!");
                } else {
                    respond("Could not find " + contact + " in contacts boss.");
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
            respond("Playing " + song + " on Spotify boss!");
        } catch (Exception e) {
            openSpotify();
        }
    }

    private void startListening() {
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            speechRecognizer.startListening(intent);
            runOnUiThread(() -> tvStatus.setText("● Listening..."));
        } catch (Exception e) {
            if (alwaysListenMode) handler.postDelayed(() -> startListening(), 2000);
        }
    }

    private void stopListening() {
        try {
            speechRecognizer.stopListening();
        } catch (Exception ignored) {}
        runOnUiThread(() -> {
            btnMic.setText("● SPEAK");
            isListening = false;
            tvStatus.setText("Ready");
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        alwaysListenMode = false;
        if (tts != null) { tts.stop(); tts.shutdown(); }
        if (speechRecognizer != null) { speechRecognizer.destroy(); }
        handler.removeCallbacksAndMessages(null);
    }
}
