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
import android.view.View;
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

    private static final int PERMISSIONS_REQUEST = 100;
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

        requestPermissions();
        initTTS();
        initSpeechRecognizer();

        btnMic.setOnClickListener(v -> {
            if (!isListening) startListening();
            else stopListening();
        });

        btnAlwaysListen.setOnClickListener(v -> {
            alwaysListenMode = !alwaysListenMode;
            if (alwaysListenMode) {
                btnAlwaysListen.setText("🔴 Stop Always Listen");
                btnAlwaysListen.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFFFF4444));
                tvStatus.setText("Always Listen Mode ON");
                speak("Always listen mode activated. I'm always here, boss.");
                startListening();
            } else {
                btnAlwaysListen.setText("👂 Always Listen");
                btnAlwaysListen.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFF00E5FF));
                tvStatus.setText("Always Listen Mode OFF");
                stopListening();
                speak("Always listen mode deactivated.");
            }
        });

        speak("RUMOX online. How can I help you, boss?");
    }

    private void initTTS() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
                tts.setPitch(0.9f);
                tts.setSpeechRate(1.0f);
            }
        });
    }

    private void speak(String text) {
        tvAssistantResponse.setText("RUMOX: " + text);
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void initSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle p) {
                tvStatus.setText("🎤 Listening...");
                btnMic.setText("⏹ Stop");
                isListening = true;
            }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float v) {}
            @Override public void onBufferReceived(byte[] b) {}
            @Override public void onEndOfSpeech() {
                btnMic.setText("🎤 Speak");
                isListening = false;
                tvStatus.setText("Processing...");
            }
            @Override public void onError(int error) {
                btnMic.setText("🎤 Speak");
                isListening = false;
                tvStatus.setText("Ready");
                if (alwaysListenMode) {
                    handler.postDelayed(() -> startListening(), 1000);
                }
            }
            @Override public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String command = matches.get(0);
                    tvUserSpeech.setText("You: " + command);
                    handleCommand(command.toLowerCase().trim());
                }
                btnMic.setText("🎤 Speak");
                isListening = false;
                tvStatus.setText("Ready");
                if (alwaysListenMode) {
                    handler.postDelayed(() -> startListening(), 2000);
                }
            }
            @Override public void onPartialResults(Bundle partial) {
                ArrayList<String> p = partial.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION);
                if (p != null && !p.isEmpty()) tvUserSpeech.setText("You: " + p.get(0));
            }
            @Override public void onEvent(int e, Bundle b) {}
        });
    }

    private void handleCommand(String cmd) {
        // === APP OPENING ===
        if (cmd.contains("whatsapp")) {
            openApp("com.whatsapp"); speak("Opening WhatsApp, boss.");
        } else if (cmd.contains("instagram")) {
            openApp("com.instagram.android"); speak("Opening Instagram.");
        } else if (cmd.contains("spotify")) {
            openApp("com.spotify.music"); speak("Opening Spotify.");
        } else if (cmd.contains("youtube")) {
            openApp("com.google.android.youtube"); speak("Opening YouTube.");
        } else if (cmd.contains("camera")) {
            startActivity(new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE));
            speak("Opening camera. Say cheese!");
        } else if (cmd.contains("settings")) {
            startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
            speak("Opening settings.");
        } else if (cmd.contains("calculator")) {
            openApp("com.google.android.calculator"); speak("Opening calculator.");
        } else if (cmd.contains("snapchat")) {
            openApp("com.snapchat.android"); speak("Opening Snapchat.");
        } else if (cmd.contains("claude")) {
            openApp("com.anthropic.claude"); speak("Opening Claude AI.");
        } else if (cmd.contains("chatgpt") || cmd.contains("gpt")) {
            openApp("com.openai.chatgpt"); speak("Opening ChatGPT.");
        } else if (cmd.contains("google") && !cmd.contains("search")) {
            openApp("com.google.android.googlequicksearchbox"); speak("Opening Google.");
        } else if (cmd.contains("comet")) {
            openApp("com.comet.app"); speak("Opening Comet.");
        } else if (cmd.contains("telegram")) {
            openApp("org.telegram.messenger"); speak("Opening Telegram.");
        } else if (cmd.contains("netflix")) {
            openApp("com.netflix.mediaclient"); speak("Opening Netflix.");
        } else if (cmd.contains("play store")) {
            openApp("com.android.vending"); speak("Opening Play Store.");
        } else if (cmd.contains("gmail")) {
            openApp("com.google.android.gm"); speak("Opening Gmail.");
        } else if (cmd.contains("maps")) {
            openApp("com.google.android.apps.maps"); speak("Opening Maps.");
        }

        // === PHONE CALLS ===
        else if (cmd.contains("call ")) {
            String contact = cmd.replace("call", "").trim();
            makeCall(contact);
        }

        // === MESSAGING ===
        else if (cmd.contains("send whatsapp message to") || cmd.contains("whatsapp message to")) {
            handleWhatsAppMessage(cmd);
        } else if (cmd.contains("send message to")) {
            handleSMS(cmd);
        }

        // === MUSIC ===
        else if (cmd.contains("play") && cmd.contains("spotify")) {
            String song = cmd.replace("play", "").replace("on spotify", "").replace("spotify", "").trim();
            playOnSpotify(song);
        } else if (cmd.contains("play music")) {
            openApp("com.spotify.music"); speak("Playing music on Spotify.");
        }

        // === SEARCH ===
        else if (cmd.contains("search for") || cmd.contains("search ")) {
            String query = cmd.replace("search for", "").replace("search", "").trim();
            Intent i = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/search?q=" + Uri.encode(query)));
            startActivity(i);
            speak("Searching for " + query);
        }

        // === TIME & DATE ===
        else if (cmd.contains("what time") || cmd.contains("current time")) {
            String time = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
            speak("It's " + time + ", boss.");
        } else if (cmd.contains("what") && cmd.contains("date") || cmd.contains("today's date")) {
            String date = new SimpleDateFormat("EEEE, MMMM d yyyy", Locale.getDefault()).format(new Date());
            speak("Today is " + date);
        }

        // === REMINDERS ===
        else if (cmd.contains("set a reminder") || cmd.contains("remind me")) {
            speak("Setting a reminder for you, boss.");
            Intent i = new Intent(AlarmClock.ACTION_SET_ALARM);
            i.putExtra(AlarmClock.EXTRA_MESSAGE, cmd);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        }

        // === NOTES ===
        else if (cmd.contains("take a note") || cmd.contains("note")) {
            String note = cmd.replace("take a note", "").replace("note", "").trim();
            speak("Note saved: " + note);
            Toast.makeText(this, "Note: " + note, Toast.LENGTH_LONG).show();
        }

        // === JARVIS RESPONSES ===
        else if (cmd.contains("hello") || cmd.contains("hi")) {
            speak("Hello boss! Ready to assist you.");
        } else if (cmd.contains("how are you")) {
            speak("I'm fully operational and ready to serve, boss!");
        } else if (cmd.contains("your name") || cmd.contains("who are you")) {
            speak("I am RUMOX, your personal AI assistant. At your service.");
        } else if (cmd.contains("joke")) {
            speak("Why don't scientists trust atoms? Because they make up everything! Just like my confidence.");
        } else if (cmd.contains("thank")) {
            speak("Always a pleasure, boss.");
        } else if (cmd.contains("bye") || cmd.contains("goodbye")) {
            speak("Goodbye boss. RUMOX standing by.");
        }

        // === UNKNOWN ===
        else {
            speak("I heard you say: " + cmd + ". I'm still learning that command, boss.");
        }
    }

    private void openApp(String pkg) {
        try {
            Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
            if (i != null) startActivity(i);
            else speak("That app is not installed on your phone, boss.");
        } catch (Exception e) {
            speak("Couldn't open that app.");
        }
    }

    private void makeCall(String contactName) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            speak("I need permission to make calls.");
            return;
        }
        String number = getContactNumber(contactName);
        if (number != null) {
            speak("Calling " + contactName);
            Intent i = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number));
            startActivity(i);
        } else {
            speak("I couldn't find " + contactName + " in your contacts.");
        }
    }

    private String getContactNumber(String name) {
        ContentResolver cr = getContentResolver();
        Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null,
            ContactsContract.Contacts.DISPLAY_NAME + " LIKE ?",
            new String[]{"%" + name + "%"}, null);
        if (cursor != null && cursor.moveToFirst()) {
            String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
            cursor.close();
            Cursor phones = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + id, null, null);
            if (phones != null && phones.moveToFirst()) {
                String number = phones.getString(
                    phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                phones.close();
                return number;
            }
        }
        return null;
    }

    private void handleWhatsAppMessage(String cmd) {
        String cleaned = cmd.replace("send whatsapp message to", "")
                           .replace("whatsapp message to", "").trim();
        String[] parts = cleaned.split(" ", 2);
        if (parts.length >= 2) {
            String contact = parts[0];
            String message = parts[1];
            String number = getContactNumber(contact);
            if (number != null) {
                number = number.replaceAll("[^0-9]", "");
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("https://api.whatsapp.com/send?phone=" + number + "&text=" + Uri.encode(message)));
                startActivity(i);
                speak("Opening WhatsApp to message " + contact);
            } else {
                speak("Couldn't find " + contact + " in contacts.");
            }
        }
    }

    private void handleSMS(String cmd) {
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
                speak("Opening messages to " + contact);
            } else {
                speak("Couldn't find " + contact + " in contacts.");
            }
        }
    }

    private void playOnSpotify(String song) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse("spotify:search:" + song));
            i.setPackage("com.spotify.music");
            startActivity(i);
            speak("Playing " + song + " on Spotify.");
        } catch (Exception e) {
            openApp("com.spotify.music");
            speak("Opening Spotify for you.");
        }
    }

    private void startListening() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        speechRecognizer.startListening(intent);
        tvStatus.setText("🎤 Listening...");
    }

    private void stopListening() {
        speechRecognizer.stopListening();
        btnMic.setText("🎤 Speak");
        isListening = false;
        tvStatus.setText("Ready");
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSIONS_REQUEST);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) { tts.stop(); tts.shutdown(); }
        if (speechRecognizer != null) speechRecognizer.destroy();
    }
}
