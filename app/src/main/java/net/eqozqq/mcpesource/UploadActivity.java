package net.eqozqq.mcpesource;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UploadActivity extends Activity {

    private static final int PICK_FILE_REQUEST = 1;
    private static final int PICK_IMAGE_REQUEST = 2;
    private static final long MAX_FILE_SIZE = 25 * 1024 * 1024;

    private static final String[] VERSIONS = {
            "Unknown version", "0.16.2", "0.16.1", "0.16.0", "0.15.10", "0.15.9", "0.15.8", "0.15.7", "0.15.6", "0.15.4", "0.15.3",
            "0.15.2", "0.15.1", "0.15.0",
            "0.14.3", "0.14.2", "0.14.1", "0.14.0", "0.13.2", "0.13.1", "0.13.0", "0.12.3", "0.12.2", "0.12.1",
            "0.12.0",
            "0.11.0", "0.10.5", "0.10.4", "0.10.3", "0.10.2", "0.10.1", "0.10.0", "0.9.5", "0.9.4", "0.9.3", "0.9.2",
            "0.9.1", "0.9.0",
            "0.8.1", "0.8.0", "0.7.6", "0.7.5", "0.7.4", "0.7.3", "0.7.2", "0.7.1", "0.7.0",
            "0.6.1", "0.6.0", "0.5.0", "0.4.0", "0.3.3", "0.3.2", "0.3.0", "0.2.2", "0.2.1", "0.2.0", "0.1.3", "0.1.2",
            "0.1.0"
    };

    private static final String[] MOD_TYPES = {
            "PTPatch", "BL Native Addon", "Script", "Other"
    };

    private static final String[] CORES = {
            "PocketMine-MP", "Nukkit", "NostalgiaCore", "Minecraft 0.1.3 Server", "Festival",
            "PMMP 1.4.1", "PMMP 1.4", "PMMP 1.3.12", "PMMP 1.3.11", "PMMP 1.3.10", "PMMP 1.3.9",
            "PMMP 1.3.8", "PMMP 1.3.7", "PMMP 1.3.5", "PMMP 1.3.4", "PMMP 1.3.3", "Other"
    };

    private static final String[] PHP_VERSIONS = {
            "PHP 5", "PHP 7", "PHP 8"
    };

    private Uri fileUri;
    private List<Uri> imageUris = new ArrayList<>();
    private List<String> selectedVersions = new ArrayList<>();
    private boolean[] checkedVersions;

    private EditText editTitle, editShortDesc, editFullDesc, editAltLink, editOriginalLink, editLicense;
    private Spinner spinnerModType, spinnerCore, spinnerPhp;
    private Button btnSelectVersions, btnSubmit;
    private TextView textFilePath, textImageCount, textSelectedVersions, textHeader;
    private RadioGroup radioGroupType;
    private LinearLayout layoutScreenshots;

    private String authorLogin;
    private String authorAvatarUrl;

    private boolean isEditing = false;
    private String existingFilePath = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(android.R.style.Theme_Holo);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        checkedVersions = new boolean[VERSIONS.length];

        textHeader = ((LinearLayout) findViewById(R.id.radio_group_type).getParent()).findViewById(R.id.text_selected_versions).getRootView().findViewWithTag("header_text");

        editTitle = findViewById(R.id.edit_title);
        editShortDesc = findViewById(R.id.edit_short_desc);
        editFullDesc = findViewById(R.id.edit_full_desc);
        editAltLink = findViewById(R.id.edit_alt_link);
        editOriginalLink = findViewById(R.id.edit_original_link);
        editLicense = findViewById(R.id.edit_license);
        btnSubmit = findViewById(R.id.btn_submit);

        btnSelectVersions = findViewById(R.id.btn_select_versions);
        textSelectedVersions = findViewById(R.id.text_selected_versions);

        btnSelectVersions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showVersionDialog();
            }
        });

        spinnerCore = findViewById(R.id.spinner_core);
        ArrayAdapter<String> coreAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, CORES);
        coreAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCore.setAdapter(coreAdapter);

        spinnerPhp = findViewById(R.id.spinner_php);
        ArrayAdapter<String> phpAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, PHP_VERSIONS);
        phpAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPhp.setAdapter(phpAdapter);

        spinnerCore.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = CORES[position];
                if (selected.contains("PocketMine-MP") || selected.contains("NostalgiaCore") ||
                        selected.contains("Festival") || selected.contains("PMMP")) {
                    spinnerPhp.setVisibility(View.VISIBLE);
                } else {
                    spinnerPhp.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerModType = findViewById(R.id.spinner_mod_type);
        ArrayAdapter<String> modAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, MOD_TYPES);
        modAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerModType.setAdapter(modAdapter);

        textFilePath = findViewById(R.id.text_file_path);
        textImageCount = findViewById(R.id.text_image_count);
        layoutScreenshots = findViewById(R.id.layout_screenshots);
        radioGroupType = findViewById(R.id.radio_group_type);

        radioGroupType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                layoutScreenshots.setVisibility(View.VISIBLE);

                if (checkedId == R.id.radio_plugins) {
                    spinnerCore.setVisibility(View.VISIBLE);
                    spinnerModType.setVisibility(View.GONE);
                    if (spinnerCore.getSelectedItem() != null) {
                        String selected = spinnerCore.getSelectedItem().toString();
                        if (selected.contains("PocketMine-MP") || selected.contains("NostalgiaCore") ||
                                selected.contains("Festival") || selected.contains("PMMP")) {
                            spinnerPhp.setVisibility(View.VISIBLE);
                        }
                    }
                } else if (checkedId == R.id.radio_mods) {
                    spinnerCore.setVisibility(View.GONE);
                    spinnerPhp.setVisibility(View.GONE);
                    spinnerModType.setVisibility(View.VISIBLE);
                } else {
                    spinnerCore.setVisibility(View.GONE);
                    spinnerPhp.setVisibility(View.GONE);
                    spinnerModType.setVisibility(View.GONE);
                }
            }
        });

        findViewById(R.id.btn_choose_file).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                startActivityForResult(intent, PICK_FILE_REQUEST);
            }
        });

        findViewById(R.id.btn_choose_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (imageUris.size() >= 5) {
                    Utils.showToast(UploadActivity.this, "Max 5 images reached");
                    return;
                }
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(intent, PICK_IMAGE_REQUEST);
            }
        });

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (fileUri == null && !isEditing) {
                    Utils.showToast(UploadActivity.this, "Please select a file");
                    return;
                }

                if (selectedVersions.isEmpty()) {
                    Utils.showToast(UploadActivity.this, "Please select at least one version");
                    return;
                }

                int typeId = radioGroupType.getCheckedRadioButtonId();
                if ((typeId == R.id.radio_maps || typeId == R.id.radio_textures) && imageUris.isEmpty() && !isEditing) {
                    Utils.showToast(UploadActivity.this, "Please select at least a thumbnail");
                    return;
                }

                performUpload();
            }
        });

        if (getIntent().getBooleanExtra("is_editing", false)) {
            isEditing = true;
            setupEditMode();
        }

        fetchUserInfo();
    }

    private void setupEditMode() {
        Intent i = getIntent();
        editTitle.setText(i.getStringExtra("title"));
        editTitle.setEnabled(false);

        editShortDesc.setText(i.getStringExtra("shortDescription"));
        editFullDesc.setText(i.getStringExtra("fullDescription"));
        editAltLink.setText(i.getStringExtra("altLink"));
        editOriginalLink.setText(i.getStringExtra("originalLink"));
        editLicense.setText(i.getStringExtra("license"));

        existingFilePath = i.getStringExtra("file_path");
        if (existingFilePath != null && !existingFilePath.isEmpty()) {
            textFilePath.setText("Current file: " + existingFilePath);
        }

        String type = i.getStringExtra("type");
        if ("maps".equals(type)) radioGroupType.check(R.id.radio_maps);
        else if ("plugins".equals(type)) radioGroupType.check(R.id.radio_plugins);
        else if ("mods".equals(type)) radioGroupType.check(R.id.radio_mods);
        else radioGroupType.check(R.id.radio_textures);

        for (int k = 0; k < radioGroupType.getChildCount(); k++) {
            radioGroupType.getChildAt(k).setEnabled(false);
        }

        String versionStr = i.getStringExtra("version");
        if (versionStr != null) {
            String[] vs = versionStr.split(", ");
            selectedVersions.addAll(Arrays.asList(vs));
            textSelectedVersions.setText(versionStr);
            for(String v : vs) {
                for(int k=0; k<VERSIONS.length; k++) {
                    if (VERSIONS[k].equals(v)) checkedVersions[k] = true;
                }
            }
        }

        String core = i.getStringExtra("core");
        if (core != null) {
            for(int k=0; k<CORES.length; k++) if(CORES[k].equals(core)) spinnerCore.setSelection(k);
        }
        String php = i.getStringExtra("php");
        if (php != null) {
            for(int k=0; k<PHP_VERSIONS.length; k++) if(PHP_VERSIONS[k].equals(php)) spinnerPhp.setSelection(k);
        }

        btnSubmit.setText("Update Content");
    }

    private void showVersionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Version(s)");
        builder.setMultiChoiceItems(VERSIONS, checkedVersions, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                checkedVersions[which] = isChecked;
            }
        });
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectedVersions.clear();
                for (int i = 0; i < VERSIONS.length; i++) {
                    if (checkedVersions[i]) {
                        selectedVersions.add(VERSIONS[i]);
                    }
                }
                textSelectedVersions.setText(TextUtils.join(", ", selectedVersions));
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void fetchUserInfo() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
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
                        authorLogin = json.optString("login");
                        authorAvatarUrl = json.optString("avatar_url");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == PICK_FILE_REQUEST) {
                Uri uri = data.getData();
                if (checkFileSize(uri)) {
                    fileUri = uri;
                    textFilePath.setText(fileUri.getPath());
                } else {
                    Utils.showToast(this, "File too large (Max 25MB)");
                }
            } else if (requestCode == PICK_IMAGE_REQUEST) {
                Uri uri = data.getData();
                imageUris.add(uri);
                textImageCount.setText(imageUris.size() + "/5 images selected");
            }
        }
    }

    private boolean checkFileSize(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
            cursor.moveToFirst();
            long size = cursor.getLong(sizeIndex);
            cursor.close();
            return size <= MAX_FILE_SIZE;
        }
        return true;
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0)
                        result = cursor.getString(index);
                }
            } finally {
                if (cursor != null)
                    cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1)
                result = result.substring(cut + 1);
        }
        return result;
    }

    private void performUpload() {
        final String title = editTitle.getText().toString();
        final String versionStr = TextUtils.join(", ", selectedVersions);
        final String shortDesc = editShortDesc.getText().toString();
        final String fullDesc = editFullDesc.getText().toString();

        String tempCore = "";
        String tempPhp = "";
        if (spinnerCore.getVisibility() == View.VISIBLE) {
            tempCore = spinnerCore.getSelectedItem().toString();
            if (spinnerPhp.getVisibility() == View.VISIBLE) {
                tempPhp = spinnerPhp.getSelectedItem().toString();
            }
        }
        final String core = tempCore;
        final String php = tempPhp;

        final String modType = spinnerModType.getSelectedItem().toString();
        final String altLink = editAltLink.getText().toString();
        final String originalLink = editOriginalLink.getText().toString();
        final String license = editLicense.getText().toString();

        int checkedId = radioGroupType.getCheckedRadioButtonId();
        String tempRepo = "textures";
        if (checkedId == R.id.radio_maps)
            tempRepo = "maps";
        else if (checkedId == R.id.radio_plugins)
            tempRepo = "plugins";
        else if (checkedId == R.id.radio_mods)
            tempRepo = "mods";
        final String repoName = tempRepo;

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(isEditing ? "Updating Info" : "Uploading Info");
        progressDialog.setMessage("Preparing...");
        progressDialog.setIndeterminate(false);
        progressDialog.setCancelable(false);
        progressDialog.show();

        new AsyncTask<Void, String, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    String token = getSharedPreferences("app", MODE_PRIVATE).getString("access_token", "");
                    if (token.isEmpty())
                        return "Not logged in";

                    publishProgress("Forking repository...");
                    String forkUrl = "https://api.github.com/repos/MCPE-Source/" + repoName + "/forks";
                    String forkResponse = sendRequest(forkUrl, "POST", token, null);
                    JSONObject forkJson = new JSONObject(forkResponse);
                    String userRepo = forkJson.getString("full_name");

                    Thread.sleep(3000);

                    List<String> uploadedScreenshots = new ArrayList<>();
                    String thumbPath = "";
                    String mainVersionFolder = selectedVersions.get(0).replace(" ", "_");
                    String finalFilePath = existingFilePath;

                    if (!imageUris.isEmpty()) {
                        for (int i = 0; i < imageUris.size(); i++) {
                            publishProgress("Uploading image " + (i + 1) + "...");
                            byte[] imgData = readBytes(imageUris.get(i));
                            String timestamp = String.valueOf(System.currentTimeMillis());
                            String name = (i == 0) ? "thumb_" + timestamp + ".jpg" : "screen_" + i + "_" + timestamp + ".jpg";
                            String path = "data/" + mainVersionFolder + "/" + title + "/" + name;

                            uploadFile(userRepo, path, imgData, token);
                            if (i == 0)
                                thumbPath = path;
                            else
                                uploadedScreenshots.add(path);
                        }
                    }

                    if (fileUri != null) {
                        publishProgress("Uploading content file...");
                        byte[] fileData = readBytes(fileUri);

                        String originalName = getFileName(fileUri);
                        String extension = "";
                        int i = originalName.lastIndexOf('.');
                        if (i > 0) {
                            extension = originalName.substring(i);
                        }
                        String finalFileName = title + extension;

                        String filePath = "data/" + mainVersionFolder + "/" + title + "/" + finalFileName;
                        uploadFile(userRepo, filePath, fileData, token);
                        finalFilePath = filePath;
                    }

                    publishProgress("Updating index...");
                    String indexUrl = "https://api.github.com/repos/" + userRepo + "/contents/" + repoName + ".json";
                    String indexResp = sendRequest(indexUrl, "GET", token, null);
                    JSONObject indexJson = new JSONObject(indexResp);
                    String sha = indexJson.getString("sha");
                    String contentBase64 = indexJson.getString("content").replaceAll("\n", "");
                    String contentStr = new String(Base64.decode(contentBase64, Base64.DEFAULT));

                    JSONArray currentArray = new JSONArray(contentStr);
                    JSONObject newItem = new JSONObject();

                    newItem.put("title", title);
                    newItem.put("version", versionStr);
                    newItem.put("short_description", shortDesc);
                    if (!fullDesc.isEmpty()) newItem.put("full_description", fullDesc);

                    if (!thumbPath.isEmpty()) {
                        newItem.put("thumbnail", thumbPath);
                    } else if (isEditing) {
                    }

                    if (finalFilePath != null && !finalFilePath.isEmpty()) {
                        newItem.put("file", finalFilePath);
                    }

                    String nowDate = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date());
                    if (isEditing) {
                        String origDate = getIntent().getStringExtra("date");
                        newItem.put("date", (origDate != null && !origDate.isEmpty()) ? origDate : nowDate);
                        newItem.put("updated_date", nowDate);
                    } else {
                        newItem.put("date", nowDate);
                    }

                    newItem.put("type", repoName);

                    if (repoName.equals("plugins")) {
                        if (!core.isEmpty()) newItem.put("core", core);
                        if (!php.isEmpty()) newItem.put("php_version", php);
                    }

                    if (repoName.equals("mods")) {
                        newItem.put("mod_type", modType);
                    }

                    if (!altLink.isEmpty()) newItem.put("alt_link", altLink);
                    if (!originalLink.isEmpty()) newItem.put("original_link", originalLink);
                    if (!license.isEmpty()) newItem.put("license", license);

                    if (authorLogin != null) {
                        JSONObject authorObj = new JSONObject();
                        authorObj.put("name", authorLogin);
                        authorObj.put("avatar", authorAvatarUrl);
                        newItem.put("author", authorObj);
                    }

                    if (!uploadedScreenshots.isEmpty()) {
                        JSONArray screens = new JSONArray();
                        for (String s : uploadedScreenshots) screens.put(s);
                        newItem.put("screenshots", screens);
                    }

                    if (isEditing) {
                        boolean found = false;
                        for (int k = 0; k < currentArray.length(); k++) {
                            JSONObject obj = currentArray.getJSONObject(k);
                            if (obj.optString("title").equals(title)) {
                                if (!newItem.has("thumbnail") && obj.has("thumbnail")) newItem.put("thumbnail", obj.getString("thumbnail"));
                                if (!newItem.has("screenshots") && obj.has("screenshots")) newItem.put("screenshots", obj.getJSONArray("screenshots"));

                                currentArray.put(k, newItem);
                                found = true;
                                break;
                            }
                        }
                        if (!found) currentArray.put(newItem);
                    } else {
                        currentArray.put(newItem);
                    }

                    String newContentEncoded = Base64.encodeToString(currentArray.toString(2).getBytes(),
                            Base64.NO_WRAP);
                    JSONObject updateJson = new JSONObject();
                    updateJson.put("message", (isEditing ? "Update " : "Add ") + title);
                    updateJson.put("content", newContentEncoded);
                    updateJson.put("sha", sha);

                    sendRequest(indexUrl, "PUT", token, updateJson.toString());

                    publishProgress("Creating Pull Request...");
                    String prUrl = "https://api.github.com/repos/MCPE-Source/" + repoName + "/pulls";
                    JSONObject prJson = new JSONObject();
                    prJson.put("title", (isEditing ? "Update " : "Add ") + title);
                    prJson.put("head", userRepo.split("/")[0] + ":main");
                    prJson.put("base", "main");
                    prJson.put("body", (isEditing ? "Updated " : "Added ") + title);

                    sendRequest(prUrl, "POST", token, prJson.toString());

                    return "Success";
                } catch (Exception e) {
                    e.printStackTrace();
                    return "Error: " + e.getMessage();
                }
            }

            @Override
            protected void onProgressUpdate(String... values) {
                progressDialog.setMessage(values[0]);
            }

            @Override
            protected void onPostExecute(String s) {
                progressDialog.dismiss();
                if (s.equals("Success")) {
                    new AlertDialog.Builder(UploadActivity.this)
                            .setTitle("Success")
                            .setMessage("Your changes have been submitted via Pull Request.")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            })
                            .setCancelable(false)
                            .show();
                } else {
                    Utils.showToast(UploadActivity.this, s);
                }
            }
        }.execute();
    }

    private void uploadFile(String repo, String path, byte[] data, String token) throws Exception {
        String url = "https://api.github.com/repos/" + repo + "/contents/" + path;
        String encoded = Base64.encodeToString(data, Base64.NO_WRAP);
        JSONObject json = new JSONObject();
        json.put("message", "Upload " + path);
        json.put("content", encoded);

        try {
            String check = sendRequest(url, "GET", token, null);
            JSONObject checkJson = new JSONObject(check);
            json.put("sha", checkJson.getString("sha"));
        } catch (Exception ignored) {
        }

        sendRequest(url, "PUT", token, json.toString());
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

    private byte[] readBytes(Uri uri) throws Exception {
        InputStream is = getContentResolver().openInputStream(uri);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }
}