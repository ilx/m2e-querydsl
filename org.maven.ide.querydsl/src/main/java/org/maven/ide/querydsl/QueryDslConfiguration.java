/**
 * (C) Ivica Loncar
 * License: Eclipse Public License
 */
package org.maven.ide.querydsl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.ArtifactScopeEnum;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.repository.metadata.ArtifactMetadata;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * @author Ivica Loncar
 */
public class QueryDslConfiguration {

    private static final String          PROCESSOR_JDO          = "com.mysema.query.apt.jdo.JDOAnnotationProcessor";
    private static final String          PROCESSOR_JPA          = "com.mysema.query.apt.jpa.JPAAnnotationProcessor";
    private static final String          PROCESSOR_HIBERNATE    = "com.mysema.query.apt.hibernate.HibernateAnnotationProcessor";
    private static final String          PROCESSOR_COLLECTIONS  = "com.mysema.query.apt.QuerydslAnnotationProcessor";

    private final Plugin                 m_plugin;
    private String                       m_outputDirectory      = "target/generated-sources";
    private String                       m_annotationProcessor  = "";
    private final String        m_queryDslVersion;
    private ArtifactMetadata[]  m_processorArtifacts  = new ArtifactMetadata[0];

    public QueryDslConfiguration(final Plugin p_mavenPlugin, final String p_queryDslVersion) {
        m_plugin = p_mavenPlugin;
        m_queryDslVersion = p_queryDslVersion;
        // init configuration values
        configure();
    }

    public String getQueryDslVersion() {
        return m_queryDslVersion;
    }

    public String getOutputDirectory() {
        return m_outputDirectory;
    }

    public ArtifactMetadata[] getProcessorArtifacts() {
        return m_processorArtifacts;
    }

    public List<Dependency> getDependencies() {
        Set<String> deps = new LinkedHashSet<String>();
        //        collectModules(deps, (Xpp3Dom) plugin.getConfiguration(), names, name);
        return m_plugin.getDependencies();
    }

    private void configure() {
        // determine apt library that should be downloaded based on the
        // type of annotation processor:
        //
        // jdo        : com.mysema.query.apt.jdo.JDOAnnotationProcessor
        // jpa        : com.mysema.query.apt.jpa.JPAAnnotationProcessor
        // hibernate  : com.mysema.query.apt.hibernate.HibernateAnnotationProcessor
        // collections: com.mysema.query.apt.QuerydslAnnotationProcessor
        Map<String, ArtifactMetadata> MAP_PROCESSORS_TO_ARTIFACTMETADATA = new HashMap<String, ArtifactMetadata>() {
            {
                //                put(PROCESSOR_COLLECTIONS, new ArtifactMetadata("com.mysema.querydsl", "querydsl-jdo", m_queryDslVersion, "jar", ArtifactScopeEnum.compile, "apt-one-jar"));
                put(PROCESSOR_HIBERNATE, new ArtifactMetadata("com.mysema.querydsl", "querydsl-jpa", m_queryDslVersion, "jar", ArtifactScopeEnum.compile, "apt-hibernate-one-jar"));
                put(PROCESSOR_JDO, new ArtifactMetadata("com.mysema.querydsl", "querydsl-jdo", m_queryDslVersion, "jar", ArtifactScopeEnum.compile, "apt-one-jar"));
                put(PROCESSOR_JPA, new ArtifactMetadata("com.mysema.querydsl", "querydsl-jpa", m_queryDslVersion, "jar", ArtifactScopeEnum.compile, "apt-one-jar"));
            }
        };

        // aggregate plugin executions (can there be more than one?):
        List<PluginExecution> executions = m_plugin.getExecutions();

        List<ArtifactMetadata> processorArtifacts = new ArrayList<ArtifactMetadata>();

        for (PluginExecution pluginExecution : executions) {
            Xpp3Dom configuration = (Xpp3Dom) pluginExecution.getConfiguration();

            if (configuration == null) {
                String msg = String.format("Plugin %s:%s is not properly configured. Plugin execution does not have configuration element.", m_plugin.getGroupId(),
                        m_plugin.getArtifactId());
                throw new IllegalStateException(msg);
            }

            Xpp3Dom outputDirectoryElement = configuration.getChild("outputDirectory");
            if (outputDirectoryElement != null) {
                m_outputDirectory = outputDirectoryElement.getValue();
            }

            Xpp3Dom processorElement = configuration.getChild("processor");
            if (processorElement != null) {
                m_annotationProcessor = processorElement.getValue();
                ArtifactMetadata artifactMetadata = MAP_PROCESSORS_TO_ARTIFACTMETADATA.get(m_annotationProcessor);
                processorArtifacts.add(artifactMetadata);
            }
        }

        m_processorArtifacts = processorArtifacts.toArray(new ArtifactMetadata[0]);

    }

}