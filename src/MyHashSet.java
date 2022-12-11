import java.util.HashSet;
import java.util.Iterator;

public class MyHashSet extends HashSet<String[]> {
    /**
     * The function checks contains by value of strings in arrays in set and not by reference.
     * @param o2 element whose presence in this set is to be tested
     * @return boolean - equals or not
     */
    @Override
    public boolean contains(Object o2) {
        if(o2==null){
            return true;
        }
        String[] myarr = (String[]) o2;
        Iterator<String[]> iter = this.iterator();
        if (!iter.hasNext()){
            return false;
        }
        String[] cur = iter.next();
        for (int i = 0; i < myarr.length; i++) {
            if (!myarr[i].equals(cur[i])) {
                break;
            }
            if (i == myarr.length - 1) {
                return true;
            }
        }
        while (iter.hasNext()){
            cur = iter.next();
            for (int i = 0; i < myarr.length; i++) {
                if (!myarr[i].equals(cur[i])) {
                    break;
                }
                if (i == myarr.length - 1) {
                    return true;
                }
            }
        }
            return false;
        }
}
