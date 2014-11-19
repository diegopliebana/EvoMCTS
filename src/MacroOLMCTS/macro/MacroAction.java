package MacroOLMCTS.macro;

import MacroOLMCTS.Agent;
import ontology.Types;

/**
 * Class that encapsulates macro-actions.
 * Created by dperez on 16/11/2014.
 */
public class MacroAction
{
    //The proper macro-action.
    public Types.ACTIONS[] actions;

    //Indicates position in the macro-action.
    public int cursor;

    public MacroAction(Types.ACTIONS[] actions)
    {
        this.actions = actions;
        cursor = 0;
    }

    public MacroAction(Types.ACTIONS[] actions, int cursor)
    {
        this.actions = actions;
        this.cursor = cursor;
    }

    public void reset()
    {
        cursor = 0;
    }

    public boolean isFinished()
    {
        return (cursor >= Agent.MACROACTION_LENGTH);
    }

    public boolean isLast()
    {
        return (cursor == Agent.MACROACTION_LENGTH-1);
    }

    public Types.ACTIONS next()
    {
        return actions[cursor++];
    }

    public Types.ACTIONS peek() { return actions[cursor];}

    public void print()
    {
        for(Types.ACTIONS act : actions)
        {
            System.out.print(act + ",");
        }
        System.out.println();
    }

    public MacroAction copy()
    {
        Types.ACTIONS[] actionscp = new Types.ACTIONS[actions.length];
        System.arraycopy(actions, 0, actionscp, 0, actions.length);
        return new MacroAction(actionscp, cursor);
    }

}
