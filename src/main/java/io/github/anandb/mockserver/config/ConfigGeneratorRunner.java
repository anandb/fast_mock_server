package io.github.anandb.mockserver.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * No-op runner. All CLI options (-g, -l, -f, -h) are handled in
 * {@code MockServerApplication.main()} before the Spring context starts.
 */
@Component
public class ConfigGeneratorRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(ConfigGeneratorRunner.class);

    @Override
    public void run(ApplicationArguments args) {
        // Nothing to do — all flag handling is in main()
    }
}
