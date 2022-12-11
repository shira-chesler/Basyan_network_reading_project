import java.io.PrintWriter;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;

public class BaysianNetwork {
    private boolean isinCPT=false;
    protected final String[] initial_variables;
    protected final Hashtable<String, Integer> initial_varOutcomes;
    protected final Hashtable<String, Double[]> initial_CPTs;
    protected final String[][] initial_varopls; //array of variable outcome options
    protected int num_of_mul=0;
    protected int num_of_sum=0;
    protected PrintWriter pw;

    public BaysianNetwork(Hashtable<String, Integer> varOutcomes, Hashtable<String, Double[]> CPTs, String[] variables, String[][] varopls, PrintWriter pw){
        this.initial_varOutcomes = varOutcomes;
        this.initial_CPTs = CPTs;
        this.initial_variables = variables;
        this.initial_varopls = varopls;
        this.pw = pw;
    }

    public void execute(int method, String query){
        Query ourq = makequery(query);
        switch (method) {
            case 1:
            {
                SimpleConc sc = new SimpleConc(this.initial_varOutcomes, this.initial_CPTs, this.initial_variables, this.initial_varopls, this.pw);
                sc.simpleConc(ourq);
                break;
            }
            case 2:
            {
                VarEliminiationByABC vebabc = new VarEliminiationByABC(this.initial_varOutcomes, this.initial_CPTs, this.initial_variables, this.initial_varopls, this.pw);
                vebabc.variableEliminationByABC(ourq);
                break;
            }
            case 3:
            {
                VarEliminationByHeuristic vebh = new VarEliminationByHeuristic(this.initial_varOutcomes, this.initial_CPTs, this.initial_variables, this.initial_varopls, this.pw);
                vebh.variableEliminationByHuristic(ourq);
                break;
            }
            case 4:
            {
                HashSet<String[]> set = new HashSet<>();
                String[] temp = new String[this.initial_variables.length];
                calcVarsOp(initial_variables,0, set, temp);
                int count = 1;
                Iterator iter = set.iterator();
                while (iter.hasNext()){
                    Expiriment ex = new Expiriment(this.initial_varOutcomes, this.initial_CPTs, this.initial_variables, this.initial_varopls, this.pw);
                    boolean b =ex.variableEliminationex(ourq, (String[]) iter.next());
                    if (b){
                        System.out.println(count);
                        count++;
                    }
                }
            }
        }
    }

    private Query makequery(String query){
        StringTokenizer readquery = new StringTokenizer(query, " ,|=");
        Hashtable<String, String> given = new Hashtable<>();
        Query ourq = new Query(readquery.nextToken(), readquery.nextToken()); //seperates the query
        while (readquery.hasMoreTokens()){
            given.put(readquery.nextToken(),readquery.nextToken()); //putting the evidences and their values into the HashTable
        }
        ourq.addEvidence(given);
        return ourq;
    }

    protected boolean queryInCPT(Query ourq){
        initial_CPTs.forEach((name, table) ->
        {
            boolean b_temp = true;
            StringTokenizer parts = new StringTokenizer(name, " ,|=");
            if (parts.nextToken().equals(ourq.getVar())){
                if(ourq.getEvidence().size()+1==name.length()/2){//num of vars in query+evidence against number of vars in cpt
                    Iterator iter = ourq.getEvidence().keySet().iterator();
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
                            var_idx = findIndex(initial_variables, vars_inCPT[var]); //gets the index of variable
                            for (int i = 0; i < initial_varopls[var_idx].length; i++) {
                                if (initial_varopls[var_idx][i].equals(ourq.getEvidence().get(vars_inCPT[var]))) { //finds the position of wanted value
                                    offset+= Math.pow(initial_varOutcomes.get(vars_inCPT[var]), vars_inCPT.length-var) * i; //in what area of the CPT we want to be with this variable
                                }
                            }
                        }
                        var_idx = findIndex(initial_variables, vars_inCPT[0]); //gets the index of the main CPT variable
                        for (int i = 0; i < initial_varopls[var_idx].length; i++) {
                            if (initial_varopls[var_idx][i].equals(ourq.getEvidence().get(vars_inCPT[0]))) { //finds the position of wanted value
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

    protected int contains(String origin, String isin){//return which var num in the cpt the var we're searching is
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

    protected int findIndex(String[] arr, String wanted_value){
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

    public void calcVarsOp(String[] arr, int idx, HashSet<String[]> options, String[] temp){
        if (idx>=arr.length){
            options.add(temp.clone());
            return;
        }
        else {
            for (int i=0; i<arr.length; i++){
                if (containsaar(temp, arr[i])){
                    continue;
                }
                temp[idx] = arr[i];
                calcVarsOp(arr, idx+1, options, temp);
            }
            temp[idx] = null;
        }
    }

    private boolean containsaar(String[] temp, String s) {
        for (int i = 0; i < temp.length; i++) {
            if (s.equals(temp[i])){
                return true;
            }

        }
        return false;
    }


}

