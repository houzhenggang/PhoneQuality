package com.cattsoft.phone.quality.utils;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.UUID;

/**
 * Created by Xiaohong on 2014/5/3.
 */
public class Devices {
    public static String getSerial(Context context) {
        String serial = android.os.Build.SERIAL;
        if (TextUtils.isEmpty(serial) || "unknow".equalsIgnoreCase(serial)) {
            try {
                Class<?> c = Class.forName("android.os.SystemProperties");
                Method get = c.getMethod("get", String.class);
                serial = (String) get.invoke(c, "ro.serialno");
            } catch (Exception ignored) {
            }
            if (TextUtils.isEmpty(serial) || "unknow".equalsIgnoreCase(serial)) {
                try {
                    serial = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
                    if (TextUtils.isEmpty(serial) || "unknow".equalsIgnoreCase(serial)) {
                        serial = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
                    }
                } catch (Exception e) {
                    serial = UUID.randomUUID().toString();
                }
            }
        }
        return serial;
    }

    public static String getOperator(Context context) {
        return ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getNetworkOperatorName();
    }

    public static String getOperatorCode(Context context) {
        return ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getNetworkOperator();
    }

    public static String getOperator(int operatorCode) {
        String carrier = "未知";
        if (operatorCode == 46000 ||
                operatorCode == 46002 ||
                operatorCode == 46007) {
            carrier = "中国移动";
        } else if (operatorCode == 46001 ||
                operatorCode == 46010) {
            carrier = "中国联通";
        } else if (operatorCode == 46003) {
            carrier = "中国电信";
        }
        return carrier;
    }

    public static String getMcc(Context context) {
        try {
            return getImsi(context).substring(0, 3);
        } catch (Exception e) {
        }
        return null;
    }

    public static String getMnc(Context context) {
        try {
            return getImsi(context).substring(3);
        } catch (Exception e) {
        }
        return null;
    }

    public static String getImsi(Context context) {
        return ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getSubscriberId();
    }

    public static boolean isMultiSim(Context context) {
        // TelephonyManager.getDefault().isMultiSimEnabled()
        return false;
    }

    public static String getExternalIP() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && (inetAddress instanceof Inet4Address)) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (Exception ex) {
        }
        return "";
    }
    public static String getRealIp() {
        String localip = getExternalIP();// 本地IP，如果没有配置外网IP则返回它
        String netip = "";// 外网IP
        try {
            Enumeration<NetworkInterface> netInterfaces =
                    NetworkInterface.getNetworkInterfaces();
            InetAddress ip = null;
            boolean finded = false;// 是否找到外网IP
            while (netInterfaces.hasMoreElements() && !finded) {
                NetworkInterface ni = netInterfaces.nextElement();
                Enumeration<InetAddress> address = ni.getInetAddresses();
                while (address.hasMoreElements()) {
                    ip = address.nextElement();
                    if (!ip.isSiteLocalAddress()
                            && !ip.isLoopbackAddress()
                            && ip.getHostAddress().indexOf(":") == -1) {// 外网IP
                        netip = ip.getHostAddress();
                        finded = true;
                        break;
                    } else if (ip.isSiteLocalAddress()
                            && !ip.isLoopbackAddress()
                            && ip.getHostAddress().indexOf(":") == -1) {// 内网IP
                        localip = ip.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
        }
        if (!Strings.isNullOrEmpty(netip)) {
            return netip;
        } else {
            return localip;
        }
    }
    public static String getWifiMac(Context context) {
        try {
            WifiManager wifi = (WifiManager) context
                    .getSystemService(context.WIFI_SERVICE);
            return wifi.getConnectionInfo().getMacAddress();
        } catch (Exception e) {
            //
        }
        return null;
    }
    public static int availableProcessors() {
        File file = new File("/sys/devices/system/cpu/");
        return file.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.matches("(?i)cpu[\\d]+");
            }
        }).length;
    }
}
