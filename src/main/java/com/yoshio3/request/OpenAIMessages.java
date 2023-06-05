package com.yoshio3.request;

import java.io.Serializable;
import java.util.List;


public class OpenAIMessages implements Serializable {
    private List<Message> messages;  

    private double temperature;  
    private double top_p;  
    private double presence_penalty;  
    private double frequency_penalty;  
    private int max_tokens;  
    private String stop;
    private boolean stream;

    public boolean isStream() {
        return stream;
    }
    public void setStream(boolean stream) {
        this.stream = stream;
    }
    public List<Message> getMessages() {
        return messages;
    }
    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
    public int getMax_tokens() {
        return max_tokens;
    }
    public void setMax_tokens(int max_tokens) {
        this.max_tokens = max_tokens;
    }
    public double getTemperature() {
        return temperature;
    }
    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }
    public double getFrequency_penalty() {
        return frequency_penalty;
    }
    public void setFrequency_penalty(double frequency_penalty) {
        this.frequency_penalty = frequency_penalty;
    }
    public double getPresence_penalty() {
        return presence_penalty;
    }
    public void setPresence_penalty(double presence_penalty) {
        this.presence_penalty = presence_penalty;
    }
    public double getTop_p() {
        return top_p;
    }
    public void setTop_p(double top_p) {
        this.top_p = top_p;
    }
    public String getStop() {
        return stop;
    }
    public void setStop(String stop) {
        this.stop = stop;
    }
}
