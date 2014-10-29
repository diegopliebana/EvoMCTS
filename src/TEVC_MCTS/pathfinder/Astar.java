package TEVC_MCTS.pathfinder;

import TEVC_MCTS.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.PriorityQueue;

/**
 * Created by diego on 05/09/14.
 */
public class Astar
{
    public static PriorityQueue<Node> closedList, openList;
    public Navigable nodeSource;
    public HashMap<Integer, ArrayList<Node>> pathCache;

    public Astar(Navigable nodeSource)
    {
        this.nodeSource = nodeSource;
        pathCache = new HashMap<Integer, ArrayList<Node>>();
    }

    public void emptyCache()
    {
        if(Config.USE_ASTAR_CACHE)
            pathCache.clear();
    }

    private static double heuristicEstimatedCost(Node curNode, Node goalNode)
    {
        //4-way: using Manhattan
        double xDiff = Math.abs(curNode.position.x - goalNode.position.x);
        double yDiff = Math.abs(curNode.position.y - goalNode.position.y);
        return xDiff + yDiff;

        //This is Euclidean distance(sub-optimal here).
        //return curNode.position.dist(goalNode.position);
    }


    private ArrayList<Node> calculatePath(Node node)
    {
        ArrayList<Node> path = new ArrayList<Node>();
        while(node != null)
        {
            if(node.parent != null) //to avoid adding the start node.
            {
                nodeSource.setMoveDir(node, node.parent);
                path.add(0,node);
            }
            node = node.parent;
        }
        return path;
    }

    public ArrayList<Node> findPath(Node start, Node goal, boolean toNeighbour)
    {
        int pathId = 0;
        if(Config.USE_ASTAR_CACHE)
        {
            pathId = start.id * 10000 + goal.id;
            if(pathCache.containsKey(pathId))
                return pathCache.get(pathId);
        }
        ArrayList<Node> path = _findPath(start, goal, toNeighbour);

        if(Config.USE_ASTAR_CACHE && path!=null)
            pathCache.put(pathId, path);

        return path;
    }

    private ArrayList<Node> _findPath(Node start, Node goal, boolean toNeighbour)
    {
        Node node = null;
        openList = new PriorityQueue<Node>();
        closedList = new PriorityQueue<Node>();

        start.totalCost = 0.0f;
        start.estimatedCost = heuristicEstimatedCost(start, goal);

        openList.add(start);

        while(openList.size() != 0)
        {
            node = openList.poll();
            closedList.add(node);

            if(node.position.equals(goal.position))
                return calculatePath(node);

            if(toNeighbour && nodeSource.isContiguous(node, goal))
                return calculatePath(node);

            ArrayList<Node> neighbours = nodeSource.getNeighbours(node);

            for(int i = 0; i < neighbours.size(); ++i)
            {
                Node neighbour = neighbours.get(i);
                double curDistance = neighbour.totalCost;

                if(!openList.contains(neighbour) && !closedList.contains(neighbour))
                {
                    neighbour.totalCost = curDistance + node.totalCost;
                    neighbour.estimatedCost = heuristicEstimatedCost(neighbour, goal);
                    neighbour.parent = node;

                    openList.add(neighbour);

                }else if(curDistance + node.totalCost < neighbour.totalCost)
                {
                    neighbour.totalCost = curDistance + node.totalCost;
                    neighbour.parent = node;

                    if(openList.contains(neighbour))
                        openList.remove(neighbour);

                    if(closedList.contains(neighbour))
                        closedList.remove(neighbour);

                    openList.add(neighbour);
                }
            }

        }

        if(! node.position.equals(goal.position))
            return null;

        return calculatePath(node);

    }

    public void printPath(ArrayList<Node> nodes)
    {
        if(nodes == null)
        {
            System.out.println("No Path");
            return;
        }

        for(Node n : nodes)
        {
            System.out.print(n.position.x + ":" + n.position.y + ", ");
        }
        System.out.println();
    }


}
