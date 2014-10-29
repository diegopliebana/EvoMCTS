package FastEvoMCTS;

import java.util.HashMap;
import java.util.Random;

/**
 * Created by Diego on 20/03/14.
 * (1+1)-ES
 */
public class VariableRandomMutHillClimber extends FitVectorSource
{
    double sDev = 1;
    double noiseDev = 0.1;
    static double noiseFac = 1.02;

    Memory memory;

    public VariableRandomMutHillClimber(String[] features, int order,
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

    protected double nextRandom()
    {
        return rand.nextGaussian() * sDev;
    }

    protected void addFeature(String featureName, boolean updateBestInd)
    {
        super.addFeature(featureName,updateBestInd);

        if(Config.USE_MEMORY)
            memory.addInfoType(featureName);
    }


    @Override
    public void returnFitness(double fitness) {
        nEvals++;
        if (order * fitness >= bestScore * order) {
            bestYet = proposed;
            bestScore = fitness;
            // success so increase the noiseDev
            noiseDev *= noiseFac;
        } else {
            // failure so decrease noiseDev
            noiseDev /= noiseFac;
        }
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

            //get the position in bestYet where its gene is located.
            int featurePosition = namesGenomeMapping.get(feature);

            for(int actIdx = 0; actIdx < nActions; actIdx++)
            {
                int weightPos = featurePosition*nActions + actIdx;

                //calculate a new one, and place it in the same position as its feature name.
                proposed[i++] = bestYet[weightPos] + rand.nextGaussian() * noiseDev;
            }
        }
        return proposed;
    }



}
