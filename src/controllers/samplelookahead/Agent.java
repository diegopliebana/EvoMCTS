package controllers.samplelookahead;


import controllers.Heuristics.SimpleStateHeuristic;
import controllers.Heuristics.StateHeuristic;
import controllers.Heuristics.WinScoreHeuristic;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;

import java.util.Random;
import java.util.concurrent.TimeoutException;

/**
 * Created with IntelliJ IDEA.
 * User: ssamot
 * Date: 14/11/13
 * Time: 21:45
 * This is a Java port from Tom Schaul's VGDL - https://github.com/schaul/py-vgdl
 */
public class Agent extends AbstractPlayer {

    /**
     * Random generator for the agent.
     */
    private Random randomGenerator;
    private long timeDue;
    private static double GAMMA = 0.90;
    private static long BREAK_MS = 30;
    private ElapsedCpuTimer timer;


    /**
     * Public constructor with state observation and time due.
     * @param stateObs state observation of the current game.
     * @param elapsedTimer Timer for the controller creation.
     */
    public Agent(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {

        timer = elapsedTimer;

        try {
            Types.ACTIONS action = nestedMonteCarlo(stateObs, 3, new SimpleStateHeuristic(stateObs),10);
        } catch (TimeoutException e) {
            System.out.println("Timed out - just testing");
            //e.printStackTrace();
        }


    }


    private double nestedMonteCarloScores(StateObservation stateObs, int maxdepth, StateHeuristic heuristic, int depth) throws TimeoutException {

        //System.out.println("depth" + depth);
        long remaining = timer.remainingTimeMillis();
        if(remaining < BREAK_MS){
            //System.out.println(remaining);
            throw new TimeoutException("Timeout");
        }

        if (depth >= maxdepth || stateObs.isGameOver() == true) {
            //System.out.println(remaining + " radfadf " + depth);
            return heuristic.evaluateState(stateObs);

        } else {
            double score = Double.NEGATIVE_INFINITY;

            for (Types.ACTIONS action : stateObs.getAvailableActions()) {
                StateObservation stCopy = stateObs.copy();
                stCopy.advance(action);
                //System.out.println("Internal " + action);
                score = Math.pow(GAMMA, depth) * Math.max(score, nestedMonteCarloScores(stCopy, maxdepth, heuristic, depth + 1));
            }

            return score;

        }


    }


    private Types.ACTIONS nestedMonteCarlo(StateObservation stateObs, int maxdepth, StateHeuristic heuristic, int iterations) throws TimeoutException{

        Types.ACTIONS maxAction = null;
        double maxScore = Double.NEGATIVE_INFINITY;
        for (Types.ACTIONS action : stateObs.getAvailableActions()) {

            double score = 0;
            for (int i = 0; i < iterations; i++) {
                StateObservation stCopy = stateObs.copy();
                stCopy.advance(action);
                score += nestedMonteCarloScores(stCopy, maxdepth, heuristic, 0)+ randomGenerator.nextDouble()*0.00001;;
            }

            System.out.println("Action:" + action + " score:" + score);
            if (score > maxScore) {
                maxScore = score;
                maxAction = action;
            }


        }

        System.out.println("====================");
        return maxAction;

    }

    /**
     * Picks an action. This function is called every game step to request an
     * action from the player.
     *
     * @param stateObs Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
     * @return An action for the current state
     */
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {




        this.timer = elapsedTimer;

        Types.ACTIONS lastGoodAction = null;
        for (int maxDepth = 1; maxDepth < 100; maxDepth++) {

            try {
                lastGoodAction = nestedMonteCarlo(stateObs, maxDepth, new WinScoreHeuristic(stateObs), 1);

            } catch (TimeoutException e) {
                System.out.println();
                System.out.println("Action:" + lastGoodAction + ", " + (timeDue - System.currentTimeMillis()) + "maxDepth" + maxDepth);
                return lastGoodAction;
            }
            //long remaining = timeDue - System.currentTimeMillis();
            //System.out.println(action + " " + remaining + " ms" + maxDepth);
        }


        return null;
    }


}
