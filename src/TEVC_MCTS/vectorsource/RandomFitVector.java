package TEVC_MCTS.vectorsource;

import TEVC_MCTS.Config;
import TEVC_MCTS.utils.Memory;
import core.game.StateObservation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 * Created by Diego on 20/03/14.
 * (1+1)-ES
 */
public class RandomFitVector extends FitVectorSource
{
    Memory memory;

    public RandomFitVector(String[] features, int order,
                           int nActions, Memory memory, Random rnd) {
        this.memory = memory;
        this.nFeatures = 0;
        this.nActions = nActions;
        this.order = order;
        this.rand = rnd;
        namesGenomeMapping = new HashMap<String, Integer>();
        bestScore = Double.NEGATIVE_INFINITY * order;

        for(String f : features)
        {
            addFeature(f, false);
        }

        bestYet = new double[ nDim() ];
        for (int i=0; i<  bestYet.length; i++)
            bestYet[i] = nextRandom();
    }

    protected void addFeature(String featureName, boolean updateBestInd)
    {
        super.addFeature(featureName,updateBestInd);

        if(Config.USE_MEMORY)
            memory.addInfoType(featureName);
    }


    @Override
    public boolean returnFitness(ArrayList<StateObservation> states,
                              ArrayList<Integer> actions,
                              double fitness) {
        //Nothing to do here.
        return false;
    }

    @Override
    public double[] getNext(String[] features) {

        proposed = new double[nDim()];
        int i=0;

        for(String feature : features)
        {
            if(!namesGenomeMapping.containsKey(feature))
            {
                //add the feature if we have never seen this one.
                addFeature(feature, true);

                //We need to create a new proposed array.
                double[] newProposed = new double[nDim()];
                System.arraycopy(proposed,0,newProposed,0,proposed.length);
                proposed = newProposed;
            }

            for(int actIdx = 0; actIdx < nActions; actIdx++)
            {
                //calculate a new one, and place it in the same position as its feature name.
                proposed[i++] = rand.nextGaussian();
            }
        }
        return proposed;
    }



}
