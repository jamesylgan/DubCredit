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
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.microsoft.cognitiveservices.speechrecognition.DataRecognitionClient;
import com.microsoft.cognitiveservices.speechrecognition.ISpeechRecognitionServerEvents;
import com.microsoft.cognitiveservices.speechrecognition.MicrophoneRecognitionClient;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionResult;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionStatus;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionMode;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionServiceFactory;

public class MainActivity extends Activity implements ISpeechRecognitionServerEvents
{
    private final String TAG = "DubCredit";
    private static final int RESULT_PICK_CONTACT = 85500;
    private SpeechIntentParser intentParser = new SpeechIntentParser();
    private TextView outputText, speechPreview, contractTextView;
    private FloatingActionButton startButton;

    int m_waitSeconds = 0;
    DataRecognitionClient dataClient = null;
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

        this.startButton = (FloatingActionButton) findViewById(R.id.speakNowButton);
        outputText = (TextView) findViewById(R.id.outputText);
        speechPreview = (TextView) findViewById(R.id.speechPreview);
        contractTextView = (TextView) findViewById(R.id.contractTextView);

        fillCreateContract();

        if (getString(R.string.primaryKey).startsWith("Please")) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.add_subscription_key_tip_title))
                    .setMessage(getString(R.string.add_subscription_key_tip))
                    .setCancelable(false)
                    .show();
        }

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
        this.startButton.setEnabled(false);

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
            this.startButton.setEnabled(true);
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
                    Button goodToGo = (Button) findViewById(R.id.goodToGo);
                    goodToGo.setVisibility(View.VISIBLE);
                    goodToGo.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent contactsIntent = new Intent(Intent.ACTION_PICK,
                                    ContactsContract.CommonDataKinds.Email.CONTENT_URI);
                            startActivityForResult(contactsIntent, 1);
                            // startActivityForResult(contactsIntent, RESULT_PICK_CONTACT);
                        }
                    });
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
        this.startButton.setEnabled(true);
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
            this.startButton.setEnabled(true);
        }
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
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
            contractTextView.setText(Html.fromHtml(createText, Html.FROM_HTML_MODE_COMPACT));
        else
            contractTextView.setText(Html.fromHtml(createText));
        return source != null && target != null && amount != null && deadline != null && rate != null && fine != null;
    }

    /**
     * Handles the Click event of the RadioButton control.
     * @param rGroup The radio grouping.
     * @param checkedId The checkedId.
     */
    private void RadioButton_Click(RadioGroup rGroup, int checkedId) {
        // Reset everything
        if (this.micClient != null) {
            this.micClient.endMicAndRecognition();
            try {
                this.micClient.finalize();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
            this.micClient = null;
        }

        if (this.dataClient != null) {
            try {
                this.dataClient.finalize();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
            this.dataClient = null;
        }

        this.startButton.setEnabled(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case RESULT_PICK_CONTACT:
                    Cursor cursor = null;
                    try {
                        String name = null ;
                        String email = null;
                        Uri uri = data.getData();
                        cursor = getContentResolver().query(uri, null, null, null, null);
                        cursor.moveToFirst();
                        int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                        int emailIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS);
                        name = cursor.getString(nameIndex);
                        email = cursor.getString(emailIndex);
                        Log.w(TAG, name);
                        Log.w(TAG, email);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

//                    FirebaseDatabase database = FirebaseDatabase.getInstance();
//                    DatabaseReference
//                    contactPicked(data);
                    break;
            }
        } else {
            Log.e(TAG, "Failed to pick contact");
        }
    }
}
