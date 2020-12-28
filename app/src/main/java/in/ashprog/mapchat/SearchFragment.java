package in.ashprog.mapchat;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.LOCATION_SERVICE;

public class SearchFragment extends Fragment implements OnMapReadyCallback {

    static GoogleMap googleMap;
    static String provider = LocationManager.NETWORK_PROVIDER;
    MapView mapView;
    LocationListener locationListener;
    TextView searchTextView;
    ProgressDialog progressDialog;
    LocationManager locationManager;

    RecyclerView recyclerView;
    BottomAdapter bottomAdapter;
    List<ParseUser> nearbyUsersList;

    public SearchFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_search, container, false);

        mapView = v.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        recyclerView = v.findViewById(R.id.bottomRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        nearbyUsersList = new ArrayList<>();
        bottomAdapter = new BottomAdapter(getContext(), nearbyUsersList);
        recyclerView.setAdapter(bottomAdapter);

        locationManager = (LocationManager) getContext().getSystemService(LOCATION_SERVICE);

        searchTextView = v.findViewById(R.id.searchTextView);
        ImageView renewImageView = v.findViewById(R.id.renewImageView);
        renewImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findUsers();
            }
        });

        progressDialog = new ProgressDialog(getContext());
        progressDialog.setCancelable(true);
        progressDialog.setProgressStyle(android.app.ProgressDialog.STYLE_SPINNER);
        progressDialog.setProgress(0);
        progressDialog.setMax(100);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                updateLocation(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @SuppressLint("MissingPermission")
            @Override
            public void onProviderEnabled(String provider) {
                if (UserActivity.userLocation == null) {
                    locationManager.requestLocationUpdates(provider, 60 * 1000, 1000, locationListener);
                    progressDialog.setMessage("Detecting Location...");
                    progressDialog.show();
                }
                findUsers();
            }

            @Override
            public void onProviderDisabled(String provider) {
                progressDialog.dismiss();
                searchTextView.setText("Enable Location.");
                Toast.makeText(getContext(), "Please enable Location Services.", Toast.LENGTH_SHORT).show();

            }
        };
        return v;
    }

    void updateLocation(Location location) {
        UserActivity.userLocation = location;
        ParseGeoPoint geo = new ParseGeoPoint(location.getLatitude(), location.getLongitude());
        ParseUser user = ParseUser.getCurrentUser();
        user.put("location", geo);
        user.saveInBackground();

        googleMap.clear();
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        addMarker(latLng, RescaleImage.addCircularBorder(UserActivity.profileBitmap, Color.CYAN), "You");

        progressDialog.dismiss();

        findUsers();
    }

    void findUsers() {
        if (UserActivity.userLocation != null) {
            searchTextView.setText("Searching...");
            ParseGeoPoint geo = new ParseGeoPoint(UserActivity.userLocation.getLatitude(), UserActivity.userLocation.getLongitude());
            progressDialog.setMessage("Finding Nearby Users....");
            progressDialog.show();
            ParseQuery<ParseUser> query = ParseUser.getQuery();
            query.whereNotEqualTo("username", UserActivity.currentUser.getUsername());
            query.whereNear("location", geo);
            query.whereEqualTo("discoverable", true);
            query.findInBackground(new FindCallback<ParseUser>() {
                @Override
                public void done(List<ParseUser> objects, ParseException e) {
                    if (e == null && objects.size() > 0) {
                        nearbyUsersList.clear();
                        searchTextView.setText((objects.size()) + " User/s found !");
                        for (ParseUser nearbyUsers : objects) {
                            ParseGeoPoint nearbyUsersGeo = nearbyUsers.getParseGeoPoint("location");
                            LatLng latLng1 = new LatLng(nearbyUsersGeo.getLatitude(), nearbyUsersGeo.getLongitude());
                            ParseFile file = (ParseFile) nearbyUsers.get("image");
                            Bitmap nearbyUsersBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.logo);
                            byte[] data = null;
                            try {
                                data = file.getData();
                            } catch (ParseException ex) {
                            }
                            if (data != null)
                                nearbyUsersBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                            String name = nearbyUsers.get("name").toString();
                            addMarker(latLng1, nearbyUsersBitmap, name);

                            nearbyUsersList.add(nearbyUsers);
                            bottomAdapter.notifyDataSetChanged();
                        }
                    } else {
                        searchTextView.setText("No nearby users !");
                    }

                    progressDialog.dismiss();
                }
            });
        }
    }

    void addMarker(LatLng latLng, Bitmap bitmap, String title) {
        Bitmap resizeBitmap = Bitmap.createScaledBitmap(bitmap, 80, 80, true);
        googleMap.addMarker(new MarkerOptions().position(latLng).title(title).icon(BitmapDescriptorFactory.fromBitmap(resizeBitmap)));
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12));
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        SearchFragment.googleMap = googleMap;
        googleMap.setPadding(0, 0, 0, 140);

        googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                        getContext(), R.raw.map_style));

        if (UserActivity.userLocation != null) {
            LatLng latLng = new LatLng(UserActivity.userLocation.getLatitude(), UserActivity.userLocation.getLongitude());
            addMarker(latLng, RescaleImage.addCircularBorder(UserActivity.profileBitmap, Color.CYAN), "You");
            findUsers();
        } else {
            locationManager.requestLocationUpdates(provider, 60 * 1000, 1000, locationListener);
        }

        if (UserActivity.userLocation == null && locationManager.isProviderEnabled(provider)) {
            progressDialog.setMessage("Detecting Location...");
            progressDialog.show();
        }

        if (!locationManager.isProviderEnabled(provider)) {
            searchTextView.setText("Enable Location.");
            Toast.makeText(getContext(), "Please enable your Location Services.", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onResume() {
        mapView.onResume();
        super.onResume();
    }


    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        if (mapView != null)
            mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
