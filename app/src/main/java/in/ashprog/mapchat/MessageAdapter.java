package in.ashprog.mapchat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.parse.ParseObject;

import java.util.ArrayList;
import java.util.Date;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageView> {

    ArrayList<ParseObject> messageList;
    Context c;

    public MessageAdapter(ArrayList<ParseObject> messageList, Context c) {
        this.messageList = messageList;
        this.c = c;
    }

    @NonNull
    @Override
    public MessageView onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v;
        if (viewType == 1)
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.receive_layout, parent, false);
        else
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.send_layout, parent, false);

        return new MessageView(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final MessageView holder, int position) {
        ParseObject object = messageList.get(position);

        holder.messageTextView.setText(object.get("message").toString());
        Date date = object.getCreatedAt();
        holder.messageTime.setText(date.toString());


        if (object.get("receiver").toString().equals(UserActivity.currentUser.getUsername())) {
            if (!object.getBoolean("read")) {
                object.put("read", true);
                object.saveInBackground();
            }
        } else {
            if (object.getBoolean("read"))
                holder.messageTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_visibility_white_15dp, 0);
            else
                holder.messageTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_visibility_off_white_15dp, 0);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (holder.messageTime.getVisibility() == View.GONE)
                    holder.messageTime.setVisibility(View.VISIBLE);
                else
                    holder.messageTime.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    @Override
    public int getItemViewType(int position) {
        String receiver = messageList.get(position).get("receiver").toString();
        if (receiver.equals(UserActivity.currentUser.getUsername()))
            return 1; // 1 means currentUser received message
        else
            return 0; // 0 means currentUser sent message
    }

    public class MessageView extends RecyclerView.ViewHolder {
        TextView messageTextView, messageTime;

        public MessageView(@NonNull View itemView) {
            super(itemView);

            messageTextView = itemView.findViewById(R.id.messageTextView);
            messageTime = itemView.findViewById(R.id.messageTime);
        }
    }
}
