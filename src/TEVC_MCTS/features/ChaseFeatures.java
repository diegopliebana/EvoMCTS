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
import java.util.LinkedHashMap;

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

    private double up_down_angry, left_right_angry, up_down_scared, left_right_scared;

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
    public LinkedHashMap<String, Double> getFeatureVector()
    {
        LinkedHashMap<String, Double> features = new LinkedHashMap<String, Double>();
        features.put("up_down_angry:"+ANGRY, up_down_angry);
        features.put("left_right_angry:"+ANGRY, left_right_angry);
        features.put("up_down_scared:"+SCARED, up_down_scared);
        features.put("left_right_scared:"+SCARED, left_right_scared);
        return features;
    }


    @Override
    protected void calcFeatures(StateObservation stateObs)
    {
        if(x_arrNeig == null)
            initNeighbours(stateObs);

        avatarPos = stateObs.getAvatarPosition();

        ArrayList<Observation>[] npcPositions = stateObs.getNPCPositions(avatarPos);

        up_down_angry = 0;
        left_right_angry = 0;
        up_down_scared = 0;
        left_right_scared = 0;

        if(npcPositions != null)
        {
            for(int i = 0; i < npcPositions.length; ++i)
            {
                if(npcPositions[i].size()>0)
                {
                    Observation closestObs = npcPositions[i].get(0);
                    if(closestObs.itype == ANGRY)
                    {
                        Types.ACTIONS act = astarActionThatMinimizes(avatarPos, closestObs.position, block_size, false);
                        if(act == Types.ACTIONS.ACTION_UP) {up_down_angry = 1;}
                        if(act == Types.ACTIONS.ACTION_DOWN) {up_down_angry = -1;}
                        if(act == Types.ACTIONS.ACTION_LEFT) {left_right_angry = 1;}
                        if(act == Types.ACTIONS.ACTION_RIGHT) {left_right_angry = -1;}

                    }else if(closestObs.itype == SCARED)
                    {
                        Types.ACTIONS act = astarActionThatMinimizes(avatarPos, closestObs.position, block_size, false);
                        if(act == Types.ACTIONS.ACTION_UP) {up_down_scared = 1;}
                        if(act == Types.ACTIONS.ACTION_DOWN) {up_down_scared = -1;}
                        if(act == Types.ACTIONS.ACTION_LEFT) {left_right_scared = 1;}
                        if(act == Types.ACTIONS.ACTION_RIGHT) {left_right_scared = -1;}
                    }
                }
            }
        }

    }

    public double valueFunction(StateObservation stateObs) {

        boolean gameOver = stateObs.isGameOver();
        Types.WINNER win = stateObs.getGameWinner();
        double rawScore = stateObs.getGameScore();

        if(gameOver && win == Types.WINNER.PLAYER_LOSES)
            //return HUGE_NEGATIVE;
            rawScore += HUGE_NEGATIVE;

        else if(gameOver && win == Types.WINNER.PLAYER_WINS)
            //return HUGE_POSITIVE;
            rawScore += HUGE_POSITIVE;

        return rawScore;
    }

    /*public double valueFunction(StateObservation stateObs)
    {
        boolean gameOver = stateObs.isGameOver();
        Types.WINNER win = stateObs.getGameWinner();
        double rawScore = stateObs.getGameScore();

        if(gameOver && win == Types.WINNER.PLAYER_LOSES)
            return (rawScore+HUGE_NEGATIVE);

        if(gameOver && win == Types.WINNER.PLAYER_WINS)
            return (rawScore+HUGE_POSITIVE);

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
    }*/

    @Override
    public double[] getHandTunedWeights(StateObservation stateObs) {
        //Four actions, 2 features  (distance to angry, distance to scared)
        //These are constant because it is always good to increase distance with
        //angry goats and decrease it with scared ones.
        return new double[]{ 0, -2, 0, 1,
                             0, 2, 0, -1,
                             -2,0, 1, 0,
                             2, 0, -1,0};
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
