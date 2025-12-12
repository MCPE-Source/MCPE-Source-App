package net.eqozqq.mcpesource;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SearchResultsActivity extends Activity {

    private ListView listView;
    private ProgressBar progressBar;
    private TextView statusText;
    private ContentAdapter adapter;
    private List<ContentItem> results = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(android.R.style.Theme_Holo);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);

        listView = findViewById(R.id.search_list);
        progressBar = findViewById(R.id.search_progress);
        statusText = findViewById(R.id.search_status);

        String query = getIntent().getStringExtra("query");
        String criteria = getIntent().getStringExtra("criteria");
        String category = getIntent().getStringExtra("category");

        adapter = new ContentAdapter(this, results);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ContentItem item = results.get(position);
                Intent intent = new Intent(SearchResultsActivity.this, DetailActivity.class);
                intent.putExtra("title", item.title);
                intent.putExtra("version", item.version);
                intent.putExtra("shortDescription", item.shortDescription);
                intent.putExtra("fullDescription", item.fullDescription);
                intent.putExtra("thumbnail", item.getFullThumbnailUrl());
                intent.putExtra("file", item.file);
                intent.putExtra("type", item.type);

                intent.putExtra("core", item.core);
                intent.putExtra("modType", item.modType);
                intent.putExtra("date", item.date);
                intent.putExtra("updatedDate", item.updatedDate);
                intent.putExtra("authorName", item.authorName);
                intent.putExtra("authorAvatar", item.authorAvatar);
                intent.putExtra("altLink", item.altLink);
                intent.putExtra("originalLink", item.originalLink);
                intent.putExtra("license", item.license);

                if (item.screenshots != null && !item.screenshots.isEmpty()) {
                    intent.putExtra("screenshots_count", item.screenshots.size());
                    for (int i = 0; i < item.screenshots.size(); i++) {
                        intent.putExtra("screenshot_" + i, item.getExampleScreenshotUrl(i));
                    }
                }

                startActivity(intent);
            }
        });

        performGlobalSearch(query, criteria, category);
    }

    private void performGlobalSearch(final String query, final String criteria, final String category) {
        new AsyncTask<Void, Void, List<ContentItem>>() {
            @Override
            protected List<ContentItem> doInBackground(Void... voids) {
                List<ContentItem> allItems = new ArrayList<>();
                String[] types;

                if (category == null || category.equals("all")) {
                    types = new String[]{"maps", "textures", "plugins", "mods"};
                } else {
                    types = new String[]{category};
                }

                for (String type : types) {
                    try {
                        String repo = type;
                        URL url = new URL("https://raw.githubusercontent.com/MCPE-Source/" + repo + "/main/" + repo + ".json?t=" + System.currentTimeMillis());
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("GET");

                        if (conn.getResponseCode() == 200) {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                            StringBuilder result = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                result.append(line);
                            }

                            JSONArray data = new JSONArray(result.toString());
                            for (int i = 0; i < data.length(); i++) {
                                JSONObject obj = data.optJSONObject(i);
                                if (obj != null) {
                                    allItems.add(new ContentItem(obj, type));
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                List<ContentItem> filtered = new ArrayList<>();
                String q = query.toLowerCase();

                for (ContentItem item : allItems) {
                    boolean match = false;
                    switch (criteria) {
                        case "title":
                            if (item.title != null && item.title.toLowerCase().contains(q)) match = true;
                            break;
                        case "desc":
                            if (item.shortDescription != null && item.shortDescription.toLowerCase().contains(q)) match = true;
                            if (item.fullDescription != null && item.fullDescription.toLowerCase().contains(q)) match = true;
                            break;
                        case "ver":
                            if (item.version != null && item.version.toLowerCase().contains(q)) match = true;
                            break;
                    }
                    if (match) filtered.add(item);
                }

                Collections.sort(filtered, new Comparator<ContentItem>() {
                    @Override
                    public int compare(ContentItem o1, ContentItem o2) {
                        if (o1.date == null) return 1;
                        if (o2.date == null) return -1;
                        return o2.date.compareTo(o1.date);
                    }
                });

                return filtered;
            }

            @Override
            protected void onPostExecute(List<ContentItem> foundItems) {
                progressBar.setVisibility(View.GONE);
                statusText.setVisibility(View.GONE);
                listView.setVisibility(View.VISIBLE);

                results.clear();
                results.addAll(foundItems);
                adapter.notifyDataSetChanged();

                if (results.isEmpty()) {
                    Utils.showToast(SearchResultsActivity.this, "No results found");
                }
            }
        }.execute();
    }
}