package com.cn.enter.relax.view;

/**
 * Created by turtle on 2015/2/10.
 */
public class ViewSize {

    //尺寸单位均为DP
    private int left;
    private int top;
    private int right;
    private int bottom;
    /*
                top
            |----------|
      left  |          |  right
            |          |
            |----------|
               bottom
     */
    public ViewSize(int left,int top,int right,int bottom){
        this.left=left;
        this.top=top;
        this.right=right;
        this.bottom=bottom;
    }

    public int getLeft() {
        return left;
    }

    public int getTop() {
        return top;
    }

    public int getRight() {
        return right;
    }

    public int getBottom() {
        return bottom;
    }
}
