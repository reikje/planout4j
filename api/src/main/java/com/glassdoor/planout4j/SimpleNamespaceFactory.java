package com.glassdoor.planout4j;

import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Optional;

import com.glassdoor.planout4j.config.Planout4jRepository;
import com.glassdoor.planout4j.config.Planout4jRepositoryImpl;
import com.glassdoor.planout4j.config.ValidationException;
import com.glassdoor.planout4j.util.VersionLogger;

import static com.google.common.base.Preconditions.*;

/**
 * Reads namespace data once and caches forever.
 */
public class SimpleNamespaceFactory implements NamespaceFactory {

    static {
        VersionLogger.log("api");
    }

    private static final Logger LOG = LoggerFactory.getLogger(SimpleNamespaceFactory.class);

    protected final Planout4jRepository planout4jRepository;

    protected volatile Map<String, NamespaceConfig> namespaceName2namespaceConfigMap = Collections.emptyMap();

    public SimpleNamespaceFactory() {
        planout4jRepository = new Planout4jRepositoryImpl();
        namespaceName2namespaceConfigMap = readConfig();
    }

    /**
     * Constructs a namespace. Verifies the input is sane (has entry for the primary unit).
     * @param name Valid namespace name
     * @param paramName2valueMap Map of string parameter names to parameter values
     * @param overrides override parameter names/values to enable "freezing"
     *                  certain computed parameters, e.g. via query string; optional
     * @throws IllegalArgumentException in case of bad input (e.g. invalid namespace name, empty input, etc.)
     */
    @Override
    public Optional<Namespace> getNamespace(final String name, final Map<String, ?> paramName2valueMap, final Map<String, ?> overrides) {
        final Optional<NamespaceConfig> config = getNamespaceConfig(name);
        return config.isPresent() ? Optional.of(new Namespace(config.get(), paramName2valueMap, overrides))
                : Optional.<Namespace>absent();
    }

    /**
     * Constructs a namespace. Verifies the input is sane (has entry for the primary unit).
     * @param name Valid namespace name
     * @param paramName2valueMap Map of string parameter names to parameter values
     * @throws IllegalArgumentException in case of bad input (e.g. invalid namespace name, empty input, etc.)
     */
    @Override
    public Optional<Namespace> getNamespace(final String name, final Map<String, ?> paramName2valueMap) {
        return getNamespace(name, paramName2valueMap, null);
    }

    @Override
    public int getNamespaceCount() {
        return getConfigMap().size();
    }

    @Override
    public Optional<NamespaceConfig> getNamespaceConfig(final String name) {
        final Map<String, NamespaceConfig> configMap = getConfigMap();
        return Optional.fromNullable(configMap.get(name));
    }

    protected final Map<String, NamespaceConfig> getConfigMap() {
       checkNotNull(namespaceName2namespaceConfigMap, "Namespaces should have been loaded during initialization");
       return namespaceName2namespaceConfigMap;
    }
    
    protected final Map<String, NamespaceConfig> readConfig() {
        LOG.debug("loading namespace data...");
        try {
            return planout4jRepository.loadAllNamespaceConfigs();
        } catch (ValidationException e) {
            LOG.error("Failed to (re)load namespace data, see earlier error messages", e);
            return namespaceName2namespaceConfigMap;
        }
    }

}
