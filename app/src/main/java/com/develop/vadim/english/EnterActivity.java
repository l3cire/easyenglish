package com.develop.vadim.english;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class EnterActivity extends AppCompatActivity {

    String TAG = "myLogs";
    public FirebaseUser user;
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_register);
        Log.d(TAG, "started");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "not firstrun");
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        if (user == null || !user.isEmailVerified()) {
            mAuth.signOut();
            startActivity(new Intent(this, AuthenticationActivity.class));
        } else {
            startActivity(new Intent(this, MainActivity.class));
        }
    }
}
