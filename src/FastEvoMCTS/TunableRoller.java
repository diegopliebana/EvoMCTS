package FastEvoMCTS;

import core.game.StateObservation;

import java.util.ArrayList;
import java.util.HashMap;

public interface TunableRoller{
    int nDim();
    void setParams(HashMap<String, Integer> featuresMap, double[] w);
    int roll(StateObservation gameState);
    String[] featureNames(StateObservation state);
    void init(StateObservation state, FeatureExtraction features);
    FeatureExtraction getFeatures();

    boolean newFeaturesFound();
    ArrayList<String> getNewFeatures();
}
