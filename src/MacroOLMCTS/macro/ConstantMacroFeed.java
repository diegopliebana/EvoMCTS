package MacroOLMCTS.macro;

/**
 * Created by dperez on 24/11/2014.
 */
public class ConstantMacroFeed implements IMacroFeed
{
    public int macroLength;

    public ConstantMacroFeed(int l)
    {
        macroLength = l;
    }

    @Override
    public int getNextLength() {
        return macroLength;
    }
}
