package com.o2o.hyodori;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements DialogflowBotReply {

    //ui component
    private Button basicButton;
    private Button followUpButton;
    private Button alramButton;
    private TextView scenarioTextView;
    private TextView textView;
    private TextView timerTextView;
    private EditText editText;
    private Button button;


    //dialogFlow
    private SessionsClient sessionsClient; // 세션 클라이언트
    private SessionName sessionName; //세션 이름
    private String uuid = UUID.randomUUID().toString(); //식별자
    private String TAG = "mainactivity"; //Tag 명


    private String command="default";

    private int cntTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        basicButton = findViewById(R.id.basicButton);
        followUpButton =findViewById(R.id.followUpButton);
        alramButton =findViewById(R.id.alramButton);
        scenarioTextView = findViewById(R.id.scenarioText);
        textView=findViewById(R.id.responseText);
        timerTextView=findViewById(R.id.timerText);
        editText=findViewById(R.id.editText1);
        button=findViewById(R.id.button1);

        //button 클릭 리스너 등록
        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                //command 를 default로 설정
                setCommand("default");

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
        basicButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                setCommand("basic");
                Toast.makeText(MainActivity.this, "오늘 날씨 어떠냐구 물어봅니다.", Toast.LENGTH_SHORT).show();
                scenarioTextView.setText("\"오늘 날씨 어때\"라고 보냄");
                sendMessageToBot("오늘 날씨 어때");
            }
        });
        followUpButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                setCommand("followup");
                Toast.makeText(MainActivity.this, "대화를 2번 이상 진행 합니다.", Toast.LENGTH_SHORT).show();
                scenarioTextView.setText("\"점심 추천해줘\"라고 보냄");
                sendMessageToBot("점심 추천해줘");
            }
        });
        alramButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Timer timer = new Timer();
                TimerTask timerTask = new TimerTask(){
                    public void run(){
                        timerTextView.setText(Integer.toString(cntTime--));
                        if(cntTime==0){
                            setCommand("alram");
                            //main Thread에서만 UI 작업을 할 수 있으므로 Handler설정
                            boolean handler = new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    //UI THREAD CODE HERE
                                    Toast.makeText(MainActivity.this, "따르릉~ 알람시간이 되었습니다. 알람 쿼리를 가져옵니다!", Toast.LENGTH_SHORT).show();
                                }
                            });
                            scenarioTextView.setText("\"알람 7시\"라고 보냄");
                            sendMessageToBot("알람 7시");
                            this.cancel();
                            timerTextView.setText("Timer Text");

                        }
                    }
                };
                timer.schedule(timerTask,0,1000);
                cntTime = 5;
                Toast.makeText(MainActivity.this, "임의로 5초뒤로 알람을 세팅합니다.", Toast.LENGTH_SHORT).show();
            }
        });

        //dialogflowAgent key정보 Setup
        setUpBot();
    }

    //credential(GoogleService 자격 증명서) 파일을 통해 session 설정
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
    public void callback(DetectIntentResponse returnResponse){
        //dialogflowAgent와 통신 성공한 경우
        if (returnResponse != null) {
            String dialogflowBotReply = returnResponse.getQueryResult().getFulfillmentText();
            Log.d("text", returnResponse.getQueryResult().toString());
            //getFulfillmentText가 있을 경우
            if (!dialogflowBotReply.isEmpty()) {
                //UI or something to do Task
                switch(command){
                    case "basic":
                        scenarioTextView.setText(scenarioTextView.getText()+"\n답장: "+dialogflowBotReply);
                        break;
                    case "followup":
                        scenarioTextView.setText(scenarioTextView.getText()+"\n답장: "+dialogflowBotReply);
                        scenarioTextView.setText(scenarioTextView.getText()+"\n\"중국집 메뉴 알려줘\"라고 보냄");
                        sendMessageToBot("중국집 메뉴 알려줘");
                        setCommand("followup2");
                        break;
                    case "followup2":
                        scenarioTextView.setText(scenarioTextView.getText()+"\n답장: "+dialogflowBotReply);
                        break;
                    case "alram":
                        scenarioTextView.setText(scenarioTextView.getText()+"\n답장: "+dialogflowBotReply);
                        break;
                    default:
                        //scenarioTextView.setText(scenarioTextView.getText()+"\n답장: "+dialogflowBotReply);
                        textView.setText(dialogflowBotReply);
                        break;
                }

            }
            else {
                Toast.makeText(this, "something went wrong", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            Toast.makeText(this, "failed to connect!", Toast.LENGTH_SHORT).show();
        }
    }

    public void setCommand(String command) {
        this.command = command;
    }

}