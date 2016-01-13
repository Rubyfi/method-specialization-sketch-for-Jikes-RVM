/*
 *  This file is part of the Jikes RVM project (http://jikesrvm.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License. You
 *  may obtain a copy of the License at
 *
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  See the COPYRIGHT.txt file distributed with this work for information
 *  regarding copyright ownership.
 */
package org.jikesrvm.compilers.opt.specialization;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jikesrvm.VM;

public class SpecializationDecisionShutdownHook implements Runnable {

  private final File outputFile;

  /**
   * Specialization does not stop when shutdown sequence is initiated.
   * Therefore, a collection that can deal with concurrent modification
   * is needed, e.g. a {@link ConcurrentLinkedQueue}.
   */
  private Queue<SpecializationDecision> decisions;

  public SpecializationDecisionShutdownHook(String fileName) {
    outputFile = new File(fileName);

    decisions = new ConcurrentLinkedQueue<SpecializationDecision>();

    boolean created = false;
    try {
      created = outputFile.createNewFile();
    } catch (IOException e) {
      if (!created) {
        VM.sysWriteln("COULD NOT CREATE FILE!");
        VM.sysWriteln(outputFile.getAbsolutePath());
      }
      e.printStackTrace();
    }
  }

  public void addDecision(SpecializationDecision decision) {
    decisions.add(decision);
  }

  @Override
  public void run() {
    FileWriter fileWriter = null;
    BufferedWriter bfw = null;
    try {
      fileWriter = new FileWriter(outputFile);
      bfw = new BufferedWriter(fileWriter);

      bfw.write("Begin of specialization decisions.\n");

      for (SpecializationDecision decision : decisions) {
        bfw.write(decision.toString());
      }

      bfw.write("End of specialization decisions.\n");
      bfw.flush();
    } catch (Exception e) {
      VM.sysWriteln("Exception during writing of specialization decisions!");
      e.printStackTrace();
    } finally {
      if (bfw != null) {
        try {
          bfw.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      } else if (fileWriter != null) {
        try {
          fileWriter.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

  }

}
