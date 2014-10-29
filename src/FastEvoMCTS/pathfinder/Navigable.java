package FastEvoMCTS.pathfinder;

import ontology.Types;
import tools.Vector2d;

import java.util.ArrayList;

/**
 * Created by diego on 05/09/14.
 */
public interface Navigable
{
    public ArrayList<Node> getPath(Vector2d origin, Vector2d destination, int block_size, boolean toNeighbour);
    public ArrayList<Node> getNeighbours(Node node);
    public boolean isContiguous(Node current, Node target); //Neighbour without checking traversable state.
    public void setMoveDir(Node node, Node pre);
}
