package com.example.vivek.ghr;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.widget.Button;
import android.widget.ImageView;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    //Declare inside class
    Button btnCamera, btnLoadImage, btnUpload, btnShare;
    ImageView iv;
    EditText etImageName;
    Bitmap bm;
    String uploadUrl = "https://viveksohal92.000webhostapp.com/uploads/uploadimage.php";
    GoogleApiClient gac;
    Location loc;
    double lat, lon;
    String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Bind inside onCreate()
        btnCamera = (Button) findViewById(R.id.btnCamera);
        btnLoadImage = (Button) findViewById(R.id.btnLoadImage);
        iv = (ImageView) findViewById(R.id.iv);
        btnUpload = (Button) findViewById(R.id.btnUpload);
        etImageName = (EditText) findViewById(R.id.etImageName);
        btnShare = (Button) findViewById(R.id.btnShare);

        GoogleApiClient.Builder builder = new GoogleApiClient.Builder(this);
        builder.addApi(LocationServices.API);
        builder.addConnectionCallbacks(this);
        builder.addOnConnectionFailedListener(this);
        gac = builder.build();

        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Opens camera
                Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(i, 100);
            }
        });

        btnLoadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, 101);
            }
        });

        btnUpload.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                //Convert Bitmap data of image into String
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bm.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
                byte[] imageBytes = byteArrayOutputStream.toByteArray();
                final String image = Base64.encodeToString(imageBytes, Base64.DEFAULT);
                final ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
                progressDialog.setTitle("Uploading");
                progressDialog.setMessage("Please wait...");
                progressDialog.show();

                //Uploading image to uploads folder
                StringRequest stringRequest = new StringRequest(Request.Method.POST, uploadUrl, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            String resp = jsonObject.getString("response");
                            Toast.makeText(MainActivity.this, resp, Toast.LENGTH_SHORT).show();
                            /*iv.setImageResource(0);
                            iv.setVisibility(View.GONE);*/
                            etImageName.setText("");
                            etImageName.setVisibility(View.GONE);
                            //btnUpload.setVisibility(View.GONE);
                            progressDialog.cancel();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
                    }
                }) {

                    @Override
                    protected Map<String, String> getParams() throws AuthFailureError {
                        Map<String, String> params = new HashMap<>();
                        //image, name, lat and lon are keys which will be used in php script
                        params.put("image", image);
                        params.put("name", etImageName.getText().toString());
                        params.put("lat", String.valueOf(lat));
                        params.put("lon", String.valueOf(lon));
                        return params;
                    }
                };

                MySingleton.getInstance(MainActivity.this).addToRequestQue(stringRequest);

            }
        });

        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_SEND);
                String imageName = etImageName.getText().toString();
                i.putExtra(Intent.EXTRA_STREAM, Uri.parse(url));
                i.putExtra(Intent.EXTRA_TEXT, imageName);
                i.setType("image/*");
                startActivity(i);
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == 100) {
            //Get Bitmap data of image
            ContentResolver c = getContentResolver();
            bm = (Bitmap) data.getExtras().get("data");
            iv.setVisibility(View.VISIBLE);
            iv.setImageBitmap(bm);
            url = MediaStore.Images.Media.insertImage(c, bm, "image", "description");
            etImageName.setVisibility(View.VISIBLE);
            btnUpload.setVisibility(View.VISIBLE);
        }

        if (resultCode == RESULT_OK && requestCode == 101 && null != data) {
            ContentResolver c = getContentResolver();
            Cursor cursor = c.query(data.getData(), null, null, null, null);
            cursor.moveToFirst();
            String ppath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            cursor.close();
            bm = BitmapFactory.decodeFile(ppath);
            iv.setVisibility(View.VISIBLE);
            iv.setImageBitmap(bm);
            url = MediaStore.Images.Media.insertImage(c, bm, "image", "description");
            etImageName.setVisibility(View.VISIBLE);
            btnUpload.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public void onConnected(Bundle bundle) {

        loc = LocationServices.FusedLocationApi.getLastLocation(gac);
        if (loc != null) {
            lat = loc.getLatitude();
            lon = loc.getLongitude();
        } else {
            Toast.makeText(this, "Please enable GPS", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this, "Connection suspended", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this, "Connection failed", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (gac != null) gac.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (gac != null) gac.disconnect();
    }
}
