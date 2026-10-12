package com.liskovsoft.smartyoutubetv2.tv.ui.drama;
import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.graphics.Color;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.smarttubedrama.bridge.DramaPrefs;
import org.smarttubedrama.bridge.DramaNasClient;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter;
public final class DramaActivity extends Activity {
 private DramaPrefs prefs;private TextView heading;private ListView list;
 private JSONArray dramas=new JSONArray(),episodes=new JSONArray();
 private int selected=-1;private final Handler handler=new Handler();
 private String pendingCode,pendingToken;private boolean destroyed;
 interface Request {JSONObject run(DramaNasClient c)throws Exception;}
 interface Done {void accept(JSONObject r,Exception error);}
 @Override public void onCreate(Bundle b){super.onCreate(b);prefs=new DramaPrefs(this);
  LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(Color.rgb(18,20,25));root.setPadding(32,24,32,16);
  heading=new TextView(this);heading.setTextSize(25);heading.setTextColor(Color.WHITE);heading.setFocusable(true);root.addView(heading);
  list=new ListView(this);root.addView(list,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
  heading.setOnClickListener(v->configure());list.setOnItemClickListener((p,v,i,id)->choose(i));
  if(prefs.server().length()==0)configure();else if(prefs.token().length()==0)pair();else load();
 }
 private void show(String title,String[] values){heading.setText(title+"  ·  确定键设置 NAS");
  ArrayAdapter<String> a=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,values){
   @Override public View getView(int i,View v,android.view.ViewGroup p){TextView t=(TextView)super.getView(i,v,p);t.setTextColor(Color.WHITE);t.setTextSize(21);t.setMinHeight(60);return t;}};
  list.setAdapter(a);list.requestFocus();
 }
 private void request(final Request req,final Done done){final String server=prefs.server(),token=prefs.token();
  new Thread(()->{JSONObject response=null;Exception error=null;try{DramaNasClient c=new DramaNasClient(server);if(token.length()>0)c.setToken(token);response=req.run(c);}catch(Exception e){error=e;}
   final JSONObject r=response;final Exception e=error;runOnUiThread(()->{if(!destroyed)done.accept(r,e);});},"drama-api").start();
 }
 private void configure(){EditText edit=new EditText(this);edit.setSingleLine(true);edit.setText(prefs.server().length()==0?"http://192.168.1.100:8088":prefs.server());
  new AlertDialog.Builder(this).setTitle("飞牛 NAS 地址").setView(edit).setNegativeButton("取消",null)
   .setPositiveButton("连接",(d,w)->{String u=edit.getText().toString().trim();if(!u.matches("https?://[^/ ]+/?")){toast("NAS 地址格式错误");return;}
    if(!u.equals(prefs.server()))prefs.save(u,"");if(prefs.token().length()==0)pair();else load();}).show();
 }
 private void pair(){show("配对",new String[]{"正在申请配对码…"});
  request(c->c.startPair("SmartTube Drama TV"),(r,e)->{if(e!=null){fail(e);return;}pendingCode=r.optString("code");pendingToken=r.optString("device_token");
   show("配对码："+pendingCode,new String[]{"手机打开 NAS 管理后台批准此设备","批准后自动进入追剧"});poll(0);});
 }
 private void poll(final int i){if(destroyed||pendingCode==null||i>=50)return;
  handler.postDelayed(()->{if(destroyed||pendingCode==null)return;final String code=pendingCode,token=pendingToken;
   request(c->c.pairStatus(code,token),(r,e)->{if(pendingCode==null)return;
    if(e==null&&"paired".equals(r.optString("status"))){prefs.save(prefs.server(),token);pendingCode=null;load();}
    else if(e==null&&"expired".equals(r.optString("status"))){pendingCode=null;toast("配对过期，请重新设置");}
    else poll(i+1);});},4000);
 }
 private void load(){show("我的追剧",new String[]{"正在读取片单…"});request(c->c.catalog(),(r,e)->{if(e!=null){fail(e);return;}
  dramas=r.optJSONArray("dramas");if(dramas==null)dramas=new JSONArray();selected=-1;String[] rows=new String[dramas.length()];
  for(int i=0;i<rows.length;i++)rows[i]=dramas.optJSONObject(i).optString("title","未命名");
  show("我的追剧",rows.length==0?new String[]{"还没有电视剧，请在 NAS 后台添加"}:rows);});
 }
 private void choose(int index){if(selected<0){if(index>=dramas.length())return;selected=index;
  JSONObject drama=dramas.optJSONObject(index);episodes=drama.optJSONArray("episodes");if(episodes==null)episodes=new JSONArray();
  String[] rows=new String[episodes.length()];for(int i=0;i<rows.length;i++){JSONObject ep=episodes.optJSONObject(i);
   rows[i]="第 "+ep.optString("episode_number",String.valueOf(i+1))+" 集  "+ep.optString("title");}
  show(drama.optString("title"),rows.length==0?new String[]{"暂无剧集"}:rows);
 }else{if(index>=episodes.length())return;String id=episodes.optJSONObject(index).optString("video_id");
  if(id.length()==0){toast("无效视频 ID");return;}PlaybackPresenter.instance(this).openVideo(id);}
 }
 @Override public void onBackPressed(){if(selected>=0)load();else super.onBackPressed();}
 @Override protected void onDestroy(){destroyed=true;pendingCode=null;handler.removeCallbacksAndMessages(null);super.onDestroy();}
 private void toast(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
 private void fail(Exception e){show("NAS 连接失败",new String[]{String.valueOf(e.getMessage()),"点击顶部标题重新设置"});}
}