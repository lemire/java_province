# Carte du Canada

Visualisateur geojson en Java affichant les provinces et territoires du Canada avec une carte interactive.

## Prérequis

- Java 21+
- Maven 3.8+

## Structure du projet

```
canada-map/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── CanadaMap.java
│   │   └── resources/
│   │       └── province_territory_simplified.geojson
│   └── test/
│       └── java/
└── README.md
```

## Dépendances

- **JavaFX 21.0.2** - Framework GUI
- **Google Gson 2.10.1** - Parsing JSON (GeoJSON)

## Build et exécution

### Compiler le projet
```bash
mvn clean compile
```

### Exécuter l'application
```bash
mvn javafx:run
```

### Créer un JAR exécutable
```bash
mvn clean package
java -jar target/canada-map-1.0.0.jar
```

## Fonctionnalités

- 🗺️ Affichage interactif de toutes les provinces et territoires canadiens
- 🎨 Carte thermique colorée selon les salaires moyens annuels par province
  - **Rouge** : salaires faibles (Île-du-Prince-Édouard - 52 800$)
  - **Orange** → **Jaune** → **Vert** : progression des salaires
  - **Vert foncé** : salaires élevés (Alberta - 69 300$)
  - **Gris** : territoires (exclus de la carte thermique)
- 📊 Légende avec échelle chromatique et valeurs de salaires
- 🏷️ Noms des régions affichés en français sur la carte
- ⚡ Projection cartographique équirectangulaire simplifiée

## Architecture technique

### GeoJSON - Format des données géospatiales

Le fichier `province_territory_simplified.geojson` est au format GeoJSON, un standard ouvert pour encoder des structures géographiques. La structure est :

```json
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "properties": {
        "NAME": "Quebec"
      },
      "geometry": {
        "type": "MultiPolygon",
        "coordinates": [[[[-73.0, 45.0], [-73.5, 45.5], ...]]]
      }
    }
  ]
}
```

Chaque feature représente une province/territoire avec :
- `properties` : métadonnées (nom)
- `geometry` : coordonnées géographiques (longitude, latitude)
- Support de **MultiPolygon** (plusieurs polygones) et **Polygon** (un simple polygone)

### Google Gson - Analyse JSON

**Gson** est une bibliothèque Java open-source pour sérialiser et désérialiser du JSON. L'analyse se déroule en 3 étapes :

```java
// 1. Ouvrir le fichier depuis les ressources
InputStream stream = ClassLoader.getSystemResourceAsStream("province_territory_simplified.geojson");

// 2. Analyser avec Gson
JsonObject geoJson = JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject();

// 3. Extraire les entités
JsonArray features = geoJson.getAsJsonArray("features");
for (JsonElement feature : features) {
    JsonObject props = feature.getAsJsonObject().getAsJsonObject("properties");
    String name = props.get("NAME").getAsString();
    // Traiter les coordonnées...
}
```

Avantages de Gson :
- ✅ Léger et performant
- ✅ API intuitive pour l'analyse JSON
- ✅ Gestion native des tableaux et objets JSON
- ✅ Idéal pour les APIs basées sur JSON

### JavaFX - Interface graphique

**JavaFX** est le framework GUI moderne de Java pour créer des applications desktop riches avec support multi-plateforme.

Utilisation dans ce projet :
- **Canvas** : Surface de dessin pour afficher la carte
- **GraphicsContext** : API pour tracer des polygones, du texte, des formes
- **Color** : Gestion des couleurs et gradients
- **Stage & Scene** : Conteneurs pour la fenêtre et le contenu
- **Font** : Rendu du texte (noms des provinces)

### Pipeline de rendu

```
GeoJSON (fichier)
    ↓ (Analyse Gson)
Liste<Province> avec coordonnées [lon, lat]
    ↓ (Projection équirectangulaire)
Pixels [x, y] dans le canevas
    ↓ (Mappage salaire → couleur)
Dégradé de couleur (rouge → vert)
    ↓ (Dessin JavaFX)
Canevas → Affichage graphique
```

## Données

### Source GeoJSON

Les données géospatiales sont incluses dans le projet :
```
src/main/resources/province_territory_simplified.geojson
```

Format : **FeatureCollection** avec coordonnées en Web Mercator (EPSG:3857)
Contient : 13 polygones pour les 10 provinces + 3 territoires

### Salaires annuels moyens (2024)

Les données de salaire sont intégrées dans le code et utilisées pour la heatmap :

| Province/Territoire | Salaire annuel moyen |
|-------------------|-----------------|
| Île-du-Prince-Édouard | 52 800$ |
| Nouvelle-Écosse | 55 850$ |
| Nouveau-Brunswick | 57 460$ |
| Manitoba | 58 080$ |
| Québec | 60 200$ |
| Terre-Neuve-et-Labrador | 61 250$ |
| Saskatchewan | 62 350$ |
| Colombie-Britannique | 63 950$ |
| Ontario | 64 220$ |
| Alberta | 69 300$ |
| Yukon | 71 350$ (gris) |
| Territoires du Nord-Ouest | 87 900$ (gris) |
| Nunavut | 87 400$ (gris) |

*Note : Les territoires sont affichés en gris et exclus du gradient de couleur.*

## Notes

- Le fichier GeoJSON est chargé depuis `src/main/resources/` (pas depuis Internet)
- Les coordonnées sont en Web Mercator (EPSG:3857) et converties en projection équirectangulaire pour l'affichage
- Google Gson remplace Jackson pour le parsing JSON (plus lightweight)
- Support natif de MultiPolygon et Polygon du format GeoJSON
- Les noms de provinces sont traduits automatiquement de l'anglais au français
- La heatmap utilise uniquement les provinces (territoires en gris)
- Plage de salaires : 52 800$ (PEI) à 69 300$ (Alberta)

## Auteur

- Code original avec modifications pour utiliser Gson
