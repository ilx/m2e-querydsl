/**
 * (C) Ivica Loncar
 * License: Eclipse Public License
 */
package org.maven.ide.querydsl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactScopeEnum;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.repository.metadata.ArtifactMetadata;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.maven.ide.querydsl.internal.AnnotationServiceLocator;
import org.maven.ide.querydsl.internal.MavenFacade;
import org.maven.ide.querydsl.internal.AnnotationServiceLocator.ServiceEntry;
import org.maven.ide.querydsl.internal.MessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Loncar
 */
public class QueryDslConfiguration {
    private final Logger log = LoggerFactory.getLogger( AptBuildParticipant.class );
    
    private static final String PROCESSOR_JDO         = "com.mysema.query.apt.jdo.JDOAnnotationProcessor";
    private static final String PROCESSOR_JPA         = "com.mysema.query.apt.jpa.JPAAnnotationProcessor";
    private static final String PROCESSOR_HIBERNATE   = "com.mysema.query.apt.hibernate.HibernateAnnotationProcessor";
    private static final String PROCESSOR_COLLECTIONS = "com.mysema.query.apt.QuerydslAnnotationProcessor";

    private final Plugin        m_plugin;
    private String              m_outputDirectory     = "target/generated-sources";
    private String              m_annotationProcessor = "";
    private final String        m_queryDslVersion;
    private ArtifactMetadata[]  m_processorArtifacts  = new ArtifactMetadata[0];
    private MavenFacade         m_mavenFacade;

    public QueryDslConfiguration(final Plugin p_mavenPlugin, final String p_queryDslVersion, MavenFacade p_mavenFacade) throws CoreException {
        m_plugin = p_mavenPlugin;
        m_queryDslVersion = p_queryDslVersion;
        m_mavenFacade = p_mavenFacade;
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

    private void configure() throws CoreException {
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
                put(PROCESSOR_HIBERNATE, new ArtifactMetadata("com.mysema.querydsl", "querydsl-jpa", m_queryDslVersion, "jar",
                        ArtifactScopeEnum.compile, "apt-hibernate-one-jar"));
                put(PROCESSOR_JDO, new ArtifactMetadata("com.mysema.querydsl", "querydsl-jdo", m_queryDslVersion, "jar", ArtifactScopeEnum.compile,
                        "apt-one-jar"));
                put(PROCESSOR_JPA, new ArtifactMetadata("com.mysema.querydsl", "querydsl-jpa", m_queryDslVersion, "jar", ArtifactScopeEnum.compile,
                        "apt-one-jar"));
            }
        };

        // aggregate plugin executions (can there be more than one?):
        List<PluginExecution> executions = m_plugin.getExecutions();

        List<ArtifactMetadata> processorArtifacts = new ArrayList<ArtifactMetadata>();

        for (PluginExecution pluginExecution : executions) {
            Xpp3Dom configuration = (Xpp3Dom) pluginExecution.getConfiguration();

            if (configuration == null) {
                String msg = MessageUtils.info(m_plugin, "Plugin execution does not have configuration element.");
                throw new IllegalStateException(msg);
            }

            Xpp3Dom outputDirectoryElement = configuration.getChild("outputDirectory");
            if (outputDirectoryElement != null) {
                m_outputDirectory = outputDirectoryElement.getValue();
            }

            Xpp3Dom processorElement = configuration.getChild("processor");
            if (processorElement != null) {
                m_annotationProcessor = processorElement.getValue();
                if (null == m_annotationProcessor) {
                    String msg = MessageUtils.info(m_plugin, "Configuration element does not define processor value."); 
                    throw new IllegalStateException(msg);
                }
                ArtifactMetadata artifactMetadata = MAP_PROCESSORS_TO_ARTIFACTMETADATA.get(m_annotationProcessor);
                if (null != artifactMetadata) {
                    processorArtifacts.add(artifactMetadata);
                }
                else {
                    // search explicit plugin dependencies
                    ArtifactMetadata processorArtifactMetadata = searchDependencies(m_annotationProcessor);
                    if (null != processorArtifactMetadata) {
                        processorArtifacts.add(processorArtifactMetadata);
                    }
                    else {
                        String msg = MessageUtils.info(m_plugin, "Unable to find artifact metadata for processor '%s'", m_annotationProcessor);
                        log.warn(msg);
                        // Alternative: just add defined dependencies and let us hope everything will work
                        
                        // we can't handle this scenario
                        throw new IllegalStateException(msg);
                    }
                    // finally: add all plugin dependencies
                    processorArtifacts.addAll(pluginDependenciesAsArtifacts());
                }
            }
            else {
                String msg = MessageUtils.info(m_plugin, "Configuration element does not define processor."); 
                throw new IllegalStateException(msg);
            }

        }

        m_processorArtifacts = processorArtifacts.toArray(new ArtifactMetadata[0]);
    }

    
    private List<ArtifactMetadata> pluginDependenciesAsArtifacts() {
        List<ArtifactMetadata> retval = new ArrayList<ArtifactMetadata>(); 
        List<Dependency> dependencies = m_plugin.getDependencies();
        for (Dependency dependency : dependencies) {
            ArtifactMetadata artifactMetaData = toArtifactMetaData(dependency);
            retval.add(artifactMetaData);
        }
        return retval;
    }

    private ArtifactMetadata searchDependencies(String p_annotationProcessor) throws CoreException {
        List<Dependency> dependencies = m_plugin.getDependencies();

        ArtifactMetadata processorArtifactMetadata = null;
        for (Dependency dependency : dependencies) {
            Artifact artifact = m_mavenFacade.resolve(dependency);
            if (artifactContainsProcessor(artifact, p_annotationProcessor)) {
                processorArtifactMetadata = toArtifactMetaData(dependency);
            }
        }

        return processorArtifactMetadata;
    }

    private boolean artifactContainsProcessor(Artifact p_artifact, String p_annotationProcessor) throws CoreException {
        File jarFile = p_artifact.getFile();
        try {
            Set<ServiceEntry> serviceEntries = AnnotationServiceLocator.getAptServiceEntries(jarFile);
            for (ServiceEntry serviceEntry : serviceEntries) {
                // ILX: this might be buggy!
                if (serviceEntry.getServiceProviderClassName().equals(p_annotationProcessor)) {
                    return true;
                }
            }
        }
        catch (IOException ex) {
            final String msg = String.format("Unable to scan jar '%s' for annotation processors. ", jarFile.getAbsolutePath());
            Status status = new Status(Status.ERROR, "org.maven.ide.querydsl", msg, ex);
            throw new CoreException(status);
        }
        
        // ILX: we could even scan jar file for class. NOT implemented. Just return false.
        
        return false;
    }

    private ArtifactMetadata toArtifactMetaData(Dependency p_dependency) {
        ArtifactMetadata artifactMetadata = new ArtifactMetadata(p_dependency.getGroupId(), p_dependency.getArtifactId(),
                p_dependency.getVersion(), p_dependency.getType(),
                ArtifactScopeEnum.valueOf(p_dependency.getScope()), p_dependency.getClassifier());
        return artifactMetadata;
    }

}