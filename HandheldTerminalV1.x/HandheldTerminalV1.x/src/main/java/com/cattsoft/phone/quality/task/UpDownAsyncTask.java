package com.cattsoft.phone.quality.task;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import com.cattsoft.phone.quality.R;
import com.cattsoft.phone.quality.task.common.SpeedRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Random;

/**
 * Created by yushiwei on 15-5-13.
 */
public class UpDownAsyncTask extends AsyncTask<Integer, Integer, SpeedRecord> {

    public static final int DOWNLOAD = 0;

    public static final int UPLOAD = 1;

    public static final int DELAY = 2;

    public static final String TAG = UpDownAsyncTask.class.getSimpleName();

    private static final String BOUNDARY = "00content0boundary00";

    private static final String CONTENT_TYPE_MULTIPART = "multipart/form-data; boundary=" + BOUNDARY;

    private static final String CRLF = "\r\n";

    private long lastTime;

    private SpeedRecord speedRecord;

    private String ip;

    private int checkTimeout = 3000;

    private int cycle = 50;

    private int testTime = 15000;

    private State state;

    private long totalStart;

    private long transfered;

    private int unit = 1;

    private String downloadUrl;

    private String uploadUrl;

    private String checkUrl;

    private int readTimeout;

    private int connTimeout;

    private int delay;

    private Context context;

    private Exception exception;

    public UpDownAsyncTask(String ip, Context context) {
        this.ip = ip;
        this.context = context;

        this.downloadUrl = parseUrl(R.string.bandwidth_download_url_template, ip);
        this.uploadUrl = parseUrl(R.string.bandwidth_upload_url_template, ip);
        this.checkUrl = parseUrl(R.string.bandwidth_check_url_template, ip);
        this.readTimeout = context.getResources().getInteger(R.integer.bandwidth_read_timeout);
        this.connTimeout = context.getResources().getInteger(R.integer.bandwidth_conn_timeout);
        speedRecord = new SpeedRecord();
        setState(State.NOTBEGIN);
    }

    private String parseUrl(int r, String ip) {
        String template = context.getResources().getString(r);
        return template.replace("${ip}", ip);
    }

    private void init() {

    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
    }

    public boolean checkServer() throws IOException {
        setState(State.CHECKING);
        HttpURLConnection connection = null;
        try {
            connection = connectionDownload();
            if (connection.getResponseCode() != 200) {
                return false;
            }
        } catch (IOException e) {
            throw e;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return true;
    }

    @Override
    public SpeedRecord doInBackground(Integer... params) {
        try {
            init();
            if (params == null || params.length == 0) {
                throw new IllegalArgumentException("缺少参数,无法执行任务");
            }
            if (params[0] == DOWNLOAD) {
                checkServer();
                download();
            } else if (params[0] == UPLOAD) {
                checkServer();
                upload();
            } else if (params[0] == DELAY) {
                delay();
            }
        } catch (SocketTimeoutException e) {
            whenError(e);
        } catch (IOException e) {
            whenError(e);
        } catch (RuntimeException e) {
            whenError(e);
        }
        return speedRecord;
    }

    public void whenError(Exception e) {
        setState(State.ERROR);
        Log.e(TAG, "任务失败！", e);
        exception = e;
    }

    public int getDelay() {
        return delay;
    }

    private void delay() throws IOException {
        HttpURLConnection connection = null;
        try {
            long start = System.currentTimeMillis();
            connection = connectionDelay();
            long end = System.currentTimeMillis();
            this.delay = new Double(end - start).intValue();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void upload() throws IOException {
        setState(State.PROCESSING);
        OutputStream os = null;
        HttpURLConnection connection = null;
        byte[] data = createFakeData(1024 * unit);
        try {
            connection = connectUpload();
            os = connection.getOutputStream();
            sendUploadRequestHead(os);

            totalStart = System.currentTimeMillis();
            lastTime = System.currentTimeMillis();
            while (true) {
                os.write(data);
                if (record(data.length)) {
                    break;
                }
            }
            Log.i(TAG, "共上传: " + transfered + " byte, 用时: " + (System.currentTimeMillis() - totalStart) + " ms");
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (Exception e) {
                Log.w(TAG, "", e);
            }
        }
    }

    private void sendUploadRequestHead(OutputStream os) throws IOException {
        os.write(("--" + BOUNDARY + CRLF).getBytes());
        os.write(("Content-Disposition: form-data; name=\"upload\";filename=\"file.fake\"" + CRLF).getBytes());
        os.write(CRLF.getBytes());
    }
    private HttpURLConnection connectionDelay() throws IOException {
        HttpURLConnection connection = getGetConnection(checkUrl);
        connection.connect();
        return connection;
    }

    private HttpURLConnection getGetConnection(String checkUrl) throws IOException {
        URL urlC = new URL(checkUrl);
        HttpURLConnection connection = (HttpURLConnection) urlC.openConnection();
        connection.setRequestProperty("Accept-Encoding", "identity");
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setReadTimeout(readTimeout);
        connection.setConnectTimeout(connTimeout);
        connection.setInstanceFollowRedirects(true);
        return connection;
    }

    private HttpURLConnection connectionDownload() throws IOException {
        HttpURLConnection connection = getGetConnection(downloadUrl);
        connection.connect();

        return connection;
    }

    private HttpURLConnection connectUpload() throws IOException {
        URL urlC = new URL(uploadUrl);
        HttpURLConnection connection = (HttpURLConnection) urlC.openConnection();
        connection.setReadTimeout(readTimeout);
        // 发送POST请求必须设置如下两行
        // Allow Inputs
        connection.setDoInput(true);
        // Allow Outputs
        connection.setDoOutput(true);
        // Don't use a cached copy.
        connection.setUseCaches(false);
        connection.setDefaultUseCaches(false);
        // Use a post method.
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + BOUNDARY);
        // 设置每次传输的流大小，可有效防止因内存不足崩溃
        // 用于在预先不知道内容长度时启用没有进行内部缓冲的 HTTP 请求正文的流。适用于大数据传输
        connection.setChunkedStreamingMode(32);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("Charsert", "UTF-8");
        connection.setRequestProperty("Content-Type", CONTENT_TYPE_MULTIPART);

        return connection;
    }

    private byte[] createFakeData(int size) {
        byte[] data = new byte[size];
        Random rd = new Random();
        rd.nextBytes(data);

        return data;
    }

    private void download() throws IOException {
        totalStart = System.currentTimeMillis();
        setState(State.PROCESSING);
        while (true) {
            downloadSingleFile();
            if (getState() == State.BREAK) {
                break;
            }
        }
        Log.i(TAG, "退出循环, 共下载: " + transfered + " byte, 用时: " + (System.currentTimeMillis() - totalStart) + " ms");
        setState(State.SUCCEED);
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public SpeedRecord getSpeedRecord() {
        return speedRecord;
    }

    private void downloadSingleFile() throws IOException {
        HttpURLConnection connection = null;
        InputStream is = null;
        try {
            long connStart = System.currentTimeMillis();
            connection = connectionDownload();
            if (connection.getResponseCode() != 200) {
                throw new IOException("Response Code: " + connection.getResponseCode());
            }
            is = connection.getInputStream();
            long connEnd = System.currentTimeMillis();

            // 刨除连接时间,让程序测满时间,同时也为显示速率提供更加平滑的曲线,让最终计算结果趋近正确值.
            totalStart -= connEnd - connStart;

            lastTime = System.currentTimeMillis();
            int size = 0;

            byte[] buffer = new byte[1024 * unit];
            while ((size = is.read(buffer)) > 0) {
                if (record(size)) {
                    break;
                }
            }
        } finally {
            try {
                if (is != null) {
                    long start = System.currentTimeMillis();
                    is.close();
                    Log.i(TAG, "关闭时间: " + (System.currentTimeMillis() - start) + "ms");
                }
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (Exception e) {
                Log.w(TAG, "", e);
            }

        }
    }

    private boolean record(int size) {
        transfered += size;

        long now = System.currentTimeMillis();

        if (now - lastTime >= cycle) {
            speedRecord.addRecord(now - totalStart, transfered);
            lastTime = now;
        }

        if (now - totalStart >= testTime) {
            setState(State.BREAK);
            return true;
        }

        return false;
    }

    public int getTestTime() {
        return testTime;
    }

    public void setTestTime(int testTime) {
        this.testTime = testTime;
    }

    public int getCycle() {
        return cycle;
    }

    public void setCycle(int cycle) {
        this.cycle = cycle;
    }

    public int getUnit() {
        return unit;
    }

    public void setUnit(int unit) {
        this.unit = unit;
    }

    public long getTotalStart() {
        return totalStart;
    }

    public static enum State {ERROR, BREAK, SUCCEED, PROCESSING, CHECKING, NOTBEGIN}
}