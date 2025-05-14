package uni.paag2.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;

import androidx.fragment.app.Fragment;

import java.util.Locale;

/**
 * Clase de utilidad para gestionar el idioma en toda la aplicación
 */
public class LocaleHelper {

    /**
     * Obtiene el idioma actual de la aplicación
     * @param context Contexto de la aplicación
     * @return Código del idioma (ej: "eu", "es")
     */
    public static String getCurrentLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        return prefs.getString("app_lang", "eu");
    }

    /**
     * Configura el idioma en un contexto
     * @param context Contexto a configurar
     * @return Contexto configurado con el idioma
     */
    public static Context setLocale(Context context) {
        String language = getCurrentLanguage(context);
        return updateLocale(context, language);
    }

    /**
     * Actualiza el idioma en un contexto
     * @param context Contexto a actualizar
     * @param language Código del idioma a aplicar
     * @return Contexto actualizado
     */
    public static Context updateLocale(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);

        return context.createConfigurationContext(config);
    }

    /**
     * Aplica el idioma configurado a los recursos de un Fragment
     * @param fragment Fragment al que se aplicará el idioma
     */
    public static void applyLocaleToFragment(Fragment fragment) {
        if (fragment == null || fragment.getContext() == null) return;

        String language = getCurrentLanguage(fragment.getContext());
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Resources resources = fragment.getResources();
        Configuration config = new Configuration(resources.getConfiguration());
        config.setLocale(locale);
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }
}