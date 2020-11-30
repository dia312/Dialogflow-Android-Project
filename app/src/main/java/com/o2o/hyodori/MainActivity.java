package com.o2o.hyodori;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
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
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements DialogflowBotReply {

    //ui component
    private Button basicButton;
    private Button followUpButton;
    private Button alramButton;
    private Button quizButton;

    private TextView scenarioTextView;
    private TextView textView;
    private TextView timerTextView;
    private EditText editText;
    private Button button;
    private LottieAnimationView voiceButton;

    //dialogFlow
    private SessionsClient sessionsClient; // 세션 클라이언트
    private SessionName sessionName; //세션 이름
    private String uuid = UUID.randomUUID().toString(); //식별자
    private String TAG = "mainactivity"; //Tag 명


    private String command = "default";

    private int cntTime = 0;

    final int PERMISSION = 1;
    RecognitionListener listener;
    SpeechRecognizer mRecognizer;
    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        basicButton = findViewById(R.id.basicButton);
        followUpButton = findViewById(R.id.followUpButton);
        alramButton = findViewById(R.id.alramButton);
        quizButton = findViewById(R.id.quizButton);
        scenarioTextView = findViewById(R.id.scenarioText);
        textView = findViewById(R.id.responseText);
        timerTextView = findViewById(R.id.timerText);
        editText = findViewById(R.id.editText1);
        button = findViewById(R.id.button1);
        voiceButton = findViewById(R.id.voice_btn);

        //퍼미션 체크( 인터넷, 오디오 )
        if (Build.VERSION.SDK_INT >= 23) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET,
                    Manifest.permission.RECORD_AUDIO}, PERMISSION);
        }

        //RecognizerIntent 객체 생성
        intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko_KR");


        //button 클릭 리스너 등록
        voiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                voiceButton.playAnimation();
                mRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
                mRecognizer.setRecognitionListener(listener);
                mRecognizer.startListening(intent);
            }
        });

        //listener 생성
        listener = new RecognitionListener() {

            @Override
            public void onRmsChanged(float rmsdB) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onResults(Bundle results) {
                // TODO Auto-generated method stub
                // 아래 코드는 음성인식된 결과를 ArrayList로 모아옵니다.
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String result="";
                // 이후 for문으로 textView에 setText로 음성인식된 결과를 수정해줍니다.
                for (int i = 0; i < matches.size(); i++) {
                    result=matches.get(i);
                }
                Toast.makeText(getApplicationContext(),result,Toast.LENGTH_SHORT).show();
                voiceButton.cancelAnimation();

                //command 를 default로 설정
                setCommand("default");
                if (!result.isEmpty()) {
                    //메시지를 담아 sendMessageToBot() 호출
                    sendMessageToBot(result);
                }
            }
                @Override
                public void onReadyForSpeech (Bundle params){
                    // TODO Auto-generated method stub

                }

                @Override
                public void onPartialResults (Bundle partialResults){
                    // TODO Auto-generated method stub

                }

                @Override
                public void onEvent ( int eventType, Bundle params){
                    // TODO Auto-generated method stub

                }

                @Override
                public void onError ( int error){
                    // TODO Auto-generated method stub
                    String message;

                    switch (error) {
                        case SpeechRecognizer.ERROR_AUDIO:
                            message = "오디오 에러";
                            break;
                        case SpeechRecognizer.ERROR_CLIENT:
                            message = "클라이언트 에러";
                            break;
                        case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                            message = "퍼미션 없음";
                            break;
                        case SpeechRecognizer.ERROR_NETWORK:
                            message = "네트워크 에러";
                            break;
                        case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                            message = "네트웍 타임아웃";
                            break;
                        case SpeechRecognizer.ERROR_NO_MATCH:
                            message = "찾을 수 없음";
                            break;
                        case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                            message = "RECOGNIZER가 바쁨";
                            break;
                        case SpeechRecognizer.ERROR_SERVER:
                            message = "서버가 이상함";
                            break;
                        case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                            message = "말하는 시간초과";
                            break;
                        default:
                            message = "알 수 없는 오류임";
                            break;
                    }

                    Toast.makeText(getApplicationContext(), "에러가 발생하였습니다. : " + message, Toast.LENGTH_SHORT).show();
                    voiceButton.cancelAnimation();

                }

                @Override
                public void onEndOfSpeech () {
                    // TODO Auto-generated method stub

                }

                @Override
                public void onBufferReceived ( byte[] buffer){
                    // TODO Auto-generated method stub

                }

                @Override
                public void onBeginningOfSpeech () {
                    // TODO Auto-generated method stub

                }
            };




        button.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick (View v){
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
        basicButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick (View v){
                setCommand("basic");
                Toast.makeText(MainActivity.this, "오늘 날씨 어떠냐구 물어봅니다.", Toast.LENGTH_SHORT).show();
                scenarioTextView.setText("\"오늘 날씨 어때\"라고 보냄");
                sendMessageToBot("오늘 날씨 어때");
            }
            });
        followUpButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick (View v){
                setCommand("followup");
                Toast.makeText(MainActivity.this, "대화를 2번 이상 진행 합니다.", Toast.LENGTH_SHORT).show();
                scenarioTextView.setText("\"점심 추천해줘\"라고 보냄");
                sendMessageToBot("점심 추천해줘");
            }
            });
        alramButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick (View v){
                Timer timer = new Timer();
                TimerTask timerTask = new TimerTask() {
                    public void run() {
                        timerTextView.setText(Integer.toString(cntTime--));
                        if (cntTime == 0) {
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
                timer.schedule(timerTask, 0, 1000);
                cntTime = 5;
                Toast.makeText(MainActivity.this, "임의로 5초뒤로 알람을 세팅합니다.", Toast.LENGTH_SHORT).show();
            }
            });
        quizButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick (View v){
                setCommand("quiz");
                Toast.makeText(MainActivity.this, "퀴즈 진행 합니다.", Toast.LENGTH_SHORT).show();
                scenarioTextView.setText("퀴즈 시작\n");
                sendMessageToBot("B006");
            }
        });

            //dialogflowAgent key정보 Setup
            setUpBot();
        }

        //credential(GoogleService 자격 증명서) 파일을 통해 session 설정
        private void setUpBot () {
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
        private void sendMessageToBot (String message){
            QueryInput input = QueryInput.newBuilder()
                    .setText(TextInput.newBuilder().setText(message).setLanguageCode("en-US")).build();
            new SendMessageInBg(this, sessionName, sessionsClient, input).execute();
        }

        @Override
        public void callback (DetectIntentResponse returnResponse){
            //dialogflowAgent와 통신 성공한 경우
            if (returnResponse != null) {
                String dialogflowBotReply = returnResponse.getQueryResult().getFulfillmentText();
                Log.d("text", returnResponse.getQueryResult().toString());
                //getFulfillmentText가 있을 경우
                if (!dialogflowBotReply.isEmpty()) {
                    //UI or something to do Task
                    switch (command) {
                        case "basic":
                            scenarioTextView.setText(scenarioTextView.getText() + "\n답장: " + dialogflowBotReply);
                            break;
                        case "followup":
                            scenarioTextView.setText(scenarioTextView.getText() + "\n답장: " + dialogflowBotReply);
                            scenarioTextView.setText(scenarioTextView.getText() + "\n\"중국집 메뉴 알려줘\"라고 보냄");
                            sendMessageToBot("중국집 메뉴 알려줘");
                            setCommand("followup2");
                            break;
                        case "followup2":
                            scenarioTextView.setText(scenarioTextView.getText() + "\n답장: " + dialogflowBotReply);
                            break;
                        case "alram":
                            scenarioTextView.setText(scenarioTextView.getText() + "\n답장: " + dialogflowBotReply);
                            break;
                        case "quiz":
                            scenarioTextView.setText(scenarioTextView.getText() + "\n퀴즈: " + dialogflowBotReply);
                            scenarioTextView.setText(scenarioTextView.getText() + "\n88년");
                            sendMessageToBot("88년");
                            setCommand("quiz2");
                            break;
                        case "quiz2":
                            scenarioTextView.setText(scenarioTextView.getText() + "\n답장: " + dialogflowBotReply);
                            if(dialogflowBotReply.contains("마스코트")){
                                scenarioTextView.setText(scenarioTextView.getText() + "\n호돌이");
                                sendMessageToBot("호돌이");
                                setCommand("quiz3");
                            }
                            break;
                        case "quiz3":
                            scenarioTextView.setText(scenarioTextView.getText() + "\n답장: " + dialogflowBotReply);
                            break;
                        default:
                            //scenarioTextView.setText(scenarioTextView.getText()+"\n답장: "+dialogflowBotReply);
                            textView.setText(dialogflowBotReply);
                            break;
                    }

                } else {
                    Toast.makeText(this, "something went wrong", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "failed to connect!", Toast.LENGTH_SHORT).show();
            }
        }

        public void setCommand (String command){
            this.command = command;
        }

    }