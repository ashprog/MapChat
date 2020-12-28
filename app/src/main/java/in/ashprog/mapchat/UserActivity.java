package in.ashprog.mapchat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.parse.GetDataCallback;
import com.parse.LogOutCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseInstallation;
import com.parse.ParseUser;
import com.parse.SaveCallback;

public class UserActivity extends AppCompatActivity {

    static Bitmap profileBitmap;
    static Location userLocation;
    static ParseUser currentUser;
    ChatListFragment chatListFragment;
    SearchFragment searchFragment;
    DrawerLayout drawerLayout;
    NavigationView navigationView;
    TextView nameTextView, usernameTextView;
    String name, username;
    BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        currentUser = ParseUser.getCurrentUser();

        currentUser.put("online", true);
        currentUser.saveInBackground();

        name = currentUser.get("name").toString();
        username = currentUser.getUsername();

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        navigationView.bringToFront();
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.profileItem:
                        Toast.makeText(UserActivity.this, "Update Profile option will be enable soon.", Toast.LENGTH_SHORT).show();
                        drawerLayout.closeDrawer(GravityCompat.START);
                        return true;

                    case R.id.logoutItem:
                        final ProgressDialog progressDialog = new ProgressDialog(UserActivity.this);
                        progressDialog.setCancelable(false);
                        progressDialog.setProgressStyle(android.app.ProgressDialog.STYLE_SPINNER);
                        progressDialog.setProgress(0);
                        progressDialog.setMax(100);
                        progressDialog.setMessage("Logging Out...");
                        progressDialog.show();
                        ParseUser.logOutInBackground(new LogOutCallback() {
                            @Override
                            public void done(ParseException e) {
                                progressDialog.dismiss();
                                if (e == null) {
                                    ParseInstallation installation = ParseInstallation.getCurrentInstallation();
                                    installation.put("username", "null");
                                    installation.saveInBackground();
                                    startActivity(new Intent(UserActivity.this, MainActivity.class));
                                    finish();
                                } else {
                                    Toast.makeText(UserActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                        return true;

                    case R.id.discoverableItem:
                        Toast.makeText(UserActivity.this, "Makes you discoverable by others", Toast.LENGTH_SHORT).show();
                        return true;
                }
                return false;
            }
        });
        Switch discoverableSwitch = navigationView.getMenu().findItem(R.id.discoverableItem).getActionView().findViewById(R.id.discoverableSwitch);
        discoverableSwitch.setChecked(currentUser.getBoolean("discoverable"));
        discoverableSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    currentUser.put("discoverable", true);
                } else {
                    currentUser.put("discoverable", false);
                }
                currentUser.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if (e == null)
                            Toast.makeText(UserActivity.this, "Changes Saved", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        final View headerView = navigationView.getHeaderView(0);
        nameTextView = headerView.findViewById(R.id.nameTextViewBottom);
        nameTextView.setText(name);
        usernameTextView = headerView.findViewById(R.id.usernameTextView);
        usernameTextView.setText("@" + username);

        ParseFile parseFile = (ParseFile) currentUser.get("image");
        parseFile.getDataInBackground(new GetDataCallback() {
            @Override
            public void done(byte[] data, ParseException e) {
                if (e == null && data != null) {
                    profileBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    Bitmap resizedProfileBitmap = Bitmap.createScaledBitmap(profileBitmap, 50, 50, true);
                    ImageView profileImageView = findViewById(R.id.profileImageViewTop);
                    profileImageView.setImageBitmap(resizedProfileBitmap);
                    ImageView navProfileImageView = headerView.findViewById(R.id.imageView);
                    navProfileImageView.setImageBitmap(profileBitmap);
                }
            }
        });

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }

        chatListFragment = new ChatListFragment();
        searchFragment = new SearchFragment();

        addFragment(chatListFragment);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.home:
                        getSupportFragmentManager().popBackStack();
                        return true;
                    case R.id.nearby:
                        if (ActivityCompat.checkSelfPermission(UserActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(UserActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(UserActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                        } else {
                            addFragment(searchFragment);
                        }
                        return true;
                }
                return false;
            }
        });

        ParseInstallation installation = ParseInstallation.getCurrentInstallation();
        if (!installation.has("username") || !installation.get("username").toString().equals(ParseUser.getCurrentUser().getUsername())) {
            installation.put("GCMSenderId", "710450804636");
            installation.put("username", ParseUser.getCurrentUser().getUsername());
            installation.saveInBackground();
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START))
            drawerLayout.closeDrawer(GravityCompat.START);
        else {
            if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
                bottomNavigationView.setSelectedItemId(R.id.home);
            } else {
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        currentUser.put("online", false);
        currentUser.saveInBackground();

        super.onDestroy();
    }

    void addFragment(Fragment fragment) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (fragment instanceof SearchFragment)
            ft.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_right, R.anim.exit_to_right);
        ft.add(R.id.frameLayout, fragment);
        ft.addToBackStack(null);
        ft.commit();
    }

    public void openDrawer(View v) {
        drawerLayout.openDrawer(GravityCompat.START);
    }
}
