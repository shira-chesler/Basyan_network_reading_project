import java.util.Comparator;
import java.util.Hashtable;

public class Query{
    private String var;
    private String value;
    private Hashtable<String, String> evidence;
    private MyHashSet ex_options;

    /**
     * Query constructor.
     * @param var - the variable that is the main variable of the query.
     * @param value - the value of the variable that is the main variable of the query.
     */
    public Query(String var, String value){
        this.var = var;
        this.value = value;
        ex_options = new MyHashSet();
    }

    /**
     * The function returns the variable that is the main variable of the query.
     * @return the variable that is the main variable of the query.
     */
    public String getVar(){
        return  this.var;
    }

    /**
     * The function returns the value of the variable that is the main variable of the query.
     * @return the value of the variable that is the main variable of the query.
     */
    public String getValue(){
        return this.value;
    }

    /**
     * The function places the evidences in the query object.
     * @param evidence - Hashtable of evidences (keys are the variable evidences, values are their values).
     */
    public void addEvidence(Hashtable<String, String> evidence){
        this.evidence = evidence;
    }

    /**
     * The function returns Hashtable of evidences (keys are the variable evidences, values are their values).
     * @return - Hashtable of evidences (keys are the variable evidences, values are their values).
     */
    public Hashtable<String, String> getEvidence(){
        return this.evidence;
    }

    /**
     * The function adds a new combination option - outcomes combination of variables.
     * @param arr - array of outcomes combination of variables
     */
    public void addOption(String[] arr){
        if (!this.ex_options.contains(arr)){
            this.ex_options.add(arr.clone());
        }
        if (this.ex_options.contains(null)){
            this.ex_options.remove(null);
        }
    }

    /**
     * The function returns MyHashSet of all outcomes combination of variables.
     * @return MyHashSet of all outcomes combination of variables.
     */
    public MyHashSet getEx_options() {
        return ex_options;
    }

    /**
     * The function iterates through all options ex_options and changes in each one the value in idx_to_change
     * entrance to value.
     * @param idx_to_change - in what column in options to change the values.
     * @param value - what value to change to.
     */
    public void changeOptions(int idx_to_change, String value){
        ex_options.forEach((arr) -> {arr[idx_to_change] = value;});
        this.value = value;
    }

}
