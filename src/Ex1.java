import javax.xml.parsers.*;
import java.io.*;
import java.math.RoundingMode;
import java.text.DecimalFormat;
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
            File file=new File("C:\\Users\\shira_chesler\\IdeaProjects\\Basyan_network_reading_project\\src\\input.txt");  //creates a new file instance
            FileReader fr=new FileReader(file);   //reads the file
            BufferedReader br=new BufferedReader(fr);  //creates a buffering character input stream
            String xmlFile = br.readLine();
            String line;
            BaysianNetwork bn = loadXml(xmlFile);
            while((line=br.readLine())!=null)
            {
                int method = line.charAt(line.length()-1) -'0';
                String query = line.substring(2, line.length()-3);
                bn.execute(method, query);
                //WriteResaultToOutput(result);
            }
            fr.close();    //closes the stream and release the resources
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }

    }

    public static BaysianNetwork  loadXml(String xmlFile){
        File inputFile = new File(xmlFile);
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = null;
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            NodeList rootList = doc.getElementsByTagName("NETWORK");
            Element root = (Element)rootList.item(0);
            NodeList vars = root.getElementsByTagName("VARIABLE");

            String[] variables = new String[vars.getLength()];
            int capacity = 0;

            Hashtable<String, Integer> nVarOutcomes = new Hashtable<>();
            String[][] caloprec =new String[vars.getLength()][];
            for (int temp = 0; temp < vars.getLength(); temp++) {
                String name = ((Element)vars.item(temp)).getElementsByTagName("NAME").item(0).getTextContent();
                NodeList varEleme = ((Element)(vars.item(temp))).getElementsByTagName("OUTCOME");
                variables[capacity] = name;
                int outcomes = 0;
                caloprec[capacity] = new String[varEleme.getLength()];
                for (int i = 0; i < varEleme.getLength(); i++)
                {
                    if (varEleme.item(i).getNodeType() == Node.ELEMENT_NODE) {
                        caloprec[capacity][i] = (varEleme.item(i).getTextContent());
                        outcomes++;
                    }
                }
                nVarOutcomes.put(name, outcomes);
                capacity++;
            }
            System.out.println(Arrays.toString(variables));
            System.out.println(nVarOutcomes.toString());

            NodeList def = root.getElementsByTagName("DEFINITION");
            Hashtable<String, String> vardepend = getCPTname(def);
            System.out.println(vardepend.toString());

            Hashtable<String, Double[]> CPTs = createCPTs(vardepend, variables,  nVarOutcomes,  def);
            System.out.println(CPTs.toString());

            BaysianNetwork bn = new BaysianNetwork(nVarOutcomes, CPTs, variables, caloprec);
            return bn;

        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException(e);
        }


    }
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

    public static Hashtable<String, Double[]> createCPTs(Hashtable<String,String> vardepend, String[] variables, Hashtable<String, Integer> nVarOutcomes, NodeList def){
        Hashtable<String, Double[]> CPTs = new Hashtable<>();
        int var_num = variables.length;
        for (int i=0; i<var_num; i++){
            String var = variables[i];
            StringBuilder CPTname = new StringBuilder(var+"|");
            int CPTsize = nVarOutcomes.get(var);
            StringTokenizer dep = new StringTokenizer(vardepend.get(var), "-");
            String s = null;
            if(dep.hasMoreTokens()){
                s = dep.nextToken();
            }
            if (s==null){
                CPTname.deleteCharAt(CPTname.length()-1);
            }
            while (s!=null && nVarOutcomes.get(s)!=null){
                CPTsize*=nVarOutcomes.get(s);
                CPTname.append(s+",");
                if (dep.hasMoreTokens()){
                    s = dep.nextToken();
                }
                else{
                    break;
                }
            }
            Double CPT[] = new Double[CPTsize];
            for (int temp = 0; temp < def.getLength(); temp++) {
                NodeList cpt = def.item(temp).getChildNodes();
                if(!cpt.equals(null)){
                    for (int j = 0; j < cpt.getLength(); j++) {
                        if (cpt.item(j).getNodeName().equals("FOR") && cpt.item(j).getTextContent().equals(var)) {
                            while (!cpt.item(j).getNodeName().equals("TABLE")){
                                j++;
                            }
                            if (cpt.item(j).getNodeType() == Node.ELEMENT_NODE) {
                                if (cpt.item(j).getNodeName().equals("TABLE")) {
                                    StringTokenizer values = new StringTokenizer(cpt.item(j).getTextContent().toString(), " ");
                                    int counter = 0;
                                    while (values.hasMoreTokens()) {
                                        try {
                                            String cur = values.nextToken();
                                            System.out.println(cur);
                                            CPT[counter] = parseDouble(cur);
                                            counter++;
                                        } catch (NumberFormatException e) {
                                            values.nextToken();
                                        }
                                    }
                                    j=cpt.getLength();
                                    temp = def.getLength();
                                }
                            }
                        }
                    }
                }
            }
            System.out.println(Arrays.toString(CPT));
            CPTs.put(CPTname.toString(),CPT);
        }
        return CPTs;
    }
}
/*
דברים לסוף:
גרסת ג'אבה נכונה
כתיבה לקובץ
 */