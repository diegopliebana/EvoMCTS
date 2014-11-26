package TEVC_MCTS.features;

import TEVC_MCTS.pathfinder.Astar;
import TEVC_MCTS.pathfinder.Node;
import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import tools.Vector2d;

import java.util.ArrayList;
import java.util.HashMap;
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
public class CircleFeatures extends NavFeatureSource
{

    private static final double HUGE_NEGATIVE = -10000000.0;
    private static final double HUGE_POSITIVE =  10000000.0;
    private int WALL = 0, WIN = 2, LOSE = 3;

    private Vector2d avatarPos;

    private double distanceToWin;
    private double distanceToLose;

    protected ArrayList<Observation> grid[][];
    protected int block_size;
    protected double maxDist;


    public CircleFeatures(StateObservation stateObs)
    {
        validAstarStatic = new ArrayList<Integer>();
        astar = new Astar(this);
        block_size = stateObs.getBlockSize();
        grid = stateObs.getObservationGrid();
        maxDist = (grid.length * grid[0].length) * block_size;

        calcFeatures(stateObs);
    }

    @Override
    public LinkedHashMap<String, Double> getFeatureVector()
    {
        LinkedHashMap<String, Double> features = new LinkedHashMap<String, Double>();
        features.put("angry:"+WIN, distanceToWin);
        //features.put("scared:"+LOSE, distanceToLose);
        return features;
    }


    @Override
    protected void calcFeatures(StateObservation stateObs)
    {
        if(x_arrNeig == null)
            initNeighbours(stateObs);

        avatarPos = stateObs.getAvatarPosition();
        ArrayList<Observation>[] portalPositions = stateObs.getPortalsPositions(avatarPos);
        distanceToWin = -1;
        distanceToLose = -1;

        if(portalPositions != null)
        {
            for(int i = 0; i < portalPositions.length; ++i)
            {
                if(portalPositions[i].size()>0)
                {
                    Observation closestObs = portalPositions[i].get(0);
                    if(closestObs.itype == WIN)
                    {
                        //distanceToWin = maxDist - avatarPos.dist(closestObs.position);
                        distanceToWin =  1 - (avatarPos.dist(closestObs.position)/maxDist);
                    }else if(closestObs.itype == LOSE)
                    {
                        distanceToLose =  1 - (avatarPos.dist(closestObs.position)/maxDist);
                        //distanceToLose = maxDist - avatarPos.dist(closestObs.position);
                    }
                }
            }
        }

    }

    public double valueFunction(StateObservation stateObs)
    {
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

    private Vector2d getClosestWinPosition(StateObservation stateObs)
    {
        ArrayList<Observation>[] portalPositions = stateObs.getPortalsPositions(avatarPos);

        if(portalPositions != null) for(int i = 0; i < portalPositions.length; ++i)
        {
            if(portalPositions[i].size()>0)
            {
                Observation closestObs = portalPositions[i].get(0);
                if(closestObs.itype == WIN)
                {
                    return closestObs.position;
                }
            }
        }
        return null;
    }

    @Override
    public double[] getHandTunedWeights(StateObservation stateObs) {
        //Four actions, 2 features  (distance to win, distance to lose)
        //return new double[]{1,-1,1,-1,1,-1,1,-1};

        double left = -1;
        double up = 1;
        double right = 1, down = -1;

        Vector2d avatarPos = stateObs.getAvatarPosition();

        if(avatarPos.x > 700)
        {
            //right edge
            right = -1;
        }
        if(avatarPos.y < 50)
        {
            //upper edge
            up = -1;
        }

        //return new double[]{left, left, right, right, down, down,up,up};
        return new double[]{left,right,down,up};
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

            if(obs.itype == WALL)
            {
                isObstacle = true;
            }
        }

        if(isObstacle)
            return null;

        return new Node(new Vector2d(x,y));
    }



}
