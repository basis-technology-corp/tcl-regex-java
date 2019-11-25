/*
* Copyright 2014 Basis Technology Corp.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.basistech.tclre.osgitest;

import com.basistech.tclre.HsrePattern;
import com.basistech.tclre.PatternFlags;
import com.basistech.tclre.RePattern;
import com.basistech.tclre.Utils;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

/**
 * Test that the OSGi bundle works.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class OsgiBundleIT extends Utils {

    private String getDependencyVersion(String groupId, String artifactId) {
        URL depPropsUrl = Resources.getResource("META-INF/maven/dependencies.properties");
        Properties depProps = new Properties();
        try {
            depProps.load(Resources.asByteSource(depPropsUrl).openStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return (String)depProps.get(String.format("%s/%s/version", groupId, artifactId));
    }

    @Configuration
    public Option[] config() {
        String projectBuildDirectory = System.getProperty("project.build.directory");
        String projectVersion = System.getProperty("project.version");

        List<String> bundleUrls = Lists.newArrayList();
        File bundleDir = new File(projectBuildDirectory, "bundles");
        File[] bundleFiles = bundleDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        });
        for (File bundleFile : bundleFiles) {
            try {
                bundleUrls.add(bundleFile.toURI().toURL().toExternalForm());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        bundleUrls.add(String.format("file:%s/tcl-regex-%s.jar", projectBuildDirectory, projectVersion));

        String[] bundles = bundleUrls.toArray(new String[0]);
        return options(
                provision(bundles),
                systemPackages(
                        // These are needed for guava.
                        "sun.misc",
                        "javax.annotation",
                        String.format("org.slf4j;version=\"%s\"", getDependencyVersion("org.slf4j", "slf4j-api"))

                ),
                junitBundles(),
                systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("WARN")
        );
    }

    @Test
    public void onceOver() throws Exception {
        // using ranges ensures that the ICU4J connection works right.
        RePattern exp = HsrePattern.compile("[^a][\u4e00-\uf000][[:upper:]][^\ufeff][\u4e00-\u4e10]b.c.d",
                PatternFlags.ADVANCED, PatternFlags.EXPANDED);
        assertTrue(exp.matcher("Q\u4e01A$\u4e09bGcHd").matches());
    }
}
