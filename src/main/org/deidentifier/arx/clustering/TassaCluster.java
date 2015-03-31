package org.deidentifier.arx.clustering;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

@SuppressWarnings("serial")
class TassaCluster extends ArrayList<TassaRecord> {
	
	// TODO: add fields like transformation and buffer
	
	
	public TassaCluster(int initialSize) {
		super(initialSize);
	}
	
	public TassaCluster(int[][] inputArray) {
		this(inputArray.length);
		for (int i = 0; i < inputArray.length; i++) {
			this.add(new TassaRecord(inputArray[i]));
		}
	}
	
	public boolean add(int[] arrayRecord) {
		TassaRecord r = new TassaRecord(arrayRecord);
		r.assignedCluster = this;
		return this.add(r);
	}

	public boolean add(TassaCluster cluster) {
		boolean successful = true;
		
		for (TassaRecord record : cluster) {
			successful &= this.add(record);
		}
		
		return successful;
	}
	
	public boolean add(TassaRecord record) {
		record.assignedCluster = this;
		return super.add(record);
	}
	
	public TassaRecord remove(int index) {
		return super.remove(index);
	}
	
	
	
	// TODO: Mockup. Provide calculation of generalization cost.
	// TODO: Maybe merge methods by changing TassaRecord to TassaCluster.
	public double getIL() {
		return 1.5;
	}
	
	public double getAddedIL(TassaRecord addedRecord) {
		// TODO: Mockup
		return -1.2;
	}
	
	public double getRemovedIL(TassaRecord removedRecord) {
		// TODO: Mockup
		return 1.1;
	}
	
	public double getMergedIL(TassaCluster mergeCandidate) {
		// TODO: Mockup
		return 1.0;
	}
	
	/**
	 * Creates a random partitioning of the cluster.
	 *
	 * @param k
	 *            The number of Records per cluster.
	 * @return A list of random clusters with max. k Records
	 */
	public List<TassaCluster> createRandomPartitioning(int k) {
		
		// shuffle dataset to prepare random partitioning
		Collections.shuffle(this);
		
		// calculate number of clusters
		int numberOfClusters = (int) Math.floor(this.size() / k);
		// calculate number of clusters, that will have k + 1 records
		int additionalRecords = this.size() % k;

		// create list of clusters as return container
		List<TassaCluster> clusterList = new ArrayList<TassaCluster>(numberOfClusters);
		Iterator<TassaRecord> iter = this.iterator();
		
		for (int i = 0; i < numberOfClusters; i++) {
			
			// until all additional records are distributed
			// each cluster will have k + 1 records
			int addRecord = (i < additionalRecords) ? 1 : 0;
			
			// create cluster object with space for k or k+1 records
			TassaCluster c = new TassaCluster(k + addRecord);
			
			// iterate through each element
			for (int j = 0; j < k + addRecord; j++) {
				c.add(iter.next());
			}
			
			// add cluster to clusterList
			clusterList.add(c);
		}
		
		return clusterList;
		
	}
	
	public TassaCluster getClosestCluster(List<TassaCluster> clusterList) {
		
		
		// TODO: Mockup
		return new TassaCluster(0);
	}
	
	public static Comparator<TassaCluster> SizeComparator = new Comparator<TassaCluster>() {
		
		public int compare(TassaCluster l1, TassaCluster l2) {
			return l1.size() - l2.size();
		}
	};
	
	
}