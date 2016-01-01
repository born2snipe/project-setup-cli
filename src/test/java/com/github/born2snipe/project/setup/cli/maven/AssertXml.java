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

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileInputStream;

import static org.junit.Assert.assertEquals;

public class AssertXml {
    public static void assertTextAt(String expectedText, File file, String xpathQuery) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);

            XPath xPath = XPathFactory.newInstance().newXPath();
            String text = xPath.compile(xpathQuery).evaluate(doc);
            assertEquals("Text @ " + xpathQuery, expectedText, text);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void assertElementDoesExist(String expectedText, File file, String xpathQuery) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);

            XPath xPath = XPathFactory.newInstance().newXPath();
            NodeList nodes = (NodeList) xPath.compile(xpathQuery).evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++) {
                String text = nodes.item(i).getTextContent();
                if (text.equals(expectedText)) {
                    Assert.fail("We expected to NOT find " + expectedText);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void assertElementExist(String expectedText, File file, String xpathQuery) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);

            XPath xPath = XPathFactory.newInstance().newXPath();
            NodeList nodes = (NodeList) xPath.compile(xpathQuery).evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++) {
                String text = nodes.item(i).getTextContent();
                if (text.equals(expectedText)) {
                    return;
                }
            }
            Assert.fail("We expected to find " + expectedText + "\nFile Contents:\n" + IOUtils.toString(new FileInputStream(file)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
