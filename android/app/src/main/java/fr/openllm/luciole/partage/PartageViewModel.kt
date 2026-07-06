package fr.openllm.luciole.partage

import android.app.Application
import androidx.lifecycle.AndroidViewModel

/** Porte le hotspot au niveau de l'Activity (survit aux changements de config / navigation). */
class PartageViewModel(app: Application) : AndroidViewModel(app) {
    private val hotspot = Hotspot(app)
    val etat = hotspot.etat
    fun demarrer() = hotspot.demarrer()
    fun arreter() = hotspot.arreter()
    override fun onCleared() {
        hotspot.arreter()
    }
}
