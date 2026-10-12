package org.smarttubedrama.bridge;
import android.os.Looper;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
public final class DramaNasClient {
 private final String url;
 private String token="";
 public DramaNasClient(String base){if(base==null||!(base.startsWith("http://")||base.startsWith("https://")))throw new IllegalArgumentException("Invalid NAS URL");url=base.replaceAll("/+$","")+"/api/v1";}
 public void setToken(String t){token=t;}
 public JSONObject startPair(String name)throws Exception{return call("POST","/pair/start",new JSONObject().put("name",name),false);}
 public JSONObject pairStatus(String code,String provisionalToken)throws Exception{String old=token;token=provisionalToken;try{return call("GET","/pair/status?code="+URLEncoder.encode(code,"UTF-8"),null,true);}finally{token=old;}}
 public JSONObject catalog()throws Exception{return call("GET","/tv/catalog",null,true);}
 private JSONObject call(String method,String path,JSONObject body,boolean auth)throws Exception{
  if(Looper.myLooper()==Looper.getMainLooper())throw new IllegalStateException("Network on main thread");
  HttpURLConnection c=(HttpURLConnection)new URL(url+path).openConnection();
  c.setRequestMethod(method);c.setConnectTimeout(8000);c.setReadTimeout(12000);
  c.setRequestProperty("Accept","application/json");
  if(auth){if(token.length()==0)throw new IOException("Device not paired");c.setRequestProperty("Authorization","Bearer "+token);}
  try{
   if(body!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=utf-8");OutputStream out=null;try{out=c.getOutputStream();out.write(body.toString().getBytes("UTF-8"));}finally{if(out!=null)out.close();}}
   int status=c.getResponseCode();InputStream in=status<400?c.getInputStream():c.getErrorStream();StringBuilder sb=new StringBuilder();
   if(in!=null){BufferedReader reader=new BufferedReader(new InputStreamReader(in,"UTF-8"));try{String line;while((line=reader.readLine())!=null){sb.append(line);if(sb.length()>8388608)throw new IOException("Response too large");}}finally{reader.close();}}
   if(status>=400)throw new IOException("NAS HTTP "+status+": "+sb.toString());
   return new JSONObject(sb.toString());
  }finally{c.disconnect();}
 }
}