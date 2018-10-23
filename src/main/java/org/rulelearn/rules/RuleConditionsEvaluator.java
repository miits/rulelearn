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

package org.rulelearn.rules;

import org.rulelearn.measures.Measure;

/**
 * Contract of an evaluator of rule conditions {@link RuleConditions}.
 *
 * @author Jerzy Błaszczyński (<a href="mailto:jurek.blaszczynski@cs.put.poznan.pl">jurek.blaszczynski@cs.put.poznan.pl</a>)
 * @author Marcin Szeląg (<a href="mailto:marcin.szelag@cs.put.poznan.pl">marcin.szelag@cs.put.poznan.pl</a>)
 */
public interface RuleConditionsEvaluator extends Measure {
	
	/**
	 * Evaluates given rule conditions.
	 * 
	 * @param ruleConditions rule conditions to be evaluated
	 * @return evaluation of given rule conditions
	 * 
	 * @throws NullPointerException if given rule conditions are {@code null}
	 */
	public double evaluate(RuleConditions ruleConditions);
	
	/**
	 * Checks if evaluation of given rule conditions, as returned by {@link #evaluate(RuleConditions)}, satisfies given threshold.
	 * Takes into account type of this measure, as returned by {@link #getType()}.
	 * 
	 * @param ruleConditions rule conditions to evaluate
	 * @param threshold threshold compared with evaluation of given rule conditions
	 * @return {@code true} if evaluation of given rule conditions, as returned by {@link #evaluate(RuleConditions)}, satisfies given threshold,
	 *         {@code false} otherwise
	 * 
	 * @throws NullPointerException if given rule conditions are {@code null}
	 */
	public default boolean evaluationSatisfiesThreshold(RuleConditions ruleConditions, double threshold) {
		return (this.getType() == MeasureType.GAIN ?
					this.evaluate(ruleConditions) >= threshold:
					this.evaluate(ruleConditions) <= threshold);
	}

}