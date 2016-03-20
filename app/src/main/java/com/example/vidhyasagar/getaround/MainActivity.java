package com.example.vidhyasagar.getaround;

import android.app.ActionBar;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.SystemRequirementsChecker;
import com.example.vidhyasagar.Description;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {


    ListView listExhibits;
    ArrayList<String[]> arrayList;
    ArrayList<String> namesOfListView;
    ArrayAdapter listAdapter;
    private BeaconManager beaconManager;
    private Region region;

    private static final Map<String, List<String>> PLACES_BY_BEACONS;

    static {
        Map<String, List<String>> placesByBeacons = new HashMap<>();
        placesByBeacons.put("24028:20615", new ArrayList<String>() {{
            add("Heavenly Sandwiches");
            // read as: "Heavenly Sandwiches" is closest
            // to the beacon with major 22504 and minor 48827
            add("Green & Green Salads");
            // "Green & Green Salads" is the next closest
            add("Mini Panini");
            // "Mini Panini" is the furthest away
        }});
        placesByBeacons.put("64904:53347", new ArrayList<String>() {{
            add("Mini Panini");
            add("Green & Green Salads");
            add("Heavenly Sandwiches");
        }});
        placesByBeacons.put("59932:55122", new ArrayList<String>() {{
            add("Subway");
            add("Burrito Boyz");
            add("Mozy's Shawarma");
        }});

        PLACES_BY_BEACONS = Collections.unmodifiableMap(placesByBeacons);
    }

    //HELPER METHODS
    public String loadJSONFromAsset(String filename) {
        String json = null;
        try {
            InputStream is = getAssets().open(filename); // open data file for parsing

            int size = is.available();
            byte[] buffer = new byte[size];

            is.read(buffer);

            is.close();

            json = new String(buffer, "UTF-8");

        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    public ArrayList<JSONObject> getBeaconDetails(List<Beacon> beacons){
        ArrayList<JSONObject> jObs = new ArrayList();
        try {
            JSONObject jObject = new JSONObject(loadJSONFromAsset("data.json")); // read json file from assets directory

            JSONArray beaconArray = new JSONArray(jObject.getString("beacons")); // get all beacon objects from json string

            for (int i = 0; i < beacons.size(); i++) {
                for (int j = 0; j < beaconArray.length(); j ++) {
                    JSONObject temp = beaconArray.getJSONObject(j);
                    if (Integer.parseInt(temp.getString("major")) == (beacons.get(i).getMajor()) &&
                            Integer.parseInt(temp.getString("minor")) == (beacons.get(i).getMinor())){ // found beacon matching
                        Log.i("success",temp.getString("major") + ", " + beacons.get(i).getMajor() );
                        jObs.add(temp);
                        break;
                    }
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jObs;
    }

    private List<String> placesNearBeacon(Beacon beacon) {
        String beaconKey = String.format("%d:%d", beacon.getMajor(), beacon.getMinor());
        if (PLACES_BY_BEACONS.containsKey(beaconKey)) {
            return PLACES_BY_BEACONS.get(beaconKey);
        }
        return Collections.emptyList();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.activity_main);


        arrayList = new ArrayList();
        namesOfListView = new ArrayList();
        namesOfListView.add("Nearby Exhibits...");
        arrayList.add(new String[] {"IGNORE", "THIS", "STRING"});
        listExhibits = (ListView) findViewById(R.id.listExhibits);

        listAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, namesOfListView);

        listExhibits.setAdapter(listAdapter);

        listExhibits.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(position != 0) {
                    Intent description = new Intent(getApplicationContext(), Description.class);
                    description.putExtra("identifier", arrayList.get(position)[0]);
                    description.putExtra("description", arrayList.get(position)[1]);
                    description.putExtra("url", arrayList.get(position)[2]);
                    description.putExtra("image", arrayList.get(position)[3]);
                    startActivity(description);

                }
            }
        });

        beaconManager = new BeaconManager(getApplicationContext());

        region = new Region("ranged region",
                UUID.fromString("B9407F30-F5F8-466E-AFF9-25556B57FE6D"), null, null);

        beaconManager.setMonitoringListener(new BeaconManager.MonitoringListener() {
            @Override
            public void onEnteredRegion(Region region, List<Beacon> list) {
                Log.i("INFO", region.toString());
                showNotification(
                        "Your gate closes in 47 minutes.",
                        "Current security wait time is 15 minutes, "
                                + "and it's a 5 minute walk from security to the gate. "
                                + "Looks like you've got plenty of time!");
            }

            @Override
            public void onExitedRegion(Region region) {
                showNotification("Hello", "Exiting region");
                // could add an "exit" notification too if you want (-:
            }
        });


        beaconManager.setRangingListener(new BeaconManager.RangingListener() {
            @Override
            public void onBeaconsDiscovered(Region region, List<Beacon> list) {
                if (!list.isEmpty()) {

                    Beacon nearestBeacon = list.get(0);
                    ArrayList<JSONObject> beaconInfo = getBeaconDetails(list);

                    for(int i = 0; i < beaconInfo.size(); i++) {
                        try {
                            if (!namesOfListView.contains(beaconInfo.get(i).getString("name")))
                            {
                                    namesOfListView.add(beaconInfo.get(i).getString("name"));
                                    arrayList.add(new String[] {beaconInfo.get(i).getString("name"),
                                            beaconInfo.get(i).getString("description"),
                                            beaconInfo.get(i).getString("url"),
                                            beaconInfo.get(i).getString("image")});
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
//                    namesOfListView.clear();
//                    for(String[] each : arrayList) {
//                        namesOfListView.add(each[0]);
//                    }

                    listAdapter.notifyDataSetChanged();

                    List<String> places = placesNearBeacon(nearestBeacon);
                    // TODO: update the UI here
                    Log.d("Airport", "Nearest places: " + places);
                }
            }
        });

        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                beaconManager.startMonitoring(new Region(
                        "monitored region",
                        UUID.fromString("B9407F30-F5F8-466E-AFF9-25556B57FE6D"),
                        null, null));
            }
        });

    }

    public void showNotification(String title, String message) {
        Intent notifyIntent = new Intent(this, MainActivity.class);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivities(this, 0,
                new Intent[] { notifyIntent }, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build();
        notification.defaults |= Notification.DEFAULT_SOUND;
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, notification);
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
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        SystemRequirementsChecker.checkWithDefaultDialogs(this);

        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                beaconManager.startRanging(region);
            }
        });
    }

    @Override
    protected void onPause() {
        beaconManager.stopRanging(region);

        super.onPause();
    }
}
