package de.dlr.ivf.urmo;

import de.dlr.ivf.urmo.router.modes.Modes;
import de.dlr.ivf.urmo.router.spring.configuration.NetConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(NetConfiguration.class)
@ConfigurationPropertiesScan
public class UrMoAcSpringbootApplication {

    public static void main(String[] args) {

        Modes.init();
        SpringApplication.run(UrMoAcSpringbootApplication.class, args);
    }

}
