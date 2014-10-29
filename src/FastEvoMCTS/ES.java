package FastEvoMCTS;

import java.util.HashMap;
import java.util.Random;

/**
 * Created by Diego on 20/03/14.
 * (1+1)-ES
 */
public class ES extends FitVectorSource
{

    public int popSize = 10;

    double sDev = 1;
    double initNoiseDev = 0.1;
    static double noiseFac = 1.02;

    double lastNoiseDev;
    int lastInit;

    double[][] pop;
    double[] fitness;
    double noiseDev[];
    double worstScore;

    Memory memory;

    public ES(String[] features, int order,
              int nActions, Memory memory, Random rnd) {
        this.memory = memory;
        this.nFeatures = 0;
        this.nActions = nActions;
        this.order = order;
        this.rand = rnd;
        namesGenomeMapping = new HashMap<String, Integer>();
        bestScore = Double.NEGATIVE_INFINITY * order;
        lastInit = -1;

        for(String f : features)
        {
            addFeature(f, false);
        }

        noiseDev = new double[popSize];
        bestYet = new double[ nDim() ];
        fitness = new double[popSize];
        pop = new double[popSize][bestYet.length];

        for (int i=0; i< pop.length; i++)
        {
            noiseDev[i] = initNoiseDev;
            for (int j=0; j<bestYet.length; j++)
                pop[i][j] = nextRandom();
        }

        for (int i=0; i<  bestYet.length; i++)
            bestYet[i] = nextRandom();


    }

    @Override
    public double[] getNext(String[] features) {

        if(lastInit+1 < popSize)
        {
            lastInit++;
            proposed = pop[lastInit];
            return proposed;
        }

        //Else, all initial population has been evaluated, need to recombine and create more

        int parent1 = rand.nextInt(popSize);
        int parent2 = rand.nextInt(popSize);
        while(parent1 == parent2)
            parent2 = rand.nextInt(popSize);

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
                if(rand.nextDouble() >= 0.5)
                {
                    lastNoiseDev =  noiseDev[parent1];
                    proposed[i++] = pop[parent1][weightPos] + rand.nextGaussian() * noiseDev[parent1];
                }else{
                    lastNoiseDev =  noiseDev[parent2];
                    proposed[i++] = pop[parent2][weightPos] + rand.nextGaussian() * noiseDev[parent2];
                }

            }
        }
        return proposed;
    }


    @Override
    public void returnFitness(double fitnessValue) {
        nEvals++;

        boolean success = false;
        boolean newWorst = false;
        int indIdx = lastInit;

        if (order * fitnessValue >= bestScore * order) {
            bestYet = proposed;
            bestScore = fitnessValue;
            success = true;
        }

        if (order * fitnessValue < worstScore * order)
        {
            newWorst = true;
            worstScore = fitnessValue;
        }

        if(nEvals-1 < popSize)
            fitness[lastInit] = fitnessValue;
        else
        {
            if(!newWorst)
            {
                //We need to replace the worst individual with the proposed one.
                double wFit = Double.POSITIVE_INFINITY * order;
                int wIdx = -1;

                for (int i=0; i< fitness.length; i++)
                {
                    if(order*fitness[i] <= wFit*order)
                    {
                        wIdx = i;
                        wFit = fitness[i];
                    }
                }

                pop[wIdx] = proposed;
                fitness[wIdx] = fitnessValue;
                worstScore = wFit;
                noiseDev[wIdx] = lastNoiseDev;
                indIdx = wIdx;

            }
        }

        if(success)
            noiseDev[indIdx] *= noiseFac;   // success so increase the noiseDev
        else
            noiseDev[indIdx] /= noiseFac;   // failure so decrease noiseDev


    }


    protected double nextRandom()
    {
        return rand.nextGaussian() * sDev;
    }

    protected void addFeature(String featureName, boolean updateIndividuals)
    {
        super.addFeature(featureName,updateIndividuals);

        if(updateIndividuals)
            updatePopulation();

        if(Config.USE_MEMORY)
            memory.addInfoType(featureName);
    }

    private void updatePopulation()
    {
        double [][]population = new double[popSize][nDim()];
        for (int i=0; i< pop.length; i++)
        {
            //copy the old best one
            System.arraycopy(pop[i], 0, population[i], 0, pop[i].length );

            //Add new weights for the new feature, one per action:
            for(int actIdx = pop[i].length; actIdx < population[i].length; actIdx++)
            {
                population[i][actIdx] = nextRandom();     //init randomly.
            }
        }

        pop = population;
    }



}
