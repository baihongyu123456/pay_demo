package com.ztzd.ztzdpay.alipay.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * 随机生成商家的订单号（日期+字母和数字的随机16位）
 */
public class PartnerNoUtil {
    public static  String outtradeno(){
        String format=new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String str="ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(16);
        for (int i=0;i<16;i++){
            char ch=str.charAt(new Random().nextInt(str.length()));
            sb.append(ch);
        }
        return format+sb.toString();
    }
}
