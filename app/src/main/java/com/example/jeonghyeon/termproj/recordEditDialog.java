package com.example.jeonghyeon.termproj;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

/**
 * Created by JeongHyeon on 2017-06-10.
 */

public class recordEditDialog extends Dialog {
    private Button titleEditBtn;
    private Button noSave;
    private EditText titleEdit;
    private TextView check;
    private TextView tag;
    private String author;
    private String date;
    private File src;
    recordDBhelper rHelper;
    SQLiteDatabase rdb;
    String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath();

    public recordEditDialog(@NonNull Context context, String inst, String file) {
        super(context);
        author = inst.substring(inst.lastIndexOf('-')+1,inst.lastIndexOf('.'));
        src = new File(dirPath + "/.gilrhyme_records/" ,file);
        date = file.substring(0,4) + "-" + file.substring(4,6) + "-" + file.substring(6,8) + " " + file.substring(8,10) + ":" + file.substring(10,12) + ":" + file.substring(12,14);

    }


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 다이얼로그 외부 화면 흐리게 표현
        WindowManager.LayoutParams lpWindow = new WindowManager.LayoutParams();
        lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        lpWindow.dimAmount = 0.8f;
        getWindow().setAttributes(lpWindow);

        setContentView(R.layout.record_save);

        titleEditBtn = (Button) findViewById(R.id.titleEditBtn);
        noSave = (Button) findViewById(R.id.noSave);
        titleEdit = (EditText)findViewById(R.id.titleEdit);
        tag = (TextView)findViewById(R.id.reordTag);
        check = (TextView)findViewById(R.id.check);
        rHelper = new recordDBhelper(getContext(), "record_" + MainActivity.userNickname +".db", null, 1);//각 사용자별 별개의 DB
        tag.setText("(prod by. " + author + ")-" + MainActivity.userNickname + ".mp3");
        rdb = rHelper.getWritableDatabase();
        rHelper.onCreate(rdb);



        // 클릭 이벤트 셋팅
        titleEditBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = titleEdit.getText().toString();
                if(title.contains("[") || title.contains("]")){
                    check.setText("[와 ]는 사용할 수 없습니다.");
                }
                else{
                    title = titleEdit.getText().toString() + tag.getText().toString();
                    File newRecord = new File(dirPath + "/.gilrhyme_records/" ,title);
                    if(newRecord.exists()){
                        check.setText("이미 존재하는 이름입니다.");
                    }
                    else{
                        Toast.makeText(getContext(),"file: " + newRecord.getPath() + "\ndate: " + date,Toast.LENGTH_LONG).show();
                        src.renameTo(newRecord);
                        ContentValues values = new ContentValues();
                        values.put("title", newRecord.getName().substring(0,newRecord.getName().lastIndexOf('-')));
                        values.put("author",author);
                        values.put("date", date);
                        rdb.insert("records",null,values);
                        recordEditDialog.super.dismiss();
                    }
                }

            }
        });

        noSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                src.delete();
                recordEditDialog.super.dismiss();
            }
        });

    }
}
