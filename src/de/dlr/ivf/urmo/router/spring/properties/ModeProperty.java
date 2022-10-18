package de.dlr.ivf.urmo.router.spring.properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("urmoac.mode")
public class ModeProperty {

    String modes;
}
