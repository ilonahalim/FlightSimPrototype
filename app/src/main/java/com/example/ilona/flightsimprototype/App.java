package com.example.ilona.flightsimprototype;

import android.content.Context;

/**
 * Created by Ilona on 26-Sep-17.
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
