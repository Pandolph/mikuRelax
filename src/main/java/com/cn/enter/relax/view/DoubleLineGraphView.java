package com.cn.enter.relax.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;

import java.util.LinkedList;
import java.util.ListIterator;

/**
 * Created by turtle on 2015/2/11.
 */
public class DoubleLineGraphView extends BasicLineGraphView {
    private static final String TAG="DoubleLineGraphView";
    private LinkedList<Integer> attentionData;//存放需要绘制的点，本质是队列
    private LinkedList<Integer> relaxData;//存放需要绘制的点，本质是队列
    private int attentionColor;
    private int relaxColor;
    public DoubleLineGraphView(Context context) {
        super(context);
        scale = context.getResources().getDisplayMetrics().density;
        paint=new Paint();
    }

    public DoubleLineGraphView(Context context, AttributeSet attrs) {
        super( context, attrs );
        scale = context.getResources().getDisplayMetrics().density;
        paint=new Paint();
    }

    public DoubleLineGraphView(Context context, AttributeSet attrs, int defStyle) {
        super( context, attrs, defStyle );
        scale = context.getResources().getDisplayMetrics().density;
        paint=new Paint();
    }

    public void initial(int attentionColor,int relaxColor,ViewSize vs,int capacity,int gridWidNum,int gridHeightNum,int dataRangeMin,int dateRangeMax){
        super.initial(0,vs);
        this.attentionColor=attentionColor;
        this.relaxColor=relaxColor;
        this.capacity=capacity;
        this.gridHeightNum=gridHeightNum;
        this.gridWidNum=gridWidNum;
        this.dataRangeMin=dataRangeMin;
        this.dateRangeMax=dateRangeMax;
        this.attentionData=new LinkedList<>();
        this.relaxData=new LinkedList<>();
        this.gridHeight=(frameBottom-frameTop)*1.f/gridHeightNum;
        this.gridWidth=(frameRight-frameLeft)*1.f/gridWidNum;
        this.offset=(frameRight-frameLeft)*1.f/(capacity-1);
        this.dataScale = ((frameBottom - frameTop) * 1.f) / (dateRangeMax - dataRangeMin);
    }

    @Override
    protected  void drawData(Canvas canvas){
        float priorX,priorY;
        int i=0;
        ListIterator<Integer> iter;
        if(!attentionData.isEmpty()) {
            paint.setColor(attentionColor);
            paint.setStrokeWidth(2);
            priorY = frameBottom - (attentionData.peek()-dataRangeMin) * dataScale;
            priorX = frameRight;
            i = 0;
            iter = attentionData.listIterator(attentionData.size());
            while (iter.hasPrevious()) {
                float nowX = frameRight - i * offset;
                float nowY = frameBottom - (iter.previous()-dataRangeMin) * dataScale;
                canvas.drawLine(dip2px(priorX), dip2px(priorY), dip2px(nowX), dip2px(nowY), paint);
                priorX = nowX;
                priorY = nowY;
                ++i;
            }
        }

        if(!relaxData.isEmpty()) {
            paint.setColor(relaxColor);
            paint.setStrokeWidth(2);
            priorY = frameBottom-(relaxData.peek()-dataRangeMin) *dataScale;
            priorX = frameRight;
            i=0;
            iter=relaxData.listIterator(relaxData.size());
            while(iter.hasPrevious()){
                float nowX=frameRight-i*offset;
                float nowY=frameBottom- (iter.previous()-dataRangeMin)*dataScale;
                canvas.drawLine(dip2px(priorX),dip2px(priorY),dip2px(nowX),dip2px(nowY),paint );
                priorX=nowX;
                priorY=nowY;
                ++i;
             }
            //Log.d(TAG,"last i "+i+"frameLeft "+frameLeft+"frameRight "+frameRight+" lastX "+priorX+" offset "+offset);
        }
    }
    public void addRelaxData(int singleData){
        relaxData.offer(singleData);
        if(relaxData.size()>capacity) relaxData.poll();
    }

    public void addAttentionData(int singleData){
        attentionData.offer(singleData);
        if(attentionData.size()>capacity) attentionData.poll();
    }

    @Override
    protected void onDraw(Canvas canvas){
        try {

            drawGrid(canvas);
            if(attentionData.size()==relaxData.size())
                drawData(canvas);
        }catch (Exception e){

        }
    }
}
