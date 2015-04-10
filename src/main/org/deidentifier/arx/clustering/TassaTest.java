package org.deidentifier.arx.clustering;

import java.io.IOException;
import java.util.ArrayList;

import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.AttributeType.Hierarchy;
import org.deidentifier.arx.criteria.KAnonymity;

public class TassaTest {

	public static void main(String[] args) throws IOException {
		
		// Load data
        Data data = Data.create("data/test.csv", ';');
        data.getDefinition().setAttributeType("age", Hierarchy.create("data/test_hierarchy_age.csv", ';'));
        data.getDefinition().setAttributeType("gender", Hierarchy.create("data/test_hierarchy_gender.csv", ';'));
        data.getDefinition().setAttributeType("zipcode", Hierarchy.create("data/test_hierarchy_zipcode.csv", ';'));

        // Configuration
        ARXConfiguration config = ARXConfiguration.create();
        config.addCriterion(new KAnonymity(2));
        config.setMaxOutliers(0d);
		
        
		TassaAlgorithmImpl tassa = new TassaAlgorithmImpl(data, config);
		
		@SuppressWarnings("unused")
		ArrayList<TassaCluster> clusterList = (ArrayList<TassaCluster>) tassa.executeTassa(0.5, 1.5);

	}

}
