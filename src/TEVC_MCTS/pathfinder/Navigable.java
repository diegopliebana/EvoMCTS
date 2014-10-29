package TEVC_MCTS.pathfinder;

import TEVC_MCTS.utils.Memory;
import core.game.StateObservation;
import ontology.Types;
import tools.Vector2d;

import java.util.ArrayList;

/**
 * Created by diego on 05/09/14.
 */
public abstract class Navigable
{
    public ArrayList<Integer> validAstarStatic;
    public Memory memory;
    public Astar astar;
    public int[] x_arrNeig = null;
    public int[] y_arrNeig = null;


    protected abstract Node extractNonObstacle(int x, int y);
    protected abstract void initNeighbours(StateObservation stObs);


    public void setMemory(Memory memory)
    {
        this.memory = memory;
    }


    protected double astarDistanceFromTo(Vector2d origin, Vector2d target, int block_size, boolean toNeighbour)
    {
        ArrayList<Node> pathToObservation = getPath(origin, target, block_size, toNeighbour);

        if(pathToObservation != null)
            return pathToObservation.size();

        return -1;
    }

    public ArrayList<Node> getPath(Vector2d origin, Vector2d destination, int block_size, boolean toNeighbour) {
        Vector2d startPos = new Vector2d((int) (origin.x / block_size), (int) (origin.y / block_size));
        Vector2d goalPos = new Vector2d((int) (destination.x / block_size), (int) (destination.y / block_size));

        Node start = new Node(startPos);
        Node goal = new Node(goalPos);

        return astar.findPath(start, goal, toNeighbour);
    }


    public ArrayList<Node> getNeighbours(Node node) {

        ArrayList<Node> neighbours = new ArrayList<Node>();
        int x = (int) (node.position.x);
        int y = (int) (node.position.y);

        for(int i = 0; i < x_arrNeig.length; ++i)
        {
            Node neig = extractNonObstacle(x + x_arrNeig[i], y + y_arrNeig[i]);
            if(neig != null) neighbours.add(neig);
        }

        return neighbours;
    }


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
