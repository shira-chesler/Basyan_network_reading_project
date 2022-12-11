import java.io.PrintWriter;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;

import static java.lang.Double.parseDouble;
import static java.util.Arrays.copyOfRange;

public class VarEliminiationByABC extends VariableElimination{

    public VarEliminiationByABC(Hashtable<String, Integer> varOutcomes, Hashtable<String, Double[]> CPTs, String[] variables, String[][] varopls, PrintWriter pw) {
        super(varOutcomes, CPTs, variables, varopls, pw);
        this.variables = variables;
        this.varOutcomes = varOutcomes;
        this.CPTs = CPTs;
        this.varopls = varopls;
    }

    public void variableEliminationByABC(Query ourq) {
        if(queryInCPT(ourq)){
            return;
        }
        getRid(ourq);
        int size = this.CPTs.size();
        String[][] namesizecpts = new String[size][3];
        Iterator iter = CPTs.keySet().iterator();
        for (int i = 0; i < namesizecpts.length; i++) {
            namesizecpts[i][0] = (String) iter.next(); //name of cpt
            namesizecpts[i][1] = String.valueOf(this.CPTs.get(namesizecpts[i][0]).length);//length of cpt
            namesizecpts[i][2] = String.valueOf(asciiSumOfCPT(namesizecpts[i][0]));//ascii sum of vars in cpt
        }
        HashMap<String, String[][]> factors = createFactorsBeforeInitialize(ourq, namesizecpts);
        factors = placeEvidence(ourq, factors);
        HashSet vars_left = new HashSet(Arrays.asList((this.variables)));
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
        Iterator left_factors = factors.keySet().iterator();
        String last_factor_name = (String) left_factors.next();
        while(left_factors.hasNext() && (contains(last_factor_name, ourq.getVar())==0)){ //there are might evidence tables
            last_factor_name = (String) left_factors.next();
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
                pw.println(Double.valueOf(df.format(parseDouble(final_facor[i][final_facor[0].length-1])))+","+num_of_sum+","+num_of_mul);
            }
        }
    }

    /*private HashMap<String, String[][]> createFactorsAfterInitialize(Query ourq, String[][] namesizecpts) {
        HashMap<String, String[][]> factors = new HashMap<>();
        for (int i = 0; i < namesizecpts.length; i++) {
            String cur_CPT_name = namesizecpts[i][0];
            StringBuilder sb = new StringBuilder(cur_CPT_name);
            sb.append(",-f"+factor_num);
            factor_num++;
            String name = sb.toString();
            //int num_of_vars = findNumOfVarsInCPT(cur_CPT_name);
            int num_of_vars = numOfVarsInCPT(cur_CPT_name);
            int length = this.CPTs.get(cur_CPT_name).length+1;
            String[][] arr = new String[length][num_of_vars+1];
            StringTokenizer st = new StringTokenizer(cur_CPT_name, "| -,");
            arr[0][arr[0].length-1] = "Probability";
            arr[0][arr[0].length-2] = st.nextToken();
            for (int j = 0; j < arr[0].length-2; j++) {//puts the names of the vars in factor
                arr[0][j] = st.nextToken();
            }
            String var = arr[0][arr[0].length-2];
            int change_rate = this.varOutcomes.get(var);
            int idx = findIndex(this.variables, var);
            for (int j = 1; j < length; j++){ //assigns the column of the vars that's its' table
                if (ourq.getEvidence().containsKey(var)){
                    arr[j][arr[0].length-2] = ourq.getEvidence().get(var);
                    continue;
                }
                arr[j][arr[0].length-2] = varopls[idx][(j-1)%(change_rate)];
            }
            for (int j = 1; j < length; j++){ //assigns the probability column
                arr[j][arr[0].length-1] = String.valueOf(CPTs.get(cur_CPT_name)[j-1]);
            }
            int evidencesize = 0;
            Iterator evidences_in_factor = ourq.getEvidence().keys().asIterator();
            while (evidences_in_factor.hasNext()){
                if(contains(cur_CPT_name, (String) evidences_in_factor.next())!=0){
                    evidencesize++;
                }
            }
            for (int j = 0; j < num_of_vars-1; j++) {//assigns the rest of the table
                var = arr[0][j];
                change_rate = (int) Math.pow(this.varOutcomes.get(var), num_of_vars-(j+1)-evidencesize);
                idx = findIndex(this.variables, var);
                int tillchange=0;
                int numvalue=0;
                for (int k = 1; k < length; k++) {
                    if (ourq.getEvidence().containsKey(var)){
                        arr[k][j] = ourq.getEvidence().get(var);
                    }
                    if (tillchange==change_rate){
                        tillchange=0;
                        numvalue=(numvalue+1)%this.varOutcomes.get(var);
                    }
                    arr[k][j] = varopls[idx][numvalue];
                    tillchange++;
                }
            }
            factors.put(name, arr);
        }
        return factors;
    }
*/

}
