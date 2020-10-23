package com.zhangche.hnovelreader;

import android.os.Message;
import android.util.Log;

import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class httpParser {
    private final String host = "https://cl.nx53.xyz";
    public ArrayList<NovelItemInfo> getNovelList(String url) {
        String[] webText = getHtml(url).split("\n");
        Log.d("zhangche",url + " get size " + webText.length);
        ArrayList<NovelItemInfo> novels = new ArrayList<NovelItemInfo>();
        for (String line : webText) {
//            for (int index = 0; index < webText.length; index ++) {
            if (line.contains("論壇公告"))
                continue;
            if (line.trim().startsWith("<h3>")) {
                String name = line.split("\"")[6];
                name = name.substring(1,name.indexOf("</"));
                name = name.substring(name.lastIndexOf(">") + 1);
                String urlText = host + "/" + line.split("\"")[1];
                novels.add(new NovelItemInfo(name,urlText));
                Log.d("zhangche",name + "," + urlText);
            }
        }
        return novels;
    }
    public NovelItemInfo getNovelInfo(String url) {
        String rawWeb = getHtml(url);
        String[] webText = rawWeb.split("\n");
        boolean isReady = false;
        NovelItemInfo novel = new NovelItemInfo();
        for (String line : webText) {
//            for (int index = 0; index < webText.length; index ++) {
            if (isReady && line.contains("tpc_content do_not_catch")) {
                String[] novelText = line.replace("</p>","").replace("<br>","\n").split(">");
                for (String str : novelText) {
                    if (str.length() > 500) {
                        novel.text += str.replace("</span","") + "\n";
                    }
                }
//                novel.text = novelText[4].replace("</span","");
//                break;
            }
            if (line.contains("<h4>")) {
                isReady = true;
                novel.name = line.substring(4,line.lastIndexOf('<'));
            }
        }
        if (novel.text.length() < 500) {
            com.zhangche.hnovelreader.MainActivity.save2File(rawWeb,"/sdcard/novels/raw/" + (novel.name.equals("")?url.split("/")[6]:novel.name));
            Log.e("zhangche","web size is " + rawWeb.length());
        }
        return novel;
    }
    public String getHtml(String url) {
        String result = "";
        Log.d("zhangche","connecting " + url);
            try {
                OkHttpClient okHttpClient;
                okHttpClient = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(url)
//                        .removeHeader("User-Agent")
//                        .addHeader("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:80.0) Gecko/20100101 Firefox/80.0")
//                        .addHeader("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
//                        .addHeader("Accept-Language","zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2")
//                        .addHeader("Accept-Encoding","gzip, deflate, br")
//                        .addHeader("DNT","1")
//                        .addHeader("Connection","keep-alive")
//                        .addHeader("Cookie","__cfduid=df6f22f1784079f2524c8b0d431a06a801603196303")
//                        .addHeader("Upgrade-Insecure-Requests","1")
//                        .addHeader("If-Modified-Since","Fri, 09 Oct 2020 07:19:27 GMT")
//                        .addHeader("Cache-Control","max-age=0")
//                        .addHeader("TE","Trailers")
                        .build();

//                for (String headerName : request.headers().names()) {
//                      Log.i("zhangche",headerName + "|is |" + request.header(headerName) + "|");
//                }
                Response response = okHttpClient.newCall(request).execute();
//                result = response.body().string();
                result = new String(response.body().bytes(),"GB2312");
            } catch (Exception e) {
                e.printStackTrace();
            }
        return result;
    }
}
