package org.maven.ide.querydsl;

import java.io.File;
import java.util.Set;

import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.configurator.MojoExecutionBuildParticipant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * @author Michael Glauche (http://glauche.de/)
 */
public class AptBuildParticipant extends MojoExecutionBuildParticipant
{
    private final Logger log = LoggerFactory.getLogger( AptBuildParticipant.class );

    public AptBuildParticipant( final MojoExecution execution )
    {
        super( execution, true );
    }

    @Override
    public Set<IProject> build( final int kind, final IProgressMonitor monitor )
    throws Exception
    {
        IMaven maven = MavenPlugin.getMaven();
        BuildContext buildContext = getBuildContext();

        // check if any of the grammar files changed
        /*
        File source = maven.getMojoParameterValue(getSession(), getMojoExecution(), "sourceDirectory", File.class);
        Scanner ds = buildContext.newScanner( source ); // delta or full scanner
        ds.scan();
        String[] includedFiles = ds.getIncludedFiles();
        if (includedFiles == null || includedFiles.length <= 0 )
        {
            return null;
        }
         */

        // execute mojo
        log.info("About to start plugin");
        Set<IProject> result = super.build( kind, monitor );
        log.info("done.");
        // tell m2e builder to refresh generated files
        File generated = maven.getMojoParameterValue(getSession(), getMojoExecution(), "outputDirectory", File.class);
        if (generated != null) {
            buildContext.refresh( generated );
        }
        log.info("did refresh: " + generated);
        return result;
    }
}