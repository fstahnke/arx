package org.deidentifier.arx.clustering;

import org.deidentifier.arx.ARXInterface;
import org.deidentifier.arx.framework.data.DataManager;
import org.deidentifier.arx.framework.data.GeneralizationHierarchy;

public class GeneralizationManager {
    
    private DataManager manager;
    private GeneralizationTree[] generalizationTrees;
    private final int numAtt;
    
    public GeneralizationManager(ARXInterface iface) {
        this.manager = iface.getDataManager();
        numAtt = iface.getNumAttributes();
        generalizationTrees = new GeneralizationTree[numAtt];

        GeneralizationHierarchy[] hierarchies = manager.getHierarchies();
        for (int i = 0; i < hierarchies.length; i++)
        {
            generalizationTrees[i] = new GeneralizationTree(hierarchies[i]);
        }
    }
    
    public int[] getGeneralizationLevels(IGeneralizable object, int[] currentGeneralizationLevels) {
        int[] result = new int[numAtt];
        
        
        
        return result;
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
    
    
    
}
