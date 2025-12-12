package net.eqozqq.mcpesource;

import android.os.AsyncTask;
import org.json.JSONArray;
import org.json.JSONException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class GitHubService {

    private static final String BASE_URL = "https://raw.githubusercontent.com/MCPE-Source/";

    public interface DataCallback {
        void onSuccess(JSONArray data);

        void onError(String error);
    }

    public static void fetchContent(final String type, final DataCallback callback) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    String repo;
                    if (type.equals("maps"))
                        repo = "maps";
                    else if (type.equals("plugins"))
                        repo = "plugins";
                    else if (type.equals("mods"))
                        repo = "mods";
                    else
                        repo = "textures";

                    URL url = new URL(BASE_URL + repo + "/main/" + repo + ".json?t=" + System.currentTimeMillis());
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setUseCaches(false);
                    conn.setDefaultUseCaches(false);
                    conn.addRequestProperty("Cache-Control", "no-cache, max-age=0");
                    conn.addRequestProperty("Pragma", "no-cache");

                    if (conn.getResponseCode() != 200) {
                        return null;
                    }

                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    return result.toString();
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String result) {
                if (result != null) {
                    try {
                        callback.onSuccess(new JSONArray(result));
                    } catch (JSONException e) {
                        callback.onError(e.getMessage());
                    }
                } else {
                    callback.onError("Failed to fetch data of type " + type);
                }
            }
        }.execute();
    }
}
