package com.dimitriskatsikas.dryingtime;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {


    public static boolean datacheck = false;
    public static boolean outdoorButtonChecked=true;
    public boolean location=false;
    public static double [] T= new double[41];
    public static double [] h=new double[41];
    public static double [] u= new double[41];
    public static double [] P= new double[41];
    public static long t;
    public static double Temperature;
    public static String latitude, longitude;
    public static String coord;
    Handler handler;
    private FirebaseAnalytics mFirebaseAnalytics;

    public MainActivity() {
        handler = new Handler();
    }

    InterstitialAd mInterstitialAd;


    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(outdoorButtonChecked){
        EditText editText = (EditText) findViewById(R.id.editText2);
        editText.setHint(R.string.hint1);
        editText.setEnabled(false);}

        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId("ca-app-pub-4442207590956373/6217774444");

        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                requestNewInterstitial();
                Intent i = new Intent(MainActivity.this, Outdoor.class);
                startActivity(i);
            }
        });

        requestNewInterstitial();

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
            mGoogleApiClient.connect();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    protected void onResume() {
        super.onResume();
        EditText editText = (EditText) findViewById(R.id.editText2);
        if (!outdoorButtonChecked) {
            SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            String value = SP.getString(getString(R.string.units_key), "1");
            if (value.equals("1")) {
                editText.setHint(R.string.hint2);
            } else {
                editText.setHint(R.string.hint3);
            }
        }

    }

    private void requestNewInterstitial() {
        AdRequest adRequest = new AdRequest.Builder().build();

        mInterstitialAd.loadAd(adRequest);
    }

    public void CalculateClick(View v) {
        EditText editText2 = (EditText) findViewById(R.id.editText2);
        if (outdoorButtonChecked) {
            if(location){
                updateWeatherData(coord);
            if (mInterstitialAd.isLoaded()) {
                mInterstitialAd.show();
            } else {
                Intent i = new Intent(MainActivity.this, Outdoor.class);
                startActivity(i);
            }}
            else{
                mGoogleApiClient.connect();
                Toast.makeText(this, getString(R.string.location_not_found), Toast.LENGTH_SHORT).show();
            }
        }
        else {
            if(location){
             if (!editText2.getText().toString().equals("")) {
                updateWeatherData(coord);
                Temperature = Double.parseDouble(editText2.getText().toString());
                SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                String value = SP.getString(getString(R.string.units_key), "1");
                if (value.equals("2")) {
                    Temperature = (Temperature - 32) * 5 / 9;
                }
                Temperature = Temperature + 273.15;
                if (mInterstitialAd.isLoaded()) {
                    mInterstitialAd.show();
                } else {
                    Intent i = new Intent(MainActivity.this, Outdoor.class);
                    startActivity(i);
                }
             }
             else{
                Toast.makeText(this, getString(R.string.temperature_not_filled), Toast.LENGTH_SHORT).show();
                }
            }
            else{
                mGoogleApiClient.connect();
                Toast.makeText(this, getString(R.string.location_not_found), Toast.LENGTH_SHORT).show();
            }
        }
    }


    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();
        EditText editText = (EditText) findViewById(R.id.editText2);

        // Check which radio button was clicked
        switch (view.getId()) {
            case R.id.radioButton:
                if (checked) {
                    outdoorButtonChecked = true;
                    editText.setText("");
                    editText.setHint(R.string.hint1);
                    editText.setEnabled(false);

                    break;
                }
            case R.id.radioButton2:
                if (checked) {
                    outdoorButtonChecked = false;
                    SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                    String value = SP.getString(getString(R.string.units_key), "1");
                    if (value.equals("1")) {
                        editText.setHint(R.string.hint2);
                    } else {
                        editText.setHint(R.string.hint3);
                    }
                    editText.setEnabled(true);
                    break;
                }
        }
    }


    private void updateWeatherData(final String coord) {
        new Thread() {
            public void run() {
                final JSONObject json = RemoteFetch.getJSON(MainActivity.this, coord);
                if (json == null) {
                    handler.post(new Runnable() {
                        public void run() {
                            datacheck = false;
                        }
                    });
                } else {
                    datacheck = true;
                    handler.post(new Runnable() {
                        public void run() {
                            renderWeather(json);
                        }
                    });
                }
            }
        }.start();
    }

    private void renderWeather(JSONObject json) {
        try {
            //Get the instance of JSONArray that contains JSONObjects
            JSONArray jsonArray = json.getJSONArray("list");

            //Iterate the jsonArray and print the info of JSONObjects
            for(int i=0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                JSONObject main = jsonObject.getJSONObject("main");
                JSONObject wind = jsonObject.getJSONObject("wind");
                if(i>40){
                    break;
                }
                h[i] = main.getDouble("humidity");
                P[i] = main.getDouble("pressure");
                T[i] = main.getDouble("temp");
                u[i] = wind.getDouble("speed");
                h[i] = h[i] / 100; // gives percentage
                P[i] = 0.7501 * P[i]; // gives pressure from hPa to mmHg
                u[i] = 3.6 * u[i]; // gives speed at Km/h
                if(i==0){
                    t=jsonObject.getLong("dt");
                }
            }


        } catch (Exception e) {
            Log.e("WeatherData", "One or more fields not found in the JSON data");
            datacheck = false;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.settings) {
            Intent c = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(c);
            return true;
        }
        if (id == R.id.about) {
            Intent c = new Intent(MainActivity.this, AboutActivity.class);
            startActivity(c);
            return true;
        }


        return super.onOptionsItemSelected(item);
    }



    /**
     * Google api callback methods
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            latitude=String.valueOf(mLastLocation.getLatitude());
            longitude=String.valueOf(mLastLocation.getLongitude());
            coord="lat="+latitude+"&lon="+longitude;
            location=true;
        }
        else{
            location=false;
        }

    }


    @Override
    public void onConnectionSuspended(int i) {
        location=false;
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        location=false;

    }
}
