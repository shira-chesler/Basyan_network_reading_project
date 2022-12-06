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
            if(arr[i] == null){
                return -1;
            }
            if (arr[i].equals(wanted_value)){
                index = i;
                break;
            }
        }
        return index;
    }

    public void execute(int method, String query){
        Query ourq = makequery(query);
        switch (method){
            case 1:
                SimpleConc sc = new SimpleConc(this.varOutcomes, this.CPTs, this.variables, this.varopls);
                sc.simpleConc(ourq);
                sc.cleanOp();
                break;
            case 2:
                VarEliminiationByABC vebabc = new VarEliminiationByABC(this.varOutcomes, this.CPTs, this.variables, this.varopls);
                vebabc.variableEliminationByABC(ourq);
                break;
            case 3:
                VarEliminationByHuristic vebh = new VarEliminationByHuristic(this.varOutcomes, this.CPTs, this.variables, this.varopls);
                vebh.variableEliminationByHuristic(ourq);
                break;
        }
    }

    public Query makequery(String query){
        StringTokenizer readquery = new StringTokenizer(query, " ,|=");
        Hashtable<String, String> given = new Hashtable<>();
        Query ourq = new Query(readquery.nextToken(), readquery.nextToken()); //seperates the query
        while (readquery.hasMoreTokens()){
            given.put(readquery.nextToken(),readquery.nextToken()); //putting the evidences and their values into the HashTable
        }
        ourq.addEvidence(given);
        return ourq;
    }
}

