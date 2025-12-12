package net.eqozqq.mcpesource;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;

public class ContentAdapter extends BaseAdapter {

    private Context context;
    private List<ContentItem> items;

    public ContentAdapter(Context context, List<ContentItem> items) {
        this.context = context;
        this.items = items;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_content, parent, false);
        }

        ContentItem item = (ContentItem) getItem(position);

        ImageView imageView = convertView.findViewById(R.id.item_image);
        TextView titleView = convertView.findViewById(R.id.item_title);
        TextView versionView = convertView.findViewById(R.id.item_version);
        TextView desc = convertView.findViewById(R.id.item_desc);

        titleView.setText(item.title);
        versionView.setText(item.version);
        desc.setText(item.shortDescription);

        if (item.type.equals("plugins") || item.type.equals("mods")) {
            imageView.setVisibility(View.GONE);
            imageView.setTag(null);
        } else {
            imageView.setVisibility(View.VISIBLE);
            if (item.thumbnail != null && !item.thumbnail.isEmpty()) {
                imageView.setImageResource(android.R.drawable.ic_menu_gallery);
                Utils.loadImage(item.getFullThumbnailUrl(), imageView);
            } else {
                imageView.setTag(null);
                imageView.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        }

        return convertView;
    }
}