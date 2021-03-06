/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package justclust.plugins.clustering.mcode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import justclust.datastructures.Data;
import justclust.datastructures.Edge;
import justclust.datastructures.Node;

/**
 * An implementation of the MCODE algorithm
 */
public class McodeAlgorithm {

    private boolean cancelled;
//    private TaskMonitor taskMonitor;
//    private static final Logger logger = LoggerFactory.getLogger(MCODEAlgorithm.class);

    //data structure for storing information required for each node
//    private static class NodeInfo {
    public static class NodeInfo {

        double density; //neighborhood density
        int numNodeNeighbors; //number of node neighbors
        Long[] nodeNeighbors; //stores node indices of all neighbors
        int coreLevel; //e.g. 2 = a 2-core
        double coreDensity; //density of the core neighborhood
        double score; //node score

        public NodeInfo() {
            this.density = 0.0;
            this.numNodeNeighbors = 0;
            this.coreLevel = 0;
            this.coreDensity = 0.0;
        }
    }
    //data structures useful to have around for more than one cluster finding iteration
    //key is the node SUID, value is a NodeInfo instance
//    private Map<Long, NodeInfo> currentNodeInfoHashMap;
    public Map<Long, NodeInfo> currentNodeInfoHashMap;
    //key is node score, value is nodeIndex
    private SortedMap<Double, List<Long>> currentNodeScoreSortedMap;
    //because every network can be scored and clustered several times with different parameters
    //these results have to be stored so that the same scores are used during exploration when
    //the user is switching between the various results
    //Since the network is not always rescored whenever a new result is generated (if the scoring parameters
    //haven't changed for example) the clustering method must save the current node scores under the new result
    //title for later reference
    //key is result id, value is nodeScoreSortedMap
    private Map<Integer, SortedMap<Double, List<Long>>> nodeScoreResultsMap = new HashMap<Integer, SortedMap<Double, List<Long>>>();
    //key is result id, value is nodeInfroHashMap
    private Map<Integer, Map<Long, NodeInfo>> nodeInfoResultsMap = new HashMap<Integer, Map<Long, NodeInfo>>();
//    private MCODEParameterSet params; //the parameters used for this instance of the algorithm
    private McodeParameterSet params; //the parameters used for this instance of the algorithm
    //stats
    private long lastScoreTime;
    private long lastFindTime;
//    private final MCODEUtil mcodeUtil;
    private final McodeUtil mcodeUtil;

    /**
     * The constructor. Use this to get an instance of MCODE to run.
     *
     * @param networkID Allows the algorithm to get the parameters of the
     * focused network
     */
//    public MCODEAlgorithm(final Long networkID, final MCODEUtil mcodeUtil) {
    public McodeAlgorithm(final Long networkID, final McodeUtil mcodeUtil) {
        this.mcodeUtil = mcodeUtil;
        this.params = mcodeUtil.getCurrentParameters().getParamsCopy(networkID);
    }

//    public MCODEAlgorithm(final TaskMonitor taskMonitor, final Long networkID, final MCODEUtil mcodeUtil) {
//        this(networkID, mcodeUtil);
//        this.taskMonitor = taskMonitor;
//    }
//    public void setTaskMonitor(TaskMonitor taskMonitor, Long networkID) {
//        this.params = mcodeUtil.getCurrentParameters().getParamsCopy(networkID);
//        this.taskMonitor = taskMonitor;
//    }
    /**
     * Get the time taken by the last score operation in this instance of the
     * algorithm
     *
     * @return the duration of the scoring portion
     */
    public long getLastScoreTime() {
        return lastScoreTime;
    }

    /**
     * Get the time taken by the last find operation in this instance of the
     * algorithm
     *
     * @return the duration of the finding process
     */
    public long getLastFindTime() {
        return lastFindTime;
    }

    /**
     * Get the parameter set used for this instance of MCODEAlgorithm
     *
     * @return The parameter set used
     */
//    public MCODEParameterSet getParams() {
    public McodeParameterSet getParams() {
        return params;
    }

    /**
     * If set, will schedule the algorithm to be cancelled at the next
     * convenient opportunity
     *
     * @param cancelled Set to true if the algorithm should be cancelled
     */
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    /**
     * Gets the calculated node score of a node from a given result. Used in
     * MCODEResultsPanel during the attribute setting method.
     *
     * @param nodeId Number which is used to identify the nodes in the
     * score-sorted tree map
     * @param resultId Id of the results for which we are retrieving a node
     * score
     * @return node score as a Double
     */
    public double getNodeScore(Long nodeId, int resultId) {
        Map<Double, List<Long>> nodeScoreSortedMap = nodeScoreResultsMap.get(resultId);

        for (double nodeScore : nodeScoreSortedMap.keySet()) {
            List<Long> nodes = nodeScoreSortedMap.get(nodeScore);

            if (nodes.contains(nodeId)) {
                return nodeScore;
            }
        }

        return 0.0;
    }

    /**
     * Gets the highest node score in a given result. Used in the
     * MCODEVisualStyleAction class to re-initialize the visual calculators.
     *
     * @param resultId Id of the result
     * @return First key in the nodeScoreSortedMap corresponding to the highest
     * score
     */
    public double getMaxScore(int resultId) {
        SortedMap<Double, List<Long>> nodeScoreSortedMap = nodeScoreResultsMap.get(resultId);

        //Since the map is sorted, the first key is the highest value
        return nodeScoreSortedMap.firstKey();
    }

    /**
     * Step 1: Score the graph and save scores as node attributes. Scores are
     * also saved internally in your instance of MCODEAlgorithm.
     *
     * @param inputNetwork The network that will be scored
     * @param resultId Title of the result, used as an identifier in various
     * hash maps
     */
//    public void scoreGraph(final CyNetwork inputNetwork, final int resultId) {
    public void scoreGraph(final int resultId) {
        final String callerID = "MCODEAlgorithm.MCODEAlgorithm";

//        if (inputNetwork == null) {
//            logger.error("In " + callerID + ": inputNetwork was null.");
//            return;
//        }

        // Initialize
        long msTimeBefore = System.currentTimeMillis();
//        final Map<Long, NodeInfo> nodeInfoHashMap = new HashMap<Long, NodeInfo>(inputNetwork.getNodeCount());
        final Map<Long, NodeInfo> nodeInfoHashMap = new HashMap<Long, NodeInfo>(McodeClusteringAlgorithm.classInstance.networkNodes.size());

        // Sort Doubles in descending order
        Comparator<Double> scoreComparator = new Comparator<Double>() {
            @Override
            public int compare(Double d1, Double d2) {
                return d2.compareTo(d1);
            }
        };

        // Will store Doubles (score) as the key, Lists of node indexes as values
        SortedMap<Double, List<Long>> nodeScoreSortedMap = new TreeMap<Double, List<Long>>(scoreComparator);

        // Iterate over all nodes and calculate MCODE score
        List<Long> al = null;
        int i = 0;
//        final List<CyNode> nodes = inputNetwork.getNodeList();
        final List<Node> nodes = McodeClusteringAlgorithm.classInstance.networkNodes;

//        for (final CyNode n : nodes) {
        for (final Node n : nodes) {
//            final NodeInfo nodeInfo = calcNodeInfo(inputNetwork, n.getSUID());
            final NodeInfo nodeInfo = calcNodeInfo(McodeClusteringAlgorithm.classInstance.networkNodes.indexOf(n));
//            nodeInfoHashMap.put(n.getSUID(), nodeInfo);
            nodeInfoHashMap.put((long) McodeClusteringAlgorithm.classInstance.networkNodes.indexOf(n), nodeInfo);
            double nodeScore = scoreNode(nodeInfo);

//            System.out.println(n.label + "\t" + nodeInfo.coreDensity + "\t" + nodeInfo.coreLevel + "\t" + nodeInfo.density + "\t" + nodeInfo.nodeNeighbors + "\t" + nodeInfo.numNodeNeighbors + "\t" + nodeInfo.score);

            // Save score for later use in TreeMap
            // Add a list of nodes to each score in case nodes have the same score
            if (nodeScoreSortedMap.containsKey(nodeScore)) {
                // Already have a node with this score, add it to the list
                al = nodeScoreSortedMap.get(nodeScore);
//                al.add(n.getSUID());
                al.add((long) McodeClusteringAlgorithm.classInstance.networkNodes.indexOf(n));
            } else {
                al = new ArrayList<Long>();
//                al.add(n.getSUID());
                al.add((long) McodeClusteringAlgorithm.classInstance.networkNodes.indexOf(n));
                nodeScoreSortedMap.put(nodeScore, al);
            }

//            if (taskMonitor != null) {
//                taskMonitor.setProgress(++i / (double) nodes.size());
//            }

            if (cancelled) {
                break;
            }
        }

        nodeScoreResultsMap.put(resultId, nodeScoreSortedMap);
        nodeInfoResultsMap.put(resultId, nodeInfoHashMap);

        currentNodeScoreSortedMap = nodeScoreSortedMap;
        currentNodeInfoHashMap = nodeInfoHashMap;

        long msTimeAfter = System.currentTimeMillis();
        lastScoreTime = msTimeAfter - msTimeBefore;
    }

    /**
     * Step 2: Find all clusters given a scored graph. If the input network has
     * not been scored, this method will return null. This method is called when
     * the user selects network scope or single node scope.
     *
     * @param inputNetwork The scored network to find clusters in.
     * @param resultSetName Title of the result
     * @return A list containing an MCODECluster object for each cluster.
     */
//    public List<MCODECluster> findClusters(final CyNetwork inputNetwork, final int resultId) {
    public List<McodeCluster> findClusters(final int resultId) {
        final SortedMap<Double, List<Long>> nodeScoreSortedMap;
        final Map<Long, NodeInfo> nodeInfoHashMap;

        // First we check if the network has been scored under this result title (i.e. scoring
        // was required due to a scoring parameter change).  If it hasn't then we want to use the
        // current scores that were generated the last time the network was scored and store them
        // under the title of this result set for later use
        if (!nodeScoreResultsMap.containsKey(resultId)) {
            nodeScoreSortedMap = currentNodeScoreSortedMap;
            nodeInfoHashMap = currentNodeInfoHashMap;

            nodeScoreResultsMap.put(resultId, nodeScoreSortedMap);
            nodeInfoResultsMap.put(resultId, nodeInfoHashMap);
        } else {
            nodeScoreSortedMap = nodeScoreResultsMap.get(resultId);
            nodeInfoHashMap = nodeInfoResultsMap.get(resultId);
        }

        String callerID = "MCODEAlgorithm.findClusters";

//        if (inputNetwork == null) {
//            logger.error("In " + callerID + ": inputNetwork was null.");
//            return null;
//        }

        if (nodeInfoHashMap == null || nodeScoreSortedMap == null) {
//            logger.error("In " + callerID + ": nodeInfoHashMap or nodeScoreSortedMap was null.");
            return null;
        }

        // Initialization
        long msTimeBefore = System.currentTimeMillis();
        HashMap<Long, Boolean> nodeSeenHashMap = new HashMap<Long, Boolean>(); //key is node SUID
        Long currentNode = null;
        double findingProgress = 0;
        double findingTotal = 0;
        Collection<List<Long>> values = nodeScoreSortedMap.values(); //returns a Collection sorted by key order (descending)

        // In order to track the progress without significant lags (for times when many nodes have the same score
        // and no progress is reported) we count all the scored nodes and track those instead
        for (List<Long> value : values) {
            findingTotal += value.size();
        }

        // Stores the list of clusters as ArrayLists of node indices in the input Network
//        List<MCODECluster> alClusters = new ArrayList<MCODECluster>();
        List<McodeCluster> alClusters = new ArrayList<McodeCluster>();

        // Iterate over node ids sorted descending by their score
        for (List<Long> alNodesWithSameScore : values) {
            // Each score may be associated with multiple nodes, iterate over these lists
            for (int j = 0; j < alNodesWithSameScore.size(); j++) {
                currentNode = alNodesWithSameScore.get(j);

                if (!nodeSeenHashMap.containsKey(currentNode)) {
                    // Store the list of all the nodes that have already been seen and incorporated in other clusters
                    Map<Long, Boolean> nodeSeenHashMapSnapShot = new HashMap<Long, Boolean>(nodeSeenHashMap);

                    // Here we use the original node score cutoff
                    List<Long> alCluster = getClusterCore(currentNode,
                            nodeSeenHashMap,
                            params.getNodeScoreCutoff(),
                            params.getMaxDepthFromStart(),
                            nodeInfoHashMap);
                    if (alCluster.size() > 0) {
                        // Make sure seed node is part of cluster, if not already in there
                        if (!alCluster.contains(currentNode)) {
                            alCluster.add(currentNode);
                        }

                        // Create an input graph for the filter and haircut methods
//                        MCODEGraph clusterGraph = createClusterGraph(alCluster, inputNetwork);
                        McodeGraph clusterGraph = createClusterGraph(alCluster);

                        if (!filterCluster(clusterGraph)) {
                            if (params.isHaircut()) {
                                haircutCluster(clusterGraph, alCluster);
                            }

                            if (params.isFluff()) {
                                fluffClusterBoundary(alCluster, nodeSeenHashMap, nodeInfoHashMap);
                            }

//                            clusterGraph = createClusterGraph(alCluster, inputNetwork);
                            clusterGraph = createClusterGraph(alCluster);
                            final double score = scoreCluster(clusterGraph);

//                            MCODECluster currentCluster = new MCODECluster(resultId, currentNode, clusterGraph, score,
//                                    alCluster, nodeSeenHashMapSnapShot);
                            McodeCluster currentCluster = new McodeCluster(resultId, currentNode, clusterGraph, score,
                                    (ArrayList<Long>) alCluster, nodeSeenHashMapSnapShot);

                            alClusters.add(currentCluster);
                        }
                    }
                }

//                if (taskMonitor != null) {
//                    // We want to be sure that only progress changes are reported and not
//                    // miniscule decimal increments so that the taskMonitor isn't overwhelmed
//                    double newProgress = ++findingProgress / findingTotal;
//
//                    if (Math.round(newProgress * 100) != Math.round((newProgress - 0.01) * 100)) {
//                        taskMonitor.setProgress(newProgress);
//                    }
//                }

                if (cancelled) {
                    break;
                }
            }
        }

        // Once the clusters have been found we either return them or in the case of selection scope, we select only
        // the ones that contain the selected node(s) and return those
//        List<MCODECluster> selectedALClusters = new ArrayList<MCODECluster>();
        List<McodeCluster> selectedALClusters = new ArrayList<McodeCluster>();

//        if (MCODEParameterSet.SELECTION.equals(params.getScope())) {
        if (McodeParameterSet.SELECTION.equals(params.getScope())) {
//            for (MCODECluster cluster : alClusters) {
            for (McodeCluster cluster : alClusters) {
                List<Long> alCluster = cluster.getALCluster();
                List<Long> alSelectedNodes = new ArrayList<Long>();

                for (int i = 0; i < params.getSelectedNodes().length; i++) {
                    alSelectedNodes.add(params.getSelectedNodes()[i]);
                }

                // Method for returning all clusters that contain any of the selected nodes
                for (Long nodeIndex : alSelectedNodes) {
                    if (alCluster.contains(nodeIndex)) {
                        selectedALClusters.add(cluster);
                        break;
                    }
                }
            }

            alClusters = selectedALClusters;
        }

        long msTimeAfter = System.currentTimeMillis();
        lastFindTime = msTimeAfter - msTimeBefore;

        return alClusters;
    }

    /**
     * Finds the cluster based on user's input via size slider.
     *
     * @param cluster cluster being explored
     * @param nodeScoreCutoff slider source value
     * @param inputNet network
     * @param resultId ID of the result set being explored
     * @return explored cluster
     */
//    public MCODECluster exploreCluster(final MCODECluster cluster,
//            final double nodeScoreCutoff,
//            final CyNetwork inputNet,
//            final int resultId) {
    public McodeCluster exploreCluster(final McodeCluster cluster,
            final double nodeScoreCutoff,
            final int resultId) {
        // This method is similar to the finding method with the exception of the filtering so that the decrease of the cluster size
        // can produce a single node, also the use of the node seen hash map is differentially applied...
        final Map<Long, NodeInfo> nodeInfoHashMap = nodeInfoResultsMap.get(resultId);
//        final MCODEParameterSet params = mcodeUtil.getCurrentParameters().getResultParams(cluster.getResultId());
        final McodeParameterSet params = mcodeUtil.getCurrentParameters().getResultParams(cluster.getResultId());
        final Map<Long, Boolean> nodeSeenHashMap;

        // If the size slider is below the set node score cutoff we use the node seen hash map so that clusters
        // with higher scoring seeds have priority, however when the slider moves higher than the node score cutoff
        // we allow the cluster to accrue nodes from all around without the priority restriction
        if (nodeScoreCutoff <= params.getNodeScoreCutoff()) {
            nodeSeenHashMap = new HashMap<Long, Boolean>(cluster.getNodeSeenHashMap());
        } else {
            nodeSeenHashMap = new HashMap<Long, Boolean>();
        }

        final Long seedNode = cluster.getSeedNode();

        final List<Long> alCluster = getClusterCore(seedNode, nodeSeenHashMap, nodeScoreCutoff, params
                .getMaxDepthFromStart(), nodeInfoHashMap);

        // Make sure seed node is part of cluster, if not already in there
        if (!alCluster.contains(seedNode)) {
            alCluster.add(seedNode);
        }

        // Create an input graph for the filter and haircut methods
//        MCODEGraph clusterGraph = createClusterGraph(alCluster, inputNet);
        McodeGraph clusterGraph = createClusterGraph(alCluster);

        if (params.isHaircut()) {
            haircutCluster(clusterGraph, alCluster);
        }

        if (params.isFluff()) {
            fluffClusterBoundary(alCluster, nodeSeenHashMap, nodeInfoHashMap);
        }

//        clusterGraph = createClusterGraph(alCluster, inputNet);
        clusterGraph = createClusterGraph(alCluster);
        final double score = scoreCluster(clusterGraph);

//        final MCODECluster newCluster = new MCODECluster(resultId, seedNode, clusterGraph, score, alCluster,
//                nodeSeenHashMap);
        final McodeCluster newCluster = new McodeCluster(resultId, seedNode, clusterGraph, score, (ArrayList<Long>) alCluster,
                nodeSeenHashMap);
        newCluster.setRank(cluster.getRank());

        return newCluster;
    }

//    private MCODEGraph createClusterGraph(final List<Long> alCluster, final CyNetwork inputNet) {
    private McodeGraph createClusterGraph(final List<Long> alCluster) {
//        final Set<CyNode> nodes = new HashSet<CyNode>();
        final Set<Node> nodes = new HashSet<Node>();

        for (final Long id : alCluster) {
//            CyNode n = inputNet.getNode(id);
            Node n = McodeClusteringAlgorithm.classInstance.networkNodes.get(id.intValue());
            nodes.add(n);
        }

//        final MCODEGraph clusterGraph = mcodeUtil.createGraph(inputNet, nodes);
        final McodeGraph clusterGraph = mcodeUtil.createGraph(McodeClusteringAlgorithm.classInstance.networkNodes, McodeClusteringAlgorithm.classInstance.networkEdges, nodes);

        return clusterGraph;
    }

    /**
     * Score node using the formula from original MCODE paper. This formula
     * selects for larger, denser cores. This is a utility function for the
     * algorithm.
     *
     * @param nodeInfo The internal data structure to fill with node information
     * @return The score of this node.
     */
    private double scoreNode(NodeInfo nodeInfo) {
        if (nodeInfo.numNodeNeighbors > params.getDegreeCutoff()) {
            nodeInfo.score = nodeInfo.coreDensity * (double) nodeInfo.coreLevel;
        } else {
            nodeInfo.score = 0.0;
        }

        return nodeInfo.score;
    }

    /**
     * Score a cluster. Currently this ranks larger, denser clusters higher,
     * although in the future other scoring functions could be created
     *
     * @param clusterGraph
     * @return The score of the cluster
     */
//    public double scoreCluster(final MCODEGraph clusterGraph) {
    public double scoreCluster(final McodeGraph clusterGraph) {
        int numNodes = 0;
        double density = 0.0, score = 0.0;

        numNodes = clusterGraph.getNodeCount();
        density = calcDensity(clusterGraph, params.isIncludeLoops());
        score = density * numNodes;

        return score;
    }

    /**
     * Calculates node information for each node according to the original MCODE
     * publication. This information is used to score the nodes in the scoring
     * stage. This is a utility function for the algorithm.
     *
     * @param inputNetwork The input network for reference
     * @param nodeId The SUID of the node in the input network to score
     * @return A NodeInfo object containing node information required for the
     * algorithm
     */
//    private NodeInfo calcNodeInfo(final CyNetwork inputNetwork, final Long nodeId) {
    private NodeInfo calcNodeInfo(final long nodeId) {
        final Long[] neighborhood;
        final String callerID = "MCODEAlgorithm.calcNodeInfo";

//        if (inputNetwork == null) {
//            logger.error("In " + callerID + ": gpInputGraph was null.");
//            return null;
//        }

        // Get neighborhood of this node (including the node)
//        CyNode rootNode = inputNetwork.getNode(nodeId);
        Node rootNode = McodeClusteringAlgorithm.classInstance.networkNodes.get((int) nodeId);
//        List<CyNode> neighbors = inputNetwork.getNeighborList(rootNode, CyEdge.Type.ANY);
        List<Node> neighbors = new ArrayList<Node>();
        for (Edge edge : rootNode.edges) {
            if (edge.node1 == rootNode && edge.node1 != edge.node2) {
                neighbors.add(edge.node2);
            }
            if (edge.node2 == rootNode && edge.node1 != edge.node2) {
                neighbors.add(edge.node1);
            }
        }

        if (neighbors.size() < 2) {
            // If there are no neighbors or just one neighbor, nodeInfo calculation is trivial
            NodeInfo nodeInfo = new NodeInfo();

            if (neighbors.size() == 1) {
                nodeInfo.coreLevel = 1;
                nodeInfo.coreDensity = 1.0;
                nodeInfo.density = 1.0;
            }

            return nodeInfo;
        }

        Long[] neighborIndexes = new Long[neighbors.size()];
        int i = 0;

//        for (CyNode n : neighbors) {
        for (Node n : neighbors) {
//            neighborIndexes[i++] = n.getSUID();
            neighborIndexes[i++] = (long) McodeClusteringAlgorithm.classInstance.networkNodes.indexOf(n);
        }

        // Add original node to extract complete neighborhood
        Arrays.sort(neighborIndexes);

        if (Arrays.binarySearch(neighborIndexes, nodeId) < 0) {
            neighborhood = new Long[neighborIndexes.length + 1];
            System.arraycopy(neighborIndexes, 0, neighborhood, 1, neighborIndexes.length);
            neighborhood[0] = nodeId;
            neighbors.add(rootNode);
        } else {
            neighborhood = neighborIndexes;
        }

        // extract neighborhood subgraph
//        final MCODEGraph neighborhoodGraph = mcodeUtil.createGraph(inputNetwork, neighbors);
        final McodeGraph neighborhoodGraph = mcodeUtil.createGraph(McodeClusteringAlgorithm.classInstance.networkNodes, McodeClusteringAlgorithm.classInstance.networkEdges, neighbors);

        if (neighborhoodGraph == null) {
            // this shouldn't happen
//            logger.error("In " + callerID + ": neighborhoodGraph was null.");
            return null;
        }

        // Calculate the node information for each node
        final NodeInfo nodeInfo = new NodeInfo();

        // Density
        if (neighborhoodGraph != null) {
            nodeInfo.density = calcDensity(neighborhoodGraph, params.isIncludeLoops());
        }

        nodeInfo.numNodeNeighbors = neighborhood.length;

        // Calculate the highest k-core
        Integer k = null;
        Object[] returnArray = getHighestKCore(neighborhoodGraph);
        k = (Integer) returnArray[0];
//        MCODEGraph kCore = (MCODEGraph) returnArray[1];
        McodeGraph kCore = (McodeGraph) returnArray[1];
        nodeInfo.coreLevel = k.intValue();

        // Calculate the core density - amplifies the density of heavily interconnected regions and attenuates
        // that of less connected regions
        if (kCore != null) {
            nodeInfo.coreDensity = calcDensity(kCore, params.isIncludeLoops());
        }

        // Record neighbor array for later use in cluster detection step
        nodeInfo.nodeNeighbors = neighborhood;

        return nodeInfo;
    }

    /**
     * Find the high-scoring central region of the cluster. This is a utility
     * function for the algorithm.
     *
     * @param startNode The node that is the seed of the cluster
     * @param nodeSeenHashMap The list of nodes seen already
     * @param nodeScoreCutoff Slider input used for cluster exploration
     * @param maxDepthFromStart Limits the number of recursions
     * @param nodeInfoHashMap Provides the node scores
     * @return A list of node indexes representing the core of the cluster
     */
    private List<Long> getClusterCore(Long startNode,
            Map<Long, Boolean> nodeSeenHashMap,
            double nodeScoreCutoff,
            int maxDepthFromStart,
            Map<Long, NodeInfo> nodeInfoHashMap) {
        List<Long> cluster = new ArrayList<Long>(); //stores node indexes
        getClusterCoreInternal(startNode,
                nodeSeenHashMap,
                ((NodeInfo) nodeInfoHashMap.get(startNode)).score,
                1,
                cluster,
                nodeScoreCutoff,
                maxDepthFromStart,
                nodeInfoHashMap);

        return cluster;
    }

    /**
     * An internal function that does the real work of getClusterCore,
     * implemented to enable recursion.
     *
     * @param startNode The node that is the seed of the cluster
     * @param nodeSeenHashMap The list of nodes seen already
     * @param startNodeScore The score of the seed node
     * @param currentDepth The depth away from the seed node that we are
     * currently at
     * @param cluster The cluster to add to if we find a cluster node in this
     * method
     * @param nodeScoreCutoff Helps determine if the nodes being added are
     * within the given threshold
     * @param maxDepthFromStart Limits the recursion
     * @param nodeInfoHashMap Provides score info
     * @return true
     */
    private boolean getClusterCoreInternal(Long startNode,
            Map<Long, Boolean> nodeSeenHashMap,
            double startNodeScore,
            int currentDepth,
            List<Long> cluster,
            double nodeScoreCutoff,
            int maxDepthFromStart,
            Map<Long, NodeInfo> nodeInfoHashMap) {
        // base cases for recursion
        if (nodeSeenHashMap.containsKey(startNode)) {
            return true; //don't recheck a node
        }

        nodeSeenHashMap.put(startNode, true);

        if (currentDepth > maxDepthFromStart) {
            return true; //don't exceed given depth from start node
        }

        // Initialization
        Long currentNeighbor;
        int numNodeNeighbors = nodeInfoHashMap.get(startNode).numNodeNeighbors;
        int i = 0;

        for (i = 0; i < numNodeNeighbors; i++) {
            // go through all currentNode neighbors to check their core density for cluster inclusion
            currentNeighbor = nodeInfoHashMap.get(startNode).nodeNeighbors[i];

            if ((!nodeSeenHashMap.containsKey(currentNeighbor))
                    && (nodeInfoHashMap.get(currentNeighbor).score >= (startNodeScore - startNodeScore * nodeScoreCutoff))) {

                // add current neighbor
                if (!cluster.contains(currentNeighbor)) {
                    cluster.add(currentNeighbor);
                }

                // try to extend cluster at this node
                getClusterCoreInternal(currentNeighbor,
                        nodeSeenHashMap,
                        startNodeScore,
                        currentDepth + 1,
                        cluster,
                        nodeScoreCutoff,
                        maxDepthFromStart,
                        nodeInfoHashMap);
            }
        }

        return true;
    }

    /**
     * Fluff up the cluster at the boundary by adding lower scoring, non
     * cluster-core neighbors This implements the cluster fluff feature.
     *
     * @param cluster The cluster to fluff
     * @param nodeSeenHashMap The list of nodes seen already
     * @param nodeInfoHashMap Provides neighbour info
     * @return true
     */
    private boolean fluffClusterBoundary(List<Long> cluster,
            Map<Long, Boolean> nodeSeenHashMap,
            Map<Long, NodeInfo> nodeInfoHashMap) {
        Long currentNode = null, nodeNeighbor = null;
        // Create a temp list of nodes to add to avoid concurrently modifying 'cluster'
        List<Long> nodesToAdd = new ArrayList<Long>();

        // Keep a separate internal nodeSeenHashMap because nodes seen during a fluffing
        // should not be marked as permanently seen,
        // they can be included in another cluster's fluffing step.
        Map<Long, Boolean> nodeSeenHashMapInternal = new HashMap<Long, Boolean>();

        // add all current neighbour's neighbours into cluster (if they have high enough clustering coefficients) and mark them all as seen
        for (int i = 0; i < cluster.size(); i++) {
            currentNode = cluster.get(i);

            for (int j = 0; j < nodeInfoHashMap.get(currentNode).numNodeNeighbors; j++) {
                nodeNeighbor = nodeInfoHashMap.get(currentNode).nodeNeighbors[j];

                if ((!nodeSeenHashMap.containsKey(nodeNeighbor))
                        && (!nodeSeenHashMapInternal.containsKey(nodeNeighbor))
                        && ((((NodeInfo) nodeInfoHashMap.get(nodeNeighbor)).density) > params.getFluffNodeDensityCutoff())) {
                    nodesToAdd.add(nodeNeighbor);
                    nodeSeenHashMapInternal.put(nodeNeighbor, true);
                }
            }
        }

        // Add fluffed nodes to cluster
        if (nodesToAdd.size() > 0) {
            cluster.addAll(nodesToAdd.subList(0, nodesToAdd.size()));
        }

        return true;
    }

    /**
     * Checks if the cluster needs to be filtered according to heuristics in
     * this method
     *
     * @param clusterGraph The cluster to check if it passes the filter
     * @return true if cluster should be filtered, false otherwise
     */
//    private boolean filterCluster(final MCODEGraph clusterGraph) {
    private boolean filterCluster(final McodeGraph clusterGraph) {
        if (clusterGraph == null) {
            return true;
        }

        // filter if the cluster does not satisfy the user specified k-core
//        MCODEGraph kCore = getKCore(clusterGraph, params.getKCore());
        McodeGraph kCore = getKCore(clusterGraph, params.getKCore());

        return kCore == null;
    }

    /**
     * Gives the cluster a haircut (removed singly connected nodes by taking a
     * 2-core)
     *
     * @param clusterGraph The cluster network
     * @param cluster The cluster node ID list (in the original graph)
     * @return true
     */
//    private boolean haircutCluster(final MCODEGraph clusterGraph, final List<Long> cluster) {
    private boolean haircutCluster(final McodeGraph clusterGraph, final List<Long> cluster) {
        // get 2-core
//        final MCODEGraph kCore = getKCore(clusterGraph, 2);
        final McodeGraph kCore = getKCore(clusterGraph, 2);

        if (kCore != null) {
            // clear the cluster and add all 2-core nodes back into it
            cluster.clear();

            // must add back the nodes in a way that preserves node indices
//            for (final CyNode n : kCore.getNodeList()) {
            for (final Node n : kCore.getNodeList()) {
//                cluster.add(n.getSUID());
                cluster.add((long) McodeClusteringAlgorithm.classInstance.networkNodes.indexOf(n));
            }
        }

        return true;
    }

    /**
     * Calculate the density of a network The density is defined as the number
     * of edges/the number of possible edges
     *
     * @param graph The input graph to calculate the density of
     * @param includeLoops Include the possibility of loops when determining the
     * number of possible edges.
     * @return The density of the network
     */
//    private double calcDensity(final MCODEGraph graph, final boolean includeLoops) {
    private double calcDensity(final McodeGraph graph, final boolean includeLoops) {
        String callerID = "MCODEAlgorithm.calcDensity";

        if (graph == null) {
//            logger.error("In " + callerID + ": network was null.");
            return (-1.0);
        }

        int nodeCount = graph.getNodeCount();
        int actualEdgeNum = getMergedEdgeCount(graph, includeLoops);
        int possibleEdgeNum = 0;

        if (includeLoops) {
            possibleEdgeNum = (nodeCount * (nodeCount + 1)) / 2;
        } else {
            possibleEdgeNum = (nodeCount * (nodeCount - 1)) / 2;
        }

        double density = possibleEdgeNum != 0 ? ((double) actualEdgeNum / (double) possibleEdgeNum) : 0;

        return density;
    }

//    private int getMergedEdgeCount(final MCODEGraph graph, final boolean includeLoops) {
    private int getMergedEdgeCount(final McodeGraph graph, final boolean includeLoops) {
        Set<String> suidPairs = new HashSet<String>();

//        for (CyEdge e : graph.getEdgeList()) {
        for (Edge e : graph.getEdgeList()) {
//            Long id1 = e.getSource().getSUID();
            Long id1 = (long) McodeClusteringAlgorithm.classInstance.networkNodes.indexOf(e.node1);
//            Long id2 = e.getTarget().getSUID();
            Long id2 = (long) McodeClusteringAlgorithm.classInstance.networkNodes.indexOf(e.node2);

            if (!includeLoops && id1 == id2) {
                continue;
            }

            String pair = id1 < id2 ? id1 + "_" + id2 : id2 + "_" + id1;
            suidPairs.add(pair);
        }

        return suidPairs.size();
    }

    /**
     * Find a k-core of a network. A k-core is a subgraph of minimum degree k
     *
     * @param inputGraph The input network
     * @param k The k of the k-core to find e.g. 4 will find a 4-core
     * @return Returns a subgraph with the core, if any was found at given k
     */
//    private MCODEGraph getKCore(final MCODEGraph inputGraph, final int k) {
    private McodeGraph getKCore(final McodeGraph inputGraph, final int k) {
        String callerID = "MCODEAlgorithm.getKCore";

        if (inputGraph == null) {
//            logger.error("In " + callerID + ": inputNetwork was null.");
            return null;
        }

        // filter all nodes with degree less than k until convergence
        boolean firstLoop = true;
//        MCODEGraph outputGraph = inputGraph;
        McodeGraph outputGraph = inputGraph;

        while (true && !cancelled) {
            int numDeleted = 0;
            final List<Long> alCoreNodeIndices = new ArrayList<Long>(outputGraph.getNodeCount());
//            final List<CyNode> nodes = outputGraph.getNodeList();
            final List<Node> nodes = outputGraph.getNodeList();

//            for (CyNode n : nodes) {
            for (Node n : nodes) {
//                int degree = outputGraph.getAdjacentEdgeList(n, CyEdge.Type.ANY).size();
                int degree = outputGraph.getAdjacentEdgeList(n).size();

                if (degree >= k) {
//                    alCoreNodeIndices.add(n.getSUID()); //contains all nodes with degree >= k
                    alCoreNodeIndices.add((long) McodeClusteringAlgorithm.classInstance.networkNodes.indexOf(n)); //contains all nodes with degree >= k
                } else {
                    numDeleted++;
                }
            }

            if (numDeleted > 0 || firstLoop) {
//                Set<CyNode> outputNodes = new HashSet<CyNode>();
                Set<Node> outputNodes = new HashSet<Node>();

                for (Long index : alCoreNodeIndices) {
//                    CyNode n = outputGraph.getNode(index);
                    Node n = outputGraph.getNode(index);
                    outputNodes.add(n);
                }

//                outputGraph = mcodeUtil.createGraph(outputGraph.getRootNetwork(), outputNodes);
                ArrayList<Node> rootNetworkNodes = new ArrayList<Node>();
                ArrayList<Edge> rootNetworkEdges = new ArrayList<Edge>();
                outputGraph.getRootNetwork(rootNetworkNodes, rootNetworkEdges);
                outputGraph = mcodeUtil.createGraph(rootNetworkNodes, rootNetworkEdges, outputNodes);

                if (outputGraph.getNodeCount() == 0) {
                    return null;
                }

                // Iterate again, but with a new k-core input graph...

                if (firstLoop) {
                    firstLoop = false;
                }
            } else {
                // stop the loop
                break;
            }
        }

        return outputGraph;
    }

    /**
     * Find the highest k-core in the input network.
     *
     * @param graph The input graph
     * @return Returns the k-value and the core as an Object array. The first
     * object is the highest k value i.e. objectArray[0] The second object is
     * the highest k-core as a MCODEGraph i.e. objectArray[1]
     */
//    private Object[] getHighestKCore(final MCODEGraph graph) {
    private Object[] getHighestKCore(final McodeGraph graph) {
        final String callerID = "MCODEAlgorithm.getHighestKCore";

        if (graph == null) {
//            logger.error("In " + callerID + ": network was null.");
            return (null);
        }

        int i = 1;
//        MCODEGraph curGraph = graph, prevGraph = null;
        McodeGraph curGraph = graph, prevGraph = null;

        while ((curGraph = getKCore(curGraph, i)) != null) {
            prevGraph = curGraph;
            i++;
        }

        Integer k = i - 1;
        final Object[] returnArray = new Object[2];
        returnArray[0] = k;
        returnArray[1] = prevGraph; //in the last iteration, curGraph is null (loop termination condition)

        return returnArray;
    }
}
