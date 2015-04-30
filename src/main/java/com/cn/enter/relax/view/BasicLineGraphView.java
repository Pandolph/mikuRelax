package com.cn.enter.relax.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Queue;

/**
 * Created by turtle on 2015/2/10. 折线图的通用类，仅用于画一条线，没有标尺
 */
public class BasicLineGraphView extends AbstractChartView {
    private static final String TAG="BasicLineGraphView";

    protected LinkedList<Integer> data;//存放需要绘制的点，本质是队列
    protected int dataRangeMin;//数据值域的下限
    protected int dateRangeMax;//数据值域的上限
    protected int capacity;//data的最大深度
    protected int gridWidNum;//横向的格子数目
    protected int gridHeightNum;//纵向的格子数目
    protected float gridWidth;//一个格子的宽度
    protected float gridHeight;//一个格子的高度
    protected float offset;//两个数据点之间的横向间距
    protected float dataScale;//将数据转化为DP的比率。data*dataScale=DP

    public BasicLineGraphView(Context context) {
        super(context);
        scale = context.getResources().getDisplayMetrics().density;
        paint=new Paint();
    }

    public BasicLineGraphView(Context context, AttributeSet attrs) {
        super( context, attrs );
        scale = context.getResources().getDisplayMetrics().density;
        paint=new Paint();
    }

    public BasicLineGraphView(Context context, AttributeSet attrs, int defStyle) {
        super( context, attrs, defStyle );
        scale = context.getResources().getDisplayMetrics().density;
        paint=new Paint();
    }

    public void initial(int color,ViewSize vs,int capacity,int gridWidNum,int gridHeightNum,int dataRangeMin,int dateRangeMax){
        super.initial(color,vs);
        this.capacity=capacity;
        this.gridHeightNum=gridHeightNum;
        this.gridWidNum=gridWidNum;
        this.dataRangeMin=dataRangeMin;
        this.dateRangeMax=dateRangeMax;
        this.data=new LinkedList<>();
        this.gridHeight=(frameBottom-frameTop)*1.f/gridHeightNum;
        this.gridWidth=(frameRight-frameLeft)*1.f/gridWidNum;
        this.offset=(frameRight-frameLeft)*1.f/(capacity-1);
        this.dataScale = ((frameBottom - frameTop) * 1.f) / (dateRangeMax - dataRangeMin);
    }

    /*
    画出折线图的背景网格
     */
    protected void drawGrid(Canvas canvas){
        //画出横纵坐标
        paint.setColor(Color.GRAY);
        paint.setAntiAlias(true);//设置抗锯齿
        paint.setStyle(Paint.Style.STROKE);//设置风格为空心
        paint.setStrokeWidth(8);
        canvas.drawLine(dip2px(frameLeft),dip2px(frameBottom),dip2px(frameRight),dip2px(frameBottom),paint);//横坐标
        canvas.drawLine(dip2px(frameLeft),dip2px(frameBottom),dip2px(frameLeft),dip2px(frameTop),paint);//纵坐标

        //画出方格
        paint.setStrokeWidth(2);

        for(int i=0;i<gridHeightNum;i++)
            canvas.drawLine(dip2px(frameLeft),dip2px(frameTop+(i*gridHeight)),dip2px(frameRight),dip2px(frameTop+i*gridHeight),paint);
        for(int i=0;i<gridWidNum;i++)
            canvas.drawLine(dip2px(frameRight-i*gridWidth),dip2px(frameTop),dip2px(frameRight-i*gridWidth),dip2px(frameBottom),paint);
    }

    /*
    画出折线图
     */
    protected  void drawData(Canvas canvas){
        paint.setColor(figureColor);
        paint.setStrokeWidth(2);
        if(!data.isEmpty()) {
            float priorY = frameBottom-(data.peek()-dataRangeMin)*dataScale;
            float priorX = frameRight;
            int i=0;
            ListIterator<Integer> iter=data.listIterator(data.size());
            while(iter.hasPrevious()){
                float nowX=frameRight-i*offset;
                float nowY=frameBottom-(iter.previous()-dataRangeMin)*dataScale;
                canvas.drawLine(dip2px(priorX),dip2px(priorY),dip2px(nowX),dip2px(nowY),paint );
                priorX=nowX;
                priorY=nowY;
                ++i;
            }
            //Log.d(TAG,"last i "+i+"frameLeft "+frameLeft+"frameRight "+frameRight+" lastX "+priorX+" offset "+offset);
        }
    }

    public void addData(int singleData){
        data.offer(singleData);
        if(data.size()>capacity) data.poll();
    }

    @Override
    protected void onDraw(Canvas canvas){
        try {
            drawGrid(canvas);
            drawData(canvas);
        }catch (Exception e){

        }
    }

}
