package com.swisscom.ais.itext7aisdemo.config;

import com.swisscom.ais.itext7.client.AisClient;
import com.swisscom.ais.itext7.client.config.AisClientConfiguration;
import com.swisscom.ais.itext7.client.config.LogbackConfiguration;
import com.swisscom.ais.itext7.client.impl.AisClientImpl;
import com.swisscom.ais.itext7.client.model.UserData;
import com.swisscom.ais.itext7.client.model.VerboseLevel;
import com.swisscom.ais.itext7.client.rest.RestClientConfiguration;
import com.swisscom.ais.itext7.client.rest.SignatureRestClient;
import com.swisscom.ais.itext7.client.rest.SignatureRestClientImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class AisConfig {

  @Value("${swisscom.ais-client.signature.staticClaimedIdentityKey}")
  private String staticClaimedIdentityKey;

  @Bean
  @ConfigurationProperties(prefix = "swisscom.ais-client")
  public Properties properties() {
    return new Properties();
  }

  @Bean
  public SignatureRestClient signatureRestClient(Properties properties) {
    RestClientConfiguration restConfig = new RestClientConfiguration().fromProperties(properties).build();
    return new SignatureRestClientImpl().withConfiguration(restConfig);
  }

  @Bean
  public AisClientConfiguration aisClientConfiguration(Properties properties) {
    return new AisClientConfiguration().fromProperties(properties).build();
  }

  @Bean(destroyMethod = "close")
  public AisClient aisClient(AisClientConfiguration aisConfig, SignatureRestClient restClient) {
    return new AisClientImpl(aisConfig, restClient);
  }

  @Bean("OnDemandUserData")
  public UserData userData(Properties properties) {
    return new UserData()
        .fromProperties(properties)
        .withConsentUrlCallback((consentUrl, userData1) -> System.out.println("Consent URL: " + consentUrl))
        .build();
  }

  @Bean("StaticUserData")
  public UserData staticUserData(Properties properties) {
    return new UserData()
        .fromProperties(properties)
        .withClaimedIdentityKey(staticClaimedIdentityKey)
        .withConsentUrlCallback((consentUrl, userData1) -> System.out.println("Consent URL: " + consentUrl))
        .build();
  }

  @Bean
  public LogbackConfiguration logbackConfiguration() {
    LogbackConfiguration logbackConfiguration = new LogbackConfiguration();
    logbackConfiguration.initialize(VerboseLevel.BASIC);
    return logbackConfiguration;
  }
}
