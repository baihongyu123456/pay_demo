package com.ztzd.ztzdpay.alipay.controller;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeAppPayModel;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.alipay.api.request.AlipayTradeFastpayRefundQueryRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeAppPayResponse;
import com.alipay.api.response.AlipayTradeFastpayRefundQueryResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.ztzd.common.utils.ApiResult;
import com.ztzd.ztzdpay.alipay.model.Wpay;
import com.ztzd.ztzdpay.alipay.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
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

    @Value("${wechat.refund.url}")
    private String wechatRefundUrl;

    @Value("${wechat.refund.query.url}")
    private String wechatRefundQueryUrl;




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
        String orderStr="";
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
            orderStr = response.getBody();
        } catch (AlipayApiException e) {
            e.printStackTrace();
            return new ApiResult().failure();
        }
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
     * @创建时间 2019年7月3日10:50:38
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

    /**
     * 支付宝退款接口
     * @param token          身份标识
     * @param out_trade_no   订单支付时传入的商户订单号,不能和 trade_no同时为空。
     * @param trade_no       支付宝交易号，和商户订单号不能同时为空
     * @param refund_amount  需要退款的金额，该金额不能大于订单金额,单位为元，支持两位小数
     * @param refund_reason  退款的原因说明
     * @param out_request_no 标识一次退款请求，同一笔交易多次退款需要保证唯一，如需部分退款，则此参数必传。
     * @return
     * @throws AlipayApiException
     */
    @RequestMapping(value="alpayRefund",method= RequestMethod.POST)
    @ResponseBody
    public JSONObject alpayRefund(String token,@RequestParam(value="out_trade_no",required=true) String out_trade_no,
                             @RequestParam(value="trade_no") String trade_no,@RequestParam(value="refund_amount",required=true) Double refund_amount,
                             @RequestParam(value="refund_reason",required=false) String refund_reason,@RequestParam(value="out_request_no",required=true) String out_request_no,
                             @RequestParam(value="operator_id",required=false) String operator_id,@RequestParam(value="store_id",required=false) String store_id,
                             @RequestParam(value="terminal_id",required=false) String terminal_id) throws AlipayApiException{
        JSONObject obj = new JSONObject();
       try {
           AlipayClient alipayClient = new DefaultAlipayClient(alipayUrl,alipayAppID,alipayPrivateKey,"json","utf-8",alipayPublicKey,alipaySignType);
           AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();

           //SDK已经封装掉了公共参数，这里只需要传入业务参数。以下方法为sdk的model入参方式(model和biz_content同时存在的情况下取biz_content)。
           //可用方法AlipayTradeRefundModel refundModel = new AlipayTradeRefundModel();来存入到model中.然后用request.setBizModel();请求
			/*request.setBizContent("{" +
			"\"out_trade_no\":\"20150320010101001\"," +
			"\"trade_no\":\"2014112611001004680073956707\"," +
			"\"refund_amount\":200.12," +
			"\"refund_reason\":\"正常退款\"," +
			"\"out_request_no\":\"HZ01RF001\"," +
			"\"operator_id\":\"OP001\"," +
			"\"store_id\":\"NJ_S_001\"," +
			"\"terminal_id\":\"NJ_T_001\"" +
			"  }");*/
           JSONObject jsonObject = new JSONObject();
           //订单支付时传入的商户订单号,不能和 trade_no同时为空。
           jsonObject.put("out_trade_no", out_trade_no);
           //支付宝交易号，和商户订单号不能同时为空
           jsonObject.put("trade_no", trade_no);
           //需要退款的金额，该金额不能大于订单金额,单位为元，支持两位小数
           jsonObject.put("refund_amount", refund_amount);
           //退款的原因说明
           jsonObject.put("refund_reason", refund_reason);
           //标识一次退款请求，同一笔交易多次退款需要保证唯一，如需部分退款，则此参数必传。
           jsonObject.put("out_request_no", out_request_no);
           //商户的操作员编号
           jsonObject.put("operator_id", operator_id);
           //商户的门店编号
           jsonObject.put("store_id", store_id);
           //商户的终端编号
           jsonObject.put("terminal_id", terminal_id);
           request.setBizContent(jsonObject.toString());
           AlipayTradeRefundResponse response = alipayClient.execute(request);
           if(response.isSuccess()){
//			System.out.println("调用成功");
           } else {
               System.out.println("调用失败");
           }
           obj.put("result", "提现成功");
           obj.put("code", 0);
       }catch (Exception e){

       }
        return obj;
    }




    /**
     * 微信退款 需要双向证书验证
     *
     * @param request
     * @param response
     * @param tradeno 微信订单号
     * @param orderno  商家订单号
     * @param callback
     */
    @RequestMapping(value = "/wechatRefund", method = RequestMethod.POST)
    public ApiResult wechatRefund(HttpServletRequest request, HttpServletResponse response, String tradeno, String orderno,
                               String callback) {
        log.info("[/wechatRefund]");
        if (StringUtil.isEmpty(tradeno) && StringUtil.isEmpty(orderno)) {
            return  new ApiResult().success("微信订单号或商家订单号不能为空");
        }

        try {
            Map<String, String> restmap = null;

            Map<String, String> parm = new HashMap<String, String>();
            parm.put("appid", wechatAppID);
            parm.put("mch_id", wechatMchid);
            parm.put("nonce_str", PayUtil.getNonceStr());
            parm.put("transaction_id", tradeno);
            //订单号
            parm.put("out_trade_no", orderno);
            //退款单号
            parm.put("out_refund_no", PayUtil.getRefundNo());
            // 订单总金额 从业务逻辑获取
            parm.put("total_fee", "10");
            // 退款金额
            parm.put("refund_fee", "10");
            parm.put("op_user_id", wechatMchid);
            //退款方式
            parm.put("refund_account", "REFUND_SOURCE_RECHARGE_FUNDS");
            parm.put("sign", PayUtil.getSign(parm, wechatSecret));

//           String xml  = XmlUtil.xmlFormat(parm,false);
            //将请求参数初始化证书相关
           // String ssl = PayUtil.ssl(wechatRefundUrl, xml, request, wechatMchid);
            String restxml = PayUtil.post(wechatRefundUrl, XmlUtil.xmlFormat(parm, false));
            restmap = XmlUtil.xmlParse(restxml);
            Map<String, String> refundMap = new HashMap<>();
        if (CollectionUtil.isNotEmpty(restmap) && "SUCCESS".equals(restmap.get("result_code"))) {
            refundMap.put("transaction_id", restmap.get("transaction_id"));
            refundMap.put("out_trade_no", restmap.get("out_trade_no"));
            refundMap.put("refund_id", restmap.get("refund_id"));
            refundMap.put("out_refund_no", restmap.get("out_refund_no"));
            log.info("订单退款：订单" + restmap.get("out_trade_no") + "退款成功，商户退款单号" + restmap.get("out_refund_no") + "，微信退款单号"
                    + restmap.get("refund_id"));
            return new ApiResult().success("退款成功");

        } else {
            if (CollectionUtil.isNotEmpty(restmap)) {
                log.info("订单退款失败：" + restmap.get("err_code") + ":" + restmap.get("err_code_des"));
            }
            return new ApiResult().success("退款失败--"+restmap.get("err_code_des"));
        }

        } catch (Exception e) {
            e.printStackTrace();
           return new ApiResult().failure("退款失败");
        }
    }


    /**
     * 支付宝退款查询接口
     * @param token          身份标识
     * @param out_trade_no   订单支付时传入的商户订单号,不能和 trade_no同时为空。
     * @param trade_no       支付宝交易号，和商户订单号不能同时为空
     * @return
     * @throws AlipayApiException
     */
    @RequestMapping(value="alpayRefundQuery",method= RequestMethod.POST)
    @ResponseBody
    public ApiResult alpayRefundQuery(String token,@RequestParam(value="out_trade_no",required=true) String out_trade_no,
                             @RequestParam(value="trade_no") String trade_no) {

        JSONObject obj = new JSONObject();
        try {
            AlipayClient alipayClient = new DefaultAlipayClient(alipayUrl,alipayAppID,alipayPrivateKey,"json","utf-8",alipayPublicKey,alipaySignType);
            AlipayTradeFastpayRefundQueryRequest request = new AlipayTradeFastpayRefundQueryRequest();
            /*request.setBizContent("{" +
                    "\"trade_no\":\"20150320010101001\"," +
                    "\"out_trade_no\":\"2014112611001004680073956707\"," +
                    "\"out_request_no\":\"2014112611001004680073956707\"," +
                    "\"org_pid\":\"2088101117952222\"" +
                    "  }");*/

            JSONObject jsonObject = new JSONObject();
            //订单支付时传入的商户订单号,不能和 trade_no同时为空。
            jsonObject.put("out_trade_no", out_trade_no);
            //支付宝交易号，和商户订单号不能同时为空
            jsonObject.put("trade_no", trade_no);
            //请求退款接口时，传入的退款请求号，如果在退款请求时未传入，则该值为创建交易时的外部交易号
            String out_request_no = PayUtil.getNonceStr();
            jsonObject.put("out_request_no", out_request_no);
            request.setBizContent(jsonObject.toString());
            AlipayTradeFastpayRefundQueryResponse response = alipayClient.execute(request);

            if(response.isSuccess()&&("REFUND_SUCCESS".equals(response.getRefundStatus())||"".equals(response.getRefundStatus()))){
                return new ApiResult().success("退款成功");
            } else {
                System.out.println("调用失败");
                return new ApiResult().success("退款失败");
            }

        }catch (Exception e){
            return  new ApiResult().failure("退款失败");
        }
    }
    /**
     * 微信退款查询接口
     * @param transaction_id       微信订单号
     * @param out_trade_no         商户订单号
     * @param out_refund_no        商户退款单号
     * @param refund_id            微信退款单号
     * @throws AlipayApiException
     */
    @RequestMapping(value="wechatRefundQuery",method= RequestMethod.POST)
    @ResponseBody
    public ApiResult wechatRefundQuery(HttpServletRequest request, HttpServletResponse response, String transaction_id,
                                       String out_trade_no, String out_refund_no, String refund_id, String callback) {

        if (StringUtil.isEmpty(transaction_id) && StringUtil.isEmpty(out_trade_no)
                && StringUtil.isEmpty(out_refund_no) && StringUtil.isEmpty(refund_id)) {
            throw new NullPointerException();
        }

        Map<String, String> restmap = null;
        try {
            Map<String, String> parm = new HashMap<String, String>();
            parm.put("appid", wechatAppID);
            parm.put("mch_id", wechatMchid);
            parm.put("transaction_id", transaction_id);
            parm.put("out_trade_no", out_trade_no);
            parm.put("refund_id", refund_id);
            parm.put("out_refund_no", out_refund_no);
            parm.put("nonce_str", PayUtil.getNonceStr());
            parm.put("sign", PayUtil.getSign(parm, wechatSecret));

            String restxml = PayUtil.post(wechatRefundQueryUrl, XmlUtil.xmlFormat(parm, false));
            restmap = XmlUtil.xmlParse(restxml);
            if (CollectionUtil.isNotEmpty(restmap) && "SUCCESS".equals(restmap.get("result_code")) && "SUCCESS".equals(restmap.get("result_code"))) {
                return new ApiResult().success("退款成功");
            }else{
                return  new ApiResult().success("退款失败");
            }
        }catch (Exception e){
            return  new ApiResult().failure("退款失败");
        }
    }

    @RequestMapping("/hello")
    @ResponseBody
    public String hello(){
        return "hello";
    }
}
