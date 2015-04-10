package org.deidentifier.arx.clustering;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.deidentifier.arx.ARXInterface;
import org.deidentifier.arx.framework.data.DataManager;


class TassaCluster extends ArrayList<TassaRecord> {

	private static final long serialVersionUID = 1L;
	
	
	private ARXInterface iface;
	private DataManager manager;
	
	// List of transformation nodes for every attribute
	// they contain level of transformation, mapping key, all contained values
	private ArrayList<GeneralizationNode> transformationNodes;
	
	// caching of calculated values
	private double informationLoss;
	private boolean clusterChanged = true;
	
	public double getInformationLoss() {
		if (clusterChanged) {
			updateGeneralization();
		}
		return informationLoss;
	}


	private void updateGeneralization() {
		
		this.updateTransformation();
		informationLoss = this.getGC_LM();
		clusterChanged = false;
	}
	

	/**
	 * Update transformation of this Cluster.
	 */
	private void updateTransformation() {		
		for (int i = 0; i < transformationNodes.size(); i++) {
			int[] dataColumn = new int[this.size()];
			for (int j = 0; j < this.size(); j++) {
				dataColumn[j] = this.get(j).recordContent[i];
			}
			transformationNodes.set(i, iface.getHierarchyTree(i).getLowestGeneralization(dataColumn));
		}
	}

	public TassaCluster(int initialSize, ARXInterface iface) {
		super(initialSize);
		this.iface = iface;
		this.manager = iface.getDataManager();
	}
	
	public TassaCluster(int[][] inputArray, ARXInterface iface) {
		this(inputArray.length, iface);
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
		clusterChanged = true;
		return super.add(record);
	}
	
	public TassaRecord remove(int index) {
		clusterChanged = true;
		return super.remove(index);
	}
	
	
	public double getGC_LM() {
		
		double gc = 0;
		
		for (int i = 0; i < iface.getNumAttributes(); i++) {
			
			int recordCardinality = transformationNodes.get(i).size();
			int attributeCardinality = manager.getHierarchies()[i].getDistinctValues()[0];
			
			gc += (recordCardinality - 1) / (attributeCardinality - 1);
		}
		
		return gc / iface.getNumAttributes();
	}
	
	public double getAddedGC(TassaRecord addedRecord) {
		// TODO: Mockup
		return -1.2;
	}
	
	public double getRemovedGC(TassaRecord removedRecord) {
		// TODO: Mockup
		return 1.1;
	}
	
	public double getMergedGC(TassaCluster mergeCandidate) {
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
			TassaCluster c = new TassaCluster(k + addRecord, this.iface);
			
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
		return new TassaCluster(0, this.iface);
	}
	
	public static Comparator<TassaCluster> SizeComparator = new Comparator<TassaCluster>() {
		
		public int compare(TassaCluster l1, TassaCluster l2) {
			return l1.size() - l2.size();
		}
	};
	
	
}