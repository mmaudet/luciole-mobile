# Runbook — Workshop Luciole-1B (30 min, 100 % on-device)

## 0. Architecture (rappel)
Le **Pixel fait tout** : `llama-server` (cerveau, sortie **JSON garantie** par la grammaire GBNF)
+ `dispatcher` (mains, `am` → intents Android natifs) + la **page web** servie aux participants.
**11 intentions** : alarme, agenda, message (email/sms), itinéraire, appel — plus **minuteur,
note, recherche (Qwant), ouvrir (app/réglage), traduction (on-device), inconnu** (message « hors catalogue »). Cf. §6.
Le dispatcher est **stdlib pur** (aucune dépendance pip). `jsonschema` est optionnel
(`DISPATCH_VALIDATE=1`). Le numéro d'appel est **extrait de la phrase** (pas de la sortie du 1B).

## 1. Pré-vol (la veille, avec réseau)
### 1a. Termux + dépendances (une seule fois)
- Installer **Termux** (APK officiel GitHub, pas le Play Store). Via adb si besoin :
  `adb shell settings put global verifier_verify_adb_installs 0` ; `adb install termux.apk` ;
  remettre la valeur à `1` ensuite.
- Pousser sur `/sdcard/Download/` : le repo (`git archive --format=tar.gz HEAD`), le modèle
  `Luciole-1B-SFT-Q4_K_M.gguf`, et `scripts/termux_setup.sh`.
- Dans Termux : `termux-setup-storage` (**Autoriser**), puis
  `bash /sdcard/Download/termux_setup.sh`
  → installe `llama-cpp python termux-am`, extrait le repo dans `~/luciole-mobile`,
  copie le modèle dans `~/models`. (Log : `/sdcard/Download/luciole-setup.log`.)

### 1b. Anti-kill (IMPORTANT)
Android tue les process en arrière-plan. Pour que le serveur tienne :
- Réglages Android → Apps → **Termux → Batterie → Sans restriction**.
- Dans Termux : `termux-wake-lock` (garde le CPU éveillé).
- **Garder Termux au premier plan** pendant la démo (voir §3).

### 1c. Test à blanc
- Serveur (déjà tuné : 6 threads sur les cœurs rapides + mlock) :
  `bash ~/luciole-mobile/server/run-server.sh` → attendre `server is listening`.
- **Pré-chauffe** (sinon le 1er appel = ~14 s ; ensuite ~2,4 s) :
  `DISPATCH_DRY_RUN=1 python ~/luciole-mobile/dispatcher/dispatcher.py "mets une alarme à 8h"`
- Tir réel : `python ~/luciole-mobile/dispatcher/dispatcher.py "mets une alarme à 7h30"`
  → l'app Horloge crée l'alarme.

## 2. Mise en scène offline (en salle)
1. **Mode avion ON** (coupe internet) → **Hotspot / Point d'accès ON** (WiFi local sans backhaul).
   Le serveur écoute sur `0.0.0.0:8080`, donc il reste joignable sur le hotspot.
2. **IP du Pixel sur son hotspot** : souvent `192.168.43.1`. Sûr : sur le 2ᵉ appareil connecté,
   regarder la **passerelle WiFi** = l'IP du Pixel. (Ou `ip -4 addr` dans Termux si `iproute2` est là.)
3. QR : `python staging/make_qr.py --ssid <SSID> --pass <PWD> --ip <IP>`
   → `wifi-join.png` (rejoindre le WiFi) + `open-url.png` (ouvrir `http://<IP>:8080/`).
4. **Preuve hors-ligne** : montrer qu'un accès externe échoue (ex. charger google.com) mais que
   la page Luciole répond — tout vient du téléphone.

## 3. Démo pilotée (Pixel vidéoprojeté)
- **Laisser Termux au premier plan** (sinon Android tue le serveur). Bonus : le **log serveur
  qui défile** sur le projecteur = preuve visuelle que « ça calcule sur le téléphone ».
- Pour chaque phrase : `python ~/luciole-mobile/dispatcher/dispatcher.py "<phrase>"`.
- **Punch** : « mets une alarme à 7h30 » → l'alarme se pose, **sans réseau**.
- Un exemple par intention :
  - alarme : « rappelle-moi d'appeler le dentiste à 14h »
  - agenda : « ajoute une réunion projet demain à 10h en salle B »
  - message : « écris un mail à propos du retard de livraison » / « envoie un SMS … en retard de 10 min »
  - itinéraire : « itinéraire vers la gare de Lyon à Paris »
  - appel : « appelle le 06 12 34 56 78 » (s'ouvre dans le **numéroteur**, ne compose pas tout seul)

## 4. Hands-on participants (web, leur propre tél)
- **Pré-chauffe web** : ouvrir une fois `http://<IP>:8080/` et lancer **une** requête (le client web
  envoie un prompt légèrement différent → réamorce le cache une fois, ~14 s, puis ~2-3 s).
- **Concurrence** : pour plusieurs requêtes simultanées, relancer le serveur avec plus de slots —
  `PARALLEL=2 CTX=4096 bash ~/luciole-mobile/server/run-server.sh` (garder chaque slot ≥ ~2048 tokens (CTX/PARALLEL ≥ 2048 ; le prompt fait ~1,3k tokens)).
- Participants : scanner les 2 QR → page → taper une phrase → **deep-link adapté à l'OS**
  (iOS : maps.apple.com / `sms:&body=` ; Android : `geo:` / `sms:?body=` ; `.ics` pour l'agenda).
- **L'alarme n'a pas de bouton côté web** (aucun URI universel pour SET_ALARM) : c'est *justement*
  ce que l'**agent natif** sur le Pixel sait faire → à montrer en pilote. Les 4 autres = deep-links.

## 5. Dépannage
- **Termux / serveur tué** (passage en arrière-plan) : rouvrir Termux, `termux-wake-lock`,
  relancer `run-server.sh` puis **re-pré-chauffer**.
- **1er appel lent (~14 s)** : normal (prompt froid) → toujours pré-chauffer avant de présenter.
- **> 10 personnes** sur le hotspot : routeur de voyage sans WAN (Pixel + salle dessus).
- **Piloter le tél depuis le Mac** (prep/debug, pas nécessaire en démo) : `sshd` Termux sur :8022
  + `adb forward tcp:8022 tcp:8022` (USB, marche même en mode avion). Cf. mémoire `pixel-ssh-bridge`.
- **`--path` non géré par le build llama** : servir `web/` via `python -m http.server 8081` et faire
  pointer la page sur `http://<IP>:8080` pour l'API.

## 6. Catalogue élargi (nouvelles actions) & limites de routage
**Démo des nouvelles actions** (toutes sauf `recherche` marchent en mode avion) :
- minuteur : « minuteur de 10 minutes pour le thé » (démarre tout de suite)
- note : « note d'acheter du pain » (feuille de partage)
- recherche : « c'est quoi la capitale de l'Australie » — sur le Pixel, `WEB_SEARCH` ouvre le **moteur par défaut du téléphone** (ex. Google sur un Pixel stock) ; **Qwant** est le chemin **web** (participants). Nécessite le réseau.
- ouvrir : « ouvre YouTube » / « ouvre les réglages Bluetooth »
- traduction : « **traduis le chien dort en anglais** » → « dog is sleeping » (**100 % on-device**, hors-ligne)
- hors-catalogue : « raconte-moi une blague » → message « je ne sais pas (encore) faire ça »

**Limites de routage connues** (plafond d'un 1B ; e2e réel **21/21** sur les formulations principales) :
- La grammaire garantit TOUJOURS un JSON **valide** (zéro crash). Ce sont des imprécisions de
  *routage* sur des formulations secondaires, pas des bugs.
- **Traduction** (qualité 1B, best-effort) : préférer « **traduis X en \<langue\>** » avec un
  **texte simple et non ambigu**. Vérifié on-device : `bonjour→hello`, `maison→house`,
  `le chien dort→dog is sleeping` ✓ ; mais mots ambigus (`chat`, `café`) souvent laissés tels
  quels, phrases longues tronquées, et « comment dit-on X » ou « merci » peuvent partir en
  `message`. **Curer les phrases de démo.** (La trad reste 100 % on-device/souveraine.)
- **« ouvre \<app hors liste\> »** (ex. Spotify) ouvre une *autre* app de la liste au lieu de
  `inconnu` : le filet `inconnu` est fiable sur les impératifs clairement hors-catalogue
  (« raconte une blague »), pas sur une app plausible. Liste : youtube, maps, chrome,
  appareil_photo, parametres, bluetooth, wifi.
- Conseil scène : s'en tenir aux formulations ci-dessus ; le hands-on tolère l'imprécision
  (le JSON reste valide, l'action est juste parfois approximative).
