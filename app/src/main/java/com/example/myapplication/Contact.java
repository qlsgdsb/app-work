package com.example.myapplication;

import java.util.List;

public class Contact {
    private long id;
    private String name;
    private List<Message> messages;

    public Contact(long id, String name, List<Message> messages) {
        this.id = id;
        this.name = name;
        this.messages = messages;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public Message getLastMessage() {
        return messages.isEmpty() ? null : messages.get(messages.size() - 1);
    }
}
