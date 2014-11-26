package MacroOLMCTS.macro;

import java.util.Random;

/**
 * Created by dperez on 24/11/2014.
 */
public class RandomNMacroFeed implements IMacroFeed
{

    public int macroLengths[];

    public Random mRnd;


    public RandomNMacroFeed(int nl)
    {
        macroLengths = new int[nl];
        for(int i = 0; i < nl;++i) macroLengths[i] = i+1;

        mRnd = new Random();
    }

    @Override
    public int getNextLength() {
        int idx = mRnd.nextInt(macroLengths.length);
        return macroLengths[idx];
    }
}
