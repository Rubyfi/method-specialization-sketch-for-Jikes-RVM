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
package org.jikesrvm.adaptive.parameterprofiling;

import java.util.Comparator;

import org.jikesrvm.adaptive.measurements.listeners.parameterprofiling.AbstractParameterInfo;
import org.jikesrvm.util.Pair;

/**
 * Imposes a descending order based on the second argument (which is considered a count).
 * <p>
 * NOTE: This comparator imposes orderings that are inconsistent with equals!
 */
public class PairComparator implements Comparator<Pair<AbstractParameterInfo, Integer>> {

  /**
   * {@inheritDoc}
   * <p>
   * Imposes a descending order based on the second argument (which is considered a count).
   * <p>
   * NOTE: This comparator imposes orderings that are inconsistent with equals!
   */
  @Override
  public int compare(Pair<AbstractParameterInfo, Integer> arg0, Pair<AbstractParameterInfo, Integer> arg1) {
    Integer countForFirst = arg0.second;
    Integer countForSecond = arg1.second;

    int valueForAscendingSorting = countForFirst.compareTo(countForSecond);
    int valueForDescendingSorting = -valueForAscendingSorting;

    return valueForDescendingSorting;
  }

}
