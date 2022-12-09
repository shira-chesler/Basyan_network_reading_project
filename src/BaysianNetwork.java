import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;

public class BaysianNetwork {
    private boolean isinCPT=false;
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


    public void execute(int method, String query){
        Query ourq = makequery(query);
        switch (method) {
            case 1 -> {
                SimpleConc sc = new SimpleConc(this.varOutcomes, this.CPTs, this.variables, this.varopls);
                sc.simpleConc(ourq);
                sc.cleanOp();
            }
            case 2 -> {
                VarEliminiationByABC vebabc = new VarEliminiationByABC(this.varOutcomes, this.CPTs, this.variables, this.varopls);
                vebabc.variableEliminationByABC(ourq);
            }
            case 3 -> {
                VarEliminationByHuristic vebh = new VarEliminationByHuristic(this.varOutcomes, this.CPTs, this.variables, this.varopls);
                vebh.variableEliminationByHuristic(ourq);
            }
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

    public boolean queryInCPT(Query ourq){
        CPTs.forEach((name, table) ->
        {
            boolean b_temp = true;
            StringTokenizer parts = new StringTokenizer(name, " ,|=");
            if (parts.nextToken().equals(ourq.getVar())){
                if(ourq.getEvidence().size()+1==name.length()/2){//num of vars in query+evidence against number of vars in cpt
                    Iterator iter = ourq.getEvidence().keys().asIterator();
                    while (iter.hasNext()) {
                        if (contains(name, (String) iter.next()) == 0) {
                            b_temp = false;
                            break;
                        }
                    }
                    if (b_temp){
                        int offset=0;
                        int var_idx;
                        String[] vars_inCPT = name.split("[| -,]"); //array of variables involved
                        for(int var = 1; var < vars_inCPT.length; var++){ //iterates through variables involved, excluding the "main" var of the CPT
                            var_idx = findIndex(variables, vars_inCPT[var]); //gets the index of variable
                            for (int i = 0; i < varopls[var_idx].length; i++) {
                                if (varopls[var_idx][i].equals(ourq.getEvidence().get(vars_inCPT[var]))) { //finds the position of wanted value
                                    offset+= Math.pow(varOutcomes.get(vars_inCPT[var]), vars_inCPT.length-var) * i; //in what area of the CPT we want to be with this variable
                                }
                            }
                        }
                        var_idx = findIndex(variables, vars_inCPT[0]); //gets the index of the main CPT variable
                        for (int i = 0; i < varopls[var_idx].length; i++) {
                            if (varopls[var_idx][i].equals(ourq.getEvidence().get(vars_inCPT[0]))) { //finds the position of wanted value
                                offset+= i;
                            }
                        }
                        DecimalFormat df = new DecimalFormat("#.#####");
                        df.setRoundingMode(RoundingMode.HALF_UP);
                        System.out.printf(Double.valueOf(df.format(table[offset]))+","+0+","+0);
                        this.isinCPT = true;
                    }
                }
            }
        });
        if (this.isinCPT){
            this.isinCPT = false;
            return true;
        }
        return this.isinCPT;
    }

    public int contains(String origin, String isin){//return which var num in the cpt the var we're searching is
        boolean b = false;
        int varnum = 0;
        StringTokenizer parts = new StringTokenizer(origin, ",|=");
        while (parts.hasMoreTokens()){
            varnum++;
            if (isin.equals(parts.nextToken())){
                b = true;
                break;
            }
        }
        if(b){
            return varnum;
        }
        return 0;
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
}

