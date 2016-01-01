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
package com.github.born2snipe.project.setup.io;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

public class MustacheTemplateRenderer {
    private static final Object NO_DATA = new Object();

    public void render(String template, File outputFile) {
        render(template, NO_DATA, outputFile);
    }

    public void render(String template, Object data, File outputFile) {
        Writer writer = null;
        try {
            writer = new PrintWriter(new FileOutputStream(outputFile));

            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile(template);
            mustache.execute(writer, data);

            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            close(writer);
        }
    }

    private void close(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
