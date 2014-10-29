package tests.core.game;
 /*
import core.ArcadeMachine;
import core.VGDLFactory;
import core.VGDLParser;
import core.VGDLRegistry;
import core.competition.CompetitionParameters;
import core.game.Game;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import org.junit.Test;

import java.lang.reflect.Method;
                                     */
/**
 * Created by Diego on 30/08/14.
 */
public class GameTest {
     /*
    @Test
    public void testForwardModel() throws Exception{

        System.out.println("Testing Aliens...");


        String gamesPath = "examples/gridphysics/";
        String gameName = "aliens";
        int level = 0;

        String game_file = gamesPath + gameName + ".txt";
        String level_file = gamesPath + gameName + "_lvl" + level +".txt";


        testGameFwdModel(game_file, level_file);

    }

    public void testGameFwdModel(String game_file, String level_file) throws Exception
    {
        VGDLFactory.GetInstance().init(); //This always first thing to do.
        VGDLRegistry.GetInstance().init();

        System.out.println(" ** Playing game " + game_file + ", level " + level_file + " **");

        // First, we create the game to be played..
        Game toPlay = new VGDLParser().parseGame(game_file);
        toPlay.buildLevel(level_file);

        //Warm the game up.
        warmUpGame(toPlay);






    }


    private void warmUpGame(Game toPlay)
    {
        try{
            ArcadeMachine am = new ArcadeMachine();
            Class arcadeMachineClass = am.getClass();
            Class[] argTypes = new Class[] { Game.class, Long.class};
            Method method = arcadeMachineClass.getDeclaredMethod("warmUp", argTypes);
            method.setAccessible(true);
            Object[] args = new Object[] {toPlay, CompetitionParameters.WARMUP_TIME};
            method.invoke(am, args);
        }catch(Exception e)
        {

        }
    }

            */
}
