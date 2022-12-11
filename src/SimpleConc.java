import java.io.PrintWriter;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;

public class SimpleConc extends BaysianNetwork{
    public SimpleConc(Hashtable<String, Integer> varOutcomes, Hashtable<String, Double[]> CPTs, String[] variables, String[][] varopls, PrintWriter pw) {
        super(varOutcomes, CPTs, variables, varopls, pw);
    }
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
            if(i==index_of_value) continue;
            else{
                opprobability += calculateOpposite(ourq, idx_of_q, i); //calculating all the other value options of the query
                num_of_sum++;
            }
        }
        double normalize_fac = (desprobability + opprobability); //we should have done num_of_sum-- then num_of_sum++
        desprobability /= normalize_fac;
        DecimalFormat df = new DecimalFormat("#.#####");
        df.setRoundingMode(RoundingMode.HALF_UP);
        pw.println(Double.valueOf(df.format(desprobability))+","+num_of_sum+","+num_of_mul);
    }


    public void calOpRec(String[] arr, int idx, Query q){//creates all the variable options for the query
        if (idx>=arr.length){
            q.addOption(arr); //adds the current option to a set
            return;
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

    public double calculateOpInQuery(Query q){
        Iterator options = q.getEx_options().iterator();
        double sum = 0;
        while (options.hasNext()){ //iterates through the combinations in the option
            double cur_combination = 1;
            String[] option = (String[]) options.next();
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
    public double calculateOpposite(Query q, int query_idx, int value_idx){
        q.changeOptions(query_idx, this.initial_varopls[query_idx][value_idx]);
        return calculateOpInQuery(q);
    }

    public double getP(String S, Double[] arr, String[] option, double cur_combination) {
        int var_idx = 0;
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
            for(int var = 1; var < vars_inCPT.length; var++){ //iterates through variables involved, excluding the "main" var of the CPT
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

    private int basePosition(String[] vars_inCPT, int start_index){
        int mul = initial_varOutcomes.get(vars_inCPT[0]);//can't forget the first variable (the main, which is the first in cpt)
        for (int i = start_index; i < vars_inCPT.length; i++) {
            mul*=initial_varOutcomes.get(vars_inCPT[i]);
        }
        return mul;
    }
    public void cleanOp() {
        this.num_of_sum = 0;
        this.num_of_mul = 0;
    }
}

/*
להוסיף - מה קורה אם זה בדיוק תא בטבלה, פשוט תשלוף את זה
 */
