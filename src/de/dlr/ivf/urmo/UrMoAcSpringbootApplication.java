package de.dlr.ivf.urmo;

import de.dlr.ivf.urmo.router.spring.properties.NetworkProperty;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(NetworkProperty.class)
public class UrMoAcSpringbootApplication {

    public static void main(String[] args) {
        SpringApplication.run(UrMoAcSpringbootApplication.class, args);
    }

}
