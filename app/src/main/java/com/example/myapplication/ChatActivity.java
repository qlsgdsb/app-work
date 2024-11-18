package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private LinearLayout chatContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // 获取传递的联系人信息和消息
        Intent intent = getIntent();
        String contactName = intent.getStringExtra("contact_name");
        List<String> contactMessages = intent.getStringArrayListExtra("contact_messages");

        // 设置联系人姓名
        TextView contactNameTextView = findViewById(R.id.contact_name);
        contactNameTextView.setText(contactName); // 仅显示联系人姓名

        // 显示所有消息
        chatContainer = findViewById(R.id.chat_container);
        displayMessages(contactMessages);
    }

    private void displayMessages(List<String> messages) {
        for (String message : messages) {
            TextView messageTextView = new TextView(this);
            messageTextView.setText(message);
            messageTextView.setTextSize(18f);
            messageTextView.setPadding(16, 16, 16, 16);
            chatContainer.addView(messageTextView);
        }
    }
}
