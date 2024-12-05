package com.example.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private DatabaseHelper dbHelper;
    private LinearLayout contactsContainer;
    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_FIRST_RUN = "isFirstRun";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        contactsContainer = view.findViewById(R.id.contactsContainer);
        dbHelper = new DatabaseHelper(requireContext());

        if (isFirstRun()) {
            insertInitialData();
            setFirstRunComplete();
        }

        displayContacts();
    }

    private boolean isFirstRun() {
        SharedPreferences preferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return preferences.getBoolean(KEY_FIRST_RUN, true);
    }

    private void setFirstRunComplete() {
        SharedPreferences preferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(KEY_FIRST_RUN, false);
        editor.apply();
    }

    private void insertInitialData() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            // 插入联系人
            ContentValues contactValues = new ContentValues();

            // 插入丁真
            contactValues.put(DatabaseHelper.COLUMN_NAME, "丁真");
            long dingzhenId = db.insert(DatabaseHelper.TABLE_CONTACTS, null, contactValues);

            // 插入丁真的消息
            insertMessage(db, dingzhenId, "你好，今天怎么样？", false);
            insertMessage(db, dingzhenId, "我刚刚完成了工作。", true);
            insertMessage(db, dingzhenId, "有时间一起吃饭吗？", false);

            // 插入李华
            contactValues.clear();
            contactValues.put(DatabaseHelper.COLUMN_NAME, "李华");
            long lihuaId = db.insert(DatabaseHelper.TABLE_CONTACTS, null, contactValues);

            // 插入李华的消息
            insertMessage(db, lihuaId, "在吗？", false);
            insertMessage(db, lihuaId, "我在的", true);
            insertMessage(db, lihuaId, "周末有空吗？", false);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void insertMessage(SQLiteDatabase db, long contactId, String message, boolean isFromMe) {
        ContentValues messageValues = new ContentValues();
        messageValues.put(DatabaseHelper.COLUMN_CONTACT_ID, contactId);
        messageValues.put(DatabaseHelper.COLUMN_MESSAGE, message);
        messageValues.put(DatabaseHelper.COLUMN_IS_FROM_ME, isFromMe ? 1 : 0);
        messageValues.put(DatabaseHelper.COLUMN_TIMESTAMP, System.currentTimeMillis());
        db.insert(DatabaseHelper.TABLE_MESSAGES, null, messageValues);
    }

    private List<Contact> loadContactsFromDatabase() {
        List<Contact> contacts = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor contactsCursor = db.query(
                DatabaseHelper.TABLE_CONTACTS,
                new String[]{DatabaseHelper.COLUMN_ID, DatabaseHelper.COLUMN_NAME},
                null, null, null, null, null);

        if (contactsCursor != null) {
            try {
                int idColumnIndex = contactsCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID);
                int nameColumnIndex = contactsCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME);

                while (contactsCursor.moveToNext()) {
                    long contactId = contactsCursor.getLong(idColumnIndex);
                    String contactName = contactsCursor.getString(nameColumnIndex);

                    List<Message> messages = loadMessagesForContact(contactId);
                    contacts.add(new Contact(contactId, contactName, messages));
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } finally {
                contactsCursor.close();
            }
        }

        return contacts;
    }

    private List<Message> loadMessagesForContact(long contactId) {
        List<Message> messages = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor messageCursor = db.query(
                DatabaseHelper.TABLE_MESSAGES,
                new String[]{
                        DatabaseHelper.COLUMN_ID,
                        DatabaseHelper.COLUMN_MESSAGE,
                        DatabaseHelper.COLUMN_IS_FROM_ME,
                        DatabaseHelper.COLUMN_TIMESTAMP
                },
                DatabaseHelper.COLUMN_CONTACT_ID + " = ?",
                new String[]{String.valueOf(contactId)},
                null, null,
                DatabaseHelper.COLUMN_TIMESTAMP + " ASC"
        );

        if (messageCursor != null) {
            try {
                int idIndex = messageCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID);
                int messageIndex = messageCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MESSAGE);
                int isFromMeIndex = messageCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_IS_FROM_ME);
                int timestampIndex = messageCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TIMESTAMP);

                while (messageCursor.moveToNext()) {
                    long messageId = messageCursor.getLong(idIndex);
                    String messageContent = messageCursor.getString(messageIndex);
                    boolean isFromMe = messageCursor.getInt(isFromMeIndex) == 1;
                    long timestamp = messageCursor.getLong(timestampIndex);

                    messages.add(new Message(messageId, messageContent, isFromMe, timestamp));
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } finally {
                messageCursor.close();
            }
        }

        return messages;
    }

    private void displayContacts() {
        List<Contact> contacts = loadContactsFromDatabase();
        contactsContainer.removeAllViews();

        for (Contact contact : contacts) {
            View contactView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_contact, contactsContainer, false);

            ImageView avatarView = contactView.findViewById(R.id.avatarImageView);
            TextView nameView = contactView.findViewById(R.id.nameTextView);
            TextView lastMessageView = contactView.findViewById(R.id.lastMessageTextView);

            nameView.setText(contact.getName());
            Message lastMessage = contact.getLastMessage();
            if (lastMessage != null) {
                lastMessageView.setText(lastMessage.getContent());
            }

            contactView.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), ChatActivity.class);
                intent.putExtra("contact_id", contact.getId());
                String encodedName = null;
                try {
                    encodedName = URLEncoder.encode(contact.getName(), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
                intent.putExtra("contact_name", encodedName);
                startActivity(intent);
            });


            contactsContainer.addView(contactView);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}
