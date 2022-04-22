package cn.cnic.zhongtai.utils;

import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.lang3.StringUtils;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;


@Slf4j
public class HttpsUtils {

    public static String doGet(String url, String queryString) {
        return HttpsUtils.doGet(url,queryString,"UTF-8",true);
    }
    /**
     * 执行一个http/https get请求，返回请求响应的文本数据
     *
     * @param url           请求的URL地址
     * @param queryString   请求的查询参数,可以为null
     * @param charset       字符集
     * @param pretty        是否美化
     * @return              返回请求响应的文本数据
     */
    public static String doGet(String url, String queryString, String charset, boolean pretty) {
        StringBuffer response = new StringBuffer();
        HttpClient client = new HttpClient();
        if(url.startsWith("https")){
            //https请求
            Protocol myhttps = new Protocol("https", new MySSLProtocolSocketFactory(), 443);
            Protocol.registerProtocol("https", myhttps);
        }
        HttpMethod method = new GetMethod(url);
        try {
            if (StringUtils.isNotBlank(queryString))
                //对get请求参数编码，汉字编码后，就成为%式样的字符串
                method.setQueryString(URIUtil.encodeQuery(queryString));
            client.executeMethod(method);
            /*if (method.getStatusCode() == HttpStatus.SC_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(method.getResponseBodyAsStream(), charset));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (pretty)
                        response.append(line).append(System.getProperty("line.separator"));
                    else
                        response.append(line);
                }
                reader.close();
            }else {
                throw new RuntimeException("请求异常，状态码："+method.getStatusCode());
            }*/
            BufferedReader reader = new BufferedReader(new InputStreamReader(method.getResponseBodyAsStream(), charset));
            String line;
            while ((line = reader.readLine()) != null) {
                if (pretty)
                    response.append(line).append(System.getProperty("line.separator"));
                else
                    response.append(line);
            }
            reader.close();
        } catch (URIException e) {
            log.error("执行Get请求时，编码查询字符串“" + queryString + "”发生异常！", e);
        } catch (Exception e) {
            log.error("执行Get请求" + url + "时，发生异常！", e);
        } finally {
            method.releaseConnection();
        }
        return response.toString();
    }

    public static String doPost(String url, Map<String, Object> params) {
        return HttpsUtils.doPost(url,params,"UTF-8",true);
    }
    /**
     * 执行一个http/https post请求，返回请求响应的文本数据
     *
     * @param url       请求的URL地址
     * @param params    请求的查询参数,可以为null
     * @param charset   字符集
     * @param pretty    是否美化
     * @return          返回请求响应的文本数据
     */
    public static String doPost(String url, Map<String, Object> params, String charset, boolean pretty  ) {
        StringBuffer response = new StringBuffer();
        HttpClient client = new HttpClient();
        if(url.startsWith("https")){
            //https请求
            Protocol myhttps = new Protocol("https", new MySSLProtocolSocketFactory(), 443);
            Protocol.registerProtocol("https", myhttps);
        }

        PostMethod method = new PostMethod(url);
        //设置参数的字符集
        method.getParams().setParameter(HttpMethodParams.HTTP_CONTENT_CHARSET,charset);
        //设置post数据
        if (params != null) {
            //HttpMethodParams p = new HttpMethodParams();
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                //p.setParameter(entry.getKey(), entry.getValue());
                method.setParameter(entry.getKey(), entry.getValue().toString());
            }
            //method.setParams(p);
        }
        try {
            client.executeMethod(method);
            int code = method.getStatusCode();
            log.debug("doPost 返回码code："+code);
           /* if (code == HttpStatus.SC_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(method.getResponseBodyAsStream(), charset));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (pretty)
                        response.append(line).append(System.getProperty("line.separator"));
                    else
                        response.append(line);
                }
                reader.close();
            } else {
                throw new RuntimeException("请求异常，状态码："+code);
            }*/
            BufferedReader reader = new BufferedReader(new InputStreamReader(method.getResponseBodyAsStream(), charset));
            String line;
            while ((line = reader.readLine()) != null) {
                if (pretty)
                    response.append(line).append(System.getProperty("line.separator"));
                else
                    response.append(line);
            }
            reader.close();
        } catch (Exception e) {
            log.error("执行Post请求" + url + "时，发生异常！", e);
            throw new RuntimeException("执行Post请求"+url+"出错",e);
        } finally {
            method.releaseConnection();
        }
        return response.toString();
    }

    /**
     * 执行一个http/https post请求， 直接写数据 json,xml,txt
     *
     * @param url       请求的URL地址
     * @param charset   字符集
     * @param pretty    是否美化
     * @return          返回请求响应的文本数据
     */
    public static String writePost(String url, String content, String charset, boolean pretty) {
        StringBuffer response = new StringBuffer();
        HttpClient client = new HttpClient();
        if(url.startsWith("https")){
            //https请求
            Protocol myhttps = new Protocol("https", new MySSLProtocolSocketFactory(), 443);
            Protocol.registerProtocol("https", myhttps);
        }
        PostMethod method = new PostMethod(url);
        try {
            //设置请求头部类型参数
            //method.setRequestHeader("Content-Type","text/plain; charset=utf-8");//application/json,text/xml,text/plain
            //method.setRequestBody(content); //InputStream,NameValuePair[],String
            //RequestEntity是个接口，有很多实现类，发送不同类型的数据
            RequestEntity requestEntity = new StringRequestEntity(content,"application/json",charset);//application/json,text/xml,text/plain
            method.setRequestEntity(requestEntity);
            client.executeMethod(method);
            if (method.getStatusCode() == HttpStatus.SC_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(method.getResponseBodyAsStream(), charset));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (pretty)
                        response.append(line).append(System.getProperty("line.separator"));
                    else
                        response.append(line);
                }
                reader.close();
            }else {
                throw new RuntimeException("请求错误，状态码："+method.getStatusCode());
            }
        } catch (Exception e) {
            log.error("执行Post请求" + url + "时，发生异常！", e);
        } finally {
            method.releaseConnection();
        }
        return response.toString();
    }

    /**
     * 解析出url参数中的键值对
     * 如 "index.jsp?Action=del&id=123"，解析出Action:del,id:123存入map中
     * 解析出参数的键值对
     *      * 如 "Action=del&id=123"，解析出Action:del,id:123存入map中
     * @param str  内容
     * @param type  解析类型：url/param
     * @return  url请求参数部分
     * @author wdd
     */
    public static Map<String, Object> urlAndParamSplit(String str,String type){
        Map<String, Object> mapRequest = new HashMap<String, Object>();
        String[] arrSplit=null;
        String data= null;
        if("url".equals(type)){
            data=TruncateUrlPage(str);
            if(data==null){
                return mapRequest;
            }
        }else{
            data = str;
        }
        arrSplit=data.split("[&]");
        for(String strSplit:arrSplit){
            String[] arrSplitEqual=null;
            arrSplitEqual= strSplit.split("[=]");
            //解析出键值
            if(arrSplitEqual.length>1){
                //正确解析
                mapRequest.put(arrSplitEqual[0], arrSplitEqual[1]);
            }else{
                if(arrSplitEqual[0]!=""){
                    //只有参数没有值，不加入
                    mapRequest.put(arrSplitEqual[0], "");
                }
            }
        }
        return mapRequest;
    }


    /**
     * 去掉url中的路径，留下请求参数部分
     * @param strURL url地址
     * @return url请求参数部分
     * @author lzf
     */
    private static String TruncateUrlPage(String strURL){
        String strAllParam=null;
        String[] arrSplit=null;
        strURL=strURL.trim().toLowerCase();
        arrSplit=strURL.split("[?]");
        if(strURL.length()>1){
            if(arrSplit.length>1){
                for (int i=1;i<arrSplit.length;i++){
                    strAllParam = arrSplit[i];
                }
            }
        }
        return strAllParam;
    }
    /**
     * 将map参数转换成url形式
     *
     * @param map
     * @return
     */
    public static String getUrlParamsByMap(Map<String, Object> map) {
        if (map == null) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            sb.append(entry.getKey() + "=" + entry.getValue());
            sb.append("&");
        }
        String s = sb.toString();
        if (s.endsWith("&")) {
            s = StringUtils.substringBeforeLast(s, "&");
        }
        return s;
    }

    public static void main(String[] args) {
        try {
            Map<String,Object> params = new HashMap<String,Object>();
            params.put("tableId", "44");
           // String str = doPost("http://127.0.0.1:666/hive/httpContentTest", params,"UTF-8",  false);
            log.error(getUrlParamsByMap(params));
            String str = doGet("http://127.0.0.1:666/hive/httpContentTest", getUrlParamsByMap(params),"UTF-8",  false);
            log.info(str );
            JSONObject obj = JSONObject.fromObject(str);
            String code = obj.getString("code");
            String data = obj.getString("data");
            log.info(data );
            //转换json格式
           // JSONArray  array = JSONArray.parseArray(data);
//            for (int i = 0;i<array.size();i++){
//                com.alibaba.fastjson.JSONObject arr = (com.alibaba.fastjson.JSONObject) array.get(i);
//                log.info(arr.toString());
//                Iterator it =arr.entrySet().iterator();
//                while (it.hasNext()) {
//                    Map.Entry<String, Object> entry = (Map.Entry<String, Object>) it.next();
//                    log.info(entry.getKey()+"=============="+entry.getValue());
//                }
//            }
         //  FileUtils.writeJsonToFile(data,"C:\\Users\\lenovo\\Desktop\\wdd.json");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}

