package com.example.utils;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class TimeUtils {

    public static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static boolean isInRange(LocalDateTime beginTime, LocalDateTime testTime, LocalDateTime endTime){
        //要求是在开始时间之前在结束时间之后
        if(testTime.compareTo(beginTime)>=0 && testTime.compareTo(endTime)<=0 ){
            return true;
        }
        return false;
    }

    public static LocalDateTime parseStringToTime(String time){
        return LocalDateTime.parse(time,DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT));
    }


}
