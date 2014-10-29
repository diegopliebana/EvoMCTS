package TEVC_MCTS.utils;

import TEVC_MCTS.Config;
import TEVC_MCTS.features.NavFeatureSource;
import core.game.Event;
import core.game.StateObservation;
import ontology.Types;
import tools.StatSummary;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

/**
 * Created by diego on 24/03/14.
 */
public class Memory
{
    public HashMap<Integer, MemoryItem> memories;
    public HashMap<Integer, MemoryItem> pastMemories;
    public HashMap<Integer, Double> pastDistances;

    public double gain;
    public double discoverScore;

    public boolean[][] contraryActions;
    public boolean[] moveActions;

    public Memory()
    {
        memories = new HashMap<Integer, MemoryItem>();
        pastMemories = new HashMap<Integer, MemoryItem>();
        pastDistances = new HashMap<Integer, Double>();
    }

    public boolean noEffect(StateObservation st, int action, StateObservation stNext)
    {
        if(!moveActions[action])
            return false;

        if(st.getAvatarPosition().equals(stNext.getAvatarPosition()))
            return true;

        return false;
    }

    public void manageGameEnd(StateObservation prev, StateObservation next)
    {
        double scorePrev = prev.getGameScore();
        double scoreNext = next.getGameScore();

        double scoreDiff = scoreNext - scorePrev;
        Event ev = retrieveLastUniqueEvent(next); //This is LIKELY the event that caused the game end or score change.
        if(ev != null){
            manageEvent(ev, scoreDiff, next.getGameWinner(), true);
        }
    }

    public Event retrieveLastUniqueEvent(StateObservation stObs)
    {
        Event event = null;

        TreeSet<Event> events = stObs.getEventsHistory();
        if(events != null && events.size()>0)
        {
            Iterator<Event> it = events.descendingSet().iterator();

            //Last event and its gameStep.
            event = it.next();
            int gameStep = event.gameStep;

            while(it.hasNext())
            {
                Event e = it.next();

                if((gameStep == e.gameStep))
                    return null;  //A second event with the same gameStep. Not unique, return null.
                else
                    return event; //Another event with a different gameStep. As it is ordered, is unique, return it.
            }
        }

        //Either null (no events), or the last and only (unique) event in the history.
        return event;
    }

    private HashMap<Integer, Double> translateFeatures(HashMap<String, Double> fs)
    {
        HashMap<Integer, Double> translated = new HashMap<Integer, Double>();

        Iterator<Map.Entry<String, Double>> itEntries = fs.entrySet().iterator();
        while(itEntries.hasNext())
        {
            Map.Entry<String, Double> entry = itEntries.next();
            String key = entry.getKey();
            double dist = entry.getValue();

            int keyN =  getIntFromStringKey(key);
            translated.put(keyN,dist);
        }
        return translated;
    }

    private int getIntFromStringKey(String infoType)
    {
        String[] type = infoType.split(":");
        return Integer.parseInt(type[1]);
    }

    private int manualCollisions(HashMap<String, Double> features, StateObservation soNext, double gainedScore, int nGameEvents)
    {
        int blockSize = soNext.getBlockSize();
        Event []tmpEvents = new Event[features.size()];
        int i = 0;
        int manEvents=0;

        Iterator<Map.Entry<String, Double>> itEntries = features.entrySet().iterator();
        while(itEntries.hasNext())
        {
            Map.Entry<String, Double> entry = itEntries.next();
            String key = entry.getKey();
            double dist = entry.getValue();
            if(!key.contains("ores") && dist < blockSize)
            {
                int id = getIntFromStringKey(key);
                tmpEvents[i++] = new Event(soNext.getGameTick(), false, 1, id, 0, 0, soNext.getAvatarPosition());
                manEvents++;
            }
        }

        for(i = 0; i < manEvents; ++i)
        {
            manageEvent(tmpEvents[i], gainedScore, soNext.getGameWinner(), (manEvents+nGameEvents) == 1);
        }

        return manEvents;
    }

    public void mark(NavFeatureSource featureExtraction)
    {
        HashMap<String, Double> fs = featureExtraction.getFeatureVector();
        HashMap<Integer, Double> feat = translateFeatures(fs);

        pastDistances.clear();
        pastMemories.clear();// = new HashMap<Integer, MemoryItem>();
        Iterator<Map.Entry<Integer, MemoryItem>> itEntries = memories.entrySet().iterator();
        while(itEntries.hasNext())
        {
            Map.Entry<Integer, MemoryItem> entry = itEntries.next();
            Integer key = entry.getKey();
            MemoryItem mem = entry.getValue();

            pastMemories.put(key, mem.copy());

            if(feat.containsKey(key))
            {
                pastDistances.put(key, feat.get(key));
            }

        }
    }

    public double getKnowledgeGain(HashMap<String, Double> features)
    {
        if(features == null)
            return 0;

        gain = 0;
        discoverScore = 0;

        HashMap<Integer, Double> feat = translateFeatures(features);

        Iterator<Map.Entry<Integer, MemoryItem>> itEntries = pastMemories.entrySet().iterator();
        while(itEntries.hasNext())
        {
            Map.Entry<Integer, MemoryItem> entry = itEntries.next();
            Integer key = entry.getKey();
            MemoryItem mem = entry.getValue();

            /*if(memories.containsKey(key))
            {
                MemoryItem curMem = memories.get(key);
                gain = curMem.computeGain(mem);

            }else{
                gain = mem.computeGain();
            }

            if(pastDistances.containsKey(key) && pastMemories.containsKey(key))
            {
                int occ = pastMemories.get(key).getTotalOcc();
                if((Config.SCORE_TYPE == Config.PLUS_CURIOSITY) && occ == 0)
                {
                    double dist = 1;
                    if(pastDistances.get(key) > 0)
                        dist = 1 - (feat.get(key)/pastDistances.get(key));

                    discoverScore += dist;

                }

                if(occ > 0)
                {
                    double dist = 1;
                    if(pastDistances.get(key) > 0)
                        dist = 1 - (feat.get(key)/pastDistances.get(key));

                    double meanSc = pastMemories.get(key).getTotalMeanScore();

                    discoverScore += dist * meanSc;

                    discoverScore += dist * pastMemories.get(key).getPercWins()
                                   - dist * pastMemories.get(key).getPercLoses();
                }
            }     */



            if(memories.containsKey(key))
            {
                MemoryItem curMem = memories.get(key);
                gain += curMem.computeGain(mem);

            }else{
                gain += mem.computeGain();
            }

            if(pastDistances.containsKey(key) && pastMemories.containsKey(key))
            {
                int occ = pastMemories.get(key).getTotalOcc();
                if((Config.SCORE_TYPE == Config.PLUS_CURIOSITY) && occ == 0)
                {
                    double dist = 1;
                    if(pastDistances.get(key) > 0)
                        dist = 1 - (feat.get(key)/pastDistances.get(key));

                    discoverScore += dist;

                }

                if(occ > 0 && pastMemories.get(key).getTotalMeanScore() >= 0 && pastDistances.get(key)>0)
                {
                    double dist = 1 - (feat.get(key)/pastDistances.get(key));

                    double meanSc = pastMemories.get(key).getTotalMeanScore();

                    if(!Double.isNaN(meanSc))
                        discoverScore += dist * meanSc;

                    discoverScore += dist * pastMemories.get(key).getPercWins()
                                     - dist * pastMemories.get(key).getPercLoses();
                }
            }
        }

        if(Double.isNaN(gain))
            return 0.0;

        return gain;
    }

    public double getPercWins(int key)
    {
        if(memories.containsKey(key))
            return memories.get(key).getPercWins();
        return 0.0;
    }


    public double getPercLoses(int key)
    {
        if(memories.containsKey(key))
            return memories.get(key).getPercLoses();
        return 0.0;
    }

    public double getTotalMeanScore(int key)
    {
        if(memories.containsKey(key))
            return memories.get(key).getTotalMeanScore();
        return 0.0;
    }


    public double getDiscoverScore()
    {
        return discoverScore;
    }

    public void forget(int timestamp)
    {
        Iterator<Map.Entry<Integer, MemoryItem>> itEntries = memories.entrySet().iterator();
        while(itEntries.hasNext())
        {
            Map.Entry<Integer, MemoryItem> entry = itEntries.next();
            MemoryItem mem = entry.getValue();
            if(mem.timestamp + Config.FORGET_TIME < timestamp)
                mem.reset();
        }


    }


    public void addInfoType(String infoType)
    {
        if(!infoType.contains("ores"))
        {
            int intID = getIntFromStringKey(infoType);
            if(!memories.containsKey(intID))
            {
                memories.put(intID, new MemoryItem());
            }
        }
    }

    public void addInformation(int prevNumEvents, double prevScore, StateObservation soNext, HashMap<String, Double> features)
    {
        double gainedScore = getGainedScore(soNext, prevScore);
        int numNewEvents = soNext.getEventsHistory().size() - prevNumEvents;
        int totalNewGameEvents = numNewEvents;

        int manEvents = manualCollisions(features, soNext, gainedScore, numNewEvents);

        Iterator<Event> itEvent = soNext.getEventsHistory().descendingSet().iterator();
        while(numNewEvents > 0 && itEvent.hasNext())
        {
            Event ev = itEvent.next();
            manageEvent(ev, gainedScore, soNext.getGameWinner(), (manEvents+totalNewGameEvents) == 1);
            numNewEvents--;
        }
    }

    private double getGainedScore(StateObservation soNext, double prevScore) {

        boolean gameLost = soNext.isGameOver() && soNext.getGameWinner() == Types.WINNER.PLAYER_LOSES;
        double gainedScore = 0;
        if(gameLost)
            gainedScore = -100;
        else
            gainedScore = soNext.getGameScore() - prevScore;
        return gainedScore;
    }

    public boolean isTraversable(int typeId)
    {
        if(!memories.containsKey(typeId))
        {
            return true;
        }
        return memories.get(typeId).isTraversable();
    }

    public void manageTraverse(Event ev, boolean traversable)
    {
        if(ev.passiveTypeId != 0)
        {
            int a = 0;
        }

        MemoryItem mem;
        if(!memories.containsKey(ev.passiveTypeId))
        {
            mem = new MemoryItem();
            memories.put(ev.passiveTypeId, mem);
        }else
            mem = memories.get(ev.passiveTypeId);

        mem.setTraversable(traversable);
    }

    public void manageEvent(Event ev, double gainedScore, Types.WINNER winner, boolean standalone)
    {
        MemoryItem mem;
        if(!memories.containsKey(ev.passiveTypeId))
        {
            mem = new MemoryItem();
            memories.put(ev.passiveTypeId, mem);
        }else
            mem = memories.get(ev.passiveTypeId);

        mem.addOcc(gainedScore, winner, standalone, ev.passiveTypeId, ev.fromAvatar, ev.gameStep);
    }

    public void report()
    {
        Iterator<Map.Entry<Integer, MemoryItem>> itEntries = memories.entrySet().iterator();
        while(itEntries.hasNext())
        {
            Map.Entry<Integer, MemoryItem> entry = itEntries.next();

            MemoryItem mem = entry.getValue();
            Integer key = entry.getKey();

            System.out.println("Entry of type " + key + ", with timestamp " + mem.timestamp + ": ");
            System.out.println("\t * makes me win at " + mem.getPercWins()*100.0 + "%, lose at "
                    + mem.getPercLoses()*100.0 + "%.");
            System.out.println("\t * is " + (mem.traversable ? "traversable" : "not traversable"));
            System.out.println("\t * is been seen " + mem.getTotalOcc());
            System.out.println("\t * produces an average score of " + mem.getCollScore() + " on collision.");
            System.out.println("\t * produces an average score of " + mem.getActScore() + " with from-avatar sprites.");
        }
    }

    private class MemoryItem
    {
        double FIRST_KNOWLEDGE = 10;
        StatSummary standaloneSS;
        StatSummary multipleSS;
        StatSummary actionSS;
        int timestamp;
        boolean traversable;

        double nWins;
        double nLoses;
        double nNone;

        public MemoryItem()
        {
            standaloneSS = new StatSummary();
            multipleSS = new StatSummary();
            actionSS = new StatSummary();
            traversable = true;
            nWins = nLoses = nNone = 0;
        }

        public int getTotalOcc()
        {
            return standaloneSS.n() + multipleSS.n() + actionSS.n();
        }

        public double getTotalMeanScore()
        {
            double totMean = 0.0;
            if(standaloneSS.n()>0) totMean+=standaloneSS.mean();
            if(multipleSS.n()>0) totMean+=multipleSS.mean();
            if(actionSS.n()>0) totMean+=actionSS.mean();
            return totMean;
        }


        public double getCollScore()
        {
            double totMean = 0.0;
            if(standaloneSS.n()>0) totMean+=standaloneSS.mean();
            if(multipleSS.n()>0) totMean+=multipleSS.mean();
            return totMean;
        }

        public double getPercWins()
        {
            if(nWins == 0) return 0;
            return nWins / (nWins + nLoses + nNone);
        }

        public double getPercLoses()
        {
            if(nLoses == 0) return 0;
            return nLoses / (nWins + nLoses + nNone);
        }

        public double getActScore()
        {
            double totMean = 0.0;
            if(actionSS.n()>0) totMean+=actionSS.mean();
            return totMean;
        }


        public void addOcc(double scoreChange, Types.WINNER winner, boolean standalone, int id, boolean fromAvatar, int timestamp)
        {

            if(fromAvatar)
            {
                if(standalone)
                {
                    actionSS.add(scoreChange);
                    //System.out.println(scoreChange);
                }else return;
            }else{
                if(standalone)
                {
                    standaloneSS.add(scoreChange);
                }else
                    multipleSS.add(scoreChange);
            }
            this.timestamp = timestamp;

            if(winner == Types.WINNER.PLAYER_LOSES)
                nLoses++;
            else if(winner == Types.WINNER.PLAYER_WINS)
            {
                //Config.USE_ASTAR = true;
                nWins++;
            }
            else nNone++;

        }

        public void setTraversable(boolean trav)
        {
            this.traversable = trav;
        }

        public boolean isTraversable()
        {
            return traversable;
        }

        private double gain(double pre, double post)
        {
            if(pre == 0)
                return  post * FIRST_KNOWLEDGE;
            return (post / pre) - 1;
        }

        public double computeGain()
        {
            return gain(0,standaloneSS.n()) + gain(0,multipleSS.n()) + gain(0,actionSS.n());
        }

        public double computeGain(MemoryItem pastMemory)
        {
            double gain = 0;

            gain += gain(pastMemory.standaloneSS.n(), standaloneSS.n());
            gain += gain(pastMemory.multipleSS.n(), multipleSS.n());
            gain += gain(pastMemory.actionSS.n(), actionSS.n());

            return gain;
        }

        public void reset()
        {
            timestamp = 0;
            standaloneSS = new StatSummary();
            multipleSS = new StatSummary();
            actionSS = new StatSummary();
        }

        public MemoryItem copy()
        {
            MemoryItem mm = new MemoryItem();
            mm.timestamp = this.timestamp;
            mm.standaloneSS = this.standaloneSS.copy();
            mm.multipleSS = this.multipleSS.copy();
            mm.actionSS = this.actionSS.copy();

            mm.timestamp = this.timestamp;
            mm.traversable = this.traversable;
            mm.nWins = this.nWins;
            mm.nLoses = this.nLoses;
            mm.nNone = this.nNone;

            return mm;
        }

        public String toString()
        {
            return "[alone oc: " + standaloneSS.n() + ", mean: " + standaloneSS.mean() + "]" +
                   "[mult oc " + multipleSS.n() + ", mean: " + multipleSS.mean() + "]" +
                   "[actn oc " + actionSS.n() + ", mean: " + actionSS.mean() + "]";
        }
    }

}
