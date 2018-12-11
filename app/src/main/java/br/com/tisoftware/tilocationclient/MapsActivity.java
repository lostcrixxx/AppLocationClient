package br.com.tisoftware.tilocationclient;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    String showUrl = "http://tilocationmobile.atspace.cc/resultadoJSON.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-23.51960983, -46.70196814);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        points();
    }


    public void points() {
        Log.i("localização","Chamou método");
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST,
                showUrl, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.i("localização","onResponse");
                try {
                    JSONArray students = response.getJSONArray("dados");
                    for (int i = 0; i < students.length(); i++) {
                        JSONObject student = students.getJSONObject(i);

                        String lat = student.getString("latitude");
                        String lng = student.getString("longitude");

                        Log.i("localização","teste " + lat + " , " + lng);
                        //result.append(um + " - " + dois + " - " + tres + "  \n");
                    }
                    //result.append("===\n");

                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.i("localização","Catch");
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.append(error.getMessage());
                Log.i("localização","Erro");
            }
        });

    }


    /*
    JsonObjectRequest obreq = new JsonObjectRequest(Request.Method.GET, url, new

            Response.Listener<JSONArray>() {

                @Override
                public void onResponse(JSONArray response) {
                    try {
                        JSONObject obj = response.getJSONObject(0);
                        Double ask = obj.getDouble("ask");
                    }
                    JSONcatch (JSONException e) {

                        e.printStackTrace();
                    }
                }
            },null);
            */
}
