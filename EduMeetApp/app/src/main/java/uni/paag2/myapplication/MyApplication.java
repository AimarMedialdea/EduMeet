package uni.paag2.myapplication;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;

/**
 * Clase base de la aplicación para configurar el idioma a nivel global
 */
public class MyApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.setLocale(base));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LocaleHelper.setLocale(this);
    }
}