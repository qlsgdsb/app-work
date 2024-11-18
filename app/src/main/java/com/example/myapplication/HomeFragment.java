package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private LinearLayout messageContainer;
    private static final String TAG = "HomeFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        messageContainer = view.findViewById(R.id.message_container);

        List<Contact> contacts = loadContactsFromJson(getContext());
        displayContacts(contacts);

        return view;
    }

    // 加载 JSON 文件，解析为联系人列表
    private List<Contact> loadContactsFromJson(Context context) {
        List<Contact> contacts = new ArrayList<>();
        try {
            InputStream is = context.getAssets().open("messages.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            String json = new String(buffer, "UTF-8");
            JSONObject jsonObject = new JSONObject(json);
            JSONArray contactsArray = jsonObject.getJSONArray("contacts");

            for (int i = 0; i < contactsArray.length(); i++) {
                JSONObject contactObject = contactsArray.getJSONObject(i);
                String name = contactObject.getString("name");
                JSONArray messagesArray = contactObject.getJSONArray("messages");

                List<String> messages = new ArrayList<>();
                for (int j = 0; j < messagesArray.length(); j++) {
                    messages.add(messagesArray.getString(j));
                }

                contacts.add(new Contact(name, messages));
            }
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Failed to open or parse messages.json", e);
        }
        return contacts;
    }

    // 显示联系人列表，每位联系人显示最后一条消息
    private void displayContacts(List<Contact> contacts) {
        for (Contact contact : contacts) {
            View contactView = LayoutInflater.from(getContext()).inflate(R.layout.item_message, messageContainer, false);

            ImageView profileImage = contactView.findViewById(R.id.profile_image);
            TextView nameText = contactView.findViewById(R.id.contact_name);
            TextView messageText = contactView.findViewById(R.id.contact_message);

            profileImage.setImageResource(R.drawable.ic_profile); // 假设使用 ic_profile 作为头像
            nameText.setText(contact.getName());
            messageText.setText(contact.getLastMessage()); // 显示最后一条消息

            // 为联系人添加点击事件，进入聊天界面
            contactView.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), ChatActivity.class);
                intent.putExtra("contact_name", contact.getName());
                intent.putStringArrayListExtra("contact_messages", new ArrayList<>(contact.getMessages()));
                startActivity(intent);
            });

            messageContainer.addView(contactView);
        }
    }

    // 联系人数据类
    private static class Contact {
        private final String name;
        private final List<String> messages;

        public Contact(String name, List<String> messages) {
            this.name = name;
            this.messages = messages;
        }

        public String getName() {
            return name;
        }

        public List<String> getMessages() {
            return messages;
        }

        public String getLastMessage() {
            return messages.isEmpty() ? "" : messages.get(messages.size() - 1);
        }
    }
}
