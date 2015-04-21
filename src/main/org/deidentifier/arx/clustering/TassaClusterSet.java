package org.deidentifier.arx.clustering;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.deidentifier.arx.ARXInterface;

public class TassaClusterSet extends ArrayList<TassaCluster> {
	
	/**
	 * 
	 */
	
	private ARXInterface iface;
	
	public TassaClusterSet(List<TassaRecord> inputDataSet, int k, ARXInterface iface) {
		this.iface = iface;
		this.createRandomPartitioning(inputDataSet, k);
	}
	
	public TassaClusterSet(List<TassaCluster> listOfClusters, ARXInterface iface) {
		super(listOfClusters);
		this.iface = iface;
	}
	
	public TassaClusterSet(ARXInterface iface) {
		this.iface = iface;
	}
	
	/**
	 * Creates a random partitioning of clusters with the given records.
	 *
	 * @param inputDataSet The records that will be distributed among the clusters
	 * @param k            The number of Records per cluster.
	 */
	public void createRandomPartitioning(List<TassaRecord> inputDataSet, int k) {
		
		// shuffle dataset to prepare random partitioning
		Collections.shuffle(inputDataSet);
		
		// calculate number of clusters
		int numberOfClusters = (int) Math.floor(inputDataSet.size() / k);
		// calculate number of clusters, that will have k + 1 records
		int additionalRecords = inputDataSet.size() % k;

		// create list of clusters as return container
		this.ensureCapacity(numberOfClusters);
		Iterator<TassaRecord> iter = inputDataSet.iterator();
		
		for (int i = 0; i < numberOfClusters; i++) {
			
			// until all additional records are distributed
			// each cluster will have k + 1 records
			int addRecord = (i < additionalRecords) ? 1 : 0;
			
			// create cluster object with space for k or k+1 records
			ArrayList<TassaRecord> c = new ArrayList<TassaRecord>(k + addRecord);
			
			// iterate through each element
			for (int j = 0; j < k + addRecord; j++) {
				c.add(iter.next());
			}
			
			// add cluster to clusterList
			this.add(new TassaCluster(c, iface));
		}
	}

	private static final long serialVersionUID = 1899366651589072401L;

	// TODO: kd-tree as general data structure for records, clusters and cluster sets???
	/**
	 * Merges the closest pair of clusters in this set of clusters.
	 * @return 
	 */
	public TassaCluster mergeClosestPair() {
		
		double closestPairDistance = Double.MAX_VALUE;
		TassaCluster[] closestPair = new TassaCluster[2];
		
		for (int i = 0; i < this.size(); i++) {
			double closestDistance = Double.MAX_VALUE;
			TassaCluster currentCluster = this.get(i);
			TassaCluster closestCluster = null;
			for (int j = i + 1; j < this.size(); j++) {
				double dist = getCommonGC(currentCluster, this.get(j));
				if (dist < closestDistance) {
					closestDistance = dist;
					closestCluster = this.get(j);
				}
			}
			if (closestDistance < closestPairDistance) {
				closestPairDistance = closestDistance;
				closestPair[0] = currentCluster;
				closestPair[1] = closestCluster;
			}
		}
		// TODO: We need a merge method, that calculates the transformation in a cheap way
		closestPair[0].addAll(closestPair[1]);
		this.remove(closestPair[1]);
		return closestPair[0];
	}
	
	public TassaCluster mergeClosestPair(TassaCluster inputCluster) {
		
		double closestDistance = Double.MAX_VALUE;
		TassaCluster closestCluster = null;
		
		for (TassaCluster currentCluster : this) {
			double dist = getCommonGC(inputCluster, currentCluster);
			if (dist < closestDistance) {
				closestDistance = dist;
				closestCluster = currentCluster;
			}
		}
		closestCluster.addAll(inputCluster);
		return closestCluster;
	}
	
	public double getCommonGC(TassaCluster c1, TassaCluster c2) {
		TassaCluster tempCluster = new TassaCluster(c1);
		tempCluster.addAll(c2);
		return tempCluster.getGC();
	}

}
