package com.example.ilona.flightsimprototype.utility;

import android.content.Context;

/**
 * Class: App
 * Author: Ilona
 * <p> The purpose of this class is to get the application's context.</>
 */

public class App extends android.app.Application {
    private static App mApp = null;
    /* (non-Javadoc)
     * @see android.app.Application#onCreate()
     */
    @Override
    public void onCreate()
    {
        super.onCreate();
        mApp = this;
    }
    public static Context context()
    {
        return mApp.getApplicationContext();
    }

}
