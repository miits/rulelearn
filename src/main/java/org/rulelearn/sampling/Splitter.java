/**
 * Copyright (C) Jerzy Błaszczyński, Marcin Szeląg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.rulelearn.sampling;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.rulelearn.core.Precondition;
import org.rulelearn.data.Decision;
import org.rulelearn.data.InformationTable;
import org.rulelearn.data.InformationTableWithDecisionDistributions;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;

/**
 * Splits {@link InformationTable information table (a data set)} into multiple information sub-tables (subsets of the data set).
 *
 * @author Jerzy Błaszczyński (<a href="mailto:jurek.blaszczynski@cs.put.poznan.pl">jurek.blaszczynski@cs.put.poznan.pl</a>)
 * @author Marcin Szeląg (<a href="mailto:marcin.szelag@cs.put.poznan.pl">marcin.szelag@cs.put.poznan.pl</a>)
 */
public class Splitter {

	/**
	 * Splits {@link InformationTable information table} provided as a parameter into multiple information sub-tables according to proportions
	 * provided as a parameter. Objects are selected from the information table into information sub-tables subsequently (i.e., according
	 * to the order of their occurrence in the information table).
	 * 
	 * @param informationTable {@link InformationTable information table} which will be split into sub tables
	 * @param splits an array with proportions, where the length is the number of {@link InformationTable information tables} to
     * construct; each value, in this array, is a proportion (fraction) of objects from the {@link InformationTable information table} 
     * passed as a parameter, which will be selected to respective constructed {@link InformationTable information sub-table}; 
     * The sum of proportions must be less than or equal to 1.0
     * 
	 * @return list with {@link InformationTable information sub-tables} with subsets of objects from the original {@link InformationTable information table}
	 * passed as a parameter 
	 * 
	 * @throws NullPointerException when the informationTable {@link InformationTable information table} or splits array provided as parameters are {@code null}
	 * @throws IllegalArgumentException when size of array with split proportions is lower than 2 or sum of split proportions is greater than 1.0 
	 */
	public List<InformationTable> split(InformationTable informationTable, double... splits) {
		Precondition.notNull(informationTable, "Information table provided to split is null.");
		Precondition.notNull(splits, "Provided array with splits is null.");
		checkProportionsArray(splits);
		List<InformationTable> subTables = new ArrayList<InformationTable> (splits.length);
		int start = 0, stop, informationTableSize = informationTable.getNumberOfObjects(), j;
		int [] indices;
		for (double split : splits) {
			stop = start + (int)(split * informationTableSize);
			if (stop > informationTableSize) { // just in case of numeric approximation errors
				stop = informationTableSize;
			}
			indices = new int [stop-start];
			j = 0;
			for (int i = start; i < stop; i++) {
				indices[j++] = i;
			}
			subTables.add(informationTable.select(indices, true));
			start = stop;
		}
		return subTables;
	}
	
	/**
	 * Splits {@link InformationTable information table} provided as a parameter into multiple information sub-tables according to proportions
	 * provided as a parameter, and according to the distribution of decisions in the information table (i.e., each constructed 
	 * sub-table has the same distribution of decisions as the original information table). More precisely, for each sub-table a proportion of objects
	 * having each of decisions present in the information table is selected. In result, not all objects from the information table must be selected 
	 * into sub-tables even if sum of proportions is equal 1.0. Objects are selected from the information table 
	 * into information sub-tables subsequently (i.e., according to the order of their occurrence in the information table).
	 * 
	 * @param informationTable {@link InformationTable information table} which will be split into sub tables
	 * @param splits an array with proportions, where the length is the number of {@link InformationTable information tables} to
     * construct; each value, in this array, is a proportion (fraction) of objects from the {@link InformationTable information table} 
     * passed as a parameter, which will be selected to respective constructed {@link InformationTable information sub-table}; 
     * The sum of proportions must be less than or equal to 1.0
     * 
	 * @return list with {@link InformationTable information sub-tables} with subsets of objects from the original {@link InformationTable information table}
	 * passed as a parameter; each constructed information sub-table has the same distribution of decisions as the original information table 
	 * 
	 * @throws NullPointerException when the informationTable {@link InformationTable information table} or splits array provided as parameters are {@code null}
	 * @throws IllegalArgumentException when size of array with split proportions is lower than 2 or sum of split proportions is greater than 1.0
	 */
	public List<InformationTable> stratifiedSplit(InformationTableWithDecisionDistributions informationTable, double... splits) {
		Precondition.notNull(informationTable, "Information table provided to split is null.");
		Precondition.notNull(splits, "Provided array with splits is null.");
		checkProportionsArray(splits);
		// initialization
		List<InformationTable> subTables = new ArrayList<InformationTable> (splits.length);;
		Set<Decision> decisions = informationTable.getDecisionDistribution().getDecisions();
		int numberOfDecisions = decisions.size();
		Map<Decision, Integer> starts = new Object2IntLinkedOpenHashMap<Decision>(numberOfDecisions);
		Map<Decision, IntArrayList> objectsToSelect = new Object2ObjectLinkedOpenHashMap<Decision, IntArrayList>(numberOfDecisions);
		Iterator<Decision> decisionIterator = decisions.iterator();
		while (decisionIterator.hasNext()) {
			Decision decision = decisionIterator.next();
			starts.put(decision, 0);
			objectsToSelect.put(decision, new IntArrayList(informationTable.getDecisionDistribution().getCount(decision)));
		}
		Decision[] objectDecisions = informationTable.getDecisions();
		IntArrayList list;
		for (int i = 0; i < objectDecisions.length; i++) {
			list = objectsToSelect.get(objectDecisions[i]);
			list.add(i);
		}
		// selection
		int numberOfObjects = informationTable.getNumberOfObjects(), start, stop, maxToSelectForDecison;
		IntArrayList indices;
		for (double split : splits) {
			indices = new IntArrayList((int)(split * numberOfObjects));
			decisionIterator = decisions.iterator();
			while (decisionIterator.hasNext()) {
				Decision decision = decisionIterator.next();
				start = starts.get(decision); 
				maxToSelectForDecison = informationTable.getDecisionDistribution().getCount(decision);
				stop = start + (int)(split * maxToSelectForDecison);
				if (stop > maxToSelectForDecison) { // just in case of numeric approximation errors
					stop = maxToSelectForDecison;
				}
				list = objectsToSelect.get(decision);
					for (int i = start; i < stop; i++) {
					indices.add(list.getInt(i));
				}
				starts.put(decision, stop);
			}
			subTables.add(informationTable.select(indices.toIntArray(), true));
		}
		
		return subTables;
	}
	
	/**
	 * Randomly splits {@link InformationTable information table} provided as a parameter into multiple information sub-tables according to proportions
	 * provided as a parameter.
	 * 
	 * @param informationTable {@link InformationTable information table} which will be split into sub tables
	 * @param random the source of randomness
	 * @param splits an array with proportions, where the length is the number of {@link InformationTable information tables} to
     * construct; each value, in this array, is a proportion (fraction) of objects from the {@link InformationTable information table} 
     * passed as a parameter, which will be selected to respective constructed {@link InformationTable information sub-table}; 
     * The sum of proportions must be less than or equal to 1.0
     * 
	 * @return list with {@link InformationTable information sub-tables} with subsets of objects from the original {@link InformationTable information table}
	 * passed as a parameter
	 * 
	 * @throws NullPointerException when the informationTable {@link InformationTable information table}, splits array or source of randomness 
	 * provided as parameters are {@code null}
	 * @throws IllegalArgumentException when size of array with split proportions is lower than 2 or sum of split proportions is greater than 1.0
	 */
	public List<InformationTable> randomSplit(InformationTable informationTable, Random random, double... splits) {
		Precondition.notNull(informationTable, "Information table provided to split is null.");
		Precondition.notNull(random, "Provided random generator is null.");
		Precondition.notNull(splits, "Provided array with splits is null.");
		checkProportionsArray(splits);
		List<InformationTable> subTables = new ArrayList<InformationTable> (splits.length);
		int informationTableSize = informationTable.getNumberOfObjects(), splitSize, i, j;
		BitSet picked = new BitSet(informationTableSize);
		int [] indices; 
		for (double split : splits) {
			splitSize =  (int)(split * informationTableSize);
			indices = new int [splitSize];
			i = 0;
			while (i < splitSize) {
				j = random.nextInt(informationTableSize);
				if (!picked.get(j)) {
					indices[i++] = j;
					picked.set(j);
				}
			}
			subTables.add(informationTable.select(indices, true));
		}
		return subTables;
	}
	
	/**
	 * Randomly splits {@link InformationTable information table} provided as a parameter into multiple information sub-tables according to proportions
	 * provided as a parameter, and according to the distribution of decisions in the information table (i.e., each constructed 
	 * sub-table has the same distribution of decisions as the original information table). 
	 * 
	 * @param informationTable {@link InformationTable information table} which will be split into sub tables
	 * @param random the source of randomness
	 * @param splits an array with proportions, where the length is the number of {@link InformationTable information tables} to
     * construct; each value, in this array, is a proportion (fraction) of objects from the {@link InformationTable information table} 
     * passed as a parameter, which will be selected to respective constructed {@link InformationTable information sub-table}; 
     * The sum of proportions must be less than or equal to 1.0
     * 
	 * @return list with {@link InformationTable information sub-tables} with subsets of objects from the original {@link InformationTable information table}
	 * passed as a parameter; each constructed information sub-table has the same distribution of decisions as the original information table
	 * 
	 * @throws NullPointerException when the informationTable {@link InformationTable information table}, splits array or source of randomness
	 * provided as parameters are {@code null}
	 * @throws IllegalArgumentException when size of array with split proportions is lower than 2 or sum of split proportions is greater than 1.0
	 */
	public List<InformationTable> randomStratifiedSplit(InformationTableWithDecisionDistributions informationTable, Random random, double... splits) {
		Precondition.notNull(informationTable, "Information table provided to split is null.");
		Precondition.notNull(random, "Provided random generator is null.");
		Precondition.notNull(splits, "Provided array with splits is null.");
		checkProportionsArray(splits);
		// initialization
		List<InformationTable> subTables = new ArrayList<InformationTable> (splits.length);;
		Set<Decision> decisions = informationTable.getDecisionDistribution().getDecisions();
		int numberOfDecisions = decisions.size(), numberOfObjectsForDecision;
		Map<Decision, IntArrayList> objectsToSelect = new Object2ObjectLinkedOpenHashMap<Decision, IntArrayList>(numberOfDecisions);
		Map<Decision, BitSet> pickedObjects = new Object2ObjectLinkedOpenHashMap<Decision, BitSet>(numberOfDecisions);
		Iterator<Decision> decisionIterator = decisions.iterator();
		while (decisionIterator.hasNext()) {
			Decision decision = decisionIterator.next();
			numberOfObjectsForDecision = informationTable.getDecisionDistribution().getCount(decision);
			objectsToSelect.put(decision, new IntArrayList(numberOfObjectsForDecision));
			pickedObjects.put(decision, new BitSet(numberOfObjectsForDecision));
		}
		Decision[] objectDecisions = informationTable.getDecisions();
		IntArrayList list;
		for (int i = 0; i < objectDecisions.length; i++) {
			list = objectsToSelect.get(objectDecisions[i]);
			list.add(i);
		}
		// selection
		IntArrayList indices;
		BitSet picked;
		int numberOfObjects = informationTable.getNumberOfObjects(), splitForDecisionSize, i, j;
		for (double split : splits) {
			indices = new IntArrayList((int)(split * numberOfObjects));
			decisionIterator = decisions.iterator();
			while (decisionIterator.hasNext()) {
				Decision decision = decisionIterator.next();
				numberOfObjectsForDecision = informationTable.getDecisionDistribution().getCount(decision);
				splitForDecisionSize = (int)(split * numberOfObjectsForDecision);
				list = objectsToSelect.get(decision);
				picked = pickedObjects.get(decision);
				i = 0;
				while (i < splitForDecisionSize) {
					j = random.nextInt(numberOfObjectsForDecision);
					if (!picked.get(j)) {
						indices.add(list.getInt(j));
						picked.set(j);
						i++;
					}
				}
			}
			subTables.add(informationTable.select(indices.toIntArray(), true));
		}
		return subTables;
	}
	
	/**
	 * Checks array with proportions - i.e., it should have not less than 2 elements and sum of its elements should be less than or equal to 1.0.
	 * 
	 * @param proportions an array with proportions
     * 
     * @throws IllegalArgumentException when size of array with proportions is lower than 2 or sum of proportions is greater than 1.0
	 */
	void checkProportionsArray (double... proportions) {
		if(proportions.length < 2)
            throw new IllegalArgumentException("Size of array with proportions must not be lower than 2.");
		
        double sum = 0;
        for(int i = 0; i < proportions.length; i++) {
            sum += proportions[i];
            // check whether fractions sum up to 1 (with some flexibility) in case of numeric approximation errors
            if(sum >= 1.0001) {
                throw new IllegalArgumentException("Sum of proportions is greater than 1.0 by index " + i + ", and it is reaching value " + sum);
            }
        }
	}
	
}