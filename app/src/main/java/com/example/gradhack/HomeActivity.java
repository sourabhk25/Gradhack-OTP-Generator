package com.example.gradhack;

import android.app.VoiceInteractor;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class HomeActivity extends AppCompatActivity {

    private TextView otp;
    private TextToSpeech tts;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_home);
        otp = (TextView) findViewById(R.id.otp);
        //initAndSpeak("Welcome Sourabh !!");
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                callPasscodeAPI();
            }
        };
        timer.scheduleAtFixedRate(task,0, 10000);
    }

    protected void callPasscodeAPI(){
        String url = "http://35.240.203.4:5000/getpasscode";
        String passcode;
        final RequestQueue requestQueue = Volley.newRequestQueue(this);
        Map<String, String> params = new HashMap();
        params.put("USERID", "5629291");
        JSONObject parameters = new JSONObject(params);

        final JsonObjectRequest objectRequest = new JsonObjectRequest(

                Request.Method.POST,
                url,
                parameters,
                new Response.Listener<JSONObject>(){
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            Log.e("Rest response", response.toString());
                            String passcode = response.get("PASSCODE").toString();
                            Log.e("passcode", passcode);
                            String s="";
                            for(int i=0;i<passcode.length();i++){
                                s += passcode.charAt(i) + " ";
                            }
                            Log.e("s",s);
                            initAndSpeak("OTP is "+s);
                            otp.setText(passcode);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener(){
                    @Override
                    public void onErrorResponse(VolleyError error){
                        Log.e("Rest response",error.toString());
                    }
                }
        );
        requestQueue.add(objectRequest);

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

    @Override
    public void onPause(){
        if(tts != null){
            tts.stop();
            tts.shutdown();
        }
        super.onPause();
    }

    @Override
    public void onBackPressed(){
        this.finishAffinity();
    }
}
