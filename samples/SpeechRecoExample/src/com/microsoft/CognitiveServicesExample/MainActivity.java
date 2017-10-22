/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license.
 * //
 * Project Oxford: http://ProjectOxford.ai
 * //
 * ProjectOxford SDK GitHub:
 * https://github.com/Microsoft/ProjectOxford-ClientSDK
 * //
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 * //
 * MIT License:
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * //
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * //
 * THE SOFTWARE IS PROVIDED ""AS IS"", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.microsoft.CognitiveServicesExample;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.microsoft.cognitiveservices.speechrecognition.DataRecognitionClient;
import com.microsoft.cognitiveservices.speechrecognition.ISpeechRecognitionServerEvents;
import com.microsoft.cognitiveservices.speechrecognition.MicrophoneRecognitionClient;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionResult;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionStatus;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionMode;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionServiceFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends Activity implements ISpeechRecognitionServerEvents
{
    private final String TAG = "DubCredit";
    private static final int RESULT_PICK_CONTACT = 85500;
    private SpeechIntentParser intentParser = new SpeechIntentParser();
    private TextView outputText, speechPreview, contractTextView, header;
    private Button startButton;
    private ContractModel contract;
    private String userId;
    private DatabaseReference databaseRoot;
    private ContractModel receivedContract;

    int m_waitSeconds = 0;
    MicrophoneRecognitionClient micClient = null;
    FinalResponseStatus isReceivedResponse = FinalResponseStatus.NotReceived;

    public enum FinalResponseStatus { NotReceived, OK, Timeout }

    /**
     * Gets the primary subscription key
     */
    public String getPrimaryKey() {
        return this.getString(R.string.primaryKey);
    }

    /**
     * Gets the LUIS application identifier.
     * @return The LUIS application identifier.
     */
    private String getLuisAppId() {
        return this.getString(R.string.luisAppID);
    }

    /**
     * Gets the LUIS subscription identifier.
     * @return The LUIS subscription identifier.
     */
    private String getLuisSubscriptionID() {
        return this.getString(R.string.luisSubscriptionID);
    }

    /**
     * Gets a value indicating whether or not to use the microphone.
     * @return true if [use microphone]; otherwise, false.
     */
    private Boolean getUseMicrophone() {
        return true;
    }

    /**
     * Gets the current speech recognition mode.
     * @return The speech recognition mode.
     */
    private SpeechRecognitionMode getMode() {
        return SpeechRecognitionMode.ShortPhrase;
    }

    /**
     * Gets the default locale.
     * @return The default locale.
     */
    private String getDefaultLocale() {
        return "en-us";
    }

    /**
     * Gets the Cognitive Service Authentication Uri.
     * @return The Cognitive Service Authentication Uri.  Empty if the global default is to be used.
     */
    private String getAuthenticationUri() {
        return this.getString(R.string.authenticationUri);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        userId = hash(getIntent().getStringExtra("user"));
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        databaseRoot = database.getReference();
        header = (TextView) findViewById(R.id.header);
        databaseRoot.child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                receivedContract = dataSnapshot.getValue(ContractModel.class);
                if (receivedContract == null)
                    return;
                Button openContracts = (Button)findViewById(R.id.contractsAvailable);
                openContracts.setVisibility(View.VISIBLE);
                openContracts.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        intentParser.entities.clear();
                        fillAcceptContract();
                        String content = getString(R.string.headerAcceptContract);
                        content = content.replace("_source", receivedContract.source);
                        content = content.replace("_value", String.format("$ %.2f", receivedContract.value));
                        content = content.replace("_rate", receivedContract.interestRate + "%");
                        content = content.replace("_date", receivedContract.date);
                        content = content.replace("_fine", String.format("$ %.2f", receivedContract.fine));
                        header.setText(content);
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, databaseError.toString());
            }
        });

        this.startButton = (Button) findViewById(R.id.speakNowButton);
        outputText = (TextView) findViewById(R.id.outputText);
        speechPreview = (TextView) findViewById(R.id.speechPreview);
        contractTextView = (TextView) findViewById(R.id.contractTextView);

        fillCreateContract();

        // setup the buttons
        final MainActivity This = this;
        this.startButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                This.StartButton_Click(arg0);
            }
        });
    }

    /**
     * Handles the Click event of the startButton control.
     */
    private void StartButton_Click(View arg0) {
        setEnableRecording(false);

        this.m_waitSeconds = this.getMode() == SpeechRecognitionMode.ShortPhrase ? 20 : 200;

        this.LogRecognitionStart();

        if (this.micClient == null) {
            this.micClient =
                    SpeechRecognitionServiceFactory.createMicrophoneClientWithIntent(
                            this,
                            this.getDefaultLocale(),
                            this,
                            this.getPrimaryKey(),
                            this.getLuisAppId(),
                            this.getLuisSubscriptionID());
            this.micClient.setAuthenticationUri(this.getAuthenticationUri());
        }

        this.micClient.startMicAndRecognition();
    }

    /**
     * Logs the recognition start.
     */
    private void LogRecognitionStart() {
        String recoSource = "microphone";
        Log.i(TAG, "Start speech recognition using " + recoSource + " with " + this.getMode() + " mode in " + this.getDefaultLocale() + " language");
    }

    public void onFinalResponseReceived(final RecognitionResult response) {
        boolean isFinalDicationMessage = this.getMode() == SpeechRecognitionMode.LongDictation &&
                (response.RecognitionStatus == RecognitionStatus.EndOfDictation ||
                        response.RecognitionStatus == RecognitionStatus.DictationEndSilenceTimeout);
        if (null != this.micClient && this.getUseMicrophone() && ((this.getMode() == SpeechRecognitionMode.ShortPhrase) || isFinalDicationMessage)) {
            // we got the final result, so it we can end the mic reco.  No need to do this
            // for dataReco, since we already called endAudio() on it as soon as we were done
            // sending all the data.
            this.micClient.endMicAndRecognition();
        }

        if (isFinalDicationMessage) {
            setEnableRecording(true);
            this.isReceivedResponse = FinalResponseStatus.OK;
        }

        if (!isFinalDicationMessage) {
            Log.i(TAG, "Final n-BEST Results");
            for (int i = 0; i < response.Results.length; i++) {
                Log.i(TAG, "[" + i + "]" + " Confidence=" + response.Results[i].Confidence +
                        " Text=\"" + response.Results[i].DisplayText + "\"");
            }
        }
    }

    /**
     * Called when a final response is received and its intent is parsed
     */
    public void onIntentReceived(final String payload) {
        Log.i(TAG, "Intent received by onIntentReceived(): " + payload);
        intentParser.addPayload(payload);
        outputText.setText(intentParser.getDescription());
        if (intentParser.entities.isEmpty()) {
            speechPreview.setText("No speech entities detected");
        }
        switch (intentParser.getIntent()) {
            case create:
                if (fillCreateContract()) {
                    contract = new ContractModel(
                            intentParser.entities.get("source"),
                            intentParser.entities.get("target"),
                            hash(userId),
                            null,
                            parseValue(intentParser.entities.get("amount")),
                            intentParser.entities.get("builtin.datetimeV2.date"),
                            Math.round(parseValue(intentParser.entities.get("builtin.percentage"))),
                            parseValue(intentParser.entities.get("fine"))
                            );
                    Button goodToGo = (Button) findViewById(R.id.goodToGo);
                    goodToGo.setVisibility(View.VISIBLE);
                    goodToGo.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent contactsIntent = new Intent(Intent.ACTION_PICK,
                                    ContactsContract.CommonDataKinds.Email.CONTENT_URI);
                            startActivityForResult(contactsIntent, 1);
                        }
                    });
                }
                break;

            case accept:
                if (fillAcceptContract()) {
                    databaseRoot.child(userId).removeValue();
                    databaseRoot.child("history").child(receivedContract.sourceId).child("accepted").setValue(true);
                }
                break;

            default:
                speechPreview.setText("Not recognized");
        }
    }

    public void onPartialResponseReceived(final String response) {
        Log.i(TAG, "Partial result received by onPartialResponseReceived(): " + response);
        speechPreview.setText(response);
    }

    public void onError(final int errorCode, final String response) {
        Log.i(TAG, "onError(): " + errorCode + " - " + response);
        setEnableRecording(true);
    }

    /**
     * Called when the microphone status has changed.
     * @param recording The current recording state
     */
    public void onAudioEvent(boolean recording) {
        Log.i(TAG, "Microphone status: " + recording);
        if (recording) {
            Log.i(TAG, "Please start speaking");
        }

        if (!recording) {
            this.micClient.endMicAndRecognition();
            setEnableRecording(true);
        }
    }

    private float parseValue(String value) {
        return Float.parseFloat(value.replaceAll("[^\\\\.0123456789]",""));
    }

    private boolean fillCreateContract() {
        String createText = getString(R.string.loan_description);
        String source = intentParser.entities.get("source");
        String target = intentParser.entities.get("target");
        String amount = intentParser.entities.get("amount");
        String deadline = intentParser.entities.get("builtin.datetimeV2.date");
        String rate = intentParser.entities.get("builtin.percentage");
        String fine = intentParser.entities.get("fine");
        createText = createText.replace("_source", source != null ? source : getString(R.string.say_your_name));
        createText = createText.replace("_target", target != null ? target : getString(R.string.other_person));
        createText = createText.replace("_amount", amount != null ? amount : getString((R.string.amount)));
        createText = createText.replace("_deadline", deadline != null ? deadline : getString((R.string.deadline)));
        createText = createText.replace("_rate", rate != null ? rate : getString((R.string.interest_rate)));
        createText = createText.replace("_fine", fine != null ? fine : getString(R.string.fine));
        contractTextView.setText(Html.fromHtml(createText));
        return source != null && target != null && amount != null && deadline != null && rate != null && fine != null;
    }

    private boolean fillAcceptContract() {
        String createText = getString(R.string.accept_description);
        String source = intentParser.entities.get("source");
        String target = intentParser.entities.get("target");
        createText = createText.replace("_source", source != null ? source : getString(R.string.say_your_name));
        createText = createText.replace("_target", target != null ? target : getString(R.string.other_person));
        contractTextView.setText(Html.fromHtml(createText));
        return source != null && target != null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.w(TAG, "onActivityResult: " + data.getDataString());
        if (resultCode == RESULT_OK) {
            String email;
            try {
                Uri uri = data.getData();
                Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                if (cursor == null)
                    return;
                cursor.moveToFirst();
                int emailIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS);
                email = cursor.getString(emailIndex);
                cursor.close();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            String destinationId = hash(email);
            contract.destinationId = destinationId;
            databaseRoot.child(destinationId).setValue(contract);
            HashMap<String, Object> transaction = new HashMap<>();
            transaction.put("value", contract.value);
            transaction.put("destination", contract.destination);
            databaseRoot.child("history").child(userId).setValue(transaction);
            Toast.makeText(this, "Offer sent to " + email, Toast.LENGTH_LONG).show();
            intentParser.entities.clear();
            fillCreateContract();
        }
    }

    private String hash(String data) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        StringBuffer hexString = new StringBuffer();
        byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        for (byte aHash : hash) {
            String hex = Integer.toHexString(0xff & aHash);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private void setEnableRecording(boolean enable) {
        this.startButton.setEnabled(enable);
        if (enable)
            this.startButton.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
        else {
            speechPreview.setText("Recording...");
            this.startButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
        }
    }
}
