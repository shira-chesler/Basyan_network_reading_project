import java.io.PrintWriter;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;

import static java.lang.Double.parseDouble;

public class VarEliminiationByABC extends VariableElimination{
    /**
     *
     * @param varOutcomes
     * @param CPTs
     * @param variables
     * @param varopls
     * @param pw
     */
    public VarEliminiationByABC(Hashtable<String, Integer> varOutcomes, Hashtable<String, Double[]> CPTs, String[] variables, String[][] varopls, PrintWriter pw) {
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
            namesizecpts[i][2] = String.valueOf(asciiSumOfCPT(namesizecpts[i][0]));//ascii sum of vars in cpt
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
        for (int i = 0; i < final_facor.length; i++) {
            if (final_facor[i][0].equals(ourq.getValue())){
                DecimalFormat df = new DecimalFormat("#.#####");
                df.setRoundingMode(RoundingMode.HALF_UP);
                pw.print(Double.valueOf(df.format(parseDouble(final_facor[i][final_facor[0].length-1])))+","+num_of_sum+","+num_of_mul);
            }
        }
    }

}
