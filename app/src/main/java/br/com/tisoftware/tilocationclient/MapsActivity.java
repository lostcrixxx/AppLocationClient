package br.com.tisoftware.tilocationclient;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
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
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Double.parseDouble;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    public static final String URL="http://tilocationmobile.atspace.cc/location.php";
    private JSONArray pontos;
    public List<LatLng> points = new ArrayList<>();


    private List<Polyline> polylinePaths = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Exibir o mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


    }



    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        // Tipo de mapa
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

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
                                        .position(new LatLng(parseDouble(lat_i) , parseDouble(long_i)))
                                //.title(Double.valueOf(lat_i).toString() + "," + Double.valueOf(long_i).toString())
                                //.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE))
                        );


                        // Novo Array para desenhar a Polyline
                        points.add(new LatLng(parseDouble(lat_i), parseDouble(long_i)));

                        /*
                        String rota ="";
                        rota = lat_i + ", " + long_i;
                        Log.d("JSONResult" , "rota " + rota);

                        Geocoder coder = new Geocoder(MapsActivity.this);
                        List<Address> address;
                        LatLng p1 = null;

                        try {
                            address = coder.getFromLocationName(rota, 1);
                            if (address == null) {
                                Log.d("JSONResult" , "Adress vazio");
                            }
                            Log.d("JSONResult" , "Adress" + address.get(1).toString());
                            Address location = address.get(1);
                            location.getLatitude();
                            location.getLongitude();

                            p1 = new LatLng(location.getLatitude(), location.getLongitude() );
                            points.add(new LatLng(parseDouble(lat_i), parseDouble(long_i)));
                            //points.add(new LatLng(parseDouble(lat_i), parseDouble(long_i)));
                            Log.d("JSONResult" , "ok");

                        } catch (Exception ex) {
                            Log.d("JSONResult" , "erro");
                            ex.printStackTrace();

                        }

*/

                        PolylineOptions polylineOptions = new PolylineOptions().
                                geodesic(true).
                                color(Color.BLUE).
                                width(10);

                        for (int j = 0; j < points.size(); j++) {
                            polylineOptions.add(points.get(j));
                            //TODO teste
                            Log.d("JSONResult", "pontos " + points.get(j).toString());
                        }

                        Log.d("JSONResult" , "Desenhando");
                        polylinePaths.add(mMap.addPolyline(polylineOptions));


                        // TODO centralizar o mapa com os Points
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-23.40888125,-46.75347317), 10.0f));
                    }

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

    private List decodePolyline(String encoded) {

        List poly = new ArrayList();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
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
