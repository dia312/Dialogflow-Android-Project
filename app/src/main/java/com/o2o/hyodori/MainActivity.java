package com.o2o.hyodori;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.dialogflow.v2.DetectIntentResponse;
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.SessionName;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.cloud.dialogflow.v2.SessionsSettings;
import com.google.cloud.dialogflow.v2.TextInput;
import com.google.common.collect.Lists;
import com.o2o.hyodori.interfaces.DialogflowBotReply;
import com.o2o.hyodori.utils.SendMessageInBg;

import java.io.InputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements DialogflowBotReply {

    private TextView textView;
    private EditText editText;
    private Button button;

    //dialogFlow
    private SessionsClient sessionsClient; // 세션 클라이언트
    private SessionName sessionName; //세션 이름
    private String uuid = UUID.randomUUID().toString(); //식별자
    private String TAG = "mainactivity"; //Tag 명

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView=findViewById(R.id.text1);
        editText=findViewById(R.id.editText1);
        button=findViewById(R.id.button1);

        //button 클릭 리스너 등록
        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                String message = editText.getText().toString();
                if (!message.isEmpty()) {
                    //UI or something to do Task
                    editText.setText("");
                    //메시지를 담아 sendMessageToBot() 호출
                    sendMessageToBot(message);
                } else {
                    Toast.makeText(MainActivity.this, "보낼 내용을 입력해주세요.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        //dialogflowAgent key정보 Setup
        setUpBot();
    }

    private void setUpBot() {
        try {
            InputStream stream = this.getResources().openRawResource(R.raw.credential);
            GoogleCredentials credentials = GoogleCredentials.fromStream(stream)
                    .createScoped(Lists.newArrayList("https://www.googleapis.com/auth/cloud-platform"));
            String projectId = ((ServiceAccountCredentials) credentials).getProjectId();

            SessionsSettings.Builder settingsBuilder = SessionsSettings.newBuilder();
            SessionsSettings sessionsSettings = settingsBuilder.setCredentialsProvider(
                    FixedCredentialsProvider.create(credentials)).build();
            sessionsClient = SessionsClient.create(sessionsSettings);
            sessionName = SessionName.of(projectId, uuid);

            Log.d(TAG, "projectId : " + projectId);
        } catch (Exception e) {
            Log.d(TAG, "setUpBot: " + e.getMessage());
        }
    }

    //dialogflow로 message를 보내는 메서드
    private void sendMessageToBot(String message) {
        QueryInput input = QueryInput.newBuilder()
                .setText(TextInput.newBuilder().setText(message).setLanguageCode("en-US")).build();
        new SendMessageInBg(this, sessionName, sessionsClient, input).execute();
    }

    @Override
    public void callback(DetectIntentResponse returnResponse) {
        //dialogflowAgent와 통신 성공한 경우
        if (returnResponse != null) {
            String dialogflowBotReply = returnResponse.getQueryResult().getFulfillmentText();
            Log.d("text", returnResponse.getQueryResult().toString());
            //getFulfillmentText가 있을 경우
            if (!dialogflowBotReply.isEmpty()) {
                //UI or something to do Task
                textView.setText(dialogflowBotReply);
            }
            else {
                Toast.makeText(this, "something went wrong", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            Toast.makeText(this, "failed to connect!", Toast.LENGTH_SHORT).show();
        }
    }
}