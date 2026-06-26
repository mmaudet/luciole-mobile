package fr.openllm.luciole.model
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
class ActionJsonTest {
    @Test fun parseAppel() {
        assertEquals(Action.Appel("0612345678"),
            ActionJson.parse("""{"type":"appel","destinataire":"0612345678"}"""))
    }
    @Test fun parseMinuteur() {
        assertEquals(Action.Minuteur(10, "thé"),
            ActionJson.parse("""{"type":"minuteur","duree_min":10,"libelle":"thé"}"""))
    }
    @Test fun parseMessageEmail() {
        assertEquals(Action.Message(Canal.EMAIL, "Retard", "Bonjour"),
            ActionJson.parse("""{"type":"message","canal":"email","objet":"Retard","corps":"Bonjour"}"""))
    }
    @Test fun parseOuvrir() {
        assertEquals(Action.Ouvrir(Cible.YOUTUBE),
            ActionJson.parse("""{"type":"ouvrir","cible":"youtube"}"""))
    }
    @Test fun parseTraduction() {
        assertEquals(Action.Traduction("bonjour", LangueCible.ANGLAIS, "hello"),
            ActionJson.parse("""{"type":"traduction","texte":"bonjour","cible":"anglais","resultat":"hello"}"""))
    }
    @Test fun parseInconnuSansChamps() {
        assertEquals(Action.Inconnu, ActionJson.parse("""{"type":"inconnu"}"""))
    }
    @Test fun typeInattenduTombeEnInconnu() {
        assertEquals(Action.Inconnu, ActionJson.parse("""{"type":"téléportation"}"""))
    }
    @Test fun jsonInvalideTombeEnInconnu() {
        assertEquals(Action.Inconnu, ActionJson.parse("pas du json"))
    }
}
