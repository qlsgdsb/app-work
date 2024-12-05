package com.example.myapplication;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {
    private LinearLayout chatContainer;
    private EditText messageInput;
    private Button sendButton;
    private DatabaseHelper dbHelper;
    private long contactId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 隐藏系统 ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_chat);

        // 初始化视图
        chatContainer = findViewById(R.id.chat_container);
        messageInput = findViewById(R.id.message_input);
        sendButton = findViewById(R.id.send_button);
        dbHelper = new DatabaseHelper(this);

        // 获取传递的联系人信息
        Intent intent = getIntent();
        contactId = intent.getLongExtra("contact_id", -1);
        String contactName = null;
        try {
            contactName = URLDecoder.decode(intent.getStringExtra("contact_name"),"UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        // 设置自定义标题栏
        TextView contactNameHeader = findViewById(R.id.contact_name_header);
        contactNameHeader.setText(contactName);

        // 设置返回按钮点击事件
        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        Log.d("ChatActivity", "Contact name: " + contactName);

        // 加载消息
        if (contactId != -1) {
            loadMessages();
        }

        // 发送按钮点击事件
        sendButton.setOnClickListener(v -> sendMessage());
    }

    private void loadMessages() {
        List<Message> messages = loadMessagesForContact(contactId);
        displayMessages(messages);
    }

    private List<Message> loadMessagesForContact(long contactId) {
        List<Message> messages = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] projection = {
                DatabaseHelper.COLUMN_ID,
                DatabaseHelper.COLUMN_MESSAGE,
                DatabaseHelper.COLUMN_IS_FROM_ME,
                DatabaseHelper.COLUMN_TIMESTAMP
        };

        String selection = DatabaseHelper.COLUMN_CONTACT_ID + " = ?";
        String[] selectionArgs = { String.valueOf(contactId) };
        String sortOrder = DatabaseHelper.COLUMN_TIMESTAMP + " ASC";

        try (Cursor cursor = db.query(
                DatabaseHelper.TABLE_MESSAGES,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        )) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID));
                String content = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MESSAGE));
                boolean isFromMe = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_IS_FROM_ME)) == 1;
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TIMESTAMP));

                Message message = new Message(id, content, isFromMe, timestamp);
                messages.add(message);
            }
        }

        return messages;
    }

    private void sendMessage() {
        String content = messageInput.getText().toString().trim();
        if (!content.isEmpty()) {
            // 保存消息到数据库
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COLUMN_CONTACT_ID, contactId);
            values.put(DatabaseHelper.COLUMN_MESSAGE, content);
            values.put(DatabaseHelper.COLUMN_IS_FROM_ME, 1);
            values.put(DatabaseHelper.COLUMN_TIMESTAMP, System.currentTimeMillis());

            db.insert(DatabaseHelper.TABLE_MESSAGES, null, values);

            // 清空输入框并刷新消息显示
            messageInput.setText("");
            loadMessages();
        }
    }

    private void displayMessages(List<Message> messages) {
        chatContainer.removeAllViews();

        for (Message message : messages) {
            TextView messageView = new TextView(this);
            messageView.setText(message.getContent());
            messageView.setTextSize(16f);
            messageView.setPadding(16, 8, 16, 8);

            // 设置最大宽度为屏幕宽度的75%
            messageView.setMaxWidth((int)(getResources().getDisplayMetrics().widthPixels * 0.75));

            // 根据消息来源设置不同的背景和对齐方式
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(16, 8, 16, 8);

            if (message.isFromMe()) {
                messageView.setBackgroundResource(R.drawable.message_bubble_sent);
                params.gravity = Gravity.END;
                messageView.setTextColor(getResources().getColor(android.R.color.black));
            } else {
                messageView.setBackgroundResource(R.drawable.message_bubble_received);
                params.gravity = Gravity.START;
                messageView.setTextColor(getResources().getColor(android.R.color.black));
            }

            messageView.setLayoutParams(params);
            chatContainer.addView(messageView);
        }

        // 滚动到最新消息
        chatContainer.post(() -> {
            ScrollView scrollView = (ScrollView) chatContainer.getParent();
            scrollView.fullScroll(ScrollView.FOCUS_DOWN);
        });
    }
}
