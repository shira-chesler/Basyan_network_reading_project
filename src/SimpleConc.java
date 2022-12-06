import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;

public class SimpleConc extends BaysianNetwork{
    private int num_of_mul=0;
    private int num_of_sum=0;
    public SimpleConc(Hashtable<String, Integer> varOutcomes, Hashtable<String, Double[]> CPTs, String[] variables, String[][] varopls) {
        super(varOutcomes, CPTs, variables, varopls);
    }
    public void simpleConc(Query ourq){ //simple conclusion operation
        String[] temp_op = new String[this.variables.length]; //temporary array to calculate all possible variables combinations
        calOpRec(temp_op, 0, ourq);
        int idx_of_q = findIndex(variables, ourq.getVar()); //finds the index of query in variables
        int index_of_value = findIndex(varopls[idx_of_q], ourq.getValue()); //finds the index of the desirable query value in its' values array
        double desprobability;
        double opprobability = 0;
        desprobability = calculateOpInQuery(ourq); //desired probability
        int num_of_options = varopls[idx_of_q].length;//options of outcomes in query
        for (int i = 0; i < num_of_options; i++) {
            if(i==index_of_value) continue;
            else{
                opprobability += calculateOpposite(ourq, idx_of_q, i); //calculating all the other value options of the query
                num_of_sum++;
            }
        }
        double normalize_fac = (desprobability + opprobability); //we should have done num_of_sum-- then num_of_sum++
        desprobability /= normalize_fac;
        //DecimalFormat df = new DecimalFormat("#.#####");
        //df.setRoundingMode(RoundingMode.CEILING);
        //System.out.println(df.format(desprobability)+","+num_of_sum+","+num_of_mul);
        System.out.println(desprobability+","+num_of_sum+","+num_of_mul);
    }


    public void calOpRec(String[] arr, int idx, Query q){//creates all the variable options for the query
        if (idx>=arr.length){
            q.addOption(arr); //adds the current option to a set
            return;
        }
        else if (q.getEvidence().containsKey(this.variables[idx])){ //if the variable we're checking it the query or one of the givens
            arr[idx] = q.getEvidence().get(this.variables[idx]);
            calOpRec(arr, idx+1, q);
        }
        else if (this.variables[idx].equals(q.getVar())){
            arr[idx] = q.getValue();
            calOpRec(arr, idx+1, q);
        }
        else {
            for (int i=0; i<this.varopls[idx].length; i++){ //if the variable isn't the query or given - iterates through it's options
                arr[idx] = this.varopls[idx][i];
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
            Enumeration<String> keys = CPTs.keys();
            while (keys.hasMoreElements()){ //gets the probability of each combination and multiples them
                String key = keys.nextElement();
                cur_combination=getP(key, CPTs.get(key) ,option ,cur_combination);
            }
            num_of_mul--; //for the first time - we multiplied a probability with 1
            sum+=cur_combination;
            num_of_sum++;
        }
        num_of_sum--;
        return sum;
    }
    public double calculateOpposite(Query q, int query_idx, int value_idx){
        q.changeOptions(query_idx, this.varopls[query_idx][value_idx]);
        return calculateOpInQuery(q);
    }

    public double getP(String S, Double[] arr, String[] option, double cur_combination) {
        int var_idx = 0;
        if (S.length() == 1) {
            var_idx = findIndex(variables, S); //finds the index of variable
            for (int i = 0; i < varopls[var_idx].length; i++) {
                if (varopls[var_idx][i].equals(option[var_idx])) { //finds the index of wanted value in value options
                    cur_combination *= arr[i];
                    num_of_mul++;
                }
            }
        }
        else{//CPT name is bigger than 1, which means there are givens
            int offset=0;
            String[] vars_inCPT = S.split("[| -,]"); //array of variables involved
            for(int var = 1; var < vars_inCPT.length; var++){ //iterates through variables involved, excluding the "main" var of the CPT
                var_idx = findIndex(variables, vars_inCPT[var]); //gets the index of variable
                for (int i = 0; i < varopls[var_idx].length; i++) {
                    if (varopls[var_idx][i].equals(option[var_idx])) { //finds the position of wanted value
                        offset+= Math.pow(varOutcomes.get(vars_inCPT[var]), vars_inCPT.length-var) * i; //in what area of the CPT we want to be with this variable
                    }
                }
            }
            var_idx = findIndex(variables, vars_inCPT[0]); //gets the index of the main CPT variable
            for (int i = 0; i < varopls[var_idx].length; i++) {
                if (varopls[var_idx][i].equals(option[var_idx])) { //finds the position of wanted value
                    offset+= i;
                }
            }
            cur_combination *= arr[offset]; //multiply the current probability combination 'till now with the probability we've found
            num_of_mul++;
        }
        return cur_combination;
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

    public void cleanOp() {
        this.num_of_sum = 0;
        this.num_of_mul = 0;
    }
}

/*
להוסיף - מה קורה אם זה בדיוק תא בטבלה, פשוט תשלוף את זה
 */
