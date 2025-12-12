package net.eqozqq.mcpesource;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TabHost;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {

    public static final String TAB_TAG_MAPS = "Maps";
    public static final String TAB_TAG_TEXTURES = "Textures";
    public static final String TAB_TAG_PLUGINS = "Plugins";
    public static final String TAB_TAG_MODS = "Mods";

    private TabHost tabHost;
    private FragmentManager fragmentManager;
    private Drawable userAvatarDrawable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(android.R.style.Theme_Holo);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fragmentManager = getFragmentManager();

        tabHost = findViewById(android.R.id.tabhost);
        tabHost.setup();

        TabHost.TabSpec tabSpecMaps = tabHost.newTabSpec(TAB_TAG_MAPS);
        tabSpecMaps.setIndicator("Maps");
        tabSpecMaps.setContent(R.id.tab_maps);
        tabHost.addTab(tabSpecMaps);

        TabHost.TabSpec tabSpecTextures = tabHost.newTabSpec(TAB_TAG_TEXTURES);
        tabSpecTextures.setIndicator("Textures");
        tabSpecTextures.setContent(R.id.tab_textures);
        tabHost.addTab(tabSpecTextures);

        TabHost.TabSpec tabSpecPlugins = tabHost.newTabSpec(TAB_TAG_PLUGINS);
        tabSpecPlugins.setIndicator("Plugins");
        tabSpecPlugins.setContent(R.id.tab_plugins);
        tabHost.addTab(tabSpecPlugins);

        TabHost.TabSpec tabSpecMods = tabHost.newTabSpec(TAB_TAG_MODS);
        tabSpecMods.setIndicator("Mods");
        tabSpecMods.setContent(R.id.tab_mods);
        tabHost.addTab(tabSpecMods);

        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                Fragment fragment = null;

                if (tabId.equals(TAB_TAG_MAPS)) {
                    fragment = new MapsFragment();
                } else if (tabId.equals(TAB_TAG_TEXTURES)) {
                    fragment = new TexturesFragment();
                } else if (tabId.equals(TAB_TAG_PLUGINS)) {
                    fragment = new PluginsFragment();
                } else if (tabId.equals(TAB_TAG_MODS)) {
                    fragment = new ModsFragment();
                }

                if (fragment != null) {
                    FragmentTransaction ft = fragmentManager.beginTransaction();
                    ft.replace(android.R.id.tabcontent, fragment);
                    ft.commit();
                }
            }
        });

        tabHost.setCurrentTab(0);

        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.replace(android.R.id.tabcontent, new MapsFragment());
        ft.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserAvatar();
    }

    private void loadUserAvatar() {
        final String token = getSharedPreferences("app", MODE_PRIVATE).getString("access_token", "");
        if (token.isEmpty()) {
            userAvatarDrawable = null;
            invalidateOptionsMenu();
            return;
        }

        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... voids) {
                try {
                    URL url = new URL("https://api.github.com/user");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Authorization", "token " + token);
                    conn.setRequestProperty("User-Agent", "MCPESource-App");

                    if (conn.getResponseCode() != 200) return null;

                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }

                    JSONObject json = new JSONObject(result.toString());
                    String avatarUrl = json.optString("avatar_url");

                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        URL imgUrl = new URL(avatarUrl);
                        return BitmapFactory.decodeStream(imgUrl.openConnection().getInputStream());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (bitmap != null) {
                    Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 96, 96, true);
                    userAvatarDrawable = new BitmapDrawable(getResources(), scaled);
                    invalidateOptionsMenu();
                }
            }
        }.execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        if (userAvatarDrawable != null) {
            MenuItem item = menu.findItem(R.id.action_profile);
            if (item != null) {
                item.setIcon(userAvatarDrawable);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_profile) {
            boolean loggedIn = getSharedPreferences("app", MODE_PRIVATE).contains("access_token");
            if (loggedIn) {
                startActivity(new Intent(this, ProfileActivity.class));
            } else {
                startActivity(new Intent(this, AuthActivity.class));
            }
            return true;
        } else if (id == R.id.action_upload) {
            boolean loggedIn = getSharedPreferences("app", MODE_PRIVATE).contains("access_token");
            if (loggedIn) {
                startActivity(new Intent(this, UploadActivity.class));
            } else {
                startActivity(new Intent(this, AuthActivity.class));
            }
            return true;
        } else if (id == R.id.action_search_global) {
            showGlobalSearchDialog();
            return true;
        } else if (id == R.id.action_about) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showGlobalSearchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Search Globally");

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_global_search, null);
        builder.setView(dialogView);

        final EditText queryInput = dialogView.findViewById(R.id.search_query);
        final RadioGroup criteriaGroup = dialogView.findViewById(R.id.search_criteria_group);
        final Spinner categorySpinner = dialogView.findViewById(R.id.search_category_spinner);

        String[] categories = {"All", "Maps", "Textures", "Plugins", "Mods"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);

        builder.setPositiveButton("Search", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String query = queryInput.getText().toString();
                int selectedId = criteriaGroup.getCheckedRadioButtonId();
                String criteria = "title";
                if (selectedId == R.id.criteria_description) criteria = "desc";
                else if (selectedId == R.id.criteria_version) criteria = "ver";

                String selectedCategory = categorySpinner.getSelectedItem().toString().toLowerCase();

                Intent intent = new Intent(MainActivity.this, SearchResultsActivity.class);
                intent.putExtra("query", query);
                intent.putExtra("criteria", criteria);
                intent.putExtra("category", selectedCategory);
                startActivity(intent);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}