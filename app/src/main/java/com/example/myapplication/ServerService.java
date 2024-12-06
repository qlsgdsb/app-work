package com.example.myapplication;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.IBinder;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ServerService extends Service {
    private static final String TAG = "ServerService";
    private MockWebServer server;
    private SQLiteDatabase db;

    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化数据库
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        db = dbHelper.getWritableDatabase();
        startServer();
    }

    private void startServer() {
        new Thread(() -> {
            try {
                server = new MockWebServer();
                server.setDispatcher(new Dispatcher() {
                    @Override
                    public MockResponse dispatch(RecordedRequest request) {
                        String path = request.getPath();
                        if ("/chat".equals(path)) {
                            return handleChatRequest(request);
                        }

                        // 其他路径的处理保持不变
                        return defaultResponse(request);
                    }
                });
                server.start(7891);
                Log.d(TAG, "服务器启动在端口: 7891");
            } catch (IOException e) {
                Log.e(TAG, "服务器启动失败: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private MockResponse handleChatRequest(RecordedRequest request) {
        try {
            String requestBody = request.getBody().readString(StandardCharsets.UTF_8);
            Log.d(TAG, "收到chat请求体: " + requestBody);

            //消息示例为"["张三","你好啊"]"
            JSONArray jsonArray = new JSONArray(requestBody);
            if (jsonArray.length() == 2) {
                String name = jsonArray.getString(0);
                String context = jsonArray.getString(1);

                try {
                    // 尝试在contact表中查找联系人
                    long contactId = findContact(name);
                    if (contactId != -1) {
                        // 找到联系人，将消息存入message表
                        saveMessage(contactId, context);
                        return new MockResponse()
                                .setResponseCode(200)
                                .setHeader("Content-Type", "application/json; charset=utf-8")
                                .setBody("{\"status\":\"success\",\"message\":\"消息已保存\"}");
                    }
                } catch (SQLiteException e) {
                    Log.e(TAG, "数据库错误: " + e.getMessage());
                    // 数据库错误（如表不存在）时返回相应消息
                    return new MockResponse()
                            .setResponseCode(404)
                            .setHeader("Content-Type", "application/json; charset=utf-8")
                            .setBody("{\"status\":\"error\",\"message\":\"联系人 " + name + " 不存在\"}");
                }

                // 未找到联系人
                return new MockResponse()
                        .setResponseCode(404)
                        .setHeader("Content-Type", "application/json; charset=utf-8")
                        .setBody("{\"status\":\"error\",\"message\":\"未找到联系人: " + name + "\"}");
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSON解析错误: " + e.getMessage());
            return new MockResponse()
                    .setResponseCode(400)
                    .setHeader("Content-Type", "application/json; charset=utf-8")
                    .setBody("{\"status\":\"error\",\"message\":\"无效的JSON格式\"}");
        }

        return new MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json; charset=utf-8")
                .setBody("{\"status\":\"error\",\"message\":\"请求格式错误\"}");
    }


    private long findContact(String name) {
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_CONTACTS,
                new String[]{DatabaseHelper.COLUMN_ID},
                DatabaseHelper.COLUMN_NAME + " = ?",
                new String[]{name},
                null, null, null
        );

        long contactId = -1;
        if (cursor != null && cursor.moveToFirst()) {
            contactId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID));
            cursor.close();
        }
        return contactId;
    }


    private void saveMessage(long contactId, String message) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_CONTACT_ID, contactId);
        values.put(DatabaseHelper.COLUMN_MESSAGE, message);
        values.put(DatabaseHelper.COLUMN_IS_FROM_ME, false);
        values.put(DatabaseHelper.COLUMN_TIMESTAMP, System.currentTimeMillis());

        db.insert(DatabaseHelper.TABLE_MESSAGES, null, values);
    }

    private MockResponse defaultResponse(RecordedRequest request) {
        Log.d(TAG, "收到新请求 ========================");
        Log.d(TAG, "请求方法: " + request.getMethod());
        Log.d(TAG, "请求路径: " + request.getPath());
        Log.d(TAG, "请求头: " + request.getHeaders());
        Log.d(TAG, "请求体: " + request.getBody().readUtf8());
        Log.d(TAG, "请求协议: " + request.getRequestLine());
        Log.d(TAG, "远程地址: " + request.getRequestUrl());
        Log.d(TAG, "================================");

        return new MockResponse()
                .setResponseCode(200)
                .setBody("已收到请求：" + request.getPath());
    }

    @Override
    public void onDestroy() {
        if (server != null) {
            try {
                server.shutdown();
                Log.d(TAG, "服务器已关闭");
            } catch (IOException e) {
                Log.e(TAG, "服务器关闭失败: " + e.getMessage());
            }
        }
        if (db != null) {
            db.close();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
