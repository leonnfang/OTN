package com.example.otn;

import android.app.Application;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Routing extends Application {

    private static final String baseURL = "https://maps.googleapis.com/maps/api/directions/json?";
    private static final String TAG = "Routing";
    private static Routing mInstance;


    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
    }


    public void getRoute(LatLng orig, LatLng dest, String apiKey)
    {
        String origin = orig.latitude + ","+orig.longitude;
        String destination = dest.latitude + ","+dest.longitude;
        String requestURL = baseURL +
                String.format("origin=%s&destination=%s&mode=walking&key=%s",
                        origin,destination, apiKey);
        Log.i(TAG, "Request URL is:" + requestURL);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, requestURL, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "getRoute Request response:" + response.toString());
                        ArrayList<LatLng> start_location = new ArrayList<>();
                        ArrayList<LatLng> end_location = new ArrayList<>();
                        parseResult(response, start_location, end_location);

                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.i(TAG, "getRoute Request error:" + error.toString());
                    }
                });
        // Access the RequestQueue through your singleton class.
        MySingleton.getInstance(mInstance).addToRequestQueue(jsonObjectRequest);
    }


    public void parseResult(JSONObject mJSONresult, ArrayList<LatLng> start_location, ArrayList<LatLng> end_location)
    {
        try{
            JSONArray routes;
            if (mJSONresult.has("status") && mJSONresult.has("routes"))
            {
                if (mJSONresult.getString("status").equals("OK")){
                    routes = mJSONresult.getJSONArray("routes");
                }
                else {
                    Log.i(TAG, "parseResult: No route to this place");
                    return;
                }
                JSONObject route = routes.getJSONObject(0);
                JSONArray Legs = route.getJSONArray("legs");
                for (int i  = 0; i < Legs.length(); i++)
                {
                    JSONObject leg = Legs.getJSONObject(i);
                    JSONObject orig = leg.getJSONObject("start_location");
                    JSONObject dest = leg.getJSONObject("end_location");
                    LatLng start = new LatLng(Double.parseDouble(orig.get("lat").toString()),
                            Double.parseDouble(orig.get("lng").toString()));
                    LatLng end = new LatLng(Double.parseDouble(dest.get("lat").toString()),
                            Double.parseDouble(dest.get("lng").toString()));
                    start_location.add(start);
                    end_location.add(end);
                }
            }
        } catch (JSONException e) {
            Log.i(TAG, "parseResult Error:" + e.toString());
        }
    }
}
