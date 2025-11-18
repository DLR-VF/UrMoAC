/*
 * Copyright (c) 2023-2024
 * Institute of Transport Research
 * German Aerospace Center
 * 
 * All rights reserved.
 * 
 * This file is part of the "UrMoAC" accessibility tool
 * https://github.com/DLR-VF/UrMoAC
 * Licensed under the Eclipse Public License 2.0
 * 
 * German Aerospace Center (DLR)
 * Institute of Transport Research (VF)
 * Rutherfordstraße 2
 * 12489 Berlin
 * Germany
 * http://www.dlr.de/vf
 */
package de.dlr.ivf.urmo.router.output;

import java.util.Collection;
import java.util.Comparator;

import de.dlr.ivf.urmo.router.algorithms.routing.SingleODResult;

/**
 * @class SingleResultComparator_DestinationID
 * @brief A comparator object for sorting AbstractSingleResult by their travel time (then by origin/destination IDs)
 * @author Daniel Krajzewicz
 */
public class SingleResultComparator_DestinationID implements Comparator<SingleODResult> {
	/**
	 * @brief Comparison factor
	 * @param c1 The first result
	 * @param c2 The second result
	 * @return Comparison
	 */
	@Override
	public int compare(SingleODResult c1, SingleODResult c2) {
		if(c1.destination.em.getOuterID()<c2.destination.em.getOuterID()) {
			return -1;
		} else if(c1.destination.em.getOuterID()>c2.destination.em.getOuterID()) {
			return 1;
		}
		return 0;
	}
	
	
	/**
	 * @brief A helper debug method to ensure the transitivity of a comparator
	 * @param comparator The comparator to test
	 * @param elements The sample elements
	 * @todo Move to somewhere else
	 */
	public static <T> void verifyTransitivity(Comparator<T> comparator, Collection<T> elements) {
        for (T first: elements) {
            for (T second: elements) {
                int result1 = comparator.compare(first, second);
                int result2 = comparator.compare(second, first);
                if (result1 != -result2) {
                    // Uncomment the following line to step through the failed case
                    //comparator.compare(first, second);
                    throw new AssertionError("compare(" + first + ", " + second + ") == " + result1 +
                        " but swapping the parameters returns " + result2);
                }
            }
        }
        for (T first: elements) {
            for (T second: elements) {
                int firstGreaterThanSecond = comparator.compare(first, second);
                if (firstGreaterThanSecond <= 0)
                    continue;
                for (T third: elements) {
                    int secondGreaterThanThird = comparator.compare(second, third);
                    if (secondGreaterThanThird <= 0)
                        continue;
                    int firstGreaterThanThird = comparator.compare(first, third);
                    if (firstGreaterThanThird <= 0) {
                        // Uncomment the following line to step through the failed case
                        //comparator.compare(first, third);
                        throw new AssertionError("compare(" + first + ", " + second + ") > 0, " +
                            "compare(" + second + ", " + third + ") > 0, but compare(" + first + ", " + third + ") == " +
                            firstGreaterThanThird);
                    }
                }
            }
        }
    }	
};
