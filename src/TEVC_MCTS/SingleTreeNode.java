package TEVC_MCTS;

import TEVC_MCTS.features.NavFeatureSource;
import TEVC_MCTS.roller.TunableRoller;
import TEVC_MCTS.roller.VariableFeatureWeightedRoller;
import TEVC_MCTS.utils.Memory;
import TEVC_MCTS.utils.Picker;
import TEVC_MCTS.vectorsource.FitVectorSource;
import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.StatSummary;
import tools.Utils;
import tools.Vector2d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;


/**
 * Node for MCTS. Includes:
 *  - Adaptive bounds for UCT, using the last [min,max] pair.
 *  -
 */

public class SingleTreeNode extends TreeNode
{
    public TunableRoller roller;
    public Memory memory;

    HashMap<String, Double> nodeFeatures;
    public boolean m_newNodeCreated;
    public static LinkedList<SingleTreeNode> m_runList = new LinkedList<SingleTreeNode>();

    private static final double HUGE_NEGATIVE = -2.0;
    private static final double HUGE_POSITIVE =  2.0;
    public static double epsilon = 1e-6;
    public SingleTreeNode parent;
    public SingleTreeNode[] children;
    public double totValue;
    public int nVisits;
    public static Random m_rnd;
    private int m_depth;
    private static double[] lastBounds = new double[]{0,1};
    private static double[] curBounds = new double[]{0,1};

    static double biasInfluence = 0.0;
    static double meanInfluence = 1.0;

    private static double rawScoreBeginPlayout;
    private static double rawScoreBeginRollout;
    private static double memScoreBeginRollout;

    ArrayList<StateObservation> rolloutStates;
    ArrayList<Integer> rolloutActions;

    private static StatSummary weightVectorFitness;
    private static double[] all_fitness;
    private static int cur_fit_it;
    private static int m_num_victories;
    public static double percVictoriesFound;


    public SingleTreeNode(Random rnd, TunableRoller roller, Memory memory) {
        this(null, null, rnd, roller, memory);
    }

    public SingleTreeNode(StateObservation state, SingleTreeNode parent, Random rnd, TunableRoller roller, Memory memory) {
        this.memory = memory;
        this.state = state;
        this.parent = parent;
        this.m_rnd = rnd;
        this.roller = roller;
        children = new SingleTreeNode[Agent.NUM_ACTIONS];
        totValue = 0.0;
        rolloutStates = new ArrayList<StateObservation>();
        rolloutActions = new ArrayList<Integer>();
        if(parent != null)
            m_depth = parent.m_depth+1;
        else
            m_depth = 0;
    }


    int firstAction = -1;
    public void mctsSearch(ElapsedCpuTimer elapsedTimer, TunableRoller roller, FitVectorSource source) {

        lastBounds[0] = curBounds[0];
        lastBounds[1] = curBounds[1];

        double avgTimeTaken = 0;
        double acumTimeTaken = 0;
        long remaining = elapsedTimer.remainingTimeMillis();
        int numIndividuals = 0;

        this.nodeFeatures = roller.getFeatures().getFeatureVector(state);

        if(Config.USE_MEMORY && Config.USE_FORGET)
            memory.forget(state.getGameTick());

        //System.out.println("###############");
        m_num_victories = 0;
        percVictoriesFound = 0;

        int remainingLimit = 5;
        //while(remaining > 2*avgTimeTaken && remaining > remainingLimit)
        while(numIndividuals < Config.MCTS_ITERATIONS)
        {
            weightVectorFitness = new StatSummary();
            all_fitness = new double[Config.INDIVIDUAL_ITERATIONS];
            cur_fit_it = 0;
            ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();
            int iterations = 0;

            //Get the features at this state of the game.
            String[] presentFeatures = roller.featureNames(state);

            //Set the next vector of weights.
            if(Config.ES_TYPE == Config.HAND_TUNED_WEIGHTS)
                roller.setParams(source.getGenMapping(),
                        roller.getFeatures().getHandTunedWeights(state));
            else
                roller.setParams(source.getGenMapping(),
                        source.getNext(presentFeatures));

            //Mark what we know about the features at this point in time.
            if(Config.USE_MEMORY)
                memory.mark(roller.getFeatures());

            //System.out.println(" **** Evaluating *** ");
            //System.out.println(source.proposed[0] + " " + source.proposed[1]);
            //System.out.println(" **** ********** *** ");

            m_runList.clear();
            rolloutStates.clear();
            rolloutActions.clear();
            m_runList.add(this); //root always in.
            firstAction = -1;

            //initial score at the beginning of the playout.
            rawScoreBeginPlayout = state.getGameScore();

            //Tree policy.
            SingleTreeNode selected = treePolicy();

            //Get the knowledge gain value at the end of the tree policy
            memScoreBeginRollout = knowledgeValue(selected.nodeFeatures);

            //Score at the beginning of the rollout.
            rawScoreBeginRollout = selected.state.getGameScore();

            StatSummary averageReward = new StatSummary();

            while(iterations < Config.INDIVIDUAL_ITERATIONS)
            {
                iterations++;
                //Perform rollout, assign fitness to weight vector and back-propagate reward.
                double value = selected.tunedRollOut(source, roller.getFeatures());
                averageReward.add(value);
            }

            backUp(selected, averageReward.mean());

            double averageFitness = weightVectorFitness.mean();

            if(weightVectorFitness.n() > 0)
            {
                //This may happen when we choose not to update a fitness (i.e.: bandits).
                double normFit = Utils.normalise(averageFitness,HUGE_NEGATIVE,HUGE_POSITIVE);
                source.returnFitness(rolloutStates, rolloutActions, normFit);
            }

            //We might have found new features during the rollouts, update vector sizes.
            if(roller.newFeaturesFound())
                source.addFeatures(roller.getNewFeatures());


            numIndividuals++;
            remaining = elapsedTimer.remainingTimeMillis();
            acumTimeTaken += (elapsedTimerIteration.elapsedMillis()) ;
            avgTimeTaken  = acumTimeTaken/numIndividuals;

        }

        int totRollouts = numIndividuals * Config.INDIVIDUAL_ITERATIONS;
        percVictoriesFound = (double) m_num_victories / totRollouts;
        //System.out.println("Perc. victories: " + percVictoriesFound);
    }

    public SingleTreeNode treePolicy() {

        SingleTreeNode cur = this;

        while (!cur.state.isGameOver() && cur.m_depth < Config.ROLLOUT_DEPTH)
        {
            if (cur.notFullyExpanded()) {
                m_newNodeCreated = true;
                SingleTreeNode stn = cur.expand();

                if(m_newNodeCreated)
                {
                    m_runList.add(0,stn);

                    if(Config.COMPUTE_HIT_MAP && !cur.state.isGameOver())
                    {
                        Vector2d pos = new Vector2d(cur.state.getAvatarPosition().x / cur.state.getBlockSize(),
                                cur.state.getAvatarPosition().y / cur.state.getBlockSize());
                        SingleMCTSPlayer.m_hitsMap[(int)pos.x][(int)pos.y]++;
                    }
                    return stn;
                }
                cur = stn;
            } else {
                cur = cur.uct();
            }
            m_runList.add(0,cur);
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

        if(firstAction == -1)
            firstAction = bestAction;

        StateObservation nextState = state.copy();
        nextState.advance(Agent.actions[bestAction]);

        if(Config.USE_MEMORY)
        {
            HashMap<String, Double> features = roller.getFeatures().getFeatureVector(nextState);
            memory.addInformation(state.getEventsHistory().size(), state.getGameScore(), nextState, features);
        }

        SingleTreeNode tn = new SingleTreeNode(nextState, this, this.m_rnd, this.roller, this.memory);
        children[bestAction] = tn;
        tn.childIdx = bestAction;

        tn.nodeFeatures = roller.getFeatures().getFeatureVector(nextState);

        return children[bestAction];
    }

    public SingleTreeNode uct() {

        SingleTreeNode selected = null;
        double bestValue = -Double.MAX_VALUE;

        for (SingleTreeNode child : this.children)
        {

            double hvVal = child.totValue;

            double childValue =  hvVal / (child.nVisits + this.epsilon);

            double uctValue = childValue +
                    Config.K * Math.sqrt(Math.log(this.nVisits + 1) / (child.nVisits + this.epsilon)) +
                    this.m_rnd.nextDouble() * this.epsilon;

            // small sampleRandom numbers: break ties in unexpanded nodes
            if (uctValue > bestValue) {
                selected = child;
                bestValue = uctValue;
            }

            /*if(this.parent == null)
                System.out.println("Child: " + child.childIdx + ", q: " + childValue + ", expl: " +                    (Math.sqrt(Math.log(this.nVisits + 1) / (child.nVisits + this.epsilon)) +
                            this.m_rnd.nextDouble() * this.epsilon) +
                    ", uct: " + uctValue);
            */
        }

        if (selected == null)
        {
            //throw new RuntimeException("Warning! returning null: " + bestValue + " : " + this.children.length);
            //System.out.println("Selected: null");
            return this.children[0];
        }
              /*
        double exploit = selected.totValue / (selected.nVisits + this.epsilon);
        double explore = Math.sqrt(Math.log(this.nVisits + 1) / (selected.nVisits + this.epsilon)) +
                this.m_rnd.nextDouble() * this.epsilon;
        if(this.parent == null)
            System.out.print("Selected: " + selected.childIdx + ", q: " + exploit + ", expl: " + explore +
                         ", uct: " + bestValue + " ... ");                                                */


        if(firstAction == -1)
            firstAction = selected.childIdx;

        return selected;
    }


    public double tunedRollOut(FitVectorSource vSource, NavFeatureSource fSource)
    {
        StateObservation rollerState = state.copy();
        int thisDepth = this.m_depth;

        HashMap<String, Double> features = null;

        while (!finishRollout(rollerState,thisDepth)) {

            //System.out.println(rolloutStates.size());
//rolloutStates.add(rollerState.copy());

            int action = roller.roll(rollerState);
            if(action >= 0)
            {
                rolloutActions.add(action);
                int prevEvCount = rollerState.getEventsHistory().size();
                double scorePrev = rollerState.getGameScore();

                rollerState.advance(Agent.actions[action]);
                //System.out.print(action);

                if(Config.USE_MEMORY)
                {
                    features  = roller.getFeatures().getFeatureVector(rollerState);
                    memory.addInformation(prevEvCount, scorePrev, rollerState, features);
                }

                if(Config.COMPUTE_HIT_MAP && !rollerState.isGameOver())
                {
                    Vector2d pos = new Vector2d(rollerState.getAvatarPosition().x / rollerState.getBlockSize(),
                            rollerState.getAvatarPosition().y / rollerState.getBlockSize());
                    //System.out.println(pos.x + " " + pos.y + "(" + thisDepth + ")");
                    SingleMCTSPlayer.m_hitsMap[(int)pos.x][(int)pos.y]++;
                }
            }

            thisDepth++;
        }

        if(rollerState.isGameOver() && rollerState.getGameWinner() == Types.WINNER.PLAYER_WINS)
        {
            m_num_victories++;
        }

        //System.out.print(",");

        //System.out.println("POSITION AT LEAF: " + rollerState.getAvatarPosition());

        int rolloutMoves = (thisDepth - this.m_depth);
        double percVectorUse = 1.0;
        //double percVectorUse = (double)rolloutMoves / Config.ROLLOUT_DEPTH;
        //double percVectorUse = Config.ROLLOUT_DEPTH /  ((double)rolloutMoves + 0.001); //this is the newInverseFitness

        double rawDelta = value(rollerState, fSource);

        //Discount factor:
        double accDiscount = Math.pow(Config.REWARD_DISCOUNT, thisDepth);
        double delta = rawDelta * accDiscount;


        if(rawDelta == HUGE_POSITIVE || rawDelta == HUGE_NEGATIVE)
        {
            memory.manageGameEnd(state, rollerState);
            if(Config.ES_TYPE != Config.HAND_TUNED_WEIGHTS)
            {
                //boolean improved = vSource.returnFitness(rolloutStates, rolloutActions, delta);
                double evo_delta = percVectorUse * delta;
                assignFitnessValue(rolloutMoves, evo_delta);
            }
            return delta;
        }

        if(delta < curBounds[0]) curBounds[0] = delta;
        if(delta > curBounds[1]) curBounds[1] = delta;

        double rawScoreEndPlayout = delta;

        double mctsScore = -1;
        double vectorFitness = -1;

        //Score is 0 and we want to use experience + curiosity:
        if(Config.SCORE_TYPE != Config.RAW_SCORE && (rawScoreEndPlayout == 0))
        {
            if(features == null)
                features  = roller.getFeatures().getFeatureVector();

            double memScoreEndPlayout = knowledgeValue(features); //score;
            vectorFitness = memScoreEndPlayout - memScoreBeginRollout;
            mctsScore = memScoreEndPlayout;
        }else{
            //We just use the score
            //vectorFitness = rawScoreEndPlayout - rawScoreBeginRollout;
            vectorFitness = rawScoreEndPlayout;
            mctsScore = rawScoreEndPlayout;
        }

        if(Config.ES_TYPE != Config.HAND_TUNED_WEIGHTS)
        {
            //vSource.returnFitness(rolloutStates, rolloutActions, vectorFitness);
            double evo_delta = percVectorUse * vectorFitness;
            assignFitnessValue(rolloutMoves, evo_delta);
        }
        return mctsScore;
    }

    private void assignFitnessValue(int rolloutMoves, double evo_delta)
    {
        if((rolloutMoves == 0) && (Config.ES_TYPE != Config.BANDIT))
            evo_delta = Double.NEGATIVE_INFINITY;

        /*if((rolloutMoves == 0) && (Config.ES_TYPE == Config.BANDIT))
        {
            int a = 0;
        }
        else */
        if((rolloutMoves != 0) || (Config.ES_TYPE != Config.BANDIT))
        {
            weightVectorFitness.add(evo_delta);
            all_fitness[cur_fit_it++] = evo_delta;
        }
    }

    public double knowledgeValue(HashMap<String, Double> features)
    {
        double infoGain = memory.getKnowledgeGain(features);
        double disc = memory.getDiscoverScore();
        return infoGain*0.66 + disc*0.33;
    }

    public double value(StateObservation a_gameState, NavFeatureSource fSource)
    {
        if(Config.FEATURES == Config.GVG_FEATURES)
        {
            return raw_value_function(a_gameState);
        }
        return fSource.valueFunction(a_gameState);
    }

    private double raw_value_function(StateObservation a_gameState)
    {
        boolean gameOver = a_gameState.isGameOver();
        Types.WINNER win = a_gameState.getGameWinner();

        if(gameOver && win == Types.WINNER.PLAYER_LOSES)
            return HUGE_NEGATIVE;

        if(gameOver && win == Types.WINNER.PLAYER_WINS)
            return HUGE_POSITIVE;

        double rawScore = a_gameState.getGameScore();
        rawScore = rawScore - rawScoreBeginPlayout;

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

    public void backUp(SingleTreeNode node, double result)
    {
        int numNodes = m_runList.size();
        for(int i = 0; i < numNodes; ++i)
        {
            SingleTreeNode stn = m_runList.get(i);
            stn.nVisits++;
            stn.totValue += result;

        }
    }

    public double meanValue() {
        return totValue / nVisits + epsilon;
    }


    public int bestBiasedAction() {

        if (roller != null && roller instanceof VariableFeatureWeightedRoller)
            return biasedRootAction(state, (VariableFeatureWeightedRoller) roller);

        return mostVisitedAction();
    }


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
    int getBiasedAction(FitVectorSource source, NavFeatureSource features, StateObservation gameState) {
        double[] best_wVector = source.bestVec();
        Random rand = new Random();
        int nActions = state.getAvailableActions().size();
        int nFeatures = features.getFeatureVectorAsArray(state).length;
        double[] bias = new double[nActions];

        double[] featureWeightVector = features.getFeatureVectorAsArray(gameState);

        int ix = 0; // used to step over params
        double tot = 0;
        for (int i=0; i<nActions; i++) {
            bias[i] = 0;

            for (int j=0; j<nFeatures; j++) {
                bias[i] += best_wVector[ix] * featureWeightVector[j];
                ix++;
            }

            // now replace with e^a[i]
            bias[i] = Math.exp(bias[i]);

            if(bias[i] == Double.POSITIVE_INFINITY)
                bias[i] = Double.MAX_VALUE;

            tot += bias[i];
        }

        // now track relative cumulative probability
        double x = rand.nextDouble();

        // an accumulator
        double acc = 0;
        int action = 0;
        for ( ; action<nActions; action++) {
            acc += bias[action] / tot;
            if (x < acc) return action;
        }
        if (action == nActions) {
            action = rand.nextInt(nActions);
        }
        return action;
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
}
