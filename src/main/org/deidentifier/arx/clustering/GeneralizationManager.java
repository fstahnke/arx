package org.deidentifier.arx.clustering;

import java.util.Collection;

import org.deidentifier.arx.framework.data.DataManager;
import org.deidentifier.arx.framework.data.GeneralizationHierarchy;

public class GeneralizationManager {
    
    private DataManager manager;
    private GeneralizationTree[] generalizationTrees;
    private final int numAtt;
    
    public GeneralizationManager(DataManager manager) {
        this.manager = manager;
        GeneralizationHierarchy[] hierarchies = manager.getHierarchies();
        numAtt = hierarchies.length;
        generalizationTrees = new GeneralizationTree[numAtt];
        for (int i = 0; i < hierarchies.length; i++)
        {
            generalizationTrees[i] = new GeneralizationTree(hierarchies[i]);
        }
    }
    
    private double getGC_LM(int[] record, int[] generalizationLevels) {
        
        double gc = 0;
        for (int i = 0; i < numAtt; i++) {
            final int recordCardinality = generalizationTrees[i].getCardinality(record[i], generalizationLevels[i]);
            final int attributeCardinality = generalizationTrees[i].getCardinality(record[i], generalizationTrees[i].maxLevel);
            
            gc += (recordCardinality - 1) / (attributeCardinality - 1);
        }
        
        return gc / numAtt;
    }
    
    public int[] getGeneralizationLevels(Collection<TassaRecord> recordCollection, int[] generalizationLevels) {

        final int[] record1 = changedObject.getCurrentGeneralization();
        final int[] record2 = getFirst().getCurrentGeneralization();
        final int[][] valuesByAttribute = new int[numAtt][recordCollection.size()];
        Iterator<TassaRecord> itr = recordCollection.iterator()
        for (int i = 0; itr.hasNext(); i++)
        {
        	for (int )
        }
        for (int i = 0; i < numAtt; i++) {
        	for (TassaRecord record : recordCollection) {
        		
        	}
            result[i] = iface.getHierarchyTree(i).getGeneralizationLevel(new int[] { record1[i], record2[i] }, currentGeneralizationLevels[i]);
        }
    }

	public int[] getTransformation(TassaRecord record, int[] generalizationLevels) {
        final int[] values = record.getCurrentGeneralization();
        int[] result = new int[numAtt];
        for (int i = 0; i < numAtt; i++) {
            result[i] = generalizationTrees[i].getTransformation(values[i], generalizationLevels[i]);
        }
        return result;
	}
    
    
    
}
