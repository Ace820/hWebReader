package com.zhangche.hnovelreader;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private final String host = "https://cl.nx53.xyz";
    private final String novelUrl = host + "/thread0806.php?fid=20&page=";
    private final String infoUrl = host + "/thread0806.php?fid=7&page=";
    private final int novelPage = 30;
    private final int infoPage = 330;
    public String targetUrl = infoUrl;
    public int targetPage = infoPage;
    boolean updateList = true;
    int threadCount = 0;
    static final String TAG = "zhangche";
//    ThreadPoolExecutor threadPool;
    ArrayList<NovelItemInfo> list = getSavedList();
    ArrayList<NovelItemInfo> failItem = new ArrayList<>();
    int parsedCount = 0;
    int i = 0;
    boolean isInBackground = false;
    Thread mainThread;
    Handler handler;
    TextView textView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
        setContentView(R.layout.novel_note);
        textView = findViewById(R.id.novelText);
//        threadPool = new ThreadPoolExecutor(
//                3,10,10, TimeUnit.SECONDS,
//                new ArrayBlockingQueue<Runnable>(3));
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
//                Log.i(TAG,"get Message " + msg.what);
                if (msg.what == 0x10) {
                    textView.setText((String)msg.obj);
//                    Bundle bundle = msg.getData();

                } else if (msg.what == 0x11) { //pass
                }
            }

        };
        mainThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                if (!initDirs()) {
                    Toast.makeText(MainActivity.this,"can not write to file",Toast.LENGTH_LONG).show();
                    Log.e(TAG,"init dir failed");
                    return;
                }
                if (updateList){
//                    list.addAll(new httpParser().getNovelList("https://cl.nx53.xyz/thread0806.php?fid=20"));
                    for (i = 1; i < targetPage; i++) {
                        list.addAll(new httpParser().getNovelList(targetUrl + i));
                    }

                    Log.d(TAG, "total size is " + list.size());
                    stripList();
                    Log.d(TAG, "total size is " + list.size());
                    String temp = "";
                    for (NovelItemInfo tmpNovel : list) {
                        if (!isNovelDownloaded(tmpNovel.name))
                            temp += tmpNovel.name + "," + tmpNovel.text + "\n";
                    }
                    save2File(temp, "/sdcard/novels/list.csv");
                    return;
                }

                for (NovelItemInfo novelItemInfo : list) {
                    parsedCount++;
                    if (isNovelDownloaded(novelItemInfo.name)) {
                        Log.i(TAG,novelItemInfo.name + ", ignored, " + parsedCount + "/" + list.size());
                        continue;
                    } else {
                        Log.i(TAG,novelItemInfo.name + "," + parsedCount + "/" + list.size());
                    }
                    Message msg = new Message();
                    msg.what = 0x10;
                    msg.obj = parsedCount + "/" + list.size();
                    handler.sendMessage(msg);
                    while (isInBackground || threadCount >= 3) {
                        try {
                            Thread.sleep(100);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            threadCount++;
                            try {
                                downloadFile(novelItemInfo);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            threadCount--;
                        }
                    }).start();

                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        isInBackground = true;
        Toast.makeText(MainActivity.this,"set to background",Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mainThread.isAlive()) {
            Log.i(TAG,"main thread start now");
            mainThread.start();
        }
        isInBackground = false;
        Toast.makeText(MainActivity.this,"away from background",Toast.LENGTH_SHORT).show();
    }

    public ArrayList<NovelItemInfo> getSavedList(){
        ArrayList<NovelItemInfo> savedList = new ArrayList<>();
        String buf = "";
        try {
            FileReader fileReader = new FileReader("/sdcard/novels/list.csv");
            BufferedReader bf = new BufferedReader(fileReader);
            while ((buf = bf.readLine()) != null) {
                savedList.add(new NovelItemInfo(buf.split(",")[0],buf.split(",")[1]));
            }
            fileReader.close();
            bf.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return savedList;
    }
//    save to download file
    public void downloadFile(NovelItemInfo novelItemInfo) {
//        if (parsedItem.contains(novelItemInfo.text)) {
        if (isNovelDownloaded(novelItemInfo.name)) {
            return;
        }
        NovelItemInfo novel = new httpParser().getNovelInfo(novelItemInfo.text);
        if ((novel.text.length() > 100) && novel.name.equals(novelItemInfo.name)) {
            save2File(novel.name + "\n\n" + novel.text,"/sdcard/novels/"+ novel.name + ".txt");
//            append2File(novelItemInfo.text,"/sdcard/novels/downloadList.csv",true);
        } else {
            Log.e(TAG,"get novel ["+novelItemInfo.name + "] failed,length is " + novel.text.length());
            failItem.add(novelItemInfo);
            append2File(novelItemInfo.name + "," + novelItemInfo.text + "\n","/sdcard/novels/failList.csv",true);
        }
    }
    public void stripList() {
        HashMap<String,String> map = new HashMap<>();
        for (NovelItemInfo item : list) {
            if (map.containsKey(item.text) && map.get(item.text).length() >= item.name.length()) {
                continue;
            }
            map.put(item.text,item.name);
        }
        list.clear();
        for (HashMap.Entry<String,String> entry : map.entrySet()) {
            list.add(new NovelItemInfo(entry.getValue(),entry.getKey()));
        }
    }
    public boolean isNovelDownloaded(String novelName) {
        File dir = new File("/sdcard/novels");
        if (!dir.isDirectory()) {
            return false;
        }
        for (String name :dir.list()) {
            if (name.equals(novelName + ".txt")) {
                return true;
            }
        }
        return false;
    }
    public boolean initDirs() {
        File dir = new File("/sdcard/novels");
        if (!dir.exists()) {
            dir.mkdir();
        } else if (dir.isFile()) {
            return false;
        }

        boolean result = false;
        File testFile = new File("/sdcard/novels/test.txt");
        try {
            testFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        result = testFile.exists();
        testFile.delete();
        return result;
    }
    public static void save2File(String str,String filePath) {
        append2File(str,filePath,false);
    }
    public static void append2File(String str,String filePath,boolean isAppend) {
        try {
            PrintWriter pw = new PrintWriter(new FileWriter(filePath,isAppend));
            pw.print(str);
            pw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    protected class downloadThread extends Thread {
        public downloadThread(String url) {

        }
        @Override
        public void run() {
            super.run();
        }
    }
}
