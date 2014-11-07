package TEVC_MCTS.vectorsource;

import TEVC_MCTS.utils.Memory;
import core.game.StateObservation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 * Created by Diego on 29/10/14.
 */
public class CMAES extends FitVectorSource{

    public CMAES(String[] features, int order,
                 int nActions, Memory memory, Random rnd) {

        /*** LEAVE THIS AS IT IS ***/
        namesGenomeMapping = new HashMap<String, Integer>();
        this.nFeatures = 0;
        this.nActions = nActions;
        this.order = order;
        this.rand = rnd;

        for(String f : features)
        {
            addFeature(f, false);
        }
        bestYet = new double[ nDim() ];

        /******************************/
        //ADD STUFF FROM THIS POINT ON.






        //This is quite optional
        for (int i=0; i<  bestYet.length; i++)
            bestYet[i] = nextRandom();
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
                //Position of this weight in the array of weights.
                int weightPos = featurePosition*nActions + actIdx;


                /** MODIFY THIS TO GIVE A VALUE TO THIS WEIGHT **/
                proposed[i] = rand.nextGaussian();



                i++;
            }
        }

        return proposed;
    }

    @Override
    public boolean returnFitness(ArrayList<StateObservation> states, ArrayList<Integer> actions, double fitnessVal) {
        //fitnessVal is the fitness value for this weight vector.
        //States and actions are the sequence of states and actions found and used during the rollout.



        return false; //no worries about the return, it's used for debug
    }
}
