package TEVC_MCTS.roller;

import TEVC_MCTS.features.NavFeatureSource;
import core.game.StateObservation;

import java.util.ArrayList;
import java.util.HashMap;

public interface TunableRoller{
    int nDim();
    void setParams(HashMap<String, Integer> featuresMap, double[] w);
    int roll(StateObservation gameState);
    String[] featureNames(StateObservation state);
    void init(StateObservation state, NavFeatureSource features);
    NavFeatureSource getFeatures();

    boolean newFeaturesFound();
    ArrayList<String> getNewFeatures();
}
