package com.fewstreet.remotefakegps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.net.wifi.WifiManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;


public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLng mapLocation;
    private static final String TAG = "MapActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        startService(new Intent(getBaseContext(), WebLocationService.class));
        Log.v(TAG, "MapActivity Created");
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter(GPSHTTPD.NEW_LOCATION_CHOSEN_ACTION));
        updateWiFiState();
        Log.v(TAG, "MapActivity Resumed");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "MapActivity Destroyed");
    }

    private void updateWiFiState() {
        TextView textIpaddr = (TextView) findViewById(R.id.ip_addr);
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiInfo winfo = wifiManager.getConnectionInfo();
        SupplicantState wifiState = winfo.getSupplicantState();

        if (wifiState == SupplicantState.COMPLETED) {
            int ipAddress = winfo.getIpAddress();
            // Convert little-endian to big-endianif needed
            if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
                ipAddress = Integer.reverseBytes(ipAddress);
            }

            byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

            String ipAddressString;
            try {
                ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
            } catch (UnknownHostException ex) {
                Log.e("WIFIIP", "Unable to get host address.");
                ipAddressString = null;
            }

            Log.v(TAG, "Connected to WiFi "+ipAddressString);
            textIpaddr.setText(String.format(getResources().getString(R.string.connect_to), ipAddressString, 8080));
        } else {
            Log.v(TAG, "Not connected to WiFi "+wifiState);
            textIpaddr.setText(R.string.not_connected);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.v(TAG, "Intent recieved");
        String action = intent.getAction();
        if (action == null) {
            return;
        }
        switch (action) {
            case WebLocationService.CLOSE_ACTION:
                Log.v(TAG, "CLOSE_ACTION recieved");
                stopService(new Intent(getBaseContext(), WebLocationService.class));
                Log.v(TAG, "Service stopped");
                finish();
                break;
        }
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            float lat = intent.getFloatExtra("lat", 0);
            float lng = intent.getFloatExtra("lng", 0);
            mapLocation = new LatLng(lat, lng);
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(mapLocation));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(mapLocation));
        }
    };

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }
}
