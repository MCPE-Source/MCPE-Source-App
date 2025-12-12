package net.eqozqq.mcpesource;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class DetailActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(android.R.style.Theme_Holo);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        ImageView imageView = findViewById(R.id.detail_image);
        TextView titleView = findViewById(R.id.detail_title);
        TextView versionView = findViewById(R.id.detail_version);
        TextView descView = findViewById(R.id.detail_desc);
        TextView coreView = findViewById(R.id.detail_core);
        TextView phpView = findViewById(R.id.detail_php);

        TextView authorNameView = findViewById(R.id.detail_author_name);
        TextView dateView = findViewById(R.id.detail_date);
        TextView updatedDateView = findViewById(R.id.detail_updated_date);
        ImageView authorAvatarView = findViewById(R.id.detail_author_avatar);

        TextView licenseView = findViewById(R.id.detail_license);
        Button btnAltLink = findViewById(R.id.btn_link_alt);
        Button btnOriginalLink = findViewById(R.id.btn_link_original);
        Button btnEdit = findViewById(R.id.btn_edit);

        LinearLayout screenshotsContainer = findViewById(R.id.detail_screenshots_container);
        View screenshotsScroll = findViewById(R.id.detail_screenshots_scroll);

        Button btnDownload = findViewById(R.id.btn_download);

        Intent intent = getIntent();
        final String title = intent.getStringExtra("title");
        final String version = intent.getStringExtra("version");
        final String fullDescription = intent.getStringExtra("fullDescription");
        final String shortDescription = intent.getStringExtra("shortDescription");
        final String thumbnail = intent.getStringExtra("thumbnail");
        final String file = intent.getStringExtra("file");
        final String type = intent.getStringExtra("type");

        final String core = intent.getStringExtra("core");
        final String php = intent.getStringExtra("php");
        final String date = intent.getStringExtra("date");
        final String updatedDate = intent.getStringExtra("updatedDate");
        final String authorName = intent.getStringExtra("authorName");
        final String authorAvatar = intent.getStringExtra("authorAvatar");
        final String altLink = intent.getStringExtra("altLink");
        final String originalLink = intent.getStringExtra("originalLink");
        final String license = intent.getStringExtra("license");
        final String modType = intent.getStringExtra("modType");

        titleView.setText(title);
        versionView.setText(version);

        if (fullDescription != null && !fullDescription.isEmpty()) {
            String html = fullDescription
                    .replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>")
                    .replaceAll("\\*(.*?)\\*", "<i>$1</i>")
                    .replaceAll("__(.*?)__", "<b>$1</b>")
                    .replaceAll("_(.*?)_", "<i>$1</i>")
                    .replaceAll("`(.*?)`", "<font face='monospace'>$1</font>")
                    .replaceAll("\n", "<br>");
            descView.setText(android.text.Html.fromHtml(html));
        } else {
            if (shortDescription != null) {
                descView.setText(shortDescription);
            } else {
                descView.setText("");
            }
        }

        View.OnClickListener fullScreenListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = (String) v.getTag();
                if (url != null) {
                    Intent intent = new Intent(DetailActivity.this, FullScreenImageActivity.class);
                    intent.putExtra("url", url);
                    startActivity(intent);
                }
            }
        };

        if (thumbnail != null && !thumbnail.isEmpty()) {
            Utils.loadImage(thumbnail, imageView);
            imageView.setTag(thumbnail);
            imageView.setOnClickListener(fullScreenListener);
        } else {
            imageView.setVisibility(View.GONE);
        }

        if (core != null && !core.isEmpty()) {
            coreView.setText("Core: " + core);
            coreView.setVisibility(View.VISIBLE);
        }

        if (php != null && !php.isEmpty()) {
            phpView.setText("PHP: " + php);
            phpView.setVisibility(View.VISIBLE);
        }

        if (modType != null && !modType.isEmpty()) {
            TextView modTypeView = findViewById(R.id.detail_mod_type);
            modTypeView.setText("Mod Type: " + modType);
            modTypeView.setVisibility(View.VISIBLE);
        }

        if (authorName != null && !authorName.isEmpty()) {
            authorNameView.setText(authorName);
            if (date != null) {
                dateView.setText("Uploaded: " + date);
            }
            if (updatedDate != null && !updatedDate.isEmpty()) {
                updatedDateView.setText("Updated: " + updatedDate);
                updatedDateView.setVisibility(View.VISIBLE);
            }

            if (authorAvatar != null)
                Utils.loadImage(authorAvatar, authorAvatarView);

            View.OnClickListener authorClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(DetailActivity.this, AuthorProfileActivity.class);
                    intent.putExtra("name", authorName);
                    intent.putExtra("avatar", authorAvatar);
                    startActivity(intent);
                }
            };
            authorNameView.setOnClickListener(authorClickListener);
            authorAvatarView.setOnClickListener(authorClickListener);
        } else {
            ((View) authorNameView.getParent().getParent()).setVisibility(View.GONE);
        }

        if (license != null && !license.isEmpty()) {
            licenseView.setText("License: " + license);
            licenseView.setVisibility(View.VISIBLE);
        }

        if (altLink != null && !altLink.isEmpty()) {
            btnAltLink.setVisibility(View.VISIBLE);
            btnAltLink.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showLinkWarning(altLink);
                }
            });
        }

        if (originalLink != null && !originalLink.isEmpty()) {
            btnOriginalLink.setVisibility(View.VISIBLE);
            btnOriginalLink.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showLinkWarning(originalLink);
                }
            });
        }

        if (intent.hasExtra("screenshots_count")) {
            int count = intent.getIntExtra("screenshots_count", 0);
            if (count > 0) {
                screenshotsScroll.setVisibility(View.VISIBLE);
                for (int i = 0; i < count; i++) {
                    String url = intent.getStringExtra("screenshot_" + i);
                    ImageView img = new ImageView(this);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(300, 300);
                    lp.setMargins(0, 0, 16, 0);
                    img.setLayoutParams(lp);
                    img.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    img.setBackgroundColor(Color.LTGRAY);
                    Utils.loadImage(url, img);

                    img.setTag(url);
                    img.setOnClickListener(fullScreenListener);

                    screenshotsContainer.addView(img);
                }
            }
        }

        btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (file != null && !file.isEmpty()) {
                    String repo = "textures";
                    if ("maps".equals(type))
                        repo = "maps";
                    else if ("plugins".equals(type))
                        repo = "plugins";
                    else if ("mods".equals(type))
                        repo = "mods";

                    String url = "https://raw.githubusercontent.com/MCPE-Source/" + repo + "/main/" + file;
                    String fileName = file;
                    int lastSlash = file.lastIndexOf('/');
                    if (lastSlash != -1)
                        fileName = file.substring(lastSlash + 1);

                    Utils.downloadFile(DetailActivity.this, url, fileName);
                    Utils.showToast(DetailActivity.this, "Download started...");
                } else {
                    Utils.showToast(DetailActivity.this, "Download not available");
                }
            }
        });

        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent editIntent = new Intent(DetailActivity.this, UploadActivity.class);
                editIntent.putExtra("is_editing", true);

                editIntent.putExtra("title", title);
                editIntent.putExtra("version", version);
                editIntent.putExtra("shortDescription", shortDescription);
                editIntent.putExtra("fullDescription", fullDescription);
                editIntent.putExtra("type", type);
                editIntent.putExtra("core", core);
                editIntent.putExtra("php", php);
                editIntent.putExtra("modType", modType);
                editIntent.putExtra("date", date);
                editIntent.putExtra("altLink", altLink);
                editIntent.putExtra("originalLink", originalLink);
                editIntent.putExtra("license", license);
                editIntent.putExtra("file_path", file);

                startActivity(editIntent);
            }
        });

        checkAuthor(authorName, btnEdit);
    }

    private void checkAuthor(final String contentAuthor, final Button btnEdit) {
        if (contentAuthor == null) return;

        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    String token = getSharedPreferences("app", MODE_PRIVATE).getString("access_token", "");
                    if (token.isEmpty())
                        return null;

                    URL url = new URL("https://api.github.com/user");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Authorization", "token " + token);
                    conn.setRequestProperty("User-Agent", "MCPESource-App");

                    if (conn.getResponseCode() == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder result = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null)
                            result.append(line);

                        JSONObject json = new JSONObject(result.toString());
                        return json.optString("login");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(String login) {
                if (login != null && login.equals(contentAuthor)) {
                    btnEdit.setVisibility(View.VISIBLE);
                }
            }
        }.execute();
    }

    private void showLinkWarning(final String url) {
        new AlertDialog.Builder(this)
                .setTitle("External Link")
                .setMessage("You are about to open an external link in your browser:\n\n" + url)
                .setPositiveButton("Open", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}