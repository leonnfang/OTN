package com.example.otn;

import android.Manifest;
import android.app.Instrumentation;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import org.json.JSONObject;

import java.io.IOException;

public class qr_scanner extends AppCompatActivity {
    private DBConnection dbConnection;

    private SurfaceView surfaceView;
    private CameraSource cameraSource;
    private TextView textView;
    protected BarcodeDetector barcodeDetector;

    private String TAG = "QR SCANNER";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        setContentView(R.layout.fragment_bike);
        surfaceView = (SurfaceView)findViewById(R.id.qrCode);
        textView = (TextView)findViewById(R.id.text_bike);

        barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE).build();
        if(!barcodeDetector.isOperational()){
            textView.setText("Could not set up the detector!");
            Log.d(TAG, "barcodeDetector set up failed");
            return;
        }

        cameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setRequestedPreviewSize(640,480).build();

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                // Check permission
                if (ActivityCompat.checkSelfPermission( getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                    Log.d(TAG, "camera permission denied");
                    return;
                }
                try {
                    cameraSource.start(holder);
                } catch (IOException e) {
                    Log.d(TAG, e.toString());
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {

            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                SparseArray<Barcode> qrCodes = detections.getDetectedItems();
                if (qrCodes.size() != 0)
                {
                    int result = dbCheck(qrCodes.valueAt(0));
                    if (result == -1) {
                        Log.d(TAG, "did not find bike in DB");
                        textView.setText("DID NOT FIND THIS BIKE IN DB");
                        return;
                    } else if (result == 0) {
                        Log.d(TAG, "Find bike in db but it is in use");
                        textView.setText("Bike is unavailable to use.");
                        return;
                    } else
                    {
                        Log.d(TAG, "qr scanner success");
                        textView.setText("You can use the bike now");
                    }
                }
            }
        });
    }

    // TODO: Function for getting bike info
    private int dbCheck(Barcode barcode)
    {
        JSONObject bike = new JSONObject();
        return -1;
    }
}


