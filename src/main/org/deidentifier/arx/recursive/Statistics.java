package org.deidentifier.arx.recursive;

import java.util.ArrayList;
import java.util.List;

import org.deidentifier.arx.utility.UtilityMeasure;

public final class Statistics {
    
    private String[][] output;
    public int suppressedTuples;
    private List<UtilityMeasure<Double>> utilityMeasures = new ArrayList<>();
    
    
    public void printStatistics() {
        System.out.println("Suppressed Tuples: " + suppressedTuples);
        utilityMeasures.get(0).evaluate(output).toString();
    }
}
