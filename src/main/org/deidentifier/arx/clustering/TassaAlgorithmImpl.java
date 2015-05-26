package org.deidentifier.arx.clustering;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.ARXInterface;
import org.deidentifier.arx.Data;

public class TassaAlgorithmImpl {
    
    private final ARXInterface iface;
    private double inititalInformationLoss;
    
    public double getInititalInformationLoss() {
        return inititalInformationLoss;
    }
    
    private double finalInformationLoss;

    public double getFinalInformationLoss() {
        return finalInformationLoss;
    }

    public TassaAlgorithmImpl(Data data, ARXConfiguration config) throws IOException {
        iface = new ARXInterface(data, config);
    }
    
    /**
     * 
     * @param alpha modifier for the initial size of clusters. has to be 0 < alpha <= 1
     * @param omega modifier for the maximum size of clusters. has to be 1 < omega <= 2
     * @return a list of clusters with the local optimum for generalization cost
     */
    
    public TassaClusterSet execute(double alpha, double omega) {


        // check for correct arguments
        if (alpha <= 0 || alpha > 1) {
            throw new IllegalArgumentException("executeTassa: Argument 'alpha' is out of bound: " + alpha);
        }
        if (omega <= 1 || omega > 2) {
            throw new IllegalArgumentException("executeTassa: Argument 'omega' is out of bound: " + omega);
        }
        
        // Input parameters of clustering
        
        final int[][] inputArrays = iface.getDataQI();
        final List<TassaRecord> dataSet = new LinkedList<>();
        for (final int[] record : inputArrays) {
            dataSet.add(new TassaRecord(record));
        }
        
        final int k = iface.getK();
        // k_0 is the initial cluster size
        final int k_0 = (int) Math.floor(alpha * k) > 0 ? (int) Math.floor(alpha * k) : 1;
        final int n = dataSet.size();
        
        // Output variable: Collection of clusters
        // initialized with random partition of data records with the cluster size alpha*k
        final TassaClusterSet output = new TassaClusterSet(dataSet, k_0, iface.getGeneralizationManager());
        double lastIL = getAverageGeneralizationCost(output);
        inititalInformationLoss = lastIL;
        
        if (iface.logging) {
            System.out.println("Initial average information loss: " + inititalInformationLoss + ", Initial cluster count: " + output.size());
        }
        
        // Helper variable to check, if records were changed
        boolean recordsChanged = true;
        int recordChangeCount = 0;

        HashSet<TassaCluster> tempOutput = new HashSet<>(output);
        HashSet<TassaCluster> modifiedClusters = new HashSet<>(tempOutput);
        
        while (recordsChanged) {
            // reset recordsChanged flag
            recordsChanged = false;
            HashSet<TassaCluster> clustersToCheck = new HashSet<>(modifiedClusters);
            modifiedClusters.clear();
            
            int recordCount = 1;
            final long initTime = System.nanoTime();
            long startTime = initTime;
            // Loop: check all records for improvement of information loss
            for (final TassaRecord record : dataSet) {
                if (iface.logging && (recordCount % iface.logNumberOfRecords == 0 || recordCount >= n)) {
                    final long stopTime = System.nanoTime();
                    System.out.println("#Clusters: " + clustersToCheck.size() +"/"+ tempOutput.size() + ", Record number: " + recordCount + ", Execution time: " + Math.round((stopTime - startTime) / 1000000.0) + " ms, Average time: " + Math.round((stopTime - initTime) * iface.logNumberOfRecords / (recordCount * 1000000.0)) + " ms");
                    startTime = stopTime;
                }
                recordCount++;
                final TassaCluster sourceCluster = record.getAssignedCluster();
                TassaCluster targetCluster = null;
                double deltaIL = Double.MAX_VALUE;
                
                // find cluster with minimal change of information loss
                if (clustersToCheck.contains(sourceCluster)) {
                    for (final TassaCluster cluster : tempOutput) {
                        if (cluster != sourceCluster) {
                            final double tempDelta = getChangeOfInformationLoss(record, cluster, n);
                            if (tempDelta < deltaIL) {
                                deltaIL = tempDelta;
                                targetCluster = cluster;
                            }
                        }
                    }
                }
                else {
                    for (final TassaCluster cluster : clustersToCheck) {
                        if (cluster != sourceCluster) {
                            final double tempDelta = getChangeOfInformationLoss(record, cluster, n);
                            if (tempDelta < deltaIL) {
                                deltaIL = tempDelta;
                                targetCluster = cluster;
                            }
                        }
                    }
                }
                
                if (sourceCluster.size() == 1 && targetCluster != null)
                {
                    // move record to target cluster
                    tempOutput.remove(targetCluster);
                    modifiedClusters.remove(targetCluster);
                    clustersToCheck.remove(targetCluster);
                    targetCluster.add(record);
                    tempOutput.add(targetCluster);
                    modifiedClusters.add(targetCluster);
                    clustersToCheck.add(targetCluster);
                    
                    // remove empty source cluster from all containing collections
                    tempOutput.remove(sourceCluster);
                    modifiedClusters.remove(sourceCluster);
                    clustersToCheck.remove(sourceCluster);
                    
                    // log the change for the next loop
                    /*if (!modifiedClusters.contains(targetCluster)) {
                        modifiedClusters.add(targetCluster);
                    }*/
                    recordsChanged = true;
                    recordChangeCount++;
                }
                
                // If change in information loss is negative, move record to new cluster
                else if (deltaIL < -0.0000000001 && targetCluster != null) {
                    
                    // remove record from source cluster
                    tempOutput.remove(sourceCluster);
                    modifiedClusters.remove(sourceCluster);
                    clustersToCheck.remove(sourceCluster);
                    sourceCluster.remove(record);
                    tempOutput.add(sourceCluster);
                    modifiedClusters.add(sourceCluster);
                    clustersToCheck.add(sourceCluster);
                    
                    // move record to target cluster
                    tempOutput.remove(targetCluster);
                    modifiedClusters.remove(targetCluster);
                    clustersToCheck.remove(targetCluster);
                    targetCluster.add(record);
                    tempOutput.add(targetCluster);
                    modifiedClusters.add(targetCluster);
                    clustersToCheck.add(targetCluster);
                    
                    // log the change for the next loop
                    /*if (!modifiedClusters.contains(targetCluster)) {
                        modifiedClusters.add(targetCluster);
                    }
                    if (!modifiedClusters.contains(sourceCluster)) {
                        modifiedClusters.add(sourceCluster);
                    }*/
                    recordsChanged = true;
                    recordChangeCount++;
                }
            }
            
            // Check for clusters greater w*k, split them and add them back to tempOutput
            final LinkedList<TassaCluster> bigClusters = new LinkedList<>();
            for (final Iterator<TassaCluster> itr = tempOutput.iterator(); itr.hasNext();) {
                final TassaCluster cluster = itr.next();
                if (cluster.size() > omega * k) {
                    itr.remove();
                    bigClusters.add(cluster);
                }
            }
            
            clustersToCheck.removeAll(bigClusters);
            modifiedClusters.removeAll(bigClusters);
            
            final LinkedList<TassaCluster> newClusters = new LinkedList<>();
            while (bigClusters.size() > 0) {
                Iterator<TassaCluster> itr = bigClusters.iterator();
                while (itr.hasNext()) {
                    newClusters.addAll(itr.next().splitCluster());
                    itr.remove();
                }
                itr = newClusters.iterator();
                while (itr.hasNext()) {
                    TassaCluster c = itr.next();
                    if (c.size() > omega * k) {
                        itr.remove();
                        bigClusters.add(c);
                    }
                }
            }
            
            modifiedClusters.addAll(newClusters);
            tempOutput.addAll(newClusters);
            clustersToCheck.addAll(newClusters);
            
            final double IL = getAverageGeneralizationCost(tempOutput);
            
            clustersToCheck.clear();
            recordChangeCount = 0;
            lastIL = IL;
        }
        
        // put small clusters into smallClusters collection
        final TassaClusterSet smallClusters = new TassaClusterSet(iface.getGeneralizationManager());
        
        for (final TassaCluster cluster : tempOutput) {
            if (cluster.size() < k) {
                smallClusters.add(cluster);
            }
        }
        // remove small clusters from tempOutput
        tempOutput.removeAll(smallClusters);
        
        // As long as there are clusters with size < k
        // merge closest two clusters and either
        // if size >= k, add them to output, or
        // if size < k, add them back to smallClusters

        final long initTime = System.nanoTime();
        long startTime = initTime;
        int mergeNumber = 0;
        
        // Add temporary clustering to output and merge clusters < k
        output.clear();
        output.addAll(tempOutput);
        
        while (smallClusters.size() > 1) {

            if (iface.logging && mergeNumber % iface.logNumberOfClusters == 0 && mergeNumber > 0) {
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
            output.mergeClosestPair(smallClusters.iterator().next());
        }
        
        finalInformationLoss = getAverageGeneralizationCost(output);
        
        System.out.println("Final average information loss: " + finalInformationLoss);
        
        
        final int[][] buffer = iface.getBuffer();
        ListIterator<TassaRecord> itr = dataSet.listIterator();
        
        if (buffer.length != dataSet.size()) {
            throw new RuntimeException("Something is wrong! The buffer length and the number of elements in our dataset don't match.");
        }
        
        
        for (int i = 0; i < buffer.length; i++) {
            final TassaRecord record = itr.next();
            buffer[i] = record.getAssignedCluster().getTransformation();
            //System.out.println(Arrays.toString(buffer[i]));
        }
        
        
        return output;
        
    }
    
    private double getChangeOfInformationLoss(TassaRecord movedRecord, TassaCluster targetCluster, int n) {
        
        final TassaCluster sourceCluster = movedRecord.getAssignedCluster();
        
        double deltaIL = (sourceCluster.getRemovedGC(movedRecord) * (sourceCluster.size() - 1)
                  + targetCluster.getAddedGC(movedRecord) * (targetCluster.size() + 1))
                  - (sourceCluster.getGC() * sourceCluster.size()
                  + targetCluster.getGC() * targetCluster.size());
        deltaIL /= n;
        
        return deltaIL;
    }
    
    public double getAverageGeneralizationCost(Iterable<TassaCluster> clusterList) {
        double result = 0.0;
        int numRecords = 0;
        for (final TassaCluster c : clusterList) {
            numRecords += c.size();
            result += c.getGC() * c.size();
        }
        return result / numRecords;
        
    }
}
