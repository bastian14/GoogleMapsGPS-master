package cl.ubiobio.mapa;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
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

public class MapsActivity extends AppCompatActivity implements LocationSource, OnMapReadyCallback,
        GoogleMap.OnMyLocationClickListener, GoogleMap.OnMapLongClickListener, GoogleMap.OnMarkerClickListener,
        GoogleMap.OnInfoWindowClickListener {

    private GoogleMap mMap;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private boolean mPermissionDenied = false;

    private LocationSource.OnLocationChangedListener listener;
    private Location mCurrentLocation;
    private LocationManager mLocationManager;
    private static long UPDATE_INTERVAL_IN_MILLISECONDS;

    private double distanciaFar;
    private double menorDistanciaDefinitiva = 0;

    private ArrayList<Farmacia> farmaciasDeTurno;
    private Farmacia farmaciaCercana;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        farmaciasDeTurno = new ArrayList<>();
        serviceFarmacias();


        /** mLocationManager gestiona las peticiones de posicion **/
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        /** Verificamos si el usuario tiene encendido el GPS, si no,
         * lo enviamos al menú para que lo encienda **/
        boolean enabledGPS = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!enabledGPS) {
            Toast.makeText(this, "No hay señal de GPS", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }

        /** Establecemos el intervalo de actualización de la posicion **/
        UPDATE_INTERVAL_IN_MILLISECONDS = 5000;

        /** Se busca el fragmento del mapa e iniciamos el mapa.
         * cuando el mapa se encuentre listo, se llamará a onMapReady() **/
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()){
                    case R.id.fab:
                        for(int i=0; i <farmaciasDeTurno.size();i++){
                            //esto es nuevo
                            distanciaFar = distancia(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude(),farmaciasDeTurno.get(i).getLatitud(), farmaciasDeTurno.get(i).getLongitud());
                            if(i==0){
                                menorDistanciaDefinitiva = distancia(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude(),farmaciasDeTurno.get(i).getLatitud(), farmaciasDeTurno.get(i).getLongitud());
                            }

                            if (distanciaFar < menorDistanciaDefinitiva) {
                                menorDistanciaDefinitiva = distancia(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude(),farmaciasDeTurno.get(i).getLatitud(), farmaciasDeTurno.get(i).getLongitud());
                                farmaciaCercana = farmaciasDeTurno.get(i); //datos farmacia mas cercana

                            }
                        }
                        LatLng desti = new LatLng(farmaciaCercana.getLatitud(), farmaciaCercana.getLongitud());

                        /** en este caso, capturamos la posicion del marcador y la posicion actual del telefono
                         * e iniciamos el proceso de trazar una ruta **/
                        LatLng origen = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
                        String url = obtenerDireccionesURL(origen,desti); //funcion para generar la URL para solicitar la ruta

                        /** Tarea Async para descargar la ruta **/
                        DownloadTask downloadTask = new DownloadTask();
                        downloadTask.execute(url);

                        mMap.addMarker(new MarkerOptions().position(desti));

                        break;
                }
            }
        });



    }

    /** gestionamos la respuesta de la petición de permisos **/
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            /** si el permiso fue aceptado, iniciamos el proceso de captura de posiciones **/
            enableMyLocation();
        } else {
            mPermissionDenied = true;
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (mPermissionDenied) {
            showMissingPermissionError();
            mPermissionDenied = false;
        }
    }

    /** Si realizamos un click sobre su posicion (punto azul), mostraremos información acerca de ese punto **/
    @Override
    public void onMyLocationClick(@NonNull Location location) {
        Toast.makeText(this, "Mi posición actual:\n" + location, Toast.LENGTH_LONG).show();
    }

    /** Dialogo de error para cuando no se acepte el permiso **/
    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }

    /** el mapa se encuentra listo, podemos modificar algunas configuraciones **/
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        /** Activacion de controles en el mapa **/
        mMap.getUiSettings().setZoomControlsEnabled(true); //control de zoom
        mMap.getUiSettings().setMyLocationButtonEnabled(true); //habilitamos boton para regresar a su posicion
        mMap.getUiSettings().setCompassEnabled(true); //el mapa busca el norte

        /** Tipo de terreno que se muestra en el mapa **/
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        // googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        // googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        // googleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        // googleMap.setMapType(GoogleMap.MAP_TYPE_NONE);

        /** Gestión de algunos eventos**/
        mMap.setOnMyLocationClickListener(this); //click sobre posicion
        mMap.setOnMapLongClickListener(this); //click largo en el mapa
        mMap.setOnMarkerClickListener(this); //click sobre marcador
        mMap.setOnInfoWindowClickListener(this); //click sobre la informacion de un marcador

        /** iniciamos el proceso de captura de posiciones **/
        enableMyLocation();
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            /** Si el permiso no fue concedido o no ha sido solicitado, se solicita **/
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (mMap != null) {
            /** Cuando ya tenemos los permisos
             * le decimos al mapa que capture la posicion y
             * modificamos de donde se obtiene la posición,
             * con el objetivo de contralar como y cuando se actualiza **/
            mMap.setMyLocationEnabled(true);
            mMap.setLocationSource(this);

            /** Se le dice de donde se captura la posicion, en este caso el GPS(puede ser desde internet),
             * el intervalo de actualizacion,
             * la distancia minima que debe modificar la posicion para ser requerida una actualizacion
             * y el evento que capura la posicion**/
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, UPDATE_INTERVAL_IN_MILLISECONDS, 10, new android.location.LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    /** cuando se captura una nueva posicion, se le entrega al evento que fue seteado en el mapa
                     * para que sea consiente se su posicion.
                     * En caso de necesitar tracking de posicion, en este punto se debe el iniciar el SW de trackeo **/
                    MapsActivity.this.listener.onLocationChanged(location);
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            });

            /** el mapa es consciente de la posción, pero necesitamos entregarle
             * la primera posicion al mapa para que cambie la vista entregada, siempre
             * y cuando, el telefono haya registrado su posicion antes. **/
            mCurrentLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (mCurrentLocation != null) {
                this.listener.onLocationChanged(mCurrentLocation);
                /** el objeto Location no es compatible con el mapa, por lo cual debemos crear un objeto
                 * compatible con este (LatLng) **/
                LatLng thisLocation = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
                /** movemos el mapa a la posicion obtenida **/
                mMap.moveCamera(CameraUpdateFactory.newLatLng(thisLocation));
                /** y le indicamos que establezca un zoom 16, entre mayor sea mas cerca se mostrará **/
                mMap.animateCamera(CameraUpdateFactory.zoomTo(16));
            }

        }

    }

    /** se inicializa el evento que captura las posiciones para el mapa **/
    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        this.listener = onLocationChangedListener;
    }

    @Override
    public void deactivate() {
        this.listener = null;
    }

    /** cuando se realiza un click prologando (poco más de un segundo),
     * se activará este evento, el cual nos entregará la posición donde se realizo el click prolongado
     * y creará un margador con la información dispuesta a continuación y borrará otros elementos
     * existentes **/
    @Override
    public void onMapLongClick(LatLng latLng) {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Haga click aqui para trazar ruta");
        mMap.clear();
        mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.addMarker(markerOptions);
    }

    /** Evento que se activa al realizar click sobre un marcador **/
    @Override
    public boolean onMarkerClick(Marker marker) {
        Log.d("click", "click en marker");
        return false;
    }

    private void generateToast(String msg){
        Toast.makeText(getApplicationContext(),msg, Toast.LENGTH_SHORT).show();
    }

    private void serviceFarmacias(){
        Log.d("LOG WS", "entre");
        String WS_URL = "http://farmanet.minsal.cl/index.php/ws/getLocalesTurnos";
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        StringRequest request = new StringRequest(
                Request.Method.GET,
                WS_URL,
                new Response.Listener<String>(){
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONArray responseJson = new JSONArray(response);

                            for(int i = 0; i < responseJson.length(); i++){
                                JSONObject o = responseJson.getJSONObject(i);
                                Farmacia far = new Farmacia();

                                far.setFecha(o.getString("fecha"));
                                far.setLocal_id(o.getString("local_id"));
                                far.setRegion_id(o.getString("fk_region"));
                                far.setComuna_id(o.getString("fk_comuna"));
                                far.setLocalidad_id(o.getString("fk_localidad"));
                                far.setNombre_farmacia(o.getString("local_nombre"));
                                far.setNombre_comuna(o.getString("comuna_nombre"));
                                far.setDireccion_farmacia(o.getString("local_direccion"));
                                far.setHorario_apertura(o.getString("funcionamiento_hora_apertura"));
                                far.setHorario_cierre(o.getString("funcionamiento_hora_cierre"));
                                far.setTelefono(o.getString("local_telefono"));
                                String lat = o.getString("local_lat");
                                String lng = o.getString("local_lng");


                                try{
                                    far.setLatitud(Double.parseDouble(lat));
                                    far.setLongitud(Double.parseDouble(lng));



                                }catch (NumberFormatException e){
                                    far.setLatitud(0);
                                    far.setLongitud(0);
                                }

                                farmaciasDeTurno.add(far);

                            }

                            Log.d("LOG", "cantidad: " + farmaciasDeTurno.size());

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("LOG WS", error.toString());
                generateToast("Error en el WEB Service");
            }
        }
        );
        requestQueue.add(request);
    }


    private double distancia(double lat1, double lng1, double lat2, double lng2){
        double R = 6378.137;
        double dLat  = rad( lat2 - lat1 );
        double dLong = rad( lng2 - lng1 );

        double a = Math.sin(dLat/2) * Math.sin(dLat/2) + Math.cos(rad(lat1)) * Math.cos(rad(lat2)) * Math.sin(dLong/2) * Math.sin(dLong/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double d = R * c;

        return d;
    }

    private double rad(double data){
        return data * Math.PI/180;
    }

    /** evento que se activa al realizar click sobre la información desplegada por un marcador **/
    @Override
    public void onInfoWindowClick(Marker marker) {
        Log.d("click", "click en info");

        /** en este caso, capturamos la posicion del marcador y la posicion actual del telefono
         * e iniciamos el proceso de trazar una ruta **/
        LatLng origen = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
        String url = obtenerDireccionesURL(origen, marker.getPosition()); //funcion para generar la URL para solicitar la ruta

        /** Tarea Async para descargar la ruta **/
        DownloadTask downloadTask = new DownloadTask();
        downloadTask.execute(url);
    }

    /** Genera un string con la URL de solicitud de ruta **/
    private String obtenerDireccionesURL(LatLng origin, LatLng dest) {
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        String sensor = "sensor=false";
        String key = "key=" + getString(R.string.google_maps_key);
        String parameters = str_origin + "&" + str_dest + "&" + sensor + "&" + key;
        String output = "json";
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;
        return url;
    }

    /** Obtiene string de datos obtenidos desde el servicio web de rutas**/
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);
            // Creamos una conexion http
            urlConnection = (HttpURLConnection) url.openConnection();
            // Conectamos
            urlConnection.connect();
            // Leemos desde URL
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

    /** clase que crea una tarea async para descargar la ruta en una hilo independiente del procesador **/
    private class DownloadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... url) {

            String data = "";
            try {
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("ERROR", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();
            parserTask.execute(result);
        }
    }

    /** parser para que obtiene los datos necesarios para crear un objeto Polyline para el mapa **/
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

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
            ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null;
            MarkerOptions markerOptions = new MarkerOptions();

            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<LatLng>();
                lineOptions = new PolylineOptions();

                List<HashMap<String, String>> path = result.get(i);

                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                lineOptions.addAll(points);
                lineOptions.width(4);
                lineOptions.color(Color.rgb(0, 0, 255));
            }
            if (lineOptions != null) {
                Log.d("ssss", "ruta");
                mMap.addPolyline(lineOptions);
            }
        }
    }

    public class DirectionsJSONParser {

        public List<List<HashMap<String, String>>> parse(JSONObject jObject) {

            List<List<HashMap<String, String>>> routes = new ArrayList<List<HashMap<String, String>>>();
            JSONArray jRoutes = null;
            JSONArray jLegs = null;
            JSONArray jSteps = null;

            try {

                jRoutes = jObject.getJSONArray("routes");

                for (int i = 0; i < jRoutes.length(); i++) {
                    jLegs = ((JSONObject) jRoutes.get(i)).getJSONArray("legs");
                    List path = new ArrayList<HashMap<String, String>>();

                    for (int j = 0; j < jLegs.length(); j++) {
                        jSteps = ((JSONObject) jLegs.get(j)).getJSONArray("steps");

                        for (int k = 0; k < jSteps.length(); k++) {
                            String polyline = "";
                            polyline = (String) ((JSONObject) ((JSONObject) jSteps.get(k)).get("polyline")).get("points");
                            List<LatLng> list = decodePoly(polyline);

                            for (int l = 0; l < list.size(); l++) {
                                HashMap<String, String> hm = new HashMap<String, String>();
                                hm.put("lat", Double.toString(((LatLng) list.get(l)).latitude));
                                hm.put("lng", Double.toString(((LatLng) list.get(l)).longitude));
                                path.add(hm);
                            }
                        }
                        routes.add(path);
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            } catch (Exception e) {
            }

            return routes;
        }

        private List<LatLng> decodePoly(String encoded) {

            List<LatLng> poly = new ArrayList<LatLng>();
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


    }
}
