package FastEvoMCTS;

/**
 * Created by diego on 22/09/14.
 */
public class Config {

    //Pathfinding
    public static boolean USE_ASTAR = false;
    public static boolean USE_ASTAR_CACHE = true;
    public static boolean USE_FORGET_ASTAR_CACHE = false;
    public static boolean TOGGLE_ASTAR = false;

    //Memory system.
    public static boolean USE_MEMORY = true;
    public static boolean USE_FORGET = true;
    public static int FORGET_TIME = 75;

    //MCTS stuff
    public static boolean USE_PRUNE_CONT_UCT = false;
    public static int ROLLOUT_DEPTH = 5;
    public static double K = Math.sqrt(2);
    public static boolean USE_OPEN_LOOP = false;

    //Fast Evolution
    public static boolean USE_EVO = true;
    public static boolean KEEP_EVO = false;  //true to never re-init ES
    public static int EVO_FORGET_TIME = 75;
    public static int ONE_PLUS_ONE = 0;
    public static int MU_PLUS_ONE = 1;
    public static int ES_TYPE = MU_PLUS_ONE;


    //Debug.
    public static boolean COMPUTE_HIT_MAP = false;

}
