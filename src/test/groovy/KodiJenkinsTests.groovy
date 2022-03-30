import com.cloudbees.plugins.credentials.CredentialsScope
import com.cloudbees.plugins.credentials.SystemCredentialsProvider
import com.tikal.jenkins.plugins.multijob.MultiJobBuild
import com.tikal.jenkins.plugins.multijob.MultiJobParametersAction
import com.tikal.jenkins.plugins.multijob.MultiJobProject

import hudson.model.ParameterValue
import hudson.model.Result
import hudson.model.StringParameterValue
import hudson.plugins.groovy.Groovy
import hudson.plugins.groovy.StringScriptSource
import hudson.util.Secret
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript
import org.jenkinsci.plugins.scriptsecurity.scripts.ClasspathEntry
import org.junit.Rule
import org.junit.Test
import org.jvnet.hudson.plugins.groovypostbuild.GroovyPostbuildRecorder
import org.jvnet.hudson.test.JenkinsRule

import org.jenkinsci.plugins.preSCMbuildstep.PreSCMBuildStepsWrapper
import hudson.tasks.BuildStep;

class KodiJenkinsTests {

  @Rule
    public JenkinsRule j = new JenkinsRule()

  @Test
    void testBuildMultiPR() {
        MultiJobProject p = j.createProject(MultiJobProject, 'root')

        File prebuild = new File('scripts/BuildMulti-PR/prebuild.groovy')

        Groovy before = new Groovy(new StringScriptSource(prebuild.text), '', '', '', '', '', '')

        ArrayList<BuildStep> steps = []
        steps.add(before)

        PreSCMBuildStepsWrapper buildSteps = new PreSCMBuildStepsWrapper(steps, true)

        p.buildWrappersList.add(buildSteps);

        File postbuild = new File('scripts/BuildMulti-PR/postbuild.groovy')

        p.publishersList.add(new GroovyPostbuildRecorder(
          new SecureGroovyScript(
            postbuild.text,
            false,
            Collections.<ClasspathEntry>emptyList()
          ),
          2,
          false
        ))

        String credentialId = '13e4b3a0-8fff-4dc6-b283-677ebd4604e3'
        String credentialDescription = 'Test Secret Text'
        Secret credentialSecret = Secret.fromString('password')

        StringCredentialsImpl cred = new StringCredentialsImpl(
          CredentialsScope.GLOBAL, credentialId, credentialDescription, credentialSecret
        )

        SystemCredentialsProvider.instance.credentials.add(cred)
        SystemCredentialsProvider.instance.save()

        assert(SystemCredentialsProvider.instance.credentials.empty == false)

        List<ParameterValue> params = []
        params.add(new StringParameterValue('ghprbPullId', '12345'))
        params.add(new StringParameterValue('GITHUB_ORG', 'lrusak'))
        params.add(new StringParameterValue('GITHUB_REPO', 'xbmc'))

        MultiJobParametersAction actions = new MultiJobParametersAction(params)

        MultiJobBuild b = p.scheduleBuild2(0, actions).get()

        j.assertBuildStatus(Result.SUCCESS, b)
    }
}
