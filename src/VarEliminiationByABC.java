import java.util.Hashtable;

public class VarEliminiationByABC extends BaysianNetwork{

    public VarEliminiationByABC(Hashtable<String, Integer> varOutcomes, Hashtable<String, Double[]> CPTs, String[] variables, String[][] varopls) {
        super(varOutcomes, CPTs, variables, varopls);
    }

    public void variableEliminationByABC(String query) {
    }
}
