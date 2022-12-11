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
     *
     * @param varOutcomes
     * @param CPTs
     * @param variables
     * @param varopls
     * @param pw
     */
    public VariableElimination(Hashtable<String, Integer> varOutcomes, Hashtable<String, Double[]> CPTs, String[] variables, String[][] varopls, PrintWriter pw) {
        super(varOutcomes, CPTs, variables, varopls, pw);
    }

    /**
     *
     * @param ourq
     */
    protected void getRid(Query ourq){ //the function removes all the variables that aren't predecessors of query or evidence
        String[] in_op = new String[this.variables.length];
        int num_of_vars=0; //counter to know the number of wanted variable
        in_op[num_of_vars] = ourq.getVar();
        num_of_vars++;
        Set<String> varstokeepst = ourq.getEvidence().keySet(); //set of the variables we need in calculation, initialized by evidence variables
        PriorityQueue<String> itvar = new PriorityQueue<>(varstokeepst);//queue with vars we wanna keep
        CPTs.forEach((S, arr) -> //adding to the set the parents of the query (its' dependencies)
        {
            StringTokenizer cName = new StringTokenizer(S, " ,|=");
            if (cName.nextToken().equals(ourq.getVar())){ //checks we got the querys' cpt
                while (cName.hasMoreTokens()){ //adds the dependencies of the querys' in the cpt (it's parents in the net)
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
     *
     * @param s
     * @return
     */
    protected int asciiSumOfCPT(String s) {
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
     *
     * @param ourq
     * @param varname
     * @param factors
     * @return
     */
    protected HashMap<String, String[][]> join(Query ourq, String varname, HashMap<String, String[][]> factors){
        HashSet<String> factorswithvar = factorsWithVar(varname, factors); //contains the names of the cpts with the var
        int size = factorswithvar.size();
        String[][] namesizefactors = new String[size][3];
        Iterator<String> iter = factorswithvar.iterator();
        for (int i = 0; i < namesizefactors.length; i++) {
            namesizefactors[i][0] = iter.next(); //name of cpt
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
        String[][] final_factor = marginalizeByVar(ourq,factors.get(final_factor_name), varname);
        factors.remove(final_factor_name);
        if (!varname.equals(ourq.getVar())){
            final_factor_name = final_factor_name.replace(varname, "");
        }
        factors.put(final_factor_name, final_factor);
        return factors;
    }

    /**
     *
     * @param namesizecpts
     * @return
     */
    protected HashMap<String, String[][]> createFactorsBeforeInitialize(String[][] namesizecpts) {
        HashMap<String, String[][]> factors = new HashMap<>();
        for (String[] namesizecpt : namesizecpts) {
            String cur_CPT_name = namesizecpt[0];
            String name = cur_CPT_name + ",-f" + factor_num;
            factor_num++;
            int num_of_vars = numOfVarsIn(cur_CPT_name);
            int length = this.CPTs.get(cur_CPT_name).length + 1;
            String[][] arr = new String[length][num_of_vars + 1];
            StringTokenizer st = new StringTokenizer(cur_CPT_name, "| -,");
            arr[0][arr[0].length - 1] = "Probability";
            arr[0][arr[0].length - 2] = st.nextToken();
            for (int j = 0; j < arr[0].length - 2; j++) {//puts the names of the vars in factor
                arr[0][j] = st.nextToken();
            }
            String var = arr[0][arr[0].length - 2];
            int change_rate = this.varOutcomes.get(var);
            int idx = findIndex(this.variables, var);
            for (int j = 1; j < length; j++) { //assigns the column of the vars that's its' table
                arr[j][arr[0].length - 2] = varopls[idx][(j - 1) % (change_rate)];
            }
            for (int j = 1; j < length; j++) { //assigns the probability column
                arr[j][arr[0].length - 1] = String.valueOf(CPTs.get(cur_CPT_name)[j - 1]);
            }
            for (int j = 0; j < num_of_vars - 1; j++) {//assigns the rest of the table
                var = arr[0][j];
                change_rate = basePosition(arr[0], j + 1, arr[0].length - 2, arr[0].length - 2);
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
     *
     * @param ourq
     * @param factors
     * @return
     */
    protected HashMap<String, String[][]> placeEvidence(Query ourq, HashMap<String, String[][]> factors){
        HashMap<String, String[][]> factors_tmp = (HashMap<String, String[][]>) factors.clone();
        factors_tmp.forEach((name, table) ->
        {
            String[] evidence_values = new String[table[0].length];
            HashSet<Integer> shared_columns = new HashSet<>();
            int num_of_row_to_div = 1; //later, we'll divide by the options of the evidences - cause we need only one option, which is given
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
     *
     * @param name
     * @return
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
     *
     * @param ourq
     * @param factora
     * @param factorb
     * @return
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
     *
     * @param vars_inCPT
     * @param start_index
     * @param end_index
     * @param first_position
     * @return
     */
    private int basePosition(String[] vars_inCPT, int start_index, int end_index, int first_position){
        int mul = varOutcomes.get(vars_inCPT[first_position]);//can't forget the first variable (the main, which is the first in cpt)
        for (int i = start_index; i < end_index; i++) {
            mul*=varOutcomes.get(vars_inCPT[i]);
        }
        return mul;
    }

    /**
     *
     * @param a
     * @param b
     * @param GivenCols
     * @return
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
     *
     * @param varname
     * @param factors
     * @return
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
     *
     * @param namesizecpts
     * @param indexstart
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
     *
     * @param ourq
     * @param factors
     * @param factora
     * @param factorb
     * @return
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
     *
     * @param ourq
     * @param fact
     * @param varsinfact
     * @param values
     * @param end_idx
     * @return
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
            if (CompareArrayIgnoringEvidence(values_to_search,copyOfRange(fact[i],0,fact[i].length-1), evidence_loc)){ //copy of range doesn't sends the probability column
                cell_value = parseDouble(fact[i][fact[0].length-1]);
                break;
            }
        }
        return cell_value;
    }

    /**
     *
     * @param withoutevifacor
     * @param withevifactor
     * @param evidenceLoc
     * @return
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
     *
     * @param ourq
     * @param cpt1
     * @param cpt2
     * @return
     */
    private HashSet<String>[] commonDiffVars(Query ourq, String cpt1, String cpt2){
        HashSet<String>[] commondiff = new HashSet[2];
        commondiff[0] = new HashSet<>();
        commondiff[1] = new HashSet<>();
        StringTokenizer parts = new StringTokenizer(cpt1, " ,|=");
        while (parts.hasMoreTokens()){
            String s = parts.nextToken();
            if (s.contains("-f") || ourq.getEvidence().containsKey(s)){
                continue;
            }
            if(contains(cpt2, s)!=0){
                commondiff[0].add(s);
            }
            else {
                commondiff[1].add(s);
            }
        }
        StringTokenizer parts2 = new StringTokenizer(cpt2, " ,|=");
        while (parts2.hasMoreTokens()) {
            String s = parts2.nextToken();
            if (s.contains("-f") || ourq.getEvidence().containsKey(s)){
                continue;
            }
            if (contains(cpt1, s) == 0) {
                commondiff[1].add(s);
            }
        }
        return commondiff;
    }

    /**
     *
     * @param ourq
     * @param fact1
     * @param fact2
     * @return
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
     *
     * @param ourq
     * @param final_factor
     * @param varname
     * @return
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
     *
     * @param sumTogether
     * @param strings
     * @param colNum
     * @return
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
