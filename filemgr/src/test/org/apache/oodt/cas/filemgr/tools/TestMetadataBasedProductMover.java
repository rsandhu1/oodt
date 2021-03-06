/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.apache.oodt.cas.filemgr.tools;

//OODT imports
import org.apache.oodt.cas.filemgr.ingest.StdIngester;
import org.apache.oodt.cas.filemgr.metadata.CoreMetKeys;
import org.apache.oodt.cas.filemgr.system.XmlRpcFileManager;
import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.cas.metadata.SerializableMetadata;

//JDK imports
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;

//Junit imports
import junit.framework.TestCase;

/**
 * @author mattmann
 * @version $Revision$
 * 
 * <p>
 * Describe your class here
 * </p>.
 */
public class TestMetadataBasedProductMover extends TestCase {

    private static final int FM_PORT = 50010;

    private XmlRpcFileManager fm;

    private String luceneCatLoc;

    private static final String transferServiceFacClass = "org.apache.oodt.cas."
            + "filemgr.datatransfer.LocalDataTransferFactory";

    private String testPathSpec = "/tmp/[MimeType]/[ProductStructure]/[CAS.ProductName]/[Filename]";

    private String expectedLoc = "/tmp/text/plain/Flat/test.txt/test.txt";

    private MetadataBasedProductMover mover;

    public TestMetadataBasedProductMover() {
    }

    public void testMoveProducts() {
        try {
            mover.moveProducts("GenericFile");
        } catch (Exception e) {
            fail(e.getMessage());
        }

        // make sure that there is a product at expectedLoc
        assertTrue(new File(expectedLoc).exists());

    }

    private void ingestTestFile() {
        Metadata prodMet = null;
        StdIngester ingester = new StdIngester(transferServiceFacClass);

        try {
            prodMet = new SerializableMetadata(new FileInputStream(
                    "./src/testdata/ingest/test.txt.met"));

            // now add the right file location
            prodMet.addMetadata(CoreMetKeys.FILE_LOCATION, new File(
                    "./src/testdata/ingest").getCanonicalPath());
            ingester.ingest(new URL("http://localhost:" + FM_PORT), new File(
                    "./src/testdata/ingest/test.txt"), prodMet);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        startXmlRpcFileManager();
        ingestTestFile();
        try {
            mover = new MetadataBasedProductMover(testPathSpec,
                    "http://localhost:" + FM_PORT);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        fm.shutdown();
        fm = null;

        // blow away lucene cat
        deleteAllFiles(luceneCatLoc);

        // blow away test file
        deleteAllFiles("/tmp/test.txt");

        // blow away test file
        deleteAllFilesRecursive("/tmp/text");
    }

    private void deleteAllFilesRecursive(String startDirPath) {
        File startDirFile = new File(startDirPath);
        File[] dirFiles = startDirFile.listFiles();

        if (dirFiles != null && dirFiles.length > 0) {
            for (int i = 0; i < dirFiles.length; i++) {
                if (dirFiles[i].isDirectory()) {
                    deleteAllFilesRecursive(dirFiles[i].getAbsolutePath());
                    // all dir files deleted, now delete dir
                    dirFiles[i].delete();
                } else {
                    dirFiles[i].delete();
                }
            }
        }

        // now delete the root dir
        startDirFile.delete();

    }

    private void deleteAllFiles(String startDir) {
        File startDirFile = new File(startDir);
        File[] delFiles = startDirFile.listFiles();

        if (delFiles != null && delFiles.length > 0) {
            for (int i = 0; i < delFiles.length; i++) {
                delFiles[i].delete();
            }
        }

        startDirFile.delete();

    }

    private void startXmlRpcFileManager() {
        // first make sure to load properties for the file manager
        // and make sure to load logging properties as well

        // set the log levels
        System.setProperty("java.util.logging.config.file", new File(
                "./src/main/resources/logging.properties").getAbsolutePath());

        // first load the example configuration
        try {
            System.getProperties().load(
                    new FileInputStream("./src/main/resources/filemgr.properties"));
        } catch (Exception e) {
            fail(e.getMessage());
        }

        // override the catalog to use: we'll use lucene
        try {
            luceneCatLoc = new File("./src/testdata/ingest/cat")
                    .getCanonicalPath();
        } catch (Exception e) {
            fail(e.getMessage());
        }

        System.setProperty("filemgr.catalog.factory",
                "org.apache.oodt.cas.filemgr.catalog.LuceneCatalogFactory");
        System.setProperty(
                "org.apache.oodt.cas.filemgr.catalog.lucene.idxPath",
                luceneCatLoc);

        // now override the repo mgr policy
        try {
            System.setProperty(
                    "org.apache.oodt.cas.filemgr.repositorymgr.dirs",
                    "file://"
                            + new File("./src/testdata/ingest/fmpolicy")
                                    .getCanonicalPath());
        } catch (Exception e) {
            fail(e.getMessage());
        }

        // now override the val layer ones
        System.setProperty("org.apache.oodt.cas.filemgr.validation.dirs",
                "file://"
                        + new File("./src/main/resources/examples/core")
                                .getAbsolutePath());

        // set up mime repo path
        System.setProperty(
                "org.apache.oodt.cas.filemgr.mime.type.repository", new File(
                        "./src/main/resources/mime-types.xml").getAbsolutePath());

        try {
            fm = new XmlRpcFileManager(FM_PORT);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

}
