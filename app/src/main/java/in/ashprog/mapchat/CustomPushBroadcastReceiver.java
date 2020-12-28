package in.ashprog.mapchat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.Person;
import androidx.core.app.RemoteInput;
import androidx.core.graphics.drawable.IconCompat;

import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParsePushBroadcastReceiver;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.json.JSONObject;

import java.util.ArrayList;

public class CustomPushBroadcastReceiver extends ParsePushBroadcastReceiver {

    public static ArrayList<String> users;
    public static ArrayList<ArrayList<NotificationCompat.MessagingStyle.Message>> historicMessages;

    static {
        users = new ArrayList<>();
        historicMessages = new ArrayList<>();
    }

    JSONObject data;
    String username;

    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("Messages", "MapChat Messages", importance);
            channel.setDescription("Messages for user");
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        onPushReceive(context, intent);
    }

    @Override
    protected void onPushReceive(Context context, Intent intent) {
        Person user;
        final CharSequence message;
        final String title, username;
        Bitmap bitmap = getLargeIcon(context, intent);
        try {
            if ("REPLY_ACTION".equals(intent.getAction())) {
                message = getMessageText(intent);
                username = intent.getStringExtra("username");
                title = intent.getStringExtra("title");
                ParseObject object = new ParseObject("Message");
                object.put("sender", ParseUser.getCurrentUser().getUsername());
                object.put("receiver", username);
                object.put("message", message);
                object.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if (e == null) {
                            ParseQuery<ParseUser> query = ParseUser.getQuery();
                            query.whereEqualTo("username", username);
                            ParseUser activeUser = null;
                            try {
                                activeUser = query.getFirst();
                                if (!activeUser.getBoolean("online")) {
                                    String jsonString = "{\"alert\": \"" + message + "\",\"badge\": \"Increment\",\"title\": \"" + UserActivity.currentUser.get("name").toString() + "\",\"username\": \"" + UserActivity.currentUser.getUsername() + "\"}";
                                    JSONObject data = new JSONObject(jsonString);
                                    ParseQuery<ParseInstallation> pushQuery = ParseInstallation.getQuery();
                                    pushQuery.whereEqualTo("username", username);
                                    ParsePush push = new ParsePush();
                                    push.setQuery(pushQuery);
                                    push.setData(data);
                                    push.send();
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                });
                ParseFile file = ParseUser.getCurrentUser().getParseFile("image");
                byte[] data = file.getData();
                Bitmap youBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                user = new Person.Builder()
                        .setName("You")
                        .setIcon(IconCompat.createWithBitmap(youBitmap))
                        .build();
            } else {
                data = getPushData(intent);
                username = data.getString("username");
                message = data.getString("alert");
                title = data.getString("title");

                if (!users.contains(username))
                    users.add(username);

                user = new Person.Builder()
                        .setName(title)
                        .setIcon(IconCompat.createWithBitmap(bitmap))
                        .build();
            }

            Intent in = new Intent(context, MessageActivity.class);
            in.putExtra("username", username);
            in.putExtra("fromPush", true);
            in.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, users.indexOf(username), in, 0);

            RemoteInput remoteInput = new RemoteInput.Builder("key_text_reply")
                    .setLabel("Enter message...")
                    .build();
            Intent in2 = new Intent(context, CustomPushBroadcastReceiver.class);
            in2.setAction("REPLY_ACTION");
            in2.putExtra("title", title);
            in2.putExtra("username", username);
            PendingIntent replyPendingIntent = PendingIntent.getBroadcast(context, users.indexOf(username), in2, PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationCompat.Action action =
                    new NotificationCompat.Action.Builder(R.drawable.ic_reply_icon,
                            "REPLY", replyPendingIntent)
                            .addRemoteInput(remoteInput)
                            .build();

            int count = countMessages();

            NotificationCompat.MessagingStyle style = new NotificationCompat.MessagingStyle(user);
            ArrayList<NotificationCompat.MessagingStyle.Message> messages = null;
            try {
                messages = historicMessages.get(users.indexOf(username));
                for (int i = 0; i < messages.size(); i++) {
                    style.addMessage(messages.get(i));
                }
                historicMessages.remove(users.indexOf(username));
            } catch (Exception e) {
                messages = new ArrayList<>();
            }
            style.addMessage(message, System.currentTimeMillis(), user);
            messages.add(new NotificationCompat.MessagingStyle.Message(message, System.currentTimeMillis(), user));
            historicMessages.add(users.indexOf(username), messages);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "Messages")
                    .setStyle(style)
                    .setColor(Color.parseColor("#009688"))
                    .setSmallIcon(R.drawable.logo)
                    .setContentTitle(title)
                    .setContentText(count + " new messages.")
                    .setLargeIcon(bitmap)
                    .setCategory(Notification.CATEGORY_MESSAGE)
                    .setContentIntent(pendingIntent)
                    .addAction(action)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
            notificationManagerCompat.notify(users.indexOf(username), builder.build());
        } catch (Exception e) {
            super.onPushReceive(context, intent);
            e.printStackTrace();
        }
    }

    @Override
    protected Bitmap getLargeIcon(Context context, Intent intent) {
        try {
            String username;
            if ("REPLY_ACTION".equals(intent.getAction())) {
                username = intent.getStringExtra("username");
            } else {
                username = getPushData(intent).getString("username");
            }
            ParseQuery<ParseUser> query = ParseUser.getQuery();
            query.whereEqualTo("username", username);
            ParseUser user = query.getFirst();
            ParseFile file = user.getParseFile("image");
            byte[] fileData = file.getData();
            Bitmap bitmap = BitmapFactory.decodeByteArray(fileData, 0, fileData.length);
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return super.getLargeIcon(context, intent);
        }
    }

    int countMessages() throws ParseException {
        ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Message");
        query.whereEqualTo("sender", username);
        query.whereEqualTo("receiver", ParseUser.getCurrentUser().getUsername());
        query.whereEqualTo("read", false);
        query.orderByAscending("createdAt");
        return query.count();
    }

    private CharSequence getMessageText(Intent intent) {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if (remoteInput != null) {
            return remoteInput.getCharSequence("key_text_reply");
        }
        return null;
    }
}
