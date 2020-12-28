package in.ashprog.mapchat;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.livequery.SubscriptionHandling;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static in.ashprog.mapchat.MainActivity.parseLiveQueryClient;

public class MessageActivity extends AppCompatActivity {

    RecyclerView messageRecyclerView;
    MessageAdapter messageAdapter;
    ArrayList<ParseObject> messageList;
    ParseObject friendObject;

    String activeUsername;
    TextView activeUserNameTextView;
    ImageView activeUserProfileImageViewTop;
    EditText messageEditText;

    ParseUser activeUser;
    Bitmap profileBitmap;

    ConstraintLayout messageLayout;
    CardView messageStatusCardView;
    TextView messageStatusTextView;
    Animation anim;

    ParseQuery<ParseInstallation> pushQuery;
    ParsePush push;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        NotificationManagerCompat.from(this).cancelAll();

        activeUsername = getIntent().getStringExtra("username");
        if (getIntent().getBooleanExtra("fromPush", false)) {
            UserActivity.currentUser = ParseUser.getCurrentUser();
            UserActivity.currentUser.put("online", true);
            UserActivity.currentUser.saveInBackground();
        }

        messageStatusTextView = findViewById(R.id.messageStatusTextView);
        messageStatusCardView = findViewById(R.id.messageStatusCardView);
        activeUserNameTextView = findViewById(R.id.activeUserNameTextView);
        activeUserProfileImageViewTop = findViewById(R.id.activeUserProfileImageViewTop);
        messageLayout = findViewById(R.id.messageLayout);
        messageLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard(messageLayout);
            }
        });
        messageEditText = findViewById(R.id.messageEditText);
        messageEditText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    send(messageEditText);
                    return true;
                }
                return false;
            }
        });

        messageRecyclerView = findViewById(R.id.messageRecyclerView);
        messageRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true));
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList, this);
        messageRecyclerView.setAdapter(messageAdapter);

        messageStatusCardViewEnter("Loading...");
        fetchActiveUser(activeUsername);
        subscribeToUser(activeUsername);
        fetchMessages(true);
        subscribeToMessages();

        queryFriend();

        pushQuery = ParseInstallation.getQuery();
        pushQuery.whereEqualTo("username", activeUsername);
        push = new ParsePush();
        push.setQuery(pushQuery);
    }

    @Override
    public void onBackPressed() {
        offlineForPush();
        super.onBackPressed();
    }

    void offlineForPush() {
        if (getIntent().getBooleanExtra("fromPush", false)) {
            UserActivity.currentUser = ParseUser.getCurrentUser();
            UserActivity.currentUser.put("online", false);
            UserActivity.currentUser.saveInBackground();
        }
    }

    void fetchActiveUser(String username) {
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo("username", username);
        query.getFirstInBackground(new GetCallback<ParseUser>() {
            @Override
            public void done(final ParseUser object, ParseException e) {
                if (object != null) {
                    activeUser = object;
                    activeUserNameTextView.setText("@" + object.getUsername());
                    ParseFile file = object.getParseFile("image");
                    file.getDataInBackground(new GetDataCallback() {
                        @Override
                        public void done(byte[] data, ParseException e) {
                            if (data != null) {
                                profileBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                                if (object.getBoolean("online"))
                                    activeUserProfileImageViewTop.setImageBitmap(Bitmap.createScaledBitmap(RescaleImage.addCircularBorder(profileBitmap, Color.parseColor("#009688")), 50, 50, true));
                                else
                                    activeUserProfileImageViewTop.setImageBitmap(Bitmap.createScaledBitmap(profileBitmap, 50, 50, true));
                            }
                        }
                    });
                }
            }
        });
    }

    void fetchMessages(final boolean isLoading) {
        ParseQuery<ParseObject> parseQuery1 = new ParseQuery<ParseObject>("Message");
        parseQuery1.whereEqualTo("sender", activeUsername);
        parseQuery1.whereEqualTo("receiver", UserActivity.currentUser.getUsername());
        ParseQuery<ParseObject> parseQuery2 = new ParseQuery<ParseObject>("Message");
        parseQuery2.whereEqualTo("sender", UserActivity.currentUser.getUsername());
        parseQuery2.whereEqualTo("receiver", activeUsername);
        ArrayList<ParseQuery<ParseObject>> queryArrayList = new ArrayList<>();
        queryArrayList.add(parseQuery1);
        queryArrayList.add(parseQuery2);
        ParseQuery<ParseObject> queries = ParseQuery.or(queryArrayList);
        queries.addDescendingOrder("createdAt");
        queries.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null && objects.size() > 0) {
                    messageList.clear();
                    for (ParseObject message : objects) {
                        messageList.add(message);
                        messageAdapter.notifyDataSetChanged();
                    }
                }
                if (isLoading)
                    messageStatusCardViewExit();
            }
        });
    }

    void subscribeToUser(final String username) {
        //Live Query
        if (parseLiveQueryClient != null) {
            ParseQuery<ParseUser> parseQuery = ParseUser.getQuery();
            parseQuery.whereEqualTo("username", username);
            SubscriptionHandling<ParseUser> subscriptionHandling = parseLiveQueryClient.subscribe(parseQuery);

            subscriptionHandling.handleEvent(SubscriptionHandling.Event.UPDATE, new SubscriptionHandling.HandleEventCallback<ParseUser>() {
                @Override
                public void onEvent(ParseQuery<ParseUser> query, final ParseUser user) {
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        public void run() {
                            fetchActiveUser(username);
                        }

                    });
                }
            });
        }
    }

    void subscribeToMessages() {
        //Live Query
        if (parseLiveQueryClient != null) {
            ParseQuery<ParseObject> parseQuery1 = new ParseQuery<ParseObject>("Message");
            parseQuery1.whereEqualTo("sender", activeUsername);
            parseQuery1.whereEqualTo("receiver", UserActivity.currentUser.getUsername());
            ParseQuery<ParseObject> parseQuery2 = new ParseQuery<ParseObject>("Message");
            parseQuery2.whereEqualTo("sender", UserActivity.currentUser.getUsername());
            parseQuery2.whereEqualTo("receiver", activeUsername);

            ArrayList<ParseQuery<ParseObject>> queryList = new ArrayList<>();
            queryList.add(parseQuery1);
            queryList.add(parseQuery2);
            ParseQuery<ParseObject> queries = ParseQuery.or(queryList);

            SubscriptionHandling<ParseObject> subscriptionHandling = parseLiveQueryClient.subscribe(queries);
            subscriptionHandling.handleEvents(new SubscriptionHandling.HandleEventsCallback<ParseObject>() {
                @Override
                public void onEvents(ParseQuery<ParseObject> query, SubscriptionHandling.Event event, ParseObject object) {
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        public void run() {
                            fetchMessages(false);
                        }
                    });
                }
            });
        }
    }

    public void send(View v) {
        messageStatusCardViewEnter("Sending...");
        hideKeyboard(v);
        addFriend();

        final String message = messageEditText.getText().toString();
        if (message.length() > 0) {
            final ParseObject parseMessage = new ParseObject("Message");
            parseMessage.put("sender", UserActivity.currentUser.getUsername());
            parseMessage.put("receiver", activeUsername);
            parseMessage.put("message", message);
            parseMessage.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    if (e == null) {
                        messageStatusTextView.setText("Sent.");
                        sendPush(message);
                    } else {
                        messageStatusTextView.setText("Failed.");
                    }
                    messageStatusCardViewExit();
                }
            });
            messageEditText.setText("");
        }
    }

    void sendPush(String message) {
        if (!activeUser.getBoolean("online")) {
            try {
                String jsonString = "{\"alert\": \"" + message + "\",\"badge\": \"Increment\",\"title\": \"" + UserActivity.currentUser.get("name").toString() + "\",\"username\": \"" + UserActivity.currentUser.getUsername() + "\"}";
                JSONObject data = new JSONObject(jsonString);
                push.setData(data);
                push.sendInBackground();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    void queryFriend() {
        ParseQuery<ParseObject> query1 = new ParseQuery<>("Friends");
        query1.whereEqualTo("sender", UserActivity.currentUser.getUsername());
        query1.whereEqualTo("receiver", activeUsername);
        ParseQuery<ParseObject> query2 = new ParseQuery<>("Friends");
        query2.whereEqualTo("sender", activeUsername);
        query2.whereEqualTo("receiver", UserActivity.currentUser.getUsername());
        ArrayList<ParseQuery<ParseObject>> queryList = new ArrayList<>();
        queryList.add(query1);
        queryList.add(query2);
        ParseQuery<ParseObject> queries = ParseQuery.or(queryList);
        queries.setLimit(1);
        queries.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                    if (objects != null && objects.size() > 0)
                        friendObject = objects.get(0);
                    else
                        friendObject = new ParseObject("Friends");
                }
            }
        });
    }

    void addFriend() {
        if (friendObject != null) {
            friendObject.put("sender", UserActivity.currentUser.getUsername());
            friendObject.put("receiver", activeUsername);
            friendObject.saveInBackground();
        }
    }

    public void back(View v) {
        offlineForPush();
        finish();
    }

    void messageStatusCardViewEnter(String text) {
        anim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.enter_from_left);
        messageStatusTextView.setText(text);
        messageStatusCardView.startAnimation(anim);
    }

    void messageStatusCardViewExit() {
        anim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.exit_to_left);
        anim.setFillAfter(true);
        anim.setFillEnabled(true);
        messageStatusCardView.startAnimation(anim);
    }

    void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
