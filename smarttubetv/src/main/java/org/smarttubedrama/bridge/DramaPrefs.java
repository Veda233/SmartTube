package org.smarttubedrama.bridge;
import android.content.Context;
import android.content.SharedPreferences;
public final class DramaPrefs {
 private final SharedPreferences p;
 public DramaPrefs(Context c){p=c.getSharedPreferences("smarttube_drama",Context.MODE_PRIVATE);}
 public String server(){return p.getString("server","");}
 public String token(){return p.getString("token","");}
 public void save(String server,String token){p.edit().putString("server",server).putString("token",token).apply();}
}