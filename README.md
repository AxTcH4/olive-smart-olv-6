# OliveSmart — Architecture & Fonctionnement

# NOTE: meni tsaliw 9raya dyal hada siro 9ray technical-handbook/readme.txt

## 1. Présentation du projet

OliveSmart est une plateforme de surveillance temporelle des oliveraies. Son objectif est de suivre individuellement les oliviers à travers plusieurs acquisitions d'images, de détecter les changements de leur état végétatif et d'identifier les arbres présentant des signes potentiels de dépérissement.

Le système repose sur une chaîne de traitement composée de plusieurs étapes :

1. Sélection géographique de la parcelle.
2. Récupération des images satellitaires correspondant à la parcelle et aux dates demandées.
3. Traitement des images et détection des couronnes.
4. Identification et suivi des arbres entre différentes acquisitions.
5. Analyse des changements temporels.
6. Évaluation du risque de dépérissement.
7. Inspection sur le terrain et validation humaine.

**Principe fondamental :** le système doit suivre le même arbre physique à travers plusieurs acquisitions, et non simplement comparer deux images d'une parcelle.

---

## 2. Architecture globale

### 2.1. Diagramme du workflow

```text
                       UTILISATEUR
                            |
                            v
                  Interface cartographique
                    (Leaflet envisagé)
                            |
                            v
                   Dessin de la parcelle
                       (GeoJSON)
                            |
                            v
                  +-------------------+
                  |  PARCEL MANAGEMENT|
                  |  Enregistrement  |
                  |  de la géométrie  |
                  +-------------------+
                            |
                            | parcelId
                            | + Polygon GeoJSON
                            v
                  Déclenchement d'un job
                  (parcelId, dateFrom,
                        dateTo)
                            |
                            v
                  +-------------------+
                  | IMAGERY RETRIEVAL |
                  |  Copernicus /     |
                  |  Sentinel-2       |
                  +-------------------+
                            |
                            | Images + métadonnées
                            v
                  +-------------------+
                  | IMAGE PROCESSING  |
                  |     WORKER        |
                  |     (OpenCV)       |
                  +-------------------+
                            |
                            | Acquisitions
                            | + Observations
                            v
                  +-------------------+
                  |   TREE TRACKING   |
                  |     SERVICE       |
                  +-------------------+
                            |
                            | Arbres identifiés
                            | + Snapshots
                            | + Correspondances
                            v
                  +-------------------+
                  |   CHANGE ANALYSIS |
                  |     SERVICE       |
                  +-------------------+
                            |
                            | Appel HTTP
                            v
                  +-------------------+
                  | CHANGE DETECTION  |
                  |     WORKER        |
                  |  (Python / ML)    |
                  +-------------------+
                            |
                            | ChangeIndicators
                            v
                  +-------------------+
                  |   DECLINE RISK    |
                  |     SERVICE       |
                  +-------------------+
                            |
                            | RiskAnalysis
                            v
                  +-------------------+
                  | FIELD INSPECTION  |
                  |     SERVICE       |
                  +-------------------+
                            |
                            | Missions d'inspection
                            | Photos + Confirmations
                            v
                  +-------------------+
                  |   DATASET SERVICE |
                  +-------------------+
                            |
                            | Labels validés
                            | Feedback
                            v
                      Amélioration
                       des modèles
```

Les différents composants ont des responsabilités distinctes. Les services Spring Boot gèrent la logique métier, tandis que les workers Python exécutent les traitements informatiques et les calculs liés aux images et aux modèles.

---

## 3. Sélection de la parcelle et récupération des images

Cette étape constitue le point d'entrée géographique du système.

Avant de lancer une analyse, l'utilisateur doit pouvoir sélectionner la parcelle agricole qu'il souhaite surveiller.

### 3.1. Interface cartographique — Frontend

L'application propose une carte interactive permettant à l'utilisateur de :

- Visualiser une zone géographique.
- Se déplacer et zoomer sur la carte.
- Dessiner le contour d'une parcelle.
- Modifier ou supprimer le contour dessiné.
- Enregistrer la parcelle.

**Technologie envisagée : Leaflet.**

Leaflet est notre candidat privilégié pour l'affichage et l'interaction avec la carte. Une extension comme Leaflet-Geoman peut être utilisée pour dessiner et modifier les polygones.

Le choix définitif de la bibliothèque cartographique reste ouvert.

Le fond cartographique permet à l'utilisateur de repérer visuellement sa parcelle. Il est distinct des images satellitaires qui seront utilisées par les traitements d'analyse.

### 3.2. Géométrie de la parcelle — GeoJSON

Lorsque l'utilisateur dessine une parcelle, le frontend récupère sa géométrie sous forme de coordonnées géographiques.

**GeoJSON est le format retenu pour représenter et échanger la géométrie des parcelles.**

Exemple :

```json
{
  "type": "Polygon",
  "coordinates": [
    [
      [-9.6120, 31.6290],
      [-9.6100, 31.6290],
      [-9.6100, 31.6270],
      [-9.6120, 31.6270],
      [-9.6120, 31.6290]
    ]
  ]
}
```

Ces coordonnées sont illustratives et ne représentent pas une parcelle réelle.

Quelques règles importantes :

- GeoJSON utilise l'ordre `[longitude, latitude]`.
- Le premier point du polygone doit être répété à la fin pour fermer la géométrie.
- Le système de coordonnées standard de GeoJSON est WGS 84 (EPSG:4326).

Le frontend transmet cette géométrie au backend lors de la création de la parcelle.

Le backend conserve notamment :

- L'identifiant `parcelId`.
- Les informations descriptives de la parcelle.
- Sa géométrie géographique.

**Important :** le `parcelId` identifie la parcelle dans notre application. Le GeoJSON décrit sa position et ses limites géographiques. Les deux informations sont nécessaires pour retrouver les images correspondant à cette parcelle.

### 3.3. Récupération des images satellitaires — Sentinel-2

**Source d'imagerie retenue initialement : Sentinel-2.**

Nous prévoyons d'utiliser le Copernicus Data Space Ecosystem pour accéder aux images Sentinel-2.

Le système doit rechercher les images satellitaires couvrant la géométrie de la parcelle, en fonction de la période demandée.

Deux capacités sont particulièrement utiles :

| API | Responsabilité |
| --- | --- |
| STAC / Catalogue | Rechercher les acquisitions disponibles pour une zone et une période données |
| Sentinel Hub Process API | Récupérer des images correspondant à une zone géographique et à des paramètres de traitement |

Le système transmet notamment :

- La géométrie de la parcelle (GeoJSON).
- La date de début.
- La date de fin.
- Des critères de sélection, comme la couverture nuageuse.

Le catalogue permet d'identifier les acquisitions disponibles. Les images retenues sont ensuite récupérées pour alimenter le traitement.

Le système doit conserver les métadonnées des acquisitions, notamment leur date et leur source.

**Point de vigilance :** Sentinel-2 possède une résolution spatiale de 10 mètres pour ses principales bandes visibles et proche infrarouge. Cette résolution est intéressante pour le suivi de la végétation, mais son adéquation avec la détection et le suivi individuel des oliviers doit être vérifiée expérimentalement.

Si la résolution s'avère insuffisante pour distinguer certaines couronnes individuelles, nous devrons étudier les possibilités d'obtenir des images à plus haute résolution.

---

## 4. Description des composants de traitement

### 4.1. Image Processing Worker — Python / OpenCV

**Objectif :** transformer les images des acquisitions en observations exploitables par le backend.

Responsabilités :

- Récupérer les images préparées pour la parcelle et la période demandée.
- Traiter les images.
- Détecter et segmenter les couronnes des oliviers.
- Extraire les observations individuelles.
- Récupérer les coordonnées géographiques et les caractéristiques nécessaires.

**Sortie :** une collection d'acquisitions contenant les observations détectées.

Modèle conceptuel :

```java
class Acquisition {
    Long id;
    LocalDate acquisitionDate;
    List<TreeObservation> observations;
}

class TreeObservation {
    Long id;

    // Coordonnées géographiques
    // Polygone de la couronne
    // Centroïde
    // Surface de la couronne
    // Indices de végétation disponibles
}
```

Une `TreeObservation` représente un arbre détecté dans une acquisition précise.

À ce stade, le système ne sait pas encore si cet arbre correspond à un arbre déjà observé dans une acquisition précédente.

**Remarque :** ce premier worker constitue une proposition d'architecture pour répondre au besoin de détection initiale des couronnes. OpenCV est la technologie retenue à ce stade ; la méthode précise de détection et de segmentation reste à définir.

### 4.2. Tree Tracking Service — Spring Boot

**Objectif :** identifier les mêmes arbres entre différentes acquisitions et maintenir leur identité dans le temps.

C'est une étape fondamentale du système.

Responsabilités :

- Comparer les observations de différentes acquisitions.
- Rechercher les arbres candidats à partir de leur proximité spatiale.
- Calculer les distances entre centroïdes.
- Calculer le Crown IoU (Intersection over Union) entre les polygones des couronnes.
- Construire une matrice de correspondance contenant les scores des candidats.
- Sélectionner les correspondances les plus pertinentes.
- Gérer les observations non appariées, les doublons, les fusions et les divisions.
- Attribuer et conserver un identifiant stable `tree_id`.

**Principe du matching :**

```text
Observation A (Acquisition 1)
             |
             v
     Recherche spatiale
       des candidats
             |
             v
    Calcul des métriques
       pour chaque candidat
             |
             v
    Calcul du matching score
             |
             v
    Matrice de correspondance
             |
             v
    Sélection des associations
       les plus cohérentes
             |
             v
    Validation de la correspondance
             |
             v
       Réutilisation du
          tree_id
```

La matrice de correspondance permet de comparer globalement les candidats entre deux acquisitions. Elle constitue une structure intermédiaire du processus de matching.

Les distances et les scores IoU sont des indicateurs de correspondance, pas des preuves absolues d'identité.

Modèle conceptuel :

```java
class TreeSnapshot {
    Long id;
    Long acquisitionId;
    TreeObservation observation;
}

class TreeCorrespondence {
    Long id;
    Long snapshotFromId;
    Long snapshotToId;

    Double distance;
    Double crownIoU;
    Double matchingScore;
}

class Tree {
    Long id; // Identifiant stable : tree_id

    List<TreeSnapshot> snapshots;
    List<TreeCorrespondence> correspondences;

    // Position représentative de l'arbre
}
```

**Concepts importants :**

- `TreeObservation` : détection d'un arbre dans une acquisition.
- `TreeSnapshot` : observation associée à un arbre suivi.
- `TreeCorrespondence` : relation entre deux snapshots.
- `Tree` : identité physique stable de l'olivier à travers le temps.

Un même `Tree` peut donc posséder plusieurs snapshots correspondant à différentes dates d'acquisition.

### 4.3. Change Analysis Service — Spring Boot

**Objectif :** mesurer les changements observés pour chaque arbre suivi.

Ce service orchestre les traitements temporels et communique avec le deuxième worker Python.

Responsabilités :

- Récupérer les arbres suivis et leurs snapshots.
- Préparer les données nécessaires à l'analyse.
- Transmettre les données au Change Detection Worker.
- Récupérer les résultats des traitements.
- Organiser les indicateurs de changement par `tree_id`.

**Sortie :** des indicateurs de changement pour chaque arbre.

Modèle conceptuel :

```java
class ChangeIndicators {
    Long id;
    Long treeId;

    // Variation de la couronne
    // Variation des indices de végétation
    // Résultats des modèles
    // Autres indicateurs temporels
}
```

Les traitements peuvent notamment inclure :

- L'enregistrement géométrique des orthomosaïques.
- La comparaison des couronnes.
- La détection des changements de végétation.
- L'analyse des changements temporels.

### 4.4. Change Detection Worker — Python / FastAPI

**Objectif :** exécuter les traitements informatiques nécessaires à l'analyse temporelle.

Ce worker est appelé par le Change Analysis Service.

Technologies envisagées :

- Python
- FastAPI
- OpenCV
- PyTorch
- Ruptures
- ONNX

Approches algorithmiques prévues dans le sujet :

| Approche | Utilisation |
| --- | --- |
| NDVI / variation des couronnes | Baseline explicable |
| CUSUM / Change Point Detection | Détection de ruptures temporelles |
| Isolation Forest | Détection d'anomalies |
| Siamese CNN | Comparaison de deux acquisitions |
| Temporal Autoencoder / TCN | Analyse de séquences temporelles |

Les modèles et méthodes seront comparés selon les données disponibles et les résultats expérimentaux.

### 4.5. Decline Risk Service — Spring Boot

**Objectif :** transformer les indicateurs de changement en une évaluation du risque de dépérissement.

Ce service ne réalise pas directement l'analyse des images. Il exploite les résultats fournis par le Change Analysis Service.

Responsabilités :

- Calculer un score de dépérissement pour chaque arbre.
- Appliquer des seuils de classification.
- Appliquer des règles de persistance pour éviter les alertes ponctuelles.
- Prendre en compte les variations saisonnières.
- Maintenir l'historique des évaluations.
- Identifier les arbres nécessitant une inspection sur le terrain.
- Prioriser les arbres selon leur niveau de risque.

Modèle conceptuel :

```java
enum RiskStatus {
    LOW_RISK,
    MODERATE_RISK,
    HIGH_RISK,
    INSPECTION_REQUIRED
}

class RiskChronologyEntry {
    LocalDate analysisDate;
    Double declineScore;
    RiskStatus status;
}

class RiskAnalysis {
    Long id;
    Long treeId;

    Double declineScore;
    RiskStatus status;

    List<RiskChronologyEntry> declineChronology;
}
```

La chronologie permet de conserver l'évolution du risque d'un même arbre au fil des analyses.

**Attention :** un statut de risque élevé ne signifie pas que l'arbre est définitivement mort ou malade. Il signifie qu'une inspection humaine peut être nécessaire.

### 4.6. Field Inspection Service — Spring Boot

**Objectif :** organiser les inspections sur le terrain et collecter les confirmations humaines.

Responsabilités :

- Récupérer les arbres nécessitant une inspection.
- Constituer une liste « Arbres à inspecter ».
- Organiser les missions d'inspection.
- Fournir la position des arbres.
- Organiser un itinéraire de visite.
- Enregistrer les photos prises sur le terrain.
- Enregistrer la condition confirmée par l'inspecteur.
- Conserver l'historique des inspections et des décisions.
- Transmettre les labels validés au Dataset Service.

Modèle conceptuel :

```java
enum TreeCondition {
    SAIN,
    STRESSE,
    MORT
}

class TreeInspection {
    Long id;

    Long treeId;
    Long riskAnalysisId;

    TreeCondition confirmedCondition;

    String photoUrl;
}
```

`confirmedCondition` est renseigné après l'inspection humaine.

Il faut distinguer deux notions :

- `RiskStatus` : évaluation automatique du risque.
- `TreeCondition` : état confirmé par l'inspecteur.

Un arbre peut donc avoir un statut `HIGH_RISK` avant inspection, puis être confirmé `SAIN`, `STRESSE` ou `MORT`.

Les inspections successives d'un même arbre doivent être conservées afin de préserver l'historique des décisions.

### 4.7. Dataset Service

**Objectif :** centraliser les labels et les retours provenant des inspections de terrain afin d'améliorer les données et les modèles.

Responsabilités :

- Recevoir les confirmations des inspecteurs.
- Conserver les labels validés.
- Associer les résultats aux arbres et aux acquisitions concernés.
- Préparer les données pour les futures expérimentations et améliorations des modèles.

Le retour du terrain permet de disposer de données de référence plus fiables pour l'évaluation des algorithmes.

---

## 5. Flux de données global

| Étape | Composant | Entrée | Sortie |
| --- | --- | --- | --- |
| 1 | Interface cartographique | Interaction utilisateur | Géométrie GeoJSON |
| 2 | Gestion des parcelles | Géométrie GeoJSON | parcelId + géométrie persistée |
| 3 | Imagery Retrieval | Géométrie + période | Images Sentinel-2 et métadonnées |
| 4 | Image Processing Worker | Images des acquisitions | Acquisitions et observations |
| 5 | Tree Tracking Service | Observations | Arbres suivis et correspondances |
| 6 | Change Analysis Service | Arbres et snapshots | Indicateurs de changement |
| 7 | Change Detection Worker | Données temporelles | Résultats des modèles |
| 8 | Decline Risk Service | ChangeIndicators | RiskAnalysis |
| 9 | Field Inspection Service | Arbres à risque | Missions et inspections |
| 10 | Dataset Service | Labels validés | Données de référence |

---

## 6. Quelques principes architecturaux

### Géométrie des parcelles

Une parcelle ne doit pas être représentée uniquement par un identifiant.

Le système doit conserver sa géométrie géographique afin de pouvoir retrouver les images couvrant exactement la zone demandée.

GeoJSON constitue le format retenu pour l'échange de géométrie entre le frontend et le backend.

### Gestion des acquisitions

Une parcelle peut posséder plusieurs acquisitions à différentes dates.

Le système doit conserver les métadonnées nécessaires pour identifier et retrouver chaque acquisition, notamment sa date, sa source et sa référence de stockage.

### Identité stable des arbres

Le `tree_id` représente l'identité physique d'un olivier.

Il doit rester identique à travers les acquisitions successives tant que le système considère qu'il s'agit du même arbre.

### Séparation des responsabilités

Chaque composant possède une responsabilité précise :

- L'interface cartographique permet de sélectionner la zone à analyser.
- Le module de récupération d'images recherche les acquisitions correspondantes.
- La détection identifie les couronnes visibles.
- Le tracking identifie les arbres entre acquisitions.
- Change Analysis mesure les changements.
- Decline Risk évalue le risque.
- Field Inspection organise et enregistre la validation humaine.
- Dataset Service centralise les retours terrain.

### Communication entre microservices

Les microservices Spring Boot communiquent à travers des contrats d'API et des DTOs.

Ils ne doivent pas dépendre directement des entités internes ou des bases de données des autres services.

Les workers Python exposent leurs capacités à travers FastAPI.

### Validation humaine

Les résultats de l'analyse automatique sont des candidats à l'inspection, et non des diagnostics définitifs.

La confirmation terrain reste une étape essentielle du système.

---

## 7. Points restant à définir

Cette architecture représente notre conception fonctionnelle actuelle. Plusieurs choix techniques devront être étudiés et validés pendant le développement :

1. Bibliothèque cartographique définitive (Leaflet ou alternative).
2. API et stratégie précises de récupération des images Sentinel-2.
3. Résolution spatiale nécessaire pour détecter et suivre individuellement les oliviers.
4. Méthode précise de détection et de segmentation des couronnes à implémenter avec OpenCV.
5. Méthode exacte de calcul du matching score.
6. Algorithme d'affectation et gestion des fusions/divisions.
7. Seuils de classification et règles de persistance.
8. Communication et orchestration des différents composants.
9. Gestion des acquisitions, des images et du stockage des résultats.

Les structures Java présentées dans ce document sont des modèles conceptuels destinés à expliquer le fonctionnement et les échanges de données. Elles ne constituent pas encore les contrats définitifs des API ni les entités JPA finales.

---

## 8. Objectif final

À partir des images Sentinel-2 d'une parcelle prises à différentes dates, OliveSmart doit être capable de :

- Sélectionner et enregistrer une parcelle géographique.
- Récupérer les images satellitaires correspondant à cette parcelle.
- Identifier individuellement les oliviers.
- Maintenir leur identité dans le temps.
- Mesurer l'évolution de leurs couronnes et de leur vigueur.
- Détecter les signes potentiels de dépérissement.
- Prioriser les arbres nécessitant une inspection.
- Collecter les confirmations humaines.
- Améliorer progressivement les modèles grâce aux données terrain.

**Le cœur du projet est donc le suivi temporel individuel des oliviers, associé à une détection du dépérissement et à une validation humaine sur le terrain.**