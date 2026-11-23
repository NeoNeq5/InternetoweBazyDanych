package pl.pwr.transit.transit.application;

import pl.pwr.transit.transit.application.model.LatLon;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ShapePointsCodec {

    private ShapePointsCodec() {
    }

    public static String write(List<LatLon> points) {
        StringBuilder json = new StringBuilder(points.size() * 24);
        json.append('[');
        for (int i = 0; i < points.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            LatLon point = points.get(i);
            json.append('[')
                    .append(String.format(Locale.ROOT, "%.6f", point.lat()))
                    .append(',')
                    .append(String.format(Locale.ROOT, "%.6f", point.lon()))
                    .append(']');
        }
        json.append(']');
        return json.toString();
    }

    public static List<List<Double>> read(String json) {
        List<List<Double>> points = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return points;
        }

        int i = 0;
        char[] chars = json.toCharArray();
        while (i < chars.length && chars[i] != '[') {
            i++;
        }
        i++;

        while (i < chars.length) {
            while (i < chars.length && Character.isWhitespace(chars[i])) {
                i++;
            }
            if (i >= chars.length || chars[i] == ']') {
                break;
            }
            if (chars[i] != '[') {
                throw new IllegalArgumentException("Invalid shape JSON");
            }
            i++;
            ParsedNumber lat = readNumber(chars, i);
            i = lat.end();
            while (i < chars.length && (Character.isWhitespace(chars[i]) || chars[i] == ',')) {
                i++;
            }
            ParsedNumber lon = readNumber(chars, i);
            i = lon.end();
            while (i < chars.length && chars[i] != ']') {
                i++;
            }
            i++;
            points.add(List.of(lat.value(), lon.value()));
            while (i < chars.length && (Character.isWhitespace(chars[i]) || chars[i] == ',')) {
                i++;
            }
        }
        return points;
    }

    private static ParsedNumber readNumber(char[] chars, int start) {
        int end = start;
        while (end < chars.length
                && (Character.isDigit(chars[end])
                || chars[end] == '-'
                || chars[end] == '.'
                || chars[end] == 'e'
                || chars[end] == 'E'
                || chars[end] == '+')) {
            end++;
        }
        return new ParsedNumber(Double.parseDouble(new String(chars, start, end - start)), end);
    }

    private record ParsedNumber(double value, int end) {
    }
}
