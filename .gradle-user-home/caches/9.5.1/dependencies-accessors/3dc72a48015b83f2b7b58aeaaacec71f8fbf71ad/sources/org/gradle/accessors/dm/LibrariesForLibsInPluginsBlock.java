package org.gradle.accessors.dm;

import org.jspecify.annotations.NullMarked;
import org.gradle.api.artifacts.MinimalExternalModuleDependency;
import org.gradle.plugin.use.PluginDependency;
import org.gradle.api.artifacts.ExternalModuleDependencyBundle;
import org.gradle.api.artifacts.MutableVersionConstraint;
import org.gradle.api.provider.Provider;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.internal.catalog.AbstractExternalDependencyFactory;
import org.gradle.api.internal.catalog.DefaultVersionCatalog;
import java.util.Map;
import org.gradle.api.internal.attributes.AttributesFactory;
import org.gradle.api.internal.artifacts.dsl.CapabilityNotationParser;
import javax.inject.Inject;
import org.gradle.api.GradleException;

/**
 * A catalog of dependencies accessible via the {@code libs} extension.
 */
@NullMarked
public class LibrariesForLibsInPluginsBlock extends AbstractExternalDependencyFactory {

    private final AbstractExternalDependencyFactory owner = this;
    private final CommonsLibraryAccessors laccForCommonsLibraryAccessors = new CommonsLibraryAccessors(owner);
    private final FlatlafLibraryAccessors laccForFlatlafLibraryAccessors = new FlatlafLibraryAccessors(owner);
    private final JacksonLibraryAccessors laccForJacksonLibraryAccessors = new JacksonLibraryAccessors(owner);
    private final JavaxLibraryAccessors laccForJavaxLibraryAccessors = new JavaxLibraryAccessors(owner);
    private final JaxbLibraryAccessors laccForJaxbLibraryAccessors = new JaxbLibraryAccessors(owner);
    private final Log4jLibraryAccessors laccForLog4jLibraryAccessors = new Log4jLibraryAccessors(owner);
    private final LogbackLibraryAccessors laccForLogbackLibraryAccessors = new LogbackLibraryAccessors(owner);
    private final OrgLibraryAccessors laccForOrgLibraryAccessors = new OrgLibraryAccessors(owner);
    private final PoiLibraryAccessors laccForPoiLibraryAccessors = new PoiLibraryAccessors(owner);
    private final RadianceLibraryAccessors laccForRadianceLibraryAccessors = new RadianceLibraryAccessors(owner);
    private final Slf4jLibraryAccessors laccForSlf4jLibraryAccessors = new Slf4jLibraryAccessors(owner);
    private final VersionAccessors vaccForVersionAccessors = new VersionAccessors(providers, config);
    private final BundleAccessors baccForBundleAccessors = new BundleAccessors(objects, providers, config, attributesFactory, capabilityNotationParser);
    private final PluginAccessors paccForPluginAccessors = new PluginAccessors(providers, config);

    @Inject
    public LibrariesForLibsInPluginsBlock(DefaultVersionCatalog config, ProviderFactory providers, ObjectFactory objects, AttributesFactory attributesFactory, CapabilityNotationParser capabilityNotationParser) {
        super(config, providers, objects, attributesFactory, capabilityNotationParser);
    }

    /**
     * Dependency provider for <b>flamingo</b> with <b>org.pushingpixels:flamengo</b> coordinates and
     * with version reference <b>flamingo</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getFlamingo() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Dependency provider for <b>forms</b> with <b>com.jgoodies:jgoodies-forms</b> coordinates and
     * with version <b>1.9.0</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getForms() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Dependency provider for <b>groovy</b> with <b>org.codehaus.groovy:groovy</b> coordinates and
     * with version reference <b>groovy</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getGroovy() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Dependency provider for <b>itext</b> with <b>com.lowagie:itext</b> coordinates and
     * with version reference <b>itext</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getItext() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Dependency provider for <b>jasperreports</b> with <b>net.sf.jasperreports:jasperreports</b> coordinates and
     * with version reference <b>jasperreports</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getJasperreports() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Dependency provider for <b>jfreechart</b> with <b>org.jfree:jfreechart</b> coordinates and
     * with version reference <b>jfreechart</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getJfreechart() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Dependency provider for <b>mpxj</b> with <b>net.sf.mpxj:mpxj</b> coordinates and
     * with version reference <b>mpxj</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getMpxj() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Dependency provider for <b>pdfbox</b> with <b>org.apache.pdfbox:pdfbox</b> coordinates and
     * with version reference <b>pdfbox</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getPdfbox() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Group of libraries at <b>commons</b>
     */
    public CommonsLibraryAccessors getCommons() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Group of libraries at <b>flatlaf</b>
     */
    public FlatlafLibraryAccessors getFlatlaf() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Group of libraries at <b>jackson</b>
     */
    public JacksonLibraryAccessors getJackson() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Group of libraries at <b>javax</b>
     */
    public JavaxLibraryAccessors getJavax() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Group of libraries at <b>jaxb</b>
     */
    public JaxbLibraryAccessors getJaxb() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Group of libraries at <b>log4j</b>
     */
    public Log4jLibraryAccessors getLog4j() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Group of libraries at <b>logback</b>
     */
    public LogbackLibraryAccessors getLogback() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Group of libraries at <b>org</b>
     */
    public OrgLibraryAccessors getOrg() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Group of libraries at <b>poi</b>
     */
    public PoiLibraryAccessors getPoi() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Group of libraries at <b>radiance</b>
     */
    public RadianceLibraryAccessors getRadiance() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Group of libraries at <b>slf4j</b>
     */
    public Slf4jLibraryAccessors getSlf4j() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Group of versions at <b>versions</b>
     */
    public VersionAccessors getVersions() {
        return vaccForVersionAccessors;
    }

    /**
     * Group of bundles at <b>bundles</b>
     */
    public BundleAccessors getBundles() {
        throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
    }

    /**
     * Group of plugins at <b>plugins</b>
     */
    public PluginAccessors getPlugins() {
        return paccForPluginAccessors;
    }

    public static class CommonsLibraryAccessors extends SubDependencyFactory {

        public CommonsLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>beanutils</b> with <b>commons-beanutils:commons-beanutils</b> coordinates and
         * with version reference <b>commons.beanutils</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getBeanutils() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

        /**
         * Dependency provider for <b>collections</b> with <b>commons-collections:commons-collections</b> coordinates and
         * with version reference <b>commons.collections</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getCollections() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

        /**
         * Dependency provider for <b>collections4</b> with <b>org.apache.commons:commons-collections4</b> coordinates and
         * with version reference <b>commons.collections4</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getCollections4() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

        /**
         * Dependency provider for <b>digester</b> with <b>commons-digester:commons-digester</b> coordinates and
         * with version reference <b>commons.digester</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getDigester() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

        /**
         * Dependency provider for <b>lang</b> with <b>commons-lang:commons-lang</b> coordinates and
         * with version reference <b>commons.lang</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getLang() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

        /**
         * Dependency provider for <b>lang3</b> with <b>org.apache.commons:commons-lang3</b> coordinates and
         * with version reference <b>commons.lang3</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getLang3() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

        /**
         * Dependency provider for <b>logging</b> with <b>commons-logging:commons-logging</b> coordinates and
         * with version reference <b>commons.logging</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getLogging() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

        /**
         * Dependency provider for <b>pool</b> with <b>commons-pool:commons-pool</b> coordinates and
         * with version reference <b>commons.pool</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getPool() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

    }

    public static class FlatlafLibraryAccessors extends SubDependencyFactory implements DependencyNotationSupplier {

        public FlatlafLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>flatlaf</b> with <b>com.formdev:flatlaf</b> coordinates and
         * with version reference <b>flatlaf</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> asProvider() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

        /**
         * Dependency provider for <b>extras</b> with <b>com.formdev:flatlaf-extras</b> coordinates and
         * with version reference <b>flatlaf</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getExtras() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

    }

    public static class JacksonLibraryAccessors extends SubDependencyFactory {

        public JacksonLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>annotations</b> with <b>com.fasterxml.jackson.core:jackson-annotations</b> coordinates and
         * with version reference <b>jackson.annotations</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getAnnotations() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

        /**
         * Dependency provider for <b>core</b> with <b>com.fasterxml.jackson.core:jackson-core</b> coordinates and
         * with version reference <b>jackson</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getCore() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

        /**
         * Dependency provider for <b>databind</b> with <b>com.fasterxml.jackson.core:jackson-databind</b> coordinates and
         * with version reference <b>jackson</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getDatabind() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

    }

    public static class JavaxLibraryAccessors extends SubDependencyFactory {
        private final JavaxActivationLibraryAccessors laccForJavaxActivationLibraryAccessors = new JavaxActivationLibraryAccessors(owner);
        private final JavaxJaxbLibraryAccessors laccForJavaxJaxbLibraryAccessors = new JavaxJaxbLibraryAccessors(owner);

        public JavaxLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>javax.activation</b>
         */
        public JavaxActivationLibraryAccessors getActivation() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

        /**
         * Group of libraries at <b>javax.jaxb</b>
         */
        public JavaxJaxbLibraryAccessors getJaxb() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

    }

    public static class JavaxActivationLibraryAccessors extends SubDependencyFactory {

        public JavaxActivationLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>api</b> with <b>javax.activation:javax.activation-api</b> coordinates and
         * with version reference <b>javax.activation</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getApi() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

    }

    public static class JavaxJaxbLibraryAccessors extends SubDependencyFactory {

        public JavaxJaxbLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>api</b> with <b>javax.xml.bind:jaxb-api</b> coordinates and
         * with version reference <b>javax.jaxb</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getApi() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

    }

    public static class JaxbLibraryAccessors extends SubDependencyFactory {

        public JaxbLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>runtime</b> with <b>org.glassfish.jaxb:jaxb-runtime</b> coordinates and
         * with version reference <b>jaxb.runtime</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getRuntime() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

    }

    public static class Log4jLibraryAccessors extends SubDependencyFactory {

        public Log4jLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>core</b> with <b>org.apache.logging.log4j:log4j-core</b> coordinates and
         * with version reference <b>log4j</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getCore() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

    }

    public static class LogbackLibraryAccessors extends SubDependencyFactory {

        public LogbackLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>classic</b> with <b>ch.qos.logback:logback-classic</b> coordinates and
         * with version reference <b>logback</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getClassic() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

    }

    public static class OrgLibraryAccessors extends SubDependencyFactory {
        private final OrgNetbeansLibraryAccessors laccForOrgNetbeansLibraryAccessors = new OrgNetbeansLibraryAccessors(owner);

        public OrgLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>org.netbeans</b>
         */
        public OrgNetbeansLibraryAccessors getNetbeans() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

    }

    public static class OrgNetbeansLibraryAccessors extends SubDependencyFactory {
        private final OrgNetbeansSwingLibraryAccessors laccForOrgNetbeansSwingLibraryAccessors = new OrgNetbeansSwingLibraryAccessors(owner);

        public OrgNetbeansLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>org.netbeans.swing</b>
         */
        public OrgNetbeansSwingLibraryAccessors getSwing() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

    }

    public static class OrgNetbeansSwingLibraryAccessors extends SubDependencyFactory {

        public OrgNetbeansSwingLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>outline</b> with <b>org.netbeans.api:org-netbeans-swing-outline</b> coordinates and
         * with version reference <b>netbeans</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getOutline() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

    }

    public static class PoiLibraryAccessors extends SubDependencyFactory implements DependencyNotationSupplier {

        public PoiLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>poi</b> with <b>org.apache.poi:poi</b> coordinates and
         * with version reference <b>poi</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> asProvider() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

        /**
         * Dependency provider for <b>ooxml</b> with <b>org.apache.poi:poi-ooxml</b> coordinates and
         * with version reference <b>poi</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getOoxml() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

    }

    public static class RadianceLibraryAccessors extends SubDependencyFactory {

        public RadianceLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>neon</b> with <b>org.pushing-pixels:radiance-neon</b> coordinates and
         * with version reference <b>radiance</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getNeon() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

        /**
         * Dependency provider for <b>trident</b> with <b>org.pushing-pixels:radiance-trident</b> coordinates and
         * with version reference <b>radiance</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getTrident() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

    }

    public static class Slf4jLibraryAccessors extends SubDependencyFactory {

        public Slf4jLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>api</b> with <b>org.slf4j:slf4j-api</b> coordinates and
         * with version reference <b>slf4j</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getApi() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

    }

    public static class VersionAccessors extends VersionFactory  {

        private final CommonsVersionAccessors vaccForCommonsVersionAccessors = new CommonsVersionAccessors(providers, config);
        private final JacksonVersionAccessors vaccForJacksonVersionAccessors = new JacksonVersionAccessors(providers, config);
        private final JavaxVersionAccessors vaccForJavaxVersionAccessors = new JavaxVersionAccessors(providers, config);
        private final JaxbVersionAccessors vaccForJaxbVersionAccessors = new JaxbVersionAccessors(providers, config);
        public VersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>flamingo</b> with value <b>5.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getFlamingo() { return getVersion("flamingo"); }

        /**
         * Version alias <b>flatlaf</b> with value <b>3.7.1</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getFlatlaf() { return getVersion("flatlaf"); }

        /**
         * Version alias <b>groovy</b> with value <b>2.4.21</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getGroovy() { return getVersion("groovy"); }

        /**
         * Version alias <b>itext</b> with value <b>2.1.7</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getItext() { return getVersion("itext"); }

        /**
         * Version alias <b>jasperreports</b> with value <b>6.21.5</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getJasperreports() { return getVersion("jasperreports"); }

        /**
         * Version alias <b>jfreechart</b> with value <b>1.5.6</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getJfreechart() { return getVersion("jfreechart"); }

        /**
         * Version alias <b>log4j</b> with value <b>2.24.3</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getLog4j() { return getVersion("log4j"); }

        /**
         * Version alias <b>logback</b> with value <b>1.5.18</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getLogback() { return getVersion("logback"); }

        /**
         * Version alias <b>mpxj</b> with value <b>11.5.4</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getMpxj() { return getVersion("mpxj"); }

        /**
         * Version alias <b>netbeans</b> with value <b>RELEASE290</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getNetbeans() { return getVersion("netbeans"); }

        /**
         * Version alias <b>pdfbox</b> with value <b>3.0.7</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getPdfbox() { return getVersion("pdfbox"); }

        /**
         * Version alias <b>poi</b> with value <b>5.5.1</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getPoi() { return getVersion("poi"); }

        /**
         * Version alias <b>radiance</b> with value <b>4.5.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getRadiance() { return getVersion("radiance"); }

        /**
         * Version alias <b>slf4j</b> with value <b>2.0.17</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getSlf4j() { return getVersion("slf4j"); }

        /**
         * Group of versions at <b>versions.commons</b>
         */
        public CommonsVersionAccessors getCommons() {
            return vaccForCommonsVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.jackson</b>
         */
        public JacksonVersionAccessors getJackson() {
            return vaccForJacksonVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.javax</b>
         */
        public JavaxVersionAccessors getJavax() {
            return vaccForJavaxVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.jaxb</b>
         */
        public JaxbVersionAccessors getJaxb() {
            return vaccForJaxbVersionAccessors;
        }

    }

    public static class CommonsVersionAccessors extends VersionFactory  {

        public CommonsVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>commons.beanutils</b> with value <b>1.9.4</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getBeanutils() { return getVersion("commons.beanutils"); }

        /**
         * Version alias <b>commons.collections</b> with value <b>3.2.2</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getCollections() { return getVersion("commons.collections"); }

        /**
         * Version alias <b>commons.collections4</b> with value <b>4.4</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getCollections4() { return getVersion("commons.collections4"); }

        /**
         * Version alias <b>commons.digester</b> with value <b>1.8.1</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getDigester() { return getVersion("commons.digester"); }

        /**
         * Version alias <b>commons.lang</b> with value <b>2.6</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getLang() { return getVersion("commons.lang"); }

        /**
         * Version alias <b>commons.lang3</b> with value <b>3.18.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getLang3() { return getVersion("commons.lang3"); }

        /**
         * Version alias <b>commons.logging</b> with value <b>1.3.5</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getLogging() { return getVersion("commons.logging"); }

        /**
         * Version alias <b>commons.pool</b> with value <b>1.6</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getPool() { return getVersion("commons.pool"); }

    }

    public static class JacksonVersionAccessors extends VersionFactory  implements VersionNotationSupplier {

        public JacksonVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>jackson</b> with value <b>2.22.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> asProvider() { return getVersion("jackson"); }

        /**
         * Version alias <b>jackson.annotations</b> with value <b>2.22</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getAnnotations() { return getVersion("jackson.annotations"); }

    }

    public static class JavaxVersionAccessors extends VersionFactory  {

        public JavaxVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>javax.activation</b> with value <b>1.2.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getActivation() { return getVersion("javax.activation"); }

        /**
         * Version alias <b>javax.jaxb</b> with value <b>2.3.1</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getJaxb() { return getVersion("javax.jaxb"); }

    }

    public static class JaxbVersionAccessors extends VersionFactory  {

        public JaxbVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>jaxb.runtime</b> with value <b>2.3.9</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getRuntime() { return getVersion("jaxb.runtime"); }

    }

    public static class BundleAccessors extends BundleFactory {
        private final CommonsBundleAccessors baccForCommonsBundleAccessors = new CommonsBundleAccessors(objects, providers, config, attributesFactory, capabilityNotationParser);

        public BundleAccessors(ObjectFactory objects, ProviderFactory providers, DefaultVersionCatalog config, AttributesFactory attributesFactory, CapabilityNotationParser capabilityNotationParser) { super(objects, providers, config, attributesFactory, capabilityNotationParser); }

        /**
         * Dependency bundle provider for <b>jackson</b> which contains the following dependencies:
         * <ul>
         *    <li>com.fasterxml.jackson.core:jackson-annotations</li>
         *    <li>com.fasterxml.jackson.core:jackson-core</li>
         *    <li>com.fasterxml.jackson.core:jackson-databind</li>
         * </ul>
         * <p>
         * This bundle was declared in catalog libs.versions.toml
         */
        public Provider<ExternalModuleDependencyBundle> getJackson() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

        /**
         * Dependency bundle provider for <b>jaxb</b> which contains the following dependencies:
         * <ul>
         *    <li>javax.activation:javax.activation-api</li>
         *    <li>javax.xml.bind:jaxb-api</li>
         *    <li>org.glassfish.jaxb:jaxb-runtime</li>
         * </ul>
         * <p>
         * This bundle was declared in catalog libs.versions.toml
         */
        public Provider<ExternalModuleDependencyBundle> getJaxb() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

        /**
         * Dependency bundle provider for <b>logging</b> which contains the following dependencies:
         * <ul>
         *    <li>org.slf4j:slf4j-api</li>
         *    <li>ch.qos.logback:logback-classic</li>
         * </ul>
         * <p>
         * This bundle was declared in catalog libs.versions.toml
         */
        public Provider<ExternalModuleDependencyBundle> getLogging() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

        /**
         * Dependency bundle provider for <b>poi</b> which contains the following dependencies:
         * <ul>
         *    <li>org.apache.poi:poi</li>
         *    <li>org.apache.poi:poi-ooxml</li>
         * </ul>
         * <p>
         * This bundle was declared in catalog libs.versions.toml
         */
        public Provider<ExternalModuleDependencyBundle> getPoi() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

        /**
         * Group of bundles at <b>bundles.commons</b>
         */
        public CommonsBundleAccessors getCommons() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

    }

    public static class CommonsBundleAccessors extends BundleFactory {

        public CommonsBundleAccessors(ObjectFactory objects, ProviderFactory providers, DefaultVersionCatalog config, AttributesFactory attributesFactory, CapabilityNotationParser capabilityNotationParser) { super(objects, providers, config, attributesFactory, capabilityNotationParser); }

        /**
         * Dependency bundle provider for <b>commons.legacy</b> which contains the following dependencies:
         * <ul>
         *    <li>commons-beanutils:commons-beanutils</li>
         *    <li>commons-collections:commons-collections</li>
         *    <li>commons-digester:commons-digester</li>
         *    <li>commons-lang:commons-lang</li>
         *    <li>org.apache.commons:commons-lang3</li>
         *    <li>commons-logging:commons-logging</li>
         *    <li>commons-pool:commons-pool</li>
         * </ul>
         * <p>
         * This bundle was declared in catalog libs.versions.toml
         */
        public Provider<ExternalModuleDependencyBundle> getLegacy() {
            throw new GradleException("Accessing libraries or bundles from version catalogs in the plugins block is not allowed. Only use versions or plugins from catalogs in the plugins block.");
        }

    }

    public static class PluginAccessors extends PluginFactory {

        public PluginAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

    }

}
