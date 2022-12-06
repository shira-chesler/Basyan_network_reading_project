import java.awt.*;
import java.util.*;

public class VarEliminiationByABC extends BaysianNetwork{
    private String[] variables;
    private Hashtable<String, Integer> varOutcomes;
    private Hashtable<String, Double[]> CPTs;
    private String[][] varopls; //array of variable outcome options
    private int num_of_mul=0;
    private int num_of_sum=0;

    public VarEliminiationByABC(Hashtable<String, Integer> varOutcomes, Hashtable<String, Double[]> CPTs, String[] variables, String[][] varopls) {
        super(varOutcomes, CPTs, variables, varopls);
        this.variables = variables;
        this.varOutcomes = varOutcomes;
        this.CPTs = CPTs;
        this.varopls = varopls;
    }

    public void variableEliminationByABC(Query ourq) {
        getRid(ourq);
        placeEvidence(ourq);
        for (int i = 0; i < variables.length; i++) {
            if (variables[i].equals(ourq.getVar()) || ourq.getEvidence().containsKey(variables[i])){
                continue;
            }
            join(variables[i]);
        }
    }

    public void getRid(Query ourq){ //the function removes all the variables that aren't predecessors of query or evidence
        String[] in_op = new String[this.variables.length];
        int num_of_vars=0; //counter to know the number of wanted variable
        in_op[num_of_vars] = ourq.getVar();
        num_of_vars++;
        Set varstokeepst = ourq.getEvidence().keySet(); //set of the variables we need in calculation, initialized by evidence variables
        HashSet<String > varstokeep = new HashSet<>(varstokeepst);
        Iterator iter = varstokeep.iterator(); //iterates through the options
        CPTs.forEach((S, arr) -> //adding to the set the parents of the query (its' dependencies)
        {
            StringTokenizer cName = new StringTokenizer(S, " ,|=");
            if (cName.nextToken().equals(ourq.getVar())){ //checks we got the querys' cpt
                while (cName.hasMoreTokens()){ //adds the dependencies of the querys' in the cpt (it's parents in the net)
                    String parent = cName.nextToken();
                    varstokeep.add(parent);
                }
            }
        });
        while (iter.hasNext()){
            String temp = (String) iter.next();
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
                        varstokeep.add(cName.nextToken());
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

    public void placeEvidence(Query ourq){
        removeEvidenceTables(ourq);
        ourq.getEvidence().forEach((evidence, value) ->
        {
            int idx = findIndex(variables,evidence);
            Hashtable<String, Double[]> CPTs_temp = (Hashtable<String, Double[]>) CPTs.clone();
            CPTs_temp.forEach((name, table) ->
            {
                int varnum = contains(name,evidence);
                if(contains(name,evidence)!=0){//note the fact that due to removeEvidenceTables function varnum will never be 1
                    int num_of_out = this.varOutcomes.get(evidence);//num of outcomes for evidence
                    Double[] arr = new  Double[(table.length/num_of_out)];
                    int givenout = findIndex(this.varopls[idx], value); //num of outcome in possible outcomes
                    int counter=0;
                    int numofvars = numOfVars(name);
                    for (int i = 0; i < arr.length; i++) {
                        if (i%Math.pow(num_of_out,numofvars-varnum)==givenout){ //if the row is with the given evidence value
                            arr[counter] = table[i];
                            counter++;
                        }
                    }
                    StringBuilder st = new StringBuilder(name);
                    st.deleteCharAt((2*varnum)-2);
                    CPTs.put(st.toString(), arr);
                    CPTs.remove(name);
                }
            });
        });
    }

    public int contains(String origin, String isin){//return which var num in the cpt the var we're searching is
        boolean b = false;
        int varnum = 0;
        StringTokenizer parts = new StringTokenizer(origin, " ,|=");
        while (parts.hasMoreTokens()){
            varnum++;
            if (isin.equals(parts.nextToken())){
                b = true;
                break;
            }
        }
        if(b){
            return varnum;
        }
        return 0;
    }

    public int numOfVars(String name){
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
        {
            CPTs.remove(name);
        });
    }

    public HashSet cptsWithVar(String varname){
        HashSet namescptswithvar = new HashSet();
        this.CPTs.forEach((name, table) ->
        {
            if (contains(name, varname)!=0){
                namescptswithvar.add(name);
            }
        });
        return namescptswithvar;
    }

    public void join(String varname){
        HashSet cptswithvar = cptsWithVar(varname); //contains the names of the cpts with the var
        Iterator iter = cptswithvar.iterator();

    }

    public HashSet<String>[] commonDiffVars(String cpt1, String cpt2){
        HashSet<String>[] commondiff = new HashSet[2];
        StringTokenizer parts = new StringTokenizer(cpt1, " ,|=");
        while (parts.hasMoreTokens()){
            String s = parts.nextToken();
            if(contains(cpt2, s)!=0){
                commondiff[0].add(s);
            }
            else {
                commondiff[1].add(s);
            }
        }
        return commondiff;
    }
}
/*
ליצור פונקציית - סט שמות טבלאות שהvar נמצא בהם
 */
