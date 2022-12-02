import java.util.HashSet;
import java.util.Iterator;

public class MyHashSet extends HashSet<String[]> {
    @Override
    public boolean contains(Object o2) {
        if(o2==null){
            return true;
        }
        String[] myarr = (String[]) o2;
        Iterator iter = this.iterator();
        if (!iter.hasNext()){
            return false;
        }
        String[] cur = (String[]) iter.next();
        for (int i = 0; i < myarr.length; i++) {
            if (!myarr[i].equals(cur[i])) {
                break;
            }
            if (i == myarr.length - 1) {
                return true;
            }
        }
        while (iter.hasNext()){
            cur = (String[]) iter.next();
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
