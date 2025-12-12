package net.eqozqq.mcpesource;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class AboutActivity extends Activity {

    private TextView versionText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(android.R.style.Theme_Holo);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        versionText = findViewById(R.id.about_version);
        Button btnGithub = findViewById(R.id.btn_github);
        Button btnWebsite = findViewById(R.id.btn_website);

        String currentVersion = "";
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            currentVersion = pInfo.versionName;
            versionText.setText("v" + currentVersion);
        } catch (Exception e) {
            e.printStackTrace();
        }

        btnGithub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = "https://github.com/MCPE-Source/MCPE-Source-App";
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            }
        });

        btnWebsite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = "https://mcpe-source.github.io";
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            }
        });

        if (!currentVersion.isEmpty()) {
            new CheckUpdateTask(currentVersion).execute();
        }
    }

    private class CheckUpdateTask extends AsyncTask<Void, Void, String> {
        private String currentVersion;

        public CheckUpdateTask(String currentVersion) {
            this.currentVersion = currentVersion;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                URL url = new URL("https://raw.githubusercontent.com/MCPE-Source/components/main/lastversion.txt");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String line = reader.readLine();
                    if (line != null) {
                        return line.trim();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String latestVersion) {
            if (latestVersion != null) {
                if (latestVersion.equals(currentVersion)) {
                    versionText.append("\nLatest version installed");
                } else {
                    versionText.append("\nUpdate available: " + latestVersion);
                }
            }
        }
    }
}