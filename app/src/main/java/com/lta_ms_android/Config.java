package com.lta_ms_android;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.Objects;

public class Config extends AppCompatActivity {
    private final String TAG = this.getClass().getSimpleName();
    EditText et_Username, et_MobileUUID; Button btn_save;

    SharedPreferences settings; SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Config");

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        editor = settings.edit();

        et_Username = findViewById(R.id.etUsername);
        btn_save = findViewById(R.id.btn_save);
        et_MobileUUID = findViewById(R.id.et_MobileUUID);

        et_Username.setText(settings.getString("username", ""));
        if (!et_Username.getText().toString().equals("")){
            et_Username.setEnabled(false);
            btn_save.setVisibility(View.GONE);
        }

        btn_save.setOnClickListener(view -> {
            String username = et_Username.getText().toString();

            if (username.equals("")){
                Log.e(TAG, "username fill is empty");
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
            else{
                setResult(Activity.RESULT_OK);
                editor.putString("username", username);
                editor.apply();
                finish();
            }
        });

        et_MobileUUID.setText(MainActivity.MobileUUID);
        et_MobileUUID.setEnabled(false);
    }
}
