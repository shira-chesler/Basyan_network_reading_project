import java.util.Hashtable;

public class VarEliminationByHuristic extends BaysianNetwork{


    public VarEliminationByHuristic(Hashtable<String, Integer> varOutcomes, Hashtable<String, Double[]> CPTs, String[] variables, String[][] varopls) {
        super(varOutcomes, CPTs, variables, varopls);
    }

    public void variableEliminationByHuristic(String query) {
    }
}
