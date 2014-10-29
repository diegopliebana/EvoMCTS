package FastEvoMCTS;

import java.util.LinkedList;
import java.util.TreeMap;

/**
 * Created by IntelliJ IDEA.
 * User: diego
 * Date: 04/03/13
 * Time: 12:41
 * To change this template use File | Settings | File Templates.
 */
public class TranspositionTable         //Singleton
{
    private static TranspositionTable m_instance;

    public TreeMap<Integer, SingleTreeNodeList> m_table;

    private TranspositionTable()
    {
        m_table = new TreeMap<Integer, SingleTreeNodeList>();
    }

    public static TranspositionTable GetInstance()
    {
        if(m_instance == null)
        {
            m_instance = new TranspositionTable();
        }
        return m_instance;
    }

    public void reset()
    {
        m_table = new TreeMap<Integer, SingleTreeNodeList>();
    }

    public void addNodeToList(int a_x, int a_y, SingleTreeNode a_node)
    {
        int hashKey = getHashKey(a_x, a_y);
        if(m_table.get(hashKey) == null)
            m_table.put(hashKey, new SingleTreeNodeList());

        m_table.get(hashKey).add(a_node);
    }

    public SingleTreeNode getRepresentative(int a_x, int a_y)
    {
        int hashKey = getHashKey(a_x, a_y);
        SingleTreeNodeList list = m_table.get(hashKey);
        if(list == null || list.size() == 0)
            return null;
        else
            return list.get(0);
    }

    
    public double getDataInPosition (int a_x, int a_y)
    {
        int hashKey = getHashKey(a_x, a_y);
        SingleTreeNodeList list = m_table.get(hashKey);

        if(list == null || list.size() == 0)
            return 0.0;
        else
            return list.get(0).totValue;

    }
    
    private int getHashKey(int a_x, int a_y)
    {
        return 100*a_x + a_y;
    }

}

class SingleTreeNodeList extends LinkedList<SingleTreeNode> {}