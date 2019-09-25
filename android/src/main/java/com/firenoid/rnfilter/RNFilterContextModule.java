package com.firenoid.rnfilter;

import android.app.Activity;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;


    public class RNFilterContextModule extends ReactContextBaseJavaModule {

        public RNFilterContextModule(ReactApplicationContext reactContext) {
            super(reactContext);
        }

        @Override
        public String getName() {
            return "RNFilter";
        }

        public Activity getActivity() {
            return this.getCurrentActivity();
        }
}
