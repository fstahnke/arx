package org.deidentifier.arx.clustering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;


public class TassaClusterSet extends ArrayList<TassaCluster> {
    
    /**
     * 
     */
    private List<TassaRecord> dataSet;
    
    public List<TassaRecord> getDataSet() {
        return dataSet;
    }
    
    private final GeneralizationManager manager;
    private final TreeSet<ClusterPair> clusterPairs;
    
    public TassaClusterSet(List<TassaRecord> inputDataSet, int k, GeneralizationManager manager) {
        this(manager);
        this.dataSet = inputDataSet;
        createRandomPartitioning(inputDataSet, k);
    }

    public TassaClusterSet(GeneralizationManager manager) {
        this.manager = manager;
        clusterPairs = new TreeSet<>();
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
    
    /*
    private void calculateClusterPairs() {
        clusterPairs.clear();
        
        for (Iterator<TassaCluster> itr1 = this.iterator(); itr1.hasNext();) {
            // Get next cluster to check
            TassaCluster c1 = itr1.next();
            // Create second iterator and iterate it to the current list element of itr1
            Iterator<TassaCluster> itr2 = this.iterator();
            for (TassaCluster c2 = itr2.next(); c1 != c2; c2 = itr2.next());
            // From here on calculate the missing pairs
            while (itr2.hasNext()) {
                TassaCluster c2 = itr2.next();
                ClusterPair newPair = new ClusterPair(c1, c2);
                clusterPairs.add(newPair);
            }
        }
    }
    */
    /**
     * Merges the closest pair of clusters in this set of clusters. New Version.
     * @return
     */
    /*
    public TassaCluster mergeClosestPair2() {
        if (clusterPairs.isEmpty()) {
            calculateClusterPairs();
        }
        
        ClusterPair closestPair = clusterPairs.first();
        while (!this.contains(closestPair.getFirst()) || !this.contains(closestPair.getSecond())) {
            clusterPairs.remove(closestPair);
            closestPair = clusterPairs.first();
        }
        
        closestPair.getFirst().addAll(closestPair.getSecond());
        
        return closestPair.getFirst();
    }
    */
    
    
    /**
     * Merges the closest pair of clusters in this set of clusters.
     * @return
     */
    public TassaCluster mergeClosestPair() {
        clusterPairs.clear();
        
        double closestPairDistance = Double.MAX_VALUE;
        final TassaCluster[] closestPair = new TassaCluster[2];
        
        for (Iterator<TassaCluster> itr = this.iterator(); itr.hasNext();) {
            
            double closestDistance = Double.MAX_VALUE;
            TassaCluster closestCluster = null;
            // Get next cluster to check
            TassaCluster c1 = itr.next();
            
            Iterator<TassaCluster> itr2 = this.iterator();
            // Move to the next element until the iterator reaches the current list element
            for (TassaCluster c2 = itr2.next(); c1 != c2; c2 = itr2.next());
            // From here on calculate the missing distances
            while (itr2.hasNext()) {
                TassaCluster c2 = itr2.next();
                double dist = c1.getAddedGC(c2);
                if (dist < closestDistance) {
                    closestDistance = dist;
                    closestCluster = c2;
                }
            }
            if (closestDistance < closestPairDistance) {
                closestPairDistance = closestDistance;
                closestPair[0] = c1;
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
