package org.deidentifier.arx.recursive;

import java.io.IOException;

import org.deidentifier.arx.ARXAnonymizer;
import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.AttributeType.Hierarchy;
import org.deidentifier.arx.criteria.KAnonymity;

public class RecursiveTest {
    
    public static void main(String[] args) throws IOException {
        
        RecursiveAlgorithm recursiveInstance = new RecursiveAlgorithm();
        Data data = Data.create("data/adult.csv", ';');
        
        // Define input files
        data.getDefinition()
            .setAttributeType("age",
                              Hierarchy.create("data/test_hierarchy_age.csv",
                                               ';'));
        data.getDefinition()
            .setAttributeType("gender",
                              Hierarchy.create("data/test_hierarchy_gender.csv",
                                               ';'));
        data.getDefinition()
            .setAttributeType("zipcode",
                              Hierarchy.create("data/test_hierarchy_zipcode.csv",
                                               ';'));
        
        
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        
        ARXConfiguration config = ARXConfiguration.create();
        

        config.addCriterion(new KAnonymity(2));
        config.setMaxOutliers(0.3d);
        
        
        recursiveInstance.execute(data, config, anonymizer);
        
    }
    
}
