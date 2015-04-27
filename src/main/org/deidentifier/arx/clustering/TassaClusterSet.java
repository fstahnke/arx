package org.deidentifier.arx.clustering;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.deidentifier.arx.ARXInterface;

public class TassaClusterSet extends LinkedList<TassaCluster> {
    
    /**
     * 
     */
    
    private final ARXInterface iface;
    
    public TassaClusterSet(List<TassaRecord> inputDataSet, int k, ARXInterface iface) {
        this.iface = iface;
        createRandomPartitioning(inputDataSet, k);
    }
    
    public TassaClusterSet(ARXInterface iface) {
        this.iface = iface;
    }
    
    /**
     * Creates a random partitioning of clusters with the given records.
     *
     * @param inputDataSet The records that will be distributed among the clusters
     * @param k The number of Records per cluster.
     */
    private void createRandomPartitioning(List<TassaRecord> inputDataSet, int k) {
        
        // shuffle dataset to prepare random partitioning
        Collections.shuffle(inputDataSet);
        
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
            this.add(new TassaCluster(c, iface));
        }
    }
    
    private static final long serialVersionUID = 1899366651589072401L;

    
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
    
}
