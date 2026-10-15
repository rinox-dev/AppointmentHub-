package com.appointmenthub.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

/**
 * Single JSON configuration shared by the REST API and the persistence layer.
 *
 * <p>The documented wire formats are:</p>
 * <ul>
 *   <li>date: {@code yyyy-MM-dd} (strict: {@code 2027-2-10} is rejected)</li>
 *   <li>time: {@code HH:mm} (strict: {@code 10:00:00} is rejected)</li>
 *   <li>timestamp: ISO-8601 in UTC, for example {@code 2026-10-15T04:30:00Z}</li>
 * </ul>
 */
public final class Json {

    /** Strict {@code yyyy-MM-dd} format, used for parsing and formatting dates. */
    public static final String DATE_PATTERN = "uuuu-MM-dd";

    /** Strict {@code HH:mm} format, used for parsing and formatting times. */
    public static final String TIME_PATTERN = "HH:mm";

    public static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern(DATE_PATTERN).withResolverStyle(ResolverStyle.STRICT);

    public static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern(TIME_PATTERN).withResolverStyle(ResolverStyle.STRICT);

    private static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .disableHtmlEscaping()
            .registerTypeAdapter(LocalDate.class,
                    (JsonSerializer<LocalDate>) (value, type, context) -> new JsonPrimitive(value.format(DATE_FORMAT)))
            .registerTypeAdapter(LocalDate.class,
                    (JsonDeserializer<LocalDate>) (json, type, context) -> LocalDate.parse(json.getAsString(), DATE_FORMAT))
            .registerTypeAdapter(LocalTime.class,
                    (JsonSerializer<LocalTime>) (value, type, context) -> new JsonPrimitive(value.format(TIME_FORMAT)))
            .registerTypeAdapter(LocalTime.class,
                    (JsonDeserializer<LocalTime>) (json, type, context) -> LocalTime.parse(json.getAsString(), TIME_FORMAT))
            .registerTypeAdapter(Instant.class,
                    (JsonSerializer<Instant>) (value, type, context) -> new JsonPrimitive(value.toString()))
            .registerTypeAdapter(Instant.class,
                    (JsonDeserializer<Instant>) (json, type, context) -> Instant.parse(json.getAsString()))
            .create();

    private Json() {
    }

    public static Gson gson() {
        return GSON;
    }
}
