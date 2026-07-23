# 🔦 Luciole Mobile

> Application Android de **démonstration** : le SLM souverain français **Luciole‑1B** tourne **100 % sur le téléphone**, hors‑ligne, et **pilote de vraies actions** de l'appareil à partir d'une phrase en langage naturel.

[![Licence: AGPL v3](https://img.shields.io/badge/Licence-AGPL%20v3-blue.svg)](LICENSE)
![Plateforme](https://img.shields.io/badge/Plateforme-Android%2012%2B-3DDC84?logo=android&logoColor=white)
![Langage](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?logo=kotlin&logoColor=white)
![100% on-device](https://img.shields.io/badge/100%25-on--device-success)
[![Modèle](https://img.shields.io/badge/Mod%C3%A8le-Luciole--1B--Instruct%20(GGUF)-orange)](https://huggingface.co/mmaudet/Luciole-1B-Instruct-GGUF)

> **Interface adaptative** : un format **téléphone** (barre d'onglets, une colonne) et un format **Pixel Fold déplié** (rail de navigation, volets côte à côte).

<p align="center">
  <img src="screenshots/onboarding.png" width="82%" alt="Écran d'accueil sur Pixel Fold déplié : la promesse à gauche, la preuve de souveraineté à droite"/>
</p>

## ✨ L'idée en une phrase

Vous dites **« mets un minuteur de 5 minutes »**, **« appelle Paul Maudet »** ou **« itinéraire vers la gare de Lyon »**. Un modèle de langage **d'un milliard de paramètres** tourne **sur le téléphone lui‑même** (aucun cloud, aucune donnée qui sort), transforme votre phrase en une **action structurée**, et l'application **déclenche l'intent Android natif** correspondant : composer un numéro, ouvrir Maps, créer une alarme.

<p align="center">
  <img src="screenshots/chat-fold.png" width="92%" alt="Pixel Fold déplié : rail de navigation, conversation et panneau de gabarits d'actions côte à côte"/>
</p>

## ⚠️ Nature de la démonstration (à lire)

C'est une **démonstration destinée à l'expérimentation**, **pas un assistant généraliste**, et c'est **volontaire**.

- **Luciole‑1B** est un **très petit modèle** (*SLM, Small Language Model*, environ 1 milliard de paramètres). À cette taille, et **sans phase complète d'instruction et d'alignement sur des préférences humaines**, un modèle a des **limites importantes** : il ne « sait pas tout », ne tient pas une conversation ouverte, et se trompe sur des demandes générales.
- **Mais** c'est précisément cette petite taille qui lui permet de **fonctionner sur un mobile**. Le compromis assumé : ces SLM ne sont réellement exploitables que sur des **cas d'usage très ciblés et soigneusement cadrés**.
- **C'est tout l'objet de cette démo** : montrer qu'on peut **inférer un modèle souverain directement sur un téléphone** et, malgré sa petite taille, en tirer un **cas d'usage précis et fiable**, ici **piloter un répertoire fini d'actions de l'appareil**. Le modèle se contente de **router** la phrase vers l'une de **12 actions**, via une **grammaire** qui *garantit* une sortie valide ; il n'improvise pas le format et n'invente pas d'action.

> 👉 À utiliser pour **tester, expérimenter, démontrer**. Ce n'est pas un produit fini.

## 🤖 Le modèle : Luciole

**[Luciole](https://huggingface.co/OpenLLM-France/Luciole-1B-Instruct-1.0)** est un modèle de langage **francophone et souverain** développé par **[OpenLLM‑France](https://github.com/OpenLLM-France)**. Cette démo utilise **`Luciole‑1B‑Instruct‑1.0`** (architecture Nemotron, environ 1 Md de paramètres), **quantifié en `Q4_K_M`** (≈ 969 Mo) pour tenir en mémoire et s'exécuter sur le téléphone via [`llama.cpp`](https://github.com/ggerganov/llama.cpp).

### 📥 Télécharger le modèle (GGUF prêt pour la démo)

Le GGUF quantifié utilisé par l'application est en téléchargement libre :

**→ [huggingface.co/mmaudet/Luciole-1B-Instruct-GGUF](https://huggingface.co/mmaudet/Luciole-1B-Instruct-GGUF)**

| Fichier | Quantification | Taille | Téléchargement |
|---|---|---|---|
| `Luciole-1B-Instruct-Q4_K_M.gguf` | Q4_K_M | ≈ 969 Mo | [⬇️ lien direct](https://huggingface.co/mmaudet/Luciole-1B-Instruct-GGUF/resolve/main/Luciole-1B-Instruct-Q4_K_M.gguf) |

```bash
# via la CLI Hugging Face
hf download mmaudet/Luciole-1B-Instruct-GGUF Luciole-1B-Instruct-Q4_K_M.gguf --local-dir .models
```

## 📱 Fonctionnalités

- **Chat** : tapez (ou dictez) une phrase, obtenez une action. La **durée de traitement** s'affiche sur chaque réponse (« traité en 3,7 s »).
- **12 actions** déclenchées par des **intents Android natifs** : ⏰ alarme, ⏱ minuteur, 📅 agenda (avec date et heure réelles), ✉️ message (e‑mail / SMS), 📞 **appel par numéro ou par nom** (résolu depuis vos contacts), 🗺 itinéraire, 🔍 recherche, 📲 ouvrir une app, 🔤 traduction, 📝 note, 📇 **scanner une carte de visite**, 👤 **créer un contact** (champs structurés), et un repli **« je ne sais pas »** assumé quand la demande sort du cadre.
- **Scan carte de visite** : capture CameraX → correction OpenCV → OCR Tesseract local → structuration Luciole on-device (`creer_contact` dans la grammaire GBNF) + heuristiques/regex en secours → brouillon éditable → Contacts Android / `.vcf`. **Luciole ne fait pas l’OCR image** ; `scanner_carte` ouvre le flux caméra, `creer_contact` remplit le carnet.
- **Bouton Aide** : un panneau de **gabarits** (« itinéraire vers … ») où l'entité est **pré‑sélectionnée**, vous tapez directement par‑dessus.
- **Sécurité** : `ACTION_DIAL` (jamais d'appel automatique), aucun message envoyé automatiquement (l'éditeur s'ouvre, vous validez).
- **Bilingue** 🇫🇷 / 🇬🇧.
- **📡 Partage (démo sur n'importe quel réseau)** : un onglet qui **crée un hotspot WiFi depuis le téléphone** et affiche **deux QR codes** (rejoindre le WiFi, puis ouvrir la page web). Les invités scannent, se connectent et testent **sans rien taper** — idéal quand on ne maîtrise pas le réseau du lieu.

<p align="center">
  <img src="screenshots/aide-telephone.png" width="44%" alt="Format téléphone : la grille de gabarits d'actions et la carte d'aperçu (l'entité est pré-sélectionnée, on insère au bouton)"/>
</p>

## 🧠 Comment ça marche : architecture « cerveau / mains »

```
        Phrase libre
            │
            ▼
   ┌──────────────────┐   JSON d'action garanti valide
   │     Cerveau      │   (décodage contraint par une grammaire GBNF)
   │    Luciole-1B    │ ──►  { "type": "appel", "destinataire": "Paul Maudet" }
   │   (on-device)    │
   └──────────────────┘
            │
            ▼
   ┌──────────────────┐   Intent Android natif
   │      Mains       │ ──►  (ACTION_DIAL tel:… vers le Téléphone)
   │  (déterministe)  │   ou écran d'affichage (note, traduction, inconnu)
   └──────────────────┘
```

- **🧠 Le cerveau** est une **interface enfichable**. Aujourd'hui : `CerveauServeur` (HTTP vers un serveur `llama.cpp` **local**, sur le téléphone, dans Termux). Prévu : `CerveauEmbarqué` (llama.cpp **dans l'APK**, via JNI, plus aucune dépendance externe).
- **La sortie est contrainte par une grammaire GBNF** : le modèle **ne peut produire** qu'un des types d'action (dont `creer_contact`), en JSON valide. Pas de parsing fragile, pas d'hallucination de format.
- **✋ Les mains** traduisent l'action typée en intent Android natif. Comme l'app est au **premier plan**, les intents partent **sans restriction** (`BAL_ALLOW_VISIBLE_WINDOW`).

## 📊 Les statistiques : la preuve que tout est local

L'onglet **Statistiques** (« le téléphone, serveur du SLM ») montre en temps réel que c'est bien **le téléphone qui calcule** : tokens servis, débit en tokens par seconde, requêtes en cours, et un **histogramme** des tokens servis sur les 60 dernières secondes. Le cadenas le rappelle : **aucune donnée ne quitte le téléphone**.

<p align="center">
  <img src="screenshots/statistiques-fold.png" width="92%" alt="Tableau de bord (Fold déplié) : tokens servis, débit, histogramme avec axe des tokens/s, et l'impact du 1er prompt"/>
</p>

**L'impact du premier prompt sur la performance au démarrage.** À chaque requête, le modèle doit d'abord « lire » le **prompt système** : environ 1 300 tokens de règles et d'exemples qui cadrent les actions. La **toute première** requête après le démarrage du serveur paie ce coût en entier et reste lente (de l'ordre de 14 s sur le téléphone). Les requêtes suivantes **réutilisent le cache** de ce préfixe (le *KV cache* de `llama.cpp`) et retombent autour de **3 à 4 s**. Pour que l'utilisateur ne subisse jamais ce premier coût, l'application envoie un **pré‑chauffage silencieux** au lancement : une requête « à blanc » qui réchauffe le cache, afin que la première vraie demande soit déjà rapide. C'est ce que montre l'histogramme : une montée en charge initiale, puis un débit qui se stabilise.

## 📡 Partage : faire tester toute la salle, sur n'importe quel réseau

L'onglet **Partage** permet à un public de tester la démo **sans dépendre du WiFi du lieu** : le téléphone **crée son propre point d'accès WiFi** (hotspot local, sans Internet), et l'app génère **deux QR codes** —

1. **Rejoindre le WiFi** : le QR encode le réseau et son mot de passe → l'invité scanne et se connecte **sans rien saisir** ;
2. **Ouvrir la page** : le QR pointe vers la web app servie par le téléphone (`http://<ip>:8080`).

Chaque carte a un bouton **Copier** / **Partager**, et le SSID et le mot de passe sont aussi affichés en clair. Tout reste **sur le téléphone** : hotspot local + SLM local = **souveraineté de bout en bout**, même face à un réseau inconnu.

## 🔧 Détails techniques

| | |
|---|---|
| **Application** | Kotlin, Jetpack Compose, Material 3, minSdk 31, JDK 21 |
| **Inférence** | `llama.cpp` (`llama-server`) dans **Termux** sur le téléphone, modèle GGUF `Q4_K_M` |
| **Contrainte de sortie** | Grammaire **GBNF** (`--grammar-file`), JSON d'action toujours valide |
| **Contrat** | Prompt système et règles d'extraction (numéro, nom, **date**) re‑portés en Kotlin (parité testée avec la référence Python) |
| **Statistiques** | Métriques Prometheus `--metrics` du serveur, lues chaque seconde |
| **Robustesse** | timeout 60 s, **pré‑chauffage** du prompt au lancement, `configChanges` (survit au pliage du Pixel Fold), cleartext limité à `localhost` |
| **Tests** | **85+** tests unitaires et Robolectric (modèle, parsing JSON, extraction, contacts, scan/OCR, VCF, mapping d'intents, i18n) |
| **Scan carte** | CameraX + OpenCV + Tesseract4Android (100 % local, sans Google Play Services) ; structuration contact via `Cerveau.extractContact()` |

L'application a été **vérifiée en conditions réelles sur un Pixel 10 Pro Fold** : appel vers le Téléphone, itinéraire vers Maps, minuteur vers l'Horloge, agenda vers l'Agenda (avec l'heure), confirmés au logcat.

### Structure du dépôt

| Dossier | Rôle |
|---|---|
| `android/` | **L'application native** (le cœur de cette démo) |
| `server/` | Scripts de lancement du `llama-server` on‑device (Termux) |
| `contract/` | La **grammaire GBNF**, le schéma des actions et le schéma `ContactCard` |
| `web/` | Un client **web** (même design « République » que l'app) servi par le téléphone, pour faire tester depuis un navigateur |
| `dispatcher/` | Le dispatcher Python de référence (preuve de concept initiale) |

## 🚀 Lancer la démo

1. **Télécharger le modèle** GGUF (voir la section ci‑dessus) dans `.models/`.
2. **Sur le téléphone (Termux)** : lancer le serveur avec la grammaire et les métriques :
   ```bash
   ./server/run-server.sh        # llama-server --grammar-file contract/actions.gbnf --metrics --port 8080
   ```
3. **Construire l'application** (depuis `android/`) :
   ```bash
   ./gradlew :app:assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
   Pour le scan OCR, copiez `fra.traineddata` et `eng.traineddata` dans `android/app/src/main/assets/tesseract/tessdata/` (voir le README dans ce dossier).
4. Ouvrir **Luciole**, dire une phrase, regarder l'action se déclencher, et l'onglet **Statistiques** monter, **sans réseau**.

### 📦 Télécharger l'APK sans rien compiler (GitHub Actions)

À chaque évolution de l'app, **GitHub Actions** construit automatiquement un **APK debug** téléchargeable :

- **Interface** : onglet **[Actions](https://github.com/mmaudet/luciole-mobile/actions)** → dernier run **« Build APK (debug) »** au vert → section **Artifacts** → `luciole-debug-apk`.
- **CLI** : `gh run download -R mmaudet/luciole-mobile`, puis `adb install -r luciole-*-debug.apk`.

## 📄 Licence

Distribué sous **GNU Affero General Public License v3.0**, voir [`LICENSE`](LICENSE).

## 🙏 Crédits

- Modèle **Luciole** : **[OpenLLM‑France](https://github.com/OpenLLM-France)** (`Luciole‑1B‑Instruct‑1.0`).
- Inférence on‑device : **[llama.cpp](https://github.com/ggerganov/llama.cpp)**.

> *Souveraineté numérique : un modèle français, sur votre téléphone, qui n'envoie rien à personne.* 🔦

## Ajout reconnaissance de carter de visite vers Vcf 

1. Préparer les modèles OCR (sur le PC, avant compilation)
```
cd /home/monsieurb/Documents/devel/luciole-mobile/android/app/src/main/assets/tesseract/tessdata
curl -L -O https://github.com/tesseract-ocr/tessdata_fast/raw/main/fra.traineddata
curl -L -O https://github.com/tesseract-ocr/tessdata_fast/raw/main/eng.traineddata
```
Sans ces fichiers, l’OCR échouera avec un message du type « Modèle OCR manquant ».

2. Compiler et installer l’APK
cd /home/monsieurb/Documents/devel/luciole-mobile/android
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
Vérifier que le téléphone est bien détecté : adb devices.

3. Lancer Luciole-1B sur le téléphone (Termux)
Le scan a deux étapes : OCR local (Tesseract) puis structuration (HTTP vers 127.0.0.1:8080). Le serveur doit tourner sur le téléphone, pas sur le PC.

Copier le dépôt + modèle GGUF sur le téléphone (ou cloner dans Termux).
Dans Termux :
cd ~/luciole-mobile   # adapter le chemin
./server/run-server.sh
Le script démarre le processus d’inférence local (`llama-server` dans Termux) sur le port 8080 avec la grammaire on-device (`actions.gbnf`). Cette grammaire autorise les actions chat **et** `creer_contact` (JSON carnet). Le scan enchaîne OCR local (Tesseract) puis structuration Luciole via HTTP vers `127.0.0.1:8080` (**localhost du téléphone**, pas de cloud). Les heuristiques OCR + regex restent un secours si le LLM est indisponible.

Astuce démo : la 1ʳᵉ requête LLM est lente (~14 s). L’app pré-chauffe au lancement ; attendre quelques secondes avant le premier scan.

4. Parcours de test dans l’app
Option A — Onglet Scan
Ouvrir Luciole
Accepter la caméra quand demandé
Aller à l’onglet Scan
Cadrer une carte de visite, appuyer sur Capturer
Attendre la progression : correction → OCR → structuration Luciole
Vérifier le brouillon (champs + texte OCR brut)
Tester Créer le contact (écran Contacts prérempli) et Exporter VCF
Option B — Depuis le chat
Dire : « scanne une carte de visite »
Le flux Scan s’ouvre automatiquement
Même enchaînement qu’en option A
5. Ce qu’il faut valider
Étape	Succès attendu
Capture
Aperçu caméra live
Scan OpenCV
Image redressée (moins d’inclinaison)
OCR
Texte brut visible dans le brouillon
Luciole
Champs nom / société / tel / email préremplis
Contact
App Contacts s’ouvre avec les champs remplis
VCF
Partage d’un fichier .vcf importable
6. Dépannage rapide
Symptôme	Cause probable	Action
« Serveur injoignable » (chat)
llama-server arrêté
Relancer ./server/run-server.sh dans Termux
« Modèle OCR manquant »
traineddata absents de l’APK
Recopier les fichiers, recompiler, réinstaller
« OCR vide »
Photo floue / mal cadrée
Reprendre avec meilleure lumière, carte bien visible
« Document non détecté »
Contour non trouvé
Recadrer ou utiliser une carte sur fond contrasté
Structuration vide
LLM indisponible ou OCR pauvre
Vérifier Termux ; les tel/email regex restent via OCR
Caméra refusée
Permission
Paramètres → Luciole → Autoriser la caméra
Logs utiles :

```
adb logcat -s Luciole
```
