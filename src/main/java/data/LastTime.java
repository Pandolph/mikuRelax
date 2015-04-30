package data;

import java.io.Serializable;

/**
 * Created by Turtle on 2015/1/27.
 */
public class LastTime implements Serializable {
    private int hour;
    private int minute;
    private int second;
    private int millisecond;
    public LastTime(int hour,int minute,int second,int millisecond){
        this.hour=hour;
        this.minute=minute;
        this.second=second;
        this.millisecond=millisecond;
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

    public int getMillisecond() {return millisecond;}

    @Override
    public String toString() {
        return String.format("%02d:%02d:%02d.%03d", hour, minute, second, millisecond);
    }
}
