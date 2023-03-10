package com.transbyte.demo.ping.cronet;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.pdns.DNSResolver;

import org.chromium.net.CronetEngine;
import org.chromium.net.ExperimentalCronetEngine;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CronetManager {

    public static final String TAG = "Cronet@Manager";

    private volatile static CronetManager instance;

    private CronetManager() {
    }

    public static CronetManager getInstance() {
        if (instance == null) {
            synchronized (CronetManager.class) {
                if (instance == null) {
                    instance = new CronetManager();
                }
            }
        }
        return instance;
    }

    // We recommend that each application uses a single, global CronetEngine. This allows Cronet
    // to maximize performance. This can either be achieved using a global static . In this example,
    // we initialize it in an Application class to manage lifecycle of the network log.
    private CronetEngine cronetEngine;

    // Executor that will invoke asynchronous Cronet callbacks. Like with the Cronet engine, we
    // recommend that it's managed centrally.
    private ExecutorService cronetCallbackExecutorService;

    public void create(Context context) {
        Log.d(TAG, "create engine");
        cronetEngine = createDefaultCronetEngine(context);
        cronetCallbackExecutorService = Executors.newFixedThreadPool(4);
    }

    public CronetEngine getCronetEngine() {
        return cronetEngine;
    }

    public ExecutorService getCronetCallbackExecutorService() {
        return cronetCallbackExecutorService;
    }

    private CronetEngine createDefaultCronetEngine(Context context) {
        // Cronet makes use of modern protocols like HTTP/2 and QUIC by default. However, to make
        // the most of servers that support QUIC, one must either specify that a particular domain
        // supports QUIC explicitly using QUIC hints, or enable the on-disk cache.
        //
        // When a QUIC hint is provided, Cronet will attempt to use QUIC from the very beginning
        // when communicating with the server and if that fails, we fall back to using HTTP. If
        // no hints are provided, Cronet uses HTTP for the first request issued to the server.
        // If the server indicates it does support QUIC, Cronet stores the information and will use
        // QUIC for subsequent request to that domain.
        //
        // We recommend that QUIC hints are provided explicitly when working with servers known
        // to support QUIC.
        JSONObject experimentalOptions;
        try {
            experimentalOptions = new JSONObject().put("HostResolverRules", getHostResolverRules("storage.googleapis.com"));
        } catch (JSONException e) {
            e.printStackTrace();
            experimentalOptions = new JSONObject();
        }

        return new ExperimentalCronetEngine.Builder(context)
                .setExperimentalOptions(experimentalOptions.toString())

                // The storage path must be set first when using a disk cache.
                .setStoragePath(context.getFilesDir().getAbsolutePath())

                // Enable on-disk cache, this enables automatic QUIC usage for subsequent requests
                // to the same domain across application restarts. If you also want to cache HTTP
                // responses, use HTTP_CACHE_DISK instead. Typically you will want to enable caching
                // in full, we turn it off for this demo to better demonstrate Cronet's behavior
                // using net protocols.
                .enableHttpCache(CronetEngine.Builder.HTTP_CACHE_DISABLED, 100 * 1024)

                // HTTP2 and QUIC support is enabled by default. When both are enabled (and no hints
                // are provided), Cronet tries to use both protocols and it's nondeterministic which
                // one will be used for the first few requests. As soon as Cronet is aware that
                // a server supports QUIC, it will always attempt to use it first. Try disabling
                // and enabling HTTP2 support and see how the negotiated protocol changes! Also try
                // forcing a new connection by enabling and disabling flight mode after the first
                // request to ensure QUIC usage.
                .enableHttp2(false)
                .enableQuic(true)

                // Brotli support is NOT enabled by default.
                .enableBrotli(true)

                // One can provide a custom user agent if desired.
//                .setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_16_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.138 Safari/537.36")

                // As noted above, QUIC hints speed up initial requests to a domain. Multiple hints
                // can be added. We don't enable them in this demo to demonstrate how QUIC
                // is being used if no hints are provided.

                // .addQuicHint("storage.googleapis.com", 443, 443)
                .addQuicHint("sit-i.scooper.news", 443, 443)
                .build();
    }

    private JSONObject getHostResolverRules(String host) {
        String ip = DNSResolver.getInstance().getIPV4ByHost(host);
        if (ip != null) {
            Log.d(TAG, "lookup aliyun host:" + host + " ip: " + ip);
        }
        List<InetAddress> dnsList = null;
        try {
            dnsList = Arrays.asList(InetAddress.getAllByName(ip));
        } catch (UnknownHostException e) {
//            e.printStackTrace();
        }

        if (TextUtils.isEmpty(host) || dnsList == null) {
            return null;
        }

        StringBuilder hostRuleStr = new StringBuilder();
        //目前测试看，即使此处设置多个dnsIp， cronet默认也只是用第一个，且第一个失败后不会自动重试第二个
        for (int i = 0, size = dnsList.size(); i < size; i++) {
            InetAddress inetAddress = dnsList.get(i);
            if (inetAddress != null) {
                String hostAddress = inetAddress.getHostAddress();
                if (!TextUtils.isEmpty(hostAddress)) {
                    hostRuleStr.append("MAP ").append(host).append(" ").append(hostAddress).append(",");
                }
            }
        }
        JSONObject host_rule = null;
        try {
            host_rule = new JSONObject().put("host_resolver_rules", hostRuleStr);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return host_rule;
    }

    /**
     * Method to start NetLog to log Cronet events.
     * Find more info about Netlog here:
     * https://www.chromium.org/developers/design-documents/network-stack/netlog
     */
    public void startNetLog() {
        if (getCronetEngine() == null) return;
        File outputFile;
        try {
            outputFile = File.createTempFile("cronet", "log", CronetApplication.application.getExternalFilesDir(null));
            getCronetEngine().startNetLogToFile(outputFile.toString(), true);
        } catch (IOException e) {
            Log.e(TAG, e.toString(), e);
        }
    }

    /**
     * Method to properly stop NetLog
     */
    public void stopNetLog() {
        if (getCronetEngine() == null) return;
        getCronetEngine().stopNetLog();
    }
}
