/*
 * ARX: Powerful Data Anonymization
 * Copyright 2012 - 2015 Florian Kohlmayer, Fabian Prasser
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.deidentifier.arx.utility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.deidentifier.arx.DataDefinition;
import org.deidentifier.arx.DataHandle;

public class DataConverter {

    /**
     * Returns the header
     * @param handle
     * @return
     */
    public String[] getHeader(DataHandle handle) {
        String[] header = new String[handle.getNumColumns()];
        for (int i = 0; i < header.length; i++) {
            header[i] = handle.getAttributeName(i);
        }
        return header;
    }
    
    /**
     * Returns an array representation of the dataset
     * @param handle
     * @return
     */
    public String[][] toArray(DataHandle handle) {
        List<String[]> list = new ArrayList<String[]>();
        for (Iterator<String[]> iter = handle.iterator(); iter.hasNext();) {
            list.add(iter.next());
        }
        return list.toArray(new String[list.size()][]);
    }
    
    /**
     * Returns an array representation of the subset, in which all rows that are not part of the
     * subset have been suppressed
     * @param handle
     * @param subset
     * @return
     */
    public String[][] toArray(DataHandle handle, DataHandle subset) {
        List<String[]> list = new ArrayList<String[]>();
        for (Iterator<String[]> iter = subset.iterator(); iter.hasNext();) {
            list.add(iter.next());
        }
        
        String[] suppressed = new String[handle.getNumColumns()];
        Arrays.fill(suppressed, "*"); // TODO
        for (int i = 0; i < handle.getNumRows() - subset.getNumRows(); i++) {
            list.add(suppressed);
        }
        
        return list.toArray(new String[list.size()][]);
    }

    /**
     * DataDefinition to Map
     * @param definition
     * @return
     */
    public Map<String, String[][]> toMap(DataDefinition definition) {
        
        Map<String, String[][]> map = new HashMap<String, String[][]>();
        for (String s : definition.getQuasiIdentifiersWithGeneralization()) {
            map.put(s, definition.getHierarchy(s));
        }
        return map;
    }
}
