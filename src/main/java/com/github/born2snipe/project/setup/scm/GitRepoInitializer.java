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
package com.github.born2snipe.project.setup.scm;

import org.apache.commons.io.IOUtils;

import java.io.File;

public class GitRepoInitializer {
    public void initIn(File projectDir) {
        executeCommandIn(projectDir, "git", "init");
        executeCommandIn(projectDir, "git", "add", ".");
        executeCommandIn(projectDir, "git", "commit", "-m", "'initial'");
    }

    private void executeCommandIn(File projectDir, String... commands) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(commands);
            processBuilder.directory(projectDir);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            System.out.println(IOUtils.toString(process.getInputStream()));
            process.waitFor();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
