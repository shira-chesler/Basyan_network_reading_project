import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;

import static java.lang.Double.parseDouble;
import static java.util.Arrays.copyOfRange;

public class VarEliminiationByABC extends BaysianNetwork{
    private String[] variables;
    private Hashtable<String, Integer> varOutcomes;
    private Hashtable<String, Double[]> CPTs;
    private String[][] varopls; //array of variable outcome options
    private int num_of_mul=0;
    private int num_of_sum=0;
    private int factor_num=1;

    public VarEliminiationByABC(Hashtable<String, Integer> varOutcomes, Hashtable<String, Double[]> CPTs, String[] variables, String[][] varopls) {
        super(varOutcomes, CPTs, variables, varopls);
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
        HashSet vars_left = new HashSet(List.of(this.variables));
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
        while(left_factors.hasNext() && (contains(last_factor_name, ourq.getVar()))==0){ //there are might evidence tables
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
                System.out.println(Double.valueOf(df.format(parseDouble(final_facor[i][final_facor[0].length-1])))+","+num_of_sum+","+num_of_mul);
            }
        }
    }

    public void getRid(Query ourq){ //the function removes all the variables that aren't predecessors of query or evidence
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
    }
    private HashMap<String, String[][]> placeEvidence(Query ourq, HashMap<String, String[][]> factors){
        HashMap<String, String[][]> factors_tmp = (HashMap<String, String[][]>) factors.clone();
        factors_tmp.forEach((name, table) ->
        {
            HashSet evidences = new HashSet(ourq.getEvidence().keySet());
            String[] evidence_values = new String[table[0].length];
            HashSet shared_columns = new HashSet();
            //int num_of_evidences_in_table = 0;
            int num_of_row_to_div = 1; //later, we'll divide by the options of the evidences - cause we need only one option, which is given
            for (int i = 0; i < table[0].length; i++) {
                if (ourq.getEvidence().containsKey(table[0][i])){ //in this column in table we have evidence
                    evidence_values[i] = ourq.getEvidence().get(table[0][i]);//the value for this column
                    //num_of_evidences_in_table++;
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

    private boolean compareOnlyByGivenCols(String[] a, String[] b, HashSet GivenCols){
        boolean bool =true;
        Iterator iter = GivenCols.iterator();
        while (iter.hasNext()){
            int col = (int) iter.next();
            if (!a[col].equals(b[col])){
                bool=false;
                break;
            }
        }
        return bool;
    }

    public int numOfVarsInCPT(String name){
        int varnum = 0;
        StringTokenizer parts = new StringTokenizer(name, " ,|=");
        while (parts.hasMoreTokens()){
            parts.nextToken();
            varnum++;
        }
        return varnum;
    }
    public void removeEvidenceTables(Query ourq){ //removes the evidences tables cause we don't need them
        ourq.getEvidence().forEach((name, arr) ->
                CPTs.remove(name));
    }

    public HashSet factorsWithVar(String varname, HashMap<String, String[][]> factors){
        HashSet namesfactorswithvar = new HashSet();
        factors.forEach((name, table) ->
        {
            if (contains(name, varname)!=0){
                namesfactorswithvar.add(name);
            }
        });
        return namesfactorswithvar;
    }

    public HashMap<String, String[][]> join(Query ourq, String varname, HashMap<String, String[][]> factors){
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
        String[][] final_factor = marginalizeByVar(ourq,factors.get(final_factor_name), varname);
        factors.remove(final_factor_name);
        final_factor_name = final_factor_name.replace(varname, "");
        factors.put(final_factor_name, final_factor);
        return factors;
    }

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
        HashSet taken = new HashSet();
        capacity=0;
        while (loc<final_factor.length){
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
            for (int i = 0; i < final_factor.length; i++) {
                if(numofiteratiomsums==this.varOutcomes.get(varname)){
                    break;
                }
                if (!taken.contains(i) && compareWithoutCol(sum_together, final_factor[i], col_num)){
                    sum+=parseDouble(final_factor[i][rowloc]);
                    num_of_sum++;
                    numofiteratiomsums++;
                    taken.add(i);
                    b=true;
                    //capacity++;
                }
            }
            rowloc--;
            if(b==true){
                capacity++;
                sum_together[rowloc]= String.valueOf(sum);
                aftermerge[capacity] = sum_together.clone();
            }
            loc++;
        }
        return aftermerge;
    }

    private boolean compareWithoutCol(String[] sumTogether, String[] strings, int colNum) {
        boolean b = true;
        int j = 0;
        for (int i = 0; i < sumTogether.length-1; i++) {
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

    private String newFactorName(Query ourq, String factora, String factorb) {
        HashSet<String>[] comdif = commonDiffVars(ourq, factora, factorb);
        StringBuilder sb = new StringBuilder();
        Iterator com = comdif[0].iterator();
        Iterator dif = comdif[1].iterator();
        while (dif.hasNext()){
            String s = (String) dif.next();
            if (ourq.getEvidence().containsKey(s) || s.contains("-f")) {
                continue;
            }
            sb.append(s+",");
        }
        while (com.hasNext()){
            String s = (String) com.next();
            if (ourq.getEvidence().containsKey(s) || s.contains("-f")) {
                continue;
            }
            sb.append(s+",");
        }
        sb.append("-f"+factor_num);
        factor_num++;
        return sb.toString();
    }


    public int asciiSumOfCPT(String s) {
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

    public void sortBySizeThenAscii(String[][] namesizecpts, int indexstart){//sort the cpts by size
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

    public HashSet<String>[] commonDiffVars(Query ourq, String cpt1, String cpt2){
        HashSet<String>[] commondiff = new HashSet[2];
        commondiff[0] = new HashSet<>();
        commondiff[1] = new HashSet<>();
        StringTokenizer parts = new StringTokenizer(cpt1, " ,|= ");
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

    private HashMap<String, String[][]> createFactorsAfterInitialize(Query ourq, String[][] namesizecpts) {
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

    private HashMap<String, String[][]> createFactorsBeforeInitialize(Query ourq, String[][] namesizecpts) {
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
                arr[j][arr[0].length-2] = varopls[idx][(j-1)%(change_rate)];
            }
            for (int j = 1; j < length; j++){ //assigns the probability column
                arr[j][arr[0].length-1] = String.valueOf(CPTs.get(cur_CPT_name)[j-1]);
            }
            /*int evidencesize = 0;
            Iterator evidences_in_factor = ourq.getEvidence().keys().asIterator();
            while (evidences_in_factor.hasNext()){
                if(contains(cur_CPT_name, (String) evidences_in_factor.next())!=0){
                    evidencesize++;
                }
            }*/
            for (int j = 0; j < num_of_vars-1; j++) {//assigns the rest of the table
                var = arr[0][j];
                change_rate = basePosition(arr[0], j+1,arr[0].length-2, arr[0].length-2);
                idx = findIndex(this.variables, var);
                int tillchange=0;
                int numvalue=0;
                for (int k = 1; k < length; k++) {
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

    private String[][] createNewFactor(Query ourq, HashMap<String, String[][]> factors, String factora, String factorb){
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
                    getCell(ourq, factors.get(factora), newfactor[0], newfactor[i], 0, newfactor[i].length-1)
                    * getCell(ourq, factors.get(factorb), newfactor[0], newfactor[i], 0, newfactor[i].length-1));
            this.num_of_mul++;
        }
        return newfactor;
    }

    private double getCell(Query ourq, String[][] fact, String[] varsinfact, String[] values, int start_idx, int end_idx) {
        HashSet evidence_loc = new HashSet();
        for (int i = 0; i < fact[0].length; i++) {
            if(ourq.getEvidence().containsKey(fact[0][i])){
                evidence_loc.add(i);//saves all the positions of the evidences in the original factor
            }
        }
        String[] values_to_search = new String[fact[0].length];
        int idx;
        for (int i = start_idx; i < end_idx; i++) {
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

    private boolean CompareArrayIgnoringEvidence(String[] withoutevifacor, String[] withevifactor, HashSet evidenceLoc) {
        boolean b = true;
        //int place_in_withevi = 0;
        for (int j = 0; j < withevifactor.length; j++) {
            if(evidenceLoc.contains(j)){
                continue;
            }
            if (!(withevifactor[j].equals(withoutevifacor[j]))){
                b = false;
                break;
            }
            //place_in_withevi++;
        }
        return b;
    }

    private int findNumOfVarInFactor(Query ourq, String name, String var){
        StringTokenizer st = new StringTokenizer(name, " ,|=");
        int num=0;
        while (st.hasMoreTokens()){
            num++;
            if (var.equals(st.nextToken())){
                break;
            }
        }
        return num;
    }

    public int findNewFactorSize(Query ourq, String fact1, String fact2){
        HashSet<String>[] comndif = commonDiffVars(ourq, fact1, fact2);
        Iterator iter_com = comndif[0].iterator();
        Iterator iter_dif = comndif[1].iterator();
        int new_cpt_size=1;
        while (iter_com.hasNext()){
            String s = (String) iter_com.next();
            if (ourq.getEvidence().containsKey(s) || s.contains("-f")){
                continue;
            }
            new_cpt_size*= varOutcomes.get(s);
        }
        while (iter_dif.hasNext()){
            String s = (String) iter_dif.next();
            if (ourq.getEvidence().containsKey(s)|| s.contains("-f")){
                continue;
            }
            new_cpt_size*= varOutcomes.get(s);
        }
        return new_cpt_size;
    }

    private int basePosition(String[] vars_inCPT, int start_index, int end_index, int first_position){
        int mul = varOutcomes.get(vars_inCPT[first_position]);//can't forget the first variable (the main, which is the first in cpt)
        for (int i = start_index; i < end_index; i++) {
            mul*=varOutcomes.get(vars_inCPT[i]);
        }
        return mul;
    }
}
