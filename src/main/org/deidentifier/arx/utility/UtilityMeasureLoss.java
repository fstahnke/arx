/*
 * ARX: Powerful Data Anonymization
 * Copyright 2012 - 2015 Florian Kohlmayer, Fabian Prasser
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.deidentifier.arx.utility;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of the Loss measure, as proposed in:<br>
 * <br>
 * Iyengar, V.: Transforming data to satisfy privacy constraints. In: Proc Int Conf Knowl Disc Data Mining, p. 279288 (2002)
 * 
 * @author Fabian Prasser
 */
public class UtilityMeasureLoss {

    /** Map of losses */
    private final Map<String, Map<String, Double>> loss;
    /** Header */
    private final String[]                         header;

    /**
     * Creates a new instance
     * @param hierarchies
     */
    public UtilityMeasureLoss(String[] header, Map<String, String[][]> hierarchies) {
        this.loss = new HashMap<String, Map<String, Double>>();
        this.header = header;
        for (String attr : hierarchies.keySet()) {
            this.loss.put(attr, getLoss(hierarchies.get(attr)));
        }
    }

    /**
     * Evaluates the utility measure
     * @param header
     * @param input
     * @return
     */
    public double evaluate(String[][] input) {
        
        double[] result = new double[input[0].length];
        
        for (String[] row : input) {
            for (int i=0; i<result.length; i++) {
                result[i] += getLoss(header[i], row[i]);
            }
        }
        
        double value = 0d;
        for (int i=0; i<result.length; i++) {
            value *= (result[i] / input.length + 1d);
        }
        return Math.pow(value, 1d / result.length) - 1d;
    }

    /**
     * Build loss
     * @param attribute
     * @param value
     * @return
     */
    private double getLoss(String attribute, String value) {
        Double loss = this.loss.get(attribute).get(value);
        return loss != null ? loss : 1d;
    }

    /**
     * Build loss
     * @param hierarchy
     * @return
     */
    private Map<String, Double> getLoss(String[][] hierarchy) {
        Map<String, Double> loss = new HashMap<String, Double>();
        
        for (int col = 0; col < hierarchy[0].length; col++) {
            for (int row = 0; row < hierarchy.length; row++) {
                String value = hierarchy[row][col];
                if (!loss.containsKey(value)) {
                    double count = 0d;
                    for (int row2 = 0; row2 < hierarchy.length; row2++) {
                        if (hierarchy[row2][col].equals(value)) {
                            count++;
                        }
                    }
                    loss.put(value, (double)count / (double)hierarchy.length);
                }
            }
        }
        
        return loss;
    }
}
