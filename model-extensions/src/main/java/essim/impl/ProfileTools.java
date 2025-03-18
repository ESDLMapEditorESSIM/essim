package essim.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.*;

@Slf4j
public class ProfileTools {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String USERNAME_PROP = "username";
    private static final String PASSWORD_PROP = "password";
    private ProfileTools() {}

    public record DBCredentials(String username, String password) {
    }

    public static DBCredentials getDBCredentials(String serverId) {
        final File propFile = new File("credentials.json");

        log.debug("Looking for credentials in {}", propFile.getAbsolutePath());

        if (propFile.exists()) {
            try (InputStream is = new FileInputStream(propFile)) {
                JsonNode root = objectMapper.readTree(is);

                if (root.has(serverId)) {
                    JsonNode server = root.get(serverId);

                    if (server.has(USERNAME_PROP) && server.has(PASSWORD_PROP)) {
                        log.debug("Using database credentials read from config file {}.", propFile);
                        return new DBCredentials(server.get(USERNAME_PROP).asText(), server.get(PASSWORD_PROP).asText());
                    } else {
                        log.info(
                                "Configuration for server found in {}, but missing fields {} and/or {}. Not using authentication",
                                propFile, USERNAME_PROP, PASSWORD_PROP);
                    }
                } else {
                    log.info("Property file {} found, but no credential info for this server, not using authentication",
                            propFile);
                }
            } catch (FileNotFoundException e) {
                log.warn("Unable to load config {}: {}", propFile, e.getMessage());
            } catch (IOException e) {
                log.error("Exception while reading config {}: {}", propFile, e.getMessage());
            }
        }

        return null;

    }
}
