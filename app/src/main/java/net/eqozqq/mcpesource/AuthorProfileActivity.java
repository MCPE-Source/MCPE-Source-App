package net.eqozqq.mcpesource;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class AuthorProfileActivity extends Activity {

    private List<ContentItem> authorItems = new ArrayList<>();
    private ContentAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(android.R.style.Theme_Holo);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_author_profile);

        String name = getIntent().getStringExtra("name");
        String avatar = getIntent().getStringExtra("avatar");

        ImageView avatarView = findViewById(R.id.profile_avatar_detail);
        TextView nameView = findViewById(R.id.profile_name_detail);
        ListView listView = findViewById(R.id.profile_content_list);

        nameView.setText(name);
        if (avatar != null && !avatar.isEmpty()) {
            Utils.loadImage(avatar, avatarView);
        }

        adapter = new ContentAdapter(this, authorItems);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ContentItem item = authorItems.get(position);
                Intent intent = new Intent(AuthorProfileActivity.this, DetailActivity.class);
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

        loadAuthorContent(name);
    }

    private void loadAuthorContent(final String authorName) {
        GitHubService.fetchContent("maps", new GitHubService.DataCallback() {
            @Override
            public void onSuccess(JSONArray data) {
                filterAndAdd(data, "maps", authorName);
            }

            @Override
            public void onError(String error) {
            }
        });

        GitHubService.fetchContent("textures", new GitHubService.DataCallback() {
            @Override
            public void onSuccess(JSONArray data) {
                filterAndAdd(data, "textures", authorName);
            }

            @Override
            public void onError(String error) {
            }
        });

        GitHubService.fetchContent("plugins", new GitHubService.DataCallback() {
            @Override
            public void onSuccess(JSONArray data) {
                filterAndAdd(data, "plugins", authorName);
            }

            @Override
            public void onError(String error) {
            }
        });

        GitHubService.fetchContent("mods", new GitHubService.DataCallback() {
            @Override
            public void onSuccess(JSONArray data) {
                filterAndAdd(data, "mods", authorName);
            }

            @Override
            public void onError(String error) {
            }
        });
    }

    private void filterAndAdd(JSONArray data, String type, final String authorName) {
        for (int i = 0; i < data.length(); i++) {
            JSONObject obj = data.optJSONObject(i);
            if (obj != null) {
                JSONObject author = obj.optJSONObject("author");
                if (author != null && authorName.equals(author.optString("name"))) {
                    authorItems.add(new ContentItem(obj, type));
                }
            }
        }

        java.util.Collections.sort(authorItems, new java.util.Comparator<ContentItem>() {
            @Override
            public int compare(ContentItem o1, ContentItem o2) {
                if (o1.date == null)
                    return 1;
                if (o2.date == null)
                    return -1;
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
}