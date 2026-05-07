package com.jarvis.skills.weather;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jarvis.skills.Skill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.logging.Logger;

@Component
public class WeatherSkill implements Skill {

    private static final Logger log = Logger.getLogger(WeatherSkill.class.getName());

    @Value("${jarvis.weather.default-city:London}")
    private String defaultCity;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    private static final Map<Integer, String> WMO_CODES = Map.ofEntries(
            Map.entry(0, "clear skies"), Map.entry(1, "mainly clear"), Map.entry(2, "partly cloudy"),
            Map.entry(3, "overcast"), Map.entry(45, "foggy"), Map.entry(61, "light rain"),
            Map.entry(63, "moderate rain"), Map.entry(65, "heavy rain"), Map.entry(71, "light snow"),
            Map.entry(73, "moderate snow"), Map.entry(75, "heavy snow"), Map.entry(80, "rain showers"),
            Map.entry(95, "thunderstorm")
    );

    @Override public String getIntentName() { return "WEATHER"; }

    @Override
    public String execute(Map<String, String> entities) {
        String city = entities.getOrDefault("location", defaultCity).trim();
        if (city.isBlank()) city = defaultCity;

        try {
            // Step 1: geocode city → lat/lon via Open-Meteo Geocoding API (free)
            String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name="
                    + URLEncoder.encode(city, StandardCharsets.UTF_8) + "&count=1&format=json";
            HttpResponse<String> geoRes = http.send(
                    HttpRequest.newBuilder().uri(URI.create(geoUrl))
                            .timeout(Duration.ofSeconds(8)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());

            JsonNode geoRoot = mapper.readTree(geoRes.body());
            JsonNode results = geoRoot.path("results");
            if (!results.isArray() || results.isEmpty()) {
                return "I couldn't find the location \"" + city + "\".";
            }
            double lat = results.get(0).path("latitude").asDouble();
            double lon = results.get(0).path("longitude").asDouble();
            String resolvedCity = results.get(0).path("name").asText(city);

            // Step 2: fetch current weather via Open-Meteo (free, no API key)
            String wxUrl = "https://api.open-meteo.com/v1/forecast?latitude=" + lat
                    + "&longitude=" + lon
                    + "&current=temperature_2m,weathercode,windspeed_10m"
                    + "&temperature_unit=celsius&windspeed_unit=kmh&timezone=auto";
            HttpResponse<String> wxRes = http.send(
                    HttpRequest.newBuilder().uri(URI.create(wxUrl))
                            .timeout(Duration.ofSeconds(8)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());

            JsonNode wx = mapper.readTree(wxRes.body()).path("current");
            double temp  = wx.path("temperature_2m").asDouble();
            int    code  = wx.path("weathercode").asInt(0);
            double wind  = wx.path("windspeed_10m").asDouble();

            String condition = WMO_CODES.getOrDefault(
                    WMO_CODES.containsKey(code) ? code : closestCode(code), "variable conditions");

            return String.format("Current weather in %s: %.0f°C with %s. Wind speed is %.0f km/h.",
                    resolvedCity, temp, condition, wind);

        } catch (Exception e) {
            log.warning("Weather fetch failed: " + e.getMessage());
            return "I couldn't retrieve the weather right now.";
        }
    }

    private int closestCode(int code) {
        return WMO_CODES.keySet().stream()
                .min((a, b) -> Math.abs(a - code) - Math.abs(b - code))
                .orElse(0);
    }
}
