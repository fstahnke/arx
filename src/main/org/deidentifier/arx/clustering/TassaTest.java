package org.deidentifier.arx.clustering;

import java.io.IOException;

import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.AttributeType.Hierarchy;
import org.deidentifier.arx.criteria.KAnonymity;

public class TassaTest {

	@SuppressWarnings("unused")
	public static void main(String[] args) throws IOException {
		
		// Load data
        Data data = Data.create("data/test.csv", ';');
        data.getDefinition().setAttributeType("age", Hierarchy.create("data/test_hierarchy_age.csv", ';'));
        data.getDefinition().setAttributeType("gender", Hierarchy.create("data/test_hierarchy_gender.csv", ';'));
        data.getDefinition().setAttributeType("zipcode", Hierarchy.create("data/test_hierarchy_zipcode.csv", ';'));
        
        Data data2 = Data.create("data/adult.csv", ';');
        data2.getDefinition().setAttributeType("age", Hierarchy.create("data/adult_hierarchy_age.csv", ';'));
        data2.getDefinition().setAttributeType("education", Hierarchy.create("data/adult_hierarchy_education.csv", ';'));
        data2.getDefinition().setAttributeType("marital-status", Hierarchy.create("data/adult_hierarchy_marital-status.csv", ';'));
        data2.getDefinition().setAttributeType("native-country", Hierarchy.create("data/adult_hierarchy_native-country.csv", ';'));
        data2.getDefinition().setAttributeType("occupation", Hierarchy.create("data/adult_hierarchy_occupation.csv", ';'));
        data2.getDefinition().setAttributeType("race", Hierarchy.create("data/adult_hierarchy_race.csv", ';'));
        data2.getDefinition().setAttributeType("salary-class", Hierarchy.create("data/adult_hierarchy_salary-class.csv", ';'));
        data2.getDefinition().setAttributeType("sex", Hierarchy.create("data/adult_hierarchy_sex.csv", ';'));
        data2.getDefinition().setAttributeType("workclass", Hierarchy.create("data/adult_hierarchy_workclass.csv", ';'));

        // Configuration
        ARXConfiguration config = ARXConfiguration.create();
        config.addCriterion(new KAnonymity(20));
        config.setMaxOutliers(0d);
		
        
//        TassaAlgorithmImpl tassa = new TassaAlgorithmImpl(data, config);
		TassaAlgorithmImpl tassa2 = new TassaAlgorithmImpl(data2, config);
		
//		TassaClusterSet clusterList = tassa.executeTassa(0.5, 1.5);
		TassaClusterSet clusterList2 = tassa2.executeTassa(0.5, 1.5);
		
		int test = clusterList2.size() + clusterList2.size();
		
		test = test + 0;

	}

}
