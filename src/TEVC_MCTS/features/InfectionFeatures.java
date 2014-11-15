package TEVC_MCTS.features;

import TEVC_MCTS.pathfinder.Astar;
import TEVC_MCTS.pathfinder.Node;
import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import tools.Vector2d;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by diego on 10/03/14.
 * Features for the game Chase:
 [VGDLRegistry] wall => 0
 [VGDLRegistry] avatar => 1
 [VGDLRegistry] sword => 2
 [VGDLRegistry] entrance => 3
 [VGDLRegistry] virus => 4
 [VGDLRegistry] moving => 5
 [VGDLRegistry] normal => 6
 [VGDLRegistry] carrier => 7
 [VGDLRegistry] npc => 8
 [VGDLRegistry] host => 9
 [VGDLRegistry] infected => 10
 [VGDLRegistry] guardian => 11
 */
public class InfectionFeatures extends NavFeatureSource
{

    private static final double HUGE_NEGATIVE = -10000000.0;
    private static final double HUGE_POSITIVE =  10000000.0;
    private int WALL = 0, CARRIER = 7, INFECTED = 10, HOST = 9, GUARDIAN = 11;

    private Vector2d avatarPos;

    private double numInfected;
    private double numHost;
    private double numGuardians;

    private double distanceToInfected;
    private double distanceToHost;
    private double distanceToGuardian;

    private boolean carrier;

    private HashMap<Integer, Double> npcDist2First;
    protected ArrayList<Observation> grid[][];
    protected int block_size;
    protected double maxDist;


    public InfectionFeatures(StateObservation stateObs)
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

        features.put("infected:"+INFECTED, distanceToInfected);
        features.put("host:"+HOST, distanceToHost);
        features.put("guardian:"+GUARDIAN, distanceToGuardian);

        return features;
    }


    @Override
    protected void calcFeatures(StateObservation stateObs)
    {
        if(x_arrNeig == null)
            initNeighbours(stateObs);

        avatarPos = stateObs.getAvatarPosition();

        ArrayList<Observation>[] npcPositions = stateObs.getNPCPositions(avatarPos);


        distanceToInfected = -1;
        distanceToHost = -1;
        distanceToGuardian = -1;

        if(npcPositions != null)
        {
            for(int i = 0; i < npcPositions.length; ++i)
            {
                if(npcPositions[i].size()>0)
                {
                    Observation closestObs = npcPositions[i].get(0);
                    if(closestObs.itype == INFECTED)
                    {
                        distanceToInfected = astarDistanceFromTo(avatarPos, closestObs.position, block_size, false);
                        numInfected = npcPositions[i].size();
                    }else if(closestObs.itype == HOST)
                    {
                        distanceToHost = astarDistanceFromTo(avatarPos, closestObs.position, block_size, false);
                        numHost = npcPositions[i].size();
                    }else if(closestObs.itype == GUARDIAN)
                    {
                        distanceToGuardian = astarDistanceFromTo(avatarPos, closestObs.position, block_size, false);
                        numGuardians = npcPositions[i].size();
                    }
                }
            }
        }

    }

    private void amICarrier(StateObservation stateObs)
    {
        int x = (int)(avatarPos.x/stateObs.getBlockSize());
        int y = (int)(avatarPos.y/stateObs.getBlockSize());

        carrier = false;
        for(Observation obs : grid[x][y])
        {
            if(obs.itype == CARRIER)
                carrier = true;
        }
    }


    public double valueFunction(StateObservation stateObs) {

        boolean gameOver = stateObs.isGameOver();
        Types.WINNER win = stateObs.getGameWinner();
        double rawScore = stateObs.getGameScore();

        if(gameOver && win == Types.WINNER.PLAYER_LOSES)
            return HUGE_NEGATIVE;

        if(gameOver && win == Types.WINNER.PLAYER_WINS)
            return HUGE_POSITIVE;

        return rawScore;
    }

    /*public double valueFunction(StateObservation stateObs)
    {
        boolean gameOver = stateObs.isGameOver();
        Types.WINNER win = stateObs.getGameWinner();

        if(gameOver && win == Types.WINNER.PLAYER_LOSES)
            return HUGE_NEGATIVE;

        if(gameOver && win == Types.WINNER.PLAYER_WINS)
            return HUGE_POSITIVE;

        avatarPos = stateObs.getAvatarPosition();
        amICarrier(stateObs);
        ArrayList<Observation>[] npcPositions = stateObs.getNPCPositions(avatarPos);

        double distanceToInfectedScore=0, distanceToHostScore=0, distanceToGuardianScore=0;
        int numInfectedScore=0, numHostScore=0, numGuardianScore=0;

        if(npcPositions != null)
        {
            for(int i = 0; i < npcPositions.length; ++i)
            {
                if(npcPositions[i].size()>0)
                {
                    Observation closestObs = npcPositions[i].get(0);
                    if(closestObs.itype == INFECTED)
                    {
                        distanceToInfectedScore = astarDistanceFromTo(avatarPos, closestObs.position, block_size, false);
                        numInfectedScore = npcPositions[i].size();
                    }else if(closestObs.itype == HOST)
                    {
                        distanceToHostScore = astarDistanceFromTo(avatarPos, closestObs.position, block_size, false);
                        numHostScore = npcPositions[i].size();
                    }else if(closestObs.itype == GUARDIAN)
                    {
                        distanceToGuardianScore = astarDistanceFromTo(avatarPos, closestObs.position, block_size, false);
                        numGuardianScore = npcPositions[i].size();
                    }
                }
            }
        }

        double numNPCs = numGuardianScore + numHostScore + numInfectedScore;

        double score = 0.0;
        if(carrier)
        {
            score += (maxDist - distanceToHostScore) / maxDist;
            score += (distanceToGuardianScore) / maxDist;
        }else{
            score += (maxDist - distanceToInfectedScore) / maxDist;
            score += (maxDist - distanceToGuardianScore) / maxDist;
        }

        score += (numInfectedScore/numNPCs);

        score += stateObs.getGameScore();

        return score;
    }*/

    @Override
    public double[] getHandTunedWeights(StateObservation stateObs) {


        amICarrier(stateObs);

        if(carrier)
        {
            //5 actions (USE+moves), 3 features
            return new double[]{0,0,0,0,0,
                    0,1,1,1,1,
                    1,-1,-1,-1,-1};
        }else{
            //5 actions (USE+moves), 3 features
            return new double[]{0,1,1,1,1,
                    0,0,0,0,0,
                    1,1,1,1,1};
        }

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

            if(obs.itype == WALL || obs.itype == GUARDIAN)
            {
                isObstacle = true;
            }
        }

        if(isObstacle)
            return null;

        return new Node(new Vector2d(x,y));

    }



}
