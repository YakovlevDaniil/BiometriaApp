package com.example.biometriaapp;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.hardware.fingerprint.FingerprintManagerCompat;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.CancellationSignal;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.widget.Toast;
import com.example.biometriaapp.services.clients.bsclient.Configuration;
import com.example.biometriaapp.services.clients.bsclient.api.AndroidApi;
import com.example.biometriaapp.services.clients.bsclient.model.DeviceIdAuthorizeRequest;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class FingerLoginActivity  extends AppCompatActivity {

    private String android_id = Settings.Secure.ANDROID_ID;
    private SharedPreferences mPreferences;
    private FingerprintHelper mFingerprintHelper;
    private Intent intent;
    private ExecutorService executor;
    private AndroidApi api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finger_login);
        intent = new Intent(this, SuccessLoginActivity.class);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        api = new AndroidApi();
        executor= Executors.newSingleThreadExecutor();
    }

    @Override
    protected void onResume() {
        super.onResume();
        prepareSensor();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mFingerprintHelper != null) {
            mFingerprintHelper.cancel();
        }
    }

    private void prepareSensor() {
        mFingerprintHelper = new FingerprintHelper(this);
        mFingerprintHelper.startAuth();
    }


    public class FingerprintHelper extends FingerprintManagerCompat.AuthenticationCallback {
        private Context mContext;
        private CancellationSignal mCancellationSignal;

        FingerprintHelper(Context context) {
            mContext = context;
        }

        void startAuth() {
            FingerprintManagerCompat manager = FingerprintManagerCompat.from(mContext);
            manager.authenticate(null, 0, null, this, null);
        }

        void cancel() {
            if (mCancellationSignal != null) {
                mCancellationSignal.cancel();
            }
        }

        @Override
        public void onAuthenticationError(int errMsgId, CharSequence errString) {
            Toast.makeText(mContext, errString, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
            Toast.makeText(mContext, helpString, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
            try {
                Future<DeviceIdAuthorizeRequest> futureRes = executor.submit(() -> {
                    return api.androidDeviceIdAuthorizePost(String.valueOf(android_id.hashCode()));
                });
                try {
                    DeviceIdAuthorizeRequest res = futureRes.get();
                    if (res.getDeviceId() == null){
                        Toast.makeText(mContext, "Вы успешно авторизовались", Toast.LENGTH_SHORT).show();
                        startActivity(intent);
                    }
                    else {
                        Toast.makeText(mContext, "Ошибка авторизации попробуйте снова", Toast.LENGTH_SHORT).show();
                    }
                } catch (ExecutionException e) {
                    e.printStackTrace();
                    Toast.makeText(mContext, "Не удалось подключится к серверу", Toast.LENGTH_SHORT).show();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            catch (Exception ex){
                ex.getMessage();
            }

            Toast.makeText(mContext, "Ошибка авторизации попробуйте снова", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onAuthenticationFailed() {
            Toast.makeText(mContext, "Ошибка авторизации попробуйте снова", Toast.LENGTH_SHORT).show();
        }
    }
}
