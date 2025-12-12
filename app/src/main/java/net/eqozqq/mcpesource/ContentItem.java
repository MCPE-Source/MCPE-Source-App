package net.eqozqq.mcpesource;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ContentItem implements Serializable {
    private static final long serialVersionUID = 1L;

    public String title;
    public String version;
    public String shortDescription;
    public String fullDescription;
    public String thumbnail;
    public String file;
    public String type;
    public String authorName;
    public String authorAvatar;
    public String date;
    public String updatedDate;
    public String core;
    public String modType;
    public String altLink;
    public String originalLink;
    public String license;
    public List<String> screenshots;

    public ContentItem(JSONObject json, String type) {
        this.type = type;
        this.title = json.optString("title");
        this.version = json.optString("version");
        this.shortDescription = json.optString("short_description");
        this.fullDescription = json.optString("full_description");
        this.thumbnail = json.optString("thumbnail");
        this.file = json.optString("file");
        this.date = json.optString("date");
        this.updatedDate = json.optString("updated_date");
        this.core = json.optString("core");
        this.modType = json.optString("mod_type");
        this.altLink = json.optString("alt_link");
        this.originalLink = json.optString("original_link");
        this.license = json.optString("license");

        JSONObject author = json.optJSONObject("author");
        if (author != null) {
            this.authorName = author.optString("name");
            this.authorAvatar = author.optString("avatar");
        }

        this.screenshots = new ArrayList<>();
        JSONArray screens = json.optJSONArray("screenshots");
        if (screens != null) {
            for (int i = 0; i < screens.length(); i++) {
                this.screenshots.add(screens.optString(i));
            }
        }
    }

    public String getFullThumbnailUrl() {
        if (thumbnail == null || thumbnail.isEmpty())
            return null;
        return getBaseUrl() + thumbnail;
    }

    public String getDownloadUrl() {
        if (file == null || file.isEmpty())
            return null;
        return getBaseUrl() + file;
    }

    public String getExampleScreenshotUrl(int index) {
        if (index < 0 || index >= screenshots.size())
            return null;
        return getBaseUrl() + screenshots.get(index);
    }

    private String getBaseUrl() {
        String repo = type;
        return "https://raw.githubusercontent.com/MCPE-Source/" + repo + "/main/";
    }
}