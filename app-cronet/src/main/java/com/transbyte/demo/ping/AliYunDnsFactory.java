package com.transbyte.demo.ping;

import android.content.Context;

import com.alibaba.pdns.DNSResolver;

public class AliYunDnsFactory {

    // 阿里云控制台接入sdk的accountId
    private final String accountID = "";
    // accessKey
    private final String accessKey = "";
    // accessSecret
    private final String accessSecret = "";
    // 设置缓存域名的最大个数，默认为100
    private final int CACHE_MAX_NUMBER = 100;
    // 设置否定缓存的最大TTL时间，默认为30秒
    private final int MAX_NEGATIVE_CACHE = 30;
    // 设置缓存的最大TTL时间，默认为3600秒
    private final int MAX_TTL_CACHE = 1 * 60 * 60;

    public void init(Context context) {
        //设置控制台接入sdk的accountId
        DNSResolver.Init(context, accountID);
        // 设置控制台接入sdk的accessKeyId
        DNSResolver.setAccessKeyId(accessKey);
        // 设置控制台接入sdk的accessKeySecret
        DNSResolver.setAccessKeySecret(accessSecret);
        // 设置是否开启short模式，默认不开启(阿里公共DNS的DoH JSON API返回数据类型分为全量JSON和简要IP数组格式)
        DNSResolver.setEnableShort(false);
        // 设置是否开启IPV6的访问模式，默认不开启
        DNSResolver.setEnableIPv6(false);
        // 设置是否开启使用缓存，默认开启
//        DNSResolver.setEnableCache(dnsConfig.enableCache);
        DNSResolver.setEnableCache(false);
        // 设置是否开启定时主动更新过期缓存 (SDK会每分钟主动更新过期缓存一次, 可能会带来域名解析次数和客户端网络流量消耗的增加)
        DNSResolver.setEnableSchedulePrefetch(false);
        // 设置缓存的最大TTL时间，默认为3600秒
        DNSResolver.setMaxTtlCache(MAX_TTL_CACHE);
        // 设置否定缓存的最大TTL时间，默认为30秒
        DNSResolver.setMaxNegativeCache(MAX_NEGATIVE_CACHE);
        // 设置访问服务端协议 http还是https模式
//        DNSResolver.setSchemaType(BuildConfig.APP_MODE == 0 ? DNSResolver.HTTPS : DNSResolver.HTTP);
        DNSResolver.setSchemaType(DNSResolver.HTTPS);//sdk 内部也使用了Cronet，请求HTTP会出错
        // 设置缓存域名的最大个数，默认为100
        DNSResolver.getInstance().setMaxCacheSize(CACHE_MAX_NUMBER);
        DNSResolver.setEnableLogger(true);
    }
}
