package org.deidentifier.arx.clustering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.deidentifier.arx.framework.data.GeneralizationHierarchy;

public class GeneralizationNode {
	
	private static final long serialVersionUID = 1L;
	
	/** The mapping key. The distinct value in the generalization array
	 * to which all children of this node are mapped to. */
	HashSet<Integer> values;
	int mappingKey;
	int level;
	GeneralizationTree tree;
	GeneralizationNode parent;
	ArrayList<GeneralizationNode> children;
	
	public GeneralizationNode(int level, int mappingKey, GeneralizationNode parent) {
		values = new HashSet<Integer>();
		this.level = level;
		this.mappingKey = mappingKey;
		this.parent = parent;
		if (parent != null) {
			this.tree = parent.tree;	
		}
		this.children = new ArrayList<GeneralizationNode>();
	}
	
	public GeneralizationNode(int level, int mappingKey, int[] values, GeneralizationTree tree) {
		this(level, mappingKey, null);
		this.tree = tree;
		this.addAll(values);
	}
	
	public boolean addAll(int[] values) {
		boolean result = true;
		for (int i : values) {
			result &= this.values.add(i);
		}
		return result;
	}
	
	// build the tree starting from this node
	/**
	 * Builds the tree for the current node.
	 *
	 * @param hierarchy the generalization hierarchy
	 */
	public void buildTree(GeneralizationHierarchy hierarchy) {
		
		if (this.level > 0) {
			
			for (int value : this.values) {
				int mappingKeyLowerLevel = hierarchy.getArray()[value][this.level-1];
				
				//GeneralizationNode child2 = this.getChild(mappingKeyLowerLevel);
				
				GeneralizationNode child = this.getChild(mappingKeyLowerLevel);
				
				// add child node for every distinct key in next lower level
				if (child == null) {
					child = new GeneralizationNode(this.level-1, mappingKeyLowerLevel, this);
					tree.put(mappingKeyLowerLevel, child);
					children.add(child);
				}
				
				// add values, that are assigned to the mapping key of the child node
				child.values.add(value);
			}
			
			// expand and build children
			for (GeneralizationNode child : children) {
				child.buildTree(hierarchy);
			}
		}
	}
	
	
	/**
	 * Gets the child with the given mapping key.
	 *
	 * @param mappingKey the mapping key of the requested child
	 * @return The child node with the given mapping key, if it exists. Otherwise null.
	 */
	public GeneralizationNode getChild(int mappingKey) {
		GeneralizationNode result = null;
		
		for (GeneralizationNode node : children) {
			if (node.mappingKey == mappingKey) {
				result = node;
			}
		}
		
		return result;
	}
	
}