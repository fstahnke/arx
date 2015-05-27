package org.deidentifier.arx.clustering;

import java.io.IOException;

import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.AttributeType.Hierarchy;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.criteria.KAnonymity;

public class TassaTest {
    
    public static void main(String[] args) throws IOException {
        
        // Load data
        final Data data = Data.create("data/test.csv", ';');
        data.getDefinition().setAttributeType("age", Hierarchy.create("data/test_hierarchy_age.csv", ';'));
        data.getDefinition().setAttributeType("gender", Hierarchy.create("data/test_hierarchy_gender.csv", ';'));
        data.getDefinition().setAttributeType("zipcode", Hierarchy.create("data/test_hierarchy_zipcode.csv", ';'));
        
        final Data data2 = Data.create("data/adult_subset.csv", ';');
        //final Data data2 = Data.create("data/adult.csv", ';');
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
        final ARXConfiguration config = ARXConfiguration.create();
        config.addCriterion(new KAnonymity(2));
        config.setMaxOutliers(0d);
        
        // Configuration
        final ARXConfiguration config2 = ARXConfiguration.create();
        config2.addCriterion(new KAnonymity(20));
        config2.setMaxOutliers(0d);

        //final TassaAlgorithmImpl tassa = new TassaAlgorithmImpl(data, config);
        final TassaAlgorithmImpl tassa2 = new TassaAlgorithmImpl(data2, config2);
        
        //final TassaClusterSet clusterList = tassa.execute(0.5, 1.5);

        TassaClusterSet output = null;
        double lastDeltaIL = -Double.MAX_VALUE;

        
        final long initTime = System.nanoTime();
        output = tassa2.execute(0.5, 1.5, output);
        final long stopTime = System.nanoTime();
        final double initialInformationLoss = tassa2.getInititalInformationLoss();
        final double finalInformationLoss = tassa2.getFinalInformationLoss();
        lastDeltaIL = finalInformationLoss - initialInformationLoss;
        System.out.println("Total runtime: " + Math.round((stopTime-initTime) / 1000000000.0) + " s, Initial Information Loss: " + initialInformationLoss + ", Final Information Loss: " + finalInformationLoss);
    
        
        /*
        for (int i = 0; lastDeltaIL <= -0.000000001d || lastDeltaIL > 0; i++) {
            final long initTime = System.nanoTime();
            output = tassa2.execute(0.5, 1.5, output);
            final long stopTime = System.nanoTime();
            final double initialInformationLoss = tassa2.getInititalInformationLoss();
            final double finalInformationLoss = tassa2.getFinalInformationLoss();
            lastDeltaIL = finalInformationLoss - initialInformationLoss;
            System.out.println("#: " + i + ", Total runtime: " + Math.round((stopTime-initTime) / 1000000000.0) + " s, Initial Information Loss: " + initialInformationLoss + ", Final Information Loss: " + finalInformationLoss);
        }
        */

        /*
        for (int i = 0; i < 50; i++) {
            final long initTime = System.nanoTime();
            tassa2.execute(0.5, 1.5, null);
            final long stopTime = System.nanoTime();
            final double initialInformationLoss = tassa2.getInititalInformationLoss();
            final double finalInformationLoss = tassa2.getFinalInformationLoss();
            System.out.println("#: " + i + ", Total runtime: " + Math.round((stopTime-initTime) / 1000000000.0) + " s, Initial Information Loss: " + initialInformationLoss + ", Final Information Loss: " + finalInformationLoss);
        }
        */
        
    }
}
