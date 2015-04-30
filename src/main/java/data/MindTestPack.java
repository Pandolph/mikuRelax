package data;

import java.io.Serializable;
/**
 * Created by Turtle on 2015/1/27.
 * 所有带Pack后缀的都使用紧凑的数组以便序列化
 */
public class MindTestPack implements Serializable {
    private long id;//测试编号
    private String name;//测试者姓名
    private int age;//测试者年龄
    private TestDate testDate;//测试时间（包括日期）
    private LastTime lastTime;//测试时长
    private String comment;//备注
    private AnalysisDataPack attentionAnalysis;//分析完成的注意力分布
    private AnalysisDataPack relaxAnalysis;//分析完成的放松度分布
    private int[] attentionData;//注意力值，偶数位置为时间(ms)，奇数位置为数值
    private int[] relaxData;//放松度值，偶数位置为时间(ms)，奇数位置为数值
    private int[] rawData;//原始脑波值，因为蓝牙传输过程中有较大的延时（100ms），发包时是攒够一堆包发过来，所以没有时间戳。可以认为是标准的512Hz，或者是在测试时间内平均分布的。
    private int[] signalData;//信号质量，偶数位置为时间(ms)，奇数位置为数值
    private int[] eegPowerData;//脑电能量（频率分量值），9个数据为一组。第一个数据为时间，后8个为数据，依次为delta，theta，lowAlpha，highAlpha，lowBeta，highBeta，low-gamma，mid-gamma

    public MindTestPack(MindTest mt){
        this.id=mt.getId();
        this.name=mt.getName();
        this.age=mt.getAge();
        this.testDate=mt.getTestDate();
        this.lastTime=mt.getLastTime();
        this.comment=mt.getComment();
        this.attentionAnalysis=mt.getAttentionAnaysis();
        this.relaxAnalysis=mt.getRelaxAnaysis();

        this.attentionData=new int[mt.getAttentionData().size()];
        for(int i=0;i<mt.getAttentionData().size();i++){
            this.attentionData[i]= mt.getAttentionData().get(i);
        }

        this.relaxData=new int[mt.getRelaxData().size()];
        for(int i=0;i<mt.getRelaxData().size();i++){
            this.relaxData[i]= mt.getRelaxData().get(i);
        }

        this.rawData=new int[mt.getRawData().size()];
        for(int i=0;i<mt.getRawData().size();i++){
            this.rawData[i]= mt.getRawData().get(i);
        }

        this.signalData=new int[mt.getSignalData().size()];
        for(int i=0;i<mt.getSignalData().size();i++){
            this.signalData[i]= mt.getSignalData().get(i);
        }

        this.eegPowerData=new int[mt.getEegPowerData().size()];
        for(int i=0;i<mt.getEegPowerData().size();i++){
            this.eegPowerData[i]= mt.getEegPowerData().get(i);
        }
    }

    public long getId() {
        return id;
    }

    public String getName(){ return name;}

    public int getAge(){ return age;}

    public TestDate getTestDate() {
        return testDate;
    }

    public LastTime getLastTime() {
        return lastTime;
    }

    public String getComment() {
        return comment;
    }

    public AnalysisDataPack getAttentionAnaysis() {
        return attentionAnalysis;
    }

    public AnalysisDataPack getRelaxAnaysis() {
        return relaxAnalysis;
    }

    public int[] getAttentionData() {
        return roundData(attentionData, 2, 1);
    }

    public int[] getRelaxData() {
        return roundData(relaxData, 2, 1);
    }

    public int[] getRawData() {
        return rawData;
    }

    public int[] getSignalData() {
        return roundData(signalData, 2, 1);
    }

    public int[] getDeltaData() {
        return roundData(eegPowerData, 9, 1);
    }
    public int[] getThetaData() {
        return roundData(eegPowerData, 9, 2);
    }
    public int[] getLowAlphaData() {
        return roundData(eegPowerData, 9, 3);
    }
    public int[] getHighAlphaData() {
        return roundData(eegPowerData, 9, 4);
    }
    public int[] getLowBetaData() {
        return roundData(eegPowerData, 9, 5);
    }
    public int[] getHighBetaData() {
        return roundData(eegPowerData, 9, 6);
    }
    public int[] getLowGammaData() {
        return roundData(eegPowerData, 9, 7);
    }
    public int[] getMidGammaData() {
        return roundData(eegPowerData, 9, 8);
    }

    public int[][] getFreqData() {
        int[] delta = getDeltaData();
        int[] theta = getThetaData();
        int[] lowAlpha = getLowAlphaData();
        int[] highAlpha = getHighAlphaData();
        int[] lowBeta = getLowBetaData();
        int[] highBeta = getHighBetaData();
        int[] lowGamma = getLowGammaData();
        int[] midGamma = getMidGammaData();
        return new int[][] {delta, theta, lowAlpha, highAlpha, lowBeta, highBeta, lowGamma, midGamma};
    }

    public int getStartDelay() {
        int min = Integer.MAX_VALUE;
        if (attentionData.length > 0)
            min = Math.min(min, attentionData[0]);
        if (relaxData.length > 0)
            min = Math.min(min, relaxData[0]);
        if (signalData.length > 0)
            min = Math.min(min, signalData[0]);
        if (eegPowerData.length > 0)
            min = Math.min(min, eegPowerData[0]);
        if (min == Integer.MAX_VALUE)
            min = 0;
        return min;
    }

    private int[] roundData(int[] data, int setSize, int setIndex) {
        int num = data.length / setSize;
        if (num <= 0)
            return new int[0];
        int start = getStartDelay() % 1000;
        int time = (int)Math.round((data[setSize*(num-1)]-start) / 1000.0) + 1;
        int[] result = new int[time];
        int i;
        for (i=0; i<time; i++) {
            result[i] = -1;
        }
        for (i=0; i<num; i++) {
            result[(int)Math.round((data[setSize*i]-start)/1000.0)] = data[setSize*i+setIndex];
        }
        return result;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
