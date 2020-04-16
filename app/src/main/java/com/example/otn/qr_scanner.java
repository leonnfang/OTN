package com.example.otn;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.SurfaceView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

public class qr_scanner extends AppCompatActivity {
    private DBConnection dbConnection;

    private SurfaceView surfaceView;
    private CameraSource cameraSource;
    private TextView textView;
    protected BarcodeDetector barcodeDetector;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        setContentView(R.layout.fragment_bike);
        surfaceView = (SurfaceView)findViewById(R.id.qrCode);
        textView = (TextView)findViewById(R.id.text_bike);

        barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE).build();
        cameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setRequestedPreviewSize(640,480).build();
        
    }
}
