package org.deidentifier.arx.clustering;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import com.carrotsearch.hppc.ObjectDoubleOpenHashMap;

public class TassaCluster extends LinkedList<TassaRecord> implements IGeneralizable {
    
    /** The number of attributes. */
    private final int                          numAtt;
    
    private int[]                              generalizationLevels;
    private int[]                              transformation;
    public int[] getTransformation() {
        return transformation;
    }

    private final ObjectDoubleOpenHashMap<TassaRecord> removedNodeGC;
    private final ObjectDoubleOpenHashMap<IGeneralizable> addedGCCache;
    
    // caching of calculated values
    private double                             generalizationCost;
    private int                                hashCode;
    
    /**
     * The modification counter at the time of the last update.
     * We use this to detect, whether the cluster was changed and
     * if we have to update generalizationCost and transformationNodes again.
     */
    private int                                lastModCount;

    private final GeneralizationManager        manager;
    
    public TassaCluster(Collection<? extends TassaRecord> recordCollection, GeneralizationManager manager) {
        super();
        this.manager = manager;
        numAtt = manager.getNumAttributes();
        generalizationLevels = new int[numAtt];
        removedNodeGC = new ObjectDoubleOpenHashMap<>();
        addedGCCache = new ObjectDoubleOpenHashMap<>();
        super.addAll(recordCollection);
        update(this);
    }
    
    /**
     * Structural methods (add, remove)    
     */
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
    
    public LinkedList<TassaCluster> splitCluster() {
        Collections.shuffle(this);
        
        Iterator<TassaRecord> itr = this.iterator();
        final int splitSize = (int)Math.floor(this.size() / 2d);
        
        LinkedList<TassaRecord> records = new LinkedList<>();
        
        for (int i = 0; i < splitSize; i++) {
            records.add(itr.next());
            itr.remove();
        }
        
        LinkedList<TassaCluster> result = new LinkedList<>();
        result.add(this);
        result.add(new TassaCluster(records, manager));
        
        this.update(this);
        
        return result;        
    }

    /**
     * Getters and setters
     */
    
    @Override
    public int[] getValues() {
    	return getFirst().getValues();
    }

    @Override
    public double getGeneralizationCost() {
        if (lastModCount < modCount) {
            update(this);
        }
        return generalizationCost;
    }

    @Override
    public int[][] getValuesByAttribute() {

        final int[][] valuesByAttribute = new int[numAtt][this.size()];
        Iterator<TassaRecord> itr = this.iterator();
        for (int i = 0; i < valuesByAttribute[0].length; i++)
        {
            int[] recordValues = itr.next().getValues();
            for (int j = 0; j < numAtt; j++) {
                valuesByAttribute[j][i] = recordValues[j];
            }
        }
        
        return valuesByAttribute;
    }
    
    public double getAddedGC(IGeneralizable addedObject) {
        double result = 0;
        
        if (addedObject instanceof TassaRecord) {
            /*if (result != 0) {
                result =  addedGCCache.get(addedObject);
            }
            else */{
                TassaRecord record = (TassaRecord)addedObject;
                result = manager.calculateGeneralizationCost(record, manager.calculateGeneralizationLevels(record.getValues(), getFirst().getValues(), generalizationLevels));
                //addedGCCache.put(record, result);
            }
        }
        if (addedObject instanceof TassaCluster) {
            result = addedGCCache.getOrDefault(addedObject, 0);
            if (result == 0) {
                TassaCluster cluster = (TassaCluster)addedObject;
                final int[] levels = new int[numAtt];
                for (int i = 0; i < numAtt; i++) {
                    levels[i] = Math.max(this.generalizationLevels[i], cluster.generalizationLevels[i]);
                }
                result = manager.calculateGeneralizationCost(cluster.getFirst(), manager.calculateGeneralizationLevels(cluster.getFirst(), getFirst(), levels));
                addedGCCache.put(cluster, result);
            }
        }
        return result;
    }
    
    public double getRemovedGC(TassaRecord record) {
        double result = 0;
        if (size() > 1) {
            result = removedNodeGC.getOrDefault(record, 0);
            // We don't have a cached value for the GC without this record
            if (result == 0) {
                // The record was not yet removed,
                // so we have to add it back after the calculation
                final boolean recordExisted = super.remove(record);
                result = manager.calculateGeneralizationCost(getFirst(), manager.calculateGeneralizationLevels(this.getValuesByAttribute()));
                // If record existed before removal, we have to put it back and add GC to the cache
                if (recordExisted) {
                    removedNodeGC.put(record, result);
                    super.add(record);
                    lastModCount = modCount;
                }
            }
        }
        else {
            return 0;
        }
        return result;
    }
    
    /**
     * 
     * @param addedObject
     * 
     * We want to update:
     * - generalizationLevels
     * - generalizationCost
     * - transformation
     * - the hash code
     * - the removedGC cache
     */
    private void update(Object addedObject) {
        final int[] levels = new int[numAtt];
        if (addedObject instanceof TassaCluster) {
            TassaCluster cluster = (TassaCluster)addedObject;
            if (cluster == this) {
                generalizationLevels = manager.calculateGeneralizationLevels(this.getValuesByAttribute());
            }
            else {
                for (int i = 0; i < numAtt; i++) {
                    levels[i] = Math.max(this.generalizationLevels[i], cluster.generalizationLevels[i]);
                }
                generalizationLevels = manager.calculateGeneralizationLevels(cluster.getFirst(), getFirst(), levels);
                
            }
            generalizationCost = manager.calculateGeneralizationCost(getFirst(), generalizationLevels);
            assignAllRecords();
        }
        if (addedObject instanceof TassaRecord) {
            TassaRecord record = (TassaRecord)addedObject;
            if (record.getAssignedCluster() != this) {
                record.setAssignedCluster(this);
                generalizationLevels = manager.calculateGeneralizationLevels(record, this, generalizationLevels);
                generalizationCost = getAddedGC(record);
            }
            else {
                generalizationLevels = manager.calculateGeneralizationLevels(this.getValuesByAttribute());
                generalizationCost = getRemovedGC(record);
            }
        }
        addedGCCache.clear();
        removedNodeGC.clear();
        transformation = manager.calculateTransformation(getFirst(), generalizationLevels);
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
        /*if (obj instanceof TassaCluster && obj.hashCode() == this.hashCode()) {
            TassaCluster cluster = (TassaCluster)obj;
            if (cluster.size() == this.size()) {
                boolean equals = true;
                final int[] transformation2 = cluster.getTransformation();
                for (int i = 0; equals && i < transformation.length; i++) {
                    equals &= (transformation[i] == transformation2[i]);
                }
                return equals;
            }
        }*/
        return false;
    }
    
    @Override
    public int hashCode() {
        if (hashCode == 0 || lastModCount != modCount) {
            hashCode = size();
            for (int i : transformation) {
                hashCode = hashCode * 524287 + i;
            }
        }
        return hashCode;
    }

    
    
}
