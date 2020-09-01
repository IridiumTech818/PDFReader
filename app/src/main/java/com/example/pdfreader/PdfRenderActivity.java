package com.example.pdfreader;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.downloader.Error;
import com.downloader.OnDownloadListener;
import com.downloader.OnProgressListener;
import com.downloader.PRDownloader;
import com.downloader.Progress;
import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnCompleteStreamListener;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.util.EncryptDecryptUtils;
import com.github.barteksc.pdfviewer.util.FileUtil;
import com.github.barteksc.pdfviewer.util.PermissionUtils;

import java.io.InputStream;

@SuppressWarnings("unused")
public class PdfRenderActivity extends AppCompatActivity implements OnPageChangeListener, OnLoadCompleteListener {

    private PDFView pdfView;
    private ProgressBar progressBar;
    private OnLoadCompleteListener onLoadCompleteListener;
    private OnPageChangeListener onPageChangeListener;
    private ProgressDialog mProgressDialog;

    private String pdfUrl;
    private static final String KEY_PDF = "key_pdf";

    public static Intent newIntent(Context context, String pdfUrl) {
        Intent newIntent = new Intent(context, PdfRenderActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(KEY_PDF, pdfUrl);
        newIntent.putExtras(bundle);
        return newIntent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_render);

        pdfView = findViewById(R.id.pdfView);
        progressBar = findViewById(R.id.progressBar);

        onLoadCompleteListener = this;
        onPageChangeListener = this;

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMax(100);
        mProgressDialog.setMessage("Please wait...");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(false);

        PRDownloader.initialize(getApplicationContext());

        if (getIntent().getExtras() != null && getIntent().getExtras().containsKey(KEY_PDF))
            this.pdfUrl = getIntent().getExtras().getString(KEY_PDF);

//        loadFromStream();

        downloadAndLoad();
    }

    private void downloadAndLoad() {
        if (PermissionUtils.isStoragePermissionGranted(this)) {
            if (FileUtil.isFileExists(this, FileUtil.getNameFromUrl(this.pdfUrl))) {
                readPdf(FileUtil.getNameFromUrl(this.pdfUrl));
            } else {
                downloadPdf(this.pdfUrl);
            }
        } else {
            PermissionUtils.grantStoragePermission(this);
        }
    }

    private void loadFromStream() {
        pdfView.loadFromStreamUrl(this.pdfUrl, new OnCompleteStreamListener() {
            @Override
            public void onComplete(InputStream inputStream) {
                pdfView.fromStream(inputStream)
                        .enableSwipe(true) // allows to block changing pages using swipe
                        .defaultPage(0)
                        .enableScreenShot(false, PdfRenderActivity.this)
                        .swipeHorizontal(false)
                        .onPageChange(onPageChangeListener)
                        .onLoad(onLoadCompleteListener)
                        .load();
            }
        });
    }


    private void downloadPdf(final String pdfUrl) {
        mProgressDialog.show();
        // Delete the old file //
        FileUtil.deleteDownloadedFile(this, FileUtil.getNameFromUrl(pdfUrl));
        PRDownloader.download(pdfUrl, FileUtil.getDirPath(this), FileUtil.getNameFromUrl(pdfUrl))
                .build()

                .setOnProgressListener(new OnProgressListener() {
                    @Override
                    public void onProgress(Progress progress) {
                        long progressPercent = progress.currentBytes * 100 / progress.totalBytes;
                        mProgressDialog.setIndeterminate(false);
                        mProgressDialog.setMax(100);
                        mProgressDialog.setProgress((int) progressPercent);
                    }
                })
                .start(new OnDownloadListener() {
                    @Override
                    public void onDownloadComplete() {
                        mProgressDialog.dismiss();
                        Toast.makeText(PdfRenderActivity.this, "Download Complete", Toast.LENGTH_SHORT).show();
                        encrypt(FileUtil.getNameFromUrl(pdfUrl));
                    }

                    @Override
                    public void onError(Error error) {

                    }
                });
    }

    private void readPdf(String fileName) {
        Toast.makeText(this, "Opening PDF", Toast.LENGTH_SHORT).show();
        pdfView.fromBytes(decrypt(fileName))
                .enableSwipe(true) // allows to block changing pages using swipe
                .defaultPage(0) // set to deafult page of the pdf
                .enableScreenShot(false, this) // let user take the screenshot
                .swipeHorizontal(true) // swipe vertically/horizontally
                .onPageChange(onPageChangeListener) // let user know the page number when changed
                .onLoad(onLoadCompleteListener) // called when the pdf is loaded
                .pageFling(true) // Swipe like ViewPager
                .fitEachPage(true) // Fit Each Page
                .enableAntialiasing(true)
                .load();

    }

    private void encrypt(String fileName) {
        try {
            byte[] fileData = FileUtil.readFile(FileUtil.getFilePath(this, fileName));
            byte[] encodedBytes = EncryptDecryptUtils.encode(EncryptDecryptUtils.getInstance(this).getSecretKey(), fileData);
            FileUtil.saveFile(encodedBytes, FileUtil.getFilePath(this, fileName));
            Toast.makeText(this, "File saved and encrypted", Toast.LENGTH_SHORT).show();
            readPdf(fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] decrypt(String fileName) {
        try {
            byte[] fileData = FileUtil.readFile(FileUtil.getFilePath(this, fileName));
            return EncryptDecryptUtils.decode(EncryptDecryptUtils.getInstance(this).getSecretKey(), fileData);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (FileUtil.isFileExists(this, FileUtil.getNameFromUrl(this.pdfUrl))) {
                readPdf(FileUtil.getNameFromUrl(this.pdfUrl));
            } else {
                downloadPdf(this.pdfUrl);
            }
        } else if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // TODO: 27-Aug-20 Denied always
            } else {
                // TODO: 27-Aug-20 Always not denied can ask again
            }
        }
    }

    @Override
    public void onPageChanged(int page, int pageCount) {
        Log.i("TAG", "onPageChanged: " + page + " , " + pageCount);
        progressBar.setProgress(page + 1);
        progressBar.setMax(pageCount);
    }

    @Override
    public void loadComplete(int nbPages) {

    }
}