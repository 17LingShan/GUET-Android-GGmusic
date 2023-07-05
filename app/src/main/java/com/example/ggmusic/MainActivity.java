package com.example.ggmusic;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ContentResolver mContentResolver;
    private ListView mPlaylist;
    private MediaCursorAdapter mCursorAdapter;

    private BottomNavigationView navigation;
    private TextView tvBottomTitle;
    private TextView tvBottomArtist;
    private ImageView ivAlbumThumbnail;
    private MediaPlayer mMediaPlayer = null;
    public static final int UPDATE_PROGRESS = 1;
    public static final String DATA_URI = "com.glriverside.xgqin.ggmusic.DATA_URI";
    public static final String TITLE = "com.glriverside.xgqin.ggmusic.TITLE";
    public static final String ARTIST = "com.glriverside.xgqin.ggmusic.ARTIST";
    public static final String ACTION_MUSIC_START = "com.glriverside.xgqin.ggmusic.ACTION_MUSIC_START";
    public static final String ACTION_MUSIC_STOP = "com.glriverside.xgqin.ggmusic.ACTION_MUSIC_STOP";

    private static final String TAG = MainActivity.class.getSimpleName();

    private Boolean mPlayStatus = true;
    private ImageView ivPlay;


    private ProgressBar pbProgress;


    private int musicIndex = 0;

    private MusicService mService;
    private boolean mBound = false;

    private MusicReceiver musicReceiver;

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_PROGRESS:
                    int position = msg.arg1;
                    pbProgress.setProgress(position);
                    break;
                default:
                    break;
            }
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MusicService.MusicServiceBinder binder = (MusicService.MusicServiceBinder) iBinder;

            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mService = null;
            mBound = false;
        }
    };

    private final String SELECTION =
            MediaStore.Audio.Media.IS_MUSIC + " = ? " + " AND " +
                    MediaStore.Audio.Media.MIME_TYPE + " LIKE ? ";
    private final String[] SELECTION_ARGS = {
            Integer.toString(1),
            "audio/mpeg"
    };
    private final int REQUEST_EXTERNAL_STORAGE = 1;

    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };


    @Override
    protected void onStart() {
        Intent intent = new Intent(MainActivity.this, MusicService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        super.onStart();
//        if (mMediaPlayer == null) {
//            mMediaPlayer = new MediaPlayer();
//        }
    }

    @Override
    protected void onStop() {
//        if (mMediaPlayer != null) {
//            mMediaPlayer.stop();
//            mMediaPlayer.release();
//            mMediaPlayer = null;
//            Log.d(TAG, "onStop invoked!");
//        }
        unbindService(mConnection);
        mBound = false;
        super.onStop();
    }

    private ListView.OnItemClickListener itemClickListener
            = new ListView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView,
                                View view, int i, long l) {
            Cursor cursor = mCursorAdapter.getCursor();
            if (cursor != null && cursor.moveToPosition(i)) {

                int titleIndex = cursor.getColumnIndex(
                        MediaStore.Audio.Media.TITLE);
                int artistIndex = cursor.getColumnIndex(
                        MediaStore.Audio.Media.ARTIST);
                int albumIdIndex = cursor.getColumnIndex(
                        MediaStore.Audio.Media.ALBUM_ID);
                int dataIndex = cursor.getColumnIndex(
                        MediaStore.Audio.Media.DATA);
                Long albumId = cursor.getLong(albumIdIndex);

                String title = cursor.getString(titleIndex);
                String artist = cursor.getString(artistIndex);
                String data = cursor.getString(dataIndex);

                //...
                Uri dataUri = Uri.parse(data);
                Log.d("MyApp", "Data URI: " + dataUri.toString());

//                if (mMediaPlayer != null) {
//                    try {
//                        mMediaPlayer.reset();
//                        mMediaPlayer.setDataSource(
//                                MainActivity.this, dataUri);
//                        mMediaPlayer.prepare();
//                        mMediaPlayer.start();
//                    } catch (IOException ex) {
//                        ex.printStackTrace();
//                    }
//                }

                musicIndex = i;

                Intent serviceIntent = new Intent(MainActivity.this, MusicService.class);
                serviceIntent.putExtra(MainActivity.DATA_URI, data);
                serviceIntent.putExtra(MainActivity.TITLE, title);
                serviceIntent.putExtra(MainActivity.ARTIST, artist);

                startService(serviceIntent);

                navigation.setVisibility(View.VISIBLE);
                if (tvBottomTitle != null) {
                    tvBottomTitle.setText(title);
                }
                if (tvBottomArtist != null) {
                    tvBottomArtist.setText(artist);
                }

//                Uri albumUri = ContentUris.withAppendedId(
//                        MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
//                        albumId);
//
//
//                cursor = mContentResolver.query(
//                        albumUri,
//                        null,
//                        null,
//                        null,
//                        null);
//
//                if (cursor != null && cursor.getCount() > 0) {
//                    cursor.moveToFirst();
//                    int albumArtIndex = cursor.getColumnIndex(
//                            MediaStore.Audio.Albums.ALBUM_ART);
//                    String albumArt = cursor.getString(
//                            albumArtIndex);
//                    Log.d(TAG, "albumArt: " + albumArt);
//                    Glide.with(MainActivity.this)
//                            .load(albumArt)
//                            .into(ivAlbumThumbnail);
//                    cursor.close();
//                }

                loadingCover(data);
            }
        }
    };

    /**
     * 加载封面
     *
     * @param mediaUri MP3文件路径
     */
    private void loadingCover(String mediaUri) {
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(mediaUri);
        byte[] picture = mediaMetadataRetriever.getEmbeddedPicture();

        if (picture == null || picture.length == 0) {
            Glide.with(MainActivity.this).load(R.drawable.alice).into(ivAlbumThumbnail);
        } else {

            Bitmap bitmap = BitmapFactory.decodeByteArray(picture, 0, picture.length);

            Glide.with(MainActivity.this)
                    .load(bitmap)
                    .into(ivAlbumThumbnail);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main);
        mPlaylist = findViewById(R.id.lv_playlist);
        mContentResolver = getContentResolver();

        mCursorAdapter = new MediaCursorAdapter(MainActivity.this);

        mPlaylist.setAdapter(mCursorAdapter);


//...
        navigation = findViewById(R.id.navigation);
        LayoutInflater.from(MainActivity.this)
                .inflate(R.layout.bottom_media_toolbar,
                        navigation,
                        true);

        ivPlay = navigation.findViewById(R.id.iv_play);
        tvBottomTitle = navigation.findViewById(R.id.tv_bottom_title);
        tvBottomArtist = navigation.findViewById(R.id.tv_bottom_artist);
        ivAlbumThumbnail = navigation.findViewById(R.id.iv_thumbnail);
        pbProgress = navigation.findViewById(R.id.progress);

        if (ivPlay != null) {
            ivPlay.setOnClickListener(MainActivity.this);
        }

        navigation.setVisibility(View.GONE);
        //...
        mPlaylist.setOnItemClickListener(itemClickListener);

//        if (mMediaPlayer == null) {
//            mMediaPlayer = new MediaPlayer();
//            Log.d(TAG, "MediaPlayer instance created!");
//        }
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            Log.d(TAG, "MediaPlayer instance created!");
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                //
            } else {
                requestPermissions(PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
        } else {
            initPlaylist();
        }
        musicReceiver = new MusicReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_MUSIC_START);
        intentFilter.addAction(ACTION_MUSIC_STOP);
        registerReceiver(musicReceiver, intentFilter);
    }


    private void initPlaylist() {
        Cursor mCursor = mContentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,
                SELECTION,
                SELECTION_ARGS,
                MediaStore.Audio.Media.DEFAULT_SORT_ORDER
        );
        mCursorAdapter.swapCursor(mCursor);
        mCursorAdapter.notifyDataSetChanged();
        if (mCursor == null) {
            Log.d("MyApp", "mCursor is null");
        } else if (!mCursor.moveToFirst()) {
            Log.d("MyApp", "mCursor is empty");
        } else {
            Log.d("MyApp", "mCursor has " + mCursor.getCount() + " rows");
        }
    }


    @Override
    protected void onDestroy() {
//        if (mMediaPlayer != null) {
//            mMediaPlayer.stop();
//            mMediaPlayer.release();
//            mMediaPlayer = null;
//        }

        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        unregisterReceiver(musicReceiver);

        super.onDestroy();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_EXTERNAL_STORAGE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initPlaylist();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.iv_play) {
            mPlayStatus = !mPlayStatus;
            if (mPlayStatus == true) {
                mService.play();
                ivPlay.setImageResource(
                        R.drawable.ic_baseline_pause_circle_outline_24);
            } else {
                mService.pause();
                ivPlay.setImageResource(
                        R.drawable.ic_play_circle_outline_black_24dp);
            }
        }
    }

    private class MusicProgressRunnable implements Runnable {

        public MusicProgressRunnable() {
        }

        @Override
        public void run() {
            boolean mThreadWorking = true;
            while (mThreadWorking) {
                try {
                    if (mService != null) {
                        int position = mService.getCurrentPosition();

                        Message message = new Message();
                        message.what = UPDATE_PROGRESS;
                        message.arg1 = position;
                        mHandler.sendMessage(message);

                        Log.d(TAG, "CurrentPosition: " + position);
                    }

                    mThreadWorking = mService.isPlaying();

                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }
    }

    public class MusicReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(ACTION_MUSIC_START)) {
                if (mService != null) {
                    pbProgress.setMax(mService.getDuration());
                    Log.d(TAG, "Duration: " + mService.getDuration());
                    new Thread(new MusicProgressRunnable()).start();
                }
            }
        }
    }

}