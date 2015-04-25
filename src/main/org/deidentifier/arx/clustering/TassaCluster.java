package org.deidentifier.arx.clustering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.deidentifier.arx.ARXInterface;
import org.deidentifier.arx.framework.data.DataManager;

class TassaCluster extends ArrayList<TassaRecord> {
    
    private static final long                  serialVersionUID = 1L;
    
    private final ARXInterface                 iface;
    private final DataManager                  manager;
    
    /** The number of attributes. */
    private final int                          numAtt;
    
    private int[]                        generalizationLevels;
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
        numAtt = iface.getNumAttributes();
        generalizationLevels = new int[numAtt];
        
        removedNodeGC = new HashMap<>();
    }
    
    public TassaCluster(Collection<TassaRecord> recordCollection, ARXInterface iface) {
        this(iface);
        addAll(recordCollection);
        getGeneralizationCost();
        lastModCount = modCount;
    }
    
    public TassaCluster(TassaCluster cluster) {
        this(cluster.iface);
        addAll(cluster);
        System.arraycopy(cluster.generalizationLevels, 0, generalizationLevels, 0, generalizationLevels.length);
    }
    
    // TODO: Better solution for assigning records?
    public void assignAllRecords() {
        for (final TassaRecord record : this) {
            record.assignedCluster = this;
        }
    }
    
    @Override
    public boolean add(TassaRecord newRecord) {
        this.generalizationCost = getAddedGC(newRecord);
        lastModCount = modCount;
        return super.add(newRecord);
    }
    
    public boolean addAll(TassaCluster cluster) {
        final boolean success = super.addAll(cluster);
        this.generalizationCost = getAddedGC(cluster);
        lastModCount = modCount;
        return success;
    }
    
    public boolean remove(TassaRecord record) {
        removedNodeGC.clear();
        return super.remove(record);
    }
    
    public double getGeneralizationCost() {
        if (lastModCount < modCount) {
            lastModCount = modCount;
            Arrays.fill(generalizationLevels, 0);
            generalizationLevels = getGeneralizationLevels(this, generalizationLevels);
            generalizationCost = getGC_LM(this.get(0).recordContent, generalizationLevels);
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
            final int[] dataColumn = new int[] { record.recordContent[i], get(0).recordContent[i] };
            result[i] = iface.getHierarchyTree(i).getGeneralizationLevel(dataColumn, currentGeneralizationLevels[i]);
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
        return getGC_LM(addedCluster.get(0).recordContent, this.getGeneralizationLevels(addedCluster.get(0), levels));
    }
    
    public double getAddedGC(TassaRecord addedRecord) {
        lastModCount = modCount;
        return getGC_LM(addedRecord.recordContent, this.getGeneralizationLevels(addedRecord, generalizationLevels));
    }
    
    public double getRemovedGC(TassaRecord removedRecord) {
        Double result = 0.0;
        if (size() > 1) {
            result = removedNodeGC.get(removedRecord);
            if (result == null) {
                super.remove(removedRecord);
                result = this.getGeneralizationCost();
                removedNodeGC.put(removedRecord, result);
                this.add(removedRecord);
            }
        }
        return result;
    }
}
