package TEVC_MCTS.roller;

import java.util.Random;

/**
 * Created by Diego on 29/10/14.
 */
public class BiasTest
{
    public static void main(String args[])
    {
        double[] best_wVector = new double[]{-1.0,1.0};
        Random rand = new Random();
        int nActions = 2;
        int nFeatures = 1;
        double[] bias = new double[nActions];

        double[] featureWeightVector = new double[]{400.0};

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

        for (int i=0; i<nActions; i++) {
            System.out.println(bias[i]);
        }

        // now track relative cumulative probability
        double x = rand.nextDouble();

        // an accumulator
        double acc = 0;
        int action = 0;
        for ( ; action<nActions; action++) {
            acc += bias[action] / tot;
            if (x < acc) System.out.println(action);
        }
        if (action == nActions) {
            action = rand.nextInt(nActions);
        }

        System.out.println(action);
    }
}
