package me.detj.alt;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.lambdaworks.crypto.SCryptUtil;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

public class Config {

    private final String passwordHash;
    private final int port;
    private final long durationMillis;
    private final long durationSeconds;

    @JsonCreator
    public Config(
            @JsonProperty("password") String password,
            @JsonProperty("duration") String duration,
            @JsonProperty("port") int port) {

        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port " + port + " is outside of the valid range (1-65535)");
        }

        this.passwordHash = password;
        this.port = port;

        Duration d = Duration.parse(duration);
        durationMillis = d.toMillis();
        durationSeconds = durationMillis / 1000;
    }

    public boolean checkPassword(String password) {
        return SCryptUtil.check(password, passwordHash);
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public int getPort() {
        return port;
    }

    public static Config readFromFile(File file) throws IOException {
        return new ObjectMapper(new YAMLFactory()).readValue(file, Config.class);
    }
}
