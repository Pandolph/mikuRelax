package data;

import java.io.Serializable;

/**
 * Created by Turtle on 2015/1/27.
 */
public class TestDate implements Serializable {
    private int year;
    private int month;
    private int day;
    private int hour;
    private int minute;
    private int second;
    public TestDate(int year,int month,int day,int hour,int minute,int second){
        this.year=year;
        this.month=month;
        this.day=day;
        this.hour=hour;
        this.minute=minute;
        this.second=second;
    }

    public int getMonth() {
        return month;
    }

    public int getDay() {
        return day;
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }

    public int getSecond() {
        return second;
    }

    public int getYear() {

        return year;
    }

    public String getDateStr() {
        return String.format("%04d/%02d/%02d", year, month, day);
    }

    public String getTimeStr() {
        return String.format("%02d:%02d:%02d", hour, minute, second);
    }

    @Override
    public String toString() {
        return getDateStr() + " " + getTimeStr();
    }

    public String toDirString() {
        return String.format("%04d-%02d-%02d_%02d-%02d-%02d", year, month, day, hour, minute, second);
    }
}
