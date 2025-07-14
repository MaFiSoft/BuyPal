// app/src/main/java/com/MaFiSoft/BuyPal/utils/KeyboardUtils.kt
// Stand: 2025-07-13_12:40:00, Codezeilen: ~10 (Zentrale Hilfsfunktion fuer Tastaturkontrolle)

package com.MaFiSoft.BuyPal.utils

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity

/**
 * Hilfsfunktion zum Ausblenden der Tastatur.
 * Diese Funktion ist global verfuegbar, um Konflikte zu vermeiden.
 *
 * @param context Der Android-Context, typischerweise LocalContext.current.
 */
fun hideKeyboard(context: Context) {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow((context as? ComponentActivity)?.currentFocus?.windowToken, 0)
}
