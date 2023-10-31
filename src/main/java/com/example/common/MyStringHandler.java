package com.example.common;

public class MyStringHandler {
    /**
     *
     * @param raw
     * @return
     */
    public static Long[] getAllIds(String raw){
        String[] raws = raw.split(",");
        Long[] dst = new Long[raws.length];
        for(int i = 0;i<raws.length;i++){
            dst[i] = Long.valueOf(raws[i]);
        }
        return dst;
    }
}
