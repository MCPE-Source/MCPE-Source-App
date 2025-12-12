package net.eqozqq.mcpesource;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;

public class ProfileContentAdapter extends BaseAdapter {

    private Context context;
    private List<ContentItem> items;
    private OnActionListener listener;

    public interface OnActionListener {
        void onEdit(ContentItem item);
        void onDelete(ContentItem item);
    }

    public ProfileContentAdapter(Context context, List<ContentItem> items, OnActionListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
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
            convertView = LayoutInflater.from(context).inflate(R.layout.item_profile_content, parent, false);
        }

        final ContentItem item = items.get(position);

        ImageView imageView = convertView.findViewById(R.id.item_profile_image);
        TextView titleView = convertView.findViewById(R.id.item_profile_title);
        TextView typeView = convertView.findViewById(R.id.item_profile_type);
        ImageView btnEdit = convertView.findViewById(R.id.btn_item_edit);
        ImageView btnDelete = convertView.findViewById(R.id.btn_item_delete);

        titleView.setText(item.title);
        typeView.setText(item.type.substring(0, 1).toUpperCase() + item.type.substring(1));

        if (item.thumbnail != null && !item.thumbnail.isEmpty()) {
            imageView.setImageResource(android.R.drawable.ic_menu_gallery);
            Utils.loadImage(item.getFullThumbnailUrl(), imageView);
        } else {
            imageView.setTag(null);
            imageView.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onEdit(item);
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onDelete(item);
            }
        });

        return convertView;
    }
}