package org.deidentifier.arx.clustering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.deidentifier.arx.ARXInterface;
import org.deidentifier.arx.framework.data.DataManager;

class TassaCluster extends ArrayList<TassaRecord> {
    
    private static final long                  serialVersionUID = 1L;
    
    private final ARXInterface                 iface;
    private final DataManager                  manager;
    
    // List of transformation nodes for every attribute
    // they contain level of transformation, mapping key, all contained values
    private final int[]                        transformation;
    private final int[]                        generalizationLevel;
    private final GeneralizationNode[]         transformationNodes;
    private final HashMap<TassaRecord, Double> removedNodeGC;
    
    // caching of calculated values
    private double                             generalizationCost;
    
    /**
     * The modification counter at the time of the last update.
     * We use this to detect, whether the cluster was changed and
     * if we have to update generalizationCost and transformationNodes again.
     */
    private int                                lastModCount;
    
    private TassaCluster(ARXInterface iface) {
        this.iface = iface;
        manager = iface.getDataManager();
        final int numAtt = iface.getNumAttributes();
        transformation = new int[numAtt];
        generalizationLevel = new int[numAtt];
        
        transformationNodes = new GeneralizationNode[numAtt];
        removedNodeGC = new HashMap<>();
    }
    
    public TassaCluster(Collection<TassaRecord> recordCollection, ARXInterface iface) {
        this(iface);
        addAll(recordCollection);
        updateGeneralization();
    }
    
    public TassaCluster(TassaCluster cluster) {
        this(cluster.iface);
        addAll(cluster);
        System.arraycopy(cluster.transformationNodes, 0, transformationNodes, 0, transformationNodes.length);
        lastModCount = modCount;
    }
    
    // TODO: Better solution for assigning records?
    public void assignAllRecords() {
        for (final TassaRecord record : this) {
            record.assignedCluster = this;
        }
    }
    
    @Override
    public boolean add(TassaRecord record) {
        updateGeneralization(record);
        return super.add(record);
    }
    
    // public boolean addAll(TassaCluster cluster) {
    // boolean success = super.addAll(cluster);
    // updateGeneralization(cluster);
    // return success;
    // }
    
    public boolean remove(TassaRecord record) {
        removedNodeGC.clear();
        return super.remove(record);
    }
    
    public double getGC() {
        if (lastModCount < modCount) {
            updateGeneralization();
        }
        return generalizationCost;
    }
    
    private void updateGeneralization() {
        this.updateTransformation();
        generalizationCost = getGC_LM();
        lastModCount = modCount;
    }
    
    private void updateGeneralization(TassaRecord addedRecord) {
        this.updateTransformation(addedRecord);
        generalizationCost = getGC_LM();
        lastModCount = modCount;
    }
    
    private void updateGeneralization(TassaCluster cluster) {
        this.updateTransformation(cluster);
        generalizationCost = getGC_LM();
        lastModCount = modCount;
    }
    
    /**
     * Update transformation of this Cluster.
     */
    private void updateTransformation() {
        for (int i = 0; i < transformation.length; i++) {
            final int[] dataColumn = new int[size()];
            for (int j = 0; j < size(); j++) {
                dataColumn[j] = get(j).recordContent[i];
            }
            transformation[i] = iface.getHierarchyTree(i).getGeneralizationLevel(dataColumn, 0);
        }
    }
    
    /**
     * Update transformation of this Cluster.
     */
    private void updateTransformation(TassaRecord addedRecord) {
        final int[] newRecord = addedRecord.recordContent;
        final int[] existingRecord = get(0).recordContent;
        for (int i = 0; i < newRecord.length; i++) {
            final int[] dataColumn = new int[] { existingRecord[i], newRecord[i] };
            generalizationLevel[i] = iface.getHierarchyTree(i).getGeneralizationLevel(dataColumn, generalizationLevel[i]);
            transformation[i] = iface.getHierarchyTree(i).getTransformation(newRecord[i], generalizationLevel[i]);
        }
    }
    
    /**
     * Update transformation of this cluster.
     *
     * @param cluster the cluster
     */
    private void updateTransformation(TassaCluster cluster) {
        for (int i = 0; i < transformationNodes.length; i++) {
            transformationNodes[i] = iface.getHierarchyTree(i).getLowestCommonAncestor(transformationNodes[i], cluster.transformationNodes[i]);
        }
        
    }
    
    public double getGC_LM() {
        
        double gc = 0;
        final int numAtt = transformationNodes.length;
        for (int i = 0; i < numAtt; i++) {
            
            // TODO: Check, whether we have the correct cardinalities here!
            final int recordCardinality = transformationNodes[i].values.size();
            final int attributeCardinality = manager.getHierarchies()[i].getDistinctValues()[0];
            
            gc += (recordCardinality - 1) / (attributeCardinality - 1);
        }
        
        return gc / numAtt;
    }
    
    public double getAddedGC(TassaRecord addedRecord) {
        final TassaCluster tempCluster = new TassaCluster(this);
        tempCluster.add(addedRecord);
        return tempCluster.getGC();
    }
    
    public double getRemovedGC(TassaRecord removedRecord) {
        Double result = 0.0;
        if (size() > 1) {
            result = removedNodeGC.get(removedRecord);
            if (result == null) {
                final TassaCluster tempCluster = new TassaCluster(this);
                tempCluster.remove(removedRecord);
                result = tempCluster.getGC();
                removedNodeGC.put(removedRecord, result);
            }
        }
        return result;
    }
}
