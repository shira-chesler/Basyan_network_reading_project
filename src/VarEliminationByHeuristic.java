import java.io.PrintWriter;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;

import static java.lang.Double.parseDouble;

public class VarEliminationByHeuristic extends VariableElimination{


    public VarEliminationByHeuristic(Hashtable<String, Integer> varOutcomes, Hashtable<String, Double[]> CPTs, String[] variables, String[][] varopls, PrintWriter pw) {
        super(varOutcomes, CPTs, variables, varopls, pw);
        this.variables = variables;
        this.varOutcomes = varOutcomes;
        this.CPTs = CPTs;
        this.varopls = varopls;
    }

    public void variableEliminationByHuristic(Query ourq) {
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
        HashSet<String> calculated = new HashSet<>();
        int[] num_Im_son_of = new int[variables.length];
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
                //System.out.println(Double.valueOf(df.format(parseDouble(final_facor[i][final_facor[0].length-1])))+","+num_of_sum+","+num_of_mul);
            }
        }
        /*Stack<String> vars_to_eliminate_by_order = VarsToEliminateByOrder(ourq, factors);
        while (!vars_to_eliminate_by_order.empty()){
            String var_to_eliminate = vars_to_eliminate_by_order.pop();
            factors = join(ourq, var_to_eliminate, factors);//means the variable is a hidden
        }
        factors = joinWithoutMarg(ourq, ourq.getVar(), factors);
        Iterator left_factors = factors.keySet().iterator();
        String last_factor_name = (String) left_factors.next();
        while(left_factors.hasNext() && (contains(last_factor_name, ourq.getVar()))==0){ //there are factors of parents and evidences
            last_factor_name = (String) left_factors.next();
        }
        if (findNumOfVarsInFactor(ourq, last_factor_name)!=1){
            vars_to_eliminate_by_order = VarsToEliminateByOrder(ourq, factors);
            while (!vars_to_eliminate_by_order.empty()) {
                String var_to_eliminate = vars_to_eliminate_by_order.pop();
                if (!isParentOfVar(ourq.getVar(), var_to_eliminate)){
                    factors = join(ourq, var_to_eliminate, factors);
                }
            }
            Iterator left_factors2 = factors.keySet().iterator();
            last_factor_name = (String) left_factors2.next();
            while(left_factors2.hasNext() && (contains(last_factor_name, ourq.getVar()))==0){ //there are factors of parents and evidences
                last_factor_name = (String) left_factors2.next();
            }
        }
        String[][] final_factor = factors.get(last_factor_name);
        int col_index_of_var = findIndex(final_factor[0], ourq.getVar());
        int col_index_of_prob = final_factor[0].length-1;
        double desired_probability = 0;
        double not_desired_probability = 0;
        String desired_probability_value = ourq.getValue();
        for (int i = 1; i < final_factor.length; i++) {
            if (final_factor[i][col_index_of_var].equals(desired_probability_value)){
                desired_probability+=parseDouble(final_factor[i][col_index_of_prob]);
            }
            else {
                not_desired_probability+=parseDouble(final_factor[i][col_index_of_prob]);
            }
            num_of_sum++;
        }
        num_of_sum-=2;//first time we initialized desired_probability or not_desired_probability
        double normalize_factor = desired_probability+not_desired_probability;
        num_of_sum++;
        desired_probability/=normalize_factor;
        DecimalFormat df = new DecimalFormat("#.#####");
        df.setRoundingMode(RoundingMode.HALF_UP);
        System.out.println(Double.valueOf(df.format(desired_probability))+","+num_of_sum+","+num_of_mul);*/
    }

    private int[] initializeNumImSon(Query ourq, HashSet<String> calculated) {
        int[] myFathers = new int[this.variables.length];
        for (int i = 0; i < myFathers.length; i++) {
            int counter = 0;
            if (ourq.getEvidence().containsKey(variables[i])){
                myFathers[i] = 0;
                continue;
            }
            Iterator iter = this.CPTs.keySet().iterator();
            while (iter.hasNext()) {
                String cur = (String) iter.next();
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

    /*private int[] initializeNumImFather(Query ourq, HashSet<String> calculated) {
        int[] myDepend = new int[this.variables.length];
        for (int i = 0; i < myDepend.length; i++) {
            int counter = 0;
            Iterator iter = this.CPTs.keySet().iterator();
            while (iter.hasNext()){
                String cur = (String) iter.next();
                if (contains(cur, this.variables[i])!=0){
                    if (!calculated.contains(this.variables[i])){
                        StringTokenizer st = new StringTokenizer(cur, " ,|=");
                        boolean b = false;
                        int token_number = 1;
                        while (st.hasMoreTokens() && !b){
                            String var = st.nextToken();
                            if (token_number==1 && calculated.contains(var)){
                                b=true;
                            }
                            if (this.variables[i].equals(var) && token_number==1){ //this is the cpt i'm main in
                                b=true;
                            }
                            if (!calculated.contains(var) && !var.equals(this.variables[i]) && !isCPTNameRoot(cur) && !isVarRoot(var) && !var.equals(ourq.getVar()) && !ourq.getEvidence().containsKey(var)){
                                counter++;
                                b = true;
                            }
                            token_number++;
                        }
                    }
                }
            }
            myDepend[i] = counter;
        }
        return myDepend;
    }*/

    private boolean isVarRoot(String var) {
        Iterator iter = this.CPTs.keySet().iterator();
        while (iter.hasNext()){
            if (iter.next().equals(var)){
                return true;
            }
        }
        return false;
    }

    /*private Stack<String> VarsToEliminateByOrder(Query ourq, HashMap<String, String[][]> factors){
        String final_var = ourq.getVar();
        Stack<String> vars_to_eliminate_by_order = new Stack<>();
        PriorityQueue<String> children = findVariablesThatDepenedOnVar(ourq, final_var, factors);
        HashSet<String> already_in_stack = new HashSet<>();
        while (children.size()!=0){
            String cur_child = children.poll();
            if (already_in_stack.contains(cur_child) || ourq.getEvidence().containsKey(cur_child) || ourq.getVar().equals(cur_child)){
                continue;
            }
            PriorityQueue<String> children_of_child = findVariablesThatDepenedOnVar(ourq, cur_child, factors);
            int size = children_of_child.size();
            for (int i = 0; i < size; i++) {
                children.add(children_of_child.poll());
            }
            already_in_stack.add(cur_child);
            vars_to_eliminate_by_order.push(cur_child);
        }
        return vars_to_eliminate_by_order;
    }*/

    /*private PriorityQueue<String> findVariablesThatDepenedOnVar(Query ourq, String var, HashMap<String, String[][]> factors) {
        Iterator names = factors.keySet().iterator();
        PriorityQueue<String> children = new PriorityQueue<>();
        while (names.hasNext()){
            String cur = (String) names.next();
            if (contains(cur, var)!=0){
                StringTokenizer st = new StringTokenizer(cur, " ,|=");
                String variable = st.nextToken();
                if ((!ourq.getEvidence().containsKey(variable)) && (!variable.equals(var))){
                    children.add(variable);
                }
            }
        }
        return children;
    }*/

    private boolean isCPTNameRoot(String varname){
        int num = numOfVarsIn(varname);
        if (num==1){
            return true;
        }
        return false;
    }

    private HashMap<String, String[][]> joinWithoutMarg(Query ourq, String varname, HashMap<String, String[][]> factors){
        HashSet factorswithvar = factorsWithVar(varname, factors); //contains the names of the cpts with the var
        int size = factorswithvar.size();
        String[][] namesizefactors = new String[size][3];
        Iterator iter = factorswithvar.iterator();
        for (int i = 0; i < namesizefactors.length; i++) {
            namesizefactors[i][0] = (String) iter.next(); //name of cpt
            namesizefactors[i][1] = String.valueOf(factors.get(namesizefactors[i][0]).length);//length of cpt
            namesizefactors[i][2] = String.valueOf(asciiSumOfCPT(namesizefactors[i][0]));//ascii sum of vars in cpt
        }
        String final_factor_name=namesizefactors[0][0];
        sortBySizeThenAscii(namesizefactors, 0);
        for (int i = 0; i < size-1; i++) {
            int j=i+1;
            String name = newFactorName(ourq, namesizefactors[i][0], namesizefactors[j][0]);
            String[][] temparr = createNewFactor(ourq, factors, namesizefactors[i][0], namesizefactors[j][0]);
            factors.remove(namesizefactors[i][0]);
            factors.remove(namesizefactors[j][0]);
            factors.put(name, temparr);
            namesizefactors[j][0] = name;
            namesizefactors[j][1] = String.valueOf(temparr.length);
            namesizefactors[j][2] = String.valueOf(asciiSumOfCPT(name));
            sortBySizeThenAscii(namesizefactors, j);
            final_factor_name = name;
        }
        String[][] final_factor = factors.get(final_factor_name);
        factors.remove(final_factor_name);
        if (!varname.equals(ourq.getVar())){
            final_factor_name = final_factor_name.replace(varname, "");
        }
        factors.put(final_factor_name, final_factor);
        return factors;
    }

    private int findNumOfVarsInFactor(Query ourq, String name){
        StringTokenizer st = new StringTokenizer(name, " ,|=");
        int num=0;
        while (st.hasMoreTokens()){
            num++;
            st.nextToken();
        }
        return num;
    }

    /*private boolean isParentOfVar(String varname, String potential_parent){
        Iterator iter = this.CPTs.keys().asIterator();
        while (iter.hasNext()){
            String cur = (String) iter.next();
            if (contains(cur, varname)!=0){
                StringTokenizer st = new StringTokenizer(cur, " ,|=");
                if (st.nextToken().equals(varname)){
                    if (contains(cur, potential_parent)!=0){
                        return true;
                    }
                }
            }
        }
        return false;
    }*/
}
