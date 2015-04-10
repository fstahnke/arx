package org.deidentifier.arx.clustering;

import java.util.ArrayList;

import org.deidentifier.arx.framework.data.GeneralizationHierarchy;

public class GeneralizationNode extends ArrayList<Integer> {
	
	private static final long serialVersionUID = 1L;
	
	/** The mapping key. The distinct value in the generalization array
	 * to which all children of this node are mapped to. */
	int mappingKey;
	int level;
	GeneralizationNode parent;
	ArrayList<GeneralizationNode> children;
	
	public GeneralizationNode(int level, int mappingKey, GeneralizationNode parent) {
		this.level = level;
		this.mappingKey = mappingKey;
		this.parent = parent;
		this.children = new ArrayList<GeneralizationNode>();
	}
	
	public GeneralizationNode(int level, int mappingKey, GeneralizationNode parent, int[] values) {
		this(level, mappingKey, parent);
		this.addAll(values);
	}
	
	public boolean addAll(int[] values) {
		boolean result = true;
		for (int i : values) {
			result &= this.add(i);
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
			
			// if level is highest level in hierarchy, this is the root node. add all values.
			if (this.level == hierarchy.getHeight() - 1 && this.isEmpty())
			{
				this.addAll(hierarchy.getDistinctValues(0));
			}
			
			for (int value : this) {
				int mappingKeyLowerLevel = hierarchy.getArray()[value][this.level-1];
				
				GeneralizationNode child = this.getChild(mappingKeyLowerLevel);
				
				// add child node for every distinct key in next lower level
				// TODO: TreeMaps better?
				if (child == null) {
					child = new GeneralizationNode(this.level-1, mappingKeyLowerLevel, this);
					this.children.add(child);
				}
				
				// add values, that are assigned to the mapping key of the child node
				child.add(value);
			}
			
			// expand and build children
			for (GeneralizationNode child : this.children) {
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