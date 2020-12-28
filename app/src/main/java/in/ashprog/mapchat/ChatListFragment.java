package in.ashprog.mapchat;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.livequery.SubscriptionHandling;

import java.util.ArrayList;
import java.util.List;

import static in.ashprog.mapchat.MainActivity.parseLiveQueryClient;

public class ChatListFragment extends Fragment {

    RecyclerView recyclerView;
    ChatListAdapter chatListAdapter;
    ArrayList<String> friends, subscribedUsers;

    public ChatListFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_chat_list, container, false);

        subscribedUsers = new ArrayList<>();
        friends = new ArrayList<>();
        recyclerView = v.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        chatListAdapter = new ChatListAdapter(getContext(), friends);
        recyclerView.setAdapter(chatListAdapter);

        getFriends();
        subscribeToFriends();

        return v;
    }

    void getFriends() {
        ParseQuery<ParseObject> query1 = new ParseQuery<>("Friends");
        query1.whereEqualTo("sender", UserActivity.currentUser.getUsername());
        ParseQuery<ParseObject> query2 = new ParseQuery<>("Friends");
        query2.whereEqualTo("receiver", UserActivity.currentUser.getUsername());
        ArrayList<ParseQuery<ParseObject>> queryList = new ArrayList<>();
        queryList.add(query1);
        queryList.add(query2);
        ParseQuery<ParseObject> queries = ParseQuery.or(queryList);
        queries.addDescendingOrder("updatedAt");
        queries.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null && objects.size() > 0) {
                    friends.clear();
                    for (ParseObject object : objects) {
                        String username;
                        if (object.get("sender").toString().equals(UserActivity.currentUser.getUsername()))
                            username = object.get("receiver").toString();
                        else
                            username = object.get("sender").toString();

                        friends.add(username);
                        subscribeToUser(username);
                        chatListAdapter.notifyDataSetChanged();
                    }
                }
            }
        });
    }

    void subscribeToFriends() {
        if (parseLiveQueryClient != null) {
            ParseQuery<ParseObject> query1 = new ParseQuery<>("Friends");
            query1.whereEqualTo("sender", UserActivity.currentUser.getUsername());
            ParseQuery<ParseObject> query2 = new ParseQuery<>("Friends");
            query2.whereEqualTo("receiver", UserActivity.currentUser.getUsername());
            ArrayList<ParseQuery<ParseObject>> queryList = new ArrayList<>();
            queryList.add(query1);
            queryList.add(query2);
            ParseQuery<ParseObject> queries = ParseQuery.or(queryList);

            SubscriptionHandling<ParseObject> subscriptionHandling = parseLiveQueryClient.subscribe(queries);
            subscriptionHandling.handleEvents(new SubscriptionHandling.HandleEventsCallback<ParseObject>() {
                @Override
                public void onEvents(ParseQuery<ParseObject> query, SubscriptionHandling.Event event, ParseObject object) {
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        public void run() {
                            getFriends();
                        }
                    });
                }
            });
        }
    }

    void subscribeToUser(final String username) {
        //Live Query
        if (!subscribedUsers.contains(username)) {
            subscribedUsers.add(username);
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
                                chatListAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                });
            }
            subscribeToMessage(username);
        }
    }

    void subscribeToMessage(String username) {
        if (parseLiveQueryClient != null) {
            ParseQuery<ParseObject> parseQuery1 = new ParseQuery<ParseObject>("Message");
            parseQuery1.whereEqualTo("receiver", UserActivity.currentUser.getUsername());
            parseQuery1.whereEqualTo("sender", username);
            ParseQuery<ParseObject> parseQuery2 = new ParseQuery<ParseObject>("Message");
            parseQuery2.whereEqualTo("sender", UserActivity.currentUser.getUsername());
            parseQuery2.whereEqualTo("receiver", username);
            ArrayList<ParseQuery<ParseObject>> queryArrayList = new ArrayList<>();
            queryArrayList.add(parseQuery1);
            queryArrayList.add(parseQuery2);
            ParseQuery<ParseObject> queries = ParseQuery.or(queryArrayList);
            queries.orderByDescending("createdAt");
            queries.setLimit(1);

            SubscriptionHandling<ParseObject> subscriptionHandling = parseLiveQueryClient.subscribe(queries);
            subscriptionHandling.handleEvents(new SubscriptionHandling.HandleEventsCallback<ParseObject>() {
                @Override
                public void onEvents(ParseQuery<ParseObject> query, SubscriptionHandling.Event event, ParseObject object) {
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        public void run() {
                            chatListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            });
        }
    }
}
