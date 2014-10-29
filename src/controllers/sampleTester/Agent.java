package controllers.sampleTester;

import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 * Created by Diego on 30/08/14.
 */
public class Agent extends AbstractPlayer{

    public ArrayList<Types.ACTIONS> actions;
    public HashMap<Types.ACTIONS, StateObservation> nextStates;
    public Random randomGenerator;

    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer)
    {
        actions = so.getAvailableActions();
        nextStates = new HashMap<Types.ACTIONS, StateObservation>(actions.size());
        randomGenerator = new Random();
    }


    @Override
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {

        actions = stateObs.getAvailableActions();
        for(Types.ACTIONS action : actions)
        {
            StateObservation nextState = stateObs;
            nextState.advance(action);
            nextStates.put(action, nextState);
        }

        int index = randomGenerator.nextInt(actions.size());
        return actions.get(index);
    }
}
