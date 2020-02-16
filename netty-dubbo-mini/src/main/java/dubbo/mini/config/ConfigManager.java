package dubbo.mini.config;

import dubbo.mini.common.utils.StringUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigManager {

  private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
  private static final ConfigManager configManager = new ConfigManager();

  private Map<String, ProtocolConfig> protocols = new ConcurrentHashMap<>();
  private Map<String, RegistryConfig> registries = new ConcurrentHashMap<>();
  private Map<String, ProviderConfig> providers = new ConcurrentHashMap<>();
  private Map<String, ConsumerConfig> consumers = new ConcurrentHashMap<>();
  public static final String DEFAULT_KEY = "default";

  public static ConfigManager getInstance() {
    return configManager;
  }

  private ConfigManager() {

  }


  public Optional<ProviderConfig> getProvider(String id) {
    return Optional.ofNullable(providers.get(id));
  }

  public Optional<ProviderConfig> getDefaultProvider() {
    return Optional.ofNullable(providers.get(DEFAULT_KEY));
  }

  public void addProvider(ProviderConfig providerConfig) {
    if (providerConfig == null) {
      return;
    }

    String key = !StringUtils.isEmpty(providerConfig.getId())
        ? providerConfig.getId()
        : (providerConfig.isDefault() == null || providerConfig.isDefault()) ? DEFAULT_KEY : null;

    if (StringUtils.isEmpty(key)) {
      throw new IllegalStateException(
          "A ProviderConfig should either has an id or it's the default one, " + providerConfig);
    }

    if (providers.containsKey(key) && !providerConfig.equals(providers.get(key))) {
      logger.warn(
          "Duplicate ProviderConfig found, there already has one default ProviderConfig or more than two ProviderConfigs have the same id, "
              +
              "you can try to give each ProviderConfig a different id. " + providerConfig);
    } else {
      providers.put(key, providerConfig);
    }
  }

  public Optional<ConsumerConfig> getConsumer(String id) {
    return Optional.ofNullable(consumers.get(id));
  }

  public Optional<ConsumerConfig> getDefaultConsumer() {
    return Optional.ofNullable(consumers.get(DEFAULT_KEY));
  }

  public void addConsumer(ConsumerConfig consumerConfig) {
    if (consumerConfig == null) {
      return;
    }

    String key = StringUtils.isNotEmpty(consumerConfig.getId())
        ? consumerConfig.getId()
        : (consumerConfig.isDefault() == null || consumerConfig.isDefault()) ? DEFAULT_KEY : null;

    if (StringUtils.isEmpty(key)) {
      throw new IllegalStateException(
          "A ConsumerConfig should either has an id or it's the default one, " + consumerConfig);
    }

    if (consumers.containsKey(key) && !consumerConfig.equals(consumers.get(key))) {
      logger.warn(
          "Duplicate ConsumerConfig found, there already has one default ConsumerConfig or more than two ConsumerConfigs have the same id, "
              +
              "you can try to give each ConsumerConfig a different id. " + consumerConfig);
    } else {
      consumers.put(key, consumerConfig);
    }
  }

  public Optional<ProtocolConfig> getProtocol(String id) {
    return Optional.ofNullable(protocols.get(id));
  }

  public Optional<List<ProtocolConfig>> getDefaultProtocols() {
    List<ProtocolConfig> defaults = new ArrayList<>();
    protocols.forEach((k, v) -> {
      if (DEFAULT_KEY.equalsIgnoreCase(k)) {
        defaults.add(v);
      } else if (v.isDefault() == null || v.isDefault()) {
        defaults.add(v);
      }
    });
    return Optional.of(defaults);
  }

  public void addProtocols(List<ProtocolConfig> protocolConfigs) {
    if (protocolConfigs != null) {
      protocolConfigs.forEach(this::addProtocol);
    }
  }

  public void addProtocol(ProtocolConfig protocolConfig) {
    if (protocolConfig == null) {
      return;
    }

    String key = StringUtils.isNotEmpty(protocolConfig.getId())
        ? protocolConfig.getId()
        : (protocolConfig.isDefault() == null || protocolConfig.isDefault()) ? DEFAULT_KEY : null;

    if (StringUtils.isEmpty(key)) {
      throw new IllegalStateException(
          "A ProtocolConfig should either has an id or it's the default one, " + protocolConfig);
    }

    if (protocols.containsKey(key) && !protocolConfig.equals(protocols.get(key))) {
      logger.warn(
          "Duplicate ProtocolConfig found, there already has one default ProtocolConfig or more than two ProtocolConfigs have the same id, "
              +
              "you can try to give each ProtocolConfig a different id. " + protocolConfig);
    } else {
      protocols.put(key, protocolConfig);
    }
  }

  public Optional<RegistryConfig> getRegistry(String id) {
    return Optional.ofNullable(registries.get(id));
  }

  public Optional<List<RegistryConfig>> getDefaultRegistries() {
    List<RegistryConfig> defaults = new ArrayList<>();
    registries.forEach((k, v) -> {
      if (DEFAULT_KEY.equalsIgnoreCase(k)) {
        defaults.add(v);
      } else if (v.isDefault() == null || v.isDefault()) {
        defaults.add(v);
      }
    });
    return Optional.of(defaults);
  }

  public void addRegistries(List<RegistryConfig> registryConfigs) {
    if (registryConfigs != null) {
      registryConfigs.forEach(this::addRegistry);
    }
  }

  public void addRegistry(RegistryConfig registryConfig) {
    if (registryConfig == null) {
      return;
    }

    String key = StringUtils.isNotEmpty(registryConfig.getId())
        ? registryConfig.getId()
        : (registryConfig.isDefault() == null || registryConfig.isDefault()) ? DEFAULT_KEY : null;

    if (StringUtils.isEmpty(key)) {
      throw new IllegalStateException(
          "A RegistryConfig should either has an id or it's the default one, " + registryConfig);
    }

    if (registries.containsKey(key) && !registryConfig.equals(registries.get(key))) {
      logger.warn(
          "Duplicate RegistryConfig found, there already has one default RegistryConfig or more than two RegistryConfigs have the same id, "
              +
              "you can try to give each RegistryConfig a different id. " + registryConfig);
    } else {
      registries.put(key, registryConfig);
    }
  }

  public Map<String, ProtocolConfig> getProtocols() {
    return protocols;
  }

  public Map<String, RegistryConfig> getRegistries() {
    return registries;
  }

  public Map<String, ProviderConfig> getProviders() {
    return providers;
  }

  public Map<String, ConsumerConfig> getConsumers() {
    return consumers;
  }

  public void refreshAll() {
    // refresh all configs here,
    getProtocols().values().forEach(ProtocolConfig::refresh);
    getRegistries().values().forEach(RegistryConfig::refresh);
    getProviders().values().forEach(ProviderConfig::refresh);
    getConsumers().values().forEach(ConsumerConfig::refresh);
  }

  // For test purpose
  public void clear() {
    this.registries.clear();
    this.protocols.clear();
    this.providers.clear();
    this.consumers.clear();
  }

}