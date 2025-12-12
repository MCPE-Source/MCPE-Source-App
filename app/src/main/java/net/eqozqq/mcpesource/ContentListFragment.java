package net.eqozqq.mcpesource;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public abstract class ContentListFragment extends Fragment {

    private ListView listView;
    private ProgressBar progressBar;
    private EditText searchField;
    private ContentAdapter adapter;
    private List<ContentItem> items;
    private List<ContentItem> allItems;
    private SwipeRefreshLayout swipeRefreshLayout;

    protected abstract String getContentType();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);
        listView = view.findViewById(R.id.list_view);
        progressBar = view.findViewById(R.id.progress_bar);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        searchField = view.findViewById(R.id.local_search);

        items = new ArrayList<>();
        allItems = new ArrayList<>();
        adapter = new ContentAdapter(getActivity(), items);
        listView.setAdapter(adapter);

        searchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadData();
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ContentItem item = items.get(position);
                Intent intent = new Intent(getActivity(), DetailActivity.class);
                intent.putExtra("title", item.title);
                intent.putExtra("version", item.version);
                intent.putExtra("shortDescription", item.shortDescription);
                intent.putExtra("fullDescription", item.fullDescription);
                intent.putExtra("thumbnail", item.getFullThumbnailUrl());
                intent.putExtra("file", item.file);
                intent.putExtra("type", getContentType());

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

        loadData();
        return view;
    }

    private void filter(String query) {
        items.clear();
        if (query.isEmpty()) {
            items.addAll(allItems);
        } else {
            String lowerQuery = query.toLowerCase();
            for (ContentItem item : allItems) {
                if (item.title.toLowerCase().contains(lowerQuery) ||
                        (item.shortDescription != null && item.shortDescription.toLowerCase().contains(lowerQuery))) {
                    items.add(item);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void loadData() {
        if (!swipeRefreshLayout.isRefreshing())
            progressBar.setVisibility(View.VISIBLE);
        GitHubService.fetchContent(getContentType(), new GitHubService.DataCallback() {
            @Override
            public void onSuccess(JSONArray data) {
                if (getActivity() == null)
                    return;
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                allItems.clear();
                for (int i = 0; i < data.length(); i++) {
                    JSONObject obj = data.optJSONObject(i);
                    if (obj != null) {
                        allItems.add(new ContentItem(obj, getContentType()));
                    }
                }

                java.util.Collections.sort(allItems, new java.util.Comparator<ContentItem>() {
                    @Override
                    public int compare(ContentItem o1, ContentItem o2) {
                        if (o1.date == null)
                            return 1;
                        if (o2.date == null)
                            return -1;
                        return o2.date.compareTo(o1.date);
                    }
                });

                filter(searchField.getText().toString());
            }

            @Override
            public void onError(String error) {
                if (getActivity() == null)
                    return;
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                Utils.showToast(getActivity(), "Error: " + error);
            }
        });
    }
}