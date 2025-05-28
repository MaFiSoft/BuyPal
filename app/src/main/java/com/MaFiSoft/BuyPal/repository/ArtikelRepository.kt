// app/src/main/java/com/MaFiSoft/BuyPal/data/repository/ArtikelRepository.kt
// Stand: 2025-05-28_22:50 (Angepasst an BenutzerRepository Muster)

package com.MaFiSoft.BuyPal.repository

import com.MaFiSoft.BuyPal.data.ArtikelEntitaet
import kotlinx.coroutines.flow.Flow

// Das ArtikelRepository-Interface.
// Definiert die Operationen, die fuer den Zugriff auf Artikeldaten verfuegbar sind (aus ViewModel-Sicht).
// Die Implementierung dieser Methoden erfolgt in ArtikelRepositoryImpl.
interface ArtikelRepository {
    // Methoden, die das ViewModel typischerweise direkt aufrufen würde
    fun getArtikelById(artikelId: String): Flow<ArtikelEntitaet?>
    fun getArtikelFuerListe(listenId: String): Flow<List<ArtikelEntitaet>>
    fun getNichtAbgehakteArtikelFuerListe(listenId: String): Flow<List<ArtikelEntitaet>>
    fun getNichtAbgehakteArtikelFuerListeUndGeschaeft(
        listenId: String,
        geschaeftId: String
    ): Flow<List<ArtikelEntitaet>>

    suspend fun artikelSpeichern(artikel: ArtikelEntitaet)
    suspend fun artikelAktualisieren(artikel: ArtikelEntitaet)
    suspend fun artikelLoeschen(artikelId: String)
    suspend fun alleArtikelFuerListeLoeschen(listenId: String)
    suspend fun toggleArtikelAbgehaktStatus(artikelId: String, listenId: String)

    // Synchronisationsfunktion (aus Implementierung übernommen)
    suspend fun syncArtikelFromFirestore()

    // Die `getAllArtikel()` Methode ist nicht im BenutzerRepository Interface,
    // wird aber in der Implementierung des ArtikelRepositoryImpl noch gebraucht,
    // oder wenn das ViewModel es direkt nutzen würde.
    // Belassen wir sie hier, da sie eine allgemeine Abfrage ist.
    fun getAllArtikel(): Flow<List<ArtikelEntitaet>>
}