package net.eqozqq.mcpesource;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface GitHubApi {
    @GET("{repo}/main/{repo}.json")
    Call<ResponseBody> getRepoContent(@Path("repo") String repo);
}