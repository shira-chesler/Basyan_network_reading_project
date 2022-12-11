import java.io.PrintWriter;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;

import static java.lang.Double.parseDouble;

public class VarEliminationByHeuristic extends VariableElimination{

    /**
     * VarEliminiationByHeuristic (shortage for VariableEliminationByHeuristic) constructor.
     * @param varOutcomes - HashTable of number of outcomes for each variable in the network.
     * @param CPTs - HashTable of (so called) CPTs in the network.
     * @param variables - array of all variables in network.
     * @param varopls - array of array, each array contains all the possible outcomes for a specific variable,
     *                indexed respectively to variables.
     * @param pw - PrintWriter object.
     */
    public VarEliminationByHeuristic(Hashtable<String, Integer> varOutcomes, Hashtable<String, Double[]> CPTs, String[] variables, String[][] varopls, PrintWriter pw) {
        super(varOutcomes, CPTs, variables, varopls, pw);
        this.variables = variables;
        this.varOutcomes = varOutcomes;
        this.CPTs = CPTs;
        this.varopls = varopls;
    }

    /**
     * The function checks if the query needs to be calculated (with queryInCPT method). If it does, it gets rid
     * of unneeded variables with getRid method, places the evidences of the query with placeEvidence method,
     * makes factors and then joins them by order decided by Heuristic (excluding query).
     * Then it joins the query factor,finds the query value we want (desired probability) in the factor,
     * and normalizes it with the query values we don't want.
     * Finally, the function prints the normalized desired probability, the number of sums
     * and the number of multiplications done in the whole operation.
     * The Heuristic chosen is to start eliminate from the roots of the BayesianNetwork (roots in graph,
     * variables with no parents) and then to continue to their sons, from there to their sons, etc. In this
     * Heuristic we don't care where to eliminate evidence variables so without loss of generality we eliminated
     * them in the first iteration (with the roots of the network).
     * @param ourq - a "Query" type object, with the parameters of the query we want to calculate.
     *
     */
    public void variableEliminationByHeuristic(Query ourq) {
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
        HashSet<String> calculated = new HashSet<>();
        int[] num_Im_son_of;
        while (calculated.size()!=this.variables.length-1){
            num_Im_son_of = initializeNumImSon(ourq, calculated);
            for (int i = 0; i < num_Im_son_of.length; i++) {
                if (this.variables[i].equals(ourq.getVar())){
                    continue;
                }
                if (num_Im_son_of[i]==0 && !calculated.contains(this.variables[i])){
                    if (ourq.getEvidence().containsKey(this.variables[i])){
                        calculated.add(this.variables[i]);
                        continue;
                    }
                    factors = join(ourq, variables[i], factors);
                    calculated.add(this.variables[i]);
                }

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

    /**
     * The function creates an array, such that in entrance i in the array there is the number of fathers that
     * haven't been calculated yet of variable[i].
     * @param ourq - a "Query" type object, with the parameters of the query we want to calculate.
     * @param calculated - HashSet of al variables that have already been calculated.
     * @return an array, in entrance i in the array there is the number of fathers that haven't
     * been calculated yet of variable[i].
     */
    private int[] initializeNumImSon(Query ourq, HashSet<String> calculated) {
        int[] myFathers = new int[this.variables.length];
        for (int i = 0; i < myFathers.length; i++) {
            int counter = 0;
            if (ourq.getEvidence().containsKey(variables[i])){
                myFathers[i] = 0;
                continue;
            }
            for (String cur : this.CPTs.keySet()) {
                if (contains(cur, this.variables[i]) != 1) { //it's not this.variables[i]'s cpt
                    continue;
                }
                StringTokenizer st = new StringTokenizer(cur, " ,|=");
                while (st.hasMoreTokens()) {
                    String var = st.nextToken();
                    if (!calculated.contains(var) && !var.equals(this.variables[i]) && !var.equals(ourq.getVar())) {
                        counter++;
                    }
                }
            }
            myFathers[i] = counter;
        }
         return myFathers;
    }

}
