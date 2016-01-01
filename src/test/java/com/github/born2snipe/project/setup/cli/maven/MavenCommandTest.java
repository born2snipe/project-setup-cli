/**
 *
 * Copyright to the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
package com.github.born2snipe.project.setup.cli.maven;

import cli.pi.CliLog;
import cli.pi.io.InputRequestor;
import com.github.born2snipe.project.setup.io.MustacheTemplateRenderer;
import com.github.born2snipe.project.setup.scm.GitRepoInitializer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;

import static cli.pi.io.InputRequestor.YesOrNo.NO;
import static cli.pi.io.InputRequestor.YesOrNo.YES;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MavenCommandTest {
    private static final String PROJECT_NAME = "project-name";
    private static final String PROJECT_VERSION = "1.0";
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();
    @Mock
    private InputRequestor inputRequestor;
    @Mock
    private CliLog log;
    @Mock
    private GitRepoInitializer gitRepoInitializer;
    @Spy
    private MustacheTemplateRenderer mustacheTemplateRenderer;
    @InjectMocks
    private MavenCommand command;
    private File outputDir;
    private File pomFile;
    private File projectDir;
    private File gitIgnoreFile;

    @Before
    public void setUp() throws Exception {
        outputDir = temp.newFolder("output");
        projectDir = new File(outputDir, PROJECT_NAME);
        pomFile = new File(projectDir, "pom.xml");
        gitIgnoreFile = new File(projectDir, ".gitignore");

        when(inputRequestor.askForRequiredInput("What is the project's name?")).thenReturn(PROJECT_NAME);
        when(inputRequestor.askForInput("What is the project's version?", "0.0.1-SNAPSHOT")).thenReturn(PROJECT_VERSION);
        when(inputRequestor.askYesOrNoQuestion("Would you like to build an UBER jar?", NO)).thenReturn(YES);
        when(inputRequestor.askYesOrNoQuestion("Will this be an open source project?", NO)).thenReturn(YES);
    }

    @Test
    public void shouldNotIncludeTheLicenseFileWhenProjectIsNotOpenSource() {
        whenUserSaysProjectIsNotOpenSource();

        executeCommand();

        assertFileDoesNotExistInProject("LICENSE.txt");
    }

    @Test
    public void shouldIncludeTheLicenseFileWhenProjectIsOpenSource() {
        executeCommand();

        assertFileExistsInProject("LICENSE.txt");
    }

    @Test
    public void shouldIncludeTheExtraArgsToTheReleasePluginWhenProjectIsOpenSource() {
        executeCommand();

        String args = "-Psonatype-oss-release -D=gpg.keyname=${env.MAVEN_PGP_KEYNAME} -Dgpg.passphrase=${env.MAVEN_PGP_PASSPHRASE}";
        AssertXml.assertTextAt(args, pomFile, "//plugins/plugin/configuration/arguments");
    }

    @Test
    public void shouldNotIncludeTheExtraArgsToTheReleasePluginWhenProjectIsNotOpenSource() {
        whenUserSaysProjectIsNotOpenSource();

        executeCommand();

        AssertXml.assertTextAt("", pomFile, "//plugins/plugin/configuration/arguments");
    }

    @Test
    public void shouldIncludeTheLicenseTagWhenProjectIsOpenSource() {
        executeCommand();

        AssertXml.assertElementExist("Apache License, Version 2.0", pomFile, "//licenses/license/name");
    }

    @Test
    public void shouldNotIncludeTheLicenseTagWhenProjectIsNotOpenSource() {
        whenUserSaysProjectIsNotOpenSource();

        executeCommand();

        AssertXml.assertElementDoesExist("Apache License, Version 2.0", pomFile, "//licenses/license/name");
    }

    @Test
    public void shouldIncludeTheLicensePluginWhenProjectIsOpenSource() {
        executeCommand();

        AssertXml.assertElementExist("maven-license-plugin", pomFile, "//plugin/artifactId");
    }

    @Test
    public void shouldNotIncludeTheLicensePluginWhenProjectIsNotOpenSource() {
        whenUserSaysProjectIsNotOpenSource();

        executeCommand();

        AssertXml.assertElementDoesExist("maven-license-plugin", pomFile, "//plugin/artifactId");
    }

    @Test
    public void shouldIncludeTheSonatypeParentPomWhenProjectIsOpenSource() {
        executeCommand();

        AssertXml.assertElementExist("oss-parent", pomFile, "/project/parent/artifactId");
    }

    @Test
    public void shouldNotIncludeTheSonatypeParentPomWhenProjectIsNotOpenSource() {
        whenUserSaysProjectIsNotOpenSource();

        executeCommand();

        AssertXml.assertElementDoesExist("oss-parent", pomFile, "/project/parent/artifactId");
    }

    @Test
    public void shouldIncludeTheShadePluginWhenAnUberJarIsWanted() {
        executeCommand();

        AssertXml.assertElementExist("maven-shade-plugin", pomFile, "//plugin/artifactId");
    }

    @Test
    public void shouldNotIncludeTheShadePluginWhenNoUberJarIsWanted() {
        when(inputRequestor.askYesOrNoQuestion("Would you like to build an UBER jar?", NO)).thenReturn(NO);

        executeCommand();

        AssertXml.assertElementDoesExist("maven-shade-plugin", pomFile, "//plugin/artifactId");
    }

    @Test
    public void shouldInitializeTheGitRepo() {
        InOrder inOrder = inOrder(mustacheTemplateRenderer, gitRepoInitializer);

        executeCommand();

        inOrder.verify(mustacheTemplateRenderer).render("gitignore.mustache", gitIgnoreFile);
        inOrder.verify(mustacheTemplateRenderer).render(eq("maven/pom.mustache"), isA(PomData.class), eq(pomFile));
        inOrder.verify(gitRepoInitializer).initIn(projectDir);
    }

    @Test
    public void shouldPopulateTheProjectsVersionNumber() {
        executeCommand();

        AssertXml.assertTextAt(PROJECT_VERSION, pomFile, "/project/version");
    }

    @Test
    public void shouldSetupADefaultGitIgnoreFile() {
        executeCommand();

        assertFileExistsInProject(".gitignore");
    }

    @Test
    public void shouldBuildOutTheProjectStructure() {
        executeCommand();

        assertFolderExistsInProject("src/main/java");
        assertFolderExistsInProject("src/main/resources");
        assertFolderExistsInProject("src/test/java");
        assertFolderExistsInProject("src/test/resources");
    }

    @Test
    public void shouldSaveThePomToTheOutputDirectory() {
        executeCommand();

        assertTrue(pomFile.exists());
    }

    @Test
    public void shouldPopulateThePomFileWithTheProjectName() throws Exception {
        executeCommand();

        AssertXml.assertTextAt(PROJECT_NAME, pomFile, "/project/artifactId");
        AssertXml.assertTextAt(PROJECT_NAME, pomFile, "/project/name");
        AssertXml.assertTextAt(PROJECT_NAME, pomFile, "/project/description");
        AssertXml.assertTextAt("https://github.com/born2snipe/" + PROJECT_NAME, pomFile, "/project/url");
        AssertXml.assertTextAt("scm:git:git@github.com:born2snipe/" + PROJECT_NAME + ".git", pomFile, "/project/scm/connection");
        AssertXml.assertTextAt("scm:git:git@github.com:born2snipe/" + PROJECT_NAME + ".git", pomFile, "/project/scm/developerConnection");
        AssertXml.assertTextAt("git@github.com:born2snipe/" + PROJECT_NAME + ".git", pomFile, "/project/scm/url");
    }

    private void executeCommand() {
        command.execute(log, "-o", outputDir.getAbsolutePath());
    }

    private void assertFileDoesNotExistInProject(String path) {
        File file = new File(projectDir, path);
        assertFalse(file.exists());
    }

    private void assertFolderExistsInProject(String path) {
        File directory = new File(projectDir, path);
        assertTrue(directory.exists());
        assertTrue(directory.isDirectory());
    }

    private void assertFileExistsInProject(String path) {
        File file = new File(projectDir, path);
        assertTrue(file.exists());
        assertTrue(file.isFile());
    }

    private void whenUserSaysProjectIsNotOpenSource() {
        when(inputRequestor.askYesOrNoQuestion("Will this be an open source project?", NO)).thenReturn(NO);
    }
}