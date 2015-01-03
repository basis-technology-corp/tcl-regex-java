/******************************************************************************
 ** This data and information is proprietary to, and a valuable trade secret
 ** of, Basis Technology Corp.  It is given in confidence by Basis Technology
 ** and may only be used as permitted under the license agreement under which
 ** it has been distributed, and in no other way.
 **
 ** Copyright (c) 2014 Basis Technology Corporation All rights reserved.
 **
 ** The technical data and information provided herein are provided with
 ** `limited rights', and the computer software provided herein is provided
 ** with `restricted rights' as those terms are defined in DAR and ASPR
 ** 7-104.9(a).
 ******************************************************************************/

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

        String[] bundles = bundleUrls.toArray(new String[bundleUrls.size()]);
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
