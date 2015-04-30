package com.cn.enter.relax.view;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.cn.enter.relax.R;
import com.cn.enter.relax.core.Database;
import com.cn.enter.relax.core.Record;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by TangLi on 2015/2/5.
 */
public class UserListView extends ListView {

    Database database;

    private SimpleAdapter adapter;

    private List<Map<String, Object>> adapterList;

    public UserListView(Context context) {
        super(context);
        init(context);
    }

    public UserListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public UserListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void init(Context context) {
        adapterList = new ArrayList<Map<String, Object>>();
        adapter = new SimpleAdapter(context, adapterList, R.layout.list_view,
                new String[]{Record.NUMBER, Record.NAME, Record.SEX, Record.AGE},
                new int[]{R.id.number, R.id.name, R.id.sex, R.id.age});
        this.setAdapter(adapter);
    }

    public void setDatabase(Database database) {
        this.database = database;
    }

    public void notifyDatabaseChanged() {
        if (database == null) {
            Log.e("UserListView", "Database unset.");
            return;
        } else
        Log.d("UserListView", "Database set:" + database);

        for (Record record : database.getRecords()) {
            boolean found = false;
            for (Map<String, Object> map : adapterList) {
                if (record.getNumber().equals(map.get("Number"))) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                adapterList.add(record.toMap());
                Log.d("UserListView", "New data added.");
            }
        }

        Iterator<Map<String, Object>> it = adapterList.iterator();
        while (it.hasNext()) {
            Map<String, Object> map = it.next();
            boolean found = false;
            for (Record record : database.getRecords()) {
                if (record.getNumber().equals(map.get("Number"))) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                it.remove();
                Log.d("UserListView", "One data removed.");
            }
        }

        adapter.notifyDataSetChanged();
        if (this.getTag() != null) {
            ((View)this.getTag()).setBackgroundColor(Color.TRANSPARENT);
            this.setTag(null);
        }
    }
}
