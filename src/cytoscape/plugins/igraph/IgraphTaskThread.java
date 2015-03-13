/**
* Copyright (C) Gerardo Huck, 2011
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published 
* by the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*  
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Lesser General Public License for more details.
*  
* You should have received a copy of the GNU Lesser General Public License
* along with this program.  If not, see <http://www.gnu.org/licenses/>.
*
*/

package cytoscape.plugins.igraph;


//This is specifically for performing the thread
public class IgraphTaskThread extends Thread{
    private int algorithmID = -1;
    //     private GLayResultObject result = null;
    private boolean result = false;


    public boolean getResult() {
	return result;
    }

    public void setAlgorithm(int id){
	this.algorithmID = id;
    }

    //         public GLayResultObject getResult(){
    //             return this.result;
    //         }
    //    @Override
    public void run(){
	
	this.result = false;
	switch(this.algorithmID){
	case 1:
	    //	    IsConnected.loadGraph_optimized();
	    result = IgraphInterface.isConnected();
	    break;

	    //               case IgraphTaskConstants.ALG_FAST_GREEDY_G:
	    //                 result = IgraphInterface.fastgreedy_g();
	    //                 break;
	    //             case IgraphTaskConstants.ALG_FAST_GREEDY_HE_G:
	    //                 result = IgraphInterface.fastgreedy_g_he();
	    //                 break;
	    //             case IgraphTaskConstants.ALG_FAST_GREEDY_HN_G:
	    //                 result = IgraphInterface.fastgreedy_g_hn();
	    //                 break;
	    //             case IgraphTaskConstants.ALG_FAST_GREEDY_HEN_G:
	    //                 result = IgraphInterface.fastgreedy_g_hen();
	    //                 break;
	    //             case IgraphTaskConstants.ALG_CLUSTER:
	    //                 result = IgraphInterface.cluster();
	    //                 break;
	    //             case IgraphTaskConstants.ALG_FAST_GREEDY:
	    //                 result = IgraphInterface.fastGreedy();
	    //                 break;
	    //             case IgraphTaskConstants.ALG_WALK_TRAP:
	    //                 result = IgraphInterface.walkTrap();
	    //                 break;
	    //             case IgraphTaskConstants.ALG_LABEL_PROPAGATION:
	    //                 result = IgraphInterface.labelPropagation();
	    //                 break;
	    //             case IgraphTaskConstants.ALG_EDGE_BETWEENNESS:
	    //                 result = IgraphInterface.edgeBetweenness();
	    //                 break;
	    //             case IgraphTaskConstants.ALG_SPIN_GLASS:
	    //                 result = IgraphInterface.spinGlass();
	    //                 break;
	    //             case IgraphTaskConstants.ALG_LEADING_EIGENVECTOR:
	    //                 result = IgraphInterface.leadingEigenvector();
	    //                 break;

	    //             case IgraphTaskConstants.ALG_SPIN_GLASS_SINGLE:
	    //                 //require at least 1 node to be selected
	    //                 result = IgraphInterface.spinGlassSingle();
	    //                 break;
                
	case -1:

	    //System.out.println("No algorithm was assigned. Do nothing.");

	}
    }
}
