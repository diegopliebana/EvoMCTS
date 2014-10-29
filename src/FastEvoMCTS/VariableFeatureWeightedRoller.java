package FastEvoMCTS;

import core.game.StateObservation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.TreeSet;

public class VariableFeatureWeightedRoller implements TunableRoller {

    public boolean uniform  = false;

    public ArrayList<String> newFeatures;
    public HashMap<String, Integer> featuresMap;

    public double[] params;
    double[] bias;

    int nActions;
    int nFeatures;

    static Random rand;
    FeatureExtraction features;

    public VariableFeatureWeightedRoller(StateObservation state, FeatureExtraction features, Random rnd) {
        // assumes that all states have the same number of
        // actions and will not work for some games
        rand = rnd;
        this.features = features;
        init(state, features);
    }

    @Override
    public void init(StateObservation state, FeatureExtraction features)
    {
        nActions = state.getAvailableActions().size();
        nFeatures = features.getFeatureVectorAsArray(state).length;
        bias = new double[nActions];
        params = new double[nActions*nFeatures];
    }

    @Override
    public FeatureExtraction getFeatures() {
        return features;
    }

    public double[] getFeatureWeights(StateObservation gameState)
    {
        double weights[] = new double[params.length];

        //HashMap<String, Double> allFeatures = features.getFeatureVector(gameState);
        String[] keys = features.getFeatureVectorKeys(gameState); //(String[]) allFeatures.keySet().toArray();

        //Check for a new unknown feature.
        for(String k : keys)
        {
            if(!featuresMap.containsKey(k))
            {
                if(!newFeatures.contains(k))
                    newFeatures.add(k); //don't add it more than once.
            }else
            {
                int pos = featuresMap.get(k);
                for(int actIdx = 0; actIdx < nActions; actIdx++)
                {
                    int weightPos = pos*nActions + actIdx;
                    if(weightPos < weights.length)  //this shouldn't be necessary...
                        weights[weightPos] = params[weightPos];
                }

            }
        }

        return weights;
    }

    public int roll(StateObservation gameState) {
        if (uniform) return rand.nextInt(nActions);
        double[] featureWeightVector = getFeatureWeights(gameState);

        // System.out.println(Arrays.toString(FastEvoMCTS));
        int ix = 0; // used to step over params
        double tot = 0;
        for (int i=0; i<nActions; i++) {
            bias[i] = 0;

            for (int j=0; j<nFeatures; j++) {
                bias[i] += params[ix] * featureWeightVector[j];
                ix++;
            }

            // now replace with e^a[i]
            bias[i] = Math.exp(bias[i]);
            tot += bias[i];
        }
        // now track relative cumulative probability
        // System.out.println(Arrays.toString(bias));
        double x = rand.nextDouble();

        // an accumulator
        double acc = 0;
        int action = 0;
        for ( ; action<nActions; action++) {
            acc += bias[action] / tot;
            if (x < acc) return action;
        }
        if (action == nActions) {
//            System.out.println("Oops: Softmax Failure: " + action);
//            System.out.println(Arrays.toString(params));
            // System.out.println();
            action = rand.nextInt(nActions);
        }
        return action;
    }


    public double[] getBiases (StateObservation gameState) {
        double[] biases = new double[bias.length];
        // uniform = true;
        // if (uniform || true) return biases;
        if (uniform) return biases;
        //double[] featureVector = features.getFeatureVectorAsArray(gameState);
        double[] featureWeightVector = getFeatureWeights(gameState);
        // System.out.println(Arrays.toString(FastEvoMCTS));
        int ix = 0; // used to step over params
        double tot = 0;
        for (int i=0; i<nActions; i++) {
            bias[i] = 0;

            for (int j=0; j<nFeatures; j++) {
                bias[i] += params[ix] * featureWeightVector[j];
                ix++;
            }
            // now replace with e^a[i]
            bias[i] = Math.exp(bias[i]);
            tot += bias[i];
        }
        for (int i=0; i<biases.length; i++) {
            biases[i] = bias[i] / tot;
        }
        return biases;
    }

    @Override
    public int nDim() {
        return nActions * nFeatures;
    }

    @Override
    public void setParams(HashMap<String, Integer> featuresMap, double[] w) {

        this.featuresMap = featuresMap;
        this.newFeatures = new ArrayList<String>();

        nFeatures = featuresMap.size();
        params = new double[nDim()];
        for (int i=0; i<nDim(); i++)
            params[i] = w[i];
    }

    @Override
    public String[] featureNames(StateObservation state) {

        return features.getFeatureVectorKeys(state);
    }


    @Override
    public boolean newFeaturesFound() {
        return this.newFeatures.size() > 0;
    }

    @Override
    public ArrayList<String> getNewFeatures() {
        return this.newFeatures;
    }

}
