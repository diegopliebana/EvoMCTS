package TEVC_MCTS.features;

import TEVC_MCTS.pathfinder.Astar;
import TEVC_MCTS.pathfinder.Node;
import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import tools.Vector2d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by diego on 10/03/14.
 * Features for the game Chase:
 * [VGDLRegistry] wall => 0
 * [VGDLRegistry] avatar => 1
 * [VGDLRegistry] carcass => 2
 * [VGDLRegistry] goat => 3
 * [VGDLRegistry] angry => 4
 * [VGDLRegistry] scared => 5
 */
public class ChaseFeatures extends NavFeatureSource
{

    private static final double HUGE_NEGATIVE = -10000000.0;
    private static final double HUGE_POSITIVE =  10000000.0;
    private int WALL = 0, ANGRY = 4, SCARED = 5;

    private Vector2d avatarPos;

    private double distanceToScared;
    private double distanceToAngry;

    private HashMap<Integer, Double> npcDist2First;
    protected ArrayList<Observation> grid[][];
    protected int block_size;
    protected double maxDist;


    public ChaseFeatures(StateObservation stateObs)
    {
        npcDist2First = new HashMap<Integer, Double>();
        validAstarStatic = new ArrayList<Integer>();
        astar = new Astar(this);

        block_size = stateObs.getBlockSize();
        grid = stateObs.getObservationGrid();
        maxDist = grid.length * grid[0].length;

        calcFeatures(stateObs);
    }

    @Override
    public HashMap<String, Double> getFeatureVector()
    {
        HashMap<String, Double> features = new HashMap<String, Double>();
        features.put("angry:"+ANGRY, distanceToAngry);
        features.put("scared:"+SCARED, distanceToScared);
        return features;
    }


    @Override
    protected void calcFeatures(StateObservation stateObs)
    {
        if(x_arrNeig == null)
            initNeighbours(stateObs);

        avatarPos = stateObs.getAvatarPosition();

        ArrayList<Observation>[] npcPositions = stateObs.getNPCPositions(avatarPos);


        distanceToAngry = -1;
        distanceToScared = -1;

        if(npcPositions != null)
        {
            for(int i = 0; i < npcPositions.length; ++i)
            {
                if(npcPositions[i].size()>0)
                {
                    Observation closestObs = npcPositions[i].get(0);
                    if(closestObs.itype == ANGRY)
                    {
                        distanceToAngry = astarDistanceFromTo(avatarPos, closestObs.position, block_size, false);
                    }else if(closestObs.itype == SCARED)
                    {
                        distanceToScared = astarDistanceFromTo(avatarPos, closestObs.position, block_size, false);
                    }
                }
            }
        }

    }

    public double valueFunction(StateObservation stateObs)
    {
        boolean gameOver = stateObs.isGameOver();
        Types.WINNER win = stateObs.getGameWinner();

        if(gameOver && win == Types.WINNER.PLAYER_LOSES)
            return HUGE_NEGATIVE;

        if(gameOver && win == Types.WINNER.PLAYER_WINS)
            return HUGE_POSITIVE;

        avatarPos = stateObs.getAvatarPosition();
        ArrayList<Observation>[] npcPositions = stateObs.getNPCPositions(avatarPos);
        double distanceToAngryScore=0, distanceToScaredScore=0;

        if(npcPositions != null)
        {
            for(int i = 0; i < npcPositions.length; ++i)
            {
                if(npcPositions[i].size()>0)
                {
                    Observation closestObs = npcPositions[i].get(0);
                    if(closestObs.itype == ANGRY)
                    {
                        distanceToAngryScore = astarDistanceFromTo(avatarPos, closestObs.position, block_size, false);
                    }else if(closestObs.itype == SCARED)
                    {
                        distanceToScaredScore = astarDistanceFromTo(avatarPos, closestObs.position, block_size, false);
                    }
                }
            }
        }

        double score = (maxDist - distanceToScaredScore) / maxDist;
        score += (distanceToAngryScore) / maxDist;
        score += stateObs.getGameScore();
        return score;
    }

    @Override
    public double[] getHandTunedWeights(StateObservation stateObs) {
        //Four actions, 2 features  (distance to angry, distance to scared)
        //These are constant because it is always good to increase distance with
        //angry goats and decrease it with scared ones.
        return new double[]{-1,1,-1,1,-1,1,-1,1};
    }


    protected void initNeighbours(StateObservation stObs)
    {
        //up, down, left, right
        x_arrNeig = new int[]{0,    0,    -1,    1};
        y_arrNeig = new int[]{-1,   1,     0,    0};
    }

    @Override
    protected Node extractNonObstacle(int x, int y)
    {
        if(x < 0 || y < 0 || x >= grid.length || y >= grid[x].length)
            return null;

        int numObs = grid[x][y].size();
        boolean isObstacle = false;
        for(int i = 0; !isObstacle && i < numObs; ++i)
        {
            Observation obs = grid[x][y].get(i);

            if(obs.itype == WALL || obs.itype == ANGRY)
            {
                isObstacle = true;
            }
        }

        if(isObstacle)
            return null;

        return new Node(new Vector2d(x,y));

    }



}
