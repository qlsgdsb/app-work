package com.example.myapplication;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import java.io.IOException;

public class ServerService extends Service {
    private static final String TAG = "ServerService";
    private MockWebServer server;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startServer();
    }

    private void startServer() {
        new Thread(() -> {
            try {
                server = new MockWebServer();
                server.setDispatcher(new Dispatcher() {
                    @Override
                    public MockResponse dispatch(RecordedRequest request) {
                        // 打印请求的详细信息
                        Log.d(TAG, "收到新请求 ========================");
                        Log.d(TAG, "请求方法: " + request.getMethod());
                        Log.d(TAG, "请求路径: " + request.getPath());
                        Log.d(TAG, "请求头: " + request.getHeaders());

                        // 打印请求体
                        String requestBody = request.getBody().readUtf8();
                        Log.d(TAG, "请求体: " + requestBody);

                        // 打印请求的其他信息
                        Log.d(TAG, "请求协议: " + request.getRequestLine());
                        Log.d(TAG, "远程地址: " + request.getRequestUrl());
                        Log.d(TAG, "================================");

                        // 返回响应
                        return new MockResponse()
                                .setResponseCode(200)
                                .setBody("已收到请求：" + request.getPath());
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

    @Override
    public void onDestroy() {
        if (server != null) {
            try {
                server.shutdown();
                Log.d(TAG, "服务器已关闭");
            } catch (IOException e) {
                Log.e(TAG, "服务器关闭失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }
}
