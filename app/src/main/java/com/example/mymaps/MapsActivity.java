// Importerer nødvendige klasser og grensesnitt for applikasjonen
package com.example.mymaps;

import android.content.DialogInterface;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.example.mymaps.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;

// Deklarasjon av MapsActivity-klassen, utvider FragmentActivity og implementerer OnMapReadyCallback og GoogleMap.OnMapClickListener
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener {
    // Deklarasjon av et GoogleMap-objekt
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setter opp visningsbinding for aktiviteten
        com.example.mymaps.databinding.ActivityMapsBinding binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialiserer kartfragmentet og setter denne klassen som lytter for tilbakeringing
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        // Tildeler GoogleMap-objektet når det er klart
        mMap = googleMap;

        // Aktiverer zoomkontroller på kartet
        mMap.getUiSettings().setZoomControlsEnabled(true);
        // Setter en klikkelytter for kartklikk
        mMap.setOnMapClickListener(this);
        // Setter en markørklikkelytter for å vise en informasjonsdialog når en markør klikkes
        mMap.setOnMarkerClickListener(marker -> {
            String markerInfo = (String) marker.getTag();
            // Vis informasjonen. Her bruker vi en AlertDialog.
            new AlertDialog.Builder(MapsActivity.this)
                    .setTitle(marker.getTitle())
                    .setMessage(markerInfo)
                    .setPositiveButton("OK", null)
                    .show();
            return true;
        });

        updateMapMarkers();

        // Opprett en Handler for å kjøre updateMapMarkers regelmessig
        final Handler handler = new Handler();
        final Runnable updateMarkers = new Runnable() {
            @Override
            public void run() {
                updateMapMarkers(); // Henter nye data og oppdaterer kartet
                handler.postDelayed(this, 2000); // Planlegger neste oppdatering etter 60 sekunder
            }
        };

        // Starter den første oppdateringen
        handler.postDelayed(updateMarkers, 2000);
    }

    @Override
    public void onMapClick(LatLng latLng) {
        // Håndterer et kartklikk ved å legge til en markør på det klikkede stedet
        Marker marker = mMap.addMarker(new MarkerOptions().position(latLng).title("Valgt Posisjon"));

        // Initierer prosessen for å sette adressen for denne markøren basert på dens plassering
        setAddressFromLocation(latLng, marker);
    }

    private void showInformationDialog(LatLng latLng, Marker marker) {
        // Metode for å vise en dialog for å legge inn informasjon om et nytt sted
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Legg til nytt sted");

        // Setter opp et tilpasset oppsett for dialogen
        final View customLayout = getLayoutInflater().inflate(R.layout.dialog, null);
        builder.setView(customLayout);

        // Initialiserer og setter opp EditText-felt for koordinater og adresse
        EditText editTextCoordinates = customLayout.findViewById(R.id.editTextCoordinates);
        editTextCoordinates.setText(latLng.latitude + ", " + latLng.longitude);
        String gps_koordinater = editTextCoordinates.getText().toString();

        EditText editTextAddress = customLayout.findViewById(R.id.editTextAddress);
        editTextAddress.setText(marker.getSnippet());
        String gateadresse = editTextAddress.getText().toString();

        // Legger til OK- og Avbryt-knapper i dialogen
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Håndterer OK-knappen for å hente og behandle den inngitte informasjonen
                EditText editTextName = customLayout.findViewById(R.id.editTextName);
                String stedsNavn = editTextName.getText().toString();
                EditText editTextDescription = customLayout.findViewById(R.id.editTextDescription);
                String beskrivelse = editTextDescription.getText().toString();

                // Oppdaterer markørens tittel med det nye navnet og potensielt sender data til en server
                marker.setTitle(stedsNavn);
                new SendPostRequest().execute("https://dave3600.cs.oslomet.no/~s199219/jsonin.php", stedsNavn, beskrivelse, gateadresse, gps_koordinater);

            }
        });
        builder.setNegativeButton("Avbryt", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Fjerner markøren hvis brukeren avbryter handlingen
                marker.remove();
            }
        });

        // Viser dialogen
        AlertDialog dialog = builder.create();
        dialog.show();


    }

    private void updateMapMarkers() {
        new FetchDataTask().execute();
    }

    private void setAddressFromLocation(LatLng latLng, Marker marker) {
        new AsyncTask<LatLng, Void, String>() {
            @Override
            protected String doInBackground(LatLng... latLngs) {
                // Bruker Geocoder for å oversette LatLng til en fysisk adresse
                Geocoder geocoder = new Geocoder(MapsActivity.this, Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocation(latLngs[0].latitude, latLngs[0].longitude, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        // Returnerer den første adresselinjen
                        return address.getAddressLine(0);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(String address) {
                // Utføres etter at Geocoder har returnert en adresse
                if (address != null) {
                    // Setter adressen som et 'snippet' for markøren
                    marker.setSnippet(address);
                    // Viser informasjonsdialogen med adressen
                    showInformationDialog(latLng, marker);
                } else {
                    // Håndterer situasjonen der ingen adresse ble funnet
                    // Kan fortsatt kalle showInformationDialog, men uten en adresse
                    showInformationDialog(latLng, marker);
                }
            }
            // Utfører AsyncTask med den aktuelle LatLng
        }.execute(latLng);
    }

    // Klassen FetchDataTask utvider AsyncTask for å utføre nettverksoperasjoner i bakgrunnen
    public class FetchDataTask extends AsyncTask<Void, Void, String> {
        // doInBackground-metoden kjøres på en separat tråd for å unngå å blokkere hovedtråden
        protected String doInBackground(Void... voids) {
            try {
                // Oppretter en URL-tilkobling til en spesifisert serveradresse
                URL url = new URL("https://dave3600.cs.oslomet.no/~s199219/jsonout.php");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                try {
                    // Leser data fra serveren ved hjelp av InputStream og BufferedReader
                    InputStream in = new BufferedInputStream(con.getInputStream());
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder result = new StringBuilder();
                    String line;
                    // Leser hver linje fra bufferen og legger den til i en StringBuilder
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    // Returnerer resultatet som en streng
                    return result.toString();
                } finally {
                    // Lukker tilkoblingen til serveren
                    con.disconnect();
                }
            } catch (Exception e) {
                // Fanger opp eventuelle unntak og skriver dem ut i loggen
                e.printStackTrace();
                return null;
            }
        }

        @Override
        // onPostExecute-metoden kjøres på hovedtråden etter at bakgrunnsoperasjonen er ferdig
        protected void onPostExecute(String result) {
            // Fjerner alle eksisterende markører fra kartet
            mMap.clear();
            // Behandler JSON-resultatet her
            // Parser JSON og legger til markører på kartet
            try {
                // Konverterer strengen til et JSONArray-objekt
                JSONArray jsonArray = new JSONArray(result);
                for (int i = 0; i < jsonArray.length(); i++) {
                    // Henter ut hvert JSONObject i JSONArray
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    // Ekstraherer informasjon fra hvert JSONObject
                    String gpsKoordinater = jsonObject.getString("gps_koordinater");
                    String[] latLng = gpsKoordinater.split(", ");
                    double lat = Double.parseDouble(latLng[0]);
                    double lng = Double.parseDouble(latLng[1]);
                    String title = jsonObject.getString("navn");
                    String description = jsonObject.getString("beskrivelse");
                    String address = jsonObject.getString("gateadresse");

                    // Oppretter en ny LatLng-instans og legger til en markør på kartet
                    LatLng location = new LatLng(lat, lng);
                    Marker marker = mMap.addMarker(new MarkerOptions().position(location).title(title));
                    // Setter sammen informasjon for markøren
                    String markerInfo = "Navn: " + title + "\nBeskrivelse: " + description + "\nAdresse: " + address + "\nGPS Koordinater: " + gpsKoordinater;
                    // Lagrer denne informasjonen i markøren
                    marker.setTag(markerInfo);
                }
            } catch (JSONException e) {
                // Fanger og logger eventuelle JSON-unntak
                e.printStackTrace();
            }
        }
    }
}