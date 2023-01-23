package me.detj.alt.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.io.File;
import java.io.IOException;

@Value
@Jacksonized
@Builder
public class ConfigData {

    private final int port;
    private final String cookieNameAuth;
    private final String cookieNameSignature;
    private final String duration;
    private final String passwordHash;

    public static ConfigData readFromFile(File file) throws IOException {
        return new ObjectMapper(new YAMLFactory()).readValue(file, ConfigData.class);
    }
}
