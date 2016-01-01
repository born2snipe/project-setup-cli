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
import cli.pi.command.CliCommand;
import cli.pi.io.InputRequestor;
import com.github.born2snipe.project.setup.io.DirectoryCreator;
import com.github.born2snipe.project.setup.io.MustacheTemplateRenderer;
import com.github.born2snipe.project.setup.scm.GitRepoInitializer;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.commons.io.IOUtils;
import org.openide.util.lookup.ServiceProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static cli.pi.io.InputRequestor.YesOrNo.NO;
import static cli.pi.io.InputRequestor.YesOrNo.YES;

@ServiceProvider(service = CliCommand.class)
public class MavenCommand extends CliCommand {
    private MustacheTemplateRenderer templateRenderer = new MustacheTemplateRenderer();
    private InputRequestor inputRequestor = new InputRequestor();
    private GitRepoInitializer gitRepoInitializer = new GitRepoInitializer();
    private DirectoryCreator directoryCreator = new DirectoryCreator();

    public MavenCommand() {
        argsParser.addArgument("-o", "--output")
                .help("The output directory")
                .dest("output");
    }

    @Override
    public String getName() {
        return "maven";
    }

    @Override
    public String getDescription() {
        return "Setup a Maven project";
    }

    @Override
    protected void executeParsedArgs(CliLog cliLog, Namespace namespace) {
        String projectName = inputRequestor.askForRequiredInput("What is the project's name?");
        String projectVersion = inputRequestor.askForInput("What is the project's version?", "0.0.1-SNAPSHOT");
        InputRequestor.YesOrNo includeShadePlugin = inputRequestor.askYesOrNoQuestion("Would you like to build an UBER jar?", NO);
        InputRequestor.YesOrNo openSource = inputRequestor.askYesOrNoQuestion("Will this be an open source project?", NO);

        File outputDir = new File((String) namespace.get("output"));
        File projectDir = new File(outputDir, projectName);
        projectDir.mkdirs();

        buildOutProductionDirectories(projectDir, cliLog);
        buildOutTestDirectories(projectDir, cliLog);
        writeScmIgnoreFile(projectDir, cliLog);

        if (openSource == YES) {
            writeLicenseFile(projectDir, cliLog);
        }

        PomData pomData = new PomData();
        pomData.projectName = projectName;
        pomData.projectVersion = projectVersion;
        pomData.shadePlugin = includeShadePlugin == YES;
        pomData.openSource = openSource == YES;

        writePomFile(projectDir, pomData, cliLog);

        cliLog.info("Initializing git repo...");
        gitRepoInitializer.initIn(projectDir);

        cliLog.info("@|green,bold New project created in directory @ |@" + projectDir.getAbsolutePath());
    }

    private void writeLicenseFile(File projectDir, CliLog cliLog) {
        cliLog.info("Creating LICENSE.txt file...");
        InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream("license/apache-2.txt");
        File licenseFile = new File(projectDir, "LICENSE.txt");

        OutputStream output = null;
        try {
            output = new FileOutputStream(licenseFile);
            IOUtils.copy(input, output);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(output);
        }
    }

    private void writeScmIgnoreFile(File projectDir, CliLog cliLog) {
        cliLog.info("Creating .gitignore file...");
        File ignoreFile = new File(projectDir, ".gitignore");
        templateRenderer.render("gitignore.mustache", ignoreFile);
    }

    private void buildOutTestDirectories(File projectDir, CliLog cliLog) {
        cliLog.info("Building test code directories...");
        directoryCreator.createDirectoriesIn(projectDir, "src/test/java", "src/test/resources");
    }

    private void buildOutProductionDirectories(File projectDir, CliLog cliLog) {
        cliLog.info("Building production code directories...");
        directoryCreator.createDirectoriesIn(projectDir, "src/main/java", "src/main/resources");
    }

    private void writePomFile(File projectDir, PomData data, CliLog cliLog) {
        cliLog.info("Generating pom file...");
        File pomFile = new File(projectDir, "pom.xml");
        templateRenderer.render("maven/pom.mustache", data, pomFile);
    }
}
