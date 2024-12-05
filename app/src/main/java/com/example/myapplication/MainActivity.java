package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.view.MenuItem;
import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 123;

    private final BottomNavigationView.OnNavigationItemSelectedListener navListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    Fragment selectedFragment = null;
                    int id = item.getItemId();

                    if (id == R.id.nav_home) {
                        selectedFragment = new HomeFragment();
                    } else if (id == R.id.nav_chats) {
                        selectedFragment = new ContactsFragment();
                    } else if (id == R.id.nav_status) {
                        selectedFragment = new FindFragment();
                    } else if (id == R.id.nav_profile) {
                        selectedFragment = new ProfileFragment();
                    }

                    if (selectedFragment != null) {
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, selectedFragment)
                                .commit();
                    }
                    return true;
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 检查并请求权限
        checkAndRequestPermissions();

        // 输出设备信息
        logDeviceInfo();

        // 启动服务器
        startService(new Intent(this, ServerService.class));

        // 设置底部导航
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(navListener);

        // 默认显示 HomeFragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }
    }

    private void checkAndRequestPermissions() {
        String[] permissions = {
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_WIFI_STATE
        };

        boolean needRequest = false;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                needRequest = true;
                break;
            }
        }

        if (needRequest) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
    }

    private void logDeviceInfo() {
        try {
            // 获取 WiFi IP 地址
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                int ipAddress = wifiManager.getConnectionInfo().getIpAddress();

                // 转换 IP 地址格式
                if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
                    ipAddress = Integer.reverseBytes(ipAddress);
                }

                byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();
                InetAddress address = InetAddress.getByAddress(ipByteArray);
                String ipAddressString = address.getHostAddress();

                Log.d(TAG, "WiFi IP Address: " + ipAddressString);
                Log.d(TAG, "Server URL: http://" + ipAddressString + ":7891");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error getting IP address", e);
        }

        // 输出设备基本信息
        Log.d(TAG, "Device Model: " + android.os.Build.MODEL);
        Log.d(TAG, "Android Version: " + android.os.Build.VERSION.RELEASE);
        Log.d(TAG, "API Level: " + android.os.Build.VERSION.SDK_INT);
        Log.d(TAG, "Manufacturer: " + android.os.Build.MANUFACTURER);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                logDeviceInfo();
            } else {
                Log.w(TAG, "Some permissions were denied");
                // 这里可以添加处理权限被拒绝的逻辑
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 停止服务
        stopService(new Intent(this, ServerService.class));
    }
}
