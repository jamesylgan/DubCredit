package com.microsoft.CognitiveServicesExample;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class SpeechIntentParser {
    public HashMap<String, String> entities = new HashMap<>();
    private String query, intent;

    SpeechIntentParser(final String payload) {
        addPayload(payload);
    }

    public void addPayload(final String payload) {
        try {
            JSONObject json = new JSONObject(payload);
            query = json.getString("query");
            String newIntent = json.getJSONArray("intents").getJSONObject(0).getString("intent");
            if (!newIntent.equals(intent)) {
                intent = newIntent;
                entities.clear();
            }
            JSONArray jsonEntities = json.getJSONArray("entities");
            for (int i = 0; i < jsonEntities.length(); i++) {
                JSONObject entity = jsonEntities.getJSONObject(i);
                String type = entity.getString("type");
                String value;
                if (type.equals("builtin.datetimeV2.date")) {
                    value = parseDateTimeEntity(entity);
                    if (value == null)
                        continue;
                } else {
                    value = entity.getString("entity");
                }
                entities.put(type, value);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getIntent() {
        return intent;
    }

    public String getDescription() {
        switch (intent) {
            case "Create contract":
                return describeCreateContract();

            default:
                return "Speech not recognized";
        }
    }

    private String describeCreateContract() {
        ArrayList<String> descriptionFields = new ArrayList<>(), missingFields = new ArrayList<>();
        String source = entities.get("source");
        String target = entities.get("target");
        String amount = entities.get("amount");
        String deadline = entities.get("builtin.datetimeV2.date");
        String rate = entities.get("percentage");
        String fine = entities.get("fine");
        if (source == null) {
            missingFields.add("say your own name");
        } else {
            descriptionFields.add("from " + source);
        }
        if (target == null) {
            missingFields.add("to whom are you donating");
        } else {
            descriptionFields.add("to " + target);
        }
        if (amount == null) {
            missingFields.add("the amount");
        } else {
            descriptionFields.add("the value of " + amount);
        }
        if (deadline == null) {
            missingFields.add("the deadline of the payment");
        } else {
            descriptionFields.add("until " + deadline);
        }
        if (rate == null) {
            missingFields.add("the interest rate");
        } else {
            descriptionFields.add("with an interest rate of " + rate);
        }
        if (fine == null) {
            missingFields.add("fine for missing the deadline");
        } else {
            descriptionFields.add("with a fine of " + fine + ", should the deadline be missed");
        }
        return buildDescription("Lending", descriptionFields, missingFields);
    }

    private String buildDescription(String header, List<String> descriptionFields, List<String> missingFields) {
        String description = header;
        for (String s : descriptionFields) {
            description += " " + s;
        }
        description += ".";
        if (!missingFields.isEmpty()) {
            description += "\nMissing information:";
            if (missingFields.size() == 1) {
                description += missingFields.get(0);
            } else {
                int i;
                for (i = 0; i < missingFields.size() - 2; i++)
                    description += missingFields.get(i) + ", ";
                description += missingFields.get(i) + " and " + missingFields.get(i + 1);;
            }
        }
        return description;
    }

    private String parseDateTimeEntity(JSONObject entity) throws JSONException {
        JSONArray dateValue = entity.getJSONObject("resolution").getJSONArray("values");
        for (int i = 0; i < dateValue.length(); i++) {
            JSONObject candidateValue = dateValue.getJSONObject(i);
            if (candidateValue.getString("type").equals("date")) {
                return candidateValue.getString("value");
            }
        }
        return null;
    }
}
