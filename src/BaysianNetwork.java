import java.util.*;

public class BaysianNetwork {
    protected final String[] variables;
    protected final Hashtable<String, Integer> varOutcomes;
    protected final Hashtable<String, Double[]> CPTs;
    protected final String[][] varopls; //array of variable outcome options

    public BaysianNetwork(Hashtable<String, Integer> varOutcomes, Hashtable<String, Double[]> CPTs, String[] variables, String[][] varopls){
        this.varOutcomes = varOutcomes;
        this.CPTs = CPTs;
        this.variables = variables;
        this.varopls = varopls;
    }


    public int findIndex(String[] arr, String wanted_value){
        int index=-1;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equals(wanted_value)){
                index = i;
                break;
            }
        }
        return index;
    }

    public void execute(int method, String query){
        switch (method){
            case 1:
                SimpleConc sc = new SimpleConc(this.varOutcomes, this.CPTs, this.variables, this.varopls);
                sc.simpleConc(query);
                break;
            case 2:
                VarEliminiationByABC vebabc = new VarEliminiationByABC(this.varOutcomes, this.CPTs, this.variables, this.varopls);
                vebabc.variableEliminationByABC(query);
                break;
            case 3:
                VarEliminationByHuristic vebh = new VarEliminationByHuristic(this.varOutcomes, this.CPTs, this.variables, this.varopls);
                vebh.variableEliminationByHuristic(query);
                break;
        }
    }
}

