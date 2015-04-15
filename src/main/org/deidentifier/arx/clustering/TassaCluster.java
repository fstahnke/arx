package org.deidentifier.arx.clustering;

import java.util.ArrayList;
import java.util.Collections;
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
	private double generalizationCost;
	
	/** The modification counter at the time of the last update.
	 * 	We use this to detect, whether the cluster was changed and
	 *  if we have to update generalizationCost and transformationNodes again.*/
	private int lastModCount;
	public TassaCluster(ARXInterface iface) {
		super();
		this.iface = iface;
		this.manager = iface.getDataManager();
		this.transformationNodes = new ArrayList<GeneralizationNode>();
	}

	public TassaCluster(TassaCluster cluster) {
		super(cluster.size());
		this.iface = cluster.iface;
		this.manager = iface.getDataManager();
		this.transformationNodes = new ArrayList<GeneralizationNode>(cluster.transformationNodes);
		this.lastModCount = this.modCount;
		for (TassaRecord record : cluster) {
			this.add(record);
		}
	}
	
	public TassaCluster(int[][] inputArray, ARXInterface iface) {
		super(inputArray.length);
		this.iface = iface;
		this.manager = iface.getDataManager();
		this.transformationNodes = new ArrayList<GeneralizationNode>(inputArray[0].length);
		for (int i = 0; i < inputArray.length; i++) {
			this.add(new TassaRecord(inputArray[i]));
		}
	}
	
	// TODO: Better solution for assigning records?
	public void assignAllRecords() {
		for (TassaRecord record : this) {
			record.assignedCluster = this;
		}
	}
	
	public boolean add(TassaRecord record) {
		return super.add(record);
	}
	
	public boolean add(int[] arrayRecord) {
		return this.add(new TassaRecord(arrayRecord));
	}
	
	public TassaRecord remove(int index) {
		return super.remove(index);
	}
	
	
	public double getGC() {
		if (lastModCount < modCount) {
			updateGeneralization();
		}
		return generalizationCost;
	}


	private void updateGeneralization() {
		
		this.updateTransformation();
		generalizationCost = this.getGC_LM();
		lastModCount = modCount;
	}
	

	/**
	 * Update transformation of this Cluster.
	 */
	private void updateTransformation() {
		transformationNodes.clear();
		for (int i = 0; i < iface.getNumAttributes(); i++) {
			int[] dataColumn = new int[this.size()];
			for (int j = 0; j < this.size(); j++) {
				dataColumn[j] = this.get(j).recordContent[i];
			}
			transformationNodes.add(i, iface.getHierarchyTree(i).getLowestGeneralization(dataColumn));
		}
	}
	
	
	public double getGC_LM() {
		
		double gc = 0;
		
		for (int i = 0; i < iface.getNumAttributes(); i++) {
			
			// TODO: Check, whether we have the correct cardinalities here!
			int recordCardinality = transformationNodes.get(i).size();
			int attributeCardinality = manager.getHierarchies()[i].getDistinctValues()[0];
			
			gc += (recordCardinality - 1) / (attributeCardinality - 1);
		}
		
		return gc / iface.getNumAttributes();
	}
	
	public double getAddedGC(TassaRecord addedRecord) {
		TassaCluster tempCluster = new TassaCluster(this);
		tempCluster.add(addedRecord);
		return tempCluster.getGC();
	}
	
	public double getRemovedGC(TassaRecord removedRecord) {
		if (this.size() > 1) {
			TassaCluster tempCluster = new TassaCluster(this);
			tempCluster.remove(removedRecord);
			return tempCluster.getGC();
		}
		else {
			return 0;
		}
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
			TassaCluster c = new TassaCluster(this.iface);
			c.ensureCapacity(k + addRecord);
			
			// iterate through each element
			for (int j = 0; j < k + addRecord; j++) {
				c.add(iter.next());
			}
			
			// add cluster to clusterList
			clusterList.add(c);
		}
		
		return clusterList;
		
	}
	
}