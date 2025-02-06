package theHindustanLive.config;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

public class DateTimeUtil {

	private static final DateTimeFormatter INPUT_FORMATTER = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z");
    private static final DateTimeFormatter OUTPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a");

    public static LocalDateTime parsePubDate(String pubDateStr) {
        try {
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(pubDateStr, INPUT_FORMATTER);
            return zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        } catch (DateTimeParseException e) {
            e.printStackTrace();
            return LocalDateTime.now();
        }
    }

    public static String formatPubDate(LocalDateTime pubDate) {
        return pubDate.format(OUTPUT_FORMATTER);
    }
}
