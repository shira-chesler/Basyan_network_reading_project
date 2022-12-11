import java.io.PrintWriter;
import java.util.*;

import static java.lang.Double.parseDouble;
import static java.util.Arrays.copyOfRange;

public class VariableElimination extends BayesianNetwork {
    protected String[] variables;
    protected Hashtable<String, Integer> varOutcomes;
    protected Hashtable<String, Double[]> CPTs;
    protected String[][] varopls;
    protected int factor_num=1;

    /**
     * VariableElimination constructor.
     * @param varOutcomes - HashTable of number of outcomes for each variable in the network.
     * @param CPTs - HashTable of (so called) CPTs in the network.
     * @param variables - array of all variables in network.
     * @param varopls - array of array, each array contains all the possible outcomes for a specific variable,
     *                indexed respectively to variables.
     * @param pw - PrintWriter object.
     */
    public VariableElimination(Hashtable<String, Integer> varOutcomes, Hashtable<String, Double[]> CPTs, String[] variables, String[][] varopls, PrintWriter pw) {
        super(varOutcomes, CPTs, variables, varopls, pw);
    }

    /**
     * The function gets rid of all the variables that aren't relevant for our query calculations.
     * The variables that aren't relevant for our query calculations are all the leaf node that are not
     * a query variable or an evidence variable. Thus, the functions adds to a queue all the parents of the
     * evidences and the query, then iterates through each variable in the queue and adds its parents to the
     * queue, and the variable to an array of variables that are relevant to in_op array (until there are no more
     * parents in the queue). Then, by in_op array, the function updates the class's fields to not contain the
     * unnecessary variables.
     * In addition, the function sorts the variables in lexicographic order.
     * @param ourq - a "Query" type object, with the parameters of the query we want to calculate.
     */
    protected void getRid(Query ourq){ //the function removes all the variables that aren't predecessors of query or evidence
        String[] in_op = new String[this.variables.length];
        int num_of_vars=0; //counter to know the number of wanted variable
        in_op[num_of_vars] = ourq.getVar();
        num_of_vars++;
        Set<String> vars_to_keep_set = ourq.getEvidence().keySet(); //set of the variables we need in calculation, initialized by evidence variables
        PriorityQueue<String> itvar = new PriorityQueue<>(vars_to_keep_set);//queue with vars we want to keep
        CPTs.forEach((S, arr) -> //adding to the set the parents of the query (its dependencies)
        {
            StringTokenizer cName = new StringTokenizer(S, " ,|=");
            if (cName.nextToken().equals(ourq.getVar())){ //checks we got the querys cpt
                while (cName.hasMoreTokens()){ //adds the dependencies of the querys in the cpt (it's parents in the net)
                    String parent = cName.nextToken();
                    itvar.add(parent);
                }
            }
        });
        while (!itvar.isEmpty()){
            String temp = itvar.poll();
            int idx = findIndex(in_op, temp);
            if (idx!=-1){ //if we already added this var to our array of variables that stays
                continue;
            }
            in_op[num_of_vars] = temp;
            num_of_vars++;
            CPTs.forEach((S, arr) ->
            {
                StringTokenizer cName = new StringTokenizer(S, " ,|=");
                if (cName.nextToken().equals(temp)){ //checks we got the desirable cpt
                    while (cName.hasMoreTokens()){ //adds the dependencies of the var in the cpt (it's parents in the net)
                        itvar.add(cName.nextToken());
                    }
                }
            });
        }
        String[] vars = new String[num_of_vars]; //array for only the vars we want
        Hashtable<String, Integer> varout = new Hashtable<>();//same as this.varOutComes but only for the vars we want
        Hashtable<String, Double[]> CPT = new Hashtable<>();//same as this.CPTs but only for the vars we want
        String[][] varop = new String[num_of_vars][];// same as this.varopls but only for the vars we want
        Arrays.sort(in_op, 0, num_of_vars); //lexicographic order of variables
        for (int i=0; i<num_of_vars; i++){
            String cur = in_op[i];
            int idx = findIndex(this.variables, cur);
            vars[i] = cur;
            varop[i] = this.varopls[idx];
            varout.put(cur, this.varOutcomes.get(cur));
            this.CPTs.forEach((S, arr) ->
            {
                StringTokenizer cName = new StringTokenizer(S, " ,|=");
                if (cName.nextToken().equals(cur)){
                    CPT.put(S, arr);
                }
            });
        }
        this.variables = vars; //running over the og variables with the current, respectively with the others
        this.varOutcomes = varout;
        this.CPTs = CPT;
        this.varopls = varop;
    } //getRid lexicographically sorts the variables

    /**
     * The function splits the CPT name, then calculates its ASCII sum
     * @param s - name of CPT
     * @return the ASCII sum of the variables in CPT
     */
    protected int asciiSumOfFactor(String s) {
        StringTokenizer parts = new StringTokenizer(s, " ,|=");
        int ascii_sum = 0;
        while (parts.hasMoreTokens()){
            char[] temp = parts.nextToken().toCharArray();
            for (char ch: temp){
                ascii_sum += ch;
            }
        }
        return ascii_sum;
    }

    /**
     * The function gets a set of all the factors that contains varname, then it gets the factors themselves.
     * For each factor, the function puts in the array "namesizefactors" the name of the factor, the
     * length of the factor and the ascii sum of vars in factor, then sends it to the sortBySizeThenAscii function.
     * After that, the function iterates on the (now sorted as we want) namesizefactors array,
     * and createsNewFactor on every 2 adjacent factors (first with second, then second with third, etc.),
     * and in each time overrides the second factor with the new factor created, and deletes the first factor
     * from the factors HashMap.
     * At last, the function sends the last factor to the marginalizeByVar function
     * which eliminates the var, then puts this last factor without the factor we eliminated into the factors
     * HashMap.
     * @param ourq - a "Query" type object, with the parameters of the query we want to calculate.
     * @param varname - the variable we want to join by.
     * @param factors - HashMap of all the factors we have at the moment
     * @return HashMap of factors without the factors containing variable needed to be eliminated (or with one
     * final factor that contains varname but is one valued so is ignored).
     */
    protected HashMap<String, String[][]> join(Query ourq, String varname, HashMap<String, String[][]> factors){
        HashSet<String> factorswithvar = factorsWithVar(varname, factors); //contains the names of the cpts with the var
        int size = factorswithvar.size();
        String[][] namesizefactors = new String[size][3];
        Iterator<String> iter = factorswithvar.iterator();
        for (int i = 0; i < namesizefactors.length; i++) {
            namesizefactors[i][0] = iter.next(); //name of factor
            namesizefactors[i][1] = String.valueOf(factors.get(namesizefactors[i][0]).length);//length of factor
            namesizefactors[i][2] = String.valueOf(asciiSumOfFactor(namesizefactors[i][0]));//ascii sum of vars in factor
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
            namesizefactors[j][2] = String.valueOf(asciiSumOfFactor(name));
            sortBySizeThenAscii(namesizefactors, j);
            final_factor_name = name;
        }
        String[][] final_factor = marginalizeByVar(ourq,factors.get(final_factor_name), varname);
        factors.remove(final_factor_name);
        if (!varname.equals(ourq.getVar())){
            final_factor_name = final_factor_name.replace(varname, "");
        }
        factors.put(final_factor_name, final_factor);
        return factors;
    }

    /**
     * The function crates the factors by assigning each one's name as the CPT name with "-f" (indicates
     * this is a factor) and a factor number (to avoid overriding factors later).
     * Then it creates for each factor a 2d array that contains all the outcomes combinations for all the variables
     * and places the correct probability for each combination.
     * Each factor is getting putted into the factors HashMap, in which the keys are the factors names and the
     * values are the factor tables themselves.
     * @param namesizecpts - 2d array in which every row in position 0 is the name of the CPT we're creating
     *                     factor from.
     * @return HashMap of all factors created.
     */
    protected HashMap<String, String[][]> createFactorsBeforeInitialize(String[][] namesizecpts) {
        HashMap<String, String[][]> factors = new HashMap<>();
        for (String[] namesizecpt : namesizecpts) {
            String cur_CPT_name = namesizecpt[0];
            String name = cur_CPT_name + ",-f" + factor_num;
            factor_num++;
            int num_of_vars = numOfVarsIn(cur_CPT_name);
            int length = this.CPTs.get(cur_CPT_name).length + 1; //we need extra place for the variables in factor names
            String[][] arr = new String[length][num_of_vars + 1];//we need extra place for the probability column
            StringTokenizer st = new StringTokenizer(cur_CPT_name, "| -,");
            arr[0][arr[0].length - 1] = "Probability";
            arr[0][arr[0].length - 2] = st.nextToken(); //the main variable of CPT which changes the fastest is always first in CPT name
            for (int j = 0; j < arr[0].length - 2; j++) {//puts the names of the vars in factor
                arr[0][j] = st.nextToken();
            }
            String var = arr[0][arr[0].length - 2];
            int change_rate = this.varOutcomes.get(var);
            int idx = findIndex(this.variables, var);
            for (int j = 1; j < length; j++) { //assigns the column of the vars that's its table
                arr[j][arr[0].length - 2] = varopls[idx][(j - 1) % (change_rate)];
            }
            for (int j = 1; j < length; j++) { //assigns the probability column
                arr[j][arr[0].length - 1] = String.valueOf(CPTs.get(cur_CPT_name)[j - 1]);
            }
            for (int j = 0; j < num_of_vars - 1; j++) {//assigns the rest of the table
                var = arr[0][j];
                change_rate = basePosition(arr[0], j + 1, arr[0].length - 2, arr[0].length - 2);//change rate of current variable in CPT and in factor
                idx = findIndex(this.variables, var);
                int tillchange = 0;
                int numvalue = 0;
                for (int k = 1; k < length; k++) {
                    if (tillchange == change_rate) {
                        tillchange = 0;
                        numvalue = (numvalue + 1) % this.varOutcomes.get(var);
                    }
                    arr[k][j] = varopls[idx][numvalue];
                    tillchange++;
                }
            }
            factors.put(name, arr);
        }
        return factors;
    }

    /**
     * The function iterates through all the factors and for each one of them finds the rows that contains values
     * that we don't want for evidence variable, and deletes them.
     * @param ourq - a "Query" type object, with the parameters of the query we want to calculate.
     * @param factors - HashMap of all factors.
     * @return updated HashMap of all factors - this time with evidences in place.
     */
    protected HashMap<String, String[][]> placeEvidence(Query ourq, HashMap<String, String[][]> factors){
        HashMap<String, String[][]> factors_tmp = (HashMap<String, String[][]>) factors.clone();
        factors_tmp.forEach((name, table) ->
        {
            String[] evidence_values = new String[table[0].length];
            HashSet<Integer> shared_columns = new HashSet<>();
            int num_of_row_to_div = 1; //later, we'll divide by the options of the evidences - because we need only one option, which is given
            for (int i = 0; i < table[0].length; i++) {
                if (ourq.getEvidence().containsKey(table[0][i])){ //in this column in table we have evidence
                    evidence_values[i] = ourq.getEvidence().get(table[0][i]);//the value for this column
                    num_of_row_to_div*=varOutcomes.get(table[0][i]);
                    shared_columns.add(i);
                }
            }
            if (shared_columns.size()!=0){
                String[][] factor_with_evidence = new String[((table.length-1)/num_of_row_to_div)+1][table[0].length];
                factor_with_evidence[0] = table[0];
                int placement_in_factor_with_evidence = 1;
                for (int i = 1; i < table.length; i++) {
                    if (compareOnlyByGivenCols(evidence_values, table[i], shared_columns)){
                        factor_with_evidence[placement_in_factor_with_evidence] = table[i].clone();
                        placement_in_factor_with_evidence++;
                    }
                }
                factors.put(name, factor_with_evidence);
            }
        });
        return factors;
    }

    /**
     * The function calculates the number of variables in name.
     * @param name - string that contains variables.
     * @return number of variables in name.
     */
    protected int numOfVarsIn(String name){
        int varnum = 0;
        StringTokenizer parts = new StringTokenizer(name, " ,|=");
        while (parts.hasMoreTokens()){
            parts.nextToken();
            varnum++;
        }
        return varnum;
    }

    /**
     * The function finds the common and different variables between factora and factorb and with commonDiffVars
     * function, then assigns by that a new factor name (that contains each one of the variables once, first
     * the commons).
     * @param ourq - a "Query" type object, with the parameters of the query we want to calculate.
     * @param factora - first factor in join
     * @param factorb - secondt factor in join
     * @return new factor name for the joining of two factors
     */
    protected String newFactorName(Query ourq, String factora, String factorb) {
        HashSet<String>[] comdif = commonDiffVars(ourq, factora, factorb);
        StringBuilder sb = new StringBuilder();
        for (String s : comdif[1]) {
            if (ourq.getEvidence().containsKey(s) || s.contains("-f")) {
                continue;
            }
            sb.append(s).append(",");
        }
        for (String s : comdif[0]){
            if (ourq.getEvidence().containsKey(s) || s.contains("-f")) {
                continue;
            }
            sb.append(s).append(",");
        }
        sb.append("-f").append(factor_num);
        factor_num++;
        return sb.toString();
    }

    /**
     * The function multiplies the number of outcomes of each one of the variables the changes "faster"
     * then vars_inCPT[start_index-1] in CPT. This number gives us the change rate of vars_inCPT[start_index-1].
     * @param vars_inCPT - array of the vars in the CPT.
     * @param start_index - what index we want to start from (from which variable to start to calculate).
     * @param end_index - because we don't want to try and get to the probability column.
     * @param first_position - the index of the first variable (the one that changes the fastest).
     * @return the changing rate of values in CPT for the variable vars_inCPT[start_index-1]
     */
    private int basePosition(String[] vars_inCPT, int start_index, int end_index, int first_position){
        int mul = varOutcomes.get(vars_inCPT[first_position]);//can't forget the first variable (the main, which is the first in cpt)
        for (int i = start_index; i < end_index; i++) {
            mul*=varOutcomes.get(vars_inCPT[i]);
        }
        return mul;
    }

    /**
     * Checks if all the values from a and b in the columns that are in GivenCols are identical or not.
     * @param a - first array .
     * @param b - second array.
     * @param GivenCols - set of the only columns we want to check.
     * @return boolean - if all the values from a and b in the columns that are in GivenCols are identical or not.
     */
    private boolean compareOnlyByGivenCols(String[] a, String[] b, HashSet<Integer> GivenCols){
        boolean bool =true;
        for (Integer givenCol : GivenCols) {
            int col = givenCol;
            if (!a[col].equals(b[col])) {
                bool = false;
                break;
            }
        }
        return bool;
    }

    /**
     * The function Iterates through all the factors and for each one checks whether it contains varname or not.
     * @param varname - variable we want to check what factors are containing it.
     * @param factors - HashMap of all factors.
     * @return HashSet of all factors that contains varname.
     */
    protected HashSet<String> factorsWithVar(String varname, HashMap<String, String[][]> factors){
        HashSet<String> namesfactorswithvar = new HashSet<>();
        factors.forEach((name, table) ->
        {
            if (contains(name, varname)!=0){
                namesfactorswithvar.add(name);
            }
        });
        return namesfactorswithvar;
    }

    /**
     * The function sorts the array by their sizes, and if there are two in the same sizes, it sorts them
     * by the ASCII value of their name.
     * @param namesizecpts - array of arrays, such in each row the first entrance is the name of a certain
     *                     factor, the second is its length, and the third is its ASCII name sum.
     * @param indexstart - from what index to start sorting.
     */
    protected void sortBySizeThenAscii(String[][] namesizecpts, int indexstart){//sort the cpts by size
        java.util.Arrays.sort(namesizecpts, indexstart, namesizecpts.length, (a, b) ->
                Integer.compare(Integer.parseInt(a[1]), Integer.parseInt(b[1])));
        for (int i = indexstart; i < namesizecpts.length-1; i++) {
            if (namesizecpts[i][1].equals(namesizecpts[i+1][1])){ //when cpts have the same size - sort by ascii sum of vars
                int j=i+1;
                while(j!=namesizecpts.length-1 && namesizecpts[i][1].equals(namesizecpts[j+1][1])){
                    j++;
                }
                java.util.Arrays.sort(namesizecpts, i, j+1, (a, b) ->
                        Integer.compare(Integer.parseInt(a[2]), Integer.parseInt(b[2])));
                i=j;
            }
        }
    }

    /**
     * The function creates a new factor in a similar way of the way that the function createFactorsBeforeInitialize
     * works, only that in creating a new factor it does it with a name given by newFactorName function, and
     * not a CPT name.
     * Moreover, in placing the probability values, it gets the wanted probability by multiplying the two
     * probability values that matches the variables combination of the same row *getting this probabilities
     * with the getCell function).
     * @param ourq - a "Query" type object, with the parameters of the query we want to calculate.
     * @param factors - HashMap of all factors.
     * @param factora - first factor in join.
     * @param factorb - second factor in join.
     * @return new factor. joining of factora and factorb.
     */
    protected String[][] createNewFactor(Query ourq, HashMap<String, String[][]> factors, String factora, String factorb){
        HashSet<String>[] comdif = commonDiffVars(ourq, factora, factorb);
        String name = newFactorName(ourq, factora, factorb);
        int length = findNewFactorSize(ourq, factora, factorb);
        String[][] newfactor = new String[length+1][comdif[0].size()+comdif[1].size()+1];
        StringTokenizer st = new StringTokenizer(name, ", |");
        int point=0;
        while (st.hasMoreTokens()){
            String s = st.nextToken();
            if (s.contains("-f")){
                continue;
            }
            newfactor[0][point] = s;
            point++;
        }
        newfactor[0][point] = "Probability";
        int change_rate = length;//div by outcomes of evidence
        for (int j = 0; j < newfactor[0].length-1; j++) {
            int till_change = 0;
            int noutcomes = varOutcomes.get(newfactor[0][j]);
            change_rate/=noutcomes;
            int value_idx = 0;
            for (int i=1; i<length+1; i++){
                if(ourq.getEvidence().containsKey(newfactor[0][j])){
                    newfactor[i][j] = ourq.getEvidence().get(newfactor[0][j]);
                }
                newfactor[i][j] = this.varopls[findIndex(this.variables, newfactor[0][j])][value_idx];
                till_change++;
                if (till_change==change_rate){
                    till_change=0;
                    value_idx=(value_idx+1)%varOutcomes.get(newfactor[0][j]);
                }
            }
        }
        for (int i = 1; i < length+1; i++) {
            newfactor[i][newfactor[0].length-1] = String.valueOf(
                    getCell(ourq, factors.get(factora), newfactor[0], newfactor[i], newfactor[i].length-1)
                            * getCell(ourq, factors.get(factorb), newfactor[0], newfactor[i], newfactor[i].length-1));
            this.num_of_mul++;
        }
        return newfactor;
    }

    /**
     * The function saves all the positions of the evidences in the original factor,
     * searches for the position of variables in the factor we want to get probability from.
     * Then it iterates through the original factors rows and compares between each one and the desired
     * values of variable combination (ignoring the variables we're not interested in). If it finds
     * a match it gets this row's probability value.
     * @param ourq - a "Query" type object, with the parameters of the query we want to calculate.
     * @param fact - factor we want to get probability from.
     * @param varsinfact - the variables we're interested in from fact.
     * @param values - the values of the variables we're interested in from fact.
     * @param end_idx - until where to check (so we don't accidentally get into the probability column).
     * @return the probability for the desired values combination given.
     */
    private double getCell(Query ourq, String[][] fact, String[] varsinfact, String[] values, int end_idx) {
        HashSet<Integer> evidence_loc = new HashSet<>();
        for (int i = 0; i < fact[0].length; i++) {
            if(ourq.getEvidence().containsKey(fact[0][i])){
                evidence_loc.add(i);//saves all the positions of the evidences in the original factor
            }
        }
        String[] values_to_search = new String[fact[0].length];
        int idx;
        for (int i = 0; i < end_idx; i++) {
            idx=findIndex(fact[0], varsinfact[i]); //searches for the position of variables in the factor
            if (idx==-1){ //didn't find position
                continue;
            }
            values_to_search[idx] = values[i];
        }
        double cell_value=-1;
        for (int i = 1; i < fact.length; i++) {
            if (CompareArrayIgnoringEvidence(values_to_search,copyOfRange(fact[i],0,fact[i].length-1), evidence_loc)){ //copy of range doesn't send the probability column
                cell_value = parseDouble(fact[i][fact[0].length-1]);
                break;
            }
        }
        return cell_value;
    }

    /**
     * The function checks for the columns that are not in evidenceLoc if all the values are identical or not.
     * @param withoutevifacor - factor without placing what we know.
     * @param withevifactor - factor with placing what we know.
     * @param evidenceLoc - Set of location of columns that are the columns we know (we don't want to check those).
     * @return boolean - for the columns that are not in evidenceLoc if all the values are identical or not.
     */
    private boolean CompareArrayIgnoringEvidence(String[] withoutevifacor, String[] withevifactor, HashSet<Integer> evidenceLoc) {
        boolean b = true;
        for (int j = 0; j < withevifactor.length; j++) {
            if(evidenceLoc.contains(j)){
                continue;
            }
            if (!(withevifactor[j].equals(withoutevifacor[j]))){
                b = false;
                break;
            }
        }
        return b;
    }


    /**
     * The function puts in an array of size 2 - in the first entrance the variables that are common between
     * fact1 and fact2 and in the second one, the variables that are different between them.
     * @param ourq - a "Query" type object, with the parameters of the query we want to calculate.
     * @param fact1 - name of first factor.
     * @param fact2 - name of second factor.
     * @return an array of size 2 - in the first entrance the variables that are common between
     * fact1 and fact2 and in the second one, the variables that are different between them.
     */
    private HashSet<String>[] commonDiffVars(Query ourq, String fact1, String fact2){
        HashSet<String>[] commondiff = new HashSet[2];
        commondiff[0] = new HashSet<>();
        commondiff[1] = new HashSet<>();
        StringTokenizer parts = new StringTokenizer(fact1, " ,|=");
        while (parts.hasMoreTokens()){
            String s = parts.nextToken();
            if (s.contains("-f") || ourq.getEvidence().containsKey(s)){ //indicator of factor
                continue;
            }
            if(contains(fact2, s)!=0){//common
                commondiff[0].add(s);
            }
            else {
                commondiff[1].add(s);//different
            }
        }
        StringTokenizer parts2 = new StringTokenizer(fact2, " ,|=");
        while (parts2.hasMoreTokens()) {
            String s = parts2.nextToken();
            if (s.contains("-f") || ourq.getEvidence().containsKey(s)){
                continue;
            }
            if (contains(fact1, s) == 0) {//different (all the commons have already been added)
                commondiff[1].add(s);
            }
        }
        return commondiff;
    }

    /**
     * The function multiplies the number of outcomes of the common variables and the different variables
     * between fact1 and fact2 (that gives all the different options to order the outcomes).
     * @param ourq - a "Query" type object, with the parameters of the query we want to calculate.
     * @param fact1 - name of first factor.
     * @param fact2 - name of second factor.
     * @return the size of the new factor that contains variables from both fact1 and fact2.
     */
    private int findNewFactorSize(Query ourq, String fact1, String fact2){
        HashSet<String>[] comndif = commonDiffVars(ourq, fact1, fact2);
        Iterator<String> iter_com = comndif[0].iterator();
        Iterator<String> iter_dif = comndif[1].iterator();
        int new_cpt_size=1;
        while (iter_com.hasNext()){
            String s = iter_com.next();
            if (ourq.getEvidence().containsKey(s) || s.contains("-f")){
                continue;
            }
            new_cpt_size*= varOutcomes.get(s);
        }
        while (iter_dif.hasNext()){
            String s = iter_dif.next();
            if (ourq.getEvidence().containsKey(s)|| s.contains("-f")){
                continue;
            }
            new_cpt_size*= varOutcomes.get(s);
        }
        return new_cpt_size;
    }

    /**
     * The function creates a new array named "after merge". It checks that it's row length is not 1
     * (if it does - returns, according to the case). Then it fills each row's variables values (creates all
     * the combinations without varname), and in assigning the probability to each row, it iterates through
     * final_factor and finds the rows in that matches the current row combination in aftermerge (compares without
     * the column of varname) - sums varname out.
     * @param ourq - a "Query" type object, with the parameters of the query we want to calculate.
     * @param final_factor - last factor after joining
     * @param varname - the variable we want to eliminate
     * @return if varname is the query, then it returns final_factor. Otherwise, if the factors row length
     * is 2 (meaning the factor is one values)- returns an empty array. Else - returns the factor after varname
     * is eliminated.
     */
    private String[][] marginalizeByVar(Query ourq, String[][] final_factor, String varname){
        int col_num = findIndex(final_factor[0], varname);
        String[] sum_together = new String[final_factor[0].length-1];
        String[][] aftermerge = new String[((final_factor.length-1)/this.varOutcomes.get(varname))+1][final_factor[0].length-1];
        if (aftermerge[0].length==1){
            if (varname.equals(ourq.getVar())){
                return final_factor;
            }
            return aftermerge;
        }
        int capacity=0;
        for (int i = 0; i < final_factor[0].length; i++) {
            if (i==col_num){
                continue;
            }
            aftermerge[0][capacity] = final_factor[0][i];
            capacity++;
        }
        int loc=1;//line
        HashSet<Integer> taken = new HashSet<>();
        capacity=0;
        while (loc<final_factor.length){
            if (taken.contains(loc)){
                loc++;
                continue;
            }
            taken.add(loc);
            int rowloc = 0;//col
            int place = 0;
            while (rowloc<sum_together.length){
                if(rowloc==col_num){
                    rowloc++;
                    continue;
                }
                sum_together[place] = final_factor[loc][rowloc];
                rowloc++;
                place++;
            }
            rowloc=final_factor[loc].length-1;
            double sum = parseDouble(final_factor[loc][rowloc]);
            int numofiteratiomsums=1;
            boolean b = false;
            for (int i = 1; i < final_factor.length; i++) {
                if(taken.contains(i)){
                    continue;
                }
                if(numofiteratiomsums==this.varOutcomes.get(varname)){ //means we sumed all the rows that contained this option
                    break;
                }
                if (compareWithoutCol(sum_together, final_factor[i], col_num)){
                    sum+=parseDouble(final_factor[i][rowloc]);
                    num_of_sum++;
                    numofiteratiomsums++;
                    taken.add(i);
                    b=true;
                    //capacity++;
                }
            }
            rowloc--;
            if(b){
                capacity++;
                sum_together[rowloc]= String.valueOf(sum);
                aftermerge[capacity] = sum_together.clone();
            }
            loc++;
        }
        return aftermerge;
    }

    /**
     * The function checks if all values in both arrays in comparison ignoring colNum in strings are identical.
     * @param sumTogether - array we want to compare with.
     * @param strings - array we want to compare ignoring column colNum.
     * @param colNum - int of column we want to ignore.
     * @return boolean - if all values in both arrays in comparison ignoring colNum in strings are identical.
     */
    private boolean compareWithoutCol(String[] sumTogether, String[] strings, int colNum) {
        boolean b = true;
        int j = 0;
        for (int i = 0; i < strings.length-1; i++) {
            if (i==colNum){
                continue;
            }
            if (!sumTogether[j].equals(strings[i])){
                b = false;
                break;
            }
            j++;
        }
        return b;
    }
}
