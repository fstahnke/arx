package org.deidentifier.arx.clustering;

import java.util.ArrayList;
import java.util.List;

public class TassaDatabase extends ArrayList<TassaCluster> {
	
	/**
	 * 
	 */
	
	public TassaDatabase(List<TassaCluster> listOfClusters) {
		super(listOfClusters);
	}
	
	public TassaDatabase() {
		super();
	}

	private static final long serialVersionUID = 1899366651589072401L;

	public TassaCluster mergeClosestPair() {
		// TODO: Mockup
		return new TassaCluster(2, null);
	}
	
	public TassaCluster mergeClosestPair(TassaDatabase targetDatabase) {
		// TODO: Mockup. Maybe move to Cluster.
		return new TassaCluster(2, null);
	}

}
