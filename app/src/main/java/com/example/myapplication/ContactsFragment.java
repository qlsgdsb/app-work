package com.example.myapplication;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class ContactsFragment extends Fragment {
    private RecyclerView recyclerView;
    private ContactsAdapter adapter;
    private DatabaseHelper dbHelper;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);

        recyclerView = view.findViewById(R.id.contacts_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        dbHelper = new DatabaseHelper(getContext());

        // 加载联系人数据
        List<Contact> contacts = loadContacts();
        adapter = new ContactsAdapter(contacts);
        recyclerView.setAdapter(adapter);

        return view;
    }

    private List<Contact> loadContacts() {
        List<Contact> contacts = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] projection = {
                DatabaseHelper.COLUMN_ID,
                DatabaseHelper.COLUMN_NAME
        };

        try (Cursor cursor = db.query(
                DatabaseHelper.TABLE_CONTACTS,
                projection,
                null,
                null,
                null,
                null,
                DatabaseHelper.COLUMN_NAME + " ASC"
        )) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME));
                contacts.add(new Contact(id, name));
            }
        }

        return contacts;
    }

    // 获取最后一条消息
    private String getLastMessage(long contactId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String lastMessage = "";

        String[] projection = {DatabaseHelper.COLUMN_MESSAGE};
        String selection = DatabaseHelper.COLUMN_CONTACT_ID + " = ?";
        String[] selectionArgs = {String.valueOf(contactId)};
        String sortOrder = DatabaseHelper.COLUMN_TIMESTAMP + " DESC LIMIT 1";

        try (Cursor cursor = db.query(
                DatabaseHelper.TABLE_MESSAGES,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        )) {
            if (cursor.moveToFirst()) {
                lastMessage = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MESSAGE));
            }
        }

        return lastMessage.isEmpty() ? "暂无消息" : lastMessage;
    }

    private class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ViewHolder> {
        private List<Contact> contacts;

        public ContactsAdapter(List<Contact> contacts) {
            this.contacts = contacts;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_contact, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Contact contact = contacts.get(position);
            holder.nameTextView.setText(contact.getName());
            holder.lastMessageTextView.setText(getLastMessage(contact.getId()));

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), ChatActivity.class);
                try {
                    intent.putExtra("contact_name", URLEncoder.encode(contact.getName(), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                intent.putExtra("contact_id", contact.getId());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return contacts.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView avatarImageView;
            TextView nameTextView;
            TextView lastMessageTextView;

            ViewHolder(View itemView) {
                super(itemView);
                avatarImageView = itemView.findViewById(R.id.avatarImageView);
                nameTextView = itemView.findViewById(R.id.nameTextView);
                lastMessageTextView = itemView.findViewById(R.id.lastMessageTextView);
            }
        }
    }

    // Contact 数据类
    private static class Contact {
        private final long id;
        private final String name;

        public Contact(long id, String name) {
            this.id = id;
            this.name = name;
        }

        public long getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}
