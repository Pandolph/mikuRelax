package com.cn.enter.relax.core;

import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import data.MindTestPack;

/**
 * Created by TangLi on 2015/2/3.
 */
public class Database implements Serializable {

    private ArrayList<Record> data;

    //  TODO: need implement
    public void read(String filename) {
    }

    //  TODO: need implement
    public void write(String filename) {
    }

    public void create() {
        data = new ArrayList<Record>();
    }

    public void add(Record record) {
        if (data == null) {
            Log.e("Relax.DataBase", "Failed to add record: database not opened.");
            return;
        }
        data.add(record);
    }

    public void remove(int index) {
        if (data == null || index < 0 || index >= data.size())
            return;
        data.remove(index);
    }

    // TODO: need implement
    public void clear() {

    }

    public List<Record> getRecords() {
        return data;
    }

    public Record get(int position) {
        if (isEmpty() || position < 0 || position >= data.size())
            return null;
        return data.get(position);
    }

    public boolean isOpened() {
        return !(data == null);
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

//    public boolean addTest(int position, MindTestPack pack) {
//        Record record = get(position);
//        if (record == null) {
//            Log.e("Database", "Record " + position + " not found.");
//            return false;
//        }
//        record.getMindTestPackList().add(pack);
//        return true;
//    }

    @Override
    public String toString() {
        return "Database{" +
                "data=" + data +
                '}';
    }
}
