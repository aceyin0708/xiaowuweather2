package com.coolweather.android;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.coolweather.android.fragment.ChooseAreaFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        getSupportFragmentManager()    //
//                .beginTransaction()
//                .add(R.id.fragment_container,new ChooseAreaFragment())   // 此处的R.id.fragment_container是要盛放fragment的父容器
//                .commit();

    }
}
