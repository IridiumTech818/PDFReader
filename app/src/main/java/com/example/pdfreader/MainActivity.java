package com.example.pdfreader;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {

    private String pdfUrl1 = "https://firebasestorage.googleapis.com/v0/b/handydiary-e2ab7.appspot.com/o/pdf%2FRich%20Dad%20Poor%20Dad%20(%20PDFDrive.com%20).pdf?alt=media&token=1e414e33-a3a5-4e1e-9d98-ee04bd02c04a";
    private String pdfUrl = "https://firebasestorage.googleapis.com/v0/b/handydiary-e2ab7.appspot.com/o/pdf%2Fprintfile.pdf?alt=media&token=baf0f379-66e3-4e9d-aed5-097e9721c4f5";
    private String pdfUrl2 = "https://www.iso.org/files/live/sites/isoorg/files/archive/pdf/en/annual_report_2009.pdf";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button3 = findViewById(R.id.open);
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(PdfRenderActivity.newIntent(MainActivity.this, pdfUrl));
            }
        });

        Button button = findViewById(R.id.openPdf1);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(PdfRenderActivity.newIntent(MainActivity.this, pdfUrl1));
            }
        });

        Button button2 = findViewById(R.id.openPdf2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(PdfRenderActivity.newIntent(MainActivity.this, pdfUrl2));
            }
        });
    }
}
