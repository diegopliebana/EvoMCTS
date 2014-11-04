package TEVC_MCTS;

/**
 * Created by diego on 22/09/14.
 */
public class Config {

    //Memory system.
    public static boolean USE_MEMORY = false;
    public static boolean USE_FORGET = true;
    public static int FORGET_TIME = 75;

    public static boolean USE_ASTAR = true;
    public static boolean USE_ASTAR_CACHE = true;
    public static boolean USE_FORGET_ASTAR_CACHE = false;

    public static int RAW_SCORE = 0; //Only raw score
    public static int PLUS_EXPERIENCE = 1; //Raw score plus experience.
    public static int PLUS_CURIOSITY = 2; //Raw score plus experience and curiosity.
    public static int SCORE_TYPE = RAW_SCORE;

    //MCTS stuff
    public static int MCTS_ITERATIONS = 100;
    public static int INDIVIDUAL_ITERATIONS = 1;
    public static int ROLLOUT_DEPTH = 10;
    public static double K = Math.sqrt(2);
    public static double REWARD_DISCOUNT = 0.8;

    public static boolean OPTIMAL_PLAY_ENABLED = false;
    public static int OPTIMAL_MAX_ACTIONS_RL = 10;
    public static int OPTIMAL_MAX_ACTIONS_CIRCLE = 15;

    //Vector source type.
    public static int HAND_TUNED_WEIGHTS = -1;
    public static int RANDOM = 0;
    public static int ONE_PLUS_ONE = 1;
    public static int MU_PLUS_ONE = 2;
    public static int BANDIT = 3;
    public static int ES_TYPE = BANDIT;

    //FEATURES:
    public static int GVG_FEATURES = 0;
    public static int CHASE_FEATURES = 1;
    public static int INFECTION_FEATURES = 2;
    public static int BOULDERDASH_FEATURES = 3;
    public static int CIRCLE_FEATURES = 4;
    public static int LEFTRIGHT_FEATURES = 5;
    public static int FEATURES = LEFTRIGHT_FEATURES;
    //public static int FEATURES = CIRCLE_FEATURES;

    //Debug.
    public static boolean COMPUTE_HIT_MAP = false;
}
