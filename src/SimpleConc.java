import java.io.PrintWriter;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

public class SimpleConc extends BayesianNetwork {

    /**
     * SimpleConc (shortage for SimpleConclusion) constructor.
     * @param varOutcomes - HashTable of number of outcomes for each variable in the network.
     * @param CPTs - HashTable of (so called) CPTs in the network.
     * @param variables - array of all variables in network.
     * @param varopls - array of array, each array contains all the possible outcomes for a specific variable,
     *                indexed respectively to variables.
     * @param pw - PrintWriter object.
     */
    public SimpleConc(Hashtable<String, Integer> varOutcomes, Hashtable<String, Double[]> CPTs, String[] variables, String[][] varopls, PrintWriter pw) {
        super(varOutcomes, CPTs, variables, varopls, pw);
    }

    /**
     * The function checks if the query needs to be calculated (with queryInCPT method). If it does, it
     * calculates all the possible outcomes option with calOpRec function. Then finds the probability for
     * the query value we want, normalizes it with the query values we don't want, and prints the probability,
     * the number of sums and the number of multiplications done in the whole operation.
     * @param ourq - a "Query" type object, with the parameters of the query we want to calculate.
     */
    public void simpleConc(Query ourq){ //simple conclusion operation
        if(queryInCPT(ourq)){
            return;
        }
        String[] temp_op = new String[this.initial_variables.length]; //temporary array to calculate all possible variables combinations
        calOpRec(temp_op, 0, ourq);
        int idx_of_q = findIndex(initial_variables, ourq.getVar()); //finds the index of query in variables
        int index_of_value = findIndex(initial_varopls[idx_of_q], ourq.getValue()); //finds the index of the desirable query value in its' values array
        double desprobability;
        double opprobability = 0;
        desprobability = calculateOpInQuery(ourq); //desired probability
        int num_of_options = initial_varopls[idx_of_q].length;//options of outcomes in query
        for (int i = 0; i < num_of_options; i++) {
            if(i!=index_of_value) {
                opprobability += calculateOpposite(ourq, idx_of_q, i); //calculating all the other value options of the query
                num_of_sum++;
            }
        }
        double normalize_fac = (desprobability + opprobability); //we should have done num_of_sum-- then num_of_sum++
        desprobability /= normalize_fac;
        DecimalFormat df = new DecimalFormat("#.#####");
        df.setRoundingMode(RoundingMode.HALF_UP);
        pw.print(Double.valueOf(df.format(desprobability))+","+num_of_sum+","+num_of_mul);
    }

    /**
     * The function calOpRec (shortage for calculate options recursively) calculates recursively all the different
     * options for variable's outcomes in query (in consideration of given information such as evidences or query
     * values), and puts it inside queries "ex_options" set property.
     * @param arr - temporary array in which we're putting outcomes options.
     * @param idx - index we're in.
     * @param q - Query object of current query.
     */
    public void calOpRec(String[] arr, int idx, Query q){//creates all the variable options for the query
        if (idx>=arr.length){
            q.addOption(arr); //adds the current option to a set
        }
        else if (q.getEvidence().containsKey(this.initial_variables[idx])){ //if the variable we're checking it the query or one of the givens
            arr[idx] = q.getEvidence().get(this.initial_variables[idx]);
            calOpRec(arr, idx+1, q);
        }
        else if (this.initial_variables[idx].equals(q.getVar())){
            arr[idx] = q.getValue();
            calOpRec(arr, idx+1, q);
        }
        else {
            for (int i=0; i<this.initial_varopls[idx].length; i++){ //if the variable isn't the query or given - iterates through it's options
                arr[idx] = this.initial_varopls[idx][i];
                calOpRec(arr, idx+1, q);
            }
        }
    }

    /**
     * The function iterates through all the different variable outcomes combinations and sums the probabilities.
     * @param q - Query object of current query.
     * @return sum of all probabilities in this set of options.
     */
    public double calculateOpInQuery(Query q){
        Iterator<String[]> options = q.getEx_options().iterator();
        double sum = 0;
        while (options.hasNext()){ //iterates through the combinations in the option
            double cur_combination = 1;
            String[] option = options.next();
            Enumeration<String> keys = initial_CPTs.keys();
            while (keys.hasMoreElements()){ //gets the probability of each combination and multiples them
                String key = keys.nextElement();
                cur_combination=getP(key, initial_CPTs.get(key) ,option ,cur_combination);
            }
            num_of_mul--; //for the first time - we multiplied a probability with 1
            sum+=cur_combination;
            num_of_sum++;
        }
        num_of_sum--;
        return sum;
    }

    /**
     * The function calculates probability options, but with a different value for query itself.
     * @param q - Query object of current query.
     * @param query_idx - index of query in variables.
     * @param value_idx - the index of new value for query we want to calculate with
     * @return sum of all probabilities in this set of options (this set of options was changed to the new
     * value of query we wanted).
     */
    public double calculateOpposite(Query q, int query_idx, int value_idx){
        q.changeOptions(query_idx, this.initial_varopls[query_idx][value_idx]);
        return calculateOpInQuery(q);
    }

    /**
     * The function gets a combination of values it has to get its probability from the CPT, then finds it
     * using "basePosition" method.
     * @param S - name of CPT.
     * @param arr - CPT.
     * @param option - which option we want to find (option=outcomes option of variables).
     * @param cur_combination - all the probabilities we multiplied so far
     * @return the probability of the current combination (the value in the CPT we found) multiplied by
     * all the previous combinations calculated for a specific probability (cur_combination param)
     */
    public double getP(String S, Double[] arr, String[] option, double cur_combination) {
        int var_idx;
        String[] vars_inCPT = S.split("[| -,]"); //array of variables involved
        if (vars_inCPT.length == 1) {
            var_idx = findIndex(initial_variables, S); //finds the index of variable
            for (int i = 0; i < initial_varopls[var_idx].length; i++) {
                if (initial_varopls[var_idx][i].equals(option[var_idx])) { //finds the index of wanted value in value options
                    cur_combination *= arr[i];
                    num_of_mul++;
                }
            }
        }
        else{//CPT name is bigger than 1, which means there are givens
            int offset=0;
            for(int var = 1; var < vars_inCPT.length; var++){ //iterates through variables involved, excluding the "main" var of the CPT,
                // cause it always appears first but its change rate is the fastest.
                var_idx = findIndex(initial_variables, vars_inCPT[var]); //gets the index of variable
                for (int i = 0; i < initial_varopls[var_idx].length; i++) {
                    if (initial_varopls[var_idx][i].equals(option[var_idx])) { //finds the position of wanted value
                        offset+= basePosition(vars_inCPT, var+1) * i; //in what area of the CPT we want to be with this variable
                    }
                }
            }
            var_idx = findIndex(initial_variables, vars_inCPT[0]); //gets the index of the main CPT variable
            for (int i = 0; i < initial_varopls[var_idx].length; i++) {
                if (initial_varopls[var_idx][i].equals(option[var_idx])) { //finds the position of wanted value
                    offset+= i;
                }
            }
            cur_combination *= arr[offset]; //multiply the current probability combination 'till now with the probability we've found
            num_of_mul++;
        }
        return cur_combination;
    }

}


