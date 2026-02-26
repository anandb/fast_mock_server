package io.github.anandb.mockserver;

import java.io.File;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.apache.commons.lang3.StringUtils.isBlank;

@SpringBootApplication
public class MockServerApplication {

    public static void main(String[] args) {
        // If running as a container, see if a configuration has been copied/mounted
        File file = new File("/.dockerenv");
        if (file.exists() && isBlank(System.getProperty("mock.server.config.file", ""))) {
            System.setProperty("mock.server.config.file", "/server.jsonmc");
        }

        SpringApplication.run(MockServerApplication.class, args);
    }
}
