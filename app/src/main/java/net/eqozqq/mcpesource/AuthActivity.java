package net.eqozqq.mcpesource;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class AuthActivity extends Activity {

    private static final String CLIENT_ID = "Ov23liVpRfUMTvxU0B2L";
    private static final String TAG = "AuthActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(android.R.style.Theme_Holo);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        Button btnLogin = findViewById(R.id.btn_login);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.showToast(AuthActivity.this, "Requesting code...");
                new RequestDeviceCodeTask().execute();
            }
        });
    }

    private class RequestDeviceCodeTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            try {
                URL url = new URL("https://github.com/login/device/code");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                String params = "client_id=" + CLIENT_ID + "&scope=public_repo";

                OutputStream os = conn.getOutputStream();
                os.write(params.getBytes());
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Response Code: " + responseCode);

                InputStream is;
                if (responseCode >= 400) {
                    is = conn.getErrorStream();
                } else {
                    is = conn.getInputStream();
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                Log.d(TAG, "Response: " + result.toString());
                return result.toString();

            } catch (Exception e) {
                Log.e(TAG, "Error: " + e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                try {
                    JSONObject json = new JSONObject(result);

                    if (json.has("error")) {
                        Utils.showToast(AuthActivity.this, "GitHub Error: " + json.optString("error_description"));
                        return;
                    }

                    String deviceCode = json.optString("device_code");
                    String userCode = json.optString("user_code");
                    String verificationUri = json.optString("verification_uri");
                    int interval = json.optInt("interval", 5);

                    if (!deviceCode.isEmpty() && !userCode.isEmpty()) {
                        showUserInstruction(userCode, verificationUri, deviceCode, interval);
                    } else {
                        Utils.showToast(AuthActivity.this, "Invalid response from GitHub");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Utils.showToast(AuthActivity.this, "JSON Error");
                }
            } else {
                Utils.showToast(AuthActivity.this, "Connection Error");
            }
        }
    }

    private void showUserInstruction(final String userCode, final String verificationUri, final String deviceCode, final int interval) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("GitHub Code", userCode);
        clipboard.setPrimaryClip(clip);

        Utils.showToast(this, "Code copied to clipboard!");

        new AlertDialog.Builder(this)
                .setTitle("GitHub Authorization")
                .setMessage("1. Code " + userCode + " copied.\n2. Click OK to open GitHub.\n3. Paste code and authorize.")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(verificationUri));
                        startActivity(browserIntent);
                        new PollAccessTokenTask(deviceCode, interval).execute();
                    }
                })
                .show();
    }

    private class PollAccessTokenTask extends AsyncTask<Void, Void, String> {
        private String deviceCode;
        private int interval;

        public PollAccessTokenTask(String deviceCode, int interval) {
            this.deviceCode = deviceCode;
            this.interval = interval;
        }

        @Override
        protected String doInBackground(Void... voids) {
            while (true) {
                try {
                    Thread.sleep((interval + 1) * 1000L);

                    URL url = new URL("https://github.com/login/oauth/access_token");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(10000);

                    String params = "client_id=" + CLIENT_ID +
                            "&device_code=" + deviceCode +
                            "&grant_type=urn:ietf:params:oauth:grant-type:device_code";

                    OutputStream os = conn.getOutputStream();
                    os.write(params.getBytes());
                    os.flush();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    InputStream is;
                    if (responseCode >= 400) {
                        is = conn.getErrorStream();
                    } else {
                        is = conn.getInputStream();
                    }

                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder resultBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        resultBuilder.append(line);
                    }

                    String response = resultBuilder.toString();
                    JSONObject json = new JSONObject(response);

                    if (json.has("access_token")) {
                        return json.getString("access_token");
                    }

                    String error = json.optString("error");
                    if (error.equals("authorization_pending")) {
                        continue;
                    } else if (error.equals("slow_down")) {
                        interval += 5;
                        continue;
                    } else if (error.equals("expired_token")) {
                        return null;
                    } else {
                        return null;
                    }

                } catch (Exception e) {
                    return null;
                }
            }
        }

        @Override
        protected void onPostExecute(String token) {
            if (token != null) {
                getSharedPreferences("app", MODE_PRIVATE).edit().putString("access_token", token).apply();
                Utils.showToast(AuthActivity.this, "Login successful!");
                startActivity(new Intent(AuthActivity.this, MainActivity.class));
                finish();
            } else {
                Utils.showToast(AuthActivity.this, "Login failed or timed out");
                finish();
            }
        }
    }
}