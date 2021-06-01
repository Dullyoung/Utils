package com.dullyoung.utils.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.util.Log;


import androidx.annotation.Nullable;

import com.dullyoung.utils.helper.PermissionHelper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/*
 *  Created by Dullyoung in  2021/4/30
 */
public class WiFiUtil {
    private String TAG = "CWiFiManager";
    private Activity mContext;
    private WifiScanCallBack mWifiScanCallBack;
    private WifiManager mWifiManager;
    private PermissionHelper permissionHelper;
    private boolean callbackResult = false;

    public WiFiUtil(Activity context, WifiScanCallBack wifiScanCallBack) {
        mContext = context;
        mWifiScanCallBack = wifiScanCallBack;
        mWifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        registerScanWifiReceiver();
        permissionHelper = new PermissionHelper();
    }

    public static String getWifiIp(Context context) {
        WifiManager mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        if (ipAddress == 0) return "未连接WIFI";
        return ((ipAddress & 0xff) + "." + (ipAddress >> 8 & 0xff) + "."
                + (ipAddress >> 16 & 0xff) + "." + (ipAddress >> 24 & 0xff));
    }

    private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) {
                Log.i(TAG, "wifi列表发生变化");
                boolean success = intent.getBooleanExtra(
                        WifiManager.EXTRA_RESULTS_UPDATED, false);
                if (!callbackResult) {
                    return;
                }
                if (success) {
                    if (mWifiScanCallBack != null) {
                        mWifiScanCallBack.onScanWifiSuccess(mWifiManager.getScanResults());
                        callbackResult = false;
                    }
                } else {
                    if (mWifiScanCallBack != null) {
                        mWifiScanCallBack.onScanWifiFail();
                        callbackResult = false;
                    }
                }
            }
        }

    };

    private void registerScanWifiReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);//监听wifi列表变化（开启一个热点或者关闭一个热点）
        mContext.registerReceiver(wifiScanReceiver, intentFilter);
    }

    public void scanWifi() {
        permissionHelper.setMustPermissions2(
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION);
        permissionHelper.checkAndRequestPermission(mContext, new PermissionHelper.OnRequestPermissionsCallback() {
            @Override
            public void onRequestPermissionSuccess() {
                if (!mWifiManager.isWifiEnabled() && !mWifiManager.setWifiEnabled(true)) {
                  //  CommonUtil.tip(mContext, "请打开Wifi开关");
                    return;
                }
                mWifiManager.startScan();
                callbackResult = true;
            }

            @Override
            public void onRequestPermissionError() {

            }
        });
    }

    public void onRequestPermissionsResult(int requestCode) {
        permissionHelper.onRequestPermissionsResult(mContext, requestCode);
    }

    public interface WifiScanCallBack {
        void onScanWifiSuccess(List<ScanResult> scanResults);

        void onScanWifiFail();
    }

    public WifiManager getWifiManager() {
        return mWifiManager;
    }

    /**
     * @param ssid ssid
     * @param pwd  if no pwd can be null
     */
    public void connectWifiPwd(String ssid, @Nullable String pwd) {
        @SuppressLint("MissingPermission")
        List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();

        for (WifiConfiguration config : configs) {
            if (config.SSID.equals("\"" + ssid + "\"")) {
                mWifiManager.disconnect();
                Log.i(TAG, "removeNetwork: " + config.networkId);
                boolean success = mWifiManager.removeNetwork(config.networkId);
                Log.i(TAG, "removeNetwork: " + success);
            }
        }
        int netId = mWifiManager.addNetwork(getWifiConfig(ssid, pwd, isHasPwd(ssid)));
        Log.i(TAG, "connectWifiPwd: " + netId + "---" + pwd);
        if (netId != -1) {
            mWifiManager.enableNetwork(netId, true);
        }
    }

    public static boolean hasPwd(ScanResult scanResult) {
        String capabilities = scanResult.capabilities;
        if (!TextUtils.isEmpty(capabilities)) {
            return getWifiCipher(capabilities) != WifiCipherType.WIFICIPHER_NOPASS;
        } else {
            return false;
        }
    }

    public WifiCipherType isHasPwd(String ssid) {
        List<ScanResult> scanResults = mWifiManager.getScanResults();
        for (ScanResult scanResult : scanResults) {
            if (scanResult.SSID.equals(ssid)) {
                String capabilities = scanResult.capabilities;
                if (!TextUtils.isEmpty(capabilities)) {
                    return getWifiCipher(capabilities);
                }
            }
        }
        return WifiCipherType.WIFICIPHER_NOPASS;
    }


    public static WifiCipherType getWifiCipher(String capabilities) {
        if (capabilities.isEmpty()) {
            return WifiCipherType.WIFICIPHER_INVALID;
        } else if (capabilities.contains("WEP")) {
            return WifiCipherType.WIFICIPHER_WEP;
        } else if (capabilities.contains("WPA") || capabilities.contains("WPA2") || capabilities.contains("WPS")) {
            return WifiCipherType.WIFICIPHER_WPA;
        } else {
            return WifiCipherType.WIFICIPHER_NOPASS;
        }
    }

    public enum WifiCipherType {
        WIFICIPHER_WEP, WIFICIPHER_WPA, WIFICIPHER_NOPASS, WIFICIPHER_INVALID
    }


    private static WifiConfiguration getWifiConfig(String ssid, String password, WifiCipherType type) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + ssid + "\"";

        if (type == WifiCipherType.WIFICIPHER_NOPASS) {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        }

        if (type == WifiCipherType.WIFICIPHER_WEP) {
            config.preSharedKey = "\"" + password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        }

        if (type == WifiCipherType.WIFICIPHER_WPA) {
            config.preSharedKey = "\"" + password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;

        }

        return config;

    }

    private static WifiConfiguration isExist(WifiManager wifiManager, String ssid) {
        @SuppressLint("MissingPermission")
        List<WifiConfiguration> configs = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration config : configs) {
            if (config.SSID.equals("\"" + ssid + "\"")) {
                return config;
            }
        }
        return null;
    }


    public void release() {
        mContext.unregisterReceiver(wifiScanReceiver);
    }

    public static String getMacAddress() {
        try {
            return loadFileAsString("/sys/class/net/wlan0/address")
                    .toUpperCase().substring(0, 17);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    private static String loadFileAsString(String filePath)
            throws IOException {
        StringBuffer fileData = new StringBuffer(1000);
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        char[] buf = new char[1024];
        int numRead = 0;
        while ((numRead = reader.read(buf)) != -1) {
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
        }
        reader.close();
        return fileData.toString();
    }

}
