package controllers.sampleMCTS;

import FastEvoMCTS.TranspositionTable;
import TEVC_MCTS.Config;
import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Utils;

import java.util.Random;

public class SingleTreeNode
{
    private static final double HUGE_NEGATIVE = -10000000.0;
    private static final double HUGE_POSITIVE =  10000000.0;
    public static double epsilon = 1e-6;
    public static double egreedyEpsilon = 0.05;
    public StateObservation state;
    public SingleTreeNode parent;
    public SingleTreeNode[] children;
    public double totValue;
    public int nVisits;
    public static Random m_rnd;
    public int m_depth;
    //protected static double[] lastBounds = new double[]{0,1}; //not in use since 15.11.2014
    //private static double[] curBounds = new double[]{0,1};
    protected static double[] bounds = new double[]{Double.MAX_VALUE, -Double.MAX_VALUE};

    private static int m_num_victories;
    public static double percVictoriesFound;

    public SingleTreeNode(Random rnd) {
        this(null, null, rnd);
        TranspositionTable.GetInstance().reset();
    }

    public SingleTreeNode(StateObservation state, SingleTreeNode parent, Random rnd) {
        //System.out.println("state = " + state);
        this.state = state;
        this.parent = parent;
        this.m_rnd = rnd;
        children = new SingleTreeNode[Agent.NUM_ACTIONS];
        totValue = 0.0;
        if(parent != null)
            m_depth = parent.m_depth+1;
        else
            m_depth = 0;
    }


    public void mctsSearch(ElapsedCpuTimer elapsedTimer) {

        //lastBounds[0] = curBounds[0];
        //lastBounds[1] = curBounds[1];

        double avgTimeTaken = 0;
        double acumTimeTaken = 0;
        long remaining = elapsedTimer.remainingTimeMillis();
        int numIters = 0;

        m_num_victories = 0;
        percVictoriesFound = 0;

        int remainingLimit = 5;
        ///while(remaining > 2*avgTimeTaken && remaining > remainingLimit){
        while(numIters < Agent.MCTS_ITERATIONS){
            ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();
            SingleTreeNode selected = treePolicy();
            double delta = selected.rollOut();
            backUp(selected, delta);

            numIters++;
            acumTimeTaken += (elapsedTimerIteration.elapsedMillis()) ;
            //System.out.println(elapsedTimerIteration.elapsedMillis() + " --> " + acumTimeTaken + " (" + remaining + ")");
            avgTimeTaken  = acumTimeTaken/numIters;
            remaining = elapsedTimer.remainingTimeMillis();
        }

        percVictoriesFound = (double) m_num_victories / numIters;

        //System.out.println("-- " + numIters + " -- ( " + avgTimeTaken + ")");

        //System.out.println(this.state.getGameTick() + "," + numIters);
        //System.out.println(numIters);
    }

    public SingleTreeNode treePolicy() {

        SingleTreeNode cur = this;

        while (!cur.state.isGameOver() && cur.m_depth < Agent.ROLLOUT_DEPTH)
        {
            if (cur.notFullyExpanded()) {
                return cur.expand();

            } else {
                SingleTreeNode next = cur.uct();
                //SingleTreeNode next = cur.egreedy();
                cur = next;
            }
        }

        return cur;
    }


    public SingleTreeNode expand() {

        int bestAction = 0;
        double bestValue = -1;

        for (int i = 0; i < children.length; i++) {
            double x = m_rnd.nextDouble();
            if (x > bestValue && children[i] == null) {
                bestAction = i;
                bestValue = x;
            }
        }

        StateObservation nextState = state.copy();
        nextState.advance(Agent.actions[bestAction]);

        SingleTreeNode tn = new SingleTreeNode(nextState, this, this.m_rnd);
        children[bestAction] = tn;
        return tn;

    }

    public SingleTreeNode uct() {

        SingleTreeNode selected = null;
        double bestValue = -Double.MAX_VALUE;
        for (SingleTreeNode child : this.children)
        {
            double hvVal = child.totValue;
            double childValue = hvVal / (child.nVisits + this.epsilon);

            childValue = Utils.normalise(childValue, bounds[0], bounds[1]);
            //childValue = Utils.normalise(childValue, lastBounds[0], lastBounds[1]);

//            double uctValue = childValue +
//                    Agent.K * Math.sqrt(Math.log(this.nVisits + 1) / (child.nVisits + this.epsilon)) +
//                    this.m_rnd.nextDouble() * this.epsilon;

            double tieBreaker = (1.0 + this.epsilon * (this.m_rnd.nextDouble() - 0.5));
            double uctValue = childValue +
                    Agent.K * Math.sqrt(Math.log(this.nVisits + 1) / (child.nVisits + this.epsilon));


            // small sampleRandom numbers: break ties in unexpanded nodes
            uctValue = Utils.tiebreaker(uctValue, this.epsilon, this.m_rnd.nextDouble());     //break ties randomly
            if (uctValue > bestValue) {
                selected = child;
                bestValue = uctValue;
            }
        }
        if (selected == null)
        {
            throw new RuntimeException("Warning! returning null: " + bestValue + " : " + this.children.length);
        }

        return selected;
    }

    public SingleTreeNode egreedy() {


        SingleTreeNode selected = null;

        if(m_rnd.nextDouble() < egreedyEpsilon)
        {
            //Choose randomly
            int selectedIdx = m_rnd.nextInt(children.length);
            selected = this.children[selectedIdx];

        }else{
            //pick the best Q.
            double bestValue = -Double.MAX_VALUE;
            for (SingleTreeNode child : this.children)
            {
                double hvVal = child.totValue;

                // small sampleRandom numbers: break ties in unexpanded nodes
                hvVal = Utils.tiebreaker(hvVal, this.epsilon, this.m_rnd.nextDouble());     //break ties randomly
                if (hvVal > bestValue) {
                    selected = child;
                    bestValue = hvVal;
                }
            }

        }


        if (selected == null)
        {
            throw new RuntimeException("Warning! returning null: " + this.children.length);
        }

        return selected;
    }


    public double rollOut()
    {
        StateObservation rollerState = state.copy();
        int thisDepth = this.m_depth;

        while (!finishRollout(rollerState,thisDepth)) {

            int action = m_rnd.nextInt(Agent.NUM_ACTIONS);
            rollerState.advance(Agent.actions[action]);
            thisDepth++;
        }


        if(rollerState.isGameOver() && rollerState.getGameWinner() == Types.WINNER.PLAYER_WINS)
        {
            m_num_victories++;
        }


        //double delta = value(rollerState);

        double rawDelta = value(rollerState);

        //Discount factor:
        double accDiscount = Math.pow(Agent.REWARD_DISCOUNT,thisDepth); //1
        //double accDiscount = Math.pow(Config.REWARD_DISCOUNT, thisDepth + (state.getGameTick() - 1));   //consider the distance from the beginning of the game
        double delta = rawDelta * accDiscount;

        if(delta < bounds[0]) {
            bounds[0] = delta;
            //System.out.format("bound0 %f %d %d\n",delta,thisDepth,state.getGameTick());
        }
        if(delta > bounds[1]) {
            bounds[1] = delta;
            //System.out.format("bound1 %f %d %d\n",delta,thisDepth,state.getGameTick());
        }

        //if(delta < curBounds[0]) curBounds[0] = delta;
        //if(delta > curBounds[1]) curBounds[1] = delta;

        //delta = Utils.normalise(delta ,lastBounds[0], lastBounds[1]);

        return delta;
    }

    public double value(StateObservation a_gameState) {

        boolean gameOver = a_gameState.isGameOver();
        Types.WINNER win = a_gameState.getGameWinner();
        double rawScore = a_gameState.getGameScore();

        if(gameOver && win == Types.WINNER.PLAYER_LOSES)
            //return HUGE_NEGATIVE;
            rawScore += HUGE_NEGATIVE;

        else if(gameOver && win == Types.WINNER.PLAYER_WINS)
            //return HUGE_POSITIVE;
            rawScore += HUGE_POSITIVE;

        return rawScore;
    }

    public boolean finishRollout(StateObservation rollerState, int depth)
    {
        if(depth >= Agent.ROLLOUT_DEPTH)      //rollout end condition.
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

//                double tieBreaker = m_rnd.nextDouble() * epsilon;
//                if (children[i].nVisits + tieBreaker > bestValue) {
//                    bestValue = children[i].nVisits + tieBreaker;
//                    selected = i;
//                }

                double childValue = children[i].nVisits;
                childValue = Utils.tiebreaker(childValue, this.epsilon, this.m_rnd.nextDouble());     //break ties randomly
                if (childValue > bestValue) {
                    bestValue = childValue;
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

//            double tieBreaker = m_rnd.nextDouble() * epsilon;
//            if(children[i] != null && children[i].totValue + tieBreaker > bestValue) {
//                bestValue = children[i].totValue + tieBreaker;
//                selected = i;
//            }

            if(children[i] != null) {
                //double tieBreaker = m_rnd.nextDouble() * epsilon;
                double childValue = children[i].totValue / (children[i].nVisits + this.epsilon);
                childValue = Utils.tiebreaker(childValue, this.epsilon, this.m_rnd.nextDouble());     //break ties randomly
                if (childValue > bestValue) {
                    bestValue = childValue;
                    selected = i;
                }
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
}
