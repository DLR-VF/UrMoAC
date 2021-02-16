/**
 * Copyright (c) 2016-2021 DLR Institute of Transport Research
 * All rights reserved.
 * 
 * This file is part of the "UrMoAC" accessibility tool
 * http://github.com/DLR-VF/UrMoAC
 * @author: Daniel.Krajzewicz@dlr.de
 * Licensed under the GNU General Public License v3.0
 * 
 * German Aerospace Center (DLR)
 * Institute of Transport Research (VF)
 * Rutherfordstraﬂe 2
 * 12489 Berlin
 * Germany
 * http://www.dlr.de/vf
 */
package de.dlr.ivf.urmo.router.output;

import java.util.Collection;
import java.util.Comparator;

/**
 * @class AbstractSingleResultComparator_TT
 * @brief A comparator object for sorting AbstractSingleResult by their travel time (then by origin/destination IDs)
 * @author Daniel Krajzewicz (c) 2017 German Aerospace Center, Institute of Transport Research
 */
public class AbstractSingleResultComparator_TT implements Comparator<AbstractSingleResult> {
	/**
	 * @brief Comparison factor
	 * @param c1 The first result
	 * @param c2 The second result
	 * @return Comparison
	 */
	@Override
	public int compare(AbstractSingleResult c1, AbstractSingleResult c2) {
		if(c1.tt<c2.tt) {
			return -1;
		} else if(c1.tt>c2.tt) {
			return 1;
		}
		if(c1.srcID<c2.srcID) {
			return -1;
		} else if(c1.srcID>c2.srcID) {
			return 1;
		}
		if(c1.destID<c2.destID) {
			return -1;
		} else if(c1.destID>c2.destID) {
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
