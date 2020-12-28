package in.ashprog.mapchat;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseUser;

import java.util.List;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

public class BottomAdapter extends RecyclerView.Adapter<BottomAdapter.BottomView> {

    List<ParseUser> parseUserList;
    Context context;

    public BottomAdapter(Context context, List<ParseUser> parseUserList) {
        this.parseUserList = parseUserList;
        this.context = context;
    }

    @NonNull
    @Override
    public BottomView onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.bottom_userfound_view, parent, false);
        return new BottomView(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final BottomView holder, final int position) {

        holder.usernameTextView.setText("@" + parseUserList.get(position).getUsername());
        holder.nameTextView.setText(parseUserList.get(position).get("name").toString());

        ParseFile file = parseUserList.get(position).getParseFile("image");
        byte[] data = null;
        try {
            data = file.getData();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        final Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        holder.profileImageView.setImageBitmap(bitmap);
        holder.profileImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
                View popupView = inflater.inflate(R.layout.popup_window_image, null);
                // create the popup window
                int width = LinearLayout.LayoutParams.WRAP_CONTENT;
                int height = LinearLayout.LayoutParams.WRAP_CONTENT;
                boolean focusable = true; // lets taps outside the popup also dismiss it
                final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);
                popupWindow.showAtLocation(v, Gravity.CENTER, 0, 0);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    popupWindow.setElevation(20f);
                }
                ImageView image = popupView.findViewById(R.id.image);
                image.setImageBitmap(bitmap);
            }
        });

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ParseGeoPoint parseGeoPoint = parseUserList.get(position).getParseGeoPoint("location");
                LatLng latLng = new LatLng(parseGeoPoint.getLatitude(), parseGeoPoint.getLongitude());
                SearchFragment.googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            }
        });

        holder.messageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent in = new Intent(context, MessageActivity.class);
                in.putExtra("username", parseUserList.get(position).getUsername());
                context.startActivity(in);
            }
        });
    }

    @Override
    public int getItemCount() {
        return parseUserList.size();
    }

    public class BottomView extends RecyclerView.ViewHolder {

        ImageView profileImageView;
        TextView nameTextView, usernameTextView;
        Button messageButton;

        public BottomView(@NonNull final View itemView) {
            super(itemView);

            profileImageView = itemView.findViewById(R.id.profileImageViewBottom);
            nameTextView = itemView.findViewById(R.id.nameTextViewBottom);
            usernameTextView = itemView.findViewById(R.id.usernameTextViewBottom);
            messageButton = itemView.findViewById(R.id.messageButton);
        }
    }
}
