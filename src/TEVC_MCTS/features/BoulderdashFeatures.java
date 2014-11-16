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
 [VGDLRegistry] dirt => 3
 [VGDLRegistry] exitdoor => 4
 [VGDLRegistry] diamond => 5
 [VGDLRegistry] boulder => 6
 [VGDLRegistry] moving => 7
 [VGDLRegistry] enemy => 8
 [VGDLRegistry] crab => 9
 [VGDLRegistry] butterfly => 10
 */
public class BoulderdashFeatures extends NavFeatureSource
{

    private static final double HUGE_NEGATIVE = -10000000.0;
    private static final double HUGE_POSITIVE =  10000000.0;
    private int WALL = 0, CRAB = 9, BUTTERFLY = 10, DIAMOND = 5, EXITDOOR = 4, BOULDER = 6;

    private Vector2d avatarPos;

    private double numDiamonds;

    private double distanceToDiamond;
    private double distanceToExit;
    private double distanceToCrab;
    private double distanceToButterfly;


    protected ArrayList<Observation> grid[][];
    protected int block_size;
    protected double maxDist;


    public BoulderdashFeatures(StateObservation stateObs)
    {
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

        features.put("diamond:"+DIAMOND, distanceToDiamond);
        features.put("exit:"+EXITDOOR, distanceToExit);
        features.put("butterfly:"+BUTTERFLY, distanceToCrab);
        features.put("crab:"+CRAB, distanceToButterfly);

        return features;
    }


    @Override
    protected void calcFeatures(StateObservation stateObs)
    {
        if(x_arrNeig == null)
            initNeighbours(stateObs);

        avatarPos = stateObs.getAvatarPosition();
        numDiamonds = getNumDiamonds(stateObs);

        ArrayList<Observation>[] npcPositions = stateObs.getNPCPositions(avatarPos);


        distanceToDiamond = -1;
        distanceToExit = -1;
        distanceToCrab = -1;
        distanceToButterfly = -1;

        if(npcPositions != null)
        {
            for(int i = 0; i < npcPositions.length; ++i)
            {
                if(npcPositions[i].size()>0)
                {
                    Observation closestObs = npcPositions[i].get(0);
                    if(closestObs.itype == DIAMOND)
                    {
                        distanceToDiamond = astarDistanceFromTo(avatarPos, closestObs.position, block_size, false);
                    }else if(closestObs.itype == EXITDOOR)
                    {
                        distanceToExit = astarDistanceFromTo(avatarPos, closestObs.position, block_size, false);
                    }else if(closestObs.itype == CRAB)
                    {
                        distanceToCrab = astarDistanceFromTo(avatarPos, closestObs.position, block_size, false);
                    }else if(closestObs.itype == BUTTERFLY)
                    {
                        distanceToButterfly = astarDistanceFromTo(avatarPos, closestObs.position, block_size, false);
                    }
                }
            }
        }

    }

    private int getNumDiamonds(StateObservation stateObs)
    {
        int numDiamonds = 0;
        if(stateObs.getAvatarResources() != null &&
                stateObs.getAvatarResources().containsKey(DIAMOND) )
            numDiamonds = stateObs.getAvatarResources().get(DIAMOND);

        return numDiamonds;
    }

    public double valueFunction(StateObservation stateObs)
    {
        boolean gameOver = stateObs.isGameOver();
        Types.WINNER win = stateObs.getGameWinner();
        double rawScore = stateObs.getGameScore();

        //if(gameOver && win == Types.WINNER.PLAYER_LOSES)
        //    return (rawScore + HUGE_NEGATIVE);

        if(gameOver && win == Types.WINNER.PLAYER_WINS)
            return (rawScore + HUGE_POSITIVE);

        avatarPos = stateObs.getAvatarPosition();
        ArrayList<Observation>[] npcPositions = stateObs.getNPCPositions(avatarPos);

        double distanceToDiamondScore=0, distanceToExitScore=0, distanceToCrabScore=0, distanceToButterflyScore=0;
        int numDiamondsScore = getNumDiamonds(stateObs);
        boolean canUseExit = (numDiamondsScore >= 10);
        double numDiamondsInGame = 0.0;

        if(npcPositions != null)
        {
            for(int i = 0; i < npcPositions.length; ++i)
            {
                if(npcPositions[i].size()>0)
                {
                    Observation closestObs = npcPositions[i].get(0);
                    if(closestObs.itype == CRAB)
                    {
                        distanceToCrabScore = astarDistanceFromTo(avatarPos, closestObs.position, block_size, false);
                    }else if(closestObs.itype == BUTTERFLY)
                    {
                        distanceToButterflyScore = astarDistanceFromTo(avatarPos, closestObs.position, block_size, false);
                    }
                }
            }
        }

        npcPositions = null;
        ArrayList<Observation>[] portalPositions = stateObs.getPortalsPositions(avatarPos);
        if(portalPositions != null)
        {
            for(int i = 0; i < portalPositions.length; ++i)
            {
                if(portalPositions[i].size()>0)
                {
                    Observation closestObs = portalPositions[i].get(0);
                    if(closestObs.itype == EXITDOOR)
                    {
                        distanceToExitScore = astarDistanceFromTo(avatarPos, closestObs.position, block_size, false);
                    }
                }
            }
        }

        portalPositions = null;
        ArrayList<Observation>[] resourcePositions = stateObs.getResourcesPositions(avatarPos);
        if(resourcePositions != null)
        {
            for(int i = 0; i < resourcePositions.length; ++i)
            {
                if(resourcePositions[i].size()>0)
                {
                    Observation closestObs = resourcePositions[i].get(0);
                    if(closestObs.itype == DIAMOND)
                    {
                        distanceToDiamondScore = astarDistanceFromTo(avatarPos, closestObs.position, block_size, false);
                        numDiamondsInGame =  resourcePositions[i].size() + numDiamondsScore;
                    }
                }
            }
        }

        double score = 0.0;
        if(canUseExit)
        {
            score += (maxDist - distanceToExitScore) / maxDist;
        }else{
            score += (maxDist - distanceToDiamondScore) / maxDist;
        }
        score += (distanceToCrabScore) / maxDist;
        score += (distanceToButterflyScore) / maxDist;

        if(numDiamondsInGame > 0)
            score += (numDiamondsScore/numDiamondsInGame);

        score += stateObs.getGameScore();

        return score;
    }

    @Override
    public double[] getHandTunedWeights(StateObservation stateObs) {

        int numDiamondsScore = getNumDiamonds(stateObs);
        boolean canUseExit = (numDiamondsScore >= 10);

        int like = 1, dislike = -1, dontcare = 0;

        if(canUseExit)
        {
            //5 actions (USE+moves), 4 features
            return new double[]{dontcare,dontcare,dontcare,dontcare,dontcare,
                    like,like,like,like,like,
                    like,dislike,dislike,dislike,dislike,
                    like,dislike,dislike,dislike,dislike};
        }else{
            //5 actions (USE+moves), 4 features
            return new double[]{like,like,like,like,like,
                    dontcare,dontcare,dontcare,dontcare,dontcare,
                    like,dislike,dislike,dislike,dislike,
                    like,dislike,dislike,dislike,dislike};
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

            if(obs.itype == WALL || obs.itype == BOULDER
            || obs.itype == CRAB  || obs.itype == BUTTERFLY)
            {
                isObstacle = true;
            }
        }

        if(isObstacle)
            return null;

        return new Node(new Vector2d(x,y));

    }



}
