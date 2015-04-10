package org.deidentifier.arx.clustering;

import java.io.IOException;
import java.util.List;

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
	
	public List<TassaCluster> executeTassa(double alpha, double omega) {

		// TODO: value "alpha" should be a variable 0 < a <= 1 provided by the ARXInterface
		// TODO: value "omega" should be a variable 1 < w <= 2 provided by the ARXInterface
		// check for correct arguments
		if (alpha <= 0 || alpha > 1)
			throw new IllegalArgumentException("executeTassa: Argument 'alpha' is out of bound: " + alpha);
		if (omega <= 1 || omega > 2)
			throw new IllegalArgumentException("executeTassa: Argument 'omega' is out of bound: " + omega);
		
		// Input parameters of clustering
		TassaCluster dataSet = new TassaCluster(iface.getDataQI(), iface);
		int k = iface.getK();
		// k_0 is the initial cluster size
		int n = dataSet.size();
		
		// Output variable: Collection of clusters
		// initialized with random partition of data records with the cluster size alpha*k
		TassaDatabase output = new TassaDatabase(dataSet.createRandomPartitioning((int)Math.floor(alpha*k)));
		
		// Helper variable to check, if records were changed
		boolean recordsChanged = true;
		
		while (recordsChanged) {
			
			// reset recordsChanged flag
			recordsChanged = false;
			
			// Loop: check all records for improvement of information loss
			for (TassaRecord record : dataSet) {
				
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
					output.remove(sourceCluster);
					recordsChanged = true;
				}
				
				// If change in information loss is negative, move record to new cluster
				else if (deltaIL < 0) {
					targetCluster.add(record);
					sourceCluster.remove(record);
					recordsChanged = true;
				}
			}
			
			// Check for clusters greater w*k and split them
			for (TassaCluster cluster : output) {
				if (cluster.size() > omega * k) {
					cluster.createRandomPartitioning(Math.floorDiv(cluster.size(), 2));
				}
			}
		}
		
		// TODO loop: as long as there are clusters of size smaller k, merge clusters
		TassaDatabase smallClusters = new TassaDatabase();
		
		for (TassaCluster cluster : output) {
			if (cluster.size() < k) {
				smallClusters.add(cluster);
				output.remove(cluster);
			}
		}
		
		
		smallClusters.sort(TassaCluster.SizeComparator);
		
		// As long as there are clusters with size < k
		// merge closest two clusters and either
		// if size >= k, add them to output, or
		// if size < k, add them back to smallClusters
		
		while (smallClusters.size() > 1) {
			TassaCluster mergedCluster = smallClusters.mergeClosestPair();
			
			if (mergedCluster.size() >= k) {
				output.add(mergedCluster);
			}
			else {
				smallClusters.add(mergedCluster);
			}
		}
		
		if (smallClusters.size() == 1) {
			output.add(smallClusters.mergeClosestPair(output));
		}
		
		return output;
		
	}
	
	private double getChangeOfInformationLoss(TassaRecord movedRecord, TassaCluster targetCluster, int n) {
		
		double deltaIL = 0.0;
		TassaCluster sourceCluster = movedRecord.assignedCluster;
		
		deltaIL = (1/n) * (
					sourceCluster.getRemovedGC(movedRecord) * (sourceCluster.size() - 1)
					+ targetCluster.getAddedGC(movedRecord) * (targetCluster.size() + 1)
					- sourceCluster.getGC() * sourceCluster.size()
					+ targetCluster.getGC() * targetCluster.size()
					);
		
		return deltaIL;
	}
}