package controllers.sampleMCTS.learning;

import controllers.sampleMCTS.SingleMCTSPlayer;
import core.game.StateObservation;
import tools.ElapsedCpuTimer;

import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: ssamot
 * Date: 14/11/13
 * Time: 21:45
 * This is a Java port from Tom Schaul's VGDL - https://github.com/schaul/py-vgdl
 */
public class Agent extends controllers.sampleMCTS.Agent{


    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer)
    {
        super(so,elapsedTimer);

    }

    public SingleMCTSPlayer getPlayer(StateObservation so, ElapsedCpuTimer elapsedTimer) {
        return new SingleMCTSPlayer(new Random(), SingleMCTSPlayer.LEARNING_STYLE.SARSA_1);
    }


}
