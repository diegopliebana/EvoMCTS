package MacroOLMCTS;

import MacroOLMCTS.macro.MacroAction;
import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Utils;

import javax.swing.plaf.nimbus.State;
import java.util.Random;

public class SingleTreeNode
{
    private static final double HUGE_NEGATIVE = -10000000.0;
    private static final double HUGE_POSITIVE =  10000000.0;
    public static double epsilon = 1e-6;
    public static double egreedyEpsilon = 0.05;
    public SingleTreeNode parent;
    public SingleTreeNode[] children;
    public double totValue;
    public int nVisits;
    public static Random m_rnd;
    public int m_depth;
    protected static double[] bounds = new double[]{0,1};
    public int childIdx;

    public static StateObservation rootState;

    public SingleTreeNode(Random rnd) {
        this(null, -1, rnd);
    }

    public SingleTreeNode(SingleTreeNode parent, int childIdx, Random rnd) {
        this.parent = parent;
        this.m_rnd = rnd;
        children = new SingleTreeNode[Agent.NUM_ACTIONS];
        totValue = 0.0;
        this.childIdx = childIdx;
        if(parent != null)
            m_depth = parent.m_depth+1;
        else
            m_depth = 0;
    }


    public void mctsSearch(ElapsedCpuTimer elapsedTimer) {

        double avgTimeTaken = 0;
        double acumTimeTaken = 0;
        long remaining = elapsedTimer.remainingTimeMillis();
        int numIters = 0;

        int remainingLimit = 5;
        while(remaining > 2*avgTimeTaken && remaining > remainingLimit){
        //while(numIters < Agent.MCTS_ITERATIONS){

            StateObservation state = rootState.copy();

            ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();
            SingleTreeNode selected = treePolicy(state);
            double delta = selected.rollOut(state);
            backUp(selected, delta);

            numIters++;
            acumTimeTaken += (elapsedTimerIteration.elapsedMillis()) ;
            //System.out.println(elapsedTimerIteration.elapsedMillis() + " --> " + acumTimeTaken + " (" + remaining + ")");
            avgTimeTaken  = acumTimeTaken/numIters;
            remaining = elapsedTimer.remainingTimeMillis();
        }
    }

    public SingleTreeNode treePolicy(StateObservation state) {

        SingleTreeNode cur = this;

        while (!state.isGameOver() && cur.m_depth < Agent.ROLLOUT_DEPTH)
        {
            if (cur.notFullyExpanded()) {
                return cur.expand(state);

            } else {
                SingleTreeNode next = cur.uct(state);
                cur = next;
            }
        }

        return cur;
    }


    public SingleTreeNode expand(StateObservation state) {

        int bestAction = 0;
        double bestValue = -1;

        for (int i = 0; i < children.length; i++) {
            double x = m_rnd.nextDouble();
            if (x > bestValue && children[i] == null) {
                bestAction = i;
                bestValue = x;
            }
        }

        //Roll the state
        //state.advance(Agent.actions[bestAction].actions[0]); //TODO: macro-advance, check!!
        advanceMacro(state, Agent.actions[bestAction]);


        SingleTreeNode tn = new SingleTreeNode(this,bestAction,this.m_rnd);
        children[bestAction] = tn;
        return tn;
    }

    public SingleTreeNode uct(StateObservation state) {

        SingleTreeNode selected = null;
        double bestValue = -Double.MAX_VALUE;
        //System.out.println("Reward bounds at UCT. LAST: " + lastBounds[0] + ", " + lastBounds[1]
        //                    + ", CURR: " + curBounds[0] + ", " + curBounds[1]);

        for (SingleTreeNode child : this.children)
        {
            double hvVal = child.totValue;
            double childValue =  hvVal / (child.nVisits + this.epsilon);

            childValue = Utils.normalise(childValue, bounds[0], bounds[1]);
            //System.out.println("norm child value: " + childValue);

            //double uctValue = childValue +
            //        Agent.K * Math.sqrt(Math.log(this.nVisits + 1) / (child.nVisits + this.epsilon)) +
            //        this.m_rnd.nextDouble() * this.epsilon;


            double tieBreaker = (1.0 + this.epsilon * (this.m_rnd.nextDouble() - 0.5));
            double uctValue = (childValue +
                    Agent.K * Math.sqrt(Math.log(this.nVisits + 1) / (child.nVisits + this.epsilon)) )*
                    tieBreaker;


            // small sampleRandom numbers: break ties in unexpanded nodes
            if (uctValue > bestValue) {
                selected = child;
                bestValue = uctValue;
            }
        }
        if (selected == null)
        {
            throw new RuntimeException("Warning! returning null: " + bestValue + " : " + this.children.length + " " +
            + bounds[0] + " " + bounds[1]);
        }

        //Roll the state:
        //state.advance(Agent.actions[selected.childIdx].actions[0]); //TODO: macro-advance, check!!
        advanceMacro(state, Agent.actions[selected.childIdx]);

        return selected;
    }


    public double rollOut(StateObservation state)
    {
        int thisDepth = this.m_depth;

        while (!finishRollout(state,thisDepth)) {

            int action = m_rnd.nextInt(Agent.NUM_ACTIONS);
            //state.advance(Agent.actions[action].actions[0]); //TODO: macro-advance, check!!
            advanceMacro(state, Agent.actions[action]);
            thisDepth++;
        }


        double rawDelta = value(state);

        //Discount factor:
        double accDiscount = Math.pow(Agent.REWARD_DISCOUNT,thisDepth); //1
        double delta = rawDelta * accDiscount;

        if(delta < bounds[0])
            bounds[0] = delta;
        if(delta > bounds[1])
            bounds[1] = delta;

        //double normDelta = Utils.normalise(delta ,lastBounds[0], lastBounds[1]);

        return delta;
    }

    public double value(StateObservation a_gameState) {

        boolean gameOver = a_gameState.isGameOver();
        Types.WINNER win = a_gameState.getGameWinner();
        double rawScore = a_gameState.getGameScore();

        if(gameOver && win == Types.WINNER.PLAYER_LOSES)
            rawScore += HUGE_NEGATIVE;

        if(gameOver && win == Types.WINNER.PLAYER_WINS)
            rawScore += HUGE_POSITIVE;

        return rawScore;
    }

    public boolean finishRollout(StateObservation rollerState, int depth)
    {
        //if(depth >= Agent.ROLLOUT_DEPTH)      //rollout end condition.
        if((depth + Agent.MACROACTION_LENGTH) >= Agent.ROLLOUT_DEPTH)      //rollout end condition.
            return true;

        if(rollerState.isGameOver())               //end of game
            return true;

        return false;
    }

    public void backUp(SingleTreeNode node, double result)
    {
        SingleTreeNode n = node;
        while(n != null)
        {
            n.nVisits++;
            n.totValue += result;
            n = n.parent;
        }
    }


    public int mostVisitedAction() {
        int selected = -1;
        double bestValue = -Double.MAX_VALUE;
        boolean allEqual = true;
        double first = -1;

        for (int i=0; i<children.length; i++) {

            if(children[i] != null)
            {
                if(first == -1)
                    first = children[i].nVisits;
                else if(first != children[i].nVisits)
                {
                    allEqual = false;
                }

                double tieBreaker = m_rnd.nextDouble() * epsilon;
                if (children[i].nVisits + tieBreaker > bestValue) {
                    bestValue = children[i].nVisits + tieBreaker;
                    selected = i;
                }
            }
        }

        if (selected == -1)
        {
            System.out.println("Unexpected selection!");
            selected = 0;
        }else if(allEqual)
        {
            //If all are equal, we opt to choose for the one with the best Q.
            selected = bestAction();
        }
        return selected;
    }

    public int bestAction()
    {
        int selected = -1;
        double bestValue = -Double.MAX_VALUE;

        for (int i=0; i<children.length; i++) {

            double tieBreaker = m_rnd.nextDouble() * epsilon;
            if(children[i] != null && children[i].totValue + tieBreaker > bestValue) {
                bestValue = children[i].totValue + tieBreaker;
                selected = i;
            }
        }

        if (selected == -1)
        {
            System.out.println("Unexpected selection!");
            selected = 0;
        }

        return selected;
    }


    public boolean notFullyExpanded() {
        for (SingleTreeNode tn : children) {
            if (tn == null) {
                return true;
            }
        }

        return false;
    }

    //This function assumes that it's not going beyond the rollout length.
    public void advanceMacro(StateObservation stObs, MacroAction action)
    {
        action.reset();
        while(!stObs.isGameOver() && !action.isFinished())
        {
            stObs.advance(action.next());
        }
    }

}
