package io.github.anandb.mockserver.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * No-op runner. All CLI options (-g, -l, -f, -h) are handled in
 * {@code MockServerApplication.main()} before the Spring context starts.
 */
@Slf4j
@Component
public class ConfigGeneratorRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        // Nothing to do — all flag handling is in main()
    }
}
