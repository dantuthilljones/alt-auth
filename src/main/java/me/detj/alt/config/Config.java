package me.detj.alt.config;

import lombok.Builder;
import lombok.Value;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

@Value
@Builder
public class Config {

    private final int port;
    private final long durationMillis;
    private final String cookieNameAuth;
    private final String cookieNameSignature;
    private final String passwordHash;

    public static Config readFromFile(File file) throws IOException {
        ConfigData data = ConfigData.readFromFile(file);
        return Config.builder()
                .port(data.getPort())
                .durationMillis(durationToMillis(data.getDuration()))
                .cookieNameAuth(data.getCookieNameAuth())
                .cookieNameSignature(data.getCookieNameSignature())
                .passwordHash(data.getPasswordHash())
                .build();
    }

    private static long durationToMillis(String durationStr) {
        Duration duration = Duration.parse(durationStr);
        return duration.toMillis();
    }
}
