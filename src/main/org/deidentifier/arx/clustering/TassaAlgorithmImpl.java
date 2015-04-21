package org.deidentifier.arx.clustering;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.ARXInterface;
import org.deidentifier.arx.Data;



public class TassaAlgorithmImpl {
	
	private ARXInterface iface;


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
		if (alpha <= 0 || alpha > 1)
			throw new IllegalArgumentException("executeTassa: Argument 'alpha' is out of bound: " + alpha);
		if (omega <= 1 || omega > 2)
			throw new IllegalArgumentException("executeTassa: Argument 'omega' is out of bound: " + omega);
		
		// Input parameters of clustering
//		TassaCluster dataSet = new TassaCluster(iface.getDataQI(), iface);
		
		final ArrayList<TassaRecord> dataSet = new ArrayList<TassaRecord>(iface.getDataQI().length);
		for (int[] record : iface.getDataQI()) {
			dataSet.add(new TassaRecord(record));
		}
		
		int k = iface.getK();
		// k_0 is the initial cluster size
		int k_0 = (int)Math.floor(alpha*k) > 0 ? (int)Math.floor(alpha*k) : 1;
		int n = dataSet.size();
		
		// Output variable: Collection of clusters
		// initialized with random partition of data records with the cluster size alpha*k
		TassaClusterSet output = new TassaClusterSet(dataSet, k_0, iface);
		for(TassaCluster cluster : output) {
			cluster.assignAllRecords();
		}
		
		// Helper variable to check, if records were changed
		boolean recordsChanged = true;
		
		while (recordsChanged) {			
			// reset recordsChanged flag
			recordsChanged = false;
			
			int recordCount = 0;
			final long initTime = System.nanoTime();
			long startTime = System.nanoTime();
			// Loop: check all records for improvement of information loss
			for (TassaRecord record : dataSet) {
				if (recordCount % 100 == 0 && recordCount > 0) {
					long stopTime = System.nanoTime();
					
					System.out.println("Record number: " + recordCount + ", Execution time: " + ((stopTime-startTime)/1000000.0) + ", Average time: " + (System.nanoTime()-initTime)/(recordCount*10000.0));
					startTime = System.nanoTime();
				}
				recordCount++;
				final TassaCluster sourceCluster = record.assignedCluster;
				TassaCluster targetCluster = null;
				double deltaIL = Double.MAX_VALUE;
				
				
				// find cluster with minimal change of information loss
				for (TassaCluster cluster : output) {
					if (cluster != sourceCluster) {
						double tempDelta = getChangeOfInformationLoss(record, cluster, n);
						if (tempDelta < deltaIL) {
							deltaIL = tempDelta;
							targetCluster = cluster;
						}
					}
				}
				
				if (sourceCluster.size() == 1)
				{
					targetCluster.add(record);
					targetCluster.assignAllRecords();
					output.remove(sourceCluster);
					recordsChanged = true;
				}
				
				// If change in information loss is negative, move record to new cluster
				else if (deltaIL < 0) {
					targetCluster.add(record);
					targetCluster.assignAllRecords();
					sourceCluster.remove(record);
					recordsChanged = true;
				}
			}
			
			// Check for clusters greater w*k, split them and add them back to output
			TassaClusterSet newClusters = new TassaClusterSet(iface);
			for (Iterator<TassaCluster> itr = output.iterator(); itr.hasNext();) {
				TassaCluster cluster = itr.next();
				if (cluster.size() > omega * k) {
					itr.remove();
					newClusters.addAll(new TassaClusterSet(cluster, (int)Math.floor(cluster.size()/2), iface));
				}
			}
			for (TassaCluster c : newClusters) {
				c.assignAllRecords();
			}
			output.addAll(newClusters);
			
			double IL = 0.0;
			for (TassaCluster c : output) {
				IL += c.getGC() * c.size();
			}
			
			IL /= dataSet.size();
			
			System.out.println("Current total information loss: " + IL);
		}
		
		// put small clusters into smallClusters collection
		TassaClusterSet smallClusters = new TassaClusterSet(iface);
		
		for (TassaCluster cluster : output) {
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
		
		while (smallClusters.size() > 1) {
			TassaCluster mergedCluster = smallClusters.mergeClosestPair();
			
			if (mergedCluster.size() >= k) {
				mergedCluster.assignAllRecords();
				output.add(mergedCluster);
				smallClusters.remove(mergedCluster);
			}
		}
		
		if (smallClusters.size() == 1) {
			TassaCluster mergedCluster = output.mergeClosestPair(smallClusters.iterator().next());
			mergedCluster.assignAllRecords();
		}
		
		for (TassaCluster cluster : output) {
			cluster.getGC();
		}
		
		return output;
		
	}
	
	private double getChangeOfInformationLoss(TassaRecord movedRecord, TassaCluster targetCluster, int n) {
		
		double deltaIL = 0.0;
		TassaCluster sourceCluster = movedRecord.assignedCluster;
		
		deltaIL = sourceCluster.getRemovedGC(movedRecord) * (sourceCluster.size() - 1)
					+ targetCluster.getAddedGC(movedRecord) * (targetCluster.size() + 1)
					- sourceCluster.getGC() * sourceCluster.size()
					+ targetCluster.getGC() * targetCluster.size();
		deltaIL /= n;
		
		return deltaIL;
	}
}