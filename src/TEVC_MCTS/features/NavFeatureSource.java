package TEVC_MCTS.features;

import TEVC_MCTS.pathfinder.Node;
import TEVC_MCTS.utils.Memory;
import TEVC_MCTS.pathfinder.Astar;
import TEVC_MCTS.pathfinder.Navigable;
import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import tools.Vector2d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Iterator;

/**
 * Created by diego on 11/10/14.
 */
public abstract class NavFeatureSource extends Navigable
{

    public abstract LinkedHashMap<String, Double> getFeatureVector();
    protected abstract void calcFeatures(StateObservation stateObs);
    public abstract double valueFunction(StateObservation stateObs);
    public abstract double[] getHandTunedWeights(StateObservation stateObs);

    /** USEFUL FUNCTIONS FOR FEATURE MANIPULATION **/
    protected void addFeaturesFromMap(HashMap<Integer, Double> map, HashMap<String, Double> features, String prefix)
    {
        Iterator<Integer> keys = map.keySet().iterator();
        while(keys.hasNext())
        {
            int itype = keys.next();
            String hashKey = prefix + itype;
            features.put(hashKey, map.get(itype));
        }

    }

    protected double[] getFeatureVectorAsArray()
    {
        HashMap<String, Double> featuresMap = getFeatureVector();
        double[] features = new double[featuresMap.size()];

        Iterator<Double> itF = featuresMap.values().iterator();
        int i = 0;
        while(itF.hasNext())
            features[i++] = itF.next();

        return features;
    }


    protected String[] getFeatureVectorKeys()
    {
        HashMap<String, Double> featuresMap = getFeatureVector();
        String[] featureNames = new String[featuresMap.size()];
        featuresMap.keySet().toArray(featureNames);

        return featureNames;
    }


    public HashMap<String, Double> getFeatureVector(StateObservation state)
    {
        calcFeatures(state);
        return getFeatureVector();
    }

    public double[] getFeatureVectorAsArray(StateObservation state)
    {
        calcFeatures(state);
        return getFeatureVectorAsArray();
    }

    public String[] getFeatureVectorKeys(StateObservation state)
    {
        calcFeatures(state);
        return getFeatureVectorKeys();
    }
}
