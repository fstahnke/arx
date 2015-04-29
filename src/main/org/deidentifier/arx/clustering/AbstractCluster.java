package org.deidentifier.arx.clustering;

import java.util.LinkedList;

public abstract class AbstractCluster extends LinkedList<AbstractCluster> {
    
    /**
     * 
     */
    private static final long serialVersionUID = -799271299943582628L;
    
    private TassaCluster assignedCluster;
    
    
    public abstract double getGeneralizationCost();
    public abstract int[] getTransformedValues();
    
    public TassaCluster getAssignedCluster() {
        return assignedCluster;
    }
    public void setAssignedCluster(TassaCluster cluster) {
        assignedCluster = cluster;
    }
    
}
