package com.transbyte.demo.ping;

import android.net.DnsResolver;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.pdns.DNSResolver;
import com.transbyte.demo.ping.cronet.CronetManager;
import com.transbyte.demo.ping.cronet.ReadToMemoryCronetCallback;

import org.chromium.net.CronetException;
import org.chromium.net.UrlRequest;
import org.chromium.net.UrlResponseInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Dns;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "CronetDemo";

    Button pingBtn;
    TextView pingText;

    String[] testUrl = new String[]{
            "https://storage.googleapis.com/cronet/sun.jpg",
            "https://sit-i10000.scooper.news/s/push/health",
            "https://223.5.5.5/resolve?name=www.taobao.com&type=A"
    };

    OkHttpClient okHttpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pingBtn = findViewById(R.id.ping_btn);
        pingText = findViewById(R.id.ping_text);

        okHttpClient = new OkHttpClient.Builder()
                .dns(new Dns() {
                    @Override
                    public List<InetAddress> lookup(String hostname) throws UnknownHostException {
                        String ip = DNSResolver.getInstance().getIPV4ByHost(hostname);
                        if (ip != null) {
                            Log.d(TAG, "lookup aliyun host:" + hostname + " ip: " + ip);
                            return Arrays.asList(InetAddress.getAllByName(ip));
                        }
                        Log.d(TAG, "lookup system host: " + hostname);
                        return Dns.SYSTEM.lookup(hostname);
                    }
                })
                .build();

        pingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestByCronet(testUrl[0]);
//                requestByOkHttp(testUrl[0]);
            }
        });
    }

    private void requestByCronet(String url) {
        StringBuffer logBuffer = new StringBuffer(new Date(System.currentTimeMillis()).toString() + "\n");
        UrlRequest.Builder builder = CronetManager.getInstance().getCronetEngine()
                .newUrlRequestBuilder(url,
                        new ReadToMemoryCronetCallback() {
                            @Override
                            public void onSucceeded(UrlRequest request, UrlResponseInfo info, byte[] bodyBytes, long latencyNanos) {
                                logBuffer
                                        .append("UrlRequest:\n")
                                        .append(info.getUrl()).append("\n")
                                        .append("UrlResponseInfo:\n")
                                        .append(info.getNegotiatedProtocol()).append("\n")
                                        .append("\n");
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        pingText.setText(logBuffer.toString());
                                    }
                                });
                            }

                            @Override
                            public void onFailed(UrlRequest request, UrlResponseInfo info, CronetException error) {
                                super.onFailed(request, info, error);
                                logBuffer
                                        .append("UrlRequest:\n")
                                        .append(request.toString())
                                        .append("\n")
                                        .append("CronetException:\n")
                                        .append(error.toString()).append("\n")
                                        .append("\n");
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        pingText.setText(logBuffer.toString());
                                    }
                                });
                            }

                            @Override
                            public void onCanceled(UrlRequest request, UrlResponseInfo info) {
                                super.onCanceled(request, info);
                                logBuffer.append("onCanceled\n");
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        pingText.setText(logBuffer.toString());
                                    }
                                });
                            }
                        },
                        CronetManager.getInstance().getCronetCallbackExecutorService());
        builder.build().start();
    }

    private void requestByOkHttp(String url) {
        StringBuffer logBuffer = new StringBuffer(new Date(System.currentTimeMillis()).toString() + "\n");

        try {
            logBuffer
                    .append("UrlRequest:\n")
                    .append(url).append("\n");
            Request request = new Request.Builder()
                    .url(new URL(url))
                    .get()
                    .build();
            Call call = okHttpClient.newCall(request);

            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    logBuffer
                            .append("UrlResponseInfo:\n")
                            .append(e.toString()).append("\n")
                            .append("\n");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pingText.setText(logBuffer.toString());
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    logBuffer
                            .append("UrlResponseInfo:\n")
                            .append(response.code()).append("\n")
                            .append(response.message()).append("\n")
                            .append(response.protocol()).append("\n");
                    Headers headers = response.headers();
                    for (int i = 0; i < headers.size(); i++) {
                        logBuffer.append(headers.name(i)).append("\t").append(headers.value(i)).append("\n");
                    }
                    logBuffer.append("\n");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pingText.setText(logBuffer.toString());
                        }
                    });
                }
            });

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
}