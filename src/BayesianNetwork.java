import java.io.PrintWriter;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;

public class BayesianNetwork {
    private boolean isinCPT=false;
    protected final String[] initial_variables;
    protected final Hashtable<String, Integer> initial_varOutcomes;
    protected final Hashtable<String, Double[]> initial_CPTs;
    protected final String[][] initial_varopls; //array of variable outcome options
    protected int num_of_mul=0;
    protected int num_of_sum=0;
    protected PrintWriter pw;

    /**
     * BayesianNetwork constructor.
     * @param varOutcomes - HashTable of number of outcomes for each variable in the network.
     * @param CPTs - HashTable of (so called) CPTs in the network.
     * @param variables - array of all variables in network.
     * @param varopls - array of array, each array contains all the possible outcomes for a specific variable,
     *                indexed respectively to variables.
     * @param pw - PrintWriter object.
     */
    public BayesianNetwork(Hashtable<String, Integer> varOutcomes, Hashtable<String, Double[]> CPTs, String[] variables, String[][] varopls, PrintWriter pw){
        this.initial_varOutcomes = varOutcomes;
        this.initial_CPTs = CPTs;
        this.initial_variables = variables;
        this.initial_varopls = varopls;
        this.pw = pw;
    }

    /**
     * Makes the query with makequery function, then by given method, sends the query and calculate it.
     * @param method - int that indicates by what method we should calculate the query.
     * @param query - String of the query (with the evidences).
     */
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
        }
    }

    /**
     * splits the query string, then puts inside a Query object the desired variable query, its value and a
     * HashTable of all the given evidences (when the key is the evidence variable and the value is its given
     * value).
     * @param query - String of the query (with the evidences).
     * @return an object of type "Query".
     */
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

    /**
     * The function runs on all the CPTs. For each CPT, it checks if it's the query's CPT, and if it is,
     * it checks if all the evidence variables are in this CPT, and there aren't anymore variables in CPT.
     * If this is true, it means all the query is contained within the CPT, then it searches for the specific
     * place of the query in CPT and prints it.
     * @param ourq - Query object, the current query we're trying to calculate.
     * @return boolean indication - if there's a CPt that already contains the whole query (means there's no
     * need to calculate anything).
     */
    protected boolean queryInCPT(Query ourq){
        initial_CPTs.forEach((name, table) ->
        {
            boolean b_temp = true;
            StringTokenizer parts = new StringTokenizer(name, " ,|=");
            if (parts.nextToken().equals(ourq.getVar())){ //means we're in the query's CPT
                if(ourq.getEvidence().size()+1==name.length()/2){//num of vars in query+evidence = number of vars in cpt
                    for (String s : ourq.getEvidence().keySet()) {
                        if (contains(name, s) == 0) {
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
                                    offset+= basePosition(vars_inCPT, var+1) * i; //in what area of the CPT we want to be with this variable
                                }
                            }
                        }
                        var_idx = findIndex(initial_variables, vars_inCPT[0]); //gets the index of the main CPT variable
                        for (int i = 0; i < initial_varopls[var_idx].length; i++) {
                            if (initial_varopls[var_idx][i].equals(ourq.getEvidence().get(vars_inCPT[0]))) { //finds the position of wanted value
                                offset+= i;
                            }
                        }
                        DecimalFormat df = new DecimalFormat("#.#####"); //prints it as required - 5 point after '.'
                        df.setRoundingMode(RoundingMode.HALF_UP);
                        pw.print(Double.valueOf(df.format(table[offset]))+","+0+","+0);
                        this.isinCPT = true;
                    }
                }
            }
        });
        if (this.isinCPT){
            this.isinCPT = false;
            return true;
        }
        return false;
    }

    /**
     * The function splits the given String by delimiters ",|=", and the searches for the desirable
     * substring.
     * @param origin - string we're searching in.
     * @param isin - the string we're searching for.
     * @return what substring number in the sting we're given is the substring we're searching for. If the
     * variable does not exist in the given name, it returns 0.
     */
    protected int contains(String origin, String isin){
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

    /**
     * The function Scans arr for wanted_value.
     * @param arr - array of strings.
     * @param wanted_value - string we want to find in arr.
     * @return If wanted_value exists in arr - returns its index in arr. Else - returns -1.
     */
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

    /**
     * The function multiplies the number of outcomes of each one of the variables the changes "faster"
     * then vars_inCPT[start_index-1] in CPT. This number gives us the change rate of vars_inCPT[start_index-1].
     * @param vars_inCPT - array of the vars in the CPT.
     * @param start_index - what index we want to start from (from which variable to start to calculate).
     * @return the changing rate of values in CPT for the variable vars_inCPT[start_index-1].
     */
    protected int basePosition(String[] vars_inCPT, int start_index){
        int mul = initial_varOutcomes.get(vars_inCPT[0]);//can't forget the first variable (the main, which is the first in cpt)
        for (int i = start_index; i < vars_inCPT.length; i++) {
            mul*=initial_varOutcomes.get(vars_inCPT[i]);
        }
        return mul;
    }

}

