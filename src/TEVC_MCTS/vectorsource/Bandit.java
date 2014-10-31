package TEVC_MCTS.vectorsource;

import TEVC_MCTS.utils.Memory;
import core.game.StateObservation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 * Created by Diego on 29/10/14.
 */
public class Bandit extends FitVectorSource{

    public double K = Math.sqrt(2);
    public double epsilon = 1e-6;
    public int popSize = 5;
    double[][] pop;
    double[] fitness;
    int[] n;
    int bigN;
    Random m_rnd;
    int lastSelected;

    public Bandit(String[] features, int order,
              int nActions, Memory memory, Random rnd) {

        m_rnd = new Random();
        namesGenomeMapping = new HashMap<String, Integer>();
        bigN = 0;
        this.nFeatures = 0;
        lastSelected = -1;
        this.nActions = nActions;
        this.order = order;
        this.rand = rnd;

        for(String f : features)
        {
            addFeature(f, false);
        }

        bestYet = new double[ nDim() ];
        fitness = new double[popSize];
        n = new int[popSize];
        pop = new double[popSize][bestYet.length];

        for (int i=0; i< pop.length; i++)
        {
            for (int j=0; j<bestYet.length; j++)
                pop[i][j] = nextRandom();
        }

       /* for (int i=0; i< pop.length-1; i++)
        {
            for (int j=0; j<bestYet.length; j++)
                pop[i][j] = nextRandom();
        }

        pop[pop.length-1][0] = -1;
        pop[pop.length-1][1] = 1;*/


        for (int i=0; i<  bestYet.length; i++)
            bestYet[i] = nextRandom();

    }




    @Override
    public double[] getNext(String[] features) {

        double bestValue = -Double.MAX_VALUE;
        lastSelected = -1;
        for(int i = 0; i < pop.length; ++i)
        {

            double q =  fitness[i] / (n[i] + this.epsilon);

            double uctValue = q +
                    K * Math.sqrt(Math.log(bigN + 1) / (n[i] + this.epsilon)) +
                    this.m_rnd.nextDouble() * this.epsilon;//tiebreaker

            if (uctValue > bestValue) {
                lastSelected = i;
                bestValue = uctValue;
            }
        }

        return pop[lastSelected];
    }

    @Override
    public boolean returnFitness(ArrayList<StateObservation> states, ArrayList<Integer> actions, double fitnessVal) {
        fitness[lastSelected] += fitnessVal;
        bigN++;
        n[lastSelected]++;
        return false;
    }
}
