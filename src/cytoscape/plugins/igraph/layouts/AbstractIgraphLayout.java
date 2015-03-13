/**************************************************************************************
Copyright (C) Gerardo Huck, 2011


This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

**************************************************************************************/

package cytoscape.plugins.igraph.layout;

import cytoscape.plugins.igraph.*;
import cytoscape.Cytoscape;
import cytoscape.layout.AbstractLayout;
import cytoscape.layout.LayoutProperties;
import cytoscape.layout.Tunable;
import cytoscape.CyNode;
import cytoscape.logger.CyLogger;

import cytoscape.view.CyNetworkView;
import cytoscape.view.CyNodeView;
import cytoscape.data.CyAttributes;

import csplugins.layout.LayoutPartition;
import csplugins.layout.LayoutEdge;
import csplugins.layout.LayoutNode;
import csplugins.layout.EdgeWeighter;
import csplugins.layout.algorithms.graphPartition.*;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.*;

import giny.view.*;


public abstract class AbstractIgraphLayout extends AbstractGraphPartition {

    private String message;
    protected LayoutProperties layoutProperties;


    /**
     * Value to set for doing unweighted layouts
     */
    public static final String UNWEIGHTEDATTRIBUTE = "(unweighted)";

    /**
     * Whether or not to use edge weights for layout
     */
    protected boolean supportWeights = false;
    
    public AbstractIgraphLayout() {
	super();
	
	//logger = CyLogger.getLogger(AbstractIgraphLayout.class);
	if (edgeWeighter == null)
	    edgeWeighter = new EdgeWeighter();
	
 	layoutProperties = new LayoutProperties(getName());
    }

    // METHODS WHICH MUST BE OVERRIDEN IN CHILD CLASS

    /**
     * Do the layout on a graph alrealy loaded into igraph
     */
    public abstract int layout(double[] x, 
			       double[] y, 
			       LayoutPartition part, 
			       HashMap<Integer,Integer> mapping, 
			       double[] weights);

    /**
     * getName is used to construct property strings
     * for this layout.
     */
    public abstract String getName();
    
    /**
     * toString is used to get the user-visible name
     * of the layout
     */
    public abstract String toString();

    // END OF METHODS WHICH MUST BE OVERRIDEN

    // METHODS WHICH MAY BE OVERRIDEN IN CHILD CLASS        

    /**
     * By default, igraph layouts support laying out only selected nodes
     */
    public boolean supportsSelectedOnly() {
	return true;
    }

    /**
     * Adds tunable objects for adjusting plugin parameters
     * Initializes default values for those parameters
     */
    protected void initialize_properties() {
	
	// Add new properties to layout 
	
	// Initialize layout properties
	layoutProperties.initializeProperties();

	if (supportWeights) {
	    edgeWeighter.getWeightTunables(layoutProperties, getInitialAttributeList());
	}	

	// Force the settings update
	updateSettings(true);
    }

    /**
     * Get new values from tunables and update parameters
     */
    public void updateSettings(boolean force) {
	layoutProperties.updateValues();	

	if (supportWeights) 
	    edgeWeighter.updateSettings(layoutProperties, force);
    }

    /**
     * Get the settings panel for this layout
     */
    public JPanel getSettingsPanel() {
	JPanel panel = new JPanel(new GridLayout(1, 1));
	panel.add(layoutProperties.getTunablePanel());
	
	return panel;
    }

    // END OF METHODS WHICH MAY BE OVERRIDEN         

    // We don't support node attribute-based layouts
    /**
     *  Tells Cytoscape whether we support node attribute based layouts
     *
     * @return nulls, which indicates that we don't
     */
    public byte[] supportsNodeAttributes() {
	return null;
    }

    // We do support edge attribute-based layouts
    /**
     *  Tells Cytoscape whether we support node attribute based layouts
     *
     * @return null if supportWeights is false, otherwise return the attribute
     *         types that can be used for weights.
     */
    public byte[] supportsEdgeAttributes() {
	if (!supportWeights)
	    return null;

	byte[] attrs = { CyAttributes.TYPE_INTEGER, CyAttributes.TYPE_FLOATING };

	return attrs;
    }

    /**
     * Returns "(unweighted)", which is the "attribute" we
     * use to tell the algorithm not to use weights
     *
     * @returns List of our "special" weights
     */
    public List<String> getInitialAttributeList() {
	ArrayList<String> list = new ArrayList<String>();
	list.add(UNWEIGHTEDATTRIBUTE);

	return list;
    }

    /**
     * Overload updateSettings for using it without arguments
     */
    public void updateSettings() {
	updateSettings(false);
    }
        
    /**
     * Revert previous settings
     */
    public void revertSettings() {
	layoutProperties.revertProperties();
    }    
    
    /**
     * Get layout properties
     */
    public LayoutProperties getSettings() {
	return layoutProperties;
    }

    /**
     * 
     */
    public void layoutPartition (LayoutPartition part) {

	// Check whether it has been canceled by the user
	if (canceled)
	    return;

	// Check whether there are nodes to layout or not
	if ( (selectedOnly && (part.nodeCount() - part.lockedNodeCount()) <= 1) 
	     || part.nodeCount() <= 1 ) 
	    return;

	// Show message on the task monitor
	taskMonitor.setStatus("Initializing: Partition: " + part.getPartitionNumber());
	
	// The completed percentage is indeterminable
	taskMonitor.setPercentCompleted(-1);

	// Figure out our starting point if we are only laying out selected nodes
	Dimension initialLocation = null;
	if (selectedOnly) {
	    initialLocation = part.getAverageLocation();
	}

	// Get the number of nodes
	int numNodes = part.nodeCount();

	// Allocate memory for storing graph edges information (to be used as arguments for JNI call)
	double[] x = new double[numNodes];
	double[] y = new double[numNodes];	

	// Load graph into native library
	HashMap<Integer,Integer> mapping = loadGraphPartition(part, selectedOnly);

	double[] weights;
	// If performing a weighted layout, deal with weights
	if (supportWeights) {
	    weights = calculateWeights(part);
	} else {
	    weights = new double[1]; // not sure if leaving it uninitialized would cause problems
	}

	// Check whether it has been canceled by the user
	if (canceled)
	    return;

	// Show message on the task monitor
	taskMonitor.setStatus("Calling native code: Partition: " + part.getPartitionNumber());

	// Do Layout
	layout(x,y, part, mapping, weights);

	// Check whether it has been canceled by the user
	if (canceled)
	    return;

	// Show message on the task monitor
	taskMonitor.setStatus("Updating display");	
	
	updateDisplay(part, mapping, x, y, initialLocation, selectedOnly);

    }// layoutPartion(LayoutPartition part)



    protected void updateDisplay(LayoutPartition part, HashMap<Integer,Integer> mapping, double[] x, double[] y, Dimension initialLocation, boolean selectedOnly) {
	// Find which ratio is required to 'scale up' the node positions so that nodes don't overlap
	double upRatio = 0.0;
	double actualRatio = 0.0;

	for (LayoutEdge edge: part.getEdgeList()) {
	    LayoutNode n1 = edge.getSource();
	    LayoutNode n2 = edge.getTarget();

	    if (n1.isLocked() || n2.isLocked())
		continue;

	    double n1Size = Math.max(n1.getHeight(), n1.getWidth());
	    double n2Size = Math.max(n2.getHeight(), n2.getWidth());

	    double edgeSize = Math.sqrt( Math.pow(x[mapping.get(n1.getIndex())] - x[mapping.get(n2.getIndex())], 2)
				       + Math.pow(y[mapping.get(n1.getIndex())] - y[mapping.get(n2.getIndex())], 2)
				       );
	    // Check if edgeSize is not zero
	    if (edgeSize > 0.0){
	    
		// actualRatio = minimun_desired_length / actual_length
		actualRatio = (n1Size + n2Size) / edgeSize;
		
		// Update upRatio if necessary
		if (actualRatio > upRatio)
		    upRatio = actualRatio;
	    }

	}
	
	// Check whether the ratio is not zero
	if (upRatio < 1.0){   
	    upRatio = 1.0;
	}

	double oldUpRatio = upRatio;


	// Move nodes to their position
	boolean success = false;
	while (!success){
	    
	    try{
		// We will keep track of the offset of the whole part so that we can eliminate it
		double minX = x[0];
		double minY = y[0];
		
		// Get the 'offset' of the whole partition, so that we can eliminate it
		for (int i = 1; i < mapping.size(); i++) {
		    
		    if (x[i] < minX)
			minX = x[i];

		    if (y[i] < minY)
			minY = y[i];
		}

		minX = upRatio * minX;
		minY = upRatio * minY;
		
		// Reset the nodes so we get the new average location
		part.resetNodes(); 
		
		// Create an iterator for processing the nodes
		Iterator<LayoutNode> iterator2 = part.getNodeList().iterator();
				
		while (iterator2.hasNext()){
		    
		    // Get next node
		    LayoutNode node = (LayoutNode) iterator2.next();

		    // If it is locked, skip it
		    if (!node.isLocked()) {			

			// Set node's X and Y positions
			node.setX(upRatio * x[mapping.get(node.getIndex())] - minX);
			node.setY(upRatio * y[mapping.get(node.getIndex())] - minY);

			// Move node to desired location
			part.moveNodeToLocation(node);
		    }
		}
	
	
		// Not quite done, yet.  If we're only laying out selected nodes, we need
		// to migrate the selected nodes back to their starting position
		if (selectedOnly) {
		    double xDelta = 0.0;
		    double yDelta = 0.0;
		    Dimension finalLocation = part.getAverageLocation();
		    xDelta = finalLocation.getWidth()  - initialLocation.getWidth();
		    yDelta = finalLocation.getHeight() - initialLocation.getHeight();
		    for (LayoutNode v: part.getNodeList()) {
			if (!v.isLocked()) {
			    v.decrement(xDelta, yDelta);
			    part.moveNodeToLocation(v);
			}
		    }
		}
		
		success = true;

	    }
	    catch(Exception excep){

		upRatio = upRatio / 10.0;
		if (upRatio <= 0.0){
		    message = "Sorry, cannot produce layout";
		    JOptionPane.showMessageDialog(Cytoscape.getDesktop(), message);
		    return;
		}
		success = false;
	    }

	}//while(!success)

    } //updateDisplay


    /**
     * This function loads a partition into igraph
     *
     */    
    public static HashMap<Integer,Integer> loadGraphPartition(LayoutPartition part, 
							      boolean selectedOnly){

	CyLogger logger = CyLogger.getLogger(AbstractIgraphLayout.class);	    
	
 	// JOptionPane.showMessageDialog(Cytoscape.getDesktop(), "Load graph called!");	   

	// Create a reverse mapping
	int nodeCount = part.nodeCount();

	HashMap<Integer, Integer> nodeIdMapping;
	if (selectedOnly) 
	    nodeIdMapping = new HashMap<Integer, Integer>(nodeCount - part.lockedNodeCount());
	else
	    nodeIdMapping = new HashMap<Integer, Integer>(nodeCount);
	int j = 0;

	Iterator<LayoutNode> nodeIt = part.nodeIterator();	
	if (selectedOnly) {
	    while(nodeIt.hasNext()) {            
		LayoutNode node = (LayoutNode) nodeIt.next();
		if (!node.isLocked()) {
		    nodeIdMapping.put(node.getIndex(), j);
		    j++;
		}
	    }
	} else {
	    while(nodeIt.hasNext()) {            
		LayoutNode node = (LayoutNode) nodeIt.next();
		nodeIdMapping.put(node.getIndex(), j);
		j++;
	    }
	}
		
	// Write edges (as pairs of consecutive nodes) in edgeArray
	int[] edgeArray = new int[part.edgeCount() * 2];
	int i = 0;
	
	Iterator<LayoutEdge> it = part.getEdgeList().iterator();

	if (selectedOnly) {	
	    while (it.hasNext()) {
		LayoutEdge e = (LayoutEdge) it.next();

		LayoutNode source = e.getSource();
		LayoutNode target = e.getTarget();

		if (source.isLocked() || target.isLocked())
		    continue;

		edgeArray[i]     = nodeIdMapping.get(source.getIndex());
		edgeArray[i + 1] = nodeIdMapping.get(target.getIndex());
		i += 2;	    
	    }
	} else {
	    while (it.hasNext()) {
		LayoutEdge e = (LayoutEdge) it.next();

		LayoutNode source = e.getSource();
		LayoutNode target = e.getTarget();

		edgeArray[i]     = nodeIdMapping.get(source.getIndex());
		edgeArray[i + 1] = nodeIdMapping.get(target.getIndex());
		i += 2;	    
	    }
	}

	IgraphInterface.createGraph(edgeArray, i, 0); // no need for directed graph

	return nodeIdMapping;
    } // loadGraphPartition()


    // Store current node positions
    public void loadPositions(LayoutPartition part, HashMap<Integer,Integer> mapping, double[] x, double[] y){
	Iterator<LayoutNode> iterator = part.getNodeList().iterator();	
	while (iterator.hasNext()){	    
	    // Get next node
	    LayoutNode node = (LayoutNode) iterator.next();
	    
	    if (!selectedOnly || !node.isLocked()) {				       
		x[mapping.get(node.getIndex())] = node.getX();
		y[mapping.get(node.getIndex())] = node.getY();		
	    }	
	}
    }


    // Calculate edge weights
    double[] calculateWeights(LayoutPartition part) {
	part.calculateEdgeWeights();
	
	// Write edge weight in an array
	double[] edgeWeights = new double[part.edgeCount()];
	int i = 0;
	
	Iterator<LayoutEdge> it = part.getEdgeList().iterator();

	if (selectedOnly) {	
	    while (it.hasNext()) {
		LayoutEdge e = (LayoutEdge) it.next();

		LayoutNode source = e.getSource();
		LayoutNode target = e.getTarget();

		if (source.isLocked() || target.isLocked())
		    continue;

		edgeWeights[i] = e.getWeight();
		i++;	    
	    }
	} else {
	    while (it.hasNext()) {
		LayoutEdge e = (LayoutEdge) it.next();

		LayoutNode source = e.getSource();
		LayoutNode target = e.getTarget();

		edgeWeights[i] = e.getWeight();
		i++;	    
	    }
	}

	return edgeWeights;
    }

}

