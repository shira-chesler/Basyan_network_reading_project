import java.io.PrintWriter;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;

import static java.lang.Double.parseDouble;

public class VarEliminationByHeuristic extends VariableElimination{

    /**
     *
     * @param varOutcomes
     * @param CPTs
     * @param variables
     * @param varopls
     * @param pw
     */
    public VarEliminationByHeuristic(Hashtable<String, Integer> varOutcomes, Hashtable<String, Double[]> CPTs, String[] variables, String[][] varopls, PrintWriter pw) {
        super(varOutcomes, CPTs, variables, varopls, pw);
        this.variables = variables;
        this.varOutcomes = varOutcomes;
        this.CPTs = CPTs;
        this.varopls = varopls;
    }

    /**
     *
     * @param ourq
     */
    public void variableEliminationByHuristic(Query ourq) {
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
            namesizecpts[i][2] = String.valueOf(asciiSumOfCPT(namesizecpts[i][0]));//ascii sum of vars in cpt
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
        for (int i = 0; i < final_facor.length; i++) {
            if (final_facor[i][0].equals(ourq.getValue())){
                DecimalFormat df = new DecimalFormat("#.#####");
                df.setRoundingMode(RoundingMode.HALF_UP);
                pw.print(Double.valueOf(df.format(parseDouble(final_facor[i][final_facor[0].length-1])))+","+num_of_sum+","+num_of_mul);
            }
        }
    }

    /**
     *
     * @param ourq
     * @param calculated
     * @return
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
