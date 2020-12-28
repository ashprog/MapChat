package in.ashprog.mapchat;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.textfield.TextInputLayout;
import com.parse.LogInCallback;
import com.parse.LogOutCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.SignUpCallback;
import com.parse.livequery.ParseLiveQueryClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    static ParseLiveQueryClient parseLiveQueryClient = null;
    static TextInputLayout emailLayout, usernameLayout, passwordLayout, nameLayout, dateLayout;
    ProgressDialog progressDialog;
    CardView cardView;
    ConstraintLayout layout;
    Animation anim;
    ParseUser user;
    ImageView logoImageView;
    byte[] byteArray;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            imageSelectIntent();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        final ParseUser parseUser = ParseUser.getCurrentUser();

        cardView = findViewById(R.id.cardView);
        cardView.setAlpha(0f);
        cardView.animate().alphaBy(1).setDuration(1000).start();

        logoImageView = findViewById(R.id.logoImageView);

        layout = findViewById(R.id.layout);
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard(layout);
            }
        });

        user = new ParseUser();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (parseUser == null || !parseUser.getBoolean("profileUpdated")) {
                    anim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.animation);
                    cardView.startAnimation(anim);

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (parseUser == null)
                                addFragment(new UserFragment());
                            else {
                                addFragment(new ProfileFragment());
                                logoImageView.setImageResource(R.drawable.ic_face_grey_100dp);
                            }
                        }
                    }, 500);
                } else {
                    startActivity(new Intent(MainActivity.this, UserActivity.class));
                    finish();
                }
            }
        }, 1000);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Please wait....");
        progressDialog.setProgressStyle(android.app.ProgressDialog.STYLE_SPINNER);
        progressDialog.setProgress(0);
        progressDialog.setMax(100);
    }

    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }

    void addFragment(Fragment fragment) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (fragment instanceof UserFragment)
            ft.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right);
        else if (fragment instanceof ProfileFragment)
            ft.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left);
        ft.replace(R.id.fragment, fragment);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.commit();
    }

    public void login(View v) {
        progressDialog.show();
        hideKeyboard(v);

        String username = usernameLayout.getEditText().getText().toString();
        String password = passwordLayout.getEditText().getText().toString();

        ParseUser.logInInBackground(username, password, new LogInCallback() {
            @Override
            public void done(ParseUser user, ParseException e) {
                progressDialog.dismiss();
                if (e == null && user != null) {
                    if (user.getBoolean("profileUpdated")) {
                        startActivity(new Intent(MainActivity.this, UserActivity.class));
                        finish();
                    } else {
                        addFragment(new ProfileFragment());
                        logoImageView.setImageResource(R.drawable.ic_face_grey_100dp);
                    }
                } else {
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    public void signUp(View v) {
        hideKeyboard(v);
        progressDialog.show();

        String username = usernameLayout.getEditText().getText().toString();
        String email = emailLayout.getEditText().getText().toString();
        String password = passwordLayout.getEditText().getText().toString();

        if (email.length() > 0 && username.length() > 0 && password.length() > 0) {
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(password);

            user.signUpInBackground(new SignUpCallback() {
                @Override
                public void done(ParseException e) {
                    progressDialog.dismiss();
                    if (e == null) {
                        ParseUser.logOut();
                        new AlertDialog.Builder(MainActivity.this)
                                .setIcon(R.drawable.ic_account_circle_white_24dp)
                                .setTitle("Account Created !")
                                .setMessage("Check your email for verification...")
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        usernameLayout.getEditText().setText("");
                                        emailLayout.getEditText().setText("");
                                        passwordLayout.getEditText().setText("");
                                    }
                                })
                                .show();
                    } else {
                        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            progressDialog.dismiss();
            Toast.makeText(this, "Please fill all the fields.", Toast.LENGTH_SHORT).show();
        }
    }

    public void logout(View v) {
        hideKeyboard(v);
        progressDialog.show();
        ParseUser.logOutInBackground(new LogOutCallback() {
            @Override
            public void done(ParseException e) {
                progressDialog.dismiss();
                if (e == null) {
                    addFragment(new UserFragment());
                    logoImageView.setImageResource(R.drawable.logo);
                    Toast.makeText(MainActivity.this, "Logged Out", Toast.LENGTH_SHORT).show();
                } else
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void submit(View v) {
        progressDialog.show();
        hideKeyboard(v);
        ParseUser user = ParseUser.getCurrentUser();
        String name = nameLayout.getEditText().getText().toString();
        String dateString = dateLayout.getEditText().getText().toString() + " 14:00:00.000";
        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS", Locale.getDefault());
        try {
            if (name.length() > 0 && dateString.length() > 0 && byteArray != null) {
                Date dob = df.parse(dateString);
                ParseFile imageFile = new ParseFile("image.png", byteArray);

                user.put("name", name);
                user.put("dob", dob);
                user.put("image", imageFile);
                user.put("profileUpdated", true);

                user.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        progressDialog.dismiss();
                        if (e == null) {
                            Toast.makeText(MainActivity.this, "Submitted", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(MainActivity.this, UserActivity.class));
                            finish();
                        } else {
                            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });

            } else {
                progressDialog.dismiss();
                Toast.makeText(this, "Please fill all the fields.", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            progressDialog.dismiss();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void selectImage(View v) {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else {
            imageSelectIntent();
        }
    }

    void imageSelectIntent() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        try {
            if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
                Uri uri = data.getData();
                Bitmap selectedBitmap = RescaleImage.getRoundedResizedBitmap(MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri));
                logoImageView.setImageBitmap(selectedBitmap);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                selectedBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byteArray = stream.toByteArray();
            }
        } catch (IOException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
