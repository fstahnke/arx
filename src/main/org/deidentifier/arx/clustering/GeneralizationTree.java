package org.deidentifier.arx.clustering;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import org.deidentifier.arx.framework.data.GeneralizationHierarchy;

public class GeneralizationTree extends TreeMap<Integer, GeneralizationNode> {
	
	private static final long serialVersionUID = 1L;
	
	GeneralizationHierarchy hierarchy;
	GeneralizationNode root;
	
	public GeneralizationTree(GeneralizationHierarchy hierarchy) {
		this.hierarchy = hierarchy;
		int maxLevel = hierarchy.getHeight() - 1;
		this.root = new GeneralizationNode(maxLevel, hierarchy.getArray()[0][maxLevel], null);
		root.buildTree(hierarchy);
		
	}

	/**
	 * Gets the lowest generalization for a given set of integers.
	 *
	 * @param dataColumn a set of integers
	 * @return the lowest level of the hierarchy, where all given integers are part of the subtree
	 */
	public GeneralizationNode getLowestGeneralization(int[] dataColumn) {
		
		ArrayList<GeneralizationNode> path = new ArrayList<GeneralizationNode>(hierarchy.getHeight());
		ArrayList<Integer> checkedValues = new ArrayList<Integer>();
		path.add(root);
		
		// Starting with highest level (root)
		int level = hierarchy.getHeight() - 1;
		// Go through tree until level = 0
		while (level > 0) {
			// Build path from root to leaf for first element in input array
			for (GeneralizationNode node : path.get(path.size()-1).children) {
				if (node.contains(dataColumn[0])) {
					path.add(node);
					level--;
					break;
				}
			}
		}
		
		// add first element to checked list so we don't do the work twice
		checkedValues.add(dataColumn[0]);
		
		for (int i : dataColumn) {
			// TODO: This might actually take more time for large datasets with lots of different values
			// 			than just checking all the nodes' arrays.
			if (!checkedValues.contains(i)) {
				// Go through all nodes in the path and delete all nodes, that don't contain i
				// TODO: It would probably be more efficient to do it bottom up
				// and break when the first node containing i is found.
				for (Iterator<GeneralizationNode> iterator = path.iterator(); iterator.hasNext();) {
					GeneralizationNode node = iterator.next();
					if (!node.contains(i)) {
						iterator.remove();
					}
				}
			}
		}
		
		return path.get(path.size()-1);
	}
}