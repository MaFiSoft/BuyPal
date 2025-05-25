// com/MaFiSoft/BuyPal/data/ProduktGeschaeftVerbindung.kt
package com.MaFiSoft.BuyPal.data

import androidx.room.Entity

/**
 * Entitaet fuer die Verbindung zwischen einem Produkt und einem Geschaeft.
 * Dies bildet eine N:M-Beziehung ab.
 *
 * @param produktId ID des Produkts.
 * @param geschaeftId ID des Geschaefts.
 */
@Entity(
    tableName = "produkt_geschaeft_verbindung",
    primaryKeys = ["produktId", "geschaeftId"] // Ein zusammengesetzter Primärschlüssel
)
data class ProduktGeschaeftVerbindung(
    val produktId: String,
    val geschaeftId: String
)
