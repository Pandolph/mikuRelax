package com.cn.enter.relax;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.cn.enter.relax.core.Database;
import com.cn.enter.relax.core.Record;
import com.cn.enter.relax.view.UserListView;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import data.LastTime;
import data.MindTestPack;


public class UserManagerActivity extends Activity {

    private final String TAG = "Relax.UserManager";

    private final Integer MAX_AGE = 40;

    private final Integer MIN_AGE = 12;

    private ImageButton deleteButton;

    private ImageButton editButton;

    private ImageButton addButton;

    private ImageButton cancelButton;

    private ImageButton finishButton;

    private ImageButton generateReportButton;

    private ImageButton deleteReportButton;

    private ImageButton addTestButton;

    private RelativeLayout addPanel;

    private EditText addNumber;

    private EditText addName;

    private Spinner addSex;

    private Spinner addAge;

    private UserListView listView;

    private RelativeLayout detailPanel;

    private LinearLayout testIdPanel;

    private WebView webView;

    private Database db;

    private TextView highlightBar;

//    private List<Pair<Button, MindTestPack>> testButtonList = new ArrayList<Pair<Button, MindTestPack>>();

    private List<Pair<Button, Long>> testButtonList = new ArrayList<Pair<Button, Long>>();

    private Button currentSelectedTestButton = null;

    private long currentTestId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_manager);

        LastTime flashMotionInfoCache  = new LastTime(1,2,3,4);

        // Get & initialize the widgets
        deleteButton = (ImageButton)this.findViewById(R.id.delete_btn);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { delete(); }
        });

        editButton = (ImageButton)this.findViewById(R.id.edit_btn);
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {  edit(); }
        });
        addButton = (ImageButton)this.findViewById(R.id.add_btn);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { add(); }
        });

        cancelButton = (ImageButton)this.findViewById(R.id.cancel_btn);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancel();
            }
        });

        finishButton = (ImageButton)this.findViewById(R.id.finish_btn);
        finishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish2();
            }
        });

        addPanel = (RelativeLayout)this.findViewById(R.id.add_panel);

        addNumber = (EditText)this.findViewById(R.id.add_number);

        addName = (EditText)this.findViewById(R.id.add_name);

        addSex = (Spinner)this.findViewById(R.id.add_sex);
        String[] sex = {"男", "女"};
        ArrayAdapter<String> sex_adapter = new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item, sex);
        sex_adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        addSex.setAdapter(sex_adapter);

        addAge = (Spinner)this.findViewById(R.id.add_age);
        Integer[] ages = new Integer[MAX_AGE - MIN_AGE + 1];
        for (Integer i = MIN_AGE; i <= MAX_AGE; ++i)
            ages[i - MIN_AGE] = i;
        ArrayAdapter<Integer> age_adapter = new ArrayAdapter<Integer>(this, R.layout.support_simple_spinner_dropdown_item, ages);
        addAge.setAdapter(age_adapter);

        listView = (UserListView)this.findViewById(R.id.list_view);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "List clicked");
                if (((UserListView)parent).getTag() != null){
                    ((View)((UserListView)parent).getTag()).setBackgroundColor(Color.TRANSPARENT);
                }
                ((UserListView)parent).setTag(view);
                view.setBackgroundColor(getResources().getColor(R.color.median_gray));

                deleteButton.setVisibility(Button.VISIBLE);
                editButton.setVisibility(Button.VISIBLE);
                //showDetail(position);
                showDetailPanel();
            }
        });



        detailPanel = (RelativeLayout)this.findViewById(R.id.detail_panel);

        deleteReportButton = (ImageButton)this.findViewById(R.id.delete_report);
        deleteReportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteReport();
            }
        });

        generateReportButton = (ImageButton)this.findViewById(R.id.generate_report);
        generateReportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listView.getTag() == null) {
                    Log.e(TAG, "Illegal state: unable to generate report with selected item.");
                    return;
                }

                int position = listView.getPositionForView((View)listView.getTag());
                Record record = db.get(position);
                if (record == null) {
                    Log.e(TAG, "Record " + position + " not found.");
                    return;
                }

                MindTestPack pack = null;
                for (Pair<Button, Long> pair : testButtonList)
                    if (pair.first == currentSelectedTestButton)
                        pack = getMindTestPack(pair.second);
                if (pack == null) {
                    Log.e(TAG, "Unable to get MindTestPack for record " + position + " of test pack " + currentSelectedTestButton + ".");
                    return;
                }
                generateReport(pack);
            }
        });

        addTestButton = (ImageButton)this.findViewById(R.id.add_test_btn);
        addTestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listView.getTag() == null) {
                    Log.e(TAG, "Illegal state: unable to generate report with selected item.");
                    return;
                }

                int position = listView.getPositionForView((View)listView.getTag());
                Record record = db.get(position);
                if (record == null) {
                    Log.e("Database", "Record " + position + " not found.");
                    return;
                }

                Date date = new Date();
                currentTestId = date.getTime();

                startTest(currentTestId,record.getName(),record.getAge());
            }
        });

        addNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (addNumber.getText().toString().isEmpty() || addName.getText().toString().isEmpty())
                    finishButton.setEnabled(false);
                else
                    finishButton.setEnabled(true);
            }
        });

        addName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (addNumber.getText().toString().isEmpty() || addName.getText().toString().isEmpty())
                    finishButton.setEnabled(false);
                else
                    finishButton.setEnabled(true);
            }
        });

        testIdPanel = (LinearLayout)this.findViewById(R.id.test_id_panel);

        highlightBar = (TextView)this.findViewById(R.id.highlight_bar);

        webView = (WebView)this.findViewById(R.id.web_view);
//        webView.setBackgroundColor(Color.RED);

        // webview prepare
        try {
            File ext = new File(Environment.getExternalStorageDirectory(), "mindwave/");
            File dir = new File(ext, "webview/");
            if (!dir.exists())
                dir.mkdirs();
            File jsDir = new File(dir, "js/");
            if (!jsDir.exists())
                jsDir.mkdirs();
            File dataDir = new File(dir, "data/");
            if (!dataDir.exists())
                dataDir.mkdirs();
            AssetManager assets = getResources().getAssets();
            String[] files = new String[]{"webview/index.html", "webview/js/dygraph-combined.js", "webview/js/interaction.js", "webview/js/svg.min.js"};
            for (String file : files) {
                File dstFile = new File(ext, file);
                InputStream src = assets.open(file);
                //dstFile.createNewFile();
                OutputStream dst = new FileOutputStream(dstFile);
                byte[] buffer = new byte[1024];
                int num;
                while ((num = src.read(buffer)) >= 0) {
                    dst.write(buffer, 0, num);
                }
                src.close();
                dst.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.requestFocus();
        webView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void editComment(String id, String comment) {
                Log.d(TAG, "editComment: " + id + " " + comment);
                setComment(comment);
            }
        }, "jsCallback");
        //webView.loadUrl("javascript:alert(jsCallback.editComment(\"id\", \"comment\"))");

        // Database initialization
        db = loadDatabase();
        if (db == null) {
            Log.i(TAG, "Cannot load database. Creating new database.");
            db = new Database();
            db.create();
        }
        Log.d(TAG, db.toString());

        listView.setDatabase(db);
        listView.notifyDatabaseChanged();

        // UI initialization
        toNormalState();
    }

    @Override
    protected void onResume() {
        saveDatabase(db);
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        saveDatabase(db);
        super.onDestroy();
        detailPanel.removeView(webView);
        webView.removeAllViews();
        webView.destroy();
    }

    private void delete() {
        Log.d(TAG, "Delete");

        if (listView.getTag() == null)
            return;

        View view = (View)listView.getTag();
        int position = listView.getPositionForView(view);

        db.remove(position);
        listView.notifyDatabaseChanged();
        toNormalState();
    }

    private void edit() {
        Log.d(TAG, "Edit");
    }

    private void add() {

        toEditState();
    }

    private void cancel() {
        Log.d(TAG, "cancel");
        toNormalState();
    }

    private void finish2() {
        Log.d(TAG, "finish");

        String number = addNumber.getText().toString();
        String name = addName.getText().toString();
        Record.Sex sex;
        if (addSex.getSelectedItem().equals("男"))
            sex = Record.Sex.MALE;
        else
            sex = Record.Sex.FEMALE;
        int age = Integer.parseInt(addAge.getSelectedItem().toString());

        Record record = new Record(number, name, sex, age);
        db.add(record);
        listView.notifyDatabaseChanged();

        toNormalState();
    }

    private void toNormalState() {
        if (!db.isEmpty() && listView.getTag() != null) {
            deleteButton.setVisibility(Button.VISIBLE);
            editButton.setVisibility(Button.VISIBLE);
        } else {
            deleteButton.setVisibility(Button.INVISIBLE);
            editButton.setVisibility(Button.INVISIBLE);
        }
        addButton.setVisibility(Button.VISIBLE);
        cancelButton.setVisibility(Button.INVISIBLE);
        finishButton.setVisibility(Button.INVISIBLE);

        addPanel.setTranslationY(0.0f);
        listView.setTranslationY(0.0f);
        addPanel.setVisibility(RelativeLayout.INVISIBLE);
        closeInputMethod();

        if (listView.getTag() != null)
            ((View)listView.getTag()).setBackgroundColor(Color.TRANSPARENT);
        listView.setTag(null);

        detailPanel.setVisibility(RelativeLayout.INVISIBLE);
    }

    private void toEditState() {
        addNumber.setText("");
        addName.setText("");
        finishButton.setEnabled(false);

        deleteButton.setVisibility(Button.INVISIBLE);
        editButton.setVisibility(Button.INVISIBLE);
        addButton.setVisibility(Button.INVISIBLE);
        cancelButton.setVisibility(Button.VISIBLE);
        finishButton.setVisibility(Button.VISIBLE);

        addPanel.setVisibility(RelativeLayout.VISIBLE);
        addPanel.setTranslationY(addPanel.getHeight());
        listView.setTranslationY(addPanel.getHeight());

        if (listView.getTag() != null)
            ((View)listView.getTag()).setBackgroundColor(Color.TRANSPARENT);
        listView.setTag(null);

        detailPanel.setVisibility(RelativeLayout.INVISIBLE);
    }

    private void closeInputMethod() {
        InputMethodManager imm = (InputMethodManager) getSystemService(this.INPUT_METHOD_SERVICE);
        if (imm.isActive() && getCurrentFocus() != null)
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    private void showDetailPanel(/*int number*/) {

        detailPanel.setVisibility(RelativeLayout.VISIBLE);
        testIdPanel.removeAllViews();
        testButtonList.clear();
        highlightBar.setVisibility(TextView.INVISIBLE);

        Record record = getCurrentSelectedRecord();
        if (record == null || record.getMindTestPackList().size() == 0) {
            setCurrentSelectedTestButton(null);
        } else {
//            for (MindTestPack pack : record.getMindTestPackList()) {
//                addTestIdButton(pack, false);
//            }
            for (Long id : record.getMindTestPackList()) {
                addTestIdButton(id, false);
            }
            setCurrentSelectedTestButton(testButtonList.get(0).first);
        }

        refreshReportButtonPanel();
    }

    private void showGraph(MindTestPack data) {
        Log.d(TAG, "showGraph: " + data.getId());
        try {
            File dataDir = new File(Environment.getExternalStorageDirectory(), "mindwave/webview/data/");
            // test only
            /*ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(Environment.getExternalStorageDirectory(), "mindwave/0.data")));
            data = (MindTestPack) ois.readObject();
            Log.d(TAG, "data read: " + data.getId() +" " + data.getTestDate().toString() + " " + data.getLastTime().toString());*/

            // write data.js
            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(new File(dataDir, "data.js")), "UTF-8");
            writer.write("config = {\r\n");
            writer.write("\ttest_number: \"" + data.getId() + "\",\r\n");
            writer.write("\ttest_name: \"" + data.getName() + "\",\r\n");
            writer.write("\ttest_age: \"" + data.getAge() + "\",\r\n");
            writer.write("\ttest_date: \"" + data.getTestDate().getDateStr() + "\",\r\n");
            writer.write("\ttest_time: \"" + data.getTestDate().getTimeStr() + "\",\r\n");
            writer.write("\ttest_duration: \"" + data.getLastTime().toString() + "\",\r\n");
            writer.write("\ttest_comment: \"" + data.getComment() + "\",\r\n");
            writer.write("\trate_focus: " + data.getAttentionAnaysis().toString() + ",\r\n");
            writer.write("\trate_relax: " + data.getRelaxAnaysis().toString() + ",\r\n");
            writer.write("}\r\n");
            writer.flush();
            writer.close();
            int[] focusData = data.getAttentionData();
            int[] relaxData = data.getRelaxData();
            int[] noiseData = data.getSignalData();
            double lastNoise = 0;
            // write focus_relax_data.csv
            writer = new OutputStreamWriter(new FileOutputStream(new File(dataDir, "focus_relax_data.csv")), "UTF-8");
            writer.write("time,focus,relax\r\n");
            int num = Math.min(Math.min(focusData.length, relaxData.length), noiseData.length);
            for (int i=0; i<num; i++) {
                if (noiseData[i] >= 0)
                    lastNoise = noiseData[i] / 4.0;
                writer.write(i+",");
                if (focusData[i] >= 0)
                    writer.write(String.valueOf(focusData[i]));
                writer.write(",");
                writer.write(String.valueOf(lastNoise));
                writer.write(",");
                if (relaxData[i] >= 0)
                    writer.write(String.valueOf(relaxData[i]));
                writer.write(",");
                writer.write(String.valueOf(lastNoise));
                writer.write("\r\n");
            }
            writer.flush();
            writer.close();
            // write origin_data.csv
            writer = new OutputStreamWriter(new FileOutputStream(new File(dataDir, "origin_data.csv")), "UTF-8");
            writer.write("time,origin\r\n");
            int[] originData = data.getRawData();
            for (int i=0; i<originData.length; i++) {
                writer.write(String.format("%.3f,%d\r\n", i/512.0, originData[i]));
            }
            writer.flush();
            writer.close();
            // write egg files
            OutputStreamWriter delta_writer = new OutputStreamWriter(new FileOutputStream(new File(dataDir, "delta_data.csv")), "UTF-8");
            OutputStreamWriter theta_writer = new OutputStreamWriter(new FileOutputStream(new File(dataDir, "theta_data.csv")), "UTF-8");
            OutputStreamWriter low_alpha_writer = new OutputStreamWriter(new FileOutputStream(new File(dataDir, "low_alpha_data.csv")), "UTF-8");
            OutputStreamWriter high_alpha_writer = new OutputStreamWriter(new FileOutputStream(new File(dataDir, "high_alpha_data.csv")), "UTF-8");
            OutputStreamWriter low_beta_writer = new OutputStreamWriter(new FileOutputStream(new File(dataDir, "low_beta_data.csv")), "UTF-8");
            OutputStreamWriter high_beta_writer = new OutputStreamWriter(new FileOutputStream(new File(dataDir, "high_beta_data.csv")), "UTF-8");
            OutputStreamWriter low_gamma_writer = new OutputStreamWriter(new FileOutputStream(new File(dataDir, "low_gamma_data.csv")), "UTF-8");
            OutputStreamWriter mid_gamma_writer = new OutputStreamWriter(new FileOutputStream(new File(dataDir, "mid_gamma_data.csv")), "UTF-8");
            int[][] freqData = data.getFreqData();
            delta_writer.write("time,delta\r\n");
            theta_writer.write("time,theta\r\n");
            low_alpha_writer.write("time,low_alpha\r\n");
            high_alpha_writer.write("time,high_alpha\r\n");
            low_beta_writer.write("time,low_beta\r\n");
            high_beta_writer.write("time,high_beta\r\n");
            low_gamma_writer.write("time,low_gamma\r\n");
            mid_gamma_writer.write("time,mid_gamma\r\n");
            for (int i=0; i<freqData[0].length; i++) {
                delta_writer.write(String.format("%d,%.3f\r\n",i, Math.log(freqData[0][i]+1)));
                theta_writer.write(String.format("%d,%.3f\r\n", i, Math.log(freqData[1][i]+1)));
                low_alpha_writer.write(String.format("%d,%.3f\r\n", i, Math.log(freqData[2][i]+1)));
                high_alpha_writer.write(String.format("%d,%.3f\r\n", i, Math.log(freqData[3][i]+1)));
                low_beta_writer.write(String.format("%d,%.3f\r\n", i, Math.log(freqData[4][i]+1)));
                high_beta_writer.write(String.format("%d,%.3f\r\n", i, Math.log(freqData[5][i]+1)));
                low_gamma_writer.write(String.format("%d,%.3f\r\n", i, Math.log(freqData[6][i]+1)));
                mid_gamma_writer.write(String.format("%d,%.3f\r\n", i, Math.log(freqData[7][i]+1)));
            }
            delta_writer.flush(); delta_writer.close();
            theta_writer.flush(); theta_writer.close();
            low_alpha_writer.flush(); low_alpha_writer.close();
            high_alpha_writer.flush(); high_alpha_writer.close();
            low_beta_writer.flush(); low_beta_writer.close();
            high_beta_writer.flush(); high_beta_writer.close();
            low_gamma_writer.flush(); low_gamma_writer.close();
            mid_gamma_writer.flush(); mid_gamma_writer.close();
            // show webview
            String path = new File(Environment.getExternalStorageDirectory(), "mindwave/webview/index.html").getCanonicalPath();
            Log.d(TAG, "Opening webview " + path);
            //webView.loadDataWithBaseURL(null, "","text/html", "utf-8",null);
            webView.clearCache(true);
            webView.clearHistory();
            webView.loadUrl("file://" + path);
        } catch (IOException e) {
            Log.d(TAG, "showGraph exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void generateReport(MindTestPack pack) {
        Log.d(TAG, "Generate report of test id " + pack.getId());
        File src = new File(Environment.getExternalStorageDirectory(), "mindwave/webview/");
        File dst = new File(Environment.getExternalStorageDirectory(), "mindwave/exports/"+pack.getName()+"/"+pack.getTestDate().toDirString()+"/");
        if (!dst.exists())
            dst.mkdirs();
        if (src.exists()) {
            try {
                FileUtils.copyDirectory(src, dst);
                Toast.makeText(this, "报告成功导出至 " + dst.getCanonicalPath(), Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                Toast.makeText(this, "报告导出失败！", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void deleteReport() {
        Log.d(TAG, "Delete report of " + currentSelectedTestButton);

        if (currentSelectedTestButton == null)
            return;

        for (int i = 0; i < testButtonList.size(); ++i) {
            if (currentSelectedTestButton == testButtonList.get(i).first) {
                MindTestPack pack = getMindTestPack(testButtonList.get(i).second);
                Button new_selected = null;

                // TODO :
//                if (i + 1 < testButtonList.size())
//                    new_selected = testButtonList.get(i + 1).first;
//                else if (i - 1 >= 0)
//                    new_selected = testButtonList.get(i - 1).first;

                testButtonList.remove(i);

                Record record = getCurrentSelectedRecord();
                if (record == null) {
                    Log.e(TAG, "Cannot get selected record.");
                    return;
                }
                record.getMindTestPackList().remove(pack.getId());
                testIdPanel.removeView(currentSelectedTestButton);

                setCurrentSelectedTestButton(new_selected);
            }
        }

        refreshReportButtonPanel();
    }

    private void addTestIdButton(Long id) {
        addTestIdButton(id, true);
    }

//    private void addTestIdButton(MindTestPack pack, boolean isUpdateSelectedTestButton) {
    private void addTestIdButton(Long id, boolean isUpdateSelectedTestButton) {
        Button btn = new Button(this){

            private boolean hasInvokedFirstTime = false;

            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);

                if (!hasInvokedFirstTime) {
                    hasInvokedFirstTime = true;
                    this.callOnClick();
                }
                if (this == currentSelectedTestButton) {
                    Log.d(TAG, "Left:" + this.getLeft() + ", right:" + this.getRight());
                    highlightBar.setLeft(this.getLeft() + testIdPanel.getLeft());
                    highlightBar.setRight(this.getRight() + testIdPanel.getLeft());
                    highlightBar.setLeft(this.getLeft() + testIdPanel.getLeft());
//                    highlightBar.setWidth(this.getWidth());
//                    highlightBar.setX(this.getX());
                }
            }
        };

        testIdPanel.addView(btn);
        testButtonList.add(new Pair<Button, Long>(btn, id));
        if (isUpdateSelectedTestButton)
            setCurrentSelectedTestButton(btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (Pair<Button, Long> pair : testButtonList) {
                    if (pair.first == (Button)v) {
                        MindTestPack pack = getMindTestPack(pair.second);
                        if (pack == null) {
                            Log.e(TAG, "Cannot get pack for " + pair.second);
                        } else
                            showGraph(pack);
                        break;
                    }
                }
                setCurrentSelectedTestButton((Button)v, false);
            }
        });
        refreshReportButtonPanel();
    }

    private void refreshReportButtonPanel() {
        if (listView.getTag() == null) {
            Log.d(TAG, "Cannot get tag for list view.");
            return;
        }
        int number = listView.getPositionForView((View)listView.getTag());

        Record record = db.get(number);
        if (record == null || record.getMindTestPackList().size() == 0) {
            generateReportButton.setVisibility(Button.INVISIBLE);
            deleteReportButton.setVisibility(Button.INVISIBLE);
        } else {
            generateReportButton.setVisibility(Button.VISIBLE);
            deleteReportButton.setVisibility(Button.VISIBLE);
        }
    }

    private Record getCurrentSelectedRecord() {
        if (listView.getTag() == null) {
            Log.d(TAG, "Cannot get tag for list view.");
            return null;
        }
        int number = listView.getPositionForView((View)listView.getTag());

        return db.get(number);
    }

    private void setCurrentSelectedTestButton(Button button) {
        setCurrentSelectedTestButton(button, true);
    }
    private void setCurrentSelectedTestButton(Button button, boolean callClick) {
        currentSelectedTestButton = button;

        if (button != null) {

            highlightBar.setVisibility(TextView.VISIBLE);
            webView.setVisibility(WebView.VISIBLE);

            if (callClick)
                button.callOnClick();

        } else {
            highlightBar.setVisibility(TextView.INVISIBLE);
            webView.setVisibility(WebView.INVISIBLE);
        }
    }

/*
Added by turtle
调用该函数，跳转至测试的Activity
 */
    private void startTest(long id,String name,int age){
        Intent intent=new Intent(UserManagerActivity.this,MonitorActivity.class);
        intent.putExtra("id",id);
        intent.putExtra("name",name);
        intent.putExtra("age",age);
        startActivityForResult(intent, 100);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        //可以根据多个请求代码来作相应的操作
        if(0==resultCode)
        {
            // 不用记录，这次记录作废
        }else{
            // 本次记录正常
            Record record = getCurrentSelectedRecord();
            if (record == null) {
                Log.e(TAG, "Cannot get current selected record.");
                return;
            }

            record.getMindTestPackList().add(currentTestId);
            addTestIdButton(currentTestId);

            Log.d(TAG,"finish");

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    void setComment(String comment) {
        MindTestPack pack = null;
        for (Pair<Button, Long> pair: testButtonList) {
            if (pair.first == currentSelectedTestButton) {
                pack = getMindTestPack(pair.second);
                break;
            }
        }

        if (pack == null) {
            Log.e(TAG, "Cannot get pack for current test.");
            return;
        }
        pack.setComment(comment);

        // Saving the pack
        try {
            //获取SDCard目录
            File sdCardDir;
            sdCardDir = Environment.getExternalStorageDirectory();
            File dirFirstFile=new File(sdCardDir,"/mindwave/");//新建一级主目录

            if(!dirFirstFile.exists()){//判断文件夹目录是否存在
                dirFirstFile.mkdir();//如果不存在则创建
            }

            File f = new File(dirFirstFile,pack.getId() + ".data");
            FileOutputStream fos = new FileOutputStream(f);
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            oos.writeObject(pack);
            oos.flush();
            oos.close();

            fos.close();
            Log.e(TAG,"finish");

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    MindTestPack getMindTestPack(Long id) {

        if (id == null) {
            Log.e(TAG, "Null ID");
            return null;
        } else {
            Log.d(TAG, "Getting pack for id " + id);
        }

        MindTestPack pack;

        try {
            //获取SDCard目录
            File sdCardDir;
            sdCardDir = Environment.getExternalStorageDirectory();
            File dirFirstFile=new File(sdCardDir,"/mindwave/");//新建一级主目录
            File f = new File(dirFirstFile, id + ".data");

            FileInputStream fis = new FileInputStream(f);
            ObjectInputStream ois = new ObjectInputStream(fis);
            pack = (MindTestPack)ois.readObject();

            ois.close();
            fis.close();

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        return pack;
    }

    Database loadDatabase() {

        Database db;

        try {
            //获取SDCard目录
            File sdCardDir;
            sdCardDir = Environment.getExternalStorageDirectory();
            File dirFirstFile = new File(sdCardDir,"/mindwave/");//新建一级主目录
            File f = new File(dirFirstFile, "main.db");

            FileInputStream fis = new FileInputStream(f);
            ObjectInputStream ois = new ObjectInputStream(fis);
            db = (Database)ois.readObject();

            ois.close();
            fis.close();

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        return db;
    }

    void saveDatabase(Database db) {

        try {
            //获取SDCard目录
            File sdCardDir;
            sdCardDir = Environment.getExternalStorageDirectory();
            File dirFirstFile = new File(sdCardDir,"/mindwave/");//新建一级主目录
            File f = new File(dirFirstFile, "main.db");

            FileOutputStream fis = new FileOutputStream(f);
            ObjectOutputStream ois = new ObjectOutputStream(fis);

            ois.writeObject(db);

            ois.close();
            fis.close();

        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }
}
