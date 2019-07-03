package com.ztzd.ztzdpay.alipay.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeAppPayModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeAppPayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.ztzd.common.utils.ApiResult;
import com.ztzd.ztzdpay.alipay.model.Wpay;
import com.ztzd.ztzdpay.alipay.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * WeChatPayController class
 *
 * @author hongyubai
 * @date 2019/7/2
 */
@Slf4j
@Controller
public class AlipayController {

        @Value("${alipay.appID}")
        private String alipayAppID;

        @Value("${alipay.partner}")
        private String partner;

        @Value("${alipay.private.key}")
        private String alipayPrivateKey;

        @Value("${alipay.public.key}")
        private String alipayPublicKey;

        @Value("${alipay.sign.type}")
        private String alipaySignType;

        @Value("${alipay.notify.url}")
        private String alipayNotifyUrl;

        @Value("${alipay.url}")
        private String alipayUrl;

        @Value("${wechat.appID}")
        private String wechatAppID;

        @Value("${wechat.mchid}")
        private String wechatMchid;

        @Value("${wechat.secret}")
        private String wechatSecret;

        @Value("${wechat.notify.url}")
        private String wechatNotifyUrl;

        @Value("${wechat.orderpay.url}")
        private String wechatOrderPayUrl;

        @Value("${wechat.queryorder.url}")
        private String wechatQueryOrderUrl;





        /**
         * 支付宝支付下单接口
         * @param amount
         * @param token
         * @return
         * @author hongyubai
         * @创建时间 2019年7月2日 09:52:36
         */
     @RequestMapping(value = "/alipay")
     @ResponseBody
    public ApiResult zPay(String amount, String token){
         //支付宝下单1.实例化客户端
         AlipayClient alipayClient = new DefaultAlipayClient(alipayUrl, alipayAppID, alipayPrivateKey , "json", "utf-8", alipayPublicKey, alipaySignType);
         //实例化具体API对应的request类,类名称和接口名称对应,当前调用接口名称：alipay.trade.app.pay
         AlipayTradeAppPayRequest request = new AlipayTradeAppPayRequest();
         //SDK已经封装掉了公共参数，这里只需要传入业务参数。以下方法为sdk的model入参方式(model和biz_content同时存在的情况下取biz_content)。
         AlipayTradeAppPayModel model = new AlipayTradeAppPayModel();
         String outtradeno = PartnerNoUtil.outtradeno();
         //商品标题
         model.setSubject("中天寓客支付宝测试");
         //商家订单号
         model.setOutTradeNo(outtradeno);
         //超时关闭该订单时间
         model.setTimeoutExpress("30m");
         //订单总金额
         model.setTotalAmount(amount);
         //销售产品码，商家和支付宝签约的产品码，为固定值QUICK_MSECURITY_PAY
         model.setProductCode("QUICK_MSECURITY_PAY");
         request.setBizModel(model);
         request.setNotifyUrl(alipayNotifyUrl);
         //这里和普通的接口调用不同，使用的是sdkExecute
         AlipayTradeAppPayResponse response = null;
         try {
             response = alipayClient.sdkExecute(request);
         } catch (AlipayApiException e) {
             e.printStackTrace();
         }
         String orderStr = response.getBody();
         return new ApiResult().success(orderStr);
    }



    /**
     * 支付宝异步通知回调地址
     * @param request
     * @param response
     * @return
     * @author hongyubai
     * @创建时间 2019年7月2日14:35:40
     */
    @RequestMapping(value = "/alipayNotify")
    @ResponseBody
    public String alipayNotify(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String notifyReturn="success";
        //1.通过返回的商家的支付信息来查询表中订单状态是否支付成功，成功则直接返回success
        //获取支付宝交易状态
        String trade_status = new String(request.getParameter("trade_status").getBytes("ISO-8859-1"), "UTF-8");
        //判断是否交易成功，交易成功则判断是否支付成功
        if ("TRADE_SUCCESS".equals(trade_status)) {
            Map<String, String> params = new HashMap<String, String>(16);
            Map requestParams = request.getParameterMap();
            for (Iterator iter = requestParams.keySet().iterator(); iter.hasNext(); ) {
                String name = (String) iter.next();
                String[] values = (String[]) requestParams.get(name);
                String valueStr = "";
                for (int i = 0; i < values.length; i++) {
                    valueStr = (i == values.length - 1) ? valueStr + values[i]
                            : valueStr + values[i] + ",";
                }
                params.put(name, valueStr);
            }
            boolean flag = AlipaySignature.rsaCheckV1(params,alipayPublicKey, "utf-8","RSA2");
            if(!flag){
                notifyReturn="fail";
            }
        }
        return notifyReturn;
    }

    /**
     * 微信支付下单接口
     * @param amount
     * @return
     * @author hongyubai
     * @创建时间 2019年7月3日10:29:52
     */
    @RequestMapping(value = "/wechatorderpay")
    @ResponseBody
    public ApiResult wechatorderpay(String amount,HttpServletRequest req){
        Wpay wpay = new Wpay();
        //获取微信支付的相关参数
        try {
        Map<String, String> parm = new HashMap<String, String>(16);
        parm.put("appid",wechatAppID);
        parm.put("mch_id",wechatMchid);
        parm.put("nonce_str", PayUtil.getNonceStr());
        parm.put("out_trade_no", PayUtil.getTradeNo());
        parm.put("body", "中天寓客-微信支付");
        parm.put("total_fee", amount);
        parm.put("spbill_create_ip", PayUtil.getRemoteAddrIp(req));
        parm.put("notify_url", wechatNotifyUrl);
        parm.put("trade_type", "APP");
        parm.put("sign", PayUtil.getSign(parm, wechatSecret));
        //将得到的参数拼成xml
        String requestXml = PayUtil.getRequestXml(parm);
        //调用统一下单接口
            String result = PayUtil.httpsRequest(wechatOrderPayUrl, "POST", requestXml);
            String xmlFormat = XmlUtil.xmlFormat(parm, false);
            Map<String, String> restmap = XmlUtil.xmlParse(result);
         //判断是否调用这个接口成功,并返回对应的wpay
        if(CollectionUtil.isNotEmpty(restmap) && "SUCCESS".equals(restmap.get("result_code"))){
            String nonceStr = PayUtil.getNonceStr();
            String time = PayUtil.payTimestamp();
            Map<String, String> payMap = new HashMap<String, String>();
            payMap.put("appid", wechatAppID);
            payMap.put("partnerid", wechatMchid);
            payMap.put("prepayid", restmap.get("prepay_id"));
            payMap.put("package", "Sign=WXPay");
            payMap.put("noncestr", nonceStr);
            payMap.put("timestamp", time);
            String sign = PayUtil.getSign(payMap, wechatSecret);
            wpay.setAppid(wechatAppID);
            wpay.setPartnerid(wechatMchid);
            wpay.setPrepayid(restmap.get("prepay_id"));
            wpay.setPackageValue("Sign=WXPay");
            wpay.setSign(sign);
            wpay.setNoncestr(nonceStr);
            wpay.setTimestamp(time);
        }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ApiResult().success(wpay);
    }


    /**
     * 微信异步回调
     * @param request
     * @param response
     * @return
     * @author hongyubai
     * @创建时间 2019年7月3日11:17:06
     */
    @RequestMapping(value = "/wechatNotify")
    @ResponseBody
    public String wechatNotify(HttpServletRequest request, HttpServletResponse response){
        String result = "fail";
        try {
            //得到微信的返回结果并解析
            ServletInputStream inputStream = request.getInputStream();
            String resxml = FileUtil.readInputStream2String(inputStream);
            Map<String, String> map = XmlUtil.xmlParse(resxml);
            //支付成功（请求该接口成功并且支付成功）
            if("SUCCESS".equals(map.get("return_code"))&&"SUCCESS".equals(map.get("result_code"))){
                //查询是否被篡改签名信息
                String sign = map.get("sign");
                String signnow = PayUtil.getSign(map, wechatSecret);
                if(sign.equals(signnow)){
                    result = XmlUtil.setXmlWX("SUCCESS", "OK");
                }else {
                    log.info("签名错误");
                }
            }else{
                log.info("订单支付通知：支付失败，" + map.get("err_code") + ":" + map.get("err_code_des"));
                result = XmlUtil.setXmlWX("fail", "微信返回的交易状态不正确(result_code=" + "SUCCESS" + ")");
            }

        }catch (Exception e){

        }
        return result;
    }


    /**
     * 支付宝查询接口，查询是否支付成功
     * @param token
     * @param out_trade_no
     * @return
     * @author hongyubai
     * @创建时间 2019年7月3日10:17:52
     */
    @RequestMapping(value = "/wechatIfpay")
    @ResponseBody
    public ApiResult wechatIfpay(String token,String out_trade_no){
        //1.先根据订单号查询该订单是否支付成功，成功即接到异步回调返回的支付成功。直接返回支付成功。如果支付状态为不成功，调用查询接口来判断是否支付成功
        try {
            AlipayClient alipayClient = new DefaultAlipayClient(alipayUrl,alipayAppID,alipayPrivateKey,"json","utf-8",alipayPublicKey,alipaySignType);
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            request.setBizContent("{" +"\"out_trade_no\":"+out_trade_no+"}");
            AlipayTradeQueryResponse response= alipayClient.execute(request);
            if(response.isSuccess()){
                //交易状态
                String tradeStatus = response.getTradeStatus();
                if("TRADE_SUCCESS".equals(tradeStatus)){
                    return new ApiResult().success();
                }
                //交易金额
                String totalAmount = response.getTotalAmount();
            }else {
                return new ApiResult().success("支付失败");
            }
        }catch (Exception e){
            return new ApiResult().failure("支付失败");

        }
        return  new ApiResult().success("支付成功");
    }

    /**
     * 微信查询接口，查询是否支付成功
     * @param token
     * @param out_trade_no
     * @return
     * @author hongyubai
     * @创建时间 2019年7月3日10:17:52
     */
    @RequestMapping(value = "/alipayIfpay")
    @ResponseBody
    public ApiResult alipayIfpay(String token,String out_trade_no){
        //1.先根据订单号查询该订单是否支付成功，成功即接到异步回调返回的支付成功。直接返回支付成功。如果支付状态为不成功，调用查询接口来判断是否支付成功
        try {
          Map<String,String> parm = new HashMap<String, String>(16);
          parm.put("appid",wechatAppID);
          parm.put("mch_id",wechatMchid);
          parm.put("out_trade_no",out_trade_no);
          parm.put("nonce_str",PayUtil.getNonceStr());
          parm.put("sign",PayUtil.getSign(parm,wechatSecret));
          String params = XmlUtil.xmlFormat(parm, false);
          String xml = PayUtil.httpsRequest(wechatQueryOrderUrl, "POST", params);
          Map<String, String> restmap = XmlUtil.xmlParse(xml);
          if("SUCCESS".equals(restmap.get("result_code"))&&"SUCCESS".equals(restmap.get("trade_state"))){
              return new ApiResult().success("支付成功");
          }
        }catch (Exception e){
            return new ApiResult().failure("支付失败");

        }
        return  new ApiResult().success("支付成功");
    }
}
