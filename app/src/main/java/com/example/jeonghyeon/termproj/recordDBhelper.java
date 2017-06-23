package com.example.jeonghyeon.termproj;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by JeongHyeon on 2017-03-27.
 * 가사 저장용 DB
 */

public class recordDBhelper extends SQLiteOpenHelper {
    public recordDBhelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version){
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db){
        String sql = "create table if not exists records("
                + "_id integer primary key autoincrement, "
                + "title text, "
                + "author text, "
                + "date text);";

        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        String sql = "drop table if exists records";
        db.execSQL(sql);

        onCreate(db);
    }
}

