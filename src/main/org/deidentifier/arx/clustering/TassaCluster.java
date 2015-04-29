package org.deidentifier.arx.clustering;

import java.util.Collection;
import java.util.HashMap;
import org.deidentifier.arx.ARXInterface;

public class TassaCluster extends AbstractCluster {
    
    private static final long                  serialVersionUID = 1L;
    
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

    private ARXInterface                       iface;
    
    public TassaCluster(Collection<AbstractCluster> recordCollection, ARXInterface iface) {
        super();
        this.iface = iface;
        numAtt = iface.getNumAttributes();
        generalizationLevels = new int[numAtt];
        removedNodeGC = new HashMap<>();
        super.addAll(recordCollection);
        update(this);
    }
    
    private void assignAllRecords() {
        for (final AbstractCluster record : this) {
            record.setAssignedCluster(this);
        }
    }
    
    @Override
    public boolean add(AbstractCluster newRecord) {
        final boolean success = super.add(newRecord);
        update(newRecord);
        return success;
    }
    
    @Override
    public final boolean addAll(Collection<? extends AbstractCluster> recordCollection) {
        final boolean success = super.addAll(recordCollection);
        update(recordCollection);
        return success;
    }
    
    public boolean remove(AbstractCluster record) {
        final boolean success = super.remove(record);
        update(record);
        return success;
    }
    
    @Override
    public double getGeneralizationCost() {
        if (lastModCount < modCount) {
            update(this);
        }
        return generalizationCost;
    }
    
    private int[] getGeneralizationLevels(AbstractCluster cluster, int[] currentGeneralizationLevels) {
        int[] result = new int[numAtt];
        
        if (cluster instanceof TassaRecord) {
            final int[] record1 = cluster.getTransformedValues();
            final int[] record2 = getFirst().getTransformedValues();
            for (int i = 0; i < numAtt; i++) {
                result[i] = iface.getHierarchyTree(i).getGeneralizationLevel(new int[] { record1[i], record2[i] }, currentGeneralizationLevels[i]);
            }
            
        }
        
        if (cluster instanceof TassaCluster) {
            if (cluster == this) {
                for (int i = 0; i < numAtt; i++) {
                    final int[] dataColumn = new int[cluster.size()];
                    int j = 0;
                    for (final AbstractCluster record : cluster) {
                        dataColumn[j] = record.getTransformedValues()[i];
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
    
    public double getAddedGC(AbstractCluster addedObject) {
        double result = 0;
        if (addedObject instanceof TassaCluster) {
            TassaCluster cluster = (TassaCluster)addedObject;
            final int[] levels = new int[numAtt];
            for (int i = 0; i < numAtt; i++) {
                levels[i] = Math.max(this.generalizationLevels[i], cluster.generalizationLevels[i]);
            }
            result = getGC_LM(cluster.getFirst().getTransformedValues(), this.getGeneralizationLevels(cluster.getFirst(), levels));
        }
        if (addedObject instanceof TassaRecord) {
            TassaRecord record = (TassaRecord)addedObject;
            result = getGC_LM(record.recordContent, this.getGeneralizationLevels(record, generalizationLevels));
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
                    result = getGC_LM(getFirst().getTransformedValues(), getGeneralizationLevels(this, new int[numAtt]));
                    removedNodeGC.put(record, result);
                    super.add(record);
                    lastModCount = modCount;
                }
                // The record has already been removed,
                // so we don't have to put it back
                else {
                    result = getGC_LM(getFirst().getTransformedValues(), getGeneralizationLevels(this, new int[numAtt]));
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
    
    private void update(Collection<? extends AbstractCluster> addedObject) {
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
            generalizationCost = getGC_LM(getFirst().getTransformedValues(), generalizationLevels);
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
    
    @Override
    public int[] getTransformedValues() {
        final int[] record = getFirst().getTransformedValues();
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
                final int[] transformation = getTransformedValues();
                final int[] transformation2 = cluster.getTransformedValues();
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
            final int[] transformation = getTransformedValues();
            for (int i : transformation) {
                hashCode = hashCode * 524287 + i;
            }
        }
        return hashCode;
    }
    
}
