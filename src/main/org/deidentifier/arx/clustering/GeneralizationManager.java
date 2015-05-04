package org.deidentifier.arx.clustering;

import org.deidentifier.arx.framework.data.DataManager;
import org.deidentifier.arx.framework.data.GeneralizationHierarchy;

public class GeneralizationManager {
    
    private final GeneralizationTree[] generalizationTrees;
    private final int numAtt;
    public int getNumAttributes() {
        return numAtt;
    }

    public GeneralizationManager(DataManager manager) {
        GeneralizationHierarchy[] hierarchies = manager.getHierarchies();
        numAtt = hierarchies.length;
        generalizationTrees = new GeneralizationTree[numAtt];
        for (int i = 0; i < hierarchies.length; i++)
        {
            generalizationTrees[i] = new GeneralizationTree(hierarchies[i]);
        }
    }
    
    public double calculateGeneralizationCost(IGeneralizable generalizeObject, int[] generalizationLevels) {
        return calculateGeneralizationCost_LossMetric(generalizeObject.getValues(), generalizationLevels);
    }
    
    // Calculate generalization levels for set of values from scratch
    public int[] calculateGeneralizationLevels(IGeneralizable generalizeObject) {
        
        final int[] result = new int[numAtt];
        final int[][] valuesByAttribute = generalizeObject.getValuesByAttribute();
        
        for (int i = 0; i < numAtt; i++) {
            result[i] = generalizationTrees[i].getGeneralizationLevel(valuesByAttribute[i], 0);
        }
        
        return result;
    }
    
    // Calculate generalization levels for two objects with initial generalization levels
    public int[] calculateGeneralizationLevels(final int[] firstValues, final int[] secondValues, int[] currentGeneralizationLevels) {
        final int[] result = new int[numAtt];
        
        for (int i = 0; i < numAtt; i++) {
            result[i] = generalizationTrees[i].getGeneralizationLevel(firstValues[i], secondValues[i], currentGeneralizationLevels[i]);
        }
        
        return result;
    }
    
    // Calculate generalization levels for two objects with initial generalization levels
    public int[] calculateGeneralizationLevels(IGeneralizable firstObject, IGeneralizable secondObject, int[] currentGeneralizationLevels) {
        final int[] result = new int[numAtt];
        final int[] firstValues = firstObject.getValues();
        final int[] secondValues = secondObject.getValues();
        
        for (int i = 0; i < numAtt; i++) {
            result[i] = generalizationTrees[i].getGeneralizationLevel(firstValues[i], secondValues[i], currentGeneralizationLevels[i]);
        }
        
        return result;
    }

	public int[] calculateTransformation(IGeneralizable generalizeObject, int[] generalizationLevels) {
        final int[] values = generalizeObject.getValues();
        int[] result = new int[numAtt];
        for (int i = 0; i < numAtt; i++) {
            result[i] = generalizationTrees[i].getTransformation(values[i], generalizationLevels[i]);
        }
        return result;
	}
    
    private double calculateGeneralizationCost_LossMetric(final int[] record, final int[] generalizationLevels) {
        
        double gc = 0d;
        for (int i = 0; i < numAtt; i++) {
            // Important: Don't use integers here. Otherwise the division will result in 0.0 because it's performed as integer division.
            final double recordCardinality = generalizationTrees[i].getCardinality(record[i], generalizationLevels[i]);
            final double attributeCardinality = generalizationTrees[i].attributeCardinality;
            
            gc += (recordCardinality - 1) / (attributeCardinality - 1);
        }
        
        return gc / numAtt;
    }
}
