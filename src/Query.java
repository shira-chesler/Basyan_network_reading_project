import java.util.Comparator;
import java.util.Hashtable;

public class Query{
    private String var;
    private String value;
    private Hashtable<String, String> evidence;
    private MyHashSet ex_options;

    public Query(String var, String value){
        this.var = var;
        this.value = value;
        ex_options = new MyHashSet();
    }

    public String getVar(){
        return  this.var;
    }

    public String getValue(){
        return this.value;
    }

    public void addEvidence(Hashtable<String, String> evidence){
        this.evidence = evidence;
    }
    public Hashtable<String, String> getEvidence(){
        return this.evidence;
    }

    public void addOption(String[] arr){
        if (!this.ex_options.contains(arr)){
            this.ex_options.add(arr.clone());
        }
        if (this.ex_options.contains(null)){
            this.ex_options.remove(null);
        }
    }

    public MyHashSet getEx_options() {
        return ex_options;
    }

    public void changeOptions(int idx_to_change, String value){
        ex_options.forEach((arr) -> {arr[idx_to_change] = value;});
        this.value = value;
    }

}
