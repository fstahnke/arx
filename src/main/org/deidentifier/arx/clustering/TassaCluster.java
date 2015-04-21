package org.deidentifier.arx.clustering;

import java.util.ArrayList;
import java.util.Collection;
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
	
	private TassaCluster(ARXInterface iface) {
		this.iface = iface;
		manager = iface.getDataManager();
		transformationNodes = new ArrayList<GeneralizationNode>(iface.getNumAttributes());
	}
	
	public TassaCluster(Collection<TassaRecord> recordCollection, ARXInterface iface) {
		this(iface);
		addAll(recordCollection);
		updateGeneralization();
	}

	public TassaCluster(TassaCluster cluster) {
		this(cluster.iface);
		addAll(cluster);
		transformationNodes.addAll(cluster.transformationNodes);
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
		transformationNodes.clear();
		for (int i = 0; i < iface.getNumAttributes(); i++) {
			int[] dataColumn = new int[this.size()];
			for (int j = 0; j < this.size(); j++) {
				dataColumn[j] = this.get(j).recordContent[i];
			}
			transformationNodes.add(i, iface.getHierarchyTree(i).getLowestCommonAncestor(dataColumn));
		}
	}
	

	/**
	 * Update transformation of this Cluster.
	 */
	private void updateTransformation(TassaRecord addedRecord) {
		int[] recordContent = addedRecord.recordContent;
		for (int i = 0; i < iface.getNumAttributes(); i++) {
			transformationNodes.set(i, iface.getHierarchyTree(i).getLowestCommonAncestor(transformationNodes.get(i), recordContent[i]));
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