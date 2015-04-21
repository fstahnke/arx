/**
 * 
 */
package org.deidentifier.arx.clustering;

import java.util.Arrays;

/**
 * @author Fabian Stahnke
 *
 */
public class TassaRecord {

	public final int[] recordContent;
	public TassaCluster assignedCluster;
	
	public TassaRecord(int[] content)
	{
		this.recordContent = content;
	}
	
	public int getSize() { return this.recordContent.length; }
	
	public int hashCode() {
		return Arrays.hashCode(recordContent);
	}
	
	public boolean equals(Object o) {
		if (o instanceof TassaRecord) {
			return (Arrays.equals(recordContent, ((TassaRecord)o).recordContent));
		}
		return false;
	}
	
	

}
