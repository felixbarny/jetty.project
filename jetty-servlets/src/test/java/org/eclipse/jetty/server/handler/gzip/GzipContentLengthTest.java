//
//  ========================================================================
//  Copyright (c) 1995-2014 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.server.handler.gzip;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import javax.servlet.Servlet;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.HttpTester;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.toolchain.test.TestTracker;
import org.eclipse.jetty.toolchain.test.TestingDir;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test the GzipHandler support for Content-Length setting variations.
 *
 * @see <a href="Eclipse Bug 354014">http://bugs.eclipse.org/354014</a>
 */
@RunWith(Parameterized.class)
public class GzipContentLengthTest
{
    @Rule
    public final TestTracker tracker = new TestTracker();
    
    /**
     * These are the junit parameters for running this test.
     * <p>
     * In addition to Jetty's DefaultServlet we have multiple test
     * servlets that arrange content-length/content-type/get stream
     * in different order so as to simulate the real world scenario
     * that caused the bug in Eclipse <a href="Bug 354014">http://bugs.eclipse.org/354014</a>
     * <p>
     * This test case will be run with each of the entries in
     * the array below as setup parameters for the test case.
     *
     * @return the junit parameters
     */
    @Parameters(name="{2}/{1} {0}")
    public static List<Object[]> data()
    {
        return Arrays.asList(new Object[][]
        {
        { TestServletLengthStreamTypeWrite.class},
        { TestServletLengthTypeStreamWrite.class},
        { AsyncTimeoutWrite.class},
        { AsyncScheduledWrite.class},
        { TestServletStreamLengthTypeWrite.class},
        { TestServletStreamLengthTypeWriteWithFlush.class },
        { TestServletStreamTypeLengthWrite.class},
        { TestServletTypeLengthStreamWrite.class},
        { TestServletTypeStreamLengthWrite.class},
        { TestServletBufferTypeLengthWrite.class},
        //{ GzipFilter.class, AsyncTimeoutWrite.class, GzipFilter.GZIP },
        //{ GzipFilter.class, AsyncScheduledWrite.class, GzipFilter.GZIP },
                                                
        { TestServletLengthStreamTypeWrite.class},
        { TestServletLengthTypeStreamWrite.class},
        { TestServletStreamLengthTypeWrite.class},
        { TestServletStreamLengthTypeWriteWithFlush.class},
        { TestServletStreamTypeLengthWrite.class},
        { TestServletTypeLengthStreamWrite.class},
        { TestServletTypeStreamLengthWrite.class},
        
        });
    }

    private static final HttpConfiguration defaultHttp = new HttpConfiguration();
    private static final int LARGE = defaultHttp.getOutputBufferSize() * 8;
    private static final int MEDIUM = defaultHttp.getOutputBufferSize();
    private static final int SMALL = defaultHttp.getOutputBufferSize() / 4;
    private static final int TINY = GzipHandler.DEFAULT_MIN_GZIP_SIZE / 2;

    private String compressionType;

    public GzipContentLengthTest(Class<? extends Servlet> testServlet)
    {
        this.testServlet = testServlet;
        this.compressionType = GzipHandler.GZIP;
    }

    @Rule
    public TestingDir testingdir = new TestingDir();

    private Class<? extends Servlet> testServlet;

    private void assertIsGzipCompressed(String filename, int filesize) throws Exception
    {
        GzipTester tester = new GzipTester(testingdir, compressionType);

        File testfile = tester.prepareServerFile(testServlet.getSimpleName() + "-" + filename,filesize);

        tester.setContentServlet(testServlet);

        try
        {
            tester.start();
            tester.assertIsResponseGzipCompressed("GET",testfile.getName());
        }
        finally
        {
            tester.stop();
        }
    }

    private void assertIsNotGzipCompressed(String filename, int filesize) throws Exception
    {
        GzipTester tester = new GzipTester(testingdir, compressionType);

        File testfile = tester.prepareServerFile(testServlet.getSimpleName() + "-" + filename,filesize);

        tester.setContentServlet(testServlet);

        try
        {
            tester.start();
            HttpTester.Response response = tester.assertIsResponseNotGzipCompressed("GET",testfile.getName(),filesize,HttpStatus.OK_200);
            Assert.assertThat(response.get("ETAG"),Matchers.startsWith("W/etag-"));
        }
        finally
        {
            tester.stop();
        }
    }

    /**
     * Tests gzip compression of a small size file
     */
    @Test
    public void testEmpty() throws Exception
    {
        assertIsNotGzipCompressed("empty.txt",0);
    }
    
    /**
     * Tests gzip compression of a small size file
     */
    @Test
    public void testIsGzipCompressedSmall() throws Exception
    {
        assertIsGzipCompressed("file-small.txt",SMALL);
    }

    /**
     * Tests gzip compression of a medium size file
     */
    @Test
    public void testIsGzipCompressedMedium() throws Exception
    {
        assertIsGzipCompressed("file-med.txt",MEDIUM);
    }

    /**
     * Tests gzip compression of a large size file
     */
    @Test
    public void testIsGzipCompressedLarge() throws Exception
    {
        assertIsGzipCompressed("file-large.txt",LARGE);
    }

    /**
     * Tests for problems with Content-Length header on small size files
     * that are not being compressed encountered when using GzipHandler
     *
     * @see <a href="Eclipse Bug 354014">http://bugs.eclipse.org/354014</a>
     */
    @Test
    public void testIsNotGzipCompressedTiny() throws Exception
    {
        assertIsNotGzipCompressed("file-tiny.txt",TINY);
    }

    /**
     * Tests for problems with Content-Length header on small size files
     * that are not being compressed encountered when using GzipHandler
     *
     * @see <a href="Eclipse Bug 354014">http://bugs.eclipse.org/354014</a>
     */
    @Test
    public void testIsNotGzipCompressedSmall() throws Exception
    {
        assertIsNotGzipCompressed("file-small.mp3",SMALL);
    }

    /**
     * Tests for problems with Content-Length header on medium size files
     * that are not being compressed encountered when using GzipHandler
     *
     * @see <a href="Eclipse Bug 354014">http://bugs.eclipse.org/354014</a>
     */
    @Test
    public void testIsNotGzipCompressedMedium() throws Exception
    {
        assertIsNotGzipCompressed("file-medium.mp3",MEDIUM);
    }

    /**
     * Tests for problems with Content-Length header on large size files
     * that were not being compressed encountered when using GzipHandler
     *
     * @see <a href="Eclipse Bug 354014">http://bugs.eclipse.org/354014</a>
     */
    @Test
    public void testIsNotGzipCompressedLarge() throws Exception
    {
        assertIsNotGzipCompressed("file-large.mp3",LARGE);
    }
}
