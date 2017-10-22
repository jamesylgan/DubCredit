package com.microsoft.CognitiveServicesExample;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoryActivity extends Activity {
    final private String TAG = "DubCredit";
    private DatabaseReference databaseRoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        String userId = hash(getIntent().getStringExtra("user"));
        databaseRoot = database.getReference();
        databaseRoot.child("history").child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                float value;
                try {
                    value = (float)dataSnapshot.child("value").getValue();
                } catch (Exception e) {
                    value = (long)dataSnapshot.child("value").getValue();
                }
                Object acceptedObject = dataSnapshot.child("accepted").getValue();
                boolean accepted;
                if (acceptedObject == null)
                    accepted = false;
                else
                    accepted = (boolean)acceptedObject;
                String destination = (String)dataSnapshot.child("destination").getValue();
                final List<Map<String, String>> listItem = new ArrayList<Map<String, String>>(1);

                final Map<String, String> listItemMap = new HashMap<>();
                listItemMap.put("text1", destination + (accepted ? " - Accepted" : " - Pending"));
                listItemMap.put("text2", String.format("$ %.2f", value));
                listItem.add(Collections.unmodifiableMap(listItemMap));

                final String[] fromMapKey = new String[] {"text1", "text2"};
                final int[] toLayoutId = new int[] {android.R.id.text1, android.R.id.text2};
                final List<Map<String, String>> list = Collections.unmodifiableList(listItem);

                SimpleAdapter adapter = new SimpleAdapter(getApplicationContext(), list,
                        android.R.layout.simple_list_item_2,
                        fromMapKey, toLayoutId);
                ListView listView = (ListView) findViewById(R.id.listView);
                listView.setAdapter(adapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, databaseError.toString());
            }
        });
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
}
