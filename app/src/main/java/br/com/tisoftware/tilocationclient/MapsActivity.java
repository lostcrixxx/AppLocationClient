package br.com.tisoftware.tilocationclient;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
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

import static java.lang.Double.parseDouble;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    public static final String URL="http://tilocationmobile.atspace.cc/location.php";
    private JSONArray pontos;
    public List<LatLng> points = new ArrayList<>();
    double fromLat, fromLon, toLat, toLon;


    private List<Polyline> polylinePaths = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Exibir o mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        carregaMapa();

    }

    public void carregaMapa() { // este metodo aqui foi explicado no exemplo do post anterior
        new Thread() {
            @Override
            public void run() {
                try {
                    buscarCoordenadasEndereco("Rua GUJ, 12 - Curitiba", "Rua Java, 10 - Curitiba");// esta é a chamada para o metodo que vai traduzir o endereço para coordenadas é //a duvida descrita, é util para trabalhar com endereços que o usuário digitar
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //       String url = RoadProvider
         //               .getUrl(fromLat, fromLon, toLat, toLon);
         //       InputStream is = getConnection(url);
         //       mRoad = RoadProvider.getRoute(is);
         //       mHandler.sendEmptyMessage(0);
            }
        }.start();
        //mapView.invalidate();
    }
    public void buscarCoordenadasEndereco(String enderecoOrigem, String enderecoDestino) throws IOException {
        Geocoder geoCoder = new Geocoder(this, Locale.getDefault());// esse Geocoder aqui é quem vai traduzir o endereço de String para coordenadas double
        List<Address> addresses = null;//este Adress aqui recebe um retorno do metodo geoCoder.getFromLocationName vc manipula este retorno pra pega as coordenadas
        addresses = geoCoder.getFromLocationName(enderecoOrigem, 1);// o numero um aqui é a quantidade maxima de resultados que vc quer receber
        fromLat = addresses.get(0).getLatitude();
        fromLon = addresses.get(0).getLongitude();
        addresses = geoCoder.getFromLocationName(enderecoDestino, 1);
        toLat = addresses.get(0).getLatitude();
        toLon = addresses.get(0).getLongitude();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL); // Tipo de mapa

        // TODO centralizar o mapa com os Points
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-23.40888125,-46.75347317), 10.0f)); // Centralizar mapa

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

                    // Directions
                    if (points.size() >= 2) {
                        LatLng origin = (LatLng) points.get(0); // Pirmeira posição
                        LatLng dest = (LatLng) points.get(6); // TODO Pegar a última posição (points.size-1)

                        // Getting URL to the Google Directions API
                        String url = getDirectionsUrl(origin, dest);

                        DownloadTask downloadTask = new DownloadTask();

                        // Start downloading json data from Google Directions API
                        downloadTask.execute(url);
                    }




                    /*
                    // Configurações da PolyLine
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

    private class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {

            String data = "";

            try {
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }
    }

    /**
     * A class to parse the Google Places in JSON format
     */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList points = null;
            PolylineOptions lineOptions = null;
            MarkerOptions markerOptions = new MarkerOptions();

            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList();
                lineOptions = new PolylineOptions();

                List<HashMap<String, String>> path = result.get(i);

                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                Log.i("JSONResult","rota");
                lineOptions.addAll(points);
                lineOptions.width(12);
                lineOptions.color(Color.RED);
                lineOptions.geodesic(true);

            }

// Drawing polyline in the Google Map for the i-th route
            mMap.addPolyline(lineOptions);
        }
    }

    private String getDirectionsUrl(LatLng origin, LatLng dest) {

        Log.i("JSONResult","entrou directions");

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

        // Sensor enabled
        //String sensor = "sensor=false";
        //String mode = "mode=driving";
        // Building the parameters to the web service
        //String parameters = str_origin + "&" + str_dest + "&" + sensor + "&" + mode;
        String parameters = str_origin + "&" + str_dest;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&key=AIzaSyDH0elODdv85HtYSH6Xai2Npc9qHCWIEuQ";


        return url;
    }

    /**
     * A method to download json data from url
     */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            java.net.URL url = new URL(strUrl);

            urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.connect();

            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
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
