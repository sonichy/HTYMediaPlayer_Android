package com.hty.mediaplayer;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.format.Formatter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

public class MainActivity extends Activity {

    PowerManager.WakeLock mWakeLock;
    VideoView videoView;
    ListView listViewClass, listViewTV;
    MediaController MC;
    MediaMetadataRetriever MMR;
    Uri uric;
    int itvc, currentPosition, width, height;

    String[] listClassStr = { "央视频道", "卫视频道", "其他频道" };
    String[] TVClass, TVItems;
    String source = "";
    boolean seekable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        videoView = (VideoView) findViewById(R.id.videoView);
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                width = mp.getVideoWidth();
                height = mp.getVideoHeight();
            }
        });
        listViewClass = (ListView) findViewById(R.id.listViewClass);
        listViewTV = (ListView) findViewById(R.id.listViewTV);

        MC = new MediaController(MainActivity.this);
        videoView.setMediaController(MC);

        int id = Resources.getSystem().getIdentifier("media_controller", "layout", "android");
        //int id = Resources.getSystem().getIdentifier("time_current", "id", "android");
//        Log.e(Thread.currentThread().getStackTrace()[2] + "", "MediaControllerId: " + id);
//        TextView currentTimeTextView = (TextView) MC.findViewById(id);
//        currentTimeTextView.setTextColor(Color.RED);

        //ProgressBar progressBar = (ProgressBar) view.findViewById(com.android.internal.R.id.mediacontroller_progress);
        View view = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(id, null);
        Log.e(Thread.currentThread().getStackTrace()[2] + "", view.toString());
        TextView currentTimeTextView = (TextView) view.findViewById(Resources.getSystem().getIdentifier("time_current", "id", "android"));
        currentTimeTextView.setTextColor(Color.RED);
        ProgressBar progressBar = (ProgressBar) view.findViewById(Resources.getSystem().getIdentifier("mediacontroller_progress","id", "android"));
        progressBar.setBackgroundColor(0xFFFF0000);
/*
        try {
            Class aClass = Class.forName("android.widget.MediaController");
            Field currentTime = aClass.getDeclaredField("mCurrentTime");
            currentTime.setAccessible(true);
            TextView currentTimeTextView = (TextView) currentTime.get(MC);
            currentTimeTextView.setTextColor(Color.RED);
        } catch (Exception pokemon) {
        }
*/
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        TVClass = new String[listClassStr.length];
        for(int i=0; i<listClassStr.length; i++) {
            TVClass[i] = ReadFile("tv" + i + ".txt");
        }
        MMR = new MediaMetadataRetriever();

        listViewClass.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listClassStr));
        listViewClass.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                int count = listViewClass.getLastVisiblePosition() - listViewClass.getFirstVisiblePosition() + 1;
                for (int i=0; i<count; i++) {
                    listViewClass.getChildAt(i).setBackgroundColor(Color.TRANSPARENT);
                }
                listViewClass.getChildAt(position - listViewClass.getFirstVisiblePosition()).setBackgroundColor(Color.argb(100,00,00,00));
                TVItems = new String[TVClass[position].split("\n").length];
                TVItems = TVClass[position].split("\n");
                String TVNames = "";
                for(int i=0; i<TVItems.length; i++){
                    TVNames += TVItems[i].split(",")[0] + ",";
                }
                listViewTV.setAdapter(new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, TVNames.split(",")));
            }
        });

        listViewTV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                int count = listViewTV.getLastVisiblePosition() - listViewTV.getFirstVisiblePosition() + 1;
                for (int i=0; i<count; i++) {
                    listViewTV.getChildAt(i).setBackgroundColor(Color.TRANSPARENT);
                }
                listViewTV.getChildAt(position - listViewTV.getFirstVisiblePosition()).setBackgroundColor(Color.argb(100,00,00,00));
                String url = TVItems[position].split(",")[1];
                Uri uri = Uri.parse(url);
                uric = uri;
                itvc = position;
                videoView.setVideoURI(uri);
                videoView.requestFocus();
                videoView.start();
                source = "net";
                //MMR.setDataSource(url, new HashMap<String, String>()); //m3u8崩溃
            }
        });

        // 动态获取权限，Android 6.0 新特性，一些保护权限，除了要在 Manifest 中声明，还要使用如下代码动态获取
        if (Build.VERSION.SDK_INT >= 23) {
            int REQUEST_CODE_CONTACT = 101;
            String[] permissions = { Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.WAKE_LOCK };
            //验证是否许可权限
            for (String str : permissions) {
                if (this.checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                    //申请权限
                    this.requestPermissions(permissions, REQUEST_CODE_CONTACT);
                    return;
                }
            }
        }

    }

    @Override
    public void onPause() {
        releaseWakeLock();
        currentPosition = videoView.getCurrentPosition();
        videoView.pause();
        Log.e(Thread.currentThread().getStackTrace()[2] + "", "onPause: " + currentPosition);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        acquireWakeLock();
        Log.e("MainActivity", "onResume " + source);
        if(seekable){
            videoView.seekTo(currentPosition);
            Log.e(Thread.currentThread().getStackTrace()[2] + "", "onResume: " + currentPosition);
        }
        videoView.start();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if(listViewClass.getVisibility() == View.VISIBLE) {
                listViewClass.setVisibility(View.GONE);
                listViewTV.setVisibility(View.GONE);
            }else{
                MenuDialog();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    //    @Override
    //    public boolean onTouch(View v, MotionEvent event) {
    //        if (listViewClass.getVisibility() == View.VISIBLE) {
    //            listViewClass.setVisibility(View.GONE);
    //            listViewTV.setVisibility(View.GONE);
    //        } else {
    //            listViewClass.setVisibility(View.VISIBLE);
    //            listViewTV.setVisibility(View.VISIBLE);
    //        }
    //        return false;
    //    }

    void AboutDialog() {
        new AlertDialog.Builder(this)
                .setIcon(R.mipmap.ic_launcher)
                .setTitle("海天鹰播放器 V1.7")
                .setMessage("播放本地视频，打开网络视频，直播视频，直播菜单双层分类，横竖屏切换。\n作者：海天鹰\nE-mail: sonichy@163.com\nQQ: 84429027\n参考：\n视频播放：http://blog.csdn.net/liuhaoyutz/article/details/10188305\nListView.getChildAt(i)返回null崩溃：\nhttps://blog.csdn.net/peakerli/article/details/37658649\nhttps://zhidao.baidu.com/question/262188209076803805.html")
                .setPositiveButton("确定", null)
                .show();
    }

    void OpenURLDialog(){
        final EditText editText = new EditText(this);
        ClipboardManager CM = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        ClipData CD = CM.getPrimaryClip();
        if(CD != null) {
            editText.setText(CD.getItemAt(0).getText().toString());
        }
        new AlertDialog.Builder(this).setTitle("网址")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setView(editText)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String s = editText.getText().toString();
                        if (!s.equals("")) {
                            Uri uri = Uri.parse(s);
                            videoView.setVideoURI(uri);
                            videoView.requestFocus();
                            videoView.start();
                            seekable = true;
                            // MMR.setDataSource(ETText, new HashMap<String, String>()); //m3u8崩溃
                        }
                    }
                })
                .show();
    }

    void MenuDialog() {
        final String items[] = {"打开视频", "打开网址","直播", "信息", "横屏", "竖屏", "截图", "关于", "退出"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //builder.setTitle("菜单");
        //builder.setIcon(R.mipmap.ic_launcher);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                switch (which) {
                    case 0:
                        OpenVideo();
                        break;
                    case 1:
                        OpenURLDialog();
                        break;
                    case 2:
                        listViewClass.setVisibility(View.VISIBLE);
                        listViewTV.setVisibility(View.VISIBLE);
                        break;
                    case 3:
                        InfoDialog();
                        break;
                    case 4:
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                        videoView.start();
                        break;
                    case 5:
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                        videoView.start();
                        break;
                    case 6:
                        Screenshot();
                        break;
                    case 7:
                        AboutDialog();
                        break;
                    case 8:
                        finish();
                        break;
                }
            }
        });
        builder.create().show();
    }

    void OpenVideo() {
        int CHOOSE_VIDEO = 100;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        startActivityForResult(intent, CHOOSE_VIDEO);
    }

    void Screenshot() {
        SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        String filepath = Environment.getExternalStorageDirectory().getPath() + "/DCIM/Screenshots/" + SDF.format(new Date()) + ".png";
        Bitmap bitmap = MMR.getFrameAtTime(videoView.getCurrentPosition() * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
        if (bitmap != null) {
            Log.e(Thread.currentThread().getStackTrace()[2] + "", "Screenshot: " + filepath);
            try {
                FileOutputStream FOS = new FileOutputStream(filepath);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, FOS);
                FOS.flush();
                FOS.close();
                Toast.makeText(getApplicationContext(), "截图保存到：\n" + filepath, Toast.LENGTH_SHORT).show();
                MediaScannerConnection.scanFile(MainActivity.this, new String[]{filepath}, null, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.e(Thread.currentThread().getStackTrace()[2] + "", "Screenshot: Bitmap is null !");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Uri uri = data.getData();
            String suri = uri.toString();
            Log.e(Thread.currentThread().getStackTrace()[2] + "", suri);
            String path = suri;
            if(suri.startsWith("content://")) {
                String[] proj = { MediaStore.Video.Media.DATA };
                Cursor cursor = managedQuery(uri, proj, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                cursor.moveToFirst();
                path = cursor.getString(column_index);
                if(path == null){
                    if (Build.VERSION.SDK_INT >= 19) {
                        String docId = DocumentsContract.getDocumentId(uri);
                        Log.e(Thread.currentThread().getStackTrace()[2] + "", docId);
                        String[] split = docId.split(":");
                        String selection = "_id=?";
                        String[] selectionArgs = new String[] { split[1] };
                        String column = "_data";
                        String[] projection = { column };
                        cursor = getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null);
                        if (cursor != null && cursor.moveToFirst()) {
                            column_index = cursor.getColumnIndexOrThrow(column);
                            path = cursor.getString(column_index);
                        }
                    }
                }
            }
            if (path != null) {
                Log.e(Thread.currentThread().getStackTrace()[2] + "", path);
                currentPosition = 0;
                seekable = true;
                videoView.setVideoPath(path);
                videoView.requestFocus();
                videoView.start();
                try {
                    MMR.setDataSource(path, new HashMap<String, String>());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    String ReadFile(String filename) {
        String txt = "";
        try {
            InputStream in = getResources().getAssets().open(filename);
            int length = in.available();
            byte[] buffer = new byte[length];
            in.read(buffer);
            in.close();
            ByteArrayOutputStream BAOS = new ByteArrayOutputStream();
            BAOS.write(buffer, 0, length);
            txt = BAOS.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return txt;
    }

    void InfoDialog() {
        Uri mUri = null;
        try {
            Field mUriField = VideoView.class.getDeclaredField("mUri");
            mUriField.setAccessible(true);
            mUri = (Uri)mUriField.get(videoView);
        } catch(Exception e) {
        }
        String path="";
        if(mUri != null){
            path = mUri.toString();
        }
        //String resolution = "分辨率：" + MMR.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH) + " X " + MMR.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        String resolution = "分辨率：" + width + " X " + height;
        String mimetype = "格式：" + MMR.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
        String bitrate = "比特率：" + MMR.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE) + " bits/sec";
        SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss");
        SDF.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
        /*
        String duration = MMR.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        if(duration == null){
            duration = "时长：null";
        }else {
            duration = "时长：" + SDF.format(Long.parseLong(MMR.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)));
        }
        */
        int duration = videoView.getDuration();
        Date date = new Date(duration);
        String sduration = "时长：" + SDF.format(date);
        if(path.startsWith("file://")){
            path = path.substring(7);
        }
        File file = new File(path);
        Log.e(Thread.currentThread().getStackTrace()[2] + "", "File size: " + file.length());
        String fileSize = "大小：" + Formatter.formatFileSize(this, file.length());
        date = new Date(file.lastModified());
        SDF = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String LMDate = SDF.format(date);
        new AlertDialog.Builder(this).setMessage("路径："+ path + "\n" + resolution + "\n创建日期：" + MMR.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE) + "\n修改日期：" + LMDate + "\n" + mimetype + "\n" + bitrate + "\n" + sduration + "\n" + fileSize).show();
    }

    private void acquireWakeLock() {
        if(mWakeLock == null) {
            PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, this.getClass().getCanonicalName());
            mWakeLock.acquire();
        }
    }

    private void releaseWakeLock() {
        if(mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }

}