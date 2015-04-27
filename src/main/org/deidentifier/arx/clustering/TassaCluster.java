package org.deidentifier.arx.clustering;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.deidentifier.arx.ARXInterface;
import org.deidentifier.arx.framework.data.DataManager;

public class TassaCluster extends LinkedList<TassaRecord> {
    
    private static final long                  serialVersionUID = 1L;
    
    private final ARXInterface                 iface;
    private final DataManager                  manager;
    
    /** The number of attributes. */
    private final int                          numAtt;
    
    private int[]                              generalizationLevels;
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
    
    private TassaCluster(ARXInterface iface) {
        this.iface = iface;
        manager = iface.getDataManager();
        numAtt = iface.getNumAttributes();
        generalizationLevels = new int[numAtt];
        
        removedNodeGC = new HashMap<>();
    }
    
    public TassaCluster(Collection<TassaRecord> recordCollection, ARXInterface iface) {
        this(iface);
        addAll(recordCollection);
        assignAllRecords(this);
        getGeneralizationCost();
    }
    
    public TassaCluster(TassaCluster cluster) {
        this(cluster.iface);
        addAll(cluster);
        System.arraycopy(cluster.generalizationLevels, 0, generalizationLevels, 0, generalizationLevels.length);
    }
    
    private void assignAllRecords(TassaCluster cluster) {
        for (final TassaRecord record : cluster) {
            record.assignedCluster = this;
        }
    }
    
    @Override
    public boolean add(TassaRecord newRecord) {
        final boolean success = super.add(newRecord);
        newRecord.assignedCluster = this;
        generalizationLevels = getGeneralizationLevels(newRecord, generalizationLevels);
        generalizationCost = getAddedGC(newRecord);
        updateCluster();
        return success;
    }
    
    public boolean addAll(TassaCluster cluster) {
        final boolean success = super.addAll(cluster);
        assignAllRecords(cluster);
        final int[] levels = new int[numAtt];
        for (int i = 0; i < numAtt; i++) {
            levels[i] = Math.max(this.generalizationLevels[i], cluster.generalizationLevels[i]);
        }
        generalizationLevels = getGeneralizationLevels(cluster.getFirst(), levels);
        generalizationCost = getGC_LM(cluster.getFirst().recordContent, generalizationLevels);
        updateCluster();
        return success;
    }
    
    public boolean remove(TassaRecord record) {
        final boolean success = super.remove(record);
        removedNodeGC.clear();
        hashCode();
        getGeneralizationCost();
        return success;
    }
    
    public double getGeneralizationCost() {
        if (lastModCount < modCount || generalizationCost == 0) {
            Arrays.fill(generalizationLevels, 0);
            generalizationLevels = getGeneralizationLevels(this, generalizationLevels);
            generalizationCost = getGC_LM(getFirst().recordContent, generalizationLevels);
            updateCluster();
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
            result[i] = iface.getHierarchyTree(i).getGeneralizationLevel(dataColumn, currentGeneralizationLevels[i]);
        }
        return result;
    }
    
    private int[] getGeneralizationLevels(TassaRecord record, int[] currentGeneralizationLevels) {
        final int[] result = new int[numAtt];
        
        for (int i = 0; i < numAtt; i++) {
            result[i] = iface.getHierarchyTree(i).getGeneralizationLevel(new int[] { record.recordContent[i], getFirst().recordContent[i] }, currentGeneralizationLevels[i]);
        }
        return result;
    }
    
    public double getGC_LM(int[] record, int[] generalizationLevels) {
        
        double gc = 0;
        for (int i = 0; i < numAtt; i++) {
            final int recordCardinality = iface.getHierarchyTree(i).getCardinality(record[i], generalizationLevels[i]);
            final int attributeCardinality = manager.getHierarchies()[i].getDistinctValues()[0];
            
            gc += (recordCardinality - 1) / (attributeCardinality - 1);
        }
        
        return gc / numAtt;
    }
    
    public double getAddedGC(TassaCluster addedCluster) {
        final int[] levels = new int[numAtt];
        for (int i = 0; i < numAtt; i++) {
            levels[i] = Math.max(this.generalizationLevels[i], addedCluster.generalizationLevels[i]);
        }
        return getGC_LM(addedCluster.getFirst().recordContent, this.getGeneralizationLevels(addedCluster.getFirst(), levels));
    }
    
    public double getAddedGC(TassaRecord addedRecord) {
        return getGC_LM(addedRecord.recordContent, this.getGeneralizationLevels(addedRecord, generalizationLevels));
    }
    
    public double getRemovedGC(TassaRecord removedRecord) {
        Double result = 0.0;
        if (size() > 1) {
            result = removedNodeGC.get(removedRecord);
            if (result == null && super.remove(removedRecord)) {
                result = getGC_LM(getFirst().recordContent, getGeneralizationLevels(this, new int[numAtt]));
                removedNodeGC.put(removedRecord, result);
                this.add(removedRecord);
            }
        }
        return result;
    }
    
    private int[] getTransformation() {
        final int[] record = getFirst().recordContent;
        int[] result = new int[numAtt];
        for (int i = 0; i < numAtt; i++) {
            result[i] = iface.getHierarchyTree(i).getTransformation(record[i], generalizationLevels[i]);
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
    
    private void updateCluster() {
        hashCode();
        lastModCount = modCount;
    }
    
}
