package pl.pwr.transit.transit.gtfs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class GtfsCsv {

    private GtfsCsv() {
    }

    static List<String> parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(ch);
                }
            } else if (ch == '"') {
                inQuotes = true;
            } else if (ch == ',') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        fields.add(current.toString());
        return fields;
    }

    static Map<String, Integer> headerIndex(List<String> header) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < header.size(); i++) {
            index.put(header.get(i).trim(), i);
        }
        return index;
    }

    static String required(List<String> fields, Map<String, Integer> index, String column) {
        Integer position = index.get(column);
        if (position == null || position >= fields.size()) {
            throw new IllegalArgumentException("Missing required GTFS column: " + column);
        }
        String value = fields.get(position).trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Empty required GTFS column: " + column);
        }
        return value;
    }

    static String optional(List<String> fields, Map<String, Integer> index, String column) {
        Integer position = index.get(column);
        if (position == null || position >= fields.size()) {
            return null;
        }
        String value = fields.get(position).trim();
        return value.isEmpty() ? null : value;
    }
}
