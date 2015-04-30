package com.cn.enter.relax.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import data.MindTestPack;

/**
 * Created by TangLi on 2015/2/3.
 */
public class Record implements Serializable {

    public enum Sex { MALE, FEMALE };

    public static String NUMBER = "Number";

    public static String NAME = "Name";

    public static String SEX = "Sex";

    public static String AGE = "Age";

    private String number;

    private String name;

    private Sex sex;

    private Integer age;

//    private List<MindTestPack> mindTestPackList = new ArrayList<MindTestPack>();

    private List<Long> mindTestPackList = new ArrayList<Long>();

    public Record() {}

    public Record(String number, String name, Sex sex, Integer age) {
        setNumber(number);
        setName(name);
        setSex(sex);
        setAge(age);
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Sex getSex() {
        return sex;
    }

    public void setSex(Sex sex) {
        this.sex = sex;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(NUMBER, number);
        map.put(NAME, name);
        map.put(SEX, sex == Sex.MALE ? "男" : "女");
        map.put(AGE, age);
        return map;
    }

//    public List<MindTestPack> getMindTestPackList() {
    public List<Long> getMindTestPackList() {
        return mindTestPackList;
    }

    @Override
    public String toString() {
        return "Record{" +
                "number='" + number + '\'' +
                ", name='" + name + '\'' +
                ", sex=" + sex +
                ", age=" + age +
                ", mindTestPackList=" + mindTestPackList +
                '}';
    }
}
