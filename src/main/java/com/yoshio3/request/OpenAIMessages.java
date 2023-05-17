package com.yoshio3.request;

import java.io.Serializable;
import java.util.List;


public class OpenAIMessages implements Serializable {
    private List<Message> messages;  

    private int max_tokens;  
    private double temperature;  
    private int frequency_penalty;  
    private int presence_penalty;  
    private double top_p;  
    private String stop;
    private boolean stream;

    public OpenAIMessages() {
    }


    public OpenAIMessages(List<Message> messages, int max_tokens, double temperature, int frequency_penalty,
            int presence_penalty, double top_p, String stop, boolean stream) {
        this.messages = messages;
        this.max_tokens = max_tokens;
        this.temperature = temperature;
        this.frequency_penalty = frequency_penalty;
        this.presence_penalty = presence_penalty;
        this.top_p = top_p;
        this.stop = stop;
        this.stream = stream;
    }
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
    public int getFrequency_penalty() {
        return frequency_penalty;
    }
    public void setFrequency_penalty(int frequency_penalty) {
        this.frequency_penalty = frequency_penalty;
    }
    public int getPresence_penalty() {
        return presence_penalty;
    }
    public void setPresence_penalty(int presence_penalty) {
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
