package org.deidentifier.arx.clustering;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.ARXInterface;
import org.deidentifier.arx.Data;

public class TassaAlgorithmImpl {
    
    private final ARXInterface iface;
    
    public TassaAlgorithmImpl(Data data, ARXConfiguration config) throws IOException {
        iface = new ARXInterface(data, config);
    }
    
    /**
     * 
     * @param alpha modifier for the initial size of clusters. has to be 0 < alpha <= 1
     * @param omega modifier for the maximum size of clusters. has to be 1 < omega <= 2
     * @return a list of clusters with the local optimum for generalization cost
     */
    
    public TassaClusterSet executeTassa(double alpha, double omega) {
        
        // TODO: value "alpha" should be a variable 0 < a <= 1 provided by the ARXInterface
        // TODO: value "omega" should be a variable 1 < w <= 2 provided by the ARXInterface
        // check for correct arguments
        if (alpha <= 0 || alpha > 1) {
            throw new IllegalArgumentException("executeTassa: Argument 'alpha' is out of bound: " + alpha);
        }
        if (omega <= 1 || omega > 2) {
            throw new IllegalArgumentException("executeTassa: Argument 'omega' is out of bound: " + omega);
        }
        
        // Input parameters of clustering
        // TassaCluster dataSet = new TassaCluster(iface.getDataQI(), iface);
        
        final LinkedList<TassaRecord> dataSet = new LinkedList<>();
        for (final int[] record : iface.getDataQI()) {
            dataSet.add(new TassaRecord(record));
        }
        
        final int k = iface.getK();
        // k_0 is the initial cluster size
        final int k_0 = (int) Math.floor(alpha * k) > 0 ? (int) Math.floor(alpha * k) : 1;
        final int n = dataSet.size();
        
        // Output variable: Collection of clusters
        // initialized with random partition of data records with the cluster size alpha*k
        final TassaClusterSet output = new TassaClusterSet(dataSet, k_0, iface.getGeneralizationManager());
        System.out.println("Initial average information loss: " + output.getAverageGeneralizationCost());
        
        /**
         * for testing purposes
         */
        final int testRounds = 100;
        double testGC = 0.0;
        for (int i = 0; i < testRounds; i++) {
            TassaClusterSet testSet = new TassaClusterSet(dataSet, k_0, iface.getGeneralizationManager());
            testGC += testSet.getAverageGeneralizationCost();
        }
        testGC /= testRounds;
        System.out.println("Test generalization cost: " + testGC);
        
        
        //final LinkedList<TassaCluster> output = new LinkedList<>(output2);
        
        
        // Helper variable to check, if records were changed
        boolean recordsChanged = true;
        int recordChangeCount = 0;
        
        LinkedList<TassaCluster> modifiedClusters = new LinkedList<>(output);
        double lastIL = 0;
        
        while (recordsChanged) {
            // reset recordsChanged flag
            recordsChanged = false;
            HashSet<TassaCluster> clustersToCheck = new HashSet<>(modifiedClusters);
            modifiedClusters.clear();
            
            int recordCount = 0;
            final long initTime = System.nanoTime();
            long startTime = initTime;
            // Loop: check all records for improvement of information loss
            for (final TassaRecord record : dataSet) {
                if (iface.logging && recordCount % iface.logNumberOfRecords == 0 && recordCount > 0) {
                    final long stopTime = System.nanoTime();
                    System.out.println("#Clusters: " + clustersToCheck.size() +"/"+ output.size() + ", Record number: " + recordCount + ", Execution time: " + Math.round((stopTime - startTime) / 1000000.0) + " ms, Average time: " + Math.round((stopTime - initTime) * iface.logNumberOfRecords / (recordCount * 1000000.0)) + " ms");
                    startTime = stopTime;
                }
                recordCount++;
                final TassaCluster sourceCluster = record.getAssignedCluster();
                TassaCluster targetCluster = null;
                double deltaIL = Double.MAX_VALUE;
                
                // find cluster with minimal change of information loss
                for (final TassaCluster cluster : clustersToCheck) {
                    if (cluster != sourceCluster) {
                        final double tempDelta = getChangeOfInformationLoss(record, cluster, n);
                        if (tempDelta < deltaIL) {
                            deltaIL = tempDelta;
                            targetCluster = cluster;
                        }
                    }
                }
                
                if (sourceCluster.size() == 1 && targetCluster != null)
                {
                    // move record to target cluster
                    clustersToCheck.remove(targetCluster);
                    targetCluster.add(record);
                    clustersToCheck.add(targetCluster);
                    
                    // remove empty source cluster from all containing collections
                    output.remove(sourceCluster);
                    modifiedClusters.remove(sourceCluster);
                    clustersToCheck.remove(sourceCluster);
                    
                    // log the change for the next loop
                    if (!modifiedClusters.contains(targetCluster)) {
                        modifiedClusters.add(targetCluster);
                    }
                    recordsChanged = true;
                    recordChangeCount++;
                }
                
                // If change in information loss is negative, move record to new cluster
                else if (deltaIL < -0.0000000001 && targetCluster != null) {
                    // remove record from source cluster
                    clustersToCheck.remove(sourceCluster);
                    sourceCluster.remove(record);
                    clustersToCheck.add((TassaCluster) sourceCluster);
                    
                    // move record to target cluster
                    clustersToCheck.remove(targetCluster);
                    targetCluster.add(record);
                    clustersToCheck.add(targetCluster);
                    
                    // log the change for the next loop
                    if (!modifiedClusters.contains(targetCluster)) {
                        modifiedClusters.add(targetCluster);
                    }
                    if (!modifiedClusters.contains(sourceCluster)) {
                        modifiedClusters.add(sourceCluster);
                    }
                    recordsChanged = true;
                    recordChangeCount++;
                }
            }
            
            // Check for clusters greater w*k, split them and add them back to output
            final LinkedList<TassaCluster> bigClusters = new LinkedList<>();
            final TassaClusterSet newClusters = new TassaClusterSet(iface.getGeneralizationManager());
            for (final Iterator<TassaCluster> itr = output.iterator(); itr.hasNext();) {
                final TassaCluster cluster = itr.next();
                if (cluster.size() > omega * k) {
                    itr.remove();
                    modifiedClusters.remove(cluster);
                    bigClusters.add(cluster);
                    newClusters.addAll(new TassaClusterSet(cluster, (int) Math.floor(cluster.size() / 2), iface.getGeneralizationManager()));
                }
            }
            modifiedClusters.addAll(newClusters);
            output.addAll(newClusters);
            
            final double IL = output.getAverageGeneralizationCost();
            
            System.out.println("Current average information loss: " + IL + ", DeltaIL: " + (IL-lastIL) + ", Records changed: " + recordChangeCount + ", Clusters to check: " + clustersToCheck.size() +"/"+ output.size());
            recordChangeCount = 0;
            lastIL = IL;
        }
        
        // put small clusters into smallClusters collection
        final TassaClusterSet smallClusters = new TassaClusterSet(iface.getGeneralizationManager());
        
        for (final TassaCluster cluster : output) {
            if (cluster.size() < k) {
                smallClusters.add(cluster);
            }
        }
        // remove small clusters from output
        output.removeAll(smallClusters);
        
        // As long as there are clusters with size < k
        // merge closest two clusters and either
        // if size >= k, add them to output, or
        // if size < k, add them back to smallClusters

        final long initTime = System.nanoTime();
        long startTime = initTime;
        int mergeNumber = 0;
        
        while (smallClusters.size() > 1) {

            if (iface.logging && mergeNumber > 0) {
                final long stopTime = System.nanoTime();
                System.out.println("Merged clusters: " + mergeNumber + ", Execution time: " + Math.round((stopTime - startTime) / 1000000.0) + " ms, Average time: " + Math.round((stopTime - initTime) / (mergeNumber * 1000000.0)) + " ms");
                startTime = stopTime;
            }
            mergeNumber++;
            
            final TassaCluster mergedCluster = smallClusters.mergeClosestPair();
            
            if (mergedCluster.size() >= k) {
                output.add(mergedCluster);
                smallClusters.remove(mergedCluster);
            }
        }
        
        if (smallClusters.size() == 1) {
            output.mergeClosestPair(smallClusters.getFirst());
        }
        
        
        System.out.println("Final average information loss: " + output.getAverageGeneralizationCost());
        
        return output;
        
    }
    
    private double getChangeOfInformationLoss(TassaRecord movedRecord, TassaCluster targetCluster, int n) {
        
        final TassaCluster sourceCluster = movedRecord.getAssignedCluster();
        
        double deltaIL = (sourceCluster.getRemovedGC(movedRecord) * (sourceCluster.size() - 1)
                  + targetCluster.getAddedGC(movedRecord) * (targetCluster.size() + 1))
                  - (sourceCluster.getGeneralizationCost() * sourceCluster.size()
                  + targetCluster.getGeneralizationCost() * targetCluster.size());
        deltaIL /= n;
        
        return deltaIL;
    }
}
