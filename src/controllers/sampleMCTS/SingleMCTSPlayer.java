package controllers.sampleMCTS;

import controllers.sampleMCTS.learning.SingleTreeNodeLearning;
import core.game.StateObservation;
import tools.ElapsedCpuTimer;

import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: Diego
 * Date: 07/11/13
 * Time: 17:13
 */
public class SingleMCTSPlayer
{

    public enum LEARNING_STYLE {
        SARSA_1,
        AC_SARSA_1,
        NONE,

    }

    LEARNING_STYLE learning_style = LEARNING_STYLE.NONE;

    /**
     * Root of the tree.
     */
    public SingleTreeNode m_root;

    /**
     * Random generator.
     */
    public Random m_rnd;


    public SingleMCTSPlayer(Random a_rnd, LEARNING_STYLE learning)
    {
        m_rnd = a_rnd;
        learning_style = learning;
    }

    /**
     * Inits the tree with the new observation state in the root.
     * @param a_gameState current state of the game.
     */
    public void init(StateObservation a_gameState)
    {
        //Set the game observation to a newly root node.
        //System.out.println("learning_style = " + learning_style);
        switch (learning_style) {
            case SARSA_1:
                m_root = new SingleTreeNodeLearning(a_gameState, null, m_rnd);
                break;
            case AC_SARSA_1:
                //m_root = new SingleTreeNodeLearning(m_rnd);
                break;

            default:
                m_root = new SingleTreeNode(m_rnd);

        }
        m_root.state = a_gameState;
    }

    /**
     * Runs MCTS to decide the action to take. It does not reset the tree.
     * @param elapsedTimer Timer when the action returned is due.
     * @return the action to execute in the game.
     */
    public int run(ElapsedCpuTimer elapsedTimer)
    {
        //Do the search within the available time.
        m_root.mctsSearch(elapsedTimer);

        //Determine the best action to take and return it.
        int action = m_root.mostVisitedAction();
        //int action = m_root.bestAction();
        return action;
    }

}
