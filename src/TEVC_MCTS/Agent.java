package TEVC_MCTS;

import TEVC_MCTS.features.*;
import TEVC_MCTS.pathfinder.Astar;
import TEVC_MCTS.pathfinder.Node;
import TEVC_MCTS.roller.TunableRoller;
import TEVC_MCTS.roller.VariableFeatureWeightedRoller;
import TEVC_MCTS.utils.Memory;
import core.game.Event;
import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

import java.awt.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.TreeSet;

/**
 * Created with IntelliJ IDEA.
 * User: ssamot
 * Date: 14/11/13
 * Time: 21:45
 * This is a Java port from Tom Schaul's VGDL - https://github.com/schaul/py-vgdl
 */
public class Agent extends AbstractPlayer {


    public static int NUM_ACTIONS;
    public static Types.ACTIONS[] actions;

    /**
     * Random generator for the agent.
     */
    private SingleMCTSPlayer mctsPlayer;

    private TunableRoller roller;

    //private GVGFeatureExtraction fe;
    private NavFeatureSource fe;

    protected Astar astar;

    private int rectWidth, rectHeight;

    private Memory memory;

    private ArrayList<Integer> validAstarStatic;

    /**
     * Public constructor with state observation and time due.
     * @param so state observation of the current game.
     * @param elapsedTimer Timer for the controller creation.
     */
    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer)
    {
        //Get the actions in a static array.
        ArrayList<Types.ACTIONS> act = so.getAvailableActions();
        actions = new Types.ACTIONS[act.size()];
        NUM_ACTIONS = actions.length;

        for(int i = 0; i < actions.length; ++i)
        {
            actions[i] = act.get(i);
        }

        boolean []moves = new boolean[NUM_ACTIONS];
        boolean [][]cont = new boolean[NUM_ACTIONS][NUM_ACTIONS];
        for(int i = 0; i < actions.length; ++i)
        {
            for(int j = 0; j < actions.length; ++j)
            {
                if( (actions[i] == Types.ACTIONS.ACTION_DOWN    && actions[j] == Types.ACTIONS.ACTION_UP) ||
                    (actions[i] == Types.ACTIONS.ACTION_UP      && actions[j] == Types.ACTIONS.ACTION_DOWN) ||
                    (actions[i] == Types.ACTIONS.ACTION_RIGHT   && actions[j] == Types.ACTIONS.ACTION_LEFT) ||
                    (actions[i] == Types.ACTIONS.ACTION_LEFT    && actions[j] == Types.ACTIONS.ACTION_RIGHT))
                    cont[i][j] = true;
            }

            if(actions[i] != Types.ACTIONS.ACTION_NIL && actions[i] != Types.ACTIONS.ACTION_USE)
                moves[i] = true;
        }


        //Create the player.
        if(Config.FEATURES == Config.CHASE_FEATURES)
            fe = new ChaseFeatures(so);
        else if(Config.FEATURES == Config.INFECTION_FEATURES)
            fe = new InfectionFeatures(so);
        else if(Config.FEATURES == Config.BOULDERDASH_FEATURES)
            fe = new BoulderdashFeatures(so);
        else if(Config.FEATURES == Config.CIRCLE_FEATURES)
            fe = new CircleFeatures(so);
        else if(Config.FEATURES == Config.LEFTRIGHT_FEATURES)
            fe = new LeftRightFeatures(so);
        else
            fe = new GVGFeatureExtraction();


        astar = new Astar(fe);
        fe.astar = astar;
        validAstarStatic = new ArrayList<Integer>();
        fe.validAstarStatic = validAstarStatic;
        memory = new Memory();
        memory.contraryActions = cont;
        memory.moveActions = moves;
        fe.setMemory(memory);

        long seed = new Random().nextInt();
        //System.out.println("MCTS Seed: " + seed);
        Random rnd = new Random(seed);

        roller = new VariableFeatureWeightedRoller(so,fe, rnd);
        mctsPlayer = new SingleMCTSPlayer(rnd, roller, memory);

        rectWidth = rectHeight = so.getBlockSize();

        Config.USE_ASTAR = false;
        initValues(so, elapsedTimer);

        //memory.report();

        //if(Config.USE_ASTAR)
        //    System.out.println("This game WILL USE A*");
        //else
        //    System.out.println("No A*");

        int a = 0;
    }


    private void initValues(StateObservation stateObs, ElapsedCpuTimer elapsedTimer)
    {
        Vector2d avatarPos = stateObs.getAvatarPosition();
        ArrayList<Observation>[] portalPositions = stateObs.getPortalsPositions(avatarPos);
        ArrayList<Observation>[] resourcesPositions = stateObs.getResourcesPositions(avatarPos);
        ArrayList<Observation>[] fixedPositions = stateObs.getImmovablePositions(avatarPos);
        ArrayList<Observation>[] movingPositions = stateObs.getMovablePositions(avatarPos);
        ArrayList<Observation>[] npcPositions = stateObs.getNPCPositions(avatarPos);

        if(fixedPositions != null) for(int i = 0; i < fixedPositions.length; ++i)
        {
            if(fixedPositions[i].size() > 5)
            {
                fixedPositions[i].clear();
            }
            else if (fixedPositions[i].size() > 0)
            {
                int type = fixedPositions[i].get(0).itype;
                if(!validAstarStatic.contains(type))
                    validAstarStatic.add(type);
            }
        }


        if(fixedPositions != null) initValues(fixedPositions, stateObs);
        if(portalPositions != null) initValues(portalPositions, stateObs);
        if(resourcesPositions != null) initValues(resourcesPositions, stateObs);
        if(movingPositions != null) initValues(movingPositions, stateObs);
        if(npcPositions != null) initValues(npcPositions, stateObs);

        //System.out.println("Before wander: "  + elapsedTimer.elapsedMillis());
        while(elapsedTimer.elapsedMillis() < 900)
        {
            //System.out.println("----");
            wander(stateObs.copy(), elapsedTimer);
        }
        //System.out.println(elapsedTimer.elapsedMillis());
    }

    private void wander(StateObservation stateObs, ElapsedCpuTimer elapsedTimer)
    {
        Random randomGenerator = new Random();
        ArrayList<Types.ACTIONS> actions = stateObs.getAvailableActions();
        boolean gameOver = stateObs.isGameOver();
        StateObservation lastAdvancedState = null;
        StateObservation stCopy = stateObs;

        while(!gameOver && (elapsedTimer.elapsedMillis() < 950))
        {
            int index = randomGenerator.nextInt(actions.size());
            Types.ACTIONS action = actions.get(index);

            lastAdvancedState = stCopy.copy();
            stCopy.advance(action);

            analyzeStateChange(lastAdvancedState, action, stCopy);

            gameOver = stCopy.isGameOver();
        }
    }


    /**
     * This tries to reach at least one sprite of each type of the observations arraylist
     * @param observations
     * @param stateObs
     */
    private void initValues( ArrayList<Observation>[] observations, StateObservation stateObs)
    {
        int block_size = stateObs.getBlockSize();
        Vector2d avatarPos = stateObs.getAvatarPosition();

        //For every type of these observations.
        for(int i = 0; i < observations.length; ++i)
        {
            boolean reached = false;
            //Try to reach at least one of the sprites.
            for(int j = 0; /*!reached &&*/ j < observations[i].size(); ++j)
            {
                Observation obs = observations[i].get(j);
                ArrayList<Node> pathToObservation = fe.getPath(avatarPos, obs.position, block_size, false);
                if(pathToObservation != null && pathToObservation.size() > 0)
                {
                    reached = followPath(stateObs.copy(), pathToObservation, block_size, obs.obsID);


                    boolean thereIsLos = lineOfSight(stateObs, avatarPos, obs.position, block_size);
                    int numCells = stateObs.getObservationGrid().length * stateObs.getObservationGrid()[0].length;

                    if(!thereIsLos && numCells<500)
                    {
                        Config.USE_ASTAR = true;
                    }
                }
            }

            // I couldn't reach any sprite of this type.
            if(!reached && observations[i].size()>0)
            {
                //System.out.println("I couldn't reach any sprite of type " + observations[i].get(0).itype);
            }
        }

    }

    private boolean lineOfSight(StateObservation stateObs, Vector2d origin, Vector2d dest, double block_size)
    {
        double increment = block_size * 0.5;
        Vector2d dir = new Vector2d(dest.x - origin.x, dest.y - origin.y);
        double distance = dir.mag();
        dir.normalise();
        dir.mul(increment);
        double acum = increment;

        ArrayList<Observation>[][] grid = stateObs.getObservationGrid();

        Vector2d pos = new Vector2d(origin.x, origin.y);
        while(acum < distance)
        {
            pos.add(dir);

            int blockX = (int) (pos.x / block_size);
            int blockY = (int) (pos.y / block_size);

            ArrayList<Observation> location = grid[blockX][blockY];
            int numSprites = location.size();
            for(int i = 0; i < numSprites; ++i)
            {
                Observation obs = location.get(i);
                if( (obs.category == Types.TYPE_STATIC)
                        && (!validAstarStatic.contains(obs.itype))
                        )
                //&& !memory.isTraversable(obs.itype) )
                {
                    return false;
                }

                if ( (obs.category != Types.TYPE_STATIC) && !memory.isTraversable(obs.itype) )
                {
                    return false;
                }
            }


            acum += increment;
        }

        return true;
    }

    private boolean followPath(StateObservation stateObs, ArrayList<Node> toFollow, int block_size, int targetID)
    {
        int MAX_ATTEMPTS = 5;
        StateObservation lastAdvancedState = null;
        boolean success = false;

        int numNodes = toFollow.size();
        for(int i = 0; i < numNodes; ++i)
        {
            Node next = toFollow.get(i);
            Types.ACTIONS nextAction = next.comingFrom;
            //System.out.println(nextAction);
            success = false;

            int attempts = 0;
            while(!success && attempts < MAX_ATTEMPTS)
            {
                StateObservation stCopy = stateObs.copy();
                stCopy.advance(nextAction);
                lastAdvancedState = stCopy;
                attempts++;

                Vector2d nextPos = stCopy.getAvatarPosition();
                Vector2d avatarPos = new Vector2d(nextPos.x / block_size, nextPos.y / block_size);
                Vector2d nodePos = new Vector2d(next.position.x, next.position.y);

                if(avatarPos.equals(nodePos))
                {
                    success = true; //We've advanced once.

                    if(i == numNodes-1)
                    {
                        //We managed to get there. Kudos.
                        //analyzeStateChange(stateObs, nextAction, stCopy);
                    }else{
                        stateObs = stCopy;
                    }

                }

                if(avatarPos.x < 0 || avatarPos.y < 0)
                {
                    //We've reached the end of the game
                    if(i == numNodes-1)
                    {
                        //At the last node! interesting...
                        //analyzeStateChange(stateObs, nextAction, stCopy);
                        success = true;
                    }else{
                        //In our way to our destination, the game finished. Maybe we can find some information.
                        //analyzeStateChange(stateObs, nextAction, stCopy);
                    }
                }

                analyzeStateChange(stateObs, nextAction, stCopy);
            }

            if(!success)
            {
                //We couldn't reach next destination. It's possible that the destination is not
                //reachable because the target occupies that space. So let's check this:

                if(lastAdvancedState != null)
                {
                    TreeSet<Event> events = lastAdvancedState.getEventsHistory();
                    for(Event e : events)
                    {
                        if(e.passiveSpriteId == targetID)
                        {
                            //There was a collision with the target
                            double scoreDiff = lastAdvancedState.getGameScore() - stateObs.getGameScore();
                            //System.out.println("Score diff achieved: " + scoreDiff);
                            analyzeStateChange(lastAdvancedState, nextAction, stateObs);
                            success = true;
                        }
                    }

                }
                return success;
            }
        }

        return success;
    }

    private void analyzeStateChange(StateObservation prev, Types.ACTIONS action, StateObservation next)
    {
        double scorePrev = prev.getGameScore();
        double scoreNext = next.getGameScore();


        double scoreDiff = scoreNext - scorePrev;
        Event ev = memory.retrieveLastUniqueEvent(next); //This is LIKELY the event that caused the game end or score change.
        if(ev != null){
            memory.manageEvent(ev, scoreDiff, next.getGameWinner(), true);
        }

        if(action != Types.ACTIONS.ACTION_USE && action != Types.ACTIONS.ACTION_NIL)
        {
            if(next.getAvatarPosition().equals(prev.getAvatarPosition()))
            {
                ev = memory.retrieveLastUniqueEvent(next); //This is LIKELY the event that caused the game end or score change.
                if(ev != null){
                    //System.out.println("Colliding with " + ev.passiveTypeId + " didn't let me move " + action);
                    memory.manageTraverse(ev, false);
                }
            }
        }

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
        //Init the player with the current state observation.
        mctsPlayer.init(stateObs, fe);

        //Determine the action using MCTS...
        int action = mctsPlayer.run(elapsedTimer);

        /*stateObs.advance(actions[action]);
        if(stateObs.isGameOver())
        {
            double best[] = mctsPlayer.source.bestVec();
            System.out.print(" -> " );
            for(int i = 0; i < best.length; ++i)
            {
                System.out.print(best[i] + ",");
            }
            System.out.println();
        }*/

        //if(stateObs.getGameTick() < 200)
        //    return Types.ACTIONS.ACTION_NIL;

        //if(stateObs.getGameTick() == 0 || stateObs.getGameTick() % 100 != 0)
        //    return Types.ACTIONS.ACTION_NIL;

        //... and return it.
        return actions[action];
    }



    public void draw(Graphics2D g)
    {
        if(Config.COMPUTE_HIT_MAP)
            drawHitMap(g);
    }

    private void drawHitMap(Graphics2D g)
    {
        for(int i = 0; i < SingleMCTSPlayer.m_hitsMap.length; ++i)
        {
            for(int j = 0; j < SingleMCTSPlayer.m_hitsMap[0].length; ++j)
            {
                int hits = SingleMCTSPlayer.m_hitsMap[i][j];

                if(hits > 0)
                {
                    setColor(g,hits);
                    g.setFont(new Font("TimesRoman", Font.BOLD, 18));

                    int x = (int)(i*rectWidth + rectWidth*0.5);
                    int y = (int)(j*rectHeight + rectHeight*0.5);
                    g.drawString("*",x,y);

                    g.setFont(new Font("TimesRoman", Font.PLAIN, 10));

                    g.drawString("["+hits+"]",x,y+10);
                }

            }
        }
        int a = 0;
    }


    Color BLACK = new Color(0,0,0);
    Color GREY0 = new Color(50, 50, 50);
    Color GREY1 = new Color(100, 100, 100);
    Color GREY2 = new Color(150, 150, 150);
    Color GREY3 = new Color(195, 195, 195);
    private void setColor(Graphics2D g, int hits)
    {

        if(hits <= 1)       g.setColor(GREY2);
        else if(hits <= 4)  g.setColor(GREY1);
        else if(hits <= 8)  g.setColor(GREY0);
        else if(hits <= 16) g.setColor(BLACK);
    }

}
