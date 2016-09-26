package com.teqnihome.cardtransfer.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * This is database helper class for save {@link BusinessCard}  that extends SQLiteOpenHelper
 * Created by dhiren
 * @author dhiren
 * @see SQLiteOpenHelper
 * @see SQLiteDatabase
 */
public class DataBaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "teqnihome";
    public static final int DATABASE_VERSION = 1;

    private static final String TABLE_BUSINESS_CARD = "business_card";
    private static final String KEY_ID = "id";
    private static final String KEY_CREATED_AT = "created_at";
    private static final String KEY_NAME = "name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_PICTURE = "picture";


    private static final String CREATE_TABLE_BUSINESS_CARD = "CREATE TABLE "
            + TABLE_BUSINESS_CARD + "(" + KEY_ID + " INTEGER PRIMARY KEY," + KEY_NAME
            + " TEXT," + KEY_EMAIL + " TEXT," + KEY_PHONE + " TEXT," + KEY_PICTURE + " TEXT," + KEY_CREATED_AT
            + " DATETIME" + ")";


    public DataBaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_BUSINESS_CARD);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BUSINESS_CARD);
        onCreate(db);
    }

    public List<BusinessCard> getAllBusinessCard() {

        List<BusinessCard> businessCardList = new ArrayList<>();

        String selectQuery = "SELECT  * FROM " + TABLE_BUSINESS_CARD + " ORDER BY "+ KEY_ID +" DESC ";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);


        if (c.moveToFirst()) {
            do {
                BusinessCard td = new BusinessCard();
                td.setId(c.getInt((c.getColumnIndex(KEY_ID))));
                td.setName(c.getString(c.getColumnIndex(KEY_NAME)));
                td.setEmail(c.getString(c.getColumnIndex(KEY_EMAIL)));
                td.setPhone(c.getString(c.getColumnIndex(KEY_PHONE)));
                td.setPicture(c.getString(c.getColumnIndex(KEY_PICTURE)));
                td.setCreatedDate(c.getString(c.getColumnIndex(KEY_CREATED_AT)));
                // adding to todo list
                businessCardList.add(td);
            } while (c.moveToNext());
        }

        return businessCardList;
    }


    public long insertBusinessCard(BusinessCard businessCard) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(KEY_NAME, businessCard.getName());
        values.put(KEY_EMAIL, businessCard.getEmail());
        values.put(KEY_PHONE, businessCard.getPhone());
        values.put(KEY_PICTURE, businessCard.getPicture());
        values.put(KEY_CREATED_AT, getDateTime());

        long row_id = db.insert(TABLE_BUSINESS_CARD, null, values);
        return row_id;

    }

    public BusinessCard getBusinessCardById(int id){
        String selectQuery = "SELECT  * FROM " + TABLE_BUSINESS_CARD + " WHERE "+ KEY_ID+" = " + id;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);

        if (c.moveToFirst()) {
            do {
                BusinessCard td = new BusinessCard();
                td.setId(c.getInt((c.getColumnIndex(KEY_ID))));
                td.setName(c.getString(c.getColumnIndex(KEY_NAME)));
                td.setEmail(c.getString(c.getColumnIndex(KEY_EMAIL)));
                td.setPhone(c.getString(c.getColumnIndex(KEY_PHONE)));
                td.setPicture(c.getString(c.getColumnIndex(KEY_PICTURE)));
                td.setCreatedDate(c.getString(c.getColumnIndex(KEY_CREATED_AT)));
                // adding to todo list
                return td;
            } while (c.moveToNext());
        }

        return null;

    }

    private String getDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }

    public void closeDB() {
        SQLiteDatabase db = this.getReadableDatabase();
        if (db != null && db.isOpen())
            db.close();
    }


}
