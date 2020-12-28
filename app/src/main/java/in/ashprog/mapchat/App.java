package in.ashprog.mapchat;

import android.app.Application;

import com.parse.Parse;
import com.parse.livequery.ParseLiveQueryClient;

import java.net.URI;

import static in.ashprog.mapchat.CustomPushBroadcastReceiver.createChannel;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId("SkHic1p689GWSdMO8Q5iZTeelcKpHXVyDNLnMMs1")
                .clientKey("xzGyTD0coJ0vBJ8aLFzwGOSNpEcReqyJjAtAlZNM")
                .server("https://ashprog.back4app.io/")
                .build()
        );

        createChannel(this);

        try {
            MainActivity.parseLiveQueryClient = ParseLiveQueryClient.Factory.getClient(new URI("https://ashprog.back4app.io/"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
