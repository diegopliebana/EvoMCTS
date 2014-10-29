package FastEvoMCTS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public abstract class FitVectorSource {

    protected HashMap<String, Integer> namesGenomeMapping;

    int nFeatures;
    int nActions;
    int order;
    int nEvals = 0;
    double[] bestYet;
    double[] proposed;
    double bestScore;


    static Random rand;
    public static int MAX_BEST = 1;
    public static int MIN_BEST = -1;


    public abstract void returnFitness(double fitness);
    public abstract double[] getNext(String[] features);
    protected abstract double nextRandom();


    public void addFeatures(ArrayList<String> newFeatures) {
        for(String feature : newFeatures)
        {
            addFeature(feature, true);
        }
    }

    protected void addFeature(String featureName, boolean updateIndividuals)
    {
        int newPosition = nFeatures;                        //we put the new feature at the last position.
        nFeatures++;                                        //and we need one more element
        namesGenomeMapping.put(featureName, newPosition);   //save the reference of where this feature goes.

        if(updateIndividuals)
        {
            double newBestYet[] = new double[nDim()];

            //copy the old best one
            System.arraycopy(bestYet, 0, newBestYet, 0, bestYet.length );

            //Add new weights for the new feature, one per action:
            for(int actIdx = bestYet.length; actIdx < newBestYet.length; actIdx++)
            {
                newBestYet[actIdx] = nextRandom();     //init randomly.
            }

            bestYet = newBestYet;                                //assign bestYet.
        }
    }

    public HashMap<String, Integer> getGenMapping() { return namesGenomeMapping; }
    public boolean exists(String feature) { return namesGenomeMapping.containsKey(feature); }
    public double[] bestVec() { return bestYet; }
    public double bestScore() { return bestScore; }
    public int nDim() { return nFeatures*nActions; }
}
