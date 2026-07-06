package fr.openllm.luciole.partage

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.NetworkInterface

sealed interface HotspotEtat {
    data object Inactif : HotspotEtat
    data object Demarrage : HotspotEtat
    data class Actif(val ssid: String, val motDePasse: String, val ip: String) : HotspotEtat
    data class Erreur(val raison: String) : HotspotEtat
}

/**
 * Pilote un hotspot WiFi LOCAL (startLocalOnlyHotspot) : l'app le démarre et lit le SSID +
 * mot de passe générés par le système (pour les mettre dans un QR). Local-only = sans Internet,
 * cohérent avec le message « tout reste sur le téléphone ».
 */
class Hotspot(context: Context) {
    private val app = context.applicationContext
    private val _etat = MutableStateFlow<HotspotEtat>(HotspotEtat.Inactif)
    val etat: StateFlow<HotspotEtat> = _etat
    private var reservation: WifiManager.LocalOnlyHotspotReservation? = null

    fun demarrer() {
        val actuel = _etat.value
        if (actuel is HotspotEtat.Actif || actuel is HotspotEtat.Demarrage) return
        _etat.value = HotspotEtat.Demarrage
        val wifi = app.getSystemService(Context.WIFI_SERVICE) as WifiManager
        try {
            wifi.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
                override fun onStarted(res: WifiManager.LocalOnlyHotspotReservation) {
                    reservation = res
                    val cfg = res.softApConfiguration
                    @Suppress("DEPRECATION")
                    val ssid = (cfg.ssid ?: "").trim('"')
                    _etat.value = HotspotEtat.Actif(ssid, cfg.passphrase ?: "", ipHotspot())
                }
                override fun onFailed(reason: Int) {
                    _etat.value = HotspotEtat.Erreur(messageEchec(reason))
                }
                override fun onStopped() {
                    reservation = null
                    _etat.value = HotspotEtat.Inactif
                }
            }, null)
        } catch (e: SecurityException) {
            _etat.value = HotspotEtat.Erreur("Permission WiFi refusée : ${e.message ?: "autorise le partage des appareils à proximité"}.")
        } catch (e: Throwable) {
            _etat.value = HotspotEtat.Erreur(e.message ?: "Impossible de démarrer le hotspot.")
        }
    }

    fun arreter() {
        reservation?.close()
        reservation = null
        _etat.value = HotspotEtat.Inactif
    }

    /**
     * IP du téléphone sur l'interface du point d'accès (l'IP que les clients du hotspot doivent viser).
     * On écarte le WiFi client (wlan0), le cellulaire (rmnet) et les VPN/tunnels ; on privilégie une
     * interface AP explicite (ap, softap, swlan, wlan1 et plus). Fallback : première IP privée trouvée.
     */
    private fun ipHotspot(): String {
        var repli: String? = null
        try {
            for (nif in NetworkInterface.getNetworkInterfaces()) {
                if (!nif.isUp || nif.isLoopback) continue
                val nom = nif.name.lowercase()
                if (nom.startsWith("wlan0") || nom.startsWith("rmnet") || nom.startsWith("tun") || nom.contains("dummy")) continue
                for (addr in nif.inetAddresses) {
                    if (addr is java.net.Inet4Address && addr.isSiteLocalAddress) {
                        val ip = addr.hostAddress ?: continue
                        val estAp = nom.startsWith("ap") || nom.startsWith("softap") || nom.startsWith("swlan") ||
                            Regex("wlan[1-9].*").matches(nom)
                        if (estAp) return ip
                        if (repli == null) repli = ip
                    }
                }
            }
        } catch (_: Throwable) {
        }
        return repli ?: "192.168.49.1"
    }

    private fun messageEchec(reason: Int): String = when (reason) {
        WifiManager.LocalOnlyHotspotCallback.ERROR_NO_CHANNEL -> "Aucun canal WiFi disponible."
        WifiManager.LocalOnlyHotspotCallback.ERROR_INCOMPATIBLE_MODE -> "Coupe d'abord ton partage de connexion (hotspot normal)."
        WifiManager.LocalOnlyHotspotCallback.ERROR_TETHERING_DISALLOWED -> "Le partage de connexion est interdit sur cet appareil."
        else -> "Échec du démarrage du hotspot."
    }
}
