package com.example.gradhack;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Locale;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class MainActivity extends AppCompatActivity {

    TextToSpeech tts;
    private TextView mHeadingLabel;
    private ImageView mFingerprintImage;
    private TextView mParaLabel;
    private Button morseButton;
    private Long lastDown,lastDuration;
    private String passwordMorseCode = "...-..-.-";   //Assuming user is already registered. Morse translates to SDK.
    private String temp;
    private int count = 0;
    private int morseLoginAttemptCount = 0;
    protected ArrayList<String> morseCode;
    private FingerprintManager fingerprintManager;
    private KeyguardManager keyguardManager;

    private KeyStore keyStore;
    private Cipher cipher;
    private String KEY_NAME = "AndroidKey";
    public Context context;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();
        mHeadingLabel = (TextView) findViewById(R.id.headingLabel);
        mFingerprintImage = (ImageView) findViewById(R.id.fingerprintImage);
        mParaLabel = (TextView) findViewById(R.id.paraLabel);
        morseButton = findViewById(R.id.morseButton);
        morseCode = new ArrayList<>();

        // Check 1: Android version should be greater or equal to Marshmallow
        // Check 2: Device has Fingerprint Scanner
        // Check 3: Have permission to use fingerprint scanner in the app
        // Check 4: Lock screen is secured with atleast 1 type of lock
        // Check 5: Atleast 1 Fingerprint is registered

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            fingerprintManager = (FingerprintManager) getSystemService(FINGERPRINT_SERVICE);
            keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);

            if(!fingerprintManager.isHardwareDetected()){

                mParaLabel.setText("Fingerprint Scanner not detected in Device");
                initAndSpeak("Fingerprint Scanner not detected in Device");
            } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED){

                mParaLabel.setText("Permission not granted to use Fingerprint Scanner");
                initAndSpeak("Permission not granted to use Fingerprint Scanner");
            } else if (!keyguardManager.isKeyguardSecure()){

                mParaLabel.setText("Add Lock to your Phone in Settings");
                initAndSpeak("Add Lock to your Phone in Settings");
            } else if (!fingerprintManager.hasEnrolledFingerprints()){

                mParaLabel.setText("You should add atleast 1 Fingerprint to use this Feature");
                initAndSpeak("You should add atleast 1 Fingerprint to use this Feature");
            } else {

                mFingerprintImage.setImageResource(R.drawable.fingerprint_icon);
                mParaLabel.setText("Place your Finger on Scanner to Access the App.");
                initAndSpeak("Welcome to OTP generator!! Authenticate using Fingerprint scanner or using Morse Code.");
                generateKey();

                if (cipherInit()){

                    FingerprintManager.CryptoObject cryptoObject = new FingerprintManager.CryptoObject(cipher);
                    FingerprintHandler fingerprintHandler = new FingerprintHandler(this);
                    fingerprintHandler.startAuth(fingerprintManager, cryptoObject);

                }
            }

        }

        //Getting Morse Code password as input
        inputMorseCode();       //Taking first input



    }

    public void initAndSpeak(final String message){
        //tts engine
        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR){
                    tts.setLanguage(Locale.US);
                    tts.speak(message,TextToSpeech.QUEUE_FLUSH,null);
                }
            }
        });
    }

    protected void inputMorseCode(){
        for(int i=0;i<9;i++){
            morseButton.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    String s;
                    if(event.getAction() == MotionEvent.ACTION_DOWN) {
                        lastDown = System.currentTimeMillis();
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        if(count<9){
                            count++;
                            lastDuration = System.currentTimeMillis() - lastDown;
                            //timeLabel.setText("Button pressed for " + Long.toString(lastDuration) + " ms");
                            if(lastDuration<=250){
                                s=".";
                            }else{
                                s="-";
                            }
                            morseCode.add(s);
                        }
                        if(count==9){
                            //morseCodeLabel.setText(morseCode.toString());
                            checkMorseCodePassword();

                        }
                        else {
                            Log.e("endMsg:","finishing MainActivity");
                            //finish();
                        }
                    }

                    return true;
                }
            });
        }
    }

    protected void checkMorseCodePassword(){
        temp = "";
        for(String str:morseCode){
            temp = temp + str;
        }
        if(passwordMorseCode.equals(temp)){
            morseLoginAttemptCount = 0;
            morseCode.clear();
            Intent intent = new Intent(MainActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        }else {
            initAndSpeak("Unauthorized User");
            morseLoginAttemptCount++;
            count = 0;
            Log.e(" morseCode before ",morseCode.toString());
            morseCode.clear();
            Log.e(" morseCode after ",morseCode.toString());
            if(morseLoginAttemptCount < 3){
                inputMorseCode();
            }else{
                initAndSpeak("Unauthorized User. Too many attempts. Try again Later. ");
                Log.e(" morseCode ","too many attempts,try again later");
                morseLoginAttemptCount = 0;
            }
        }
    }


    @TargetApi(Build.VERSION_CODES.M)
    private void generateKey() {

        try {

            keyStore = KeyStore.getInstance("AndroidKeyStore");
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");

            keyStore.load(null);
            keyGenerator.init(new
                    KeyGenParameterSpec.Builder(KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT |
                            KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(
                            KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());
            keyGenerator.generateKey();

        } catch (KeyStoreException | IOException | CertificateException
                | NoSuchAlgorithmException | InvalidAlgorithmParameterException
                | NoSuchProviderException e) {

            e.printStackTrace();

        }

    }

    @TargetApi(Build.VERSION_CODES.M)
    public boolean cipherInit() {
        try {
            cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException("Failed to get Cipher", e);
        }


        try {

            keyStore.load(null);

            SecretKey key = (SecretKey) keyStore.getKey(KEY_NAME,
                    null);

            cipher.init(Cipher.ENCRYPT_MODE, key);

            return true;

        } catch (KeyPermanentlyInvalidatedException e) {
            return false;
        } catch (KeyStoreException | CertificateException | UnrecoverableKeyException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to init Cipher", e);
        }

    }


    @Override
    public void onPause(){
        if(tts != null){
            tts.stop();
            tts.shutdown();
        }
        super.onPause();
    }

}
