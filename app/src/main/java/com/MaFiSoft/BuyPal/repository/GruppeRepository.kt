// app/src/main/java/com/MaFiSoft/BuyPal/repository/GruppeRepository.kt
// Stand: 2025-06-23_21:43:00, Codezeilen: ~40 (Anpassung an neue Gruppenlogik mit Beitrittscode)

package com.MaFiSoft.BuyPal.repository

import com.MaFiSoft.BuyPal.data.GruppeEntitaet
import kotlinx.coroutines.flow.Flow

/**
 * Schnittstelle fuer das Gruppe-Repository.
 * Definiert Operationen zum Abrufen und Verwalten von Gruppen.
 * Angepasst fuer Room-first-Strategie und Beitrittslogik.
 */
interface GruppeRepository {
    // Methoden zum Abrufen von Gruppen
    fun getGruppeById(gruppeId: String): Flow<GruppeEntitaet?>
    fun getAllGruppen(): Flow<List<GruppeEntitaet>> // Holt alle aktiven Gruppen (nicht zur Loeschung vorgemerkt)

    // Methode zum Abrufen von Gruppen, in denen eine bestimmte Benutzer-ID Mitglied ist
    fun getGruppenByMitgliedId(benutzerId: String): Flow<List<GruppeEntitaet>>

    // Methoden zum Speichern, Aktualisieren, Loeschen (Room-first, setzt Sync-Flags)
    suspend fun gruppeSpeichern(gruppe: GruppeEntitaet) // Speichert/Aktualisiert in Room und markiert fuer Sync
    suspend fun markGruppeForDeletion(gruppe: GruppeEntitaet) // Setzt Loeschungs-Flag und markiert fuer Sync (Soft Delete)
    suspend fun loescheGruppe(gruppeId: String) // Fuer endgueltige Loeschung (typischerweise nur vom SyncManager aufgerufen)

    /**
     * Synchronisiert die Gruppendaten zwischen der lokalen Room-Datenbank und Firestore.
     * Implementiert eine Room-first-Strategie.
     */
    suspend fun syncGruppenDaten()

    /**
     * Versucht, einer Gruppe mit dem angegebenen Namen und Beitrittscode beizutreten.
     * Wenn die Gruppe existiert und der Code korrekt ist, wird der aktuelle Benutzer
     * der Mitgliederliste der Gruppe in Firestore hinzugefuegt und die Gruppe lokal gepullt.
     *
     * @param gruppenName Der Name der Gruppe, der beigetreten werden soll.
     * @param beitrittsCode Der Beitrittscode der Gruppe.
     * @param aktuellerBenutzerId Die ID des aktuellen Benutzers, der beitreten moechte.
     * @return True, wenn der Beitritt erfolgreich war, False sonst (z.B. Gruppe nicht gefunden, Code falsch).
     */
    suspend fun gruppeBeitreten(gruppenName: String, beitrittsCode: String, aktuellerBenutzerId: String): Boolean
}
