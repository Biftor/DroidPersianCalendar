package com.byagowi.persiancalendar.view.preferences;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.widget.TextView;

import com.byagowi.persiancalendar.Constants;
import com.byagowi.persiancalendar.R;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by ebrahim on 3/26/16.
 */
public class GPSLocationDialog extends PreferenceDialogFragmentCompat {

    LocationManager locationManager;
    Context context;
    TextView textView;

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);

        context = getContext();
        textView = new TextView(context);
        textView.setPadding(32, 32, 32, 32);
        textView.setTextSize(20);
        textView.setText(R.string.pleasewait);

        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        tryRetrieveLocation();
        LocalBroadcastManager.getInstance(context).registerReceiver(permissionGrantReceiver,
                new IntentFilter(Constants.LOCATION_PERMISSION_RESULT));

        builder.setPositiveButton("", null);
        builder.setNegativeButton("", null);
        builder.setView(textView);
    }

    BroadcastReceiver permissionGrantReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            tryRetrieveLocation();
        }
    };

    // http://stackoverflow.com/a/12963889
    private Location getLastBestLocation(Location locationGPS, Location locationNet) {
        long gpsLocationTime = 0;
        if (null != locationGPS) {
            gpsLocationTime = locationGPS.getTime();
        }

        long netLocationTime = 0;

        if (null != locationNet) {
            netLocationTime = locationNet.getTime();
        }

        if (0 < gpsLocationTime - netLocationTime) {
            return locationGPS;
        } else {
            return locationNet;
        }
    }

    // Just ask for permission once, if we couldn't get it, nvm
    public boolean first = true;

    public void tryRetrieveLocation() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            Location location = getLastBestLocation(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER),
                    locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER));
            if (location != null) {
                showLocation(location);
            }

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, locationListener);
        } else if (first) {
            first = false;
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    Constants.LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            dismiss();
        }
    }

    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            showLocation(location);
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
        }

        @Override
        public void onProviderEnabled(String s) {
        }

        @Override
        public void onProviderDisabled(String s) {
        }
    };

    String latitude;
    String longitude;
    String cityName;

    public void showLocation(Location location) {
        latitude = String.format(Locale.ENGLISH, "%.4f", location.getLatitude());
        longitude = String.format(Locale.ENGLISH, "%.4f", location.getLongitude());
        Geocoder gcd = new Geocoder(context, Locale.getDefault());
        List<Address> addresses;
        try {
            addresses = gcd.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses.size() > 0) {
                cityName = addresses.get(0).getLocality();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        String result = "";
        if (cityName != null) {
            result = cityName + "\n\n";
        }
        result += getString(R.string.latitude) + ": " + latitude + "\n" +
                getString(R.string.longitude) + ": " + longitude;
        textView.setText(result);
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(permissionGrantReceiver);

        if (latitude != null && longitude != null) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(Constants.PREF_LATITUDE, latitude);
            editor.putString(Constants.PREF_LONGITUDE, longitude);
            if (cityName != null) {
                editor.putString(Constants.PREF_GEOCODED_CITYNAME, cityName);
            }
            editor.commit();
        }

        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.removeUpdates(locationListener);
        }
    }
}
