package com.example.myapplication;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;
import java.util.ArrayList;
import android.util.Log;
import android.view.Gravity;


public class ChatActivity extends AppCompatActivity {
    private LinearLayout chatContainer;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatContainer = findViewById(R.id.chat_container);
        dbHelper = new DatabaseHelper(this);

        // 获取传递的联系人信息
        Intent intent = getIntent();
        long contactId = intent.getLongExtra("contact_id", -1);
        String contactName = intent.getStringExtra("contact_name");

        // 设置联系人姓名
        TextView contactNameTextView = findViewById(R.id.contact_name);
        contactNameTextView.setText(contactName);

        // 从数据库加载消息
        if (contactId != -1) {
            List<Message> messages = loadMessagesForContact(contactId);
            displayMessages(messages);
        }
    }

    private List<Message> loadMessagesForContact(long contactId) {
        List<Message> messages = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(
                DatabaseHelper.TABLE_MESSAGES,
                new String[]{
                        DatabaseHelper.COLUMN_MESSAGE,
                        DatabaseHelper.COLUMN_IS_FROM_ME,
                        DatabaseHelper.COLUMN_TIMESTAMP
                },
                DatabaseHelper.COLUMN_CONTACT_ID + " = ?",
                new String[]{String.valueOf(contactId)},
                null,
                null,
                DatabaseHelper.COLUMN_TIMESTAMP + " ASC"
        );

        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    String content = cursor.getString(0);
                    boolean isFromMe = cursor.getInt(1) == 1;
                    long timestamp = cursor.getLong(2);
                    messages.add(new Message(0, content, isFromMe, timestamp));
                }
            } finally {
                cursor.close();
            }
        }

        return messages;
    }

    private void displayMessages(List<Message> messages) {
        for (Message message : messages) {
            TextView messageView = new TextView(this);
            messageView.setText(message.getContent());
            messageView.setTextSize(18f);
            messageView.setPadding(16, 16, 16, 16);

            // 设置消息的布局参数
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

            // 根据消息来源设置显示位置
            if (message.isFromMe()) {
                params.gravity = Gravity.END; // 我发送的消息靠右
            } else {
                params.gravity = Gravity.START; // 收到的消息靠左
            }

            messageView.setLayoutParams(params);
            chatContainer.addView(messageView);
            Log.d("ChatActivity", "Added message: " + message.getContent());
        }
    }

}
