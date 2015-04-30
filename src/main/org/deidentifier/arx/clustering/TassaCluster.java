package org.deidentifier.arx.clustering;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

import org.deidentifier.arx.ARXInterface;

public class TassaCluster extends LinkedList<TassaRecord> implements IGeneralizable {
    
    private static final long                  serialVersionUID = 1L;
    
    /** The number of attributes. */
    private final int                          numAtt;
    
    private int[]                              generalizationLevels;
    private int[]                              transformation;
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

    private ARXInterface                       iface;
    
    public TassaCluster(Collection<? extends TassaRecord> recordCollection, ARXInterface iface) {
        super();
        this.iface = iface;
        numAtt = iface.getNumAttributes();
        generalizationLevels = new int[numAtt];
        removedNodeGC = new HashMap<>();
        super.addAll(recordCollection);
        update(this);
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
    
    public boolean remove(TassaRecord record) {
        final boolean success = super.remove(record);
        update(record);
        return success;
    }

    public int[] getCurrentGeneralization() {
        final int[] record = getFirst().getCurrentGeneralization();
        int[] result = new int[numAtt];
        for (int i = 0; i < numAtt; i++) {
            result[i] = iface.getHierarchyTree(i).getTransformation(record[i], generalizationLevels[i]);
        }
        return result;
    }

    public double getGeneralizationCost() {
        if (lastModCount < modCount) {
            update(this);
        }
        return generalizationCost;
    }
    
    private int[] getGeneralizationLevels(IGeneralizable changedObject, int[] currentGeneralizationLevels) {
        int[] result = new int[numAtt];
        
        if (changedObject instanceof TassaRecord) {
            final int[] record1 = changedObject.getCurrentGeneralization();
            final int[] record2 = getFirst().getCurrentGeneralization();
            for (int i = 0; i < numAtt; i++) {
                result[i] = iface.getHierarchyTree(i).getGeneralizationLevel(new int[] { record1[i], record2[i] }, currentGeneralizationLevels[i]);
            }
            
        }
        
        if (changedObject instanceof TassaCluster) {
            TassaCluster cluster = (TassaCluster)changedObject;
            if (changedObject == this) {
                for (int i = 0; i < numAtt; i++) {
                    final int[] dataColumn = new int[cluster.size()];
                    int j = 0;
                    for (final TassaRecord record : cluster) {
                        dataColumn[j] = record.getCurrentGeneralization()[i];
                        j++;
                    }
                    result[i] = iface.getHierarchyTree(i).getGeneralizationLevel(dataColumn, currentGeneralizationLevels[i]);
                }
                
            }
            else {
                result = getGeneralizationLevels(cluster.getFirst(), currentGeneralizationLevels);
            }
        }
        
        return result;
    }
    
    private double getGC_LM(int[] record, int[] generalizationLevels) {
        
        double gc = 0;
        for (int i = 0; i < numAtt; i++) {
			final GeneralizationTree tree = iface.getHierarchyTree(i);
            final int recordCardinality = tree.getCardinality(record[i], generalizationLevels[i]);
            final int attributeCardinality = tree.getCardinality(record[i], tree.maxLevel);
            
            gc += (recordCardinality - 1) / (attributeCardinality - 1);
        }
        
        return gc / numAtt;
    }
    
    public double getAddedGC(IGeneralizable movedRecord) {
        double result = 0;
        if (movedRecord instanceof TassaCluster) {
            TassaCluster cluster = (TassaCluster)movedRecord;
            final int[] levels = new int[numAtt];
            for (int i = 0; i < numAtt; i++) {
                levels[i] = Math.max(this.generalizationLevels[i], cluster.generalizationLevels[i]);
            }
            result = getGC_LM(cluster.getFirst().getCurrentGeneralization(), this.getGeneralizationLevels(cluster.getFirst(), levels));
        }
        if (movedRecord instanceof TassaRecord) {
            TassaRecord record = (TassaRecord)movedRecord;
            result = getGC_LM(record.getCurrentGeneralization(), this.getGeneralizationLevels(record, generalizationLevels));
        }
        return result;
    }
    
    public double getRemovedGC(TassaRecord record) {
        Double result = 0.0;
        if (size() > 1) {
            result = removedNodeGC.get(record);
            // We don't have a cached value for the GC without this record
            if (result == null) {
                // The record was not yet removed,
                // so we have to add it back after the calculation
                if (super.remove(record)) {
                    result = getGC_LM(getFirst().getCurrentGeneralization(), getGeneralizationLevels(this, new int[numAtt]));
                    removedNodeGC.put(record, result);
                    super.add(record);
                    lastModCount = modCount;
                }
                // The record has already been removed,
                // so we don't have to put it back
                else {
                    result = getGC_LM(getFirst().getCurrentGeneralization(), getGeneralizationLevels(this, new int[numAtt]));
                }
            }
//            else {
//                LinkedList<TassaRecord> test = new LinkedList<>(removedNodeGC.keySet());
//                boolean sameObject = false;
//                for (TassaRecord record : test) {
//                    sameObject |= record == removedRecord;
//                }
//                if (!sameObject) {
//                    System.out.println("Yeah!");
//                }
//            }
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
            generalizationCost = getGC_LM(getFirst().getCurrentGeneralization(), generalizationLevels);
            assignAllRecords();
        }
        if (addedObject instanceof TassaRecord) {
            TassaRecord record = (TassaRecord)addedObject;
            if (record.getAssignedCluster() != this) {
                record.setAssignedCluster(this);
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
    
    private void assignAllRecords() {
        for (final TassaRecord record : this) {
            record.setAssignedCluster(this);
        }
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
                final int[] transformation = getCurrentGeneralization();
                final int[] transformation2 = cluster.getCurrentGeneralization();
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
            final int[] transformation = getCurrentGeneralization();
            for (int i : transformation) {
                hashCode = hashCode * 524287 + i;
            }
        }
        return hashCode;
    }
    
}
