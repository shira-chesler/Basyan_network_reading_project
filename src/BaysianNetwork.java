import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;

public class BaysianNetwork {
    private String[] variables;
    private Hashtable<String, Integer> varOutcomes;
    private Hashtable<String, Double[]> CPTs;
    private HashSet[] varopls;

    public BaysianNetwork(Hashtable<String, Integer> varOutcomes, Hashtable<String, Double[]> CPTs, String[] variables, HashSet[] varopls){
        this.varOutcomes = varOutcomes;
        this.CPTs = CPTs;
        this.variables = variables;
        this.varopls = varopls;
    }

    public Double simpleConc(String query){
        StringTokenizer readquery = new StringTokenizer(query, " ,|=");
        Hashtable<String, String> given = new Hashtable<String, String>();
        Query ourq = new Query(readquery.nextToken(), readquery.nextToken()); //seperates the query
        while (readquery.hasMoreTokens()){
            given.put(readquery.nextToken(),readquery.nextToken()); //putting the evidences into the HashTable
        }
        ourq.addEvidence(given);
        /*int n_hidden=0;
        int n_combinations=1;
        HashSet hidden = new HashSet();
        for (int i=0; i< this.variables.length; i++){
            String var = this.variables[i]; // the current var we're looking at
            if (!given.contains(var) && !ourq.getVar().equals(var)){ //checks that the var is a hidden
                n_hidden++;
                n_combinations*=this.varOutcomes.get(var);
                hidden.add(var);
            }
        }

        Iterator options = hidden.iterator();
        while (options.hasNext()){
            cur_relevant_hidoptions.next();
        }
*/
    }

    public void calOpRec(String[] arr, int idx, Query q){
        if (idx>=arr.length){
            q.addOption(arr);
            return;
        }
        if (q.getEvidence().containsKey(this.variables[idx])){
            arr[idx] = q.getEvidence().get(this.variables[idx]);
            calOpRec(arr, idx+1, q);
        }
        else {
            Iterator options = this.varopls[idx].iterator();
            while (options.hasNext()){
               arr[idx] = options.next().toString();
               calOpRec(arr, idx+1, q);
            }
        }
    }
}

