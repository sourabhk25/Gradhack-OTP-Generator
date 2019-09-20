package com.example.gradhack;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.Locale;


@TargetApi(Build.VERSION_CODES.M)
public class FingerprintHandler extends FingerprintManager.AuthenticationCallback {

    private Context context;
    private TextToSpeech tts;

    public FingerprintHandler(Context context){
        this.context = context;
    }

    public void startAuth(FingerprintManager fingerprintManager, FingerprintManager.CryptoObject cryptoObject){
        CancellationSignal cancellationSignal = new CancellationSignal();
        fingerprintManager.authenticate(cryptoObject, cancellationSignal, 0, this, null);
    }

    @Override
    public void onAuthenticationError(int errorCode, CharSequence errString) {
        this.update("There was an Authentication Error. " + errString, false);
        initAndSpeak("There was an Authentication Error, "+ errString);
    }

    @Override
    public void onAuthenticationFailed() {
        this.update("Unauthorized User.", false);
        initAndSpeak("Unauthorized User");
    }

    @Override
    public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
        this.update("Error: " + helpString, false);
        initAndSpeak(helpString.toString());
    }

    @Override
    public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
        Intent intent = new Intent(context,HomeActivity.class);
        context.startActivity(intent);
    }


    private void update(String s, boolean b) {
        TextView paraLabel = (TextView) ((Activity)context).findViewById(R.id.paraLabel);
        ImageView imageView = (ImageView) ((Activity)context).findViewById(R.id.fingerprintImage);
        paraLabel.setText(s);

        if(b == false){
            paraLabel.setTextColor(Color.WHITE);
        } else {
            paraLabel.setTextColor(Color.WHITE);
            imageView.setImageResource(R.drawable.done);
        }

    }

    public void initAndSpeak(final String message){
        //tts engine
        tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR){
                    tts.setLanguage(Locale.US);
                    tts.speak(message,TextToSpeech.QUEUE_FLUSH,null);
                }
            }
        });
    }
}
