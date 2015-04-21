package org.deidentifier.arx.clustering;

import java.util.ArrayList;
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
		for (int[] record :  inputArray) {
			this.add(new TassaRecord(record));
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
			transformationNodes.add(i, iface.getHierarchyTree(i).getLowestCommonAncestor(dataColumn));
		}
	}
	
	
	public double getGC_LM() {
		
		double gc = 0;
		
		for (int i = 0; i < iface.getNumAttributes(); i++) {
			
			// TODO: Check, whether we have the correct cardinalities here!
			int recordCardinality = transformationNodes.get(i).values.size();
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
}