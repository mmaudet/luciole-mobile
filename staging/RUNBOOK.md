# Runbook — Workshop Luciole-1B (30 min)

## Pré-vol (avant la salle)
1. Modèle sur le tél : `~/models/Luciole-1B-SFT-Q4_K_M.gguf` (cf. Task 11/13).
2. Termux : `pkg install llama-cpp python termux-am` ; `pip install requests jsonschema`.
3. Repo sur le tél : `~/luciole-mobile` (cloné/poussé) avec `contract/`, `web/`, `server/`.
4. Test à blanc : `bash ~/luciole-mobile/server/run-server.sh` puis, depuis un 2e appareil
   sur le hotspot, ouvrir `http://<ip>:8080/`.

## Mise en scène (en salle)
1. **Mode avion ON** (coupe internet) → **Hotspot ON** (WiFi local, sans backhaul).
2. Relever l'IP du Pixel sur son hotspot (souvent `192.168.43.1`).
3. Générer les QR : `python staging/make_qr.py --ssid <SSID> --pass <PWD> --ip <IP>`.
4. Projeter `wifi-join.png` (rejoindre le WiFi) puis `open-url.png` (ouvrir la page).
5. Preuve hors-ligne : depuis un tél de la salle, montrer que `google.com` échoue mais la page répond.

## Démo (Pixel, vidéoprojeté)
- Lancer `run-server.sh`. Pour chaque action : `python ~/luciole-mobile/dispatcher/dispatcher.py "<phrase>"`.
- Phrase « punch » : « rappelle-moi d'appeler le dentiste à 14h » → l'alarme se crée, **sans réseau**.

## Repli
- > 10 personnes : routeur de voyage sans WAN (Pixel + salle dessus).
- `--path` absent du build : `python -m http.server 8081` dans `web/`, et la page pointe sur `http://<ip>:8080` pour l'API (passer la grammaire par requête).
