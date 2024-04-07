package slice;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.AllApplicationEntrypoints;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.pruned.ApplicationLoaderPolicy;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.slicer.*;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.MapUtil;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.modref.ModRef;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.OrdinalSet;

import javax.swing.plaf.synth.SynthOptionPaneUI;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class main {
//    static String jarPath ="./lib/hadoop-hdfs-2.6.5.jar;./lib/hadoop-common-2.6.5.jar";./lib/hadoop2.9.2/hadoop-common-2.9.2.jar
    static String jarPath ="./lib/hadoop2.9.2/hadoop-common-2.9.2.jar;./lib/yarn/hadoop-yarn-server-applicationhistoryservice-2.9.2.jar";
    static String exclusionsFilePath = "JavaAllExclusions.txt";
    static File exclusionsFile = new File(exclusionsFilePath);
    static String confName = "39 = invokevirtual";
    static String methodName="<init>";
    static String fullClassName="Lorg/apache/hadoop/yarn/server/timeline/LeveldbTimelineStore";
    static AnalysisScope scope =null;
    static ClassHierarchy cha =null;
    static CallGraphBuilder builder =null;
    static Iterable<Entrypoint> Mainentrypoints;
    static Iterable<Entrypoint> entrypoints;
    static AnalysisCache cache = null;
    static CallGraph cg = null;
    static Collection<Statement> collections =null;
    static PointerAnalysis pa;
    static ModRef modRef;
    static Map<CGNode, OrdinalSet<PointerKey>> ref;
    static Map<CallSiteReference, Set<Statement>> callerParamStatements = HashMapFactory.make();
    static Map<CallSiteReference, Set<Statement>> callerReturnStatements = HashMapFactory.make();
    static SlowSparseNumberedGraph<Statement> delegate = SlowSparseNumberedGraph.make();
    static Slicer.DataDependenceOptions dOptions = Slicer.DataDependenceOptions.NONE;
    static Map<CGNode, OrdinalSet<PointerKey>> mod;

    public static void main(String[] args) throws IOException, ClassHierarchyException, CancelException {
        Mainentrypoints = new LinkedList<Entrypoint>();

        cache = new AnalysisCacheImpl();
        collections = new LinkedList<Statement>();

        scope = buildScope(jarPath, exclusionsFile);
        cha = buildHierarchy(scope);

        entrypoints = new AllApplicationEntrypoints(scope, cha);
//        entrypoints = Util.makeMainEntrypoints(scope,cha);
//        System.out.println();


        AnalysisOptions options = new AnalysisOptions(scope, entrypoints);

        builder = buildCallGraph(options, cache, scope, cha);

        cg = builder.makeCallGraph(options, null);
        System.err.println("node number:");
        System.err.println(cg.getNumberOfNodes());

        Collection<Statement> tempCollections = new LinkedList<Statement>();
        boolean flag = false;
        System.out.println("node");

        for (CGNode node : cg) {
            if (flag)
                break;
            if (fullClassName.equals(node.getMethod().getDeclaringClass().getName().toString())) {
                System.out.println("test");
                System.out.println(node.getMethod().getName().toString());
                System.out.println("--------------------------------------------");
            }
        }
//                if (methodName.equals(node.getMethod().getName().toString())) {   ///init
//                    IR ir = node.getIR();
//                    SSAInstruction[] ssaInstructions = ir.getInstructions();
//                    for (int i = 0; i < ssaInstructions.length; i++) {
//                        SSAInstruction s = ssaInstructions[i];
//                        if (s instanceof SSAInstruction) {
//                            System.out.println(s.toString());
//                        }
//                    }
//                }
//                if (methodName.equals(node.getMethod().getName().toString())){
//
//                    List<SSAInstruction> ls = getSSA(node);
//                    ls.removeAll(Collections.singleton(null));
//                    int size = ls.size();
//                    for (int i=0;i<size;i++){
//                        if (ls.get(i).toString().contains("getfield") && ls.get(i).toString().contains(confName)){
//                            int index = getInstructionIndex(node, ls.get(i));
//                            Statement s = new NormalStatement(node, index);
//                            System.out.println(s.toString());
//                            tempCollections = Slicer.computeForwardSlice(s, cg, builder.getPointerAnalysis(), Slicer.DataDependenceOptions.NO_BASE_NO_HEAP, Slicer.ControlDependenceOptions.NO_EXCEPTIONAL_EDGES);
//                            flag = true;
//                        }
//                    }
//                }
//        System.out.println(tempCollections);


//        collections =makeSliceFromConf(fullClassName,seed,cg);
        /*

        collections = makeSliceFromMehtod(fullClassName,methodName,confName,cg);

        if (collections !=null){
            ApplicationLoaderPolicy applicationLoaderPolicy = ApplicationLoaderPolicy.INSTANCE;

            Iterator<Statement> pruneIterator = collections.iterator();

            Statement sliceStatement = null;

            while (pruneIterator.hasNext()) {
                sliceStatement = pruneIterator.next();
                CGNode node = sliceStatement.getNode();
                if (!applicationLoaderPolicy.check(node)) {
                    pruneIterator.remove();
                }
            }
            System.out.println("-------------------------------------------------------------slice statement");
            outputSlice(collections);
        }else{
            System.err.println("slice collection is null!");
        }*/

    }
    static public void addParamPassingStatement(int callIndex,CGNode node,Map<CGNode,OrdinalSet<PointerKey>> ref,IR ir){
        SSAAbstractInvokeInstruction call = (SSAAbstractInvokeInstruction) ir.getInstructions()[callIndex];
        Collection<Statement> params = MapUtil.findOrCreateSet(callerParamStatements, call.getCallSite());
        Collection<Statement> rets = MapUtil.findOrCreateSet(callerReturnStatements, call.getCallSite());

        for (int j = 0; j < call.getNumberOfUses(); j++) {
            Statement st = new ParamCaller(node, callIndex, call.getUse(j));
            delegate.addNode(st);
            params.add(st);
        }
        if (!call.getDeclaredResultType().equals(TypeReference.Void)) {
            Statement st = new NormalReturnCaller(node, callIndex);
            delegate.addNode(st);
            rets.add(st);
        }
        {
            if (!dOptions.isIgnoreExceptions()) {
                Statement st = new ExceptionalReturnCaller(node, callIndex);
                delegate.addNode(st);
                rets.add(st);
            }
        }

        if (!dOptions.isIgnoreHeap()) {
            OrdinalSet<PointerKey> uref = unionHeapLocations(cg, node, call, ref);
            for (PointerKey p : uref) {
                Statement st = new HeapStatement.HeapParamCaller(node, callIndex, p);
                delegate.addNode(st);
                params.add(st);
            }
            OrdinalSet<PointerKey> umod = unionHeapLocations(cg, node, call, mod);
            for (PointerKey p : umod) {
                Statement st = new HeapStatement.HeapReturnCaller(node, callIndex, p);
                delegate.addNode(st);
                rets.add(st);
            }
        }
    }
    static public OrdinalSet<PointerKey> unionHeapLocations(CallGraph cg, CGNode n, SSAAbstractInvokeInstruction call,Map<CGNode, OrdinalSet<PointerKey>> loc) {
        BitVectorIntSet bv = new BitVectorIntSet();
        for (CGNode t : cg.getPossibleTargets(n, call.getCallSite())) {
            bv.addAll(loc.get(t).getBackingSet());
        }
        return new OrdinalSet<PointerKey>(bv, loc.get(n).getMapping());
    }
    static public AnalysisScope buildScope( String classPath, File exclusionsFile) throws IOException {
        return AnalysisScopeReader.makeJavaBinaryAnalysisScope(jarPath,exclusionsFile);
    }
    static public ClassHierarchy buildHierarchy(AnalysisScope scope) throws ClassHierarchyException {
        return ClassHierarchyFactory.makeWithRoot(scope);
    }
    static public Iterable<Entrypoint> makeEntrypoints(AnalysisScope scope,ClassHierarchy cha){
        return com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha);
    }
    static public CallGraphBuilder buildCallGraph(AnalysisOptions options,AnalysisCache cache,AnalysisScope scope,ClassHierarchy cha){
        return Util.makeVanillaZeroOneCFABuilder(Language.JAVA,options,cache,cha,scope);
    }
    static public List<SSAInstruction> getSSA(CGNode node){

        List<SSAInstruction> list = new LinkedList<SSAInstruction>();

        SSAInstruction[] instructions = node.getIR().getInstructions();
        for (SSAInstruction ins:instructions){
            list.add(ins);
        }
        return list;
    }

    public static int getInstructionIndex(CGNode node,SSAInstruction instr){
        int index =-1;
        SSAInstruction[] instructions = node.getIR().getInstructions();

        for (int i=0;i<instructions.length;i++){
            if (instr == instructions[i]){
                index = i;
                break;
            }
        }
        return index;
    }

    static public Collection<Statement> makeSlice(String seed,  CallGraph cg) throws CancelException {
        Collection<Statement> tempCollections =new LinkedList<Statement>();
        Collection<Statement> Collections =new LinkedList<Statement>();
        List<Statement> statements =new LinkedList<Statement>();
        Statement temp = null;

        for (CGNode node:cg){
//            System.out.println("node.getMethod().getName().toString()");
//            System.out.println(node.getMethod().getDeclaringClass().getName().toString());

            if (node.getMethod().getName().toString().equals(seed)){
                List<SSAInstruction> ls = getSSA(node);
                int size = ls.size();
                for (int i=0;i<size;i++){
                    if (ls.get(i) != null){
                        int index = getInstructionIndex(node, ls.get(i));
                        Statement s = new NormalStatement(node, index);
                        statements.add(s);
                        temp =s;
                    }
                }
            }
        }

        tempCollections = Slicer.computeForwardSlice(temp,cg , builder.getPointerAnalysis(), Slicer.DataDependenceOptions.NO_BASE_NO_HEAP, Slicer.ControlDependenceOptions.NONE);

        for (Statement s:tempCollections){
            if (s.getKind().toString().equals("NORMAL"))
                Collections.add(s);
        }

        return Collections;
    }

    static public Collection<Statement> makeSlice(String className,String seed,  CallGraph cg) throws CancelException {
        Collection<Statement> tempCollections =new LinkedList<Statement>();
        Collection<Statement> Collections =new LinkedList<Statement>();

        boolean classFlag = false;
        boolean seedFlag = false;

        boolean flag = false;

        for (CGNode node:cg){
            if (flag)
                break;
            if(className.equals(node.getMethod().getDeclaringClass().getName().toString())){
                classFlag =true;
                List<SSAInstruction> ls = getSSA(node);
                int size = ls.size();
                for (int i=0;i<size;i++){
                    if (ls.get(i) !=null){
                        if (ls.get(i).toString().contains(seed)){
                            seedFlag =true;
                            flag = true;
                            int index = getInstructionIndex(node, ls.get(i-1));
                            Statement s = new NormalStatement(node, index);
                            System.out.println("-----------------------------statement---------------------------");
                            if (s==null)
                                System.out.println("null");
                            else
                                System.out.println("test");

                            tempCollections = Slicer.computeForwardSlice(s,cg , builder.getPointerAnalysis(), Slicer.DataDependenceOptions.NO_BASE_NO_HEAP, Slicer.ControlDependenceOptions.NO_EXCEPTIONAL_EDGES);
                            break;
                        }
                    }
                }
            }
        }

        // 第一步过滤 去掉非normal的
        /// 去掉输出
        if (classFlag == false){
            System.err.println("there is no such class named:" + className);
            return null;
        }
        else if (seedFlag == false){
            System.err.println("there is no such seed named:" + seed);
            return null;
        }

        for (Statement s:tempCollections){
            if (s.getKind().toString().equals("NORMAL")
                    && !s.toString().contains("Ljava/"))
                Collections.add(s);
        }

        return Collections;
    }


    static public Collection<Statement> makeSliceFromConf(String fullClassName,String seed,CallGraph cg) throws CancelException {
        Collection<Statement> tempCollections =new LinkedList<Statement>();
        Collection<Statement> Collections =new LinkedList<Statement>();

        boolean flag =false;
        for (CGNode node:cg){
            if (flag)
                break;
            if (fullClassName.equals(node.getMethod().getDeclaringClass().getName().toString())){
                List<SSAInstruction> ls = getSSA(node);
                int size = ls.size();
                for (int i = 0; i < size; i++) {
                    if (ls.get(i) !=null && ls.get(i).toString().contains(seed) && ls.get(i).toString().contains("getfield")){
                        int index = getInstructionIndex(node, ls.get(i));
                        Statement s = new NormalStatement(node, index);
                        System.out.println("--------------------------statement---------------------------");
                        System.out.println(s.toString());
                        tempCollections = Slicer.computeForwardSlice(s,cg , builder.getPointerAnalysis(), Slicer.DataDependenceOptions.NO_BASE_NO_HEAP, Slicer.ControlDependenceOptions.NO_EXCEPTIONAL_EDGES);
                        flag = true;
                        break;
                    }
                }
            }
        }
        for (Statement s:tempCollections){
            if (s.getKind().toString().equals("NORMAL")
                    && !s.toString().contains("Ljava/")
            )
                Collections.add(s);
        }
        return Collections;
    }

    static public Collection<Statement> makeSliceFromMehtod(String fullClassName,String methodName,String confName,CallGraph cg) throws CancelException {
        Collection<Statement> tempCollections = new LinkedList<Statement>();
        Collection<Statement> Collections = new LinkedList<Statement>();

        boolean flag = false;
        for (CGNode node : cg) {
            if (flag)
                break;
            if (fullClassName.equals(node.getMethod().getDeclaringClass().getName().toString())) {
//                System.out.println(node.getMethod().getName().toString());
                if (methodName.equals(node.getMethod().getName().toString())) {
                    List<SSAInstruction> ls = getSSA(node);
                    ls.removeAll(java.util.Collections.singleton(null));
                    int size = ls.size();
                    for (int i = 0; i < size; i++) {
//                        System.out.println(ls.get(i).toString());

                        if (ls.get(i).toString().contains(confName)){
//                        if (ls.get(i).toString().contains("getfield") && ls.get(i).toString().contains(confName)){
                            System.out.println("----------------------seed----------------------");
                            System.out.println(ls.get(i).toString());

                            int index = getInstructionIndex(node, ls.get(i+2));
                            Statement s = new NormalStatement(node, index);
                            tempCollections = Slicer.computeForwardSlice(s, cg, builder.getPointerAnalysis(), Slicer.DataDependenceOptions.NO_BASE_NO_HEAP, Slicer.ControlDependenceOptions.NO_EXCEPTIONAL_EDGES);
                            flag = true;
                            break;
                        }
                    }
                }
            }
        }
        return tempCollections;

//            for (Statement s : tempCollections) {
//                if (s.getKind().toString().equals("NORMAL"))
////                        && !s.toString().contains("Ljava/"))
////                && (s.toString().contains("binaryop") || s.toString().contains("return") || s.toString().contains("getstatic") || s.toString().contains("conditional branch")))
//                    Collections.add(s);
//            }
//            return Collections;
    }
    static public Collection<Statement> makeSlice(String className,String methodName,String seed,  CallGraph cg) throws CancelException{
        Collection<Statement> tempCollections =new LinkedList<Statement>();
        Collection<Statement> Collections =new LinkedList<Statement>();

        boolean flag = false;
        for (CGNode node:cg){
            if (flag)
                break;
            if (className.equals(node.getMethod().getDeclaringClass().getName().toString())){
//                System.out.println("method");
//                System.out.println(node.getMethod().getName().toString());
                if (methodName.equals(node.getMethod().getName().toString())){
                    List<SSAInstruction> ls = getSSA(node);
                    for (int i=0;i<ls.size();i++){
                        if (ls.get(i) !=null){
                            System.out.println(ls.get(i).toString());
                            if (ls.get(i).toString().contains(seed)){
                                int index = getInstructionIndex(node, ls.get(i));
                                Statement s = new NormalStatement(node, index);
                                System.out.println("---------------seed---------------------------");
                                System.out.println(s.toString());
                                tempCollections = Slicer.computeForwardSlice(s,cg , builder.getPointerAnalysis(), Slicer.DataDependenceOptions.NO_BASE_NO_HEAP, Slicer.ControlDependenceOptions.NO_EXCEPTIONAL_EDGES);

                                flag = true;
                                break;
                            }
                        }
                    }
                }


            }
        }

        for (Statement s:tempCollections){
            if (s.getKind().toString().equals("NORMAL")
                    && !s.toString().contains("Ljava/lang/System")
                    && !s.toString().contains("Ljava/io/PrintStream"))
                Collections.add(s);
        }

        return Collections;
    }
    static public void outputSlice(Collection<Statement> slices){
        int num=0;
        for (Statement s:slices){
                System.out.println(num +" :" +  s);
                num++;
        }
    }
    static public void  outputEntryPoint(Iterable<Entrypoint> itEntryPoint) {
        for (Iterator<Entrypoint> it = itEntryPoint.iterator(); it.hasNext(); ) {
            Entrypoint entrypoint = it.next();
            System.out.println(entrypoint);
        }
    }
    static public void outputCG( CallGraph cg){
        for (CGNode node:cg) {
            System.out.println(node.toString());
        }

    }
}
