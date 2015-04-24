package org.deidentifier.arx.clustering;

import java.util.HashMap;
import java.util.HashSet;

import org.deidentifier.arx.framework.data.GeneralizationHierarchy;

// TODO: Try using IdentityHashMap, but be aware of the cache limit of Integer from -128 to +127
public class GeneralizationTree extends HashMap<Integer, GeneralizationNode> {
	
	private static final long serialVersionUID = 1L;
	
	GeneralizationHierarchy hierarchy;
	GeneralizationNode root;
	int[][] hierarchyArray;
	
	public GeneralizationTree(GeneralizationHierarchy hierarchy) {
		this.hierarchy = hierarchy;
		int maxLevel = hierarchy.getHeight() - 1;
		this.root = new GeneralizationNode(maxLevel, hierarchy.getArray()[0][maxLevel], hierarchy.getDistinctValues(0), this);
		root.buildTree(hierarchy);
		hierarchyArray = hierarchy.getArray();
	}

	public GeneralizationNode getLowestCommonAncestor(GeneralizationNode node1, GeneralizationNode node2) {
		
		return null;
	}
	
	public GeneralizationNode getLowestCommonAncestor(GeneralizationNode node, int i) {
		
		for (GeneralizationNode commonNode = node; commonNode != root; commonNode = commonNode.parent) {
			if (commonNode.values.contains(i)) {
				return commonNode;
			}
		}
		return root;
	}
	
	public int getGeneralizationLevel(int[] values, int lvl) {

		int val = hierarchyArray[values[0]][lvl];
		int maxLvl = hierarchyArray[0].length - 1;
		
		for (int i = 1; i < values.length && lvl != maxLvl; i++) {
			while (hierarchyArray[values[i]][lvl] != val) {
				val = hierarchyArray[values[i-1]][++lvl];
			}
		}
		return lvl;
	}
	
	public int getTransformation(int value, int lvl) {
		return hierarchyArray[value][lvl];
	}
	
	public int getCardinality(int lvl) {
		
		int[] distinctValues = new int[hierarchyArray.length];
		int distinctNumber = 0;
		
		for (int[] record : hierarchyArray) {
		    // check, if value at position i already exists
		    for (int j = 0; j < distinctNumber; j++) {
			if (record[lvl] == distinctValues[j]) {
			    distinctValues[distinctNumber] = record[lvl];
			    distinctNumber++;
			    break;
			}
		    }
		}
		return distinctNumber;
	}
	
	
	public int getCardinalityHashSet(int lvl) {
		
		HashSet<Integer> distinctValues = new HashSet<>(hierarchyArray.length);
		
		for (int[] record : hierarchyArray) {
		    distinctValues.add(record[lvl]);
		}
		
		return distinctValues.size();
	}
	


	/**
	 * Gets the lowest generalization for a given set of integers.
	 *
	 * @param dataColumn a set of integers
	 * @return the lowest level of the hierarchy, where all given integers are part of the subtree
	 */
	public GeneralizationNode getLowestCommonAncestor(int[] dataColumn) {
		
		GeneralizationNode commonNode = this.get((Integer)dataColumn[0]);
		
		for (int i : dataColumn) {
			while (!commonNode.values.contains(i)) {
				commonNode = commonNode.parent;
			}
		}
		
		return commonNode;
	}
}