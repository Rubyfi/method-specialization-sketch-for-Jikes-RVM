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
package org.jikesrvm.adaptive.measurements.organizers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jikesrvm.VM;
import org.jikesrvm.adaptive.controller.Controller;
import org.jikesrvm.adaptive.measurements.RuntimeMeasurements;
import org.jikesrvm.adaptive.measurements.listeners.parameterprofiling.EncodingHelper;
import org.jikesrvm.adaptive.measurements.listeners.parameterprofiling.ParameterDecoder;
import org.jikesrvm.adaptive.measurements.listeners.parameterprofiling.ParameterListener;
import org.jikesrvm.adaptive.parameterprofiling.MethodDataProviderImpl;
import org.jikesrvm.adaptive.parameterprofiling.MethodProfile;
import org.jikesrvm.adaptive.parameterprofiling.MethodProfile.CandidateType;
import org.jikesrvm.adaptive.parameterprofiling.ParameterProfile;
import org.jikesrvm.adaptive.parameterprofiling.ParameterProfileInformation;
import org.jikesrvm.classloader.RVMMethod;
import org.jikesrvm.classloader.RVMType;
import org.jikesrvm.classloader.TypeReference;
import org.jikesrvm.compilers.opt.OptimizingCompilerException;
import org.jikesrvm.runtime.Callbacks;
import org.vmmagic.pragma.NonMoving;

/**
 * This class organizes information gained from {@link ParameterListener} to
 * build value and type profiles for methods.
 * <p>
 * TODO Consider cleaning / throwing away data regularly.
 */
@NonMoving
public class ParameterProfileOrganizer extends Organizer {

  private Map<RVMMethod, List<ParameterProfileInformation>> profilesForAllMethods;
  private File fileForProfileInfo;
  private static final boolean DEBUG = false;

  private SpecializedMethodCreater specializedMethodCreater;

  /**
   * If this option is set, all data from the {@link ParameterListener} will be
   * summarized at the method level using {@link MethodProfile}s. Otherwise, the
   * data will be saved in one {@link ParameterProfile} per sample.
   * <p>
   * Note that specialization using {@link ParameterProfile}s is currently not
   * implemented.
   */
  private static final boolean USE_SUMMARIZED_PROFILES = true;

  private static final MethodProfile.CandidateType profileType = CandidateType.ALL;

  /**
   * Constructs a new organizer for parameter profiles.
   */
  public ParameterProfileOrganizer() {
    profilesForAllMethods = new HashMap<RVMMethod, List<ParameterProfileInformation>>();
    int initialCapacity = 64;
    // at most organizer thread and specializer will update
    int concurrencyLevelForUpdates = 2;
    float defaultLoadFactor = 0.75f;


    profilesForAllMethods = new ConcurrentHashMap<RVMMethod, List<ParameterProfileInformation>>(initialCapacity, defaultLoadFactor, concurrencyLevelForUpdates);
  }

  public void reportProfiles() {
    FileWriter fileWriter = null;
    BufferedWriter bfw = null;
    try {
      fileWriter = new FileWriter(fileForProfileInfo);

      bfw = new BufferedWriter(fileWriter);
      bfw.write("My report:\n");
      bfw.write("Parameter profile count: " + profilesForAllMethods.size() + "\n");

      bfw.write("Parameter profiles: ");

      StringBuilder sb = new StringBuilder();
      for (List<ParameterProfileInformation> profilesForASingleMethod : profilesForAllMethods.values()) {
        List<String> sortedProfileList = new ArrayList<String>(profilesForASingleMethod.size());
        for (ParameterProfileInformation paramProfile : profilesForASingleMethod) {
          sortedProfileList.add(paramProfile.toString());
        }
        Collections.sort(sortedProfileList);

        for (String s : sortedProfileList) {
          sb.append(s);
        }
      }
      bfw.write(sb.toString());
      bfw.write("\n");

      bfw.write("DONE WITH PROFILES");
      bfw.flush();
    } catch (SecurityException e) {
      printStackTraceToLog(e);
    } catch (IOException e) {
      printStackTraceToLog(e);
    } finally {
      if (bfw != null)
        try {
          bfw.close();
        } catch (IOException e) {
          printStackTraceToLog(e);
        }
      else {
        if (fileWriter != null)
          try {
            fileWriter.close();
          } catch (IOException e) {
            printStackTraceToLog(e);
          }
      }
    }
  }

  private void printStackTraceToLog(Exception e) {
    for (StackTraceElement ste : e.getStackTrace()) {
      VM.sysWriteln(ste.toString());
    }
  }

  @Override
  public void report() {
    if (Controller.options.LOGGING_LEVEL >= 1) {
      if (listener != null) {
        VM.sysWriteln("\t Report of my listener:");
        listener.report();
      }
    }
  }

  /**
   * Installs a {@link ParameterListener}.
   */
  @Override
  protected void initialize() {
    if (USE_SUMMARIZED_PROFILES == false) {
      String unsupportedMode = "Use of per-sample profiles is not supported!";
      if (VM.VerifyAssertions) {
        VM._assert(USE_SUMMARIZED_PROFILES, unsupportedMode);
      } else {
        OptimizingCompilerException.UNREACHABLE(unsupportedMode);
      }
    }

    ParameterListener paramListener = new ParameterListener();
    listener = paramListener;
    listener.setOrganizer(this);

    specializedMethodCreater = new SpecializedMethodCreater(this);
    Callbacks.addMethodCompileMonitor(specializedMethodCreater);

    RuntimeMeasurements.installCBSContextListenerOnParamTicks(paramListener);

    if (DEBUG) {
      createFileForProfileInfo();
      Runtime.getRuntime().addShutdownHook(new Thread(new ProfileShutdownHook(this)));
    }
  }

  private void createFileForProfileInfo() {
    long timestamp = System.currentTimeMillis();
    DateFormat df = DateFormat.getDateTimeInstance();
    String date = df.format(new Date(timestamp));

    String userHome = System.getProperty("user.home");

    fileForProfileInfo = new File(userHome + "/parameterProfiles_" + date + ".txt");
    boolean created = false;
    try {
      created = fileForProfileInfo.createNewFile();
    } catch (IOException e) {
      if (!created) {
        VM.sysWriteln("COULD NOT CREATE FILE!");
        VM.sysWriteln(fileForProfileInfo.getAbsolutePath());
      }
      e.printStackTrace();
    }
  }

  @Override
  void thresholdReached() {
    ParameterListener paramListener = ((ParameterListener) listener);
    ParameterDecoder decoder = paramListener.getParameterDecoder();
    decoder.switchToDecodeMode();

    RVMMethod[] methods = paramListener.getMethods();
    int[] paramStartIndexes = paramListener.getParamStartIndexes();
    for (int index = 0; index < paramStartIndexes.length; index++) {
      int paramStartIndex = paramStartIndexes[index];
      if (paramStartIndex == ParameterListener.NO_ENTRY) {
        if (DEBUG) {
          VM.sysWriteln("Skipping \"sample\" because no sample was taken!");
        }
        continue;
      }

      RVMMethod method = methods[index];
      if (method == null) {
        if (DEBUG) {
          VM.sysWriteln("Method for a profile not found, skipping method");
        }
        continue;
      }

      int currentParamIndex = paramStartIndex;
      ParameterProfileInformation pp;
      // TODO extract following lines to separate method
      if (USE_SUMMARIZED_PROFILES) {
        pp = getSummarizedProfile(method);
      } else {
        pp = new ParameterProfile(method);
      }

      // implicit this
      if (!method.isStatic()) {
        currentParamIndex = addCurrentValue(decoder, currentParamIndex, pp, TypeReference.Class);
      }


      TypeReference[] parameterTypes = method.getParameterTypes();
      for (int i = 0; i < parameterTypes.length; i++) {
        TypeReference ref = parameterTypes[i];
        currentParamIndex = addCurrentValue(decoder, currentParamIndex, pp, ref);
      }

      if (!USE_SUMMARIZED_PROFILES) {
        List<ParameterProfileInformation> profileList = profilesForAllMethods.get(method);
        if (profileList == null) {
          profileList = new ArrayList<ParameterProfileInformation>();
          profilesForAllMethods.put(method, profileList);
          profileList.add(pp);
        } else {
          boolean merged = tryMergingProfile(pp, profileList);
          if (!merged) {
            profileList.add(pp);
          }
        }
      }
    }

    checkThatEncodingsAndDecodingsMatch(decoder);

    listener.reset();
  }

  private void checkThatEncodingsAndDecodingsMatch(ParameterDecoder decoder) {
    if (VM.VerifyAssertions) {
      VM._assert(decoder.getErrorFlag() == ParameterDecoder.ErrorFlag.NO_ERROR, "An error occured in the decoder!");
      String doesNotMatch = "Number of encodings and decodings does not add up! It is " + decoder.getEncodeDecodeBalance() + "!";
      if (!decoder.numberOfEncodingsAndDecodingsMatches()) {
        VM._assert(VM.NOT_REACHED, doesNotMatch);
      }

    } else if (DEBUG) {
      if (!decoder.numberOfEncodingsAndDecodingsMatches()) {
        System.err.println("Number of encodings and decodings does not add up! It is:");
        System.err.println(decoder.getEncodeDecodeBalance());
      }
    }
  }

  protected ParameterProfileInformation getSummarizedProfile(RVMMethod method) {
    ParameterProfileInformation pp;
    List<ParameterProfileInformation> profiles = profilesForAllMethods.get(method);
    if (profiles == null) {
      profiles = new ArrayList<ParameterProfileInformation>(1);
      profilesForAllMethods.put(method, profiles);
      pp = new MethodProfile(new MethodDataProviderImpl(method), profileType);
      profiles.add(pp);
    } else {
      if (VM.VerifyAssertions) {
        VM._assert(profiles.size() == 1, "Only one method profile allowed when using summarized profiles!");
      }
      pp = profiles.get(0);
    }
    return pp;
  }

  public boolean tryMergingProfile(ParameterProfileInformation ppi, List<ParameterProfileInformation> profileList) {
    boolean merged = false;
    ParameterProfile profileToMerge = (ParameterProfile) ppi;
    for (ParameterProfileInformation profileInfo : profileList) {
      ParameterProfile profile = (ParameterProfile) profileInfo;
      if (profile.mergeWith(profileToMerge)) {
        merged = true;
        break;
      }
    }
    return merged;
  }

  protected static int addCurrentValue(ParameterDecoder decoder, int currentParamIndex, ParameterProfileInformation pp, TypeReference ref) {
    if (ref.isReferenceType()) {
      RVMType type = decoder.decodeType(currentParamIndex);
      currentParamIndex += EncodingHelper.getBytesForTypeReference(TypeReference.Class);

      pp.addNewType(type);
    } else if (ref.isBooleanType()) {
      boolean b = decoder.decodeBoolean(currentParamIndex);
      currentParamIndex += EncodingHelper.getBytesForTypeReference(TypeReference.Boolean);

      pp.addNewBooleanValue(b);
    } else if (ref.isByteType()) {
      byte byteValue = decoder.decodeByte(currentParamIndex);
      currentParamIndex += EncodingHelper.getBytesForTypeReference(TypeReference.Byte);

      pp.addNewByteValue(byteValue);
    } else if (ref.isCharType()) {
      char charValue = decoder.decodeChar(currentParamIndex);
      currentParamIndex += EncodingHelper.getBytesForTypeReference(TypeReference.Char);

      pp.addNewCharValue(charValue);
    } else if (ref.isDoubleType()) {
      double doubleValue = decoder.decodeDouble(currentParamIndex);
      currentParamIndex += EncodingHelper.getBytesForTypeReference(TypeReference.Double);

      pp.addNewDoubleValue(doubleValue);
    } else if (ref.isFloatType()) {
      float floatValue = decoder.decodeFloat(currentParamIndex);
      currentParamIndex += EncodingHelper.getBytesForTypeReference(TypeReference.Float);

      pp.addNewFloatValue(floatValue);
    } else if (ref.isIntType()) {
      int intValue = decoder.decodeInt(currentParamIndex);
      currentParamIndex += EncodingHelper.getBytesForTypeReference(TypeReference.Int);

      pp.addNewIntValue(intValue);
    } else if (ref.isLongType()) {
      long longValue = decoder.decodeLong(currentParamIndex);
      currentParamIndex += EncodingHelper.getBytesForTypeReference(TypeReference.Long);

      pp.addNewLongValue(longValue);
    } else if (ref.isShortType()) {
      short shortValue = decoder.decodeShort(currentParamIndex);
      currentParamIndex += EncodingHelper.getBytesForTypeReference(TypeReference.Short);

      pp.addNewShortValue(shortValue);
    } else {
      if (ref.isWordLikeType()) {
        // do nothing
      } else if (ref.isCodeType()) {
        // do nothing
      } else {
        // do nothing
      }
    }

    return currentParamIndex;
  }

  boolean profileAvailable(RVMMethod method) {
    return profilesForAllMethods.containsKey(method);
  }

  List<ParameterProfileInformation> getProfiles(RVMMethod method) {
    List<ParameterProfileInformation> profiles = profilesForAllMethods.get(method);
    return profiles;
  }

  void throwAwayDataForMethod(RVMMethod method) {
    if (DEBUG) {
      System.out.println("Specialization Profiling: Throwing away all data for " + method);
    }

    profilesForAllMethods.remove(method);
  }

}
