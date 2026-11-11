package com.mekongmobile.smartcrm.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * Cấu hình Jackson: Định dạng thời gian chuẩn ISO 8601 kèm múi giờ UTC+7 (Asia/Ho_Chi_Minh).
 * Ví dụ: 2026-11-08T12:14:21+07:00
 */
@Configuration
public class JacksonConfig {

    public static final ZoneId ZONE_VN = ZoneId.of("Asia/Ho_Chi_Minh");
    public static final DateTimeFormatter ISO_OFFSET_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonTimeZoneCustomizer() {
        return builder -> {
            builder.timeZone(TimeZone.getTimeZone(ZONE_VN));
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            // Serializer cho LocalDateTime: xuất chuẩn ISO 8601 kèm múi giờ +07:00
            builder.serializerByType(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
                @Override
                public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                    if (value == null) {
                        gen.writeNull();
                    } else {
                        ZonedDateTime zdt = value.atZone(ZONE_VN);
                        gen.writeString(zdt.format(ISO_OFFSET_FORMATTER));
                    }
                }
            });

            // Deserializer cho LocalDateTime: hỗ trợ cả ISO Offset (+07:00, Z) và ISO Local
            builder.deserializerByType(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
                @Override
                public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                    String text = p.getText();
                    if (text == null || text.isBlank()) {
                        return null;
                    }
                    try {
                        return OffsetDateTime.parse(text).atZoneSameInstant(ZONE_VN).toLocalDateTime();
                    } catch (Exception e) {
                        try {
                            return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        } catch (Exception e2) {
                            return LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        }
                    }
                }
            });
        };
    }
}

