package com.yoshio3.request;

import java.io.Serializable;

public class Message implements Serializable {
    private String role;  
    private String content;

    public String getRole() {
        return role;
    }
    public void setRole(String role) {
        this.role = role;
    }
    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }      

}
