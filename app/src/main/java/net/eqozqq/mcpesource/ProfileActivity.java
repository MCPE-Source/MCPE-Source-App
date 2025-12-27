package net.eqozqq.mcpesource;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ProfileActivity extends Activity {

    private List<ContentItem> myItems = new ArrayList<>();
    private ProfileContentAdapter adapter;
    private String currentUserLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(android.R.style.Theme_Holo);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        final ImageView avatarView = findViewById(R.id.profile_avatar);
        final TextView nameView = findViewById(R.id.profile_name);
        Button btnLogout = findViewById(R.id.btn_logout);
        ListView listView = findViewById(R.id.profile_content_list);

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSharedPreferences("app", MODE_PRIVATE).edit().remove("access_token").apply();
                Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });

        adapter = new ProfileContentAdapter(this, myItems, new ProfileContentAdapter.OnActionListener() {
            @Override
            public void onEdit(ContentItem item) {
                Intent editIntent = new Intent(ProfileActivity.this, UploadActivity.class);
                editIntent.putExtra("is_editing", true);

                editIntent.putExtra("title", item.title);
                editIntent.putExtra("version", item.version);
                editIntent.putExtra("shortDescription", item.shortDescription);
                editIntent.putExtra("fullDescription", item.fullDescription);
                editIntent.putExtra("type", item.type);
                editIntent.putExtra("core", item.core);
                editIntent.putExtra("php", item.updatedDate);
                editIntent.putExtra("modType", item.modType);
                editIntent.putExtra("date", item.date);
                editIntent.putExtra("altLink", item.altLink);
                editIntent.putExtra("originalLink", item.originalLink);
                editIntent.putExtra("license", item.license);
                editIntent.putExtra("file_path", item.file);

                startActivity(editIntent);
            }

            @Override
            public void onDelete(final ContentItem item) {
                new AlertDialog.Builder(ProfileActivity.this)
                        .setTitle("Delete Content")
                        .setMessage("Are you sure you want to delete \"" + item.title + "\"? This will create a removal request.")
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new DeleteTask(item).execute();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
        listView.setAdapter(adapter);

        loadUserProfile(avatarView, nameView);
    }

    private void loadUserProfile(final ImageView avatarView, final TextView nameView) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    String token = getSharedPreferences("app", MODE_PRIVATE).getString("access_token", "");
                    if (token.isEmpty()) return null;

                    URL url = new URL("https://api.github.com/user");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Authorization", "token " + token);
                    conn.setRequestProperty("User-Agent", "MCPESource-App");

                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) result.append(line);
                    return result.toString();
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String result) {
                if (result != null) {
                    try {
                        JSONObject json = new JSONObject(result);
                        currentUserLogin = json.optString("login");
                        String avatarUrl = json.optString("avatar_url");

                        nameView.setText(currentUserLogin);
                        if (!avatarUrl.isEmpty()) {
                            Utils.loadImage(avatarUrl, avatarView);
                        }

                        loadUserContent();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }.execute();
    }

    private void loadUserContent() {
        if (currentUserLogin == null) return;

        myItems.clear();
        String[] types = {"maps", "textures", "plugins", "mods"};

        for (final String type : types) {
            GitHubService.fetchContent(ProfileActivity.this, type, new GitHubService.DataCallback() {
                @Override
                public void onSuccess(JSONArray data) {
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject obj = data.optJSONObject(i);
                        if (obj != null) {
                            JSONObject author = obj.optJSONObject("author");
                            if (author != null && currentUserLogin.equals(author.optString("name"))) {
                                myItems.add(new ContentItem(obj, type));
                            }
                        }
                    }
                    sortAndNotify();
                }

                @Override
                public void onError(String error) {}
            });
        }
    }

    private void sortAndNotify() {
        Collections.sort(myItems, new Comparator<ContentItem>() {
            @Override
            public int compare(ContentItem o1, ContentItem o2) {
                if (o1.date == null) return 1;
                if (o2.date == null) return -1;
                return o2.date.compareTo(o1.date);
            }
        });

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });
    }

    private class DeleteTask extends AsyncTask<Void, String, String> {
        private ContentItem item;
        private ProgressDialog progressDialog;

        public DeleteTask(ContentItem item) {
            this.item = item;
        }

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(ProfileActivity.this);
            progressDialog.setMessage("Creating deletion request...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                String token = getSharedPreferences("app", MODE_PRIVATE).getString("access_token", "");
                if (token.isEmpty()) return "Not logged in";

                String repoName = item.type;
                publishProgress("Forking repository...");
                String forkUrl = "https://api.github.com/repos/MCPE-Source/" + repoName + "/forks";
                String forkResponse = sendRequest(forkUrl, "POST", token, null);
                JSONObject forkJson = new JSONObject(forkResponse);
                String userRepo = forkJson.getString("full_name");

                Thread.sleep(2000);

                publishProgress("Updating index...");
                String indexUrl = "https://api.github.com/repos/" + userRepo + "/contents/" + repoName + ".json";

                String upstreamUrl = "https://api.github.com/repos/MCPE-Source/" + repoName + "/contents/" + repoName + ".json";
                String indexResp = sendRequest(upstreamUrl, "GET", token, null);

                JSONObject indexJson = new JSONObject(indexResp);
                String forkFileResp = "";
                try {
                    forkFileResp = sendRequest(indexUrl, "GET", token, null);
                } catch (Exception e) {
                    forkFileResp = indexResp;
                }

                JSONObject forkFileJson = new JSONObject(forkFileResp);
                String sha = forkFileJson.getString("sha");

                String contentBase64 = indexJson.getString("content").replaceAll("\n", "");
                String contentStr = new String(Base64.decode(contentBase64, Base64.DEFAULT));
                JSONArray currentArray = new JSONArray(contentStr);

                JSONArray newArray = new JSONArray();
                for (int i = 0; i < currentArray.length(); i++) {
                    JSONObject obj = currentArray.getJSONObject(i);
                    if (!obj.optString("title").equals(item.title)) {
                        newArray.put(obj);
                    }
                }

                String newContentEncoded = Base64.encodeToString(newArray.toString(2).getBytes(), Base64.NO_WRAP);
                JSONObject updateJson = new JSONObject();
                updateJson.put("message", "Delete " + item.title);
                updateJson.put("content", newContentEncoded);
                updateJson.put("sha", sha);

                sendRequest(indexUrl, "PUT", token, updateJson.toString());

                publishProgress("Creating Pull Request...");
                String prUrl = "https://api.github.com/repos/MCPE-Source/" + repoName + "/pulls";
                JSONObject prJson = new JSONObject();
                prJson.put("title", "Delete " + item.title);
                prJson.put("head", userRepo.split("/")[0] + ":main");
                prJson.put("base", "main");
                prJson.put("body", "Request to delete content: " + item.title);

                sendRequest(prUrl, "POST", token, prJson.toString());

                return "Success";
            } catch (Exception e) {
                e.printStackTrace();
                return "Error: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String s) {
            progressDialog.dismiss();
            if (s.equals("Success")) {
                Utils.showToast(ProfileActivity.this, "Deletion request sent");
            } else {
                Utils.showToast(ProfileActivity.this, s);
            }
        }

        private String sendRequest(String urlSpec, String method, String token, String body) throws Exception {
            URL url = new URL(urlSpec);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setRequestProperty("Authorization", "token " + token);
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            conn.setRequestProperty("User-Agent", "MCPESource-App");

            if (body != null) {
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                os.write(body.getBytes());
                os.close();
            }

            int code = conn.getResponseCode();
            if (code >= 400) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                StringBuilder err = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null)
                    err.append(line);
                throw new Exception("HTTP " + code + ": " + err.toString());
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
                response.append(line);
            return response.toString();
        }
    }
}