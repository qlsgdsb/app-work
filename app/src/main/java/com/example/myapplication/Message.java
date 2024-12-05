package com.example.myapplication;

public class Message {
    private long id;
    private String content;
    private boolean isFromMe;
    private long timestamp;

    public Message(long id, String content, boolean isFromMe, long timestamp) {
        this.id = id;
        this.content = content;
        this.isFromMe = isFromMe;
        this.timestamp = timestamp;
    }

    public long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public boolean isFromMe() {
        return isFromMe;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
