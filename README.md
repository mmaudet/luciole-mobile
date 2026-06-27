# 🔦 Luciole Mobile

> Application Android de **démonstration** : le SLM souverain français **Luciole‑1B** tourne **100 % sur le téléphone**, hors‑ligne, et **pilote de vraies actions** de l'appareil à partir d'une phrase en langage naturel.

[![Licence: AGPL v3](https://img.shields.io/badge/Licence-AGPL%20v3-blue.svg)](LICENSE)
![Plateforme](https://img.shields.io/badge/Plateforme-Android%2012%2B-3DDC84?logo=android&logoColor=white)
![Langage](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?logo=kotlin&logoColor=white)
![100% on-device](https://img.shields.io/badge/100%25-on--device-success)
![Modèle](https://img.shields.io/badge/Mod%C3%A8le-Luciole--1B--Instruct--1.0-orange)

---

## ✨ L'idée en une phrase

Vous dites **« mets un minuteur de 5 minutes »**, **« appelle Paul Maudet »** ou **« itinéraire vers la gare de Lyon »** → un modèle de langage **d'1 milliard de paramètres** tourne **sur le téléphone lui‑même** (aucun cloud, aucune donnée qui sort), transforme votre phrase en une **action structurée**, et l'application **déclenche l'intent Android natif** correspondant : composer un numéro, ouvrir Maps, créer une alarme, etc.

<p align="center">
  <img src="screenshots/chat.png" width="32%" alt="Chat — une phrase devient une action"/>
  <img src="screenshots/aide.png" width="32%" alt="Panneau d'aide — gabarits d'actions"/>
  <img src="screenshots/statistiques.png" width="32%" alt="Statistiques — tout est calculé localement"/>
</p>

---

## ⚠️ Nature de la démonstration — à lire

C'est une **démonstration destinée à l'expérimentation**, **pas un assistant généraliste** — et c'est **volontaire**.

- **Luciole‑1B** est un **très petit modèle** (*SLM — Small Language Model*, ~1 milliard de paramètres). À cette taille, et **sans phase complète d'instruction et d'alignement sur des préférences humaines**, un modèle a des **limites importantes** : il ne « sait pas tout », ne tient pas une conversation ouverte, et se trompe sur des demandes générales.
- **Mais** c'est exactement cette petite taille qui lui permet de **fonctionner sur un mobile**. Le compromis assumé : ces SLM ne sont réellement exploitables que sur des **cas d'usage très ciblés et soigneusement cadrés**.
- **C'est tout l'objet de cette démo** : montrer qu'on peut **inférer un modèle souverain directement sur un téléphone** et, malgré sa petite taille, en tirer un **cas d'usage précis et fiable** — ici **piloter un répertoire fini d'actions de l'appareil**. Le modèle ne fait que **router** la phrase vers l'une de **11 actions**, via une **grammaire** qui *garantit* une sortie valide ; il n'improvise pas le format et n'invente pas d'action.

> 👉 À utiliser pour **tester, expérimenter, démontrer**. Ce n'est pas un produit fini.

---

## 🤖 Le modèle : Luciole

**[Luciole](https://huggingface.co/OpenLLM-France/Luciole-1B-Instruct-1.0)** est un modèle de langage **francophone et souverain** développé par **[OpenLLM‑France](https://github.com/OpenLLM-France)**. Cette démo utilise la version **`Luciole‑1B‑Instruct‑1.0`** (architecture Nemotron, ~1 Md de paramètres), **quantifiée en `Q4_K_M`** (~969 Mo) pour tenir en mémoire et s'exécuter sur le téléphone via [`llama.cpp`](https://github.com/ggerganov/llama.cpp).

---

## 📱 Fonctionnalités

- **Chat** : tapez (ou dictez) une phrase → action. La **durée de traitement** s'affiche sur chaque réponse (« *traité en 3,7 s* »).
- **11 actions** déclenchées par des **intents Android natifs** :
  ⏰ alarme · ⏱ minuteur · 📅 agenda *(avec date/heure réelle)* · ✉️ message *(e‑mail / SMS)* · 📞 **appel par numéro ou par nom** *(résolu depuis vos contacts)* · 🗺 itinéraire · 🔍 recherche · 📲 ouvrir une app · 🔤 traduction · 📝 note · et un repli **« je ne sais pas »** assumé quand la demande sort du cadre.
- **Bouton Aide** : un panneau de **gabarits** (« *itinéraire vers …* ») où l'entité est **pré‑sélectionnée** — vous tapez directement par‑dessus.
- **Écran « Statistiques »** *(le téléphone, serveur du SLM)* : tokens servis, débit tok/s, **histogramme en temps réel** — la **preuve visuelle** que tout est calculé localement (`🔒 aucune donnée ne quitte le téléphone`).
- **Sécurité** : `ACTION_DIAL` (jamais d'appel automatique), aucun message envoyé automatiquement (l'éditeur s'ouvre, vous validez).
- **Bilingue** 🇫🇷 / 🇬🇧.

---

## 🧠 Comment ça marche — architecture « cerveau / mains »

```
   Phrase libre
       │
       ▼
 ┌───────────────┐   JSON d'action garanti valide
 │   🧠 Cerveau   │   (décodage contraint par une grammaire GBNF)
 │  Luciole‑1B    │ ─────────────►  { "type": "appel", "destinataire": "Paul Maudet" }
 │  (on‑device)   │
 └───────────────┘
       │
       ▼
 ┌───────────────┐
 │   ✋ Mains      │   →  Intent Android natif  (ACTION_DIAL tel:…  →  Téléphone)
 │  (déterministe)│      ou écran d'affichage (note, traduction, inconnu)
 └───────────────┘
```

- **🧠 Le cerveau** est une **interface enfichable**. Aujourd'hui : `CerveauServeur` (HTTP → un serveur `llama.cpp` **local**, sur le téléphone, dans Termux). Prévu : `CerveauEmbarqué` (llama.cpp **dans l'APK**, via JNI → plus aucune dépendance).
- **La sortie est contrainte par une grammaire GBNF** : le modèle **ne peut produire** qu'un des 11 types d'action, en JSON valide. Pas de parsing fragile, pas d'hallucination de format.
- **✋ Les mains** mappent l'action typée vers un intent Android natif. Comme l'app est au **premier plan**, les intents partent **sans restriction** (`BAL_ALLOW_VISIBLE_WINDOW`).

---

## 🔧 Détails techniques

| | |
|---|---|
| **App** | Kotlin · Jetpack Compose · Material 3 · minSdk 31 · JDK 21 |
| **Inférence** | `llama.cpp` (`llama-server`) dans **Termux** sur le téléphone ; modèle GGUF `Q4_K_M` |
| **Contrainte de sortie** | Grammaire **GBNF** (`--grammar-file`) → JSON d'action toujours valide |
| **Contrat** | Prompt système + règles d'extraction (numéro / nom / **date**) re‑portés en Kotlin (parité testée avec la référence Python) |
| **Statistiques** | Métriques Prometheus `--metrics` du serveur, lues toutes les secondes |
| **Robustesse** | timeout 60 s · **pré‑chauffage** du prompt au lancement · `configChanges` (survit au pliage du Pixel Fold) · cleartext limité à `localhost` |
| **Tests** | **69** tests unitaires / Robolectric (modèle, parsing JSON, extraction, contacts, mapping d'intents, i18n) |

L'app a été développée **pilotée par une suite d'agents** (spécification → plan → implémentation tâche par tâche avec revue), puis **vérifiée en conditions réelles sur un Pixel 10 Pro Fold** (DIAL → Téléphone, VIEW → Maps, etc., confirmés au logcat).

### Structure du dépôt

| Dossier | Rôle |
|---|---|
| `android/` | **L'application native** (le cœur de cette démo) |
| `server/` | Scripts de lancement du `llama-server` on‑device (Termux) |
| `contract/` | La **grammaire GBNF** + le schéma des actions |
| `web/` | Un client **web** alternatif servi par le téléphone (variante « toute la salle teste ») |
| `dispatcher/` | Le dispatcher Python de référence (preuve de concept initiale) |

---

## 🚀 Lancer la démo

1. **Sur le téléphone (Termux)** — installer `llama.cpp`, déposer le GGUF `Luciole‑1B‑Instruct‑Q4_K_M.gguf`, puis lancer le serveur avec la grammaire et les métriques :
   ```bash
   ./server/run-server.sh        # llama-server --grammar-file contract/actions.gbnf --metrics --port 8080
   ```
2. **Construire l'app** (depuis `android/`) :
   ```bash
   ./gradlew :app:assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
3. Ouvrir **Luciole**, dire une phrase, regarder l'action se déclencher — et l'onglet **Statistiques** monter, **sans réseau**.

---

## 🗺 Feuille de route

- ✅ **Phase 1 (ici)** : app native ↔ serveur `llama.cpp` local sur le téléphone.
- 🔜 **Phase 2** : modèle **embarqué dans l'APK** (JNI + grammaire) → **zéro Termux**, un seul installable.

---

## 📄 Licence

Distribué sous **GNU Affero General Public License v3.0** — voir [`LICENSE`](LICENSE).

## 🙏 Crédits

- Modèle **Luciole** — **[OpenLLM‑France](https://github.com/OpenLLM-France)** (`Luciole‑1B‑Instruct‑1.0`).
- Inférence on‑device — **[llama.cpp](https://github.com/ggerganov/llama.cpp)**.
- Démonstration par **Michel Maudet**.

> *Souveraineté numérique : un modèle français, sur votre téléphone, qui n'envoie rien à personne.* 🔦
