package com.devlomi.fireapp.activities.main;

import android.os.AsyncTask;
import android.util.Log;

import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;

public class Task extends AsyncTask<String, Void, String> {

    @Override
    protected String doInBackground(String... strings) {
        try {
            Translate translate = TranslateOptions.getDefaultInstance().getService();
            Translation translation = translate.translate("¡Hola Mundo!");
            Log.e("TranslateOptions", "onCreate: " + "Translated Text:\n\t%s\n " + translation.getTranslatedText());
            return translation.getTranslatedText();
        }catch (RuntimeException e){
            return null;
        }
    }
}
