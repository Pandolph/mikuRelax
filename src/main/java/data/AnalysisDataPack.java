package data;

import java.io.Serializable;

/**
 * Created by Turtle on 2015/1/27.
 * 该类是用于存储分析完的数据的，例如注意力值分布和放松度分布.数组里的每个元素的值代表了处于该等级的数据个数，显然level[0]+level[1]+level[2]+level[3]=总的数据数目
 */
public class AnalysisDataPack implements Serializable {
    private int[] level=new int[4];//4个等级的数据数目
    private int sum;
    private int count;

    public AnalysisDataPack(){
        level[0]=0;
        level[1]=0;
        level[2]=0;
        level[3]=0;
        sum = 0;
        count = 0;
    }

/*
从外部添加数据
 */
    public void addData(int data){
        if(0<=data && data<=100) {
            this.sum += data;
            this.count ++;
            if(0<=data && data<25){
                this.level[0] ++;
            }
            if(25<=data && data<50){
                this.level[1] ++;
            }
            if(50<=data && data<75){
                this.level[2] ++;
            }
            if(75<=data && data<=100){
                this.level[3] ++;
            }
        }
    }
    /*
    获得原始记录
     */
    public int getLevel(int level){
        if(level<0 || level>3)
            return -1;
        return this.level[level];
    }

/*
获得归一化的比例（百分比，值分布在0~100）
 */
    public double getNormalizedData(int level){
        double totalDataNum=this.level[0]+this.level[1]+this.level[2]+this.level[3];
        if(0<=level && level<=3){
            return 100*((double)this.level[level])/(double)(totalDataNum);
        }else
            return 0;
    }

    @Override
    public String toString() {
        int levelsum = level[0] + level[1] + level[2] + level[3];
        if (levelsum == 0)
            levelsum = 1;
        int avg = 0;
        if (count > 0)
            avg = sum/count;
        return String.format("[%d,%d,%d,%d,%d]", avg, 100*level[0]/levelsum, 100*level[1]/levelsum, 100*level[2]/levelsum, 100*level[3]/levelsum);
    }
}
