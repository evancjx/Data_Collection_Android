package com.lta_ms_android;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.Objects;

public class Config extends AppCompatActivity {
    EditText etUsername; Button btn_save;

    SharedPreferences settings; SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Config");

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        editor = settings.edit();

        etUsername = findViewById(R.id.etUsername);
        etUsername.setText(settings.getString("username", MainActivity.MobileUUID));

        btn_save = findViewById(R.id.btn_save);
        btn_save.setOnClickListener(view -> {
            String username = etUsername.getText().toString();

            editor.putString("username", username);
            editor.apply();

            setResult(Activity.RESULT_OK);
            finish();
        });
    }
}
