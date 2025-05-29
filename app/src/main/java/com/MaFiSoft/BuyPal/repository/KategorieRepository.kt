// app/src/main/java/com/MaFiSoft/BuyPal/repository/KategorieRepository.kt
// Stand: 2025-05-29 (Neu erstellt nach Goldstandard)

package com.MaFiSoft.BuyPal.repository

import com.MaFiSoft.BuyPal.data.KategorieEntitaet
import kotlinx.coroutines.flow.Flow

interface KategorieRepository {

    // Room-Operationen (Datenquelle für die UI)
    fun getAllKategorienFlow(): Flow<List<KategorieEntitaet>>
    fun getKategorieByIdFlow(kategorieId: String): Flow<KategorieEntitaet?>
    suspend fun saveKategorieLocal(kategorie: KategorieEntitaet)
    suspend fun updateKategorieLocal(kategorie: KategorieEntitaet)
    suspend fun deleteKategorieLocal(kategorieId: String)

    // Firestore-Synchronisationsoperationen (aufgerufen vom SyncManager)
    suspend fun syncKategorienToFirestore() // Hochladen lokaler Änderungen zu Firestore
    suspend fun syncKategorienFromFirestore() // Herunterladen von Firestore nach Room

    // Kombinierte Sync-Funktion für den SyncManager
    suspend fun syncKategorienMitFirestore() // Ruft beide Sync-Operationen auf
}