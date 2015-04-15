package org.deidentifier.arx.clustering;

import java.util.ArrayList;
import java.util.List;

public class TassaClusterSet extends ArrayList<TassaCluster> {
	
	/**
	 * 
	 */
	
	public TassaClusterSet(List<TassaCluster> listOfClusters) {
		super(listOfClusters);
	}
	
	public TassaClusterSet() {
		super();
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
