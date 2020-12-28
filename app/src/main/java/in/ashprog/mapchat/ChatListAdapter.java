package in.ashprog.mapchat;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.parse.GetCallback;
import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatListView> {

    ArrayList<String> friends;
    Context c;

    public ChatListAdapter(Context c, ArrayList<String> friends) {
        this.c = c;
        this.friends = friends;
    }

    @NonNull
    @Override
    public ChatListView onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_view, parent, false);
        return new ChatListView(view);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onBindViewHolder(@NonNull final ChatListView holder, final int position) {

        try {
            final ParseUser[] friend = new ParseUser[1];
            ParseQuery<ParseUser> query = ParseUser.getQuery();
            query.whereEqualTo("username", friends.get(position));
            query.getFirstInBackground(new GetCallback<ParseUser>() {
                @Override
                public void done(ParseUser object, ParseException e) {
                    if (e == null && object != null) {
                        friend[0] = object;
                        holder.chatListNameTextView.setText(friend[0].get("name").toString());


                        ParseFile file = friend[0].getParseFile("image");
                        file.getDataInBackground(new GetDataCallback() {
                            @Override
                            public void done(byte[] data, ParseException e) {
                                if (e == null && data != null) {
                                    final Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                                    if (friend[0].getBoolean("online"))
                                        holder.chatListImageView.setImageBitmap(RescaleImage.addCircularBorder(bitmap, Color.parseColor("#009688")));
                                    else
                                        holder.chatListImageView.setImageBitmap(bitmap);
                                    holder.chatListImageView.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {

                                            LayoutInflater inflater = (LayoutInflater) c.getSystemService(LAYOUT_INFLATER_SERVICE);
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
                                }
                            }
                        });

                        ParseQuery<ParseObject> query1 = new ParseQuery<ParseObject>("Message");
                        query1.whereEqualTo("sender", UserActivity.currentUser.getUsername());
                        query1.whereEqualTo("receiver", friends.get(position));
                        ParseQuery<ParseObject> query2 = new ParseQuery<ParseObject>("Message");
                        query2.whereEqualTo("sender", friends.get(position));
                        query2.whereEqualTo("receiver", UserActivity.currentUser.getUsername());
                        ArrayList<ParseQuery<ParseObject>> listQuery = new ArrayList<>();
                        listQuery.add(query1);
                        listQuery.add(query2);
                        ParseQuery<ParseObject> queries = ParseQuery.or(listQuery);
                        queries.addDescendingOrder("createdAt");
                        queries.setLimit(1);
                        queries.getFirstInBackground(new GetCallback<ParseObject>() {
                            @Override
                            public void done(ParseObject object, ParseException e) {
                                if (object != null) {
                                    ParseObject lastMessage = object;
                                    holder.chatListMessageTextView.setText(lastMessage.get("message").toString());
                                    if (lastMessage.get("receiver").toString().equals(UserActivity.currentUser.getUsername())) {

                                        holder.chatListMessageTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);

                                        if (!lastMessage.getBoolean("read")) {
                                            holder.chatListMessageTextView.setTextColor(Color.parseColor("#009688"));
                                            holder.chatListMessageTextView.setTypeface(null, Typeface.BOLD);
                                            holder.itemView.setElevation(10f);
                                        } else {
                                            holder.chatListMessageTextView.setTextColor(Color.parseColor("#494848"));
                                            holder.chatListMessageTextView.setTypeface(null, Typeface.NORMAL);
                                            holder.itemView.setElevation(0f);
                                        }
                                    } else {
                                        holder.chatListMessageTextView.setTextColor(Color.parseColor("#494848"));
                                        holder.chatListMessageTextView.setTypeface(null, Typeface.NORMAL);
                                        holder.itemView.setElevation(0f);

                                        if (lastMessage.getBoolean("read"))
                                            holder.chatListMessageTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_visibility_green_18dp, 0, 0, 0);
                                        else
                                            holder.chatListMessageTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_visibility_off_green_18dp, 0, 0, 0);

                                    }
                                }
                            }
                        });


                        holder.itemView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent in = new Intent(c, MessageActivity.class);
                                in.putExtra("username", friends.get(position));
                                c.startActivity(in);
                            }
                        });
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return friends.size();
    }

    public class ChatListView extends RecyclerView.ViewHolder {
        ImageView chatListImageView;
        TextView chatListNameTextView, chatListMessageTextView;

        public ChatListView(@NonNull View itemView) {
            super(itemView);

            chatListImageView = itemView.findViewById(R.id.chatListProfileImageView);
            chatListNameTextView = itemView.findViewById(R.id.chatListNameTextView);
            chatListMessageTextView = itemView.findViewById(R.id.chatListMessageTextView);
        }
    }
}
