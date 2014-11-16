package controllers.sampleMCTS.learning;

import FastEvoMCTS.FeatureExtraction;
import controllers.sampleMCTS.*;
import core.game.StateObservation;
import ontology.Types;
import tools.Utils;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: ssamot
 * Date: 11/03/14
 * Time: 13:21
 * This is a Java port from Tom Schaul's VGDL - https://github.com/schaul/py-vgdl
 */
public class SingleTreeNodeLearning extends SingleTreeNode {

    private  HashMap<Integer, Types.ACTIONS> action_mapping;
    private HashMap<Types.ACTIONS, Integer> r_action_mapping;
    private int N_ACTIONS;


    private static ArrayRealVector theta[];
    private static ArrayRealVector min;
    private static ArrayRealVector max;

    private static ArrayRealVector min_;
    private static ArrayRealVector scale_;
    private static Integer cacheSize;
    private final static double epsilon = 0.3;
    private final static double learning_rate = 0.1;
    public static boolean initialised = false;






    public SingleTreeNodeLearning(StateObservation state, SingleTreeNode parent, Random rnd) {
        super(state,parent,rnd);
        //System.out.println("state = " + state);

        init();

    }


    private void init() {

        //System.out.println("init");

        action_mapping = new HashMap<Integer, Types.ACTIONS>();
        r_action_mapping = new HashMap<Types.ACTIONS, Integer>();
        int i = 0;
        for (Types.ACTIONS action : state.getAvailableActions()) {
            action_mapping.put(i, action);
            r_action_mapping.put(action, i);
            i++;
        }

        N_ACTIONS = state.getAvailableActions().size();


    }

    private void updateFeatureMinMax(double[] features) {

        if(min == null) {
            min = new ArrayRealVector(features, true);
            max = new ArrayRealVector(features, true);
        }
        else {



        for(int i = 0 ; i < features.length; i++) {
            min.setEntry(i, Math.min(features[i], min.getEntry(i)));
            max.setEntry(i, Math.max(features[i], max.getEntry(i)));
        }
        }


        ArrayRealVector data_range = max.subtract(min);

        for(int i = 0 ; i < features.length; i++) {
            double value = data_range.getEntry(i);
            if(value == 0.0) {
                data_range.setEntry(i, 1.0);
            }
        }


        scale_ = new ArrayRealVector(features.length, 1.0).ebeDivide(data_range);
        min_ = new ArrayRealVector(features.length, 1.0).subtract( min.ebeMultiply(scale_) );
    }


    private ArrayRealVector getScaledFeatureVector(double[] features) {
        
        ArrayRealVector X = new ArrayRealVector(features);
        //System.out.println("X = " + X);
        
        X = X.ebeMultiply(scale_);
        X = X.add(min_);
        return X;

    }
    
    private int predictAction(ArrayRealVector state, double epsilon) {



        if(m_rnd.nextDouble() < epsilon) {
            return m_rnd.nextInt(N_ACTIONS);
        }
        else {

            double maxScore = Double.NEGATIVE_INFINITY;
            int argmax = -1;

            for (int i = 0; i < N_ACTIONS; i++) {
                double score = state.dotProduct(theta[i]);
                if(score > maxScore) {
                    argmax = i;
                    maxScore = score;
                }
            }

            return argmax;
        }
        
    }


    private double[] secondorderpolynomial(double[] features) {

        if(cacheSize == null) {
            //so = new ArrayList(Arrays.asList(FastEvoMCTS));
            cacheSize = features.length;
            for (int i = 0; i < features.length; i++) {
                for (int j = i; j < features.length; j++) {
                    cacheSize+=1;
                    //System.out.println("cacheSize = " + cacheSize);
                }
            }

            cacheSize+=1;
            //System.out.println("cacheSize = " + cacheSize);

            if(theta == null) {
                theta = new ArrayRealVector[N_ACTIONS];

                for(int i = 0 ; i < N_ACTIONS; i++) {
                    theta[i] = new ArrayRealVector(cacheSize, 0.0001);
                }
            }
        }

        double[] so  = new double[cacheSize];
        int f = 0;
        for (; f < features.length; f++) {
            so[f] = features[f];
        }

        //System.out.println("f = " + f);
        //System.out.println("FastEvoMCTS.length = " + FastEvoMCTS.length);


        for (int i = 0; i < features.length; i++) {
            for (int j = i; j < features.length; j++) {
                so[f] = features[i]*features[j];
                //System.out.println("f = " + f);
                f++;
            }
        }

        so[f] = 1.0;
        return so;

    }



    private void learnLinear(ArrayList<ArrayRealVector> states, ArrayList<Integer> actions, double reward) {
        double normReward = Utils.normalise(reward, bounds[0], bounds[1]);
        for (int i = 0; i <states.size() ; i++) {
            ArrayRealVector a_theta = theta[actions.get(i)];
            double Q_value = a_theta.dotProduct(states.get(i));
            double mc_error = normReward - Q_value;
            for (int j = 0; j < a_theta.getDimension(); j++) {
                double oldValue = a_theta.getEntry(j);
                a_theta.setEntry(j, oldValue + learning_rate * oldValue * mc_error);
                //System.out.println("reward = " + normReward);
                //System.out.println("Q_value = " + Q_value);
                //System.out.println("a_theta = " + a_theta);
            }
        }

        //System.out.println("theta = " + theta + " reward = " + normReward);
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
        nextState.advance(controllers.sampleMCTS.Agent.actions[bestAction]);

        SingleTreeNode tn = new SingleTreeNodeLearning(nextState, this, this.m_rnd);
        children[bestAction] = tn;
        return tn;

    }


    public double rollOut()
    {

        if(!initialised) {
            initialised = true;
        }


        StateObservation rollerState = state.copy();
        int thisDepth = this.m_depth;


        ArrayList<ArrayRealVector> states = new ArrayList<ArrayRealVector>();
        ArrayList<Integer> actions = new ArrayList<Integer>();

        while (!finishRollout(rollerState,thisDepth)) {


            FeatureExtraction fe = new FeatureExtraction(rollerState);
            double[] features = fe.getFeatureVectorAsArray();
            features = secondorderpolynomial(features);
            updateFeatureMinMax(features);
            ArrayRealVector f_state = getScaledFeatureVector(features);

            //System.out.println("f_state = " + f_state);

            int action = predictAction(f_state, epsilon);
            //System.out.println("action = " + action);


            rollerState.advance(action_mapping.get(action));
            thisDepth++;

            states.add(f_state);
            actions.add(action);


        }

        //learning SARSA(1)



        double delta = value(rollerState);

        if(delta < bounds[0]) bounds[0] = delta;
        if(delta > bounds[1]) bounds[1] = delta;

        //double normDelta = Utils.normalise(delta, lastBounds[0], lastBounds[1]);

        learnLinear(states,actions, delta);

        return delta;
    }
}
