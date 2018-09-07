package com.hty.mediaplayer;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

public class MainActivity extends Activity {

    VideoView video;
    ListView listViewClass, listViewTV;
    MediaController mc;
    MediaMetadataRetriever MMR;
    Uri uric;
    int itvc, currentPosition;
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");

    String[] listClassStr = {"战旗频道", "央视频道", "卫视频道", "剧场频道", "剧集频道", "影视频道"};
    String[] TVClass,TVItems;
    static int psc = -1, pstv = -1;
    String source = "";
    boolean seekable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        video = (VideoView) findViewById(R.id.video);
        listViewClass = (ListView) findViewById(R.id.listViewClass);
        listViewTV = (ListView) findViewById(R.id.listViewTV);

        mc = new MediaController(MainActivity.this);
        //mc.setMediaPlayer(video);
        video.setMediaController(mc);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        TVClass = new String[listClassStr.length];
        for(int i=0; i<listClassStr.length; i++) {
            TVClass[i] = ReadFile("tv" + i + ".txt");
        }
        MMR = new MediaMetadataRetriever();

        listViewClass.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listClassStr));
        listViewClass.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                listViewClass.getChildAt(position).setBackgroundColor(Color.argb(100,00,00,00));
                if (psc != -1 && psc != position){
                    listViewClass.getChildAt(psc).setBackgroundColor(Color.TRANSPARENT);
                }
                psc = position;
                TVItems = new String[TVClass[position].split("\n").length];
                TVItems = TVClass[position].split("\n");
                String TVNames = "";
                for(int i=0; i<TVItems.length; i++){
                    TVNames += TVItems[i].split(",")[0] + ",";
                }
                listViewTV.setAdapter(new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, TVNames.split(",")));
            }
        });

        listViewTV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                //listViewTV.getChildAt(position).setBackgroundColor(Color.argb(100,00,00,00));
                if (pstv != -1 && pstv != position){
                    //listViewTV.getChildAt(pstv).setBackgroundColor(Color.TRANSPARENT);
                }
                pstv = position;
                String url = TVItems[position].split(",")[1];
                Uri uri = Uri.parse(url);
                uric = uri;
                itvc = position;
                video.setVideoURI(uri);
                video.requestFocus();
                video.start();
                source = "net";
                //MMR.setDataSource(url, new HashMap<String, String>()); //m3u8崩溃
            }
        });

        // 动态获取权限，Android 6.0 新特性，一些保护权限，除了要在 AndroidManifest 中声明权限，还要使用如下代码动态获取
        if (Build.VERSION.SDK_INT >= 23) {
            int REQUEST_CODE_CONTACT = 101;
            String[] permissions = { Manifest.permission.WRITE_EXTERNAL_STORAGE };
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
        currentPosition = video.getCurrentPosition();
        video.pause();
        Log.e("Pause", "" + currentPosition);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e("MainActivity", "onResume " + source);
        if(seekable){
            video.seekTo(currentPosition);
            Log.e("MainActivity", "onResume " + source + ":" + currentPosition);
        }
        video.start();
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
                .setTitle("海天鹰播放器 V1.6")
                .setMessage("播放本地视频，打开网络视频，直播视频，直播菜单双层分类，横竖屏切换。\n作者：黄颖\nE-mail: sonichy@163.com\nQQ: 84429027\n参考：\n视频播放：http://blog.csdn.net/liuhaoyutz/article/details/10188305")
                .setPositiveButton("确定", null)
                .show();
    }

    void OpenURLDialog(){
        final EditText ET = new EditText(this);
        ClipboardManager CM = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        ClipData CD = CM.getPrimaryClip();
        if(CD != null) {
            ET.setText(CD.getItemAt(0).getText().toString());
        }
        new AlertDialog.Builder(this).setTitle("网址")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setView(ET)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String ETText = ET.getText().toString();
                        if (!ETText.equals("")) {
                            Uri uri = Uri.parse(ETText);
                            video.setVideoURI(uri);
                            video.requestFocus();
                            video.start();
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
                        video.start();
                        break;
                    case 5:
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                        video.start();
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
        String filepath = Environment.getExternalStorageDirectory().getPath() + "/Pictures/Screenshots/" + sdf.format(new Date()) + ".png";
        Bitmap bitmap = MMR.getFrameAtTime(video.getCurrentPosition() * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
        if (bitmap != null) {
            Log.e("screenshot", "Bitmap got: " + filepath);
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
            Log.e("screenshot", "Bitmap is NULL!");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Uri uri = data.getData();
            String suri = uri.toString();
            Log.e("uri", suri);
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
                        Log.e("DocId", docId);
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
                Log.e("Path", path);
                currentPosition = 0;
                seekable = true;
                video.setVideoPath(path);
                video.requestFocus();
                video.start();
                try {
                    //if (Build.VERSION.SDK_INT >= 14)
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
        String resolution = "分辨率：" + MMR.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH) + " X " + MMR.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        String createDate = "创建日期：" + MMR.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE);
        String mimetype = "格式：" + MMR.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
        String bitrate = "比特率：" + MMR.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE) + " bits/sec";
        SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss");
        SDF.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
        String duration = MMR.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        if(duration == null){
            duration = "时长：null";
        }else {
            duration = "时长：" + SDF.format(Long.parseLong(MMR.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)));
        }
        new AlertDialog.Builder(this).setMessage(resolution + "\n" + createDate + "\n" + mimetype + "\n" + bitrate + "\n" + duration).show();
    }

}