package com.microsoft.CognitiveServicesExample;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class ContractModel {
    public String source;
    public String destination;
    public float value;
    public String date;
    public int interestRate;
    public float fine;

    public ContractModel() {}

    public ContractModel(String source, String destination, float value, String date, int interestRate, float fine) {
        this.source = source;
        this.destination = destination;
        this.value = value;
        this.date = date;
        this.interestRate = interestRate;
        this.fine = fine;
    }

}