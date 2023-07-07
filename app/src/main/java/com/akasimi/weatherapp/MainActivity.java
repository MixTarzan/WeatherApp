package com.akasimi.weatherapp;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;

import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import com.google.android.material.textfield.TextInputEditText;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private RelativeLayout homeLayout;
    private ProgressBar imgLoading;
    private TextView txtCityName, txtTemperature, txtCondition;
    private TextInputEditText cityEdt;
    private ImageView backIV;
    private ImageView iconIV;
    private ArrayList<WeatherModel> weatherModelArrayList;
    private WeatherAdapter weatherAdapter;
    private final int PERMISSIONS_CODE = 1;
    private String cityName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setContentView(R.layout.activity_main);
        homeLayout = findViewById(R.id.id_home);
        imgLoading = findViewById(R.id.id_loading);
        txtCityName = findViewById(R.id.txt_cityName);
        txtTemperature = findViewById(R.id.txt_temperature);
        txtCondition = findViewById(R.id.id_condition);
        RecyclerView weatherRV = findViewById(R.id.idRvWeather);
        cityEdt = findViewById(R.id.txt_edit_city);
        backIV = findViewById(R.id.img_back);
        iconIV = findViewById(R.id.img_icon);
        ImageView searchIV = findViewById(R.id.img_search);
        weatherModelArrayList = new ArrayList<>();
        weatherAdapter = new WeatherAdapter(this, weatherModelArrayList);
        weatherRV.setAdapter(weatherAdapter);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, PERMISSIONS_CODE);
        } else {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            if (locationManager != null) {
                Location location = null;

                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    try {
                        location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                }

                if (location != null) {
                    cityName = getCityName(location.getLatitude(), location.getLongitude());
                    getWeatherInfo(cityName);
                } else {
                    Toast.makeText(MainActivity.this, "Weather provider not available", Toast.LENGTH_SHORT).show();
                }
            }
        }

        searchIV.setOnClickListener(view -> {
            String city = Objects.requireNonNull(cityEdt.getText()).toString();
            if (city.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please enter a city name", Toast.LENGTH_SHORT).show();
            } else {
                txtCityName.setText(cityName);
                getWeatherInfo(city);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_CODE){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show();
            }else {
                Toast.makeText(this, "Please provide the permission", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private String getCityName(double latitude, double longitude) {
        String cityName = "not found";
        Geocoder geo = new Geocoder(getBaseContext(), Locale.getDefault());

        try {
            List<Address> addresses = geo.getFromLocation(latitude, longitude, 10);

            if (!addresses.isEmpty()) {
                for (Address address : addresses) {
                    String city = address.getLocality();
                    if (city != null && !city.equals("")) {
                        cityName = city;
                        break;
                    }
                }
            } else {
                Log.e(TAG, "No addresses found");
                Toast.makeText(this, "User City not found :(", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return cityName;
    }

    private void getWeatherInfo(String cityName){
        String url = "http://api.weatherapi.com/v1/forecast.json?key=f0eddb8b5d504c96abb110255230707&q="+cityName+"&days=1&aqi=yes&alerts=yes";
        txtCityName.setText(cityName);
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, response -> {
            imgLoading.setVisibility(View.GONE);
            homeLayout.setVisibility(View.VISIBLE);
            weatherModelArrayList.clear();

            try {
                String temp = response.getJSONObject("current").getString("temp_c");
                txtTemperature.setText(temp + "°C" );
                int isDay = response.getJSONObject("current").getInt("is_day");
                String condition = response.getJSONObject("current").getJSONObject("condition").getString("text");
                String conditionIcon = response.getJSONObject("current").getJSONObject("condition").getString("icon");
                Picasso.get().load("http:".concat(conditionIcon)).into(iconIV);
                txtCondition.setText(condition);

                if (isDay == 1) {
                    Picasso.get().load("https://s3.us-west-2.amazonaws.com/images.unsplash.com/application-1688737239687-e0416f13656cimage").into(backIV);
                }else {
                    Picasso.get().load("https://s3.us-west-2.amazonaws.com/images.unsplash.com/application-1688765111415-b4b3d9195c8dimage").into(backIV);
                }

                JSONObject forecastObj = response.getJSONObject("forecast");
                JSONObject forecast = forecastObj.getJSONArray("forecastday").getJSONObject(0);
                JSONArray hourArray = forecast.getJSONArray("hour");

                for (int i = 0; i < forecast.length(); i++) {
                    JSONObject hourObj = hourArray.getJSONObject(i);
                    String time = hourObj.getString("time");
                    String temper = hourObj.getString("temp_c");
                    String img = hourObj.getJSONObject("condition").getString("icon");
                    String wind = hourObj.getString("wind_kph");
                    weatherModelArrayList.add(new WeatherModel(time, temper, img, wind));
                }
                weatherAdapter.notifyDataSetChanged();
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

        }, error -> {
            String errorMessage = error.getMessage();
            Log.e(TAG, "Weather API Error: " + errorMessage);
            Toast.makeText(MainActivity.this, "Weather API Error: " + errorMessage, Toast.LENGTH_SHORT).show();
        });

        requestQueue.add(jsonObjectRequest);
    }
}