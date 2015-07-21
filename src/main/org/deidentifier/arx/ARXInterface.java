package org.deidentifier.arx;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.deidentifier.arx.clustering.GeneralizationManager;
import org.deidentifier.arx.criteria.KAnonymity;
import org.deidentifier.arx.criteria.LDiversity;
import org.deidentifier.arx.criteria.TCloseness;
import org.deidentifier.arx.framework.check.distribution.DistributionAggregateFunction;
import org.deidentifier.arx.framework.data.DataManager;
import org.deidentifier.arx.framework.data.Dictionary;
import org.deidentifier.arx.framework.data.GeneralizationHierarchy;

// TODO: Auto-generated Javadoc
/**
 * This class provides a rudimentary interface to the internal ARX data structures.
 *
 * @author Fabian Prasser
 */
public class ARXInterface {

    /**  The data manager. */
    private final DataManager      manager;
    
    /**
     * Gets the data manager for the current data set.
     *
     * @return the data manager
     */
    public DataManager getDataManager()
    {
    	return this.manager;
    }
    
    /**  The buffer. */
    private final int[][]          buffer;
    
    /**  The config. */
    private final ARXConfiguration config;
    /** The generalization manager. */
    private final GeneralizationManager generalizationManager;
    
    public GeneralizationManager getGeneralizationManager() {
		return generalizationManager;
	}

	/** Turn logging on or off. */
    public final boolean logging = true;
    /** The number of records that is processed between each logging tick. */
    public final int logNumberOfRecords = 1000;
    /** The number of clusters that is processed between each logging tick. */
    public final int logNumberOfClusters = 100;

    /**
     * Creates a new interface to the internal ARX data structures.
     *
     * @param data the data
     * @param config the config
     * @throws IOException Signals that an I/O exception has occurred.
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
        handle.getDefinition().materializeHierarchies(handle);
        checkBeforeEncoding(handle, config);
        handle.getRegistry().reset();
        handle.getRegistry().createInputSubset(config);

        String[] header = ((DataHandleInput) handle).header;
        int[][] dataArray = ((DataHandleInput) handle).data;
        Dictionary dictionary = ((DataHandleInput) handle).dictionary;
        manager = new DataManager(header, dataArray, dictionary, handle.getDefinition(), config.getCriteria(), new HashMap<String, DistributionAggregateFunction>());

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
        
        // Create generalization manager
        generalizationManager = new GeneralizationManager(manager);
    }

    /**
     * Returns the input data array (quasi-identifiers).
     *
     * @return the data qi
     */
    public int[][] getDataQI() {
        return manager.getDataGeneralized().getArray();
    }

    /**
     * Returns the output buffer.
     *
     * @return the buffer
     */
    public int[][] getBuffer() {
        return buffer;
    }

    /**
     * Returns the hierarchy for the attribute at the given index.
     *
     * @param index the index
     * @return the hierarchy
     */
    public int[][] getHierarchy(int index) {
        return manager.getHierarchies()[index].getArray();
    }

    /**
     * Returns the name of the attribute at the given index.
     *
     * @param index the index
     * @return the attribute
     */
    public String getAttribute(int index) {
        return manager.getDataGeneralized().getHeader()[index];
    }

    /**
     * Returns the number of quasi-identifying attributes.
     *
     * @return the num attributes
     */
    public int getNumAttributes() {
        return buffer[0].length;
    }

    /**
     * Returns the parameter 'k', as in k-anonymity.
     *
     * @return the k
     */
    public int getK() {
        return config.getMinimalGroupSize();
    }

    /**
     * Performs some sanity checks.
     *
     * @param config the config
     * @param manager the manager
     */
    private void checkAfterEncoding(final ARXConfiguration config, final DataManager manager) {

        if (config.containsCriterion(KAnonymity.class)) {
            KAnonymity c = config.getCriterion(KAnonymity.class);
            if ((c.getK() > manager.getDataGeneralized().getDataLength()) || (c.getK() < 1)) {
                throw new IllegalArgumentException("Parameter k (" + c.getK() + ") musst be positive and less or equal than the number of rows (" + manager.getDataGeneralized().getDataLength() + ")");
            }
        }
        if (config.containsCriterion(LDiversity.class)) {
            for (LDiversity c : config.getCriteria(LDiversity.class)) {
                if ((c.getL() > manager.getDataGeneralized().getDataLength()) || (c.getL() < 1)) {
                    throw new IllegalArgumentException("Parameter l (" + c.getL() + ") musst be positive and less or equal than the number of rows (" + manager.getDataGeneralized().getDataLength() + ")");
                }
            }
        }

        // Check whether all hierarchies are monotonic
        for (final GeneralizationHierarchy hierarchy : manager.getHierarchies()) {
            hierarchy.checkMonotonicity(manager);
        }

        // check min and max sizes
        final int[] hierarchyHeights = manager.getHierachiesHeights();
        final int[] minLevels = manager.getHierarchiesMinLevels();
        final int[] maxLevels = manager.getHierarchiesMaxLevels();

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
