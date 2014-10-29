package FastEvoMCTS;

import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Utils;
import tools.Vector2d;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;


/**
 * Node for MCTS. Includes:
 *  - Adaptive bounds for UCT, using the last [min,max] pair.
 *  - Transposition tables [Work in progress].
 *  -
 */

public class SingleTreeNodeOL extends TreeNode {
    public TunableRoller roller;
    public Memory memory;

    public boolean m_newNodeCreated;
    public static LinkedList<SingleTreeNodeOL> m_runList = new LinkedList<SingleTreeNodeOL>();

    private static final double HUGE_NEGATIVE = -10000000.0;
    private static final double HUGE_POSITIVE =  10000000.0;
    public static double epsilon = 1e-6;
    public SingleTreeNodeOL parent;
    public SingleTreeNodeOL[] children;
    public double totValue;
    public int nVisits;
    public static Random m_rnd;
    private int m_depth;
    private static double[] lastBounds = new double[]{0,1};
    private static double[] curBounds = new double[]{0,1};

    static double biasInfluence = 0.0;
    static double meanInfluence = 1.0;

    private static double initialScore;

    public SingleTreeNodeOL(Random rnd, TunableRoller roller, Memory memory) {
        this(null, null, rnd, roller, memory);
    }

    public SingleTreeNodeOL(StateObservation state, SingleTreeNodeOL parent, Random rnd, TunableRoller roller, Memory memory) {
        this.memory = memory;
        this.state = state;
        this.parent = parent;
        this.m_rnd = rnd;
        this.roller = roller;
        children = new SingleTreeNodeOL[Agent.NUM_ACTIONS];
        totValue = 0.0;
        if(parent != null)
            m_depth = parent.m_depth+1;
        else
            m_depth = 0;
    }


    @Override
    public void mctsSearch(ElapsedCpuTimer elapsedTimer, TunableRoller roller, FitVectorSource source) {

        lastBounds[0] = curBounds[0];
        lastBounds[1] = curBounds[1];

        double avgTimeTaken = 0;
        double acumTimeTaken = 0;
        long remaining = elapsedTimer.remainingTimeMillis();
        int numIters = 0;

        if(Config.USE_MEMORY && Config.USE_FORGET)
            memory.forget(state.getGameTick());

        int remainingLimit = 5;
        while(remaining > 2*avgTimeTaken && remaining > remainingLimit)
        {
            ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();
            m_runList.clear();
            m_runList.add(this); //root always in.
            initialScore = state.getGameScore();

            if(Config.USE_EVO)
            {
                String[] presentFeatures = roller.featureNames(state);
                double[] weights = source.getNext(presentFeatures);
                roller.setParams(source.getGenMapping(), weights);
            }

            if(Config.USE_MEMORY)
                memory.mark(roller.getFeatures());

            SingleTreeNodeOL selected = treePolicy(this);
            double value = 0;
            if(Config.USE_EVO)
            {
                value = selected.tunedRollOut();
                source.returnFitness(value);
            }else
                value = selected.rollOut();

            backUp(selected, value);

            if(Config.USE_EVO && roller.newFeaturesFound())
                source.addFeatures(roller.getNewFeatures());

            remaining = elapsedTimer.remainingTimeMillis();
            numIters++;
            acumTimeTaken += (elapsedTimerIteration.elapsedMillis()) ;
            //System.out.println(elapsedTimerIteration.elapsedMillis() + " --> " + acumTimeTaken + " (" + remaining + ")");
            avgTimeTaken  = acumTimeTaken/numIters;

        }
        //System.out.println(this.state.getGameTick() + "," + numIters);
        //System.out.println(numIters);
    }

    public SingleTreeNodeOL treePolicy(SingleTreeNodeOL node) {

        SingleTreeNodeOL cur = node;
        StateObservation stateObs = node.state.copy();

        while (!stateObs.isGameOver() && cur.m_depth < Config.ROLLOUT_DEPTH)
        {
            if (cur.notFullyExpanded()) {
                m_newNodeCreated = true;
                SingleTreeNodeOL stn = cur.expand(stateObs);

                if(m_newNodeCreated)
                {
                    m_runList.add(0,stn);
                    if(Config.COMPUTE_HIT_MAP && !stateObs.isGameOver())
                    {
                        Vector2d pos = new Vector2d(stateObs.getAvatarPosition().x / stateObs.getBlockSize(),
                                                    stateObs.getAvatarPosition().y / stateObs.getBlockSize());
                        SingleMCTSPlayer.m_hitsMap[(int)pos.x][(int)pos.y]++;
                    }
                    return stn;
                }
                cur = stn;
            } else {
                cur = cur.uct(stateObs);
            }
            m_runList.add(0,cur);
            stateObs = cur.state;
        }

        return cur;
    }


    public SingleTreeNodeOL expand(StateObservation stateObs) {

        int bestAction = 0;
        double bestValue = -1;

        for (int i = 0; i < children.length; i++) {

            boolean prune = false;
            if(Config.USE_PRUNE_CONT_UCT && parent != null && parent.childIdx != -1)
            {
                prune = memory.contraryActions[parent.childIdx][i];
            }

            if(!prune)
            {
                double x = m_rnd.nextDouble();
                if (x > bestValue && children[i] == null) {
                    bestAction = i;
                    bestValue = x;
                }
            }
        }

        stateObs.advance(Agent.actions[bestAction]);

       // boolean hasNoEffect = memory.noEffect(state, bestAction, nextState);


        if(Config.USE_MEMORY)
            memory.addInformation(state.getEventsHistory().size(), state.getGameScore(),
                                  stateObs, roller.getFeatures().getFeatureVector());

        SingleTreeNodeOL tn = new SingleTreeNodeOL(stateObs, this, this.m_rnd, this.roller, this.memory);
        children[bestAction] = tn;
        tn.childIdx = bestAction;

        return children[bestAction];
    }

    public SingleTreeNodeOL uct(StateObservation stateObs) {

        SingleTreeNodeOL selected = null;
        double bestValue = -Double.MAX_VALUE;
        int uctAction = -1;
        for (int i = 0; i < this.children.length; ++i)
        //for (SingleTreeNodeOL child : this.children)
        {
            SingleTreeNodeOL child = this.children[i];
            double hvVal = child.totValue;
            double childValue =  hvVal / (child.nVisits + this.epsilon);

            double uctValue = childValue +
                    Config.K * Math.sqrt(Math.log(this.nVisits + 1) / (child.nVisits + this.epsilon)) +
                    this.m_rnd.nextDouble() * this.epsilon;

            // small sampleRandom numbers: break ties in unexpanded nodes
            if (uctValue > bestValue) {
                selected = child;
                bestValue = uctValue;
                uctAction = i;
            }
        }

        if (selected == null)
        {
            //throw new RuntimeException("Warning! returning null: " + bestValue + " : " + this.children.length);
            return this.children[0];
        }

        stateObs.advance(stateObs.getAvailableActions().get(uctAction));
        selected.state = stateObs;
        return selected;
    }

    public double rollOut()
    {
        StateObservation rollerState = state;
        int thisDepth = this.m_depth;
        HashMap<String, Double> features = null;
        while (!finishRollout(rollerState,thisDepth)) {

            int action = m_rnd.nextInt(Agent.NUM_ACTIONS);

            int prevEvCount = rollerState.getEventsHistory().size();
            double scorePrev = rollerState.getGameScore();

            rollerState.advance(Agent.actions[action]);

            if(Config.USE_MEMORY)
            {
                features  = roller.getFeatures().getFeatureVector(rollerState);
                memory.addInformation(prevEvCount, scorePrev, rollerState, features);
            }

            thisDepth++;
        }

        double delta = value(rollerState);
        if(delta == HUGE_POSITIVE || delta == HUGE_NEGATIVE)
            return delta;

        if(delta < curBounds[0]) curBounds[0] = delta;
        if(delta > curBounds[1]) curBounds[1] = delta;

        double normDelta = Utils.normalise(delta ,lastBounds[0], lastBounds[1]);

        if(Config.USE_MEMORY)
        {
            if(features == null)
                features  = roller.getFeatures().getFeatureVector();

            if(normDelta == 0)
            {
                double infoGain = memory.getKnowledgeGain(features);
                double disc = memory.getDiscoverScore();
                double score = infoGain*0.66 + disc*0.33;
                return score;
            }else
                return normDelta;
        }


        return normDelta;
    }


    public double tunedRollOut()
    {
        StateObservation rollerState = state;
        int thisDepth = this.m_depth;

        HashMap<String, Double> features = null;


        while (!finishRollout(rollerState,thisDepth)) {

            //int action = m_rnd.nextInt(FastEvoMCTS.Agent.NUM_ACTIONS);
            int action = roller.roll(rollerState);
            if(action >= 0)
            {
                int prevEvCount = rollerState.getEventsHistory().size();
                double scorePrev = rollerState.getGameScore();

                rollerState.advance(Agent.actions[action]);

                if(Config.USE_MEMORY)
                {
                    features  = roller.getFeatures().getFeatureVector(rollerState);
                    memory.addInformation(prevEvCount, scorePrev, rollerState, features);
                }

                if(Config.COMPUTE_HIT_MAP && !rollerState.isGameOver())
                {
                    Vector2d pos = new Vector2d(rollerState.getAvatarPosition().x / rollerState.getBlockSize(),
                                                rollerState.getAvatarPosition().y / rollerState.getBlockSize());
                    SingleMCTSPlayer.m_hitsMap[(int)pos.x][(int)pos.y]++;
                }
            }

            thisDepth++;
        }

        double delta = value(rollerState);
        if(delta == HUGE_POSITIVE || delta == HUGE_NEGATIVE)
            return delta;

        if(delta < curBounds[0]) curBounds[0] = delta;
        if(delta > curBounds[1]) curBounds[1] = delta;

        double normDelta = Utils.normalise(delta ,lastBounds[0], lastBounds[1]);

        if(Config.USE_MEMORY)
        {
            if(features == null)
                features  = roller.getFeatures().getFeatureVector();

            if(normDelta == 0)
            {
                double infoGain = memory.getKnowledgeGain(features);
                double disc = memory.getDiscoverScore();
                double score = infoGain*0.66 + disc*0.33;
                return score;
            }else
                return normDelta;
        }


        return normDelta;
    }

    public double value(StateObservation a_gameState) {

        boolean gameOver = a_gameState.isGameOver();
        Types.WINNER win = a_gameState.getGameWinner();
        double rawScore = a_gameState.getGameScore();
        rawScore = rawScore - initialScore;

        if(gameOver && win == Types.WINNER.PLAYER_LOSES)
            return HUGE_NEGATIVE;

        if(gameOver && win == Types.WINNER.PLAYER_WINS)
            return HUGE_POSITIVE;

        return rawScore;
    }

    public boolean finishRollout(StateObservation rollerState, int depth)
    {
        if(depth >= Config.ROLLOUT_DEPTH)      //rollout end condition.
            return true;

        if(rollerState.isGameOver())               //end of game
            return true;

        return false;
    }

    public void backUp(SingleTreeNodeOL node, double result)
    {
        /*SingleTreeNode n = node;
        while(n != null)
        {
            n.nVisits++;
            n.totValue += result;
            n = n.parent;
        } */

        int numNodes = m_runList.size();
        for(int i = 0; i < numNodes; ++i)
        {
            SingleTreeNodeOL stn = m_runList.get(i);
            stn.nVisits++;
            stn.totValue += result;

        }
    }

    public double meanValue() {
        return totValue / nVisits + epsilon;
    }


    @Override
    public int bestBiasedAction() {

        if (roller != null && roller instanceof VariableFeatureWeightedRoller)
            return biasedRootAction(state, (VariableFeatureWeightedRoller) roller);

        return mostVisitedAction();
    }


    @Override
    public int biasedRootAction(StateObservation state, VariableFeatureWeightedRoller roller) {
        Picker<Integer> p = new Picker<Integer>();
        int bestAction;

        double[] biases = roller.getBiases(state);
        // System.out.println(Arrays.toString(biases));
        for (int i=0; i<children.length; i++) {
            if (children[i] != null) {
                p.add(children[i].meanValue() * meanInfluence + biases[i] * biasInfluence + m_rnd.nextDouble() * epsilon, i);
            }
        }
        return p.getBest();
    }

    @Override
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

                if (children[i].nVisits + m_rnd.nextDouble() * epsilon > bestValue) {
                    bestValue = children[i].nVisits;
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

    @Override
    public int bestAction()
    {
        int selected = -1;
        double bestValue = -Double.MAX_VALUE;

        for (int i=0; i<children.length; i++) {

            if(children[i] != null && children[i].totValue + m_rnd.nextDouble() * epsilon > bestValue) {
                bestValue = children[i].totValue;
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
        for (SingleTreeNodeOL tn : children) {
            if (tn == null) {
                return true;
            }
        }

        return false;
    }
}
