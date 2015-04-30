package data;

import java.util.ArrayList;

/**
 * Created by turtle on 2015/2/4.
 * 该类是用于正在测试时的数据记录
 */
public class MindTest {
    private long id;//测试编号
    private String name;//测试者姓名
    private int age;//测试者年龄
    private TestDate testDate;//测试时间（包括日期）
    private LastTime lastTime;//测试时长
    private String comment;//备注
    private AnalysisDataPack attentionAnaysis;//分析完成的注意力分布
    private AnalysisDataPack relaxAnaysis;//分析完成的放松度分布
    private ArrayList<Integer> attentionData;//注意力值，偶数位置为时间(ms)，奇数位置为数值
    private ArrayList<Integer> relaxData;//放松度值，偶数位置为时间(ms)，奇数位置为数值
    private ArrayList<Integer> rawData;//原始脑波值，偶数位置为时间(ms)，奇数位置为数值
    private ArrayList<Integer> signalData;//信号质量，偶数位置为时间(ms)，奇数位置为数值
    private ArrayList<Integer> eegPowerData;//脑电能量（频率分量值），9个数据为一组。第一个数据为时间，后8个为数据，依次为delta，theta，lowAlpha，highAlpha，lowBeta，highBeta，low-gamma，mid-gamma

    public MindTest(long id,String name,int age,TestDate testDate){
        this.id=id;
        this.name=name;
        this.age=age;
        this.testDate=testDate;
        this.comment="";
        this.attentionAnaysis=new AnalysisDataPack();
        this.relaxAnaysis=new AnalysisDataPack();
        this.attentionData=new ArrayList<>();
        this.relaxData=new ArrayList<>();
        this.rawData=new ArrayList<>();
        this.signalData=new ArrayList<>();
        this.eegPowerData=new ArrayList<>();

    }

    public ArrayList<Integer> getAttentionData() {
        return attentionData;
    }

    public ArrayList<Integer> getRelaxData() {
        return relaxData;
    }

    public ArrayList<Integer> getRawData() {
        return rawData;
    }

    public ArrayList<Integer> getSignalData() {
        return signalData;
    }

    public ArrayList<Integer> getEegPowerData() {
        return eegPowerData;
    }

    public AnalysisDataPack getAttentionAnaysis() { return attentionAnaysis; }

    public AnalysisDataPack getRelaxAnaysis() { return relaxAnaysis;  }

    public long getId() {
        return id;
    }

    public String getName(){ return name;}

    public int getAge(){ return age;}

    public TestDate getTestDate() {
        return testDate;
    }

    public LastTime getLastTime(){ return lastTime;}

    public String getComment(){ return comment;}

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setLastTime(LastTime lastTime) {
        this.lastTime = lastTime;
    }

    public void addRawData(int data){
        rawData.add(data);
    }

    public void addAttentionData(int time,int data){
        if(0<=data && data<=100) {
            attentionData.add(time);
            attentionData.add(data);
            attentionAnaysis.addData(data);
        }
    }

    public void addRelaxData(int time,int data){
        if(0<=data && data<=100) {
            relaxData.add(time);
            relaxData.add(data);
            relaxAnaysis.addData(data);
        }
    }

    public void addSignalData(int time,int data){
        if(0<=data && data<=200) {
            signalData.add(time);
            signalData.add(data);
        }
    }

    public void addEEGPowerData(int time,int data0,int data1,int data2,int data3,int data4,int data5,int data6,int data7){
        eegPowerData.add(time);
        eegPowerData.add(data0);
        eegPowerData.add(data1);
        eegPowerData.add(data2);
        eegPowerData.add(data3);
        eegPowerData.add(data4);
        eegPowerData.add(data5);
        eegPowerData.add(data6);
        eegPowerData.add(data7);
    }
}
