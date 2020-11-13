package com.o2o.hyodori.utils;

import android.os.AsyncTask;
import android.util.Log;

import com.google.cloud.dialogflow.v2.DetectIntentRequest;
import com.google.cloud.dialogflow.v2.DetectIntentResponse;
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.SessionName;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.o2o.hyodori.interfaces.DialogflowBotReply;

public class SendMessageInBg extends AsyncTask<Void, Void, DetectIntentResponse>{
    private SessionName session;
    private SessionsClient sessionsClient;
    private QueryInput queryInput;
    private String TAG = "async";
    private DialogflowBotReply dialogflowBotReply;

    public SendMessageInBg(DialogflowBotReply dialogflowBotReply, SessionName session, SessionsClient sessionsClient,
                           QueryInput queryInput) {
        this.dialogflowBotReply = dialogflowBotReply;
        this.session = session;
        this.sessionsClient = sessionsClient;
        this.queryInput = queryInput;
    }

    @Override
    protected DetectIntentResponse doInBackground(Void... voids) {
        try {
            //DetectIntentRequest를 통해 request 값을 담아서
            DetectIntentRequest detectIntentRequest =
                    DetectIntentRequest.newBuilder()
                            .setSession(session.toString())
                            .setQueryInput(queryInput)
                            .build();
            //sessionsClient의 detectIntent 메서드에 담아 호출 -> DetectIntentResponse 객체를 반환함.
            return sessionsClient.detectIntent(detectIntentRequest);
        } catch (Exception e) {
            Log.d(TAG, "doInBackground: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(DetectIntentResponse response) {
        //handle return response here
        dialogflowBotReply.callback(response);
    }
}
