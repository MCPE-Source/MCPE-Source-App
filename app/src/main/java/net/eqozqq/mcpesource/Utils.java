package net.eqozqq.mcpesource;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;

public class Utils {

    public static void loadImage(final String url, final android.widget.ImageView imageView) {
        imageView.setTag(url);
        new AsyncTask<Void, Void, android.graphics.Bitmap>() {
            @Override
            protected android.graphics.Bitmap doInBackground(Void... voids) {
                try {
                    java.net.URL u = new java.net.URL(url);
                    return android.graphics.BitmapFactory.decodeStream(u.openConnection().getInputStream());
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(android.graphics.Bitmap bitmap) {
                Object tag = imageView.getTag();
                if (tag != null && tag.equals(url)) {
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                    }
                }
            }
        }.execute();
    }

    public static void downloadFile(final Context context, String url, final String fileName) {
        final DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(url);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setTitle(fileName);
        request.setDescription("Downloading " + fileName);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

        final long downloadId = downloadManager.enqueue(request);

        final ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setTitle("Downloading " + fileName);
        progressDialog.setMessage("Starting...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setIndeterminate(false);
        progressDialog.setMax(100);
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean downloading = true;
                while (downloading) {
                    DownloadManager.Query q = new DownloadManager.Query();
                    q.setFilterById(downloadId);
                    Cursor cursor = downloadManager.query(q);
                    if (cursor.moveToFirst()) {
                        int bytesDownloadedIndex = cursor
                                .getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                        int bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);

                        if (bytesDownloadedIndex != -1 && bytesTotalIndex != -1) {
                            final int bytesDownloaded = cursor.getInt(bytesDownloadedIndex);
                            final int bytesTotal = cursor.getInt(bytesTotalIndex);

                            if (bytesTotal > 0) {
                                final int progress = (int) ((bytesDownloaded * 100L) / bytesTotal);
                                ((android.app.Activity) context).runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        progressDialog.setMessage(
                                                bytesDownloaded / 1024 + "KB / " + bytesTotal / 1024 + "KB");
                                        progressDialog.setProgress(progress);
                                    }
                                });
                            }
                        }

                        int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                        if (statusIndex != -1) {
                            int status = cursor.getInt(statusIndex);
                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                downloading = false;
                                ((android.app.Activity) context).runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        progressDialog.dismiss();
                                        Utils.showToast(context, "Download Complete");
                                    }
                                });
                            } else if (status == DownloadManager.STATUS_FAILED) {
                                downloading = false;
                                ((android.app.Activity) context).runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        progressDialog.dismiss();
                                        Utils.showToast(context, "Download Failed");
                                    }
                                });
                            }
                        }
                    }
                    cursor.close();
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }).start();
    }

    public static void showToast(Context context, String message) {
        android.widget.Toast toast = new android.widget.Toast(context);
        toast.setDuration(android.widget.Toast.LENGTH_SHORT);
        android.view.View view = android.view.LayoutInflater.from(context).inflate(R.layout.toast, null);
        android.widget.TextView text = view.findViewById(R.id.toast_text);
        text.setText(message);
        toast.setView(view);
        toast.show();
    }
}