package EntryPoint;

import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.AllApplicationEntrypoints;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.util.config.AnalysisScopeReader;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;
import static slice.main.buildCallGraph;

//import randoop.TestValue;


public class point {
    static String jarPath="G:\\java\\confD\\lib\\hadoop\\hdfs\\hadoop-hdfs-2.10.2.jar;G:\\java\\confD\\lib\\hadoop\\mapreduce\\hadoop-mapreduce-client-app-2.10.2.jar;" +
            "G:\\java\\confD\\lib\\hadoop\\mapreduce\\hadoop-mapreduce-client-core-2.10.2.jar;G:\\java\\confD\\lib\\hadoop\\mapreduce\\hadoop-mapreduce-client-common-2.10.2.jar;" +
            "G:\\java\\confD\\lib\\hadoop\\mapreduce\\hadoop-mapreduce-client-hs-2.10.2.jar;G:\\java\\confD\\lib\\hadoop\\yarn\\hadoop-yarn-client-2.10.2.jar;" +
            "G:\\java\\confD\\lib\\hadoop\\yarn\\hadoop-yarn-common-2.10.2.jar;G:\\java\\confD\\lib\\hadoop\\yarn\\hadoop-yarn-api-2.10.2.jar";
    static List<String> jarsList;

//    static {
//        try {
//            jarsList = getJars("G:\\java\\confD\\lib\\hadoop");
//            jarsList.forEach(jar->{jarPath+=jar+";";});
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    //    static String jarPath = "./lib/test.jar";
    static String exclusionsFilePath = "JavaAllExclusions.txt";
    static File exclusionsFile = new File(exclusionsFilePath);
    static File jarFile = new File(jarPath);

    static Iterable<Entrypoint> entrypoints;
    static AnalysisCache cache = null;
    static AnalysisScope scope =null;
    static ClassHierarchy cha =null;
    static CallGraphBuilder builder =null;
    static CallGraph cg = null;

    public HashSet<methodClass> methodList = new HashSet<>();

    static public AnalysisScope buildScope( String classPath, File exclusionsFile) throws IOException {
        return AnalysisScopeReader.makeJavaBinaryAnalysisScope(classPath,exclusionsFile);
    }
    static public ClassHierarchy buildHierarchy(AnalysisScope scope) throws ClassHierarchyException {
        return ClassHierarchyFactory.makeWithRoot(scope);
    }
    public static void main(String[] args) throws IOException, ClassHierarchyException, CallGraphBuilderCancelException {
        TreeSet<String> methodSet = new TreeSet<>();
        TreeSet<String> clazzSet = new TreeSet<>();


        cache = new AnalysisCacheImpl();

        scope = buildScope(jarPath, exclusionsFile);
        cha = buildHierarchy(scope);

        entrypoints = new AllApplicationEntrypoints(scope, cha);

        AnalysisOptions options = new AnalysisOptions(scope, entrypoints);

        builder = buildCallGraph(options, cache, scope, cha);

        cg = builder.makeCallGraph(options, null);
        System.err.println("node number:");
        System.err.println(cg.getNumberOfNodes());


        for (CGNode node : cg) {
            String clazz = node.getMethod().getDeclaringClass().toString();

            if (clazz.contains("hadoop")) {
                methodSet.add(node.getMethod().toString());
                clazzSet.add(node.getMethod().getDeclaringClass().toString());
//                System.out.println("clazz:"+clazz);
//                if(clazz.contains("DefaultSpeculator")){
//                    System.err.println("clazz:"+clazz);
//                    System.out.println();
//                }
            }
        }

//        methodSet.forEach(System.out::println);
        System.out.println("methodSet.size()"+methodSet.size());
//        clazzSet.forEach(System.err::println);
        System.err.println("clazzSet.size()"+clazzSet.size());
//        System.out.println();
//        System.out.println();

        System.out.println("writing to TXT");
        getClassPath(clazzSet);
    }
    public static List<String> getJars(String dir) throws IOException {

        List<String> allJars = new LinkedList<>();
        File sdir = new File(dir);
        if(!sdir.isDirectory()){
            System.err.println(dir + "is not a directory!");
        }

        //过滤出目录
//        try (Stream<Path> paths = Files.walk(Paths.get(dir))) {
//            paths.filter(Files::isDirectory)
//                    .forEach(System.out::println);
//        }

        //按后缀名过滤
        try (Stream<Path> paths = Files.walk(Paths.get(dir), 10)) {
            paths.map(Path::toString).filter(f -> f.endsWith(".jar"))
                    /**
                     * 过滤包含test的jar包
                     */
                    .filter(not(f->f.contains("test")))
//                    .forEach(System.out::println);
                    .forEach(allJars::add);

        }

        /**
         * 只包含hadoop的jar包
         */
        List<String> jars = new LinkedList<>();
        allJars.forEach(jar->{
            if(new File(jar).getName().contains("hadoop") && !new File(jar).getName().contains("source")){
                jars.add(jar);
            }
        });

//        return allJars;
        return jars;
    }
    public static void getClassPath(TreeSet<String> classSet) throws IOException {
        TreeSet<String> newSet = new TreeSet<String>();
        Iterator it = classSet.iterator();
        while (it.hasNext()){
//            Lorg/apache/hadoop/filecacheDistributedCache>
//            String[] arr = it.next().toString().split(",");
            String s1 = it.next().toString().replace("<Application,L","");
            s1=s1.replace(">","");
//            arr[1] = arr[1].replace("/",".");
            newSet.add(s1.replace("/","."));
        }
//        System.out.print(newSet.toString());
        output(newSet);
    }
    public static String trimStartEnd(String s){
        return s.substring(1,s.length() -1);
//        char[] arr = s.toCharArray();
//        char[] ret = new char[arr.length -2];
//        for (int i = 0; i < ret.length; i++) {
//            ret[i] = arr[i+1];
//        }
//        return String.copyValueOf(ret);
    }
    public static void output(TreeSet<String> set) throws IOException {
//        File writeMethod = new File("./lib/methodSet.txt");
        File writeClazz = new File("./lib/clazzSet.txt");
//        writeMethod.createNewFile();
        writeClazz.createNewFile();
//        FileWriter writer1 = new FileWriter(writeMethod);
        FileWriter writer2 = new FileWriter(writeClazz);
//        BufferedWriter out1 = new BufferedWriter(writer1);
        BufferedWriter out2 = new BufferedWriter(writer2);
//        out1.write(methodSet.toString());
//        out1.flush();
        out2.write(set.toString());
        out2.flush();
//        out1.close();
        out2.close();
    }
    @Test
    public void test1() throws IOException {
        TreeSet<String> ts = new TreeSet<String>();
        ts.add("<Application,Lorg/apache/hadoop/filecache/DistributedCache>");
        ts.add("<Application,Lorg/apache/hadoop/mapred/AuditLogger>");
        ts.add("<Application,Lorg/apache/hadoop/mapred/BackupStore$BackupRamManager>");
        getClassPath(ts);
    }
}
