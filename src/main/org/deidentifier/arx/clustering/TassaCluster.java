package org.deidentifier.arx.clustering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.deidentifier.arx.ARXInterface;
import org.deidentifier.arx.framework.data.DataManager;


class TassaCluster extends ArrayList<TassaRecord> {

	private static final long serialVersionUID = 1L;
	
	
	private ARXInterface iface;
	private DataManager manager;
	
	// List of transformation nodes for every attribute
	// they contain level of transformation, mapping key, all contained values
	private GeneralizationNode[] transformationNodes;
	private HashMap<TassaRecord, Double> removedNodeGC;
	private GeneralizationTree[] hierarchyTrees;
	
	// caching of calculated values
	private double generalizationCost;
	
	/** The modification counter at the time of the last update.
	 * 	We use this to detect, whether the cluster was changed and
	 *  if we have to update generalizationCost and transformationNodes again.*/
	private int lastModCount;
	
	private TassaCluster(ARXInterface iface) {
		this.iface = iface;
		manager = iface.getDataManager();
		transformationNodes = new GeneralizationNode[iface.getNumAttributes()];
		hierarchyTrees = new GeneralizationTree[transformationNodes.length];
		for (int i = 0; i < transformationNodes.length; i++) {
			hierarchyTrees[i] = iface.getHierarchyTree(i);
		}
		removedNodeGC = new HashMap<TassaRecord, Double>();
	}
	
	public TassaCluster(Collection<TassaRecord> recordCollection, ARXInterface iface) {
		this(iface);
		addAll(recordCollection);
		updateGeneralization();
	}

	public TassaCluster(TassaCluster cluster) {
		this(cluster.iface);
		addAll(cluster);
		for (int i = 0; i < transformationNodes.length; i++) {
			transformationNodes[i] = cluster.transformationNodes[i];
		}
		lastModCount = modCount;
	}

	// TODO: Better solution for assigning records?
	public void assignAllRecords() {
		for (TassaRecord record : this) {
			record.assignedCluster = this;
		}
	}
	
	public boolean add(TassaRecord record) {
		boolean success = super.add(record);
		updateGeneralization(record);
		return success;
	}
	
	public TassaRecord remove(int index) {
		removedNodeGC.clear();
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


	private void updateGeneralization(TassaRecord addedRecord) {
		this.updateTransformation(addedRecord);
		generalizationCost = this.getGC_LM();
		lastModCount = modCount;
	}
	

	/**
	 * Update transformation of this Cluster.
	 */
	private void updateTransformation() {
		for (int i = 0; i < transformationNodes.length; i++) {
			int[] dataColumn = new int[this.size()];
			for (int j = 0; j < this.size(); j++) {
				dataColumn[j] = this.get(j).recordContent[i];
			}
			transformationNodes[i] = hierarchyTrees[i].getLowestCommonAncestor(dataColumn);
		}
	}
	

	/**
	 * Update transformation of this Cluster.
	 */
	private void updateTransformation(TassaRecord addedRecord) {
		int[] recordContent = addedRecord.recordContent;
		for (int i = 0; i < recordContent.length; i++) {
			transformationNodes[i] = hierarchyTrees[i].getLowestCommonAncestor(transformationNodes[i], recordContent[i]);
		}
	}
	
	
	public double getGC_LM() {
		
		double gc = 0;
		int numAtt = transformationNodes.length;
		for (int i = 0; i < numAtt; i++) {
			
			// TODO: Check, whether we have the correct cardinalities here!
			int recordCardinality = transformationNodes[i].values.size();
			int attributeCardinality = hierarchyTrees[i].root.values.size();
			
			gc += (recordCardinality - 1) / (attributeCardinality - 1);
		}
		
		return gc / numAtt;
	}
	
	public double getAddedGC(TassaRecord addedRecord) {
		TassaCluster tempCluster = new TassaCluster(this);
		tempCluster.add(addedRecord);
		return tempCluster.getGC();
	}
	
	public double getRemovedGC(TassaRecord removedRecord) {
		Double result = 0.0;
		if (this.size() > 1) {
			result = removedNodeGC.get(removedRecord);
			if (result == null) {
				TassaCluster tempCluster = new TassaCluster(this);
				tempCluster.remove(removedRecord);
				result = tempCluster.getGC();
				removedNodeGC.put(removedRecord, result);
			}
		}
		return result;
	}
}