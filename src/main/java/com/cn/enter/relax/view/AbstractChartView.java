package com.cn.enter.relax.view;

import android.content.Context;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by turtle on 2015/2/10.
 */
public class AbstractChartView extends View{
    protected static float scale;
    protected Paint paint;
    protected int figureColor;//图的颜色

    //绘图外框的大小，单位dp
    protected int frameLeft=0;
    protected int frameRight=0;
    protected int frameTop=0;
    protected int frameBottom=0;

    public AbstractChartView(Context context) {
        super(context);
        scale = context.getResources().getDisplayMetrics().density;
        paint=new Paint();
    }

    public AbstractChartView(Context context, AttributeSet attrs) {
        super( context, attrs );
        scale = context.getResources().getDisplayMetrics().density;
        paint=new Paint();
    }

    public AbstractChartView(Context context, AttributeSet attrs, int defStyle) {
        super( context, attrs, defStyle );
        scale = context.getResources().getDisplayMetrics().density;
        paint=new Paint();
    }

    /*
将DP转换为PX
 */
    public static float dip2px(int dpValue) {
        return (dpValue * scale + 0.5f);
    }

    /*
将DP转换为PX
*/
    public static float dip2px(float dpValue) {
        return (dpValue * scale + 0.5f);
    }

    public void initial(int figureColor,ViewSize vs){
        this.figureColor=figureColor;
        frameLeft=vs.getLeft();
        frameRight=vs.getRight();
        frameTop=vs.getTop();
        frameBottom=vs.getBottom();
    }
}
