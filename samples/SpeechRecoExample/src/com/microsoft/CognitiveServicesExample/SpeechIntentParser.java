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
    public enum Intent {
        unknown, create, accept;
    }

    SpeechIntentParser() {}

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
                switch (type) {
                    case "builtin.datetimeV2.date":
                        value = parseDateTimeEntity(entity);
                        if (value == null)
                            continue;
                        break;

                    case "builtin.percentage":
                        value = entity.getJSONObject("resolution").getString("value");
                        break;

                    case "target":
                        value = entity.getString("entity");
                        value = Character.toUpperCase(value.charAt(0)) + value.substring(1);
                        break;

                    default:
                        value = entity.getString("entity");
                }
                entities.put(type, value);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public Intent getIntent() {
        switch (intent) {
            case "Create contract":
                return Intent.create;

            default:
                return Intent.unknown;
        }
    }

    public String getDescription() {
        switch (getIntent()) {
            case create:
                return describeCreateContract();

            default:
                return "Speech not recognized";
        }
    }

    private String describeCreateContract() {
        ArrayList<String> missingFields = new ArrayList<>();
        String source = entities.get("source");
        String target = entities.get("target");
        String amount = entities.get("amount");
        String deadline = entities.get("builtin.datetimeV2.date");
        String rate = entities.get("builtin.percentage");
        String fine = entities.get("fine");
        if (source == null)
            missingFields.add("say your own name");
        if (target == null)
            missingFields.add("to whom are you donating");
        if (amount == null)
            missingFields.add("the amount");
        if (deadline == null)
            missingFields.add("the deadline of the payment");
        if (rate == null)
            missingFields.add("the interest rate");
        if (fine == null)
            missingFields.add("fine for missing the deadline");
        return buildDescription("lend", missingFields);
    }

    private String buildDescription(String action, List<String> missingFields) {
        if (missingFields.isEmpty())
            return "Good to go!";
        String description = "\nMissing information to " + action + ": ";
        if (missingFields.size() == 1) {
            description += missingFields.get(0);
        } else {
            int i;
            for (i = 0; i < missingFields.size() - 2; i++)
                description += missingFields.get(i) + ", ";
            description += missingFields.get(i) + " and " + missingFields.get(i + 1);;
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
