package br.com.tisoftware.tilocationclient;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import br.com.tisoftware.tilocationclient.directionhelpers.FetchURL;
import br.com.tisoftware.tilocationclient.directionhelpers.TaskLoadedCallback;

import static java.lang.Double.parseDouble;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener, TaskLoadedCallback, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;

    boolean status;
    public static final String URL="http://tilocationmobile.atspace.cc/location.php"; // JSON
    private JSONArray pontos; // Dados do banco
    public List<LatLng> points = new ArrayList<>(); // Coordenadas

    // Polyline Directions
    private Polyline currentPolyline;

    private List<Polyline> polylinePaths = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

            runtime_permissions();


        // Exibir o mapa
        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }




    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL); // Tipo de mapa


        // TODO Solicitar permissão para local
        // Verifica as permissões
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {

            return;
        }

        // Localização do próprio celular
        //mMap.setMyLocationEnabled(true);


        RequestQueue requestQueue= Volley.newRequestQueue(getApplicationContext());
        StringRequest stringRequest = new StringRequest(Request.Method.GET, URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("JSONResult" , response.toString());
                JSONObject objeto = null;
                polylinePaths = new ArrayList<>();

                try{
                    objeto = new JSONObject(response);
                    pontos = objeto.getJSONArray("Pontos");
                    for(int i=0;i<pontos.length();i++){
                        JSONObject jsonObject1 = pontos.getJSONObject(i);
                        String lat_i = jsonObject1.getString("latitude");
                        String long_i = jsonObject1.getString("longitude");

                        mMap.addMarker(new MarkerOptions()
                                        .position(new LatLng(parseDouble(lat_i) , parseDouble(long_i))) // Coordenada
                                //.title(Double.valueOf(lat_i).toString() + "," + Double.valueOf(long_i).toString()) // Titulo
                                //.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE)) // Imagem do icone
                        );

                        // Novo Array para desenhar a Polyline
                        points.add(new LatLng(parseDouble(lat_i), parseDouble(long_i)));

                    }


                    // Directions - Pega primeira e a última posição
                    if (points.size() >= 2) {
                        LatLng origin = (LatLng) points.get(0); // Pirmeira posição
                        LatLng dest = (LatLng) points.get(points.size()-1); // TODO Pegar a última posição
                        new FetchURL(MapsActivity.this).execute(getUrl(origin, dest,"walking"), "walking");
                        // driving(Carro), walking(Caminhando)

                        // TODO centralizar o mapa com os Points
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(dest, 15.0f)); // Centralizar mapa

                        mMap.addCircle(
                                new CircleOptions()
                                        .center(dest) // Última posição capturada
                                .radius(400.0)
                                .strokeWidth(3f)
                                .strokeColor(Color.RED)
                                .fillColor(Color.argb(70, 150,50, 50))



                        );


                    }



/*
                    // Configurações da PolyLine (linha reta entre pontos)
                    PolylineOptions polylineOptions = new PolylineOptions().
                            geodesic(true).
                            color(Color.BLUE). // Cor
                            width(10); // Espessura

                    // Adicionando os pontos de ligações
                    for (int j = 0; j < points.size(); j++) {
                        polylineOptions.add(points.get(j));
                        //TODO teste
                        Log.d("JSONResult", "pontos " + points.get(j).toString());
                    }

                    Log.d("JSONResult" , "Desenhando");
                    polylinePaths.add(mMap.addPolyline(polylineOptions));
*/


                }catch (NullPointerException e){
                    e.printStackTrace();

                }

                catch (JSONException e){
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                Toast.makeText(MapsActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });


        int socketTimeout = 10000;
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        stringRequest.setRetryPolicy(policy);
        requestQueue.add(stringRequest);

    }


    private String getUrl(LatLng origin, LatLng dest, String directionMode) {
        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

        // Waypoints
        String waypoints = "";
        for(int i=2;i<points.size();i++){
            LatLng point  = (LatLng) points.get(i);
            if(i==2)
                waypoints = "waypoints=";
            waypoints += point.latitude + "," + point.longitude + "|";
        }

        // Mode
        String mode = "mode=" + directionMode;
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + waypoints + "&" + mode;
        // Output format
        String output = "json";
        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&key=" + getString(R.string.google_maps_key);
        return url;
    }

    @Override
    public void onTaskDone(Object... values) {
        if (currentPolyline != null)
            currentPolyline.remove();
        currentPolyline = mMap.addPolyline((PolylineOptions) values[0]);
    }

    private boolean runtime_permissions() {
        if(Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){

            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},100);

            return true;
        }
        return false;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 100){
            if( grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED){
                status = true;
            }else {
                runtime_permissions();
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {

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

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }
}
