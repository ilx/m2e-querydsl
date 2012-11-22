/**
 * (C) Ivica Loncar
 * License: Eclipse Public License
 */
package org.maven.ide.querydsl;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.metadata.ArtifactMetadata;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.apt.core.util.AptConfig;
import org.eclipse.jdt.apt.core.util.IFactoryPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.jdt.AbstractJavaProjectConfigurator;
import org.maven.ide.querydsl.internal.MavenFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Loncar
 */
public class AptConfigurator extends AbstractJavaProjectConfigurator {
	private static final String M2_REPO = "M2_REPO";
	private static final Logger log = LoggerFactory.getLogger(AptConfigurator.class);

    //    /**
    //     * @author Michael Glauche (http://glauche.de/)
    //     */
    //    @Override
    //    public AbstractBuildParticipant getBuildParticipant(final IMavenProjectFacade projectFacade, final MojoExecution execution, final IPluginExecutionMetadata executionMetadata) {
    //        return new AptBuildParticipant(execution);
    //    }

    /**
     * @author Ivica Loncar
     */
    @Override
    public void configure(final ProjectConfigurationRequest p_request, final IProgressMonitor p_monitor) throws CoreException {
        super.configure(p_request, p_monitor);

        IProject project = p_request.getProject();
        if (project.hasNature(JavaCore.NATURE_ID)) {
            // enable annotation processing
            IJavaProject javaProject = JavaCore.create(project);
            AptConfig.setEnabled(javaProject, true);

            MavenFacade mavenFacade = new MavenFacade(p_request, p_monitor);            
            // Associate annotation processor jar. Since querydsl doesn't use generic apt plugin nor specifies extra dependencies we have to hardcode it.
            QueryDslConfiguration queryDslConfiguration = getQueryDslConfiguration(mavenFacade);
            // IRepositoryRegistry repositoryRegistry = MavenPlugin.getRepositoryRegistry();
            // List<IRepository> repositories = repositoryRegistry.getRepositories(IRepositoryRegistry.SCOPE_LOCAL | IRepositoryRegistry.SCOPE_SETTINGS | IRepositoryRegistry.SCOPE_PROJECT);

            IMaven maven = MavenPlugin.getMaven();

            IPath m2RepoVar = JavaCore.getClasspathVariable(M2_REPO);
            IFactoryPath factoryPath = AptConfig.getFactoryPath(javaProject);

            ArtifactMetadata[] artifactsMetadata = queryDslConfiguration.getProcessorArtifacts();
            for (ArtifactMetadata artifactMetadata : artifactsMetadata) {
            	Artifact artifact = mavenFacade.resolve(artifactMetadata);

                File file = artifact.getFile();
                if ((file == null) || !file.exists() || !file.canRead()) {
                    throw new IllegalStateException("Cannot find file for artifact " + artifact + " file:" + file);
                }
                String filePath = file.getAbsolutePath();
                IPath path = Path.fromOSString(filePath);
                
                IPath relativeToM2Repo = path.makeRelativeTo(m2RepoVar);
                
                IPath jarPath = Path.fromOSString("M2_REPO").append(relativeToM2Repo);
                
                factoryPath.addVarJar(jarPath);
            }

            AptConfig.setFactoryPath(javaProject, factoryPath);

            Map<String, String> options = new HashMap<String, String>() {
                {
                    // we would like to override existing files
                    put("defaultOverride", "true");
                }
            };
            AptConfig.setProcessorOptions(options, javaProject);

            AptConfig.setGenSrcDir(javaProject, queryDslConfiguration.getOutputDirectory());
        }
    }

    private QueryDslConfiguration getQueryDslConfiguration(final MavenFacade p_mavenFacade) throws CoreException {
    	MavenProject mavenProject = p_mavenFacade.getMavenProject();

        // find querydsl version so we can resolve matching one-jar from repo
        String queryDslVersion = "0.0.0";
        List<Dependency> dependencies = mavenProject.getDependencies();
        for (Dependency dependency : dependencies) {
            if ("com.mysema.querydsl".equals(dependency.getGroupId())) {
                queryDslVersion = dependency.getVersion();
                break;
            }
        }

        Plugin queryDslPlugin = mavenProject.getPlugin("com.mysema.maven:maven-apt-plugin");
        QueryDslConfiguration queryDslConfiguration = new QueryDslConfiguration(queryDslPlugin, queryDslVersion, p_mavenFacade);

        return queryDslConfiguration;
    }

}