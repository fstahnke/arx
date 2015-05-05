package org.deidentifier.arx.clustering;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class TassaClusterSet extends LinkedList<TassaCluster> {
    
    /**
     * 
     */

    private final GeneralizationManager manager;
    private final HashMap<TassaCluster,TreeMap<Double,TassaCluster>> clusterDistances;
    
    public TassaClusterSet(List<TassaRecord> inputDataSet, int k, GeneralizationManager manager) {
        this(manager);
        createRandomPartitioning(inputDataSet, k);
    }
    
    public TassaClusterSet(GeneralizationManager manager) {
        this.manager = manager;
        clusterDistances = new HashMap<TassaCluster, TreeMap<Double, TassaCluster>>();
    }
    
    /**
     * Creates a random partitioning of clusters with the given records.
     *
     * @param inputDataSet The records that will be distributed among the clusters
     * @param k The number of Records per cluster.
     */
    private void createRandomPartitioning(Collection<TassaRecord> inputDataSet, int k) {
        
        // shuffle data set to prepare random partitioning
        Collections.shuffle((List<TassaRecord>) inputDataSet);
        
        // calculate number of clusters
        final int numberOfClusters = (int) Math.floor(inputDataSet.size() / k);
        // calculate number of clusters, that will have k + 1 records
        final int additionalRecords = inputDataSet.size() % k;
        
        // create list of clusters as return container
        final Iterator<TassaRecord> iter = inputDataSet.iterator();
        
        for (int i = 0; i < numberOfClusters; i++) {
            
            // until all additional records are distributed
            // each cluster will have k + 1 records
            final int addRecord = (i < additionalRecords) ? 1 : 0;
            
            // create cluster object with space for k or k+1 records
            final LinkedList<TassaRecord> c = new LinkedList<>();
            
            // iterate through each element
            for (int j = 0; j < k + addRecord; j++) {
                c.add(iter.next());
            }
            
            // add cluster to clusterList
            this.add(new TassaCluster(c, manager));
        }
    }
    
    private static final long serialVersionUID = 1899366651589072401L;
    
    private void calculateClusterDistances() {
        clusterDistances.clear();
        
        for (TassaCluster c1 : this) {
            for (TassaCluster c2 : this) {
                if (c1 != c2) {
                    
                }
            }
        }
    }
    
    /**
     * Merges the closest pair of clusters in this set of clusters.
     * @return
     */
    public TassaCluster mergeClosestPair() {
        
        double closestPairDistance = Double.MAX_VALUE;
        final TassaCluster[] closestPair = new TassaCluster[2];

        for (int i = 0; i < size(); i++) {

            double closestDistance = Double.MAX_VALUE;
            final TassaCluster currentCluster = get(i);
            TassaCluster closestCluster = null;
            for (int j = i + 1; j < size(); j++) {
                final double dist = currentCluster.getAddedGC(get(j));
                if (dist < closestDistance) {
                    closestDistance = dist;
                    closestCluster = get(j);
                }
            }
            if (closestDistance < closestPairDistance) {
                closestPairDistance = closestDistance;
                closestPair[0] = currentCluster;
                closestPair[1] = closestCluster;
            }
        }
        
        closestPair[0].addAll(closestPair[1]);
        this.remove(closestPair[1]);
        return closestPair[0];
    }
    
    
    
    public TassaCluster mergeClosestPair(TassaCluster inputCluster) {
        
        double closestDistance = Double.MAX_VALUE;
        TassaCluster closestCluster = null;
        
        for (final TassaCluster currentCluster : this) {
            final double dist = inputCluster.getAddedGC(currentCluster);
            if (dist < closestDistance) {
                closestDistance = dist;
                closestCluster = currentCluster;
            }
        }
        closestCluster.addAll(inputCluster);
        return closestCluster;
    }
    
    public double getAverageGeneralizationCost() {
        double result = 0.0;
        int numRecords = 0;
        for (final TassaCluster c : this) {
            numRecords += c.size();
            result += c.getGeneralizationCost() * c.size();
        }
        return result / numRecords;
        
    }
    
}
