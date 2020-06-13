/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataWriter;
import org.jacoco.core.data.IExecutionDataVisitor;
import org.jacoco.core.data.ISessionInfoVisitor;
import org.jacoco.core.data.SessionInfo;
import org.jacoco.core.runtime.RemoteControlReader;
import org.jacoco.core.runtime.RemoteControlWriter;

/**
 * This example starts a socket server to collect coverage from agents that run
 * in output mode <code>tcpclient</code>. The collected data is dumped to a
 * local file.
 */
public final class JacocoServer {

  private static final String DESTFILE = "/tmp/jacoco-combined.exec";

  private static final int PORT = 6300;

  private JacocoServer() {
  }

  public static void main(final String[] args) throws IOException {
    final ExecutionDataWriter fileWriter = new ExecutionDataWriter(
        new FileOutputStream(DESTFILE));
    final ServerSocket server = new ServerSocket(PORT, 0,
        InetAddress.getByName("0.0.0.0"));
    while (true) {
      final Handler handler = new Handler(server.accept(), fileWriter);
      new Thread(handler).start();
    }
  }

  private static class Handler
      implements Runnable, ISessionInfoVisitor, IExecutionDataVisitor {

    private final Socket socket;

    private final RemoteControlReader reader;

    private final ExecutionDataWriter fileWriter;

    Handler(final Socket socket, final ExecutionDataWriter fileWriter)
        throws IOException {
      this.socket = socket;
      this.fileWriter = fileWriter;

      // Just send a valid header:
      new RemoteControlWriter(socket.getOutputStream());

      reader = new RemoteControlReader(socket.getInputStream());
      reader.setSessionInfoVisitor(this);
      reader.setExecutionDataVisitor(this);

    }

    @SuppressWarnings("checkstyle:EmptyBlock")
    public void run() {
      try {
        while (reader.read()) {
        }
        socket.close();
        synchronized (fileWriter) {
          fileWriter.flush();
        }
      } catch (final IOException e) {
        e.printStackTrace();
      }
    }

    public void visitSessionInfo(final SessionInfo info) {
      System.out.printf("Retrieving execution Data for session: %s%n",
          info.getId());
      synchronized (fileWriter) {
        fileWriter.visitSessionInfo(info);
      }
    }

    public void visitClassExecution(final ExecutionData data) {
      synchronized (fileWriter) {
        fileWriter.visitClassExecution(data);
      }
    }
  }
}
