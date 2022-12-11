import javax.xml.parsers.*;
import java.io.*;
import java.util.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import static java.lang.Double.parseDouble;

public class Ex1 {
    public static void main(String[] args) {
        try
        {
            FileWriter fw;
            BufferedWriter bw;
            PrintWriter pw;

            fw = new FileWriter("output.txt");
            bw = new BufferedWriter(fw);
            pw = new PrintWriter(bw);

            File file=new File("input.txt");  //creates a new file instance
            FileReader fr=new FileReader(file);   //reads the file
            BufferedReader br=new BufferedReader(fr);  //creates a buffering character input stream
            String xmlFile = br.readLine();
            String line;
            BayesianNetwork bn = loadXml(xmlFile, pw);
            boolean first = true;
            while((line=br.readLine())!=null)
            {
                if (!first){
                    pw.println();
                }
                int method = line.charAt(line.length()-1) -'0';
                String query = line.substring(2, line.length()-3);
                bn.execute(method, query);
                first=false;
            }
            fr.close();    //closes the stream and release the resources
            pw.flush();
            pw.close();
            bw.close();
            fw.close();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }

    }

    /**
     * The function reads the given xml, then puts the information into a BayesianNetwork object.
     * In the xml file, the function searches for the TagName "NETWORK" (beginning of network),then gets list
     * of all elements with TagName "VARIABLE". For each element, the function puts the name of the variable
     * in an array called "variables", saves the number of outcomes of the variable in a HashTable "nVarOutcomes",
     * saves all the outcomes options in an array "varopls" (shortage for "variable options list"), and creates
     * a HashTable that contains all the given probabilities for variable given its parents in network (the
     * HashTable name is CPTs, although it contains more information than a real CPT - for convenience).
     * After all this information is collected, the function sends it to the BayesianNetwork constructor and
     * creates a BayesianNetwork object, then returns it.
     * @param xmlFile - the xml file we want to load.
     * @param pw - a PrintWriter object.
     * @return - a BayesianNetwork object.
     */
    public static BayesianNetwork loadXml(String xmlFile, PrintWriter pw){
        File inputFile = new File(xmlFile);
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder;
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            NodeList rootList = doc.getElementsByTagName("NETWORK"); //beginning of network in the xml file
            Element root = (Element)rootList.item(0);
            NodeList vars = root.getElementsByTagName("VARIABLE"); //list of all variable nodes in network

            String[] variables = new String[vars.getLength()]; //will be the array of all variables in network
            int capacity = 0;

            Hashtable<String, Integer> nVarOutcomes = new Hashtable<>(); //saves the number of outcomes for an array
            String[][] varopls =new String[vars.getLength()][];
            for (int temp = 0; temp < vars.getLength(); temp++) {
                String name = ((Element)vars.item(temp)).getElementsByTagName("NAME").item(0).getTextContent();
                NodeList varEleme = ((Element)(vars.item(temp))).getElementsByTagName("OUTCOME");
                variables[capacity] = name;
                int outcomes = 0;
                varopls[capacity] = new String[varEleme.getLength()];
                for (int i = 0; i < varEleme.getLength(); i++)
                {
                    if (varEleme.item(i).getNodeType() == Node.ELEMENT_NODE) {
                        varopls[capacity][i] = (varEleme.item(i).getTextContent());
                        outcomes++;
                    }
                }
                nVarOutcomes.put(name, outcomes);
                capacity++;
            }

            NodeList def = root.getElementsByTagName("DEFINITION");
            Hashtable<String, String> vardepend = getCPTname(def);

            Hashtable<String, Double[]> CPTs = createCPTs(vardepend, variables,  nVarOutcomes,  def);

            return new BayesianNetwork(nVarOutcomes, CPTs, variables, varopls, pw);

        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException(e);
        }


    }

    /**
     * The function creates a HashTable "vardepend" (shortage for variable's dependent), and for each element
     * node in def, gets the main variable of the node (by TagName "FOR"), creates a string of all is parents,
     * when the parents' names are separated by '-' (if TagName is not "TABLE", means it's "GIVEN"), and puts the
     * parents string as value in "vardepend" for key - the name of the main variable.
     * @param def - node list of elements with TagName "DEFINITION" in xml.
     * @return Hashtable which in the keys are variables and for each key, the value is
     * a string of the variables that this specific variable dependent on (it's parents in the network).
     */
    public static Hashtable<String, String> getCPTname(NodeList def){
        Hashtable<String, String> vardepend = new Hashtable<>();
        for (int temp = 0; temp < def.getLength(); temp++) {
            NodeList cpt = def.item(temp).getChildNodes();
            String variable = null;

            StringBuilder dependencies = new StringBuilder();
            boolean got_depend = false;
            for (int i= 0; i<cpt.getLength(); i++){
                if (cpt.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    if (cpt.item(i).getNodeName().equals("FOR")){
                        variable = cpt.item(i).getTextContent();
                    }
                    else if(!cpt.item(i).getNodeName().equals("TABLE")){
                        dependencies.append(cpt.item(i).getTextContent()).append("-");
                        got_depend = true;
                    }
                }
            }
            if (got_depend){
                dependencies.deleteCharAt(dependencies.length()-1);
            }
            vardepend.put(variable, dependencies.toString());
        }
        return vardepend;
    }

    /**
     * The function creates a new Hashtable named "CPTs". For each variable in the network, it creates its CPT
     * name by the following scheme: the name of the variable is first, then there is a "|", and after that
     * the variables that the variable depends on (its parents in the network), separated by ",".
     * After it creates a name, the function searches in the NoseList def, for the current variable's node.
     * Then it gets by TagName "TABLE" to the values of the wanted (so called) CPT, puts it in an array
     * and puts the CPT-array as value in CPTs with the key-name computed at the beginning.
     * @param vardepend - Hashtable which in the keys are variables and for each key, the value is
     *                  a string of the variables that this specific variable dependent on
     *                  (it's parents in the network).
     * @param variables - array of all the variables in the network.
     * @param nVarOutcomes - Hashtable which in the keys are variables and for each key, the value is
     *                     the number of outcomes it has in the network.
     * @param def - node list of elements with TagName "DEFINITION" in xml.
     * @return A Hashtable ("CPTs") which in the keys are names of CPTs and for each key, the value is the CPT itself
     * (actually it's not a real CPT, for it contains more information, but we used this name for convenience).
     */
    public static Hashtable<String, Double[]> createCPTs(Hashtable<String,String> vardepend, String[] variables, Hashtable<String, Integer> nVarOutcomes, NodeList def){
        Hashtable<String, Double[]> CPTs = new Hashtable<>();
        for (String var : variables) {
            StringBuilder CPTname = new StringBuilder(var + "|");
            int CPTsize = nVarOutcomes.get(var);
            StringTokenizer dep = new StringTokenizer(vardepend.get(var), "-");
            String s = null;
            if (dep.hasMoreTokens()) {
                s = dep.nextToken();
            }
            if (s == null) {
                CPTname.deleteCharAt(CPTname.length() - 1);
            }
            while (s != null && nVarOutcomes.get(s) != null) {
                CPTsize *= nVarOutcomes.get(s);
                CPTname.append(s).append(",");
                if (dep.hasMoreTokens()) {
                    s = dep.nextToken();
                } else {
                    break;
                }
            }
            Double[] CPT = new Double[CPTsize];
            for (int temp = 0; temp < def.getLength(); temp++) {
                NodeList cpt = def.item(temp).getChildNodes();
                if (!cpt.equals(null)) {
                    for (int j = 0; j < cpt.getLength(); j++) {
                        if (cpt.item(j).getNodeName().equals("FOR") && cpt.item(j).getTextContent().equals(var)) {
                            while (!cpt.item(j).getNodeName().equals("TABLE")) {
                                j++;
                            }
                            if (cpt.item(j).getNodeType() == Node.ELEMENT_NODE) {
                                if (cpt.item(j).getNodeName().equals("TABLE")) {
                                    StringTokenizer values = new StringTokenizer(cpt.item(j).getTextContent().toString(), " ");
                                    int counter = 0;
                                    while (values.hasMoreTokens()) {
                                        try {
                                            String cur = values.nextToken();
                                            CPT[counter] = parseDouble(cur);
                                            counter++;
                                        } catch (NumberFormatException e) {
                                            values.nextToken();
                                        }
                                    }
                                    j = cpt.getLength();
                                    temp = def.getLength();
                                }
                            }
                        }
                    }
                }
            }
            CPTs.put(CPTname.toString(), CPT);
        }
        return CPTs;
    }
}
