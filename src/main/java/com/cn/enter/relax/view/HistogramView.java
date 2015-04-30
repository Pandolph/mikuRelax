package com.cn.enter.relax.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;

//用于显示注意力值和放松度的柱状图
public class HistogramView extends AbstractChartView {
    private static final String TAG="HistogramView";

    //柱状图大小，单位dp
    private int figureLeft=19;
    private int figureRight=39;
    private int figureBottom=210;
    private int figureMaxHeight=190;//最大高度190

    private int data;//要显示的数据

    public HistogramView(Context context) {
        super(context);
    }

    public HistogramView(Context context, AttributeSet attrs) {
        super( context, attrs );
    }

    public HistogramView(Context context, AttributeSet attrs, int defStyle) {
        super( context, attrs, defStyle );
    }


    public void initial(int color,ViewSize vs){
        super.initial(color, vs);//调用父类的初始化方法
        data=0;
        figureLeft=(int)(frameLeft+(frameRight-frameLeft)/3.);
        figureRight=(int)(frameLeft+2*(frameRight-frameLeft)/3.);
        figureBottom=frameBottom;
        figureMaxHeight=(int)(frameTop+((frameBottom-frameTop))*(19/21.));
    }

    /*
    提供给外部的更新数据接口
     */
    public void setData(int data){
        this.data=data;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        try {//try 为了防止程序返回桌面时报错（canvas可能拿不到）
            if (canvas != null) {
                paint.setColor(Color.GRAY);
                paint.setAntiAlias(true);//设置抗锯齿

                //绘制灰色外框
                paint.setStyle(Paint.Style.STROKE);//设置风格为空心
                canvas.drawRect(dip2px(frameLeft), dip2px(frameTop), dip2px(frameRight), dip2px(frameBottom), paint);// 长方形

                //绘制柱状图
                paint.setStyle(Paint.Style.FILL);//设置风格为实心
                paint.setColor(figureColor);
                float figureTop=figureBottom-data/100.f*figureMaxHeight;
                Log.d(TAG,"figure "+figureTop+" "+figureBottom);
                canvas.drawRect(dip2px(figureLeft), dip2px(figureTop), dip2px(figureRight), dip2px(figureBottom), paint);// 长方形

                //在柱状图上方绘制文字
                paint.setTextSize(dip2px(16));
                String text=""+data;

                // 下面这行是实现文字水平居中
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(text, dip2px(frameRight/2.f),dip2px(figureTop-2.f) , paint);
            }
        } catch (Exception e) {
        }
    }

}
