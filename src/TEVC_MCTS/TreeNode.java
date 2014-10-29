package TEVC_MCTS;

import TEVC_MCTS.features.NavFeatureSource;
import TEVC_MCTS.roller.TunableRoller;
import TEVC_MCTS.roller.VariableFeatureWeightedRoller;
import TEVC_MCTS.vectorsource.FitVectorSource;
import core.game.StateObservation;
import tools.ElapsedCpuTimer;

/**
 * Created by diego on 23/09/14.
 */
public abstract class TreeNode {

    public StateObservation state;
    public int childIdx;


    abstract void mctsSearch(ElapsedCpuTimer elapsedTimer, TunableRoller roller, FitVectorSource source);

    abstract int bestBiasedAction();

    abstract int biasedRootAction(StateObservation state, VariableFeatureWeightedRoller roller);

    abstract int mostVisitedAction();

    abstract int getBiasedAction(FitVectorSource source, NavFeatureSource features, StateObservation gameState);

    abstract int bestAction();
}
