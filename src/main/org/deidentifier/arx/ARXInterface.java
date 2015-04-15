package org.deidentifier.arx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.deidentifier.arx.clustering.GeneralizationTree;
import org.deidentifier.arx.criteria.KAnonymity;
import org.deidentifier.arx.criteria.LDiversity;
import org.deidentifier.arx.criteria.TCloseness;
import org.deidentifier.arx.framework.check.groupify.HashGroupifyEntry;
import org.deidentifier.arx.framework.data.DataManager;
import org.deidentifier.arx.framework.data.Dictionary;
import org.deidentifier.arx.framework.data.GeneralizationHierarchy;
import org.deidentifier.arx.framework.lattice.Node;
import org.deidentifier.arx.metric.InformationLoss;

/**
 * This class provides a rudimentary interface to the internal ARX data structures
 * @author Fabian Prasser
 *
 */
public class ARXInterface {

    /** The data manager */
    private final DataManager      manager;
    /** The buffer */
    private final int[][]          buffer;
    /** The config */
    private final ARXConfiguration config;
    /** The hierarchy trees. */
    private ArrayList<GeneralizationTree> hierarchyTrees;

    /**
     * Creates a new interface to the internal ARX data structures
     * @param data
     * @param config
     * @throws IOException
     */
    public ARXInterface(final Data data, ARXConfiguration config) throws IOException {

        // Check simplifying assumptions
        if (config.getMaxOutliers() > 0d) {
            throw new UnsupportedOperationException("Outliers are not supported");
        }

        if (config.getCriteria().size() != 1) {
            throw new UnsupportedOperationException("Only exactly one criterion is supported");
        }

        if (!(config.getCriteria().iterator().next() instanceof KAnonymity)) {
            throw new UnsupportedOperationException("Only the k-anonymity criterion is supported");
        }

        if (((DataHandleInput) data.getHandle()).isLocked()) {
            throw new RuntimeException("This data handle is locked. Please release it first");
        }

        if (data.getDefinition().getSensitiveAttributes().size() > 1 && config.isProtectSensitiveAssociations()) {
            throw new UnsupportedOperationException("Currently not supported!");
        }

        // Encode data
        DataHandle handle = data.getHandle();
        handle.getDefinition().materialize(handle);
        checkBeforeEncoding(handle, config);
        handle.getRegistry().reset();
        handle.getRegistry().createInputSubset(config);

        String[] header = ((DataHandleInput) handle).header;
        int[][] dataArray = ((DataHandleInput) handle).data;
        Dictionary dictionary = ((DataHandleInput) handle).dictionary;
        manager = new DataManager(header, dataArray, dictionary, handle.getDefinition(), config.getCriteria());

        // Initialize
        this.config = config;
        config.initialize(manager);

        // Check
        checkAfterEncoding(config, manager);

        // Build buffer
        int[][] array = getDataQI();
        buffer = new int[array.length][];
        for (int i = 0; i < array.length; i++) {
            buffer[i] = new int[array[0].length];
        }
        
        // Generate generalization hierarchy trees
        hierarchyTrees = new ArrayList<GeneralizationTree>(manager.getHierarchies().length);
        for (GeneralizationHierarchy hierarchy : manager.getHierarchies())
        {
        	this.hierarchyTrees.add(new GeneralizationTree(hierarchy));
        }
    }
    
    /**
     * Gets the hierarchy tree.
     *
     * @param index the index of the attribute
     * @return the hierarchy tree
     */
    public GeneralizationTree getHierarchyTree(int index) {
    	return this.hierarchyTrees.get(index);
    }
    
    /**
     * Gets the data manager for the current data set.
     *
     * @return the data manager
     */
    public DataManager getDataManager()
    {
    	return this.manager;
    }

    /**
     * Returns the input data array (quasi-identifiers)
     * @return
     */
    public int[][] getDataQI() {
        return manager.getDataQI().getArray();
    }
    
    /**
     * Returns the information loss of a cluster
     * @param data
     * @param count
     * @param transformation
     * @return
     */
    public InformationLoss<?> getInformationLoss(int[] data, int count, int[] transformation) {
    	HashGroupifyEntry entry = new HashGroupifyEntry(data, count);
    	Node node = new Node(0);
    	node.setTransformation(transformation, getLevel(transformation));
    	return config.getMetric().getInformationLoss(node, entry).getInformationLoss();
    }
    
    /**
     * TODO: Get rid of this (easy and efficient with streams in Java 8)
     * @param transformation
     * @return
     */
    private int getLevel(int[] transformation) {
		int level = 0;
		for (int t : transformation) {
			level += t;
		}
		
		return level;
		
		/* Java 8:
		return IntStream.of(transformation).sum();
		*/
	}

	/**
     * Returns the input data array (sensitive attributes)
     * @return
     */
    public int[][] getDataSE() {
        return manager.getDataSE().getArray();
    }

    /**
     * Returns the output buffer
     * @return
     */
    public int[][] getBuffer() {
        return buffer;
    }

    /**
     * Returns the hierarchy for the attribute at the given index
     * @param index
     * @return
     */
    public int[][] getHierarchy(int index) {
        return manager.getHierarchies()[index].getArray();
    }

    /**
     * Returns the name of the attribute at the given index
     * @param index
     * @return
     */
    public String getAttribute(int index) {
        return manager.getDataQI().getHeader()[index];
    }

    /**
     * Returns the number of quasi-identifying attributes
     * @return
     */
    public int getNumAttributes() {
        return buffer[0].length;
    }

    /**
     * Returns the parameter 'k', as in k-anonymity
     * @return
     */
    public int getK() {
        return config.getMinimalGroupSize();
    }

    /**
     * Performs some sanity checks.
     *
     * @param config
     * @param manager the manager
     */
    private void checkAfterEncoding(final ARXConfiguration config, final DataManager manager) {

        if (config.containsCriterion(KAnonymity.class)) {
            KAnonymity c = config.getCriterion(KAnonymity.class);
            if ((c.getK() > manager.getDataQI().getDataLength()) || (c.getK() < 1)) {
                throw new IllegalArgumentException("Parameter k (" + c.getK() + ") musst be positive and less or equal than the number of rows (" + manager.getDataQI().getDataLength() + ")");
            }
        }
        if (config.containsCriterion(LDiversity.class)) {
            for (LDiversity c : config.getCriteria(LDiversity.class)) {
                if ((c.getL() > manager.getDataQI().getDataLength()) || (c.getL() < 1)) {
                    throw new IllegalArgumentException("Parameter l (" + c.getL() + ") musst be positive and less or equal than the number of rows (" + manager.getDataQI().getDataLength() + ")");
                }
            }
        }

        // Check whether all hierarchies are monotonic
        for (final GeneralizationHierarchy hierarchy : manager.getHierarchies()) {
            hierarchy.checkMonotonicity(manager);
        }

        // check min and max sizes
        final int[] hierarchyHeights = manager.getHierachyHeights();
        final int[] minLevels = manager.getMinLevels();
        final int[] maxLevels = manager.getMaxLevels();

        for (int i = 0; i < hierarchyHeights.length; i++) {
            if (minLevels[i] > (hierarchyHeights[i] - 1)) {
                throw new IllegalArgumentException("Invalid minimum generalization for attribute '" + manager.getHierarchies()[i].getName() + "': " +
                                                   minLevels[i] + " > " + (hierarchyHeights[i] - 1));
            }
            if (minLevels[i] < 0) {
                throw new IllegalArgumentException("The minimum generalization for attribute '" + manager.getHierarchies()[i].getName() + "' has to be positive!");
            }
            if (maxLevels[i] > (hierarchyHeights[i] - 1)) {
                throw new IllegalArgumentException("Invalid maximum generalization for attribute '" + manager.getHierarchies()[i].getName() + "': " +
                                                   maxLevels[i] + " > " + (hierarchyHeights[i] - 1));
            }
            if (maxLevels[i] < minLevels[i]) {
                throw new IllegalArgumentException("The minimum generalization for attribute '" + manager.getHierarchies()[i].getName() +
                                                   "' has to be lower than or requal to the defined maximum!");
            }
        }
    }

    /**
     * Performs some sanity checks.
     * 
     * @param handle
     *            the data handle
     * @param config
     *            the configuration
     */
    private void checkBeforeEncoding(final DataHandle handle, final ARXConfiguration config) {

        // Lots of checks
        if (handle == null) {
            throw new NullPointerException("Data must not be null!");
        }
        if (config.containsCriterion(LDiversity.class) ||
            config.containsCriterion(TCloseness.class)) {
            if (handle.getDefinition().getSensitiveAttributes().size() == 0) {
                throw new IllegalArgumentException("You need to specify a sensitive attribute!");
            }
        }
        for (String attr : handle.getDefinition().getSensitiveAttributes()) {
            boolean found = false;
            for (LDiversity c : config.getCriteria(LDiversity.class)) {
                if (c.getAttribute().equals(attr)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                for (TCloseness c : config.getCriteria(TCloseness.class)) {
                    if (c.getAttribute().equals(attr)) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                throw new IllegalArgumentException("No criterion defined for sensitive attribute: '" + attr + "'!");
            }
        }
        for (LDiversity c : config.getCriteria(LDiversity.class)) {
            if (handle.getDefinition().getAttributeType(c.getAttribute()) != AttributeType.SENSITIVE_ATTRIBUTE) {
                throw new RuntimeException("L-Diversity criterion defined for non-sensitive attribute '" + c.getAttribute() + "'!");
            }
        }
        for (TCloseness c : config.getCriteria(TCloseness.class)) {
            if (handle.getDefinition().getAttributeType(c.getAttribute()) != AttributeType.SENSITIVE_ATTRIBUTE) {
                throw new RuntimeException("T-Closeness criterion defined for non-sensitive attribute '" + c.getAttribute() + "'!");
            }
        }

        // Check handle
        if (!(handle instanceof DataHandleInput)) {
            throw new IllegalArgumentException("Invalid data handle provided!");
        }

        // Check if all defines are correct
        DataDefinition definition = handle.getDefinition();
        Set<String> attributes = new HashSet<String>();
        for (int i = 0; i < handle.getNumColumns(); i++) {
            attributes.add(handle.getAttributeName(i));
        }
        for (String attribute : handle.getDefinition().getSensitiveAttributes()) {
            if (!attributes.contains(attribute)) {
                throw new IllegalArgumentException("Sensitive attribute '" + attribute + "' is not contained in the dataset");
            }
        }
        for (String attribute : handle.getDefinition().getInsensitiveAttributes()) {
            if (!attributes.contains(attribute)) {
                throw new IllegalArgumentException("Insensitive attribute '" + attribute + "' is not contained in the dataset");
            }
        }
        for (String attribute : handle.getDefinition().getIdentifyingAttributes()) {
            if (!attributes.contains(attribute)) {
                throw new IllegalArgumentException("Identifying attribute '" + attribute + "' is not contained in the dataset");
            }
        }
        for (String attribute : handle.getDefinition().getQuasiIdentifyingAttributes()) {
            if (!attributes.contains(attribute)) {
                throw new IllegalArgumentException("Quasi-identifying attribute '" + attribute + "' is not contained in the dataset");
            }
        }

        // Perform sanity checks
        Set<String> qis = definition.getQuasiIdentifyingAttributes();
        if ((config.getMaxOutliers() < 0d) || (config.getMaxOutliers() > 1d)) {
            throw new IllegalArgumentException("Suppression rate " + config.getMaxOutliers() + "must be in [0, 1]");
        }
        if (qis.size() == 0) {
            throw new IllegalArgumentException("You need to specify at least one quasi-identifier");
        }
    }
}