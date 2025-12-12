package net.eqozqq.mcpesource;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ImageView;

public class FullScreenImageActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(android.R.style.Theme_NoTitleBar_Fullscreen);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_image);

        String url = getIntent().getStringExtra("url");
        ImageView imageView = findViewById(R.id.fullscreen_image);
        if (url != null) {
            Utils.loadImage(url, imageView);
        }
    }
}
