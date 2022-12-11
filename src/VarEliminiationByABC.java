import java.io.PrintWriter;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;

import static java.lang.Double.parseDouble;

public class VarEliminiationByABC extends VariableElimination{
    /**
     * VarEliminiationByABC (shortage for VariableEliminationByABC) constructor.
     * @param varOutcomes - HashTable of number of outcomes for each variable in the network.
     * @param CPTs - HashTable of (so called) CPTs in the network.
     * @param variables - array of all variables in network.
     * @param varopls - array of array, each array contains all the possible outcomes for a specific variable,
     *                indexed respectively to variables.
     * @param pw - PrintWriter object.
     */
    public VarEliminiationByABC(Hashtable<String, Integer> varOutcomes, Hashtable<String, Double[]> CPTs, String[] variables, String[][] varopls, PrintWriter pw) {
        super(varOutcomes, CPTs, variables, varopls, pw);
        this.variables = variables;
        this.varOutcomes = varOutcomes;
        this.CPTs = CPTs;
        this.varopls = varopls;
    }

    /**
     * The function checks if the query needs to be calculated (with queryInCPT method). If it does, it gets rid
     * of unneeded variables with getRid method, places the evidences of the query with placeEvidence method,
     * makes factors and then joins them by lexicographic order (excluding query).
     * Then it joins the query factor,finds the query value we want (desired probability) in the factor,
     * and normalizes it with the query values we don't want.
     * Finally, the function prints the normalized desired probability, the number of sums
     * and the number of multiplications done in the whole operation.
     * @param ourq - a "Query" type object, with the parameters of the query we want to calculate.
     */
    public void variableEliminationByABC(Query ourq) {
        if(queryInCPT(ourq)){
            return;
        }
        getRid(ourq);
        int size = this.CPTs.size();
        String[][] namesizecpts = new String[size][3];
        Iterator<String> iter = CPTs.keySet().iterator();
        for (int i = 0; i < namesizecpts.length; i++) {
            namesizecpts[i][0] = iter.next(); //name of cpt
            namesizecpts[i][1] = String.valueOf(this.CPTs.get(namesizecpts[i][0]).length);//length of cpt
            namesizecpts[i][2] = String.valueOf(asciiSumOfFactor(namesizecpts[i][0]));//ascii sum of vars in cpt
        }
        HashMap<String, String[][]> factors = createFactorsBeforeInitialize(namesizecpts);
        factors = placeEvidence(ourq, factors);
        HashSet<String> vars_left = new HashSet<>(Arrays.asList((this.variables)));
        while (vars_left.size()!=1){
            for (String variable : variables) {
                if (!vars_left.contains(variable) || variable.equals(ourq.getVar()) || ourq.getEvidence().containsKey(variable)) {
                    if (ourq.getEvidence().containsKey(variable)){
                        vars_left.remove(variable);
                    }
                    continue;
                }
                factors = join(ourq, variable, factors);//means the variable is a hidden
                vars_left.remove(variable);
            }
        }
        factors = join(ourq, ourq.getVar(), factors);
        Iterator<String> left_factors = factors.keySet().iterator();
        String last_factor_name = left_factors.next();
        while(left_factors.hasNext() && (contains(last_factor_name, ourq.getVar())==0)){ //there are might evidence tables
            last_factor_name = left_factors.next();
        }
        String[][] final_facor = factors.get(last_factor_name);
        double normelize_fac=0;
        for (int i=1; i < this.varOutcomes.get(ourq.getVar())+1; i++){
            normelize_fac+=parseDouble(final_facor[i][final_facor[0].length-1]);
            num_of_sum++;
        }
        num_of_sum--;
        for (int i=1; i < this.varOutcomes.get(ourq.getVar())+1; i++){
            final_facor[i][final_facor[0].length-1] = String.valueOf(parseDouble(final_facor[i][final_facor[0].length-1])/normelize_fac);
        }
        for (String[] strings : final_facor) {
            if (strings[0].equals(ourq.getValue())) {
                DecimalFormat df = new DecimalFormat("#.#####");
                df.setRoundingMode(RoundingMode.HALF_UP);
                pw.print(Double.valueOf(df.format(parseDouble(strings[final_facor[0].length - 1]))) + "," + num_of_sum + "," + num_of_mul);
            }
        }
    }

}
