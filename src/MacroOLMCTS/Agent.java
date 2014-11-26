package MacroOLMCTS;

import MacroOLMCTS.macro.ConstantMacroFeed;
import MacroOLMCTS.macro.IMacroFeed;
import MacroOLMCTS.macro.MacroAction;
import MacroOLMCTS.macro.RandomNMacroFeed;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: ssamot
 * Date: 14/11/13
 * Time: 21:45
 * This is a Java port from Tom Schaul's VGDL - https://github.com/schaul/py-vgdl
 */
public class Agent extends AbstractPlayer {

    public static int NUM_ACTIONS;
    public static int MCTS_ITERATIONS = 100;
    public static double K = Math.sqrt(2);
    public static double REWARD_DISCOUNT = 1.00;

    public static int ROLLOUT_DEPTH = 30; //10;
    public static int MACROACTION_LENGTH = 5;

    //public static Types.ACTIONS[] actions;

    public static MacroAction[] actions;
    public MacroAction currentMacro;
    public IMacroFeed macroFeed;

    protected SingleMCTSPlayer mctsPlayer;

    /**
     * Public constructor with state observation and time due.
     * @param so state observation of the current game.
     * @param elapsedTimer Timer for the controller creation.
     */
    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer)
    {
        //Get the actions in a static array.
        ArrayList<Types.ACTIONS> act = so.getAvailableActions();
        actions = new MacroAction[act.size()];
        NUM_ACTIONS = actions.length;

        //macroFeed = new ConstantMacroFeed(3);
        macroFeed = new RandomNMacroFeed(5);

        setNewActions(act);

        //Create the player.
        mctsPlayer = getPlayer(so, elapsedTimer);
    }

    public void setNewActions(IMacroFeed macroFeed, StateObservation so)
    {
        this.macroFeed = macroFeed;
        this.setNewActions(so.getAvailableActions());
    }


    public void setNewActions(ArrayList<Types.ACTIONS> act)
    {
        MACROACTION_LENGTH = macroFeed.getNextLength();
        if(act.size() > 0) for(int i = 0; i < actions.length; ++i)
        {
            Types.ACTIONS singleAction = act.get(i);
            Types.ACTIONS[] actionsArray = new Types.ACTIONS[MACROACTION_LENGTH];

            for(int j = 0; j < MACROACTION_LENGTH; ++j)
            {
                actionsArray[j] = singleAction;
            }

            actions[i] = new MacroAction(actionsArray);
        }
        //System.out.print(actions[0].actions.length + ",");
    }

    public SingleMCTSPlayer getPlayer(StateObservation so, ElapsedCpuTimer elapsedTimer) {
        return new SingleMCTSPlayer(new Random());
    }


    /**
     * Picks an action. This function is called every game step to request an
     * action from the player.
     * @param stateObs Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
     * @return An action for the current state
     */
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer)
    {
        //System.out.println("#########");
        //Advance to the starting point after the current macro-action is executed.
        if(currentMacro != null)
        {
            int curpos=stateObs.getGameTick();
            MacroAction macroHelp = currentMacro.copy();
            while(!macroHelp.isFinished())
            {
                stateObs.advance(macroHelp.next());
            }
            //System.out.println("Advancing game state from " + curpos + " to " + stateObs.getGameTick());
        }

        //Reset the tree if no current macro-action or it is finished.
        if(currentMacro == null || currentMacro.cursor==1)
        {
            //Set the state observation object as the new root of the tree.
            //System.out.println("Creating a new tree...");
            setNewActions(stateObs.getAvailableActions());
            mctsPlayer.init(stateObs);
        }

        //Run MCTS for another cycle
        //System.out.println("Executing tree from " + mctsPlayer.m_root.rootState.getGameTick());
        int action = mctsPlayer.run(elapsedTimer);

        if(currentMacro == null || currentMacro.isFinished())
        {
            //It's time to determine the next macro action.
            currentMacro = actions[action].copy();
            currentMacro.reset();
            //System.out.println("New macro-action set:");
            //currentMacro.print();
        }

        //... and return the next action.
        //System.out.println("Returning action index " + currentMacro.cursor + ": " + currentMacro.peek());
        return currentMacro.next();
    }

}
