package net.eqozqq.mcpesource;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONException;
import java.io.File;
import java.io.IOException;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class GitHubService {

    private static final String BASE_URL = "https://raw.githubusercontent.com/MCPE-Source/";
    private static GitHubApi api;

    public interface DataCallback {
        void onSuccess(JSONArray data);
        void onError(String error);
    }

    private static GitHubApi getApi(Context context) {
        if (api == null) {
            File httpCacheDirectory = new File(context.getCacheDir(), "http-cache");
            int cacheSize = 10 * 1024 * 1024;
            Cache cache = new Cache(httpCacheDirectory, cacheSize);

            OkHttpClient client = new OkHttpClient.Builder()
                    .cache(cache)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            api = retrofit.create(GitHubApi.class);
        }
        return api;
    }

    public static void fetchContent(Context context, final String type, final DataCallback callback) {
        String repo;
        if (type.equals("maps")) repo = "maps";
        else if (type.equals("plugins")) repo = "plugins";
        else if (type.equals("mods")) repo = "mods";
        else repo = "textures";

        getApi(context).getRepoContent(repo).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String jsonString = response.body().string();
                        callback.onSuccess(new JSONArray(jsonString));
                    } catch (IOException | JSONException e) {
                        callback.onError(e.getMessage());
                    }
                } else {
                    callback.onError("Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }
}