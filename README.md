# luciole-mobile

Démo **on-device** de **Luciole-1B** (OpenLLM-France) : un LLM français qui tourne
entièrement sur smartphone, **hors-ligne (mode avion)**, et transforme une phrase
en **action réelle** — alarme, agenda, message, itinéraire, appel.

Pensé comme un **workshop de 30 min** : démo pilotée sur Pixel 10 Pro Fold +
mini hands-on où la salle teste sur ses propres téléphones (iOS **et** Android)
via une API locale exposée par le téléphone.

**Architecture cerveau / mains** : le modèle (le *cerveau*) produit une intention
structurée agnostique de l'OS (JSON garanti valide par décodage contraint GBNF) ;
une couche d'exécution déterministe (les *mains*) la traduit en action native
(intent Android sur le Pixel) ou en deep-link universel (côté participants).

> La documentation de conception et le plan d'implémentation sont conservés en
> local (dossier `docs/`, non versionné).
