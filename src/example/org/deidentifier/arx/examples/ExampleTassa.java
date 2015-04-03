package org.deidentifier.arx.examples;

import java.io.IOException;

import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.ARXInterface;
import org.deidentifier.arx.AttributeType.Hierarchy;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.criteria.KAnonymity;

public class ExampleTassa {

    /**
     * Entry point.
     * 
     * @param args
     *            the arguments
     * @throws IOException
     */
    public static void main(final String[] args) throws IOException {

        // Load data
        Data data = Data.create("data/test.csv", ';');
        data.getDefinition().setAttributeType("age", Hierarchy.create("data/test_hierarchy_age.csv", ';'));
        data.getDefinition().setAttributeType("gender", Hierarchy.create("data/test_hierarchy_gender.csv", ';'));
        data.getDefinition().setAttributeType("zipcode", Hierarchy.create("data/test_hierarchy_zipcode.csv", ';'));

        // Configuration
        ARXConfiguration config = ARXConfiguration.create();
        config.addCriterion(new KAnonymity(2));
        config.setMaxOutliers(0d);

        // Access internals
        ARXInterface iface = new ARXInterface(data, config);
        
        iface.getDataQI();
        iface.getDataSE();
        iface.getBuffer(); 
        iface.getHierarchy(0);
        iface.getAttribute(0);
        iface.getNumAttributes();
        iface.getK();
    }
}
