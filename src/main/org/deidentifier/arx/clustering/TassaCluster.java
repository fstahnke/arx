package org.deidentifier.arx.clustering;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.deidentifier.arx.ARXInterface;

public class TassaCluster extends LinkedList<TassaRecord> {
    
    private static final long                  serialVersionUID = 1L;
    
    /** The number of attributes. */
    private final int                          numAtt;
    
    private int[]                              generalizationLevels;
	private final GeneralizationTree[]         generalizationTrees;
    private final HashMap<TassaRecord, Double> removedNodeGC;
    
    // caching of calculated values
    private double                             generalizationCost;
    private int                                hashCode;
    
    /**
     * The modification counter at the time of the last update.
     * We use this to detect, whether the cluster was changed and
     * if we have to update generalizationCost and transformationNodes again.
     */
    private int                                lastModCount;
    
    public TassaCluster(Collection<TassaRecord> recordCollection, ARXInterface iface) {
        super();
        numAtt = iface.getNumAttributes();
        generalizationTrees = new GeneralizationTree[numAtt];
        for (int i = 0; i < numAtt; i++) {
            generalizationTrees[i] = iface.getHierarchyTree(i);
        }
        generalizationLevels = new int[numAtt];
        removedNodeGC = new HashMap<>();
        super.addAll(recordCollection);
        update(this);
    }
    
    private void assignAllRecords() {
        for (final TassaRecord record : this) {
            record.assignedCluster = this;
        }
    }
    
    @Override
    public boolean add(TassaRecord newRecord) {
        final boolean success = super.add(newRecord);
        update(newRecord);
        return success;
    }
    
    @Override
    public final boolean addAll(Collection<? extends TassaRecord> recordCollection) {
        final boolean success = super.addAll(recordCollection);
        update(recordCollection);
        return success;
    }
    
    @Override
    public boolean remove(Object record) {
        final boolean success = super.remove(record);
        update(record);
        return success;
    }
    
    public double getGeneralizationCost() {
        if (lastModCount < modCount) {
            update(this);
        }
        return generalizationCost;
    }
    
    private int[] getGeneralizationLevels(List<TassaRecord> recordCollection, int[] currentGeneralizationLevels) {
        final int[] result = new int[numAtt];
        
        for (int i = 0; i < numAtt; i++) {
            final int[] dataColumn = new int[recordCollection.size()];
            int j = 0;
            for (final TassaRecord record : recordCollection) {
                dataColumn[j] = record.recordContent[i];
                j++;
            }
            result[i] = generalizationTrees[i].getGeneralizationLevel(dataColumn, currentGeneralizationLevels[i]);
        }
        return result;
    }
    
    private int[] getGeneralizationLevels(TassaRecord record, int[] currentGeneralizationLevels) {
		final int[] record1 = record.recordContent;
		final int[] record2 = getFirst().recordContent;
        final int[] result = new int[numAtt];
        
        for (int i = 0; i < numAtt; i++) {
            result[i] = generalizationTrees[i].getGeneralizationLevel(new int[] { record1[i], record2[i] }, currentGeneralizationLevels[i]);
        }
        return result;
    }
    
    public double getGC_LM(int[] record, int[] generalizationLevels) {
        
        double gc = 0;
        for (int i = 0; i < numAtt; i++) {
			final GeneralizationTree tree = generalizationTrees[i];
            final int recordCardinality = tree.getCardinality(record[i], generalizationLevels[i]);
            final int attributeCardinality = tree.getCardinality(record[i], tree.maxLevel);
            
            gc += (recordCardinality - 1) / (attributeCardinality - 1);
        }
        
        return gc / numAtt;
    }
    
    public double getAddedGC(Object addedObject) {
        double result = 0;
        if (addedObject instanceof TassaCluster) {
            TassaCluster cluster = (TassaCluster)addedObject;
            final int[] levels = new int[numAtt];
            for (int i = 0; i < numAtt; i++) {
                levels[i] = Math.max(this.generalizationLevels[i], cluster.generalizationLevels[i]);
            }
            result = getGC_LM(cluster.getFirst().recordContent, this.getGeneralizationLevels(cluster.getFirst(), levels));
        }
        if (addedObject instanceof TassaRecord) {
            TassaRecord record = (TassaRecord)addedObject;
            result = getGC_LM(record.recordContent, this.getGeneralizationLevels(record, generalizationLevels));
        }
        return result;
    }
    
    public double getRemovedGC(TassaRecord removedRecord) {
        Double result = 0.0;
        if (size() > 1) {
            result = removedNodeGC.get(removedRecord);
            // We don't have a cached value for the GC without this record
            if (result == null) {
                // The record was not yet removed,
                // so we have to add it back after the calculation
                if (super.remove(removedRecord)) {
                    result = getGC_LM(getFirst().recordContent, getGeneralizationLevels(this, new int[numAtt]));
                    removedNodeGC.put(removedRecord, result);
                    super.add(removedRecord);
                    lastModCount = modCount;
                }
                // The record has already been removed,
                // so we don't have to put it back
                else {
                    result = getGC_LM(getFirst().recordContent, getGeneralizationLevels(this, new int[numAtt]));
                }
            }
        }
        return result;
    }
    
    private void update(Object addedObject) {
        final int[] levels = new int[numAtt];
        if (addedObject instanceof TassaCluster) {
            TassaCluster cluster = (TassaCluster)addedObject;
            if (cluster == this) {
                generalizationLevels = getGeneralizationLevels(this, levels);
            }
            else {
                for (int i = 0; i < numAtt; i++) {
                    levels[i] = Math.max(this.generalizationLevels[i], cluster.generalizationLevels[i]);
                }
                generalizationLevels = getGeneralizationLevels(cluster.getFirst(), levels);
                
            }
            generalizationCost = getGC_LM(getFirst().recordContent, generalizationLevels);
            assignAllRecords();
        }
        if (addedObject instanceof TassaRecord) {
            TassaRecord record = (TassaRecord)addedObject;
            if (record.assignedCluster != this) {
                record.assignedCluster = this;
                generalizationLevels = getGeneralizationLevels(record, generalizationLevels);
                generalizationCost = getAddedGC(record);
            }
            else {
                generalizationLevels = getGeneralizationLevels(this, levels);
                generalizationCost = getRemovedGC(record);
            }
        }
        removedNodeGC.clear();
        hashCode();
        lastModCount = modCount;
    }
    
    private int[] getTransformation() {
        final int[] record = getFirst().recordContent;
        int[] result = new int[numAtt];
        for (int i = 0; i < numAtt; i++) {
            result[i] = generalizationTrees[i].getTransformation(record[i], generalizationLevels[i]);
        }
        return result;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof TassaCluster && obj.hashCode() != hashCode()) {
            TassaCluster cluster = (TassaCluster)obj;
            if (cluster.size() == this.size()) {
                boolean equals = true;
                final int[] transformation = getTransformation();
                final int[] transformation2 = cluster.getTransformation();
                for (int i = 0; equals && i < transformation.length; i++) {
                    equals &= (transformation[i] == transformation2[i]);
                }
            }
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        if (hashCode == 0 || lastModCount != modCount) {
            hashCode = size();
            final int[] transformation = getTransformation();
            for (int i : transformation) {
                hashCode = hashCode * 524287 + i;
            }
        }
        return hashCode;
    }
    
}
