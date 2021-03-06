package com.example.musicplayer;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private boolean isStopThread = false;
    private Button lastSong_btn;
    private Button nextSong_btn;
    private Button playOrPause_btn;
    private TextView playingSong_tv;
    private ListView music_lv;
    private SeekBar play_sb;
    private MediaPlayer mediaPlayer;
    private ArrayAdapter<String> adapter;
    private List<String> songList = new ArrayList<String>() {{
        add("Anne Frank");
        add("A Peaceful Winter");
        add("What Will Be");
        add("This is Christmas");
        add("Jingle Bells");
    }};
    private List<String> downloadUrls = new ArrayList<String>() {{
        add("https://files.freemusicarchive.org/storage-freemusicarchive-org/tracks/NiWAOMdSP8Hj6H1IqKWh91dpxPfpQmAwE2QDwuKs.mp3?download=1&name=Audiobinger%20-%20Anne%20Frank.mp3");
        add("https://files.freemusicarchive.org/storage-freemusicarchive-org/tracks/KB6SsSDTdXstNTi5nxuRxSsakiv1dqiIqmJajLVC.mp3?download=1&name=Scott%20Holmes%20Music%20-%20A%20Peaceful%20Winter.mp3");
        add("https://files.freemusicarchive.org/storage-freemusicarchive-org/tracks/Y7ifqVEq9ADGpDyFyHqEc1rlYvAfuUNt3soAEaAr.mp3?download=1&name=Bumy%20Goldson%20-%20What%20Will%20Be.mp3");
        add("https://files.freemusicarchive.org/storage-freemusicarchive-org/tracks/B0ZXKzirlqgxJL0cBZfUsGsRIW6VBikm7G9E98cl.mp3?download=1&name=Scott%20Holmes%20Music%20-%20This%20is%20Christmas.mp3");
        add("https://files.freemusicarchive.org/storage-freemusicarchive-org/tracks/FJdCUSFwpg5Kvd5oSCyKdtK6AkkBSz320KkJPP4n.mp3?download=1&name=Scott%20Holmes%20Music%20-%20Jingle%20Bells.mp3");
    }};

    private DownloadService.DownloadBinder downloadBinder;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("tag", "?????????????????????onServiceConnected??????");
            downloadBinder = (DownloadService.DownloadBinder) service;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    private void init() {
        mediaPlayer = new MediaPlayer();
        play_sb = findViewById(R.id.play_sb);
        playingSong_tv = findViewById(R.id.playingSong_tv);
        lastSong_btn = findViewById(R.id.lastSong_btn);
        nextSong_btn = findViewById(R.id.nextSong_btn);
        playOrPause_btn = findViewById(R.id.playOrPause_btn);
        music_lv = findViewById(R.id.music_lv);
        adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, songList);
        music_lv.setAdapter(adapter);

        new Thread() {
            public void run() {
                while (true) {
                    if (isStopThread) {
                        break;
                    }
                    if (mediaPlayer != null) {
                        play_sb.setProgress(mediaPlayer.getCurrentPosition());
                    }
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();

        //????????????
        music_lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (!playingSong_tv.getText().toString().equals("Not Found")) {
                    mediaPlayer.reset();
                }
                playOrPause_btn.setText("??????");
                String downloadUrl = downloadUrls.get(position);
                String url = getUrl(downloadUrl);
                File file = new File(url);
                //????????????????????????
                if (file.exists()) {
                    Log.d("tag", "????????????");
                    Log.d("tag", url);
                    initMediaPlayer(url); // ?????????MediaPlayer
                    initSeeKBar();
                    mediaPlayer.start();
                    playOrPause_btn.setText("??????");
                    playingSong_tv.setText(songList.get(position));
                } else {  //????????????????????????
                    Log.d("tag", "???????????????");
                    downloadBinder.startDownload(downloadUrl);
                    initMediaPlayer(url); // ?????????MediaPlayer
                    initSeeKBar();
                    mediaPlayer.start();
                    playOrPause_btn.setText("??????");
                }
            }
        });

        //????????????????????????
        lastSong_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //????????????????????????
                if (playingSong_tv.getText().toString().equals("Not Found")) {
                    Toast.makeText(MainActivity.this, "???????????????", Toast.LENGTH_SHORT).show();
                    return;
                }
                //??????????????????????????????????????????????????????
                mediaPlayer.reset();
                int position = songList.indexOf(playingSong_tv.getText().toString());
                int lastPosition = (position - 1 + 5) % 5;
                String url =  getUrl(downloadUrls.get(lastPosition));
                File file = new File(url);
                if (file.exists()){
                    initMediaPlayer(url);
                    initSeeKBar();
                    mediaPlayer.start();
                    playOrPause_btn.setText("??????");
                }else{
                    Toast.makeText(MainActivity.this,"????????????????????????????????????",Toast.LENGTH_SHORT).show();
                    playOrPause_btn.setText("??????");
                }
                playingSong_tv.setText(songList.get(lastPosition));
            }
        });

        //????????????????????????
        nextSong_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //????????????????????????
                if (playingSong_tv.getText().toString().equals("Not Found")) {
                    Toast.makeText(MainActivity.this, "???????????????", Toast.LENGTH_SHORT).show();
                    return;
                }
                //??????????????????????????????????????????????????????
                mediaPlayer.reset();
                int position = songList.indexOf(playingSong_tv.getText().toString());
                int nextPosition = (position + 1) % 5;
                String url =  getUrl(downloadUrls.get(nextPosition));
                File file = new File(url);
                if (file.exists()){
                    initMediaPlayer(url);
                    initSeeKBar();
                    mediaPlayer.start();
                    playOrPause_btn.setText("??????");
                }else{
                    Toast.makeText(MainActivity.this,"????????????????????????????????????",Toast.LENGTH_SHORT).show();
                    playOrPause_btn.setText("??????");
                }
                playingSong_tv.setText(songList.get(nextPosition));
            }
        });

        //??????????????????????????????
        playOrPause_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //????????????????????????????????????
                if (playingSong_tv.getText().toString().equals("Not Found")) {
                    Toast.makeText(MainActivity.this, "???????????????", Toast.LENGTH_SHORT).show();
                    return;
                }
                //????????????????????????,????????????
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    playOrPause_btn.setText("??????");
                } else {  //???????????????????????????????????????
                    mediaPlayer.start();
                    playOrPause_btn.setText("??????");
                }
            }
        });

        play_sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mediaPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        Intent intent = new Intent(this, DownloadService.class);
        startService(intent); // ????????????
        Log.d("tag", "????????????");
        bindService(intent, connection, BIND_AUTO_CREATE); // ????????????
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    private void initMediaPlayer(String filename) {
        try {
            mediaPlayer.setDataSource(filename); // ???????????????????????????
            mediaPlayer.prepare(); // ???MediaPlayer?????????????????????
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //??????????????????
    private void initSeeKBar() {
        play_sb.setMax(mediaPlayer.getDuration());
    }

    //???????????????????????????
    private String getUrl(String downloadUrl) {
        String url = downloadUrl;
        String fileName = url.substring(url.lastIndexOf('/'));
        //??????????????????
        String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
        return directory + fileName;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "?????????????????????????????????", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(connection);
        isStopThread = true;
    }
}