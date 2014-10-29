package FastEvoMCTS;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import tools.Vector2d;
import FastEvoMCTS.pathfinder.Astar;
import FastEvoMCTS.pathfinder.Navigable;
import FastEvoMCTS.pathfinder.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by diego on 10/03/14.
 */
public class FeatureExtraction implements Navigable
{
    private Vector2d avatarPos;
    private Vector2d[] closestNPC, closestRes, closestPort, closestMov, closestFix;
    private HashMap<Integer, Double> npcDist2First;
    private HashMap<Integer, Double> resDist2First;
    private HashMap<Integer, Double> portDist2First;
    private HashMap<Integer, Double> movDist2First;
    private HashMap<Integer, Double> fixDist2First;
    private HashMap<Integer, Integer> resources;
    protected ArrayList<Observation> grid[][];
    public ArrayList<Integer> validAstarStatic;
    private Memory memory;
    public Astar astar;
    protected int block_size;

    int[] x_arrNeig = null;
    int[] y_arrNeig = null;

    public FeatureExtraction()
    {
        npcDist2First = new HashMap<Integer, Double>();
        resDist2First = new HashMap<Integer, Double>();
        portDist2First = new HashMap<Integer, Double>();
        movDist2First = new HashMap<Integer, Double>();
        fixDist2First = new HashMap<Integer, Double>();
        resources = new HashMap<Integer, Integer>();
        validAstarStatic = new ArrayList<Integer>();
        astar = new Astar(this);
    }

    public FeatureExtraction(StateObservation stateObs)
    {
        npcDist2First = new HashMap<Integer, Double>();
        resDist2First = new HashMap<Integer, Double>();
        portDist2First = new HashMap<Integer, Double>();
        movDist2First = new HashMap<Integer, Double>();
        fixDist2First = new HashMap<Integer, Double>();
        resources = new HashMap<Integer, Integer>();
        validAstarStatic = new ArrayList<Integer>();
        astar = new Astar(this);

        calcFeatures(stateObs);
    }


    public void setMemory(Memory memory)
    {
        this.memory = memory;
    }

    private void calcFeatures(StateObservation stateObs)
    {
        //game related
        //double score = stateObs.getGameScore();
        //int gameTick = stateObs.getGameTick();
        //Types.WINNER win = stateObs.getGameWinner();
        //boolean gameOver = stateObs.isGameOver();

        //avatar related
        avatarPos = stateObs.getAvatarPosition();
        //double avatarSpeed = stateObs.getAvatarSpeed();
        //Vector2d avatarOrientation = stateObs.getAvatarOrientation();
        resources = stateObs.getAvatarResources();
        grid = stateObs.getObservationGrid();
        block_size = stateObs.getBlockSize();

        if(x_arrNeig == null)
        {
            //THIS IS A BIT OF A HACK, WOULDN'T WORK WITH OTHER ACTION SETS:
            ArrayList<Types.ACTIONS> actions = stateObs.getAvailableActions();
            if(actions.size() == 3)
            {
                //left, right
                x_arrNeig = new int[]{-1, 1};
                y_arrNeig = new int[]{0,  0};
            }else
            {
                //up, down, left, right
                x_arrNeig = new int[]{0,    0,    -1,    1};
                y_arrNeig = new int[]{-1,   1,     0,    0};
            }
        }


        //external entities.
        ArrayList<Observation>[] fixedPositions = stateObs.getImmovablePositions(avatarPos);
        ArrayList<Observation>[] movingPositions = stateObs.getMovablePositions(avatarPos);
        ArrayList<Observation>[] npcPositions = stateObs.getNPCPositions(avatarPos);
        ArrayList<Observation>[] resourcesPositions = stateObs.getResourcesPositions(avatarPos);
        ArrayList<Observation>[] portalPositions = stateObs.getPortalsPositions(avatarPos);

        //Filtering fixed positions:
        if(fixedPositions != null) for(int i = 0; i < fixedPositions.length; ++i)
        {
            int numObservations = fixedPositions[i].size();
            if(numObservations > 0)
            {
                int type = fixedPositions[i].get(0).itype;
                double meanScore = memory.getTotalMeanScore(type);

                //if(meanScore > 0 || numObservations <= 5)
                if(numObservations <= 5)
                {
                    if(!validAstarStatic.contains(type))
                        validAstarStatic.add(type);

                }else{
                    fixedPositions[i].clear();
                }
            }
        }

        //Building FastEvoMCTS:

        if(npcPositions != null)  closestNPC = new Vector2d[npcPositions.length];
        if(resourcesPositions != null)  closestRes = new Vector2d[resourcesPositions.length];
        if(portalPositions != null)  closestPort = new Vector2d[portalPositions.length];
        if(fixedPositions != null)  closestFix = new Vector2d[fixedPositions.length];//new Vector2d[numFixed];
        if(movingPositions != null)  closestMov = new Vector2d[movingPositions.length];


        if(Config.USE_ASTAR)
        {
            astarDistancesToFirst(npcPositions, closestNPC, npcDist2First);
            astarDistancesToFirst(resourcesPositions, closestRes, resDist2First);
            astarDistancesToFirst(portalPositions, closestPort, portDist2First);
            astarDistancesToFirst(movingPositions, closestMov, movDist2First);
            astarDistancesToFirst(fixedPositions, closestFix, fixDist2First);
        }else{
            euqDistancesToFirst(npcPositions, closestNPC, npcDist2First);
            euqDistancesToFirst(resourcesPositions, closestRes, resDist2First);
            euqDistancesToFirst(portalPositions, closestPort, portDist2First);
            euqDistancesToFirst(movingPositions, closestMov, movDist2First);
        }
    }

    private void euqDistancesToFirst(ArrayList<Observation>[] positions, Vector2d[] closest, HashMap<Integer, Double> distances)
    {
        if(positions == null)
            return; //no positions.

        int numTypes = positions.length;

        //Set default maximum distance (some sprite types might not appear again).
        Iterator<Integer> keys = distances.keySet().iterator();
        while(keys.hasNext())
            distances.put(keys.next(), Double.MAX_VALUE);

        //For all observations found:
        for(int i = 0; i < numTypes; ++i)
        {
            if(positions[i].size() > 0)
            {
                Observation firstObs = positions[i].get(0);

                //its attributes
                int thisType = firstObs.itype;
                double distance = firstObs.sqDist;
                closest[i] = firstObs.position;

                //insert them in the treemap.
                distances.put(thisType, distance);
            }
        }

    }


    private void astarDistancesToFirst(ArrayList<Observation>[] positions, Vector2d[] closest, HashMap<Integer, Double> distances)
    {
        if(positions == null)
            return; //no positions.

        int numTypes = positions.length;

        //Set default maximum distance (some sprite types might not appear again).
        Iterator<Integer> keys = distances.keySet().iterator();
        while(keys.hasNext())
            distances.put(keys.next(), Double.MAX_VALUE);

        //For all observations found:
        for(int i = 0; i < numTypes; ++i)
        {
            if(positions[i].size() > 0)
            {
                Observation firstObs = positions[i].get(0);

                //its attributes
                closest[i] = firstObs.position;
                ArrayList<Node> pathToObservation = null;

                boolean traversable = memory.isTraversable(firstObs.itype);
                pathToObservation = getPath(avatarPos, firstObs.position, block_size, !traversable);

                double distance;
                if(pathToObservation != null)
                {
                    distance = pathToObservation.size();
                    //insert them in the treemap.
                    distances.put(firstObs.itype, distance*block_size);
                }else{
                   /* System.out.println("Path to observation null: (" +
                            avatarPos.x/block_size + "," + avatarPos.y/block_size + ") to (" +
                            firstObs.position.x/block_size + "," + firstObs.position.y/block_size + ") bs: "
                     + block_size); */
                }
            }
        }

    }

    public HashMap<String, Double> getFeatureVector()
    {
        HashMap<String, Double> features = new HashMap<String, Double>();
        if(npcDist2First != null)  addFeaturesFromMap(npcDist2First, features, "npc:");
        if(resDist2First != null)  addFeaturesFromMap(resDist2First, features, "res:");
        if(portDist2First != null) addFeaturesFromMap(portDist2First, features, "por:");
        if(fixDist2First != null) addFeaturesFromMap(fixDist2First, features, "fix:");
        if(movDist2First != null) addFeaturesFromMap(movDist2First, features, "mov:");
        if(resources != null) addResourcesToFeatures(features, "ores:");
        return features;
    }

    private void addResourcesToFeatures(HashMap<String, Double> features, String prefix)
    {
        Iterator<Integer> keys = resources.keySet().iterator();
        while(keys.hasNext())
        {
            int itype = keys.next();
            String hashKey = prefix + itype;
            features.put(hashKey, (double) resources.get(itype));
        }

    }

    private void addFeaturesFromMap(HashMap<Integer, Double> map, HashMap<String, Double> features, String prefix)
    {
        Iterator<Integer> keys = map.keySet().iterator();
        while(keys.hasNext())
        {
            int itype = keys.next();
            String hashKey = prefix + itype;
            features.put(hashKey, map.get(itype));
        }

    }

    public HashMap<String, Double> getFeatureVector(StateObservation state)
    {
        calcFeatures(state);
        return getFeatureVector();
    }

    public double[] getFeatureVectorAsArray(StateObservation state)
    {
        calcFeatures(state);
        return getFeatureVectorAsArray();
    }

    public double[] getFeatureVectorAsArray()
    {
        HashMap<String, Double> featuresMap = getFeatureVector();
        double[] features = new double[featuresMap.size()];

        Iterator<Double> itF = featuresMap.values().iterator();
        int i = 0;
        while(itF.hasNext())
            features[i++] = itF.next();

        return features;
    }

    public String[] getFeatureVectorKeys(StateObservation state)
    {
        calcFeatures(state);
        return getFeatureVectorKeys();
    }

    public String[] getFeatureVectorKeys()
    {
        HashMap<String, Double> featuresMap = getFeatureVector();
        String[] featureNames = new String[featuresMap.size()];
        featuresMap.keySet().toArray(featureNames);

        return featureNames;
    }

    @Override
    public ArrayList<Node> getPath(Vector2d origin, Vector2d destination, int block_size, boolean toNeighbour) {
        Vector2d startPos = new Vector2d((int) (origin.x / block_size), (int) (origin.y / block_size));
        Vector2d goalPos = new Vector2d((int) (destination.x / block_size), (int) (destination.y / block_size));

        Node start = new Node(startPos);
        Node goal = new Node(goalPos);

        return astar.findPath(start, goal, toNeighbour);
    }


    @Override
    public ArrayList<Node> getNeighbours(Node node) {

        ArrayList<Node> neighbours = new ArrayList<Node>();
        int x = (int) (node.position.x);
        int y = (int) (node.position.y);

        for(int i = 0; i < x_arrNeig.length; ++i)
        {
            Node neig = extractNonStatic(x+x_arrNeig[i],y+y_arrNeig[i]);  //Change here to use memory for pathfinding.
            if(neig != null) neighbours.add(neig);
        }

        return neighbours;
    }

    @Override
    public void setMoveDir(Node node, Node pre) {

        Types.ACTIONS action = Types.ACTIONS.ACTION_NIL;
        if(pre.position.x < node.position.x)
            action = Types.ACTIONS.ACTION_RIGHT;
        if(pre.position.x > node.position.x)
            action = Types.ACTIONS.ACTION_LEFT;

        if(pre.position.y < node.position.y)
            action = Types.ACTIONS.ACTION_DOWN;
        if(pre.position.y > node.position.y)
            action = Types.ACTIONS.ACTION_UP;

        node.comingFrom = action;
    }

    private Node extractNonStatic(int x, int y)
    {
        if(x < 0 || y < 0 || x >= grid.length || y >= grid[x].length)
            return null;

        int numObs = grid[x][y].size();
        boolean isObstacle = false;
        for(int i = 0; !isObstacle && i < numObs; ++i)
        {
            Observation obs = grid[x][y].get(i);


            //if(!memory.isTraversable(obs.itype))


            if( (obs.category == Types.TYPE_STATIC)
                    && (!validAstarStatic.contains(obs.itype))
                    && !memory.isTraversable(obs.itype) )
            {
                isObstacle = true;
            }

            if ( (obs.category != Types.TYPE_STATIC) && !memory.isTraversable(obs.itype) )
            {
                isObstacle = true;
            }

            if(memory.getPercLoses(obs.itype) > memory.getPercWins(obs.itype))
            {
                isObstacle = true;
            }

        }

        if(isObstacle)
            return null;

        return new Node(new Vector2d(x,y));

    }


    @Override
    public boolean isContiguous(Node current, Node target)
    {
        int x = (int) (current.position.x);
        int y = (int) (current.position.y);

        for(int i = 0; i < x_arrNeig.length; ++i)
        {
            int nextX = x+x_arrNeig[i];
            int nextY = y+y_arrNeig[i];

            Node newNode = new Node(new Vector2d(nextX,nextY));

            if(newNode.equals(target))
                return true;
        }

        return false;
    }

    public void emptyAstarCache()
    {
        astar.emptyCache();
    }


}
