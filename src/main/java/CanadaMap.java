import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.geometry.Bounds;
import javafx.stage.Stage;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class CanadaMap extends Application {

    private static final double WIDTH = 1000;
    private static final double HEIGHT = 800;

    // Couleurs différentes pour les provinces (10+ couleurs)
    private static final Color[] COLORS = {
        Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE,
        Color.PURPLE, Color.CYAN, Color.MAGENTA, Color.YELLOW,
        Color.PINK, Color.LIGHTGREEN, Color.BROWN, Color.LIGHTCYAN,
        Color.LIGHTGRAY, Color.SALMON, Color.KHAKI
    };

    private Map<String, List<List<double[]>>> provinces = new HashMap<>();
    private Map<String, Double> salaries = new HashMap<>(); // Salaires annuels moyens
    private double minLon = Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;
    private double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;

    private String hoveredProvince = null;
    private double mouseX = -1, mouseY = -1;

    @Override
    public void start(Stage stage) {
        initializeSalaries();
        loadGeoJSON();
        
        // Ajouter du padding de 5% autour de la carte
        double lonPadding = (maxLon - minLon) * 0.05;
        double latPadding = (maxLat - minLat) * 0.05;
        minLon -= lonPadding;
        maxLon += lonPadding;
        minLat -= latPadding;
        maxLat += latPadding;
        
        System.out.println("Limites calculées: Lon [" + minLon + ", " + maxLon + "], Lat [" + minLat + ", " + maxLat + "]");

        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        drawMap(gc);

        canvas.setOnMouseMoved(e -> {
            mouseX = e.getX();
            mouseY = e.getY();
            hoveredProvince = findProvinceAt(mouseX, mouseY);
            drawMap(gc);
        });
        canvas.setOnMouseExited(e -> {
            hoveredProvince = null;
            drawMap(gc);
        });

        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root, WIDTH, HEIGHT);

        stage.setTitle("Carte des provinces du Canada");
        stage.setScene(scene);
        stage.show();
    }

    private void loadGeoJSON() {
        try {
            // Charger le fichier GeoJSON depuis les ressources
            InputStream geoJsonStream = ClassLoader.getSystemResourceAsStream("province_territory_simplified.geojson");
            if (geoJsonStream == null) {
                System.err.println("Erreur: Le fichier province_territory_simplified.geojson n'a pas été trouvé dans les ressources");
                return;
            }
            
            InputStreamReader reader = new InputStreamReader(geoJsonStream);

            JsonObject geoJson = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray features = geoJson.getAsJsonArray("features");

            int colorIndex = 0;
            for (JsonElement featureElement : features) {
                JsonObject feature = featureElement.getAsJsonObject();
                JsonObject properties = feature.getAsJsonObject("properties");
                
                // Essayer différentes clés possibles pour le nom
                String name = null;
                if (properties != null) {
                    if (properties.has("NAME")) {
                        name = properties.get("NAME").getAsString();
                    } else if (properties.has("name")) {
                        name = properties.get("name").getAsString();
                    } else if (properties.has("PRENAME")) {
                        name = properties.get("PRENAME").getAsString();
                    } else if (properties.has("prename")) {
                        name = properties.get("prename").getAsString();
                    } else if (properties.has("PROVINCE")) {
                        name = properties.get("PROVINCE").getAsString();
                    } else if (properties.has("province")) {
                        name = properties.get("province").getAsString();
                    }
                }
                
                if (name == null) {
                    System.out.println("Warning: No name found for feature, available properties: " + properties.keySet());
                    continue;
                }

                JsonObject geometry = feature.getAsJsonObject("geometry");
                String geometryType = geometry.get("type").getAsString();

                if ("MultiPolygon".equals(geometryType)) {
                    List<List<double[]>> polygons = new ArrayList<>();
                    JsonArray coordinates = geometry.getAsJsonArray("coordinates");

                    for (JsonElement polyElement : coordinates) {
                        JsonArray poly = polyElement.getAsJsonArray();
                        List<double[]> ring = new ArrayList<>();

                        // Exterior ring (first element)
                        if (poly.size() > 0) {
                            JsonArray exteriorRing = poly.get(0).getAsJsonArray();
                            for (JsonElement pointElement : exteriorRing) {
                                JsonArray point = pointElement.getAsJsonArray();
                                double lon = point.get(0).getAsDouble();
                                double lat = point.get(1).getAsDouble();
                                ring.add(new double[]{lon, lat});
                            }
                            polygons.add(ring);
                        }
                    }

                    if (!polygons.isEmpty()) {
                        provinces.put(name, polygons);

                        // Mettre à jour les limites globales
                        for (double[] point : polygons.get(0)) {
                            minLon = Math.min(minLon, point[0]);
                            maxLon = Math.max(maxLon, point[0]);
                            minLat = Math.min(minLat, point[1]);
                            maxLat = Math.max(maxLat, point[1]);
                        }
                    }
                } else if ("Polygon".equals(geometryType)) {
                    List<List<double[]>> polygons = new ArrayList<>();
                    JsonArray coordinates = geometry.getAsJsonArray("coordinates");

                    if (coordinates.size() > 0) {
                        JsonArray exteriorRing = coordinates.get(0).getAsJsonArray();
                        List<double[]> ring = new ArrayList<>();

                        for (JsonElement pointElement : exteriorRing) {
                            JsonArray point = pointElement.getAsJsonArray();
                            double lon = point.get(0).getAsDouble();
                            double lat = point.get(1).getAsDouble();
                            ring.add(new double[]{lon, lat});
                        }
                        polygons.add(ring);
                        provinces.put(name, polygons);

                        // Mettre à jour les limites globales
                        for (double[] point : ring) {
                            minLon = Math.min(minLon, point[0]);
                            maxLon = Math.max(maxLon, point[0]);
                            minLat = Math.min(minLat, point[1]);
                            maxLat = Math.max(maxLat, point[1]);
                        }
                    }
                }
            }
            reader.close();
            System.out.println("Loaded " + provinces.size() + " provinces/territories");
        } catch (Exception e) {
            System.err.println("Error loading GeoJSON");
            e.printStackTrace();
        }
    }

    private String translateProvinceName(String englishName) {
        Map<String, String> translations = Map.ofEntries(
            Map.entry("Alberta", "Alberta"),
            Map.entry("British Columbia", "Colombie-Britannique"),
            Map.entry("Manitoba", "Manitoba"),
            Map.entry("New Brunswick", "Nouveau-Brunswick"),
            Map.entry("Newfoundland and Labrador", "Terre-Neuve-et-Labrador"),
            Map.entry("Northwest Territories", "Territoires du Nord-Ouest"),
            Map.entry("Nova Scotia", "Nouvelle-Écosse"),
            Map.entry("Nunavut", "Nunavut"),
            Map.entry("Ontario", "Ontario"),
            Map.entry("Prince Edward Island", "Île-du-Prince-Édouard"),
            Map.entry("Quebec", "Québec"),
            Map.entry("Saskatchewan", "Saskatchewan"),
            Map.entry("Yukon", "Yukon")
        );
        return translations.getOrDefault(englishName, englishName);
    }

    private void initializeSalaries() {
        salaries.put("Newfoundland and Labrador", 61250.0);
        salaries.put("Prince Edward Island", 52800.0);
        salaries.put("Nova Scotia", 55850.0);
        salaries.put("New Brunswick", 57460.0);
        salaries.put("Quebec", 60200.0);
        salaries.put("Ontario", 64220.0);
        salaries.put("Manitoba", 58080.0);
        salaries.put("Saskatchewan", 62350.0);
        salaries.put("Alberta", 69300.0);
        salaries.put("British Columbia", 63950.0);
        salaries.put("Yukon", 71350.0);
        salaries.put("Northwest Territories", 87900.0);
        salaries.put("Nunavut", 87400.0);
    }

    private Color salaryToColor(String provinceName, Double salary) {
        // Les territoires restent gris
        if (provinceName.equals("Yukon") || provinceName.equals("Northwest Territories") || 
            provinceName.equals("Nunavut")) {
            return Color.gray(0.7);
        }
        
        if (salary == null) {
            return Color.LIGHTGRAY;
        }
        
        // Min: 52800 (PEI), Max: 69300 (Alberta) - provinces seulement
        double minSalary = 52800.0;
        double maxSalary = 69300.0;
        
        // Normaliser entre 0 et 1
        double normalized = (salary - minSalary) / (maxSalary - minSalary);
        normalized = Math.max(0, Math.min(1, normalized)); // Clamp entre 0 et 1
        
        // Gradient: Rouge (bas salaire) -> Orange -> Jaune -> Vert (haut salaire)
        if (normalized < 0.25) {
            // Rouge au Orange
            double t = normalized / 0.25;
            return Color.color(1.0, 0.5 * t, 0);
        } else if (normalized < 0.5) {
            // Orange au Jaune
            double t = (normalized - 0.25) / 0.25;
            return Color.color(1.0, 0.5 + 0.5 * t, 0);
        } else if (normalized < 0.75) {
            // Jaune au Vert clair
            double t = (normalized - 0.5) / 0.25;
            return Color.color(1.0 - 0.5 * t, 1.0, 0);
        } else {
            // Vert clair au Vert foncé
            double t = (normalized - 0.75) / 0.25;
            return Color.color(0.5 - 0.3 * t, 1.0 - 0.3 * t, 0);
        }
    }

    private void drawMap(GraphicsContext gc) {
        gc.setFill(Color.LIGHTBLUE);
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        for (Map.Entry<String, List<List<double[]>>> entry : provinces.entrySet()) {
            String name = entry.getKey();
            Color color = salaryToColor(name, salaries.get(name));
            gc.setFill(color);
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(1);

            for (List<double[]> polygon : entry.getValue()) {
                double[] xPoints = new double[polygon.size()];
                double[] yPoints = new double[polygon.size()];

                for (int i = 0; i < polygon.size(); i++) {
                    double lon = polygon.get(i)[0];
                    double lat = polygon.get(i)[1];
                    // Projection simple (équirectangulaire)
                    xPoints[i] = (lon - minLon) / (maxLon - minLon) * WIDTH;
                    yPoints[i] = HEIGHT - (lat - minLat) / (maxLat - minLat) * HEIGHT;
                }

                gc.fillPolygon(xPoints, yPoints, xPoints.length);
                gc.strokePolygon(xPoints, yPoints, xPoints.length);
            }
        }

        // Dessiner la légende
        drawLegend(gc);

        // Tooltip survol
        if (hoveredProvince != null) {
            drawHoverPopup(gc);
        }
    }

    private void drawHoverPopup(GraphicsContext gc) {
        String frenchName = translateProvinceName(hoveredProvince);
        Double salary = salaries.get(hoveredProvince);
        String salaryStr = salary != null ? String.format("Salaire moyen : %,.0f $", salary) : "Salaire moyen : N/D";

        Font popupFont = Font.font(12);
        Text n = new Text(frenchName);
        n.setFont(popupFont);
        Text s = new Text(salaryStr);
        s.setFont(popupFont);
        double padding = 8;
        double lineH = n.getLayoutBounds().getHeight();
        double w = Math.max(n.getLayoutBounds().getWidth(), s.getLayoutBounds().getWidth()) + padding * 2;
        double h = lineH * 2 + padding * 2 + 2;

        double px = mouseX + 14;
        double py = mouseY + 14;
        if (px + w > WIDTH) px = mouseX - w - 14;
        if (py + h > HEIGHT) py = mouseY - h - 14;
        if (px < 0) px = 0;
        if (py < 0) py = 0;

        gc.setFill(Color.color(1, 1, 1, 0.95));
        gc.fillRect(px, py, w, h);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        gc.strokeRect(px, py, w, h);

        gc.setFill(Color.BLACK);
        gc.setFont(popupFont);
        Bounds nb = n.getLayoutBounds();
        gc.fillText(frenchName, px + padding, py + padding - nb.getMinY());
        Bounds sb = s.getLayoutBounds();
        gc.fillText(salaryStr, px + padding, py + padding + lineH + 2 - sb.getMinY());
    }

    private String findProvinceAt(double x, double y) {
        for (Map.Entry<String, List<List<double[]>>> entry : provinces.entrySet()) {
            for (List<double[]> polygon : entry.getValue()) {
                int n = polygon.size();
                double[] xs = new double[n];
                double[] ys = new double[n];
                for (int i = 0; i < n; i++) {
                    double lon = polygon.get(i)[0];
                    double lat = polygon.get(i)[1];
                    xs[i] = (lon - minLon) / (maxLon - minLon) * WIDTH;
                    ys[i] = HEIGHT - (lat - minLat) / (maxLat - minLat) * HEIGHT;
                }
                if (pointInPolygon(x, y, xs, ys)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    private boolean pointInPolygon(double x, double y, double[] xs, double[] ys) {
        boolean inside = false;
        int n = xs.length;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            if (((ys[i] > y) != (ys[j] > y)) &&
                (x < (xs[j] - xs[i]) * (y - ys[i]) / (ys[j] - ys[i]) + xs[i])) {
                inside = !inside;
            }
        }
        return inside;
    }

    private void drawLegend(GraphicsContext gc) {
        double legendX = WIDTH - 200;
        double legendY = 20;
        double legendWidth = 180;
        double legendHeight = 200;

        // Fond blanc semi-transparent
        gc.setFill(Color.color(1, 1, 1, 0.9));
        gc.fillRect(legendX, legendY, legendWidth, legendHeight);
        
        // Bordure noire
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        gc.strokeRect(legendX, legendY, legendWidth, legendHeight);

        // Titre
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font(12));
        gc.fillText("Salaire moyen", legendX + 10, legendY + 20);

        // Gradient bar
        double barX = legendX + 10;
        double barY = legendY + 30;
        double barWidth = 160;
        double barHeight = 20;

        // Dessiner le gradient
        int steps = 100;
        for (int i = 0; i < steps; i++) {
            double normalized = (double) i / steps;
            Color color;
            
            if (normalized < 0.25) {
                double t = normalized / 0.25;
                color = Color.color(1.0, 0.5 * t, 0);
            } else if (normalized < 0.5) {
                double t = (normalized - 0.25) / 0.25;
                color = Color.color(1.0, 0.5 + 0.5 * t, 0);
            } else if (normalized < 0.75) {
                double t = (normalized - 0.5) / 0.25;
                color = Color.color(1.0 - 0.5 * t, 1.0, 0);
            } else {
                double t = (normalized - 0.75) / 0.25;
                color = Color.color(0.5 - 0.3 * t, 1.0 - 0.3 * t, 0);
            }
            
            gc.setFill(color);
            gc.fillRect(barX + (i * barWidth / steps), barY, barWidth / steps + 1, barHeight);
        }

        // Bordure du gradient
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        gc.strokeRect(barX, barY, barWidth, barHeight);

        // Labels de min/max salaires (provinces seulement)
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font(10));
        gc.fillText("53K$", barX, barY + barHeight + 15);
        gc.fillText("69K$", barX + barWidth - 30, barY + barHeight + 15);

        // Info territoires
        gc.setFill(Color.gray(0.7));
        gc.fillRect(barX, barY + 50, 160, 15);
        gc.setStroke(Color.BLACK);
        gc.strokeRect(barX, barY + 50, 160, 15);
        
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font(10));
        gc.fillText("Territoires", barX + 5, barY + 62);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
