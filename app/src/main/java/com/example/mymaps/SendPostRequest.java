package com.example.mymaps;

import android.net.Uri;
import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class SendPostRequest extends AsyncTask<String, Void, String> {
    protected String doInBackground(String... params) {
        try {
            URL url = new URL(params[0]); // URL til PHP-skriptet
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            String data = Uri.encode("navn", "UTF-8")
                    + "=" + Uri.encode(params[1], "UTF-8")
                    + "&" + Uri.encode("beskrivelse", "UTF-8")
                    + "=" + Uri.encode(params[2], "UTF-8")
                    + "&" + Uri.encode("gateadresse", "UTF-8")
                    + "=" + Uri.encode(params[3], "UTF-8")
                    + "&" + Uri.encode("gps_koordinater", "UTF-8")
                    + "=" + Uri.encode(params[4], "UTF-8");
            writer.write(data);

            writer.flush();
            writer.close();
            os.close();

            int responseCode=conn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in=new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuffer sb = new StringBuffer("");
                String line="";

                while((line = in.readLine()) != null) {
                    sb.append(line);
                    break;
                }

                in.close();
                return sb.toString();
            }
            else {
                return "false : " + responseCode;
            }
        }
        catch(Exception e){
            return "Exception: " + e.getMessage();
        }
    }

    @Override
    protected void onPostExecute(String result) {
        // Håndter respons fra serveren her
    }
}
