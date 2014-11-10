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

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.AsyncContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class AsyncScheduledWrite extends TestDirContentServlet
{
    private static class DispatchBack implements Runnable
    {
        private final AsyncContext ctx;

        public DispatchBack(AsyncContext ctx)
        {
            this.ctx = ctx;
        }

        @Override
        public void run()
        {
            ctx.dispatch();
        }
    }

    private ScheduledExecutorService scheduler;

    public void init(ServletConfig config) throws ServletException
    {
        super.init(config);
        scheduler = Executors.newScheduledThreadPool(3);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {        
        Boolean suspended = (Boolean)request.getAttribute("SUSPENDED");
        if (suspended==null || !suspended)
        {
            request.setAttribute("SUSPENDED",Boolean.TRUE);
            AsyncContext ctx = request.startAsync();
            ctx.setTimeout(0);
            scheduler.schedule(new DispatchBack(ctx),500,TimeUnit.MILLISECONDS);
        }
        else
        {
            String fileName = request.getServletPath();
            byte[] dataBytes = loadContentFileBytes(fileName);

            response.setContentLength(dataBytes.length);

            ServletOutputStream out = response.getOutputStream();

            if (fileName.endsWith("txt"))
                response.setContentType("text/plain");
            else if (fileName.endsWith("mp3"))
                response.setContentType("audio/mpeg");
            response.setHeader("ETag","W/etag-" + fileName);

            out.write(dataBytes);
        }
    }
}