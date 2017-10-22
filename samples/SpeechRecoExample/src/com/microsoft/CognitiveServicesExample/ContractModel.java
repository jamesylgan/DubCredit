package com.microsoft.CognitiveServicesExample;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class ContractModel {
    public String source;
    public String destination;
    public String sourceId;
    public String destinationId;
    public float value;
    public String date;
    public int interestRate;
    public float fine;

    public ContractModel() {}

    public ContractModel(String source, String destination, String sourceId, String destinationId, float value, String date, int interestRate, float fine) {
        this.source = source;
        this.destination = destination;
        this.sourceId = sourceId;
        this.destinationId = destinationId;
        this.value = value;
        this.date = date;
        this.interestRate = interestRate;
        this.fine = fine;
    }

}