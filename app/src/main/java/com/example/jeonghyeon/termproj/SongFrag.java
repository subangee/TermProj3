package com.example.jeonghyeon.termproj;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by JeongHyeon on 2017-04-16.
 */

public class SongFrag extends Fragment implements MediaPlayer.OnCompletionListener {
    public View SView;
    public TextView selected;
    public Button load;
    public Button record;
    public Button userSong;
    public Button songBoard;

    private InstDBLoader instDBLoader;
    private ListView instList;
    private WebView boardWebView;
    private LinearLayout recordView;
    private LinearLayout userRecordView;
    private InstAdapter adapter;
    private File instFile = null;
    private File recFile = null;
    private static int lock;
    private boolean ready;
    private View pre = null;

    // 미리 상수 선언
    private static final int REC_STOP = 0;
    private static final int RECORDING = 1;
    private static final int PLAY_STOP = 0;
    private static final int PLAYING = 1;
    private static final int PLAY_PAUSE = 2;

    private MediaRecorder mRecorder = null;
    private MediaPlayer recordPlayer = null;
    private MediaPlayer instPlayer = null;

    private int mRecState = REC_STOP;
    private int mPlayerState = PLAY_STOP;
    private int rPlayerState = PLAY_STOP;
    private SeekBar mRecProgressBar, mPlayProgressBar, rPlayProgressBar;
    private Button mBtnStartRec, mBtnStartPlay, mBtnStopPlay, rBtnStartPlay, rBtnStopPlay;
    private String mFilePath, mFileName = null;
    private TextView mTvPlayStartPoint;
    private TextView mTvPlayMaxPoint;
    private TextView rTvPlayStartPoint;
    private TextView rTvPlayMaxPoint;
    private ListView recordList;
    private int mCurRecTimeMs = 0;
    private int mCurProgressTimeDisplay = 0;

    recordDBhelper rHelper;
    SQLiteDatabase rdb;
    Cursor rdbC;
    SimpleCursorAdapter recordAdapter;

    int point_max;

    // 녹음시 SeekBar처리
    Handler mProgressHandler = new Handler() {
        public void handleMessage(Message msg) {
            mCurRecTimeMs = mCurRecTimeMs + 100;
            mCurProgressTimeDisplay = mCurProgressTimeDisplay + 100;

            // 녹음시간이 음수이면 정지버튼을 눌러 정지시켰음을 의미하므로
            // SeekBar는 그대로 정지시키고 레코더를 정지시킨다.
            if (mCurRecTimeMs < 0) {
            }
            // 녹음시간이 아직 최대녹음제한시간보다 작으면 녹음중이라는 의미이므로
            // SeekBar의 위치를 옮겨주고 0.1초 후에 다시 체크하도록 한다.
            else if (mCurRecTimeMs < point_max) {
                mRecProgressBar.setProgress(mCurProgressTimeDisplay);
                mProgressHandler.sendEmptyMessageDelayed(0, 100);
            }
            // 녹음시간이 최대 녹음제한 시간보다 크면 녹음을 정지 시킨다.
            else {
                mBtnStartRecOnClick();
            }
        }
    };

    Handler mProgressHandler2 = new Handler() {
        public void handleMessage(Message msg) {
            if (instPlayer == null) return;

            try {
                if (instPlayer.isPlaying()) {
                    mPlayProgressBar.setProgress(instPlayer.getCurrentPosition());
                    mProgressHandler2.sendEmptyMessageDelayed(0, 100);
                }
            } catch (IllegalStateException e) {
            } catch (Exception e) {
            }
        }
    };

    Handler rProgressHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (recordPlayer == null) return;

            try {
                if (recordPlayer.isPlaying()) {
                    rPlayProgressBar.setProgress(recordPlayer.getCurrentPosition());
                    rProgressHandler.sendEmptyMessageDelayed(0, 100);
                }
            } catch (IllegalStateException e) {
            } catch (Exception e) {
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        SView = inflater.inflate(R.layout.fragment_third, container, false);

        ready = false;
        instList = (ListView) SView.findViewById(R.id.instList);


        selected = (TextView) SView.findViewById(R.id.selectedInst);
        load = (Button) SView.findViewById(R.id.load);
        record = (Button) SView.findViewById(R.id.record);
        userSong = (Button)SView.findViewById(R.id.userSong);
        songBoard = (Button) SView.findViewById(R.id.songBoard);
        mBtnStartPlay = (Button) SView.findViewById(R.id.btnStartPlay);
        mBtnStartRec = (Button) SView.findViewById(R.id.btnStartRec);
        mBtnStopPlay = (Button) SView.findViewById(R.id.btnStopPlay);
        rBtnStartPlay = (Button)SView.findViewById(R.id.btnStartRecordPlay);
        rBtnStopPlay = (Button)SView.findViewById(R.id.btnStopRecordPlay);
        mPlayProgressBar = (SeekBar) SView.findViewById(R.id.playProgressBar);
        rPlayProgressBar = (SeekBar) SView.findViewById(R.id.recordPlayProgressBar);
        mRecProgressBar = (SeekBar)SView.findViewById(R.id.recProgressBar);
        mTvPlayStartPoint = (TextView) SView.findViewById(R.id.tvPlayStartPoint);
        mTvPlayMaxPoint = (TextView) SView.findViewById(R.id.tvPlayMaxPoint);
        rTvPlayStartPoint = (TextView) SView.findViewById(R.id.recordPlayStartPoint);
        rTvPlayMaxPoint = (TextView) SView.findViewById(R.id.recordPlayMaxPoint);
        boardWebView = (WebView) SView.findViewById(R.id.boardWebView);
        recordList = (ListView)SView.findViewById(R.id.userRecordList);
        recordView = (LinearLayout) SView.findViewById(R.id.recordView);
        userRecordView = (LinearLayout)SView.findViewById(R.id.userRecordView);
        instPlayer = new MediaPlayer();
        lock = 1;
        instDBLoader = new InstDBLoader();//비트 DB에 접근
        instDBLoader.load();

        // 미디어 레코더 저장할 파일 생성
        mFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/.gilrhyme_records/";
        File dir = new File(mFilePath);
        // 폴더가 존재하지 않을 경우 폴더를 만듦
        if (!dir.exists()) {
            dir.mkdir();
        }


        rHelper = new recordDBhelper(getContext(), "record_" + MainActivity.userNickname +".db", null, 1);//각 사용자별 별개의 DB
        rdb = rHelper.getWritableDatabase();
        rdbC = rdb.query("records", null, null, null, null, null, null, null);
        recordAdapter = new SimpleCursorAdapter(SView.getContext(), android.R.layout.simple_list_item_2, rdbC,
                new String[] {"title","date"}, new int[] {android.R.id.text1,android.R.id.text2}, 0);
        recordList.setAdapter(recordAdapter);

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!instDBLoader.Flag()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                SView.post(new Runnable() {
                    @Override
                    public void run() {//
                        TextView t = (TextView) SView.findViewById(R.id.loadMsg);
                        t.setVisibility(View.GONE);
                        adapter = new InstAdapter(SView.getContext(), R.layout.instlist_layout, instDBLoader.getList());
                        instList.setAdapter(adapter);
                        ready = true;
                    }
                });
            }
        }).start();

        boardWebView.getSettings().setJavaScriptEnabled(true);
        boardWebView.getSettings().setBuiltInZoomControls(true);
        boardWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        boardWebView.setWebViewClient(new MyWebViewClient());
        boardWebView.loadUrl("http://roadofrhyme.esy.es/?kboard_id=1");

        load.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ready) {
                    boardWebView.setVisibility(View.GONE);
                    userRecordView.setVisibility(View.GONE);
                    recordView.setVisibility(View.GONE);
                    instList.setVisibility(View.VISIBLE);
                }
            }
        });

        instList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                final String instTitle = ((InstClass) (instList.getAdapter().getItem(position))).getTitle() + "-" + ((InstClass) (instList.getAdapter().getItem(position))).getAuthor();
                instFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/.gilrhyme_insts/" + instTitle + ".mp3");
                PopupMenu popupMenu = new PopupMenu(SView.getContext(), view);
                popupMenu.getMenuInflater().inflate(R.menu.instmenu, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.selectInst:
                                if (!instFile.exists()) {
                                    Toast toast = Toast.makeText(SView.getContext(), "먼저 다운로드 받아주세요.", Toast.LENGTH_SHORT);
                                    toast.show();
                                    instFile = null;
                                    break;
                                }
                                selected.setText("선택된 비트 : " + instFile.getPath());
                                break;
                            case R.id.downloadInst:
                                if (lock == 1) {
                                    lock();
                                    Intent service = new Intent(SView.getContext(), DownloadService.class);
                                    service.putExtra("FILE_NAME", instTitle + ".mp3");
                                    getActivity().startService(service);
                                    instFile = null;
                                } else if (lock == 0) {
                                    Toast toast = Toast.makeText(SView.getContext(), "현재 다운로드가 완료된 후 다시 시도해 주세요", Toast.LENGTH_SHORT);
                                    toast.show();
                                }
                                break;
                            case R.id.deleteInst:
                                boolean d = instFile.delete();
                                if (d) {
                                    Toast toast = Toast.makeText(SView.getContext(), "삭제되었습니다.", Toast.LENGTH_SHORT);
                                    toast.show();
                                }
                                instFile = null;
                                break;
                        }
                        return false;
                    }
                });
                popupMenu.show();
            }
        });

        recordList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, final int position, long id) {
                rdbC.moveToPosition(position);
                recFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/.gilrhyme_records/"+rdbC.getString(1)+ "-" + MainActivity.userNickname + ".mp3");
                PopupMenu popupMenu = new PopupMenu(SView.getContext(), view);
                popupMenu.getMenuInflater().inflate(R.menu.recordmenu, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.selectRecord:
                                if (!recFile.exists()) {
                                    Toast toast = Toast.makeText(SView.getContext(), "파일이 존재하지 않습니다. \n목록에서 제거합니다.", Toast.LENGTH_SHORT);
                                    toast.show();
                                    rdb.delete("records", "_id=?", new String[]{rdbC.getString(0)});
                                    rdbC = rdb.query("records", null, null, null, null, null, null, null);
                                    recordAdapter.changeCursor(rdbC);
                                    recFile = null;
                                    break;
                                }
                                else{
                                    if (pre != null) {
                                        pre.setBackgroundColor(Color.TRANSPARENT);
                                    }
                                    view.setBackgroundColor(Color.parseColor("#FFCE87"));
                                    pre = view;
                                }
                                break;
                            case R.id.uploadRecord:

                                break;
                            case R.id.deleteRecord:
                                if (!recFile.exists()) {
                                    Toast toast = Toast.makeText(SView.getContext(), "파일이 존재하지 않습니다. \n목록에서 제거합니다.", Toast.LENGTH_SHORT);
                                    toast.show();
                                    rdb.delete("records", "_id=?", new String[]{rdbC.getString(0)});
                                    rdbC = rdb.query("records", null, null, null, null, null, null, null);
                                    recordAdapter.changeCursor(rdbC);
                                    recFile = null;
                                    break;
                                }
                                final AlertDialog.Builder delAlert = new AlertDialog.Builder(SView.getContext());//삭제 경고 다이얼로그
                                delAlert.setTitle("삭제");
                                delAlert.setMessage("정말로 삭제하시겠습니까?");
                                delAlert.setCancelable(false)
                                        .setPositiveButton("예",new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                rdb.delete("records", "_id=?", new String[]{rdbC.getString(0)});
                                                rdbC = rdb.query("records", null, null, null, null, null, null, null);
                                                recordAdapter.changeCursor(rdbC);
                                                recFile.delete();
                                                dialog.cancel();
                                            }
                                        })
                                        .setNegativeButton("아니오", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.cancel();
                                            }
                                        });
                                recFile = null;
                                AlertDialog del = delAlert.create();
                                del.show();
                                break;
                        }
                        return false;
                    }
                });
                popupMenu.show();
            }
        });

        songBoard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ready) {
                    recordView.setVisibility(View.GONE);
                    instList.setVisibility(View.GONE);
                    userRecordView.setVisibility(View.GONE);
                    boardWebView.setVisibility(View.VISIBLE);
                }
            }
        });

        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ready) {
                    boardWebView.setVisibility(View.GONE);
                    instList.setVisibility(View.GONE);
                    userRecordView.setVisibility(View.GONE);
                    recordView.setVisibility(View.VISIBLE);
                }
            }
        });

        userSong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ready) {
                    rdbC = rdb.query("records", null, null, null, null, null, null, null);
                    recordAdapter.changeCursor(rdbC);
                    boardWebView.setVisibility(View.GONE);
                    instList.setVisibility(View.GONE);
                    recordView.setVisibility(View.GONE);
                    userRecordView.setVisibility(View.VISIBLE);
                }
            }
        });

        mBtnStartPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startPlayBtn();
            }
        });

        mBtnStopPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopPlayBtn();
            }
        });

        mBtnStartRec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopPlayBtn();
                mBtnStartRecOnClick();
            }
        });

        rBtnStartPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRecordPlayBtn();
            }
        });

        rBtnStopPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecordPlayBtn();
            }
        });

        return SView;
    }


    private void startPlayBtn(){
        if(instFile == null){
            Toast.makeText(SView.getContext(),"비트를 선택해 주세요", Toast.LENGTH_SHORT).show();
        }
        else if (mPlayerState == PLAY_STOP) {
            mPlayerState = PLAYING;
            initMediaPlayer();
            startPlay();
            updateUI();
        } else if (mPlayerState == PLAYING) {
            mPlayerState = PLAY_PAUSE;
            pausePlay();
            updateUI();
        } else if (mPlayerState == PLAY_PAUSE) {
            mPlayerState = PLAYING;
            startPlay();
            updateUI();
        }
    }

    // 재생 시작
    private void startPlay() {
        Log.v("ProgressRecorder", "startPlay().....");

        try {
            instPlayer.start();

            // SeekBar의 상태를 0.1초마다 체크
            mProgressHandler2.sendEmptyMessageDelayed(0, 100);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void pausePlay() {
        Log.v("ProgressRecorder", "pausePlay().....");

        // 재생을 일시 정지하고
        instPlayer.pause();

        // 재생이 일시정지되면 즉시 SeekBar 메세지 핸들러를 호출한다.
        mProgressHandler2.sendEmptyMessageDelayed(0, 0);
    }

    private void stopPlayBtn(){
        if (instPlayer != null && instPlayer.isPlaying()) {
            mPlayerState = PLAY_STOP;
            stopPlay();
            releaseMediaPlayer();
            updateUI();
        }
    }

    private void stopPlay() {
        Log.v("ProgressRecorder", "stopPlay().....");

        // 재생을 중지하고
        instPlayer.stop();

        // 즉시 SeekBar 메세지 핸들러를 호출한다.
        mProgressHandler2.sendEmptyMessageDelayed(0, 0);
    }

    private void releaseMediaPlayer() {
        Log.v("ProgressRecorder", "releaseMediaPlayer().....");
        instPlayer.release();
        instPlayer = null;
        mPlayProgressBar.setProgress(0);
    }

    public void onCompletion(MediaPlayer mp) {
        if(mp == instPlayer) {
            mPlayerState = PLAY_STOP; // 재생이 종료됨
            // 재생이 종료되면 즉시 SeekBar 메세지 핸들러를 호출한다.
            mProgressHandler2.sendEmptyMessageDelayed(0, 0);
            updateUI();
        }
        else if(mp == recordPlayer){
            rPlayerState = PLAY_STOP;
            rProgressHandler.sendEmptyMessageDelayed(0,0);
            updateRecUI();
        }
    }

    private void updateUI() {
        if (mRecState == REC_STOP) {
            mBtnStartRec.setText("Rec");
            mRecProgressBar.setProgress(0);
        } else if (mRecState == RECORDING)
            mBtnStartRec.setText("Stop");

        if (mPlayerState == PLAY_STOP) {
            mBtnStartPlay.setText("Play");
            mPlayProgressBar.setProgress(0);
        } else if (mPlayerState == PLAYING)
            mBtnStartPlay.setText("Pause");
        else if (mPlayerState == PLAY_PAUSE)
            mBtnStartPlay.setText("Start");
    }

    private void initMediaPlayer() {
        // 미디어 플레이어 생성
        if (instPlayer == null)
            instPlayer = new MediaPlayer();
        else
            instPlayer.reset();

        instPlayer.setOnCompletionListener(this);
        //String fullFilePath = mFilePath + mFileName;

        try {
            instPlayer.setDataSource(instFile.getPath());
            instPlayer.prepare();

            point_max = instPlayer.getDuration();

            mPlayProgressBar.setMax(point_max);

            int maxMinPoint = point_max / 1000 / 60;
            int maxSecPoint = (point_max / 1000) % 60;

            String minMinPointStr = "";
            String minSecPointStr = "";
            String maxMinPointStr = "";
            String maxSecPointStr = "";

            //맥스포인트 시간표시
            if (maxMinPoint < 10)
                maxMinPointStr = "0" + maxMinPoint + ":";
            else
                maxMinPointStr = maxMinPoint + ":";

            if (maxSecPoint < 10)
                maxSecPointStr = "0" + maxSecPoint;
            else
                maxSecPointStr = String.valueOf(maxSecPoint);

            // 시크바 시작부분 설정
            mTvPlayMaxPoint.setText(maxMinPointStr + maxSecPointStr);

            mPlayProgressBar.setProgress(0);
        } catch (Exception e) {
            Log.v("ProgressRecorder", "미디어 플레이어 Prepare Error ==========> " + e);
        }
    }

    private boolean mBtnStartRecOnClick() {
        if(instFile == null){
            Toast.makeText(SView.getContext(),"비트를 선택해 주세요", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (mRecState == REC_STOP) {
            mRecState = RECORDING;
            startRec();
            updateUI();
        } else if (mRecState == RECORDING) {
            mRecState = REC_STOP;
            stopRec();
            updateUI();
        }
        return true;
    }

    // 녹음시작
    private void startRec() {
        mCurRecTimeMs = 0;
        mCurProgressTimeDisplay = 0;

        // SeekBar의 상태를 0.1초후 체크 시작
        mProgressHandler.sendEmptyMessageDelayed(0, 100);



        if (mRecorder == null) {
            mRecorder = new MediaRecorder();
            mRecorder.reset();
        } else {
            mRecorder.reset();
        }

        try {

            //오디오 파일 생성
            SimpleDateFormat timeStampFormat = new SimpleDateFormat(
                    "yyyyMMddHHmmss");
            mFileName =  timeStampFormat.format(new Date()).toString()
                    + "Rec.mp3";
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
            mRecorder.setOutputFile(mFilePath + mFileName);
            mRecorder.prepare();
            mRecorder.start();
            startPlayBtn();

        } catch (IllegalStateException e) {
            Toast.makeText(SView.getContext(), "IllegalStateException", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(SView.getContext(), "IOException",Toast.LENGTH_SHORT).show();
        }
    }

    // 녹음정지
    private void stopRec() {
        try {
            mRecorder.stop();
            mPlayerState = PLAY_STOP;
            stopPlayBtn();
        } catch (Exception e) {

        } finally {
            mRecorder.release();
            mRecorder = null;
            recordEditDialog rEditDialog = new recordEditDialog(SView.getContext(),instFile.getPath(),mFileName);//닉네임 설정 다이얼로그
            rEditDialog.setCancelable(false);
            rEditDialog.show();
        }

        mCurRecTimeMs = -999;
        // SeekBar의 상태를 즉시 체크
        mProgressHandler.sendEmptyMessageDelayed(0, 0);
    }

    public static boolean lock(){
        if(lock == 1){
            lock = 0;
            return true;
        }
        else{
            return false;
        }
    }

    public static boolean unlock(){
        if(lock == 0){
            lock = 1;
            return true;
        }
        else{
            return false;
        }
    }

    //이하 녹음 한 노래 재생할 때 쓰는 함수들
    //비트재생과 변수만 다름
    private void startRecordPlayBtn(){
        if(recFile == null){
            Toast.makeText(SView.getContext(),"파일을 선택해 주세요", Toast.LENGTH_SHORT).show();
        }
        else if (rPlayerState == PLAY_STOP) {
            rPlayerState = PLAYING;
            initRecMediaPlayer();
            startRecPlay();
            updateRecUI();
        } else if (rPlayerState == PLAYING) {
            rPlayerState = PLAY_PAUSE;
            pauseRecPlay();
            updateRecUI();
        } else if (rPlayerState == PLAY_PAUSE) {
            rPlayerState = PLAYING;
            startRecPlay();
            updateRecUI();
        }
    }

    // 재생 시작
    private void startRecPlay() {
        Log.v("ProgressRecorder", "startRecPlay().....");

        try {
            recordPlayer.start();

            // SeekBar의 상태를 0.1초마다 체크
            rProgressHandler.sendEmptyMessageDelayed(0, 100);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void pauseRecPlay() {
        Log.v("ProgressRecorder", "pauseRecPlay().....");

        // 재생을 일시 정지하고
        recordPlayer.pause();

        // 재생이 일시정지되면 즉시 SeekBar 메세지 핸들러를 호출한다.
        rProgressHandler.sendEmptyMessageDelayed(0, 0);
    }

    private void stopRecordPlayBtn(){
        if (instPlayer != null && instPlayer.isPlaying()) {
            mPlayerState = PLAY_STOP;
            stopRecPlay();
            releaseRecMediaPlayer();
            updateRecUI();
        }
    }

    private void stopRecPlay() {
        Log.v("ProgressRecorder", "stopRecPlay().....");

        // 재생을 중지하고
        recordPlayer.stop();

        // 즉시 SeekBar 메세지 핸들러를 호출한다.
        rProgressHandler.sendEmptyMessageDelayed(0, 0);
    }

    private void releaseRecMediaPlayer() {
        Log.v("ProgressRecorder", "releaseRecMediaPlayer().....");
        recordPlayer.release();
        recordPlayer = null;
        rPlayProgressBar.setProgress(0);
    }

    private void updateRecUI() {
        if (rPlayerState == PLAY_STOP) {
            rBtnStartPlay.setText("Play");
            rPlayProgressBar.setProgress(0);
        } else if (rPlayerState == PLAYING)
            rBtnStartPlay.setText("Pause");
        else if (rPlayerState == PLAY_PAUSE)
            rBtnStartPlay.setText("Start");
    }

    private void initRecMediaPlayer() {
        // 미디어 플레이어 생성
        if (recordPlayer == null)
            recordPlayer = new MediaPlayer();
        else
            recordPlayer.reset();

        recordPlayer.setOnCompletionListener(this);
        //String fullFilePath = mFilePath + mFileName;

        try {
            recordPlayer.setDataSource(recFile.getPath());
            recordPlayer.prepare();

            point_max = recordPlayer.getDuration();

            rPlayProgressBar.setMax(point_max);

            int maxMinPoint = point_max / 1000 / 60;
            int maxSecPoint = (point_max / 1000) % 60;

            String minMinPointStr = "";
            String minSecPointStr = "";
            String maxMinPointStr = "";
            String maxSecPointStr = "";

            //맥스포인트 시간표시
            if (maxMinPoint < 10)
                maxMinPointStr = "0" + maxMinPoint + ":";
            else
                maxMinPointStr = maxMinPoint + ":";

            if (maxSecPoint < 10)
                maxSecPointStr = "0" + maxSecPoint;
            else
                maxSecPointStr = String.valueOf(maxSecPoint);

            // 시크바 시작부분 설정
            rTvPlayMaxPoint.setText(maxMinPointStr + maxSecPointStr);

            rPlayProgressBar.setProgress(0);
        } catch (Exception e) {
            Log.v("ProgressRecorder", "미디어 플레이어 Prepare Error ==========> " + e);
        }
    }


    public static SongFrag newInstance() {

        SongFrag songFrag = new SongFrag();
        Bundle bundle = new Bundle();
        songFrag.setArguments(bundle);
        return songFrag;
    }

    private class MyWebViewClient extends WebViewClient{

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url){
            view.loadUrl(url);
            return true;
        }
    }
}
