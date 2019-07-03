package com.ztzd.ztzdpay.alipay.model;

import lombok.Data;

import java.util.Date;
@Data
public class Wpay {

	private String appid;
	
	private String noncestr;
	
	private String partnerid;
	
	private String sign;
	
	private String prepayid;
	
	private String packageValue;
	
	private Date date;

	private String timestamp;
	

	private String wxtradeid;

	private String orderNo;
	
	private String outtradeno;
	
	

	
	
}
