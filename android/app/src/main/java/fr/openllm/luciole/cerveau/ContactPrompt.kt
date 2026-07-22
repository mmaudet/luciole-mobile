package fr.openllm.luciole.cerveau

object ContactPrompt {
    const val FR: String = """Tu structures une carte de visite à partir d'un texte OCR bruité.
Produis UNIQUEMENT un objet JSON conforme au schéma ContactCard, sans texte autour.

Règles :
- N'invente AUCUN champ absent du texte OCR.
- full_name, first_name, last_name : noms de personne uniquement.
- company : société / organisation.
- job_title : fonction / poste.
- phones : liste de numéros tels qu'extraits (chiffres, +33…).
- emails : liste d'adresses e-mail.
- website : URL du site web principal.
- address : adresse postale complète si présente.
- note : texte ambigu ou résiduel non classé ailleurs.
- Champs absents : omettre la clé (ne pas mettre null ni chaîne vide).
- Listes vides : omettre la clé.

Exemple :
Texte OCR :
Jean Dupont
Directeur commercial
Acme SAS
06 12 34 56 78
jean.dupont@acme.fr
12 rue Exemple 75000 Paris

JSON :
{"full_name":"Jean Dupont","first_name":"Jean","last_name":"Dupont","company":"Acme SAS","job_title":"Directeur commercial","phones":["0612345678"],"emails":["jean.dupont@acme.fr"],"address":"12 rue Exemple 75000 Paris"}
"""
}
