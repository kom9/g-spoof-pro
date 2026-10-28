package com.gspoof.pro

import android.content.Context
import android.location.Address
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Format alamat gaya aplikasi BPR:
 *   [jalan + nomor], [Kecamatan X / kota], [provinsi]
 *
 * Contoh target:
 *   "Jl. Kp. Cicayur No.22, Kecamatan Pagedangan, Banten"
 *   "Jl. Raya Pagedangan Kampung Ranca Lele No.Raya, Tangerang"
 *
 * Yang dibuang: kode pos, "Indonesia", "RW 10", "LA", Plus Code.
 * Sumber utama: Geocoder Android (data Google, sama seperti app BPR),
 * fallback: Nominatim kalau Geocoder tidak tersedia / offline.
 */
object AddressFormat {

    suspend fun lookup(context: Context, lat: Double, lon: Double): String? =
        withContext(Dispatchers.IO) {
            fromGeocoder(context, lat, lon) ?: fromNominatim(lat, lon)
        }

    // ---------- Android Geocoder (utama) ----------

    private fun fromGeocoder(context: Context, lat: Double, lon: Double): String? = try {
        val gc = Geocoder(context, Locale("id", "ID"))
        @Suppress("DEPRECATION")
        val list = gc.getFromLocation(lat, lon, 1)
        list?.firstOrNull()?.let { formatAddress(it) }
    } catch (t: Throwable) {
        null
    }

    private fun formatAddress(a: Address): String? {
        val street = buildStreet(a)
        val kecamatan = a.subAdminArea?.trim()?.takeIf { it.isNotEmpty() }
            ?: a.locality?.trim()?.takeIf { it.isNotEmpty() }
        val kota = a.locality?.trim()?.takeIf { it.isNotEmpty() }
            ?: a.subAdminArea?.trim()
        val provinsi = a.adminArea?.trim()?.takeIf { it.isNotEmpty() }

        val parts = ArrayList<String>(3)
        if (street != null) parts.add(street)

        // Kecamatan dipakai kalau ada; kalau tidak, kota.
        val second = when {
            kecamatan == null -> kota
            kecamatan.lowercase(Locale.ROOT).startsWith("kecamatan") -> kecamatan
            kecamatan.lowercase(Locale.ROOT).startsWith("kota ") -> kecamatan
            else -> "Kecamatan $kecamatan"
        }
        if (second != null) parts.add(second)

        if (provinsi != null && parts.none { it.equals(provinsi, true) }) parts.add(provinsi)

        return parts.filter { it.isNotBlank() }.joinToString(", ").ifBlank { null }
    }

    private fun buildStreet(a: Address): String? {
        val road = a.thoroughfare?.trim()
        val no = a.subThoroughfare?.trim()
        if (road != null && road.isNotEmpty()) {
            return when {
                no.isNullOrEmpty() -> road
                road.contains(no, ignoreCase = true) -> road
                no.startsWith("No", ignoreCase = true) -> "$road $no"
                else -> "$road No.$no"
            }
        }
        // Tanpa nama jalan: pakai nama fitur / kelurahan supaya tidak kosong.
        val fallback = a.featureName?.trim()
        return fallback?.takeIf { it.isNotEmpty() && !it.matches(Regex("^[0-9+]{4,}.*")) }
    }

    // ---------- Nominatim (fallback) ----------

    private fun fromNominatim(lat: Double, lon: Double): String? {
        var conn: java.net.HttpURLConnection? = null
        return try {
            val url = java.net.URL(
                "https://nominatim.openstreetmap.org/reverse?format=json&zoom=18" +
                    "&addressdetails=1&accept-language=id&lat=$lat&lon=$lon"
            )
            conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "GSpoofPro/2.1 (Android)")
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode !in 200..299) return null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val addr = org.json.JSONObject(body).optJSONObject("address") ?: return null
            formatNominatim(addr)
        } catch (t: Throwable) {
            null
        } finally {
            runCatching { conn?.disconnect() }
        }
    }

    private fun formatNominatim(addr: org.json.JSONObject): String? {
        val road = addr.optString("road").trim()
        val no = addr.optString("house_number").trim()
        val street = when {
            road.isEmpty() -> addr.optString("neighbourhood").trim()
                .ifEmpty { addr.optString("hamlet").trim() }
            no.isEmpty() -> road
            road.contains(no, ignoreCase = true) -> road
            no.startsWith("No", true) -> "$road $no"
            else -> "$road No.$no"
        }.takeIf { it.isNotEmpty() }

        val kec = addr.optString("city_district").trim()
            .ifEmpty { addr.optString("municipality").trim() }
            .ifEmpty { addr.optString("suburb").trim() }
            .ifEmpty { addr.optString("village").trim() }
        val kota = addr.optString("city").trim()
            .ifEmpty { addr.optString("town").trim() }
            .ifEmpty { addr.optString("county").trim() }
        val prov = addr.optString("state").trim()

        val parts = ArrayList<String>(3)
        if (street != null) parts.add(street)
        val second = when {
            kec.isEmpty() -> kota
            kec.startsWith("Kecamatan", true) -> kec
            else -> "Kecamatan $kec"
        }
        if (second.isNotEmpty()) parts.add(second)
        if (prov.isNotEmpty() && parts.none { it.equals(prov, true) }) parts.add(prov)

        return parts.joinToString(", ").ifBlank { null }
    }
}
