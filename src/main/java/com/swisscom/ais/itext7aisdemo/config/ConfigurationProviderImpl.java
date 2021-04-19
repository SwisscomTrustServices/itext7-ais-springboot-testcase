package com.swisscom.ais.itext7aisdemo.config;

import com.swisscom.ais.itext7.client.common.provider.ConfigurationProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ConfigurationProviderImpl implements ConfigurationProvider {

  private final Environment environment;

  public ConfigurationProviderImpl(Environment environment) {
    this.environment = environment;
  }

  @Override
  public String getProperty(String name) {
    return environment.getProperty("swisscom.ais-client." + name);
  }
}
