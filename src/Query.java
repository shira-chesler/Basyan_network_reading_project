import java.util.HashSet;
import java.util.Hashtable;

public class Query {
    private String var;
    private String value;
    private Hashtable<String, String> evidence;
    private HashSet ex_options;

    public Query(String var, String value){
        this.var = var;
        this.value = value;
    }

    public String getVar(){
        return  this.var;
    }

    public void addEvidence(Hashtable<String, String> evidence){
        this.evidence = evidence;
    }
    public Hashtable<String, String> getEvidence(){
        return this.evidence;
    }

    public void addOption(String[] arr){
        this.ex_options.add(arr.clone());
    }
}
