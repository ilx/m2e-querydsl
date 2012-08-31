package org.maven.ide.querydsl.internal;

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.metadata.ArtifactMetadata;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;

public class MavenFacade {
	private ProjectConfigurationRequest m_configurationRequest;
	private IProgressMonitor m_monitor;

	public MavenFacade(ProjectConfigurationRequest p_configurationRequest, IProgressMonitor p_monitor) {
		this.m_configurationRequest = p_configurationRequest;
		this.m_monitor = p_monitor;
	}
	
	
	public List<ArtifactRepository> getRemoteArtifactRepositories() {
        List<ArtifactRepository> remoteArtifactRepositories = m_configurationRequest.getMavenProject().getRemoteArtifactRepositories();
        return remoteArtifactRepositories;
	}

	public MavenProject getMavenProject() throws CoreException {
        IMavenProjectFacade projectFacade = m_configurationRequest.getMavenProjectFacade();
        MavenProject mavenProject = projectFacade.getMavenProject(m_monitor);
        return mavenProject;
	}
	
	public Artifact resolve(ArtifactMetadata p_artifactMetadata) throws CoreException {
		IMaven maven = MavenPlugin.getMaven();
		List<ArtifactRepository> remoteArtifactRepositories = getRemoteArtifactRepositories();
        Artifact artifact = maven.resolve(p_artifactMetadata.getGroupId(), p_artifactMetadata.getArtifactId(), p_artifactMetadata.getVersion(), p_artifactMetadata.getType(),
                p_artifactMetadata.getClassifier(), remoteArtifactRepositories , m_monitor);
        return artifact;
	}
	
	public Artifact resolve(Dependency dependecy) throws CoreException {
		IMaven maven = MavenPlugin.getMaven();
		List<ArtifactRepository> remoteArtifactRepositories = getRemoteArtifactRepositories();
		Artifact artifact = maven.resolve(dependecy.getGroupId(), dependecy.getArtifactId(), dependecy.getVersion(), dependecy.getType(), dependecy.getClassifier(), remoteArtifactRepositories, m_monitor);
		
		return artifact;
	}
}
