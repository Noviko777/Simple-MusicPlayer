package com.test.musicplayer;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    ImageButton playButton;
    TextView lengthTextView, currentLengthTextView;
    SeekBar lengthSeekBar;
    MediaPlayer mediaPlayer;
    boolean isPlay = false, isMove = false;


    private ArrayList<Song> songList;
    private int currSong = -1;
    private ListView songView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playButton = findViewById(R.id.play);
        lengthSeekBar = findViewById(R.id.lengthSeekBar);
        lengthTextView = findViewById(R.id.lengthTextView);
        currentLengthTextView = findViewById(R.id.currentLengthTextView);

        final Timer timer = new Timer();


        lengthSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(isMove){
                    int millis = lengthSeekBar.getProgress();
                    if(TimeUnit.MILLISECONDS.toSeconds(millis) >= 3600) {
                        currentLengthTextView.setText(String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
                                TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                                TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))));
                    }
                    else {
                        currentLengthTextView.setText(String.format("%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                                TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))));
                    }
                    mediaPlayer.seekTo(millis);
                    timer.schedule(new MyTimerTask(), 1000, 1000);
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isMove = true;
                timer.purge();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isMove = false;
            }
        });

        mediaPlayer = MediaPlayer.create(this, R.raw.bad_guy);

        updateTime();

        timer.schedule(new MyTimerTask(), 1000, 1000);


        songView = findViewById(R.id.songList);
        songList = new ArrayList<Song>();
        getSongList();


        Collections.sort(songList, new Comparator<Song>(){
            public int compare(Song a, Song b){
                return a.getTitle().compareTo(b.getTitle());
            }
        });

        SongAdapter songAdt = new SongAdapter(this, songList);
        songView.setAdapter(songAdt);
    }

    public void getSongList() {

        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if(musicCursor!=null && musicCursor.moveToFirst()){
            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            //add songs to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                songList.add(new Song(thisId, thisTitle, thisArtist));
            }
            while (musicCursor.moveToNext());
        }
    }


    public  void songPicked(View view){

        long id = songList.get(Integer.parseInt(view.getTag().toString())).getID();
        currSong = Integer.parseInt(view.getTag().toString());

        playSong(id);
    }

    void playSong(long id) {

            mediaPlayer.stop();
            Uri contentUri = ContentUris.withAppendedId(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
            mediaPlayer = MediaPlayer.create(this, contentUri);
            mediaPlayer.start();
            isPlay = true;
            playButton.setImageResource(R.drawable.pause_btn);
            updateTime();



    }

    void updateTime() {

        lengthSeekBar.setMax(mediaPlayer.getDuration()); // Задаем SeekBar`y максимальное значение, взяв милисекунды из плеера
        int millis = lengthSeekBar.getMax();
        // Преобразуем милисекунды в формат времени
        if(TimeUnit.MILLISECONDS.toSeconds(millis) >= 3600) {
            lengthTextView.setText(String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
                    TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                    TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))));
        }
        else {
            lengthTextView.setText(String.format("%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                    TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))));
        }
    }

    public void playButton(View view) {

        // Меняем кнопку
        if(isPlay == false) {
            playButton.setImageResource(R.drawable.pause_btn);
            mediaPlayer.start();
            isPlay = true;
        }
        else {
            playButton.setImageResource(R.drawable.play_btn);
            mediaPlayer.pause();
            isPlay = false;
        }

    }

    public void nextMusic(View view) {

        if(currSong < songList.size() - 1) {
            currSong++;
            long id = songList.get(currSong).getID();
            playSong(id);
        }

    }

    public void previMusic(View view) {

        if(currSong > 0) {
            currSong--;
            long id = songList.get(currSong).getID();
            playSong(id);
        }
    }

    class MyTimerTask extends TimerTask {

        @Override
        public void run() {


            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    int millis = mediaPlayer.getCurrentPosition();
                    if(TimeUnit.MILLISECONDS.toSeconds(millis) >= 3600) {
                        currentLengthTextView.setText(String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
                                TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                                TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))));
                    }
                    else {
                        currentLengthTextView.setText(String.format("%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                                TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))));
                    }
                    lengthSeekBar.setProgress(millis);
                }
            });
        }
    }
}
