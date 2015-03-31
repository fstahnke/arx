/**
 * 
 */
package org.deidentifier.arx.clustering;

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
	
	

}
