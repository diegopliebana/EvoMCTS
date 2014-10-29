package controllers.pathfinder;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;
import FastEvoMCTS.pathfinder.Astar;
import FastEvoMCTS.pathfinder.Navigable;
import FastEvoMCTS.pathfinder.Node;

import java.awt.*;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: ssamot
 * Date: 14/11/13
 * Time: 21:45
 * This is a Java port from Tom Schaul's VGDL - https://github.com/schaul/py-vgdl
 */
public class Agent extends AbstractPlayer implements Navigable {


    /**
     * Observation grid.
     */
    protected ArrayList<Observation> grid[][];

    /**
     * Pathfinder
     */
    protected Astar astar;


    /**
     * Random generator for the agent.
     */
    protected Random randomGenerator;


    /**
     * block size
     */
    protected int block_size;


    /**
     * Public constructor with state observation and time due.
     * @param so state observation of the current game.
     * @param elapsedTimer Timer for the controller creation.
     */
    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer)
    {
        astar = new Astar(this);
        randomGenerator = new Random();
        grid = so.getObservationGrid();
        block_size = so.getBlockSize();
    }


    /**
     * Picks an action. This function is called every game step to request an
     * action from the player.
     * @param stateObs Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
     * @return An action for the current state
     */
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {

        grid = stateObs.getObservationGrid();

        ArrayList<Observation>[] portalPositions = stateObs.getPortalsPositions();
        Observation firstPortal = portalPositions[0].get(0);

        ArrayList<Node> pathToFirstPortal = getPath(stateObs.getAvatarPosition(), firstPortal.position, block_size, true);


        Types.ACTIONS action = null;
        if(pathToFirstPortal != null && pathToFirstPortal.size() > 0)
        {
            Node firstNode = pathToFirstPortal.get(0);
            double d = Double.POSITIVE_INFINITY;

            ArrayList<Types.ACTIONS> actions = stateObs.getAvailableActions();
            for(Types.ACTIONS act : actions)
            {
                StateObservation stCopy = stateObs.copy();
                stCopy.advance(act);

                Vector2d avatarPos = new Vector2d(stCopy.getAvatarPosition().x / block_size,
                                                  stCopy.getAvatarPosition().y / block_size);
                Vector2d nextPos = new Vector2d(firstNode.position.x, firstNode.position.y);

                if((avatarPos.x < 0 || avatarPos.y < 0) &&
                   (stCopy.getGameWinner() != Types.WINNER.PLAYER_LOSES))
                {
                    return act;
                }

                if(avatarPos.equals(nextPos))
                {
                    return act;
                }
            }

        }

        ArrayList<Types.ACTIONS> actions = stateObs.getAvailableActions();
        int index = randomGenerator.nextInt(actions.size());
        action = actions.get(index);
        return action;
    }

    /**
     * Prints the number of different types of sprites available in the "positions" array.
     * Between brackets, the number of observations of each type.
     * @param positions array with observations.
     * @param str identifier to print
     */
    private void printDebug(ArrayList<Observation>[] positions, String str)
    {
        if(positions != null){
            System.out.print(str + ":" + positions.length + "(");
            for (int i = 0; i < positions.length; i++) {
                System.out.print(positions[i].size() + ",");
            }
            System.out.print("); ");
        }else System.out.print(str + ": 0; ");
    }

    /**
     * Gets the player the control to draw something on the screen.
     * It can be used for debug purposes.
     * @param g Graphics device to draw to.
     */
    public void draw(Graphics2D g)
    {
        int half_block = (int) (block_size*0.5);
        for(int j = 0; j < grid[0].length; ++j)
        {
            for(int i = 0; i < grid.length; ++i)
            {
                if(grid[i][j].size() > 0)
                {
                    Observation firstObs = grid[i][j].get(0); //grid[i][j].size()-1
                    //Three interesting options:
                    int print = firstObs.category; //firstObs.itype; //firstObs.obsID;
                    g.drawString(print + "", i*block_size+half_block,j*block_size+half_block);
                }
            }
        }
    }


    @Override
    public ArrayList<Node> getPath(Vector2d origin, Vector2d destination, int block_size, boolean toNeighbour) {

        Vector2d startPos = new Vector2d(origin.x / block_size, origin.y / block_size);
        Vector2d goalPos = new Vector2d(destination.x / block_size, destination.y / block_size);

        Node start = new Node(startPos);
        Node goal = new Node(goalPos);

        return astar.findPath(start, goal, toNeighbour);
    }

    @Override
    public ArrayList<Node> getNeighbours(Node node) {
        ArrayList<Node> neighbours = new ArrayList<Node>();
        int x = (int) (node.position.x);
        int y = (int) (node.position.y);

        //up:
        Node neig = extractFreeNode(x,y-1);
        if(neig != null) neighbours.add(neig);

        //down:
        neig = extractFreeNode(x,y+1);
        if(neig != null) neighbours.add(neig);

        //left:
        neig = extractFreeNode(x-1,y);
        if(neig != null) neighbours.add(neig);

        //right:
        neig = extractFreeNode(x+1,y);
        if(neig != null) neighbours.add(neig);

        return neighbours;
    }

    @Override
    public void setMoveDir(Node node, Node pre) {

    }

    private Node extractFreeNode(int x, int y)
    {
        if(x < 0 || y < 0 || x > grid.length || y > grid[0].length)
            return null;

        int numObs = grid[x][y].size();
        boolean isObstacle = false;
        for(int i = 0; i < numObs; ++i)
        {
            Observation obs = grid[x][y].get(i);
            if(obs.category == Types.TYPE_STATIC)
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
        System.out.println("ERROR: This function is not implemented.");
        return false;
    }

}
