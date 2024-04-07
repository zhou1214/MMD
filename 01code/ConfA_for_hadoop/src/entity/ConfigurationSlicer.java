package entity;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.AllApplicationEntrypoints;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.pruned.ApplicationLoaderPolicy;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.*;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ipa.slicer.thin.CISlicer;
import com.ibm.wala.ssa.*;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.io.FileProvider;
import Utils.*;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;

import java.io.IOException;
import java.util.*;



public class ConfigurationSlicer {
    public enum CG {
        OneCFA,ZeroCFA,RTA,
    }

    public final String classPath;
    public final String mainClass;
//    public final String Prefix;
//    public final String key;

    private CG type = CG.OneCFA;
    private String exclusionFile = CallGraphTestUtil.REGRESSION_EXCLUSIONS;
    private DataDependenceOptions dataOption = DataDependenceOptions.NO_BASE_NO_HEAP_NO_EXCEPTIONS;
    private ControlDependenceOptions controlOption = ControlDependenceOptions.NONE;
    private boolean contextSensitive = false;
    private AnalysisScope scope = null;
    private ClassHierarchy cha = null;
    private Iterable<Entrypoint> entryPoints = null;
    private AnalysisOptions options = null;
    private CallGraphBuilder builder = null;
    private CallGraph cg = null;
    private String byPassFile = null;
    private boolean backward = false;
    private CISlicer slicer = null;
    private boolean extractAllGets = false;
    private boolean addSliceSeedFromGet = false;
    private boolean addStatementDistance = false;
    private String targetPackageName = null;
    private boolean useReturnSeed = false;
    private static AnalysisCache cache = null;

    public ConfigurationSlicer(String classPath,String mainClass){
        this.classPath = classPath;
        this.mainClass = mainClass;
    }
    public void setCGType(CG type){
        this.type = type;
    }
    public void setExclusiveFile(String fileName){
        this.exclusionFile = fileName;
    }
    public void setDataDependenceOptions(DataDependenceOptions op){
        this.dataOption =op;
    }
    public void setControlDependenceOptions(ControlDependenceOptions op){
        this.controlOption = op;
    }
    public void setContextSensitive(boolean b){
        this.contextSensitive = b;
    }

    public void buildAnalysis(){
        try {
            cache = new AnalysisCacheImpl();

            System.out.println("Using exclusion file: " + this.exclusionFile);
            System.out.println("CG type: " + this.type);
            this.buildScope();
            this.buildClassHierarchy();

            if (this.entryPoints == null){
                this.entryPoints = new AllApplicationEntrypoints(this.scope, this.cha);
            }
            this.options = new AnalysisOptions(this.scope,this.entryPoints);
            this.builder = Util.makeVanillaZeroOneCFABuilder(Language.JAVA,options,cache,cha,scope);

            System.out.println("Building call graph...");

            System.err.println("Number of entry points: "+Utils.countIterable(this.entryPoints));

            this.cg = this.builder.makeCallGraph(this.options,null);
            System.err.println("Number of node: " + this.cg.getNumberOfNodes());

        }catch (Throwable e){
            System.err.println("error in  buildAnalysis");
            throw new Error(e);
        }
    }
    public void buildScope(){
        try {
            this.scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(this.classPath,(new FileProvider()).getFile(this.exclusionFile));
        }catch (IOException e){
            System.err.println("error in buildScope");
            throw new Error(e);
        }
    }
    public void buildClassHierarchy(){
        if (this.scope == null){
            this.buildScope();
        }
        try {
            this.cha = ClassHierarchyFactory.makeWithRoot(this.scope);
        }catch (ClassHierarchyException e){
            System.err.println("error in buildClassHierarchy");
            throw new Error(e);
        }
    }
    public ConfPropOutput outputSliceConfOption(ConfEntity entity) throws CancelException {
        long startT = System.currentTimeMillis();
        Collection<Statement> stmts = sliceConfOption(entity);

        if (stmts !=null){     ///application 剪枝
            ApplicationLoaderPolicy applicationLoaderPolicy = ApplicationLoaderPolicy.INSTANCE;

            Iterator<Statement> pruneIterator = stmts.iterator();

            Statement sliceStatement = null;

            while (pruneIterator.hasNext()) {
                sliceStatement = pruneIterator.next();
                CGNode node = sliceStatement.getNode();
                if (!applicationLoaderPolicy.check(node)) {
                    pruneIterator.remove();
                }
            }
        }


        System.out.println("Time cost: " + (System.currentTimeMillis() - startT)/1000 + " s");
        Collection<IRStatement> irs = convert(stmts);
        ConfPropOutput output = new ConfPropOutput(entity, irs, this.targetPackageName);

        return output;
    }

    static Collection<IRStatement> convert(Collection<Statement> stmts){
        Collection<IRStatement> irs = new LinkedList<IRStatement>();

        for (Statement s:stmts){
            if (s instanceof StatementWithInstructionIndex){
                if (s.getNode().getMethod() instanceof ShrikeBTMethod){
                    try {
                        IRStatement ir = new IRStatement((StatementWithInstructionIndex)s);
                        irs.add(ir);
                    }catch (Throwable e){
                        continue;
                    }
                }

            }
        }
        return irs;
    }
    public Collection<Statement> sliceConfOptionFromGetter(ConfEntity entity) throws CancelException {
        this.CheckCG();
        Statement s = this.extractConfStatementFromGetter(entity);
        System.out.println("---------------statement-------------------");
        System.out.println(s);
        if(s == null) {
            return Collections.EMPTY_LIST;
        }
        System.out.println("Add additional seed: " + s + " to: " + entity.getConfName());
        return this.performSlicing(s);
    }
    public Statement extractConfStatementFromGetter(ConfEntity entity) throws CancelException {
        String confClassName = entity.getClassName();
        String confName = entity.getConfName();
        boolean isStatic = entity.getIsStatic();
        Set<String> skippedMethods = new HashSet<String>();
        skippedMethods.add("equals");
        skippedMethods.add("hashCode");
        skippedMethods.add("toString");
        skippedMethods.add("<init>");
        skippedMethods.add("<clinit>");

        boolean flag = false;
        for (CGNode node:cg){
            if (flag)
                break;
            if (confClassName.equals(node.getMethod().getDeclaringClass().getName().toString())){
                List<SSAInstruction> ls = getSSA(node);
                for (int i=0;i<ls.size();i++){
                    if (ls.get(i) !=null){
                        if (ls.get(i) !=null && ls.get(i).toString().contains(confName) && ls.get(i).toString().contains("getfield")){
                            int index = getInstructionIndex(node, ls.get(i));
                            Statement s = new NormalStatement(node, index);
                            System.out.println("--------------------------statement---------------------------");
                            System.out.println(s.toString());
                            flag = true;
                            return s;
                        }
                    }
                }
            }
        }
        return null;
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
    public Collection<Statement> sliceConfOption(ConfEntity entity) throws CancelException {
        CheckCG();
        Statement s = this.extractConfStatement(entity);

        if (s ==null){
            IClass clz = WALAUtils.lookupClass(this.getClassHierarchy(),entity.getClassName() );

            if (clz !=null){
                String signature = entity.getClassName() + "." +
                        (entity.getAssignMethod() == null
                                ?(entity.getIsStatic() ? "<clinit>" : "<init>")
                                : entity.getAssignMethod());

                Collection<CGNode> nodes = WALAUtils.lookupCGNode(this.getCallGraph(), signature);

                for (CGNode node:nodes){
                    WALAUtils.printAllIRs(node);
                }
                if (nodes.isEmpty()){
                    System.err.println(" no such nodes in CG: " + signature);
                    return new LinkedList<Statement>();
                }
            }
            Utils.checkTrue(clz == null, "Class: " + entity.getClassName()
                    + ",  here is the entity: " + entity);
        }

        Utils.checkNotNull(s, "statement is null? " + entity);
        Collection<Statement> slice = this.performSlicing(s);

        if (this.extractAllGets){
            Collection<Statement> stmtsFromGetters = this.sliceConfOptionFromEveryGetter(entity);
            slice.addAll(stmtsFromGetters);
        }

        return slice;

    }
    public Collection<Statement> extractAllGetStatements(ConfEntity entity) {
        return ConfUtils.getextractAllGetStatements(entity, this.getCallGraph());
    }
    Collection<Statement> sliceConfOptionFromEveryGetter(ConfEntity entity) throws CancelException {
        this.CheckCG();
        Collection<Statement> allStmts = new LinkedHashSet<Statement>();

        Collection<Statement> allGetStmts = this.extractAllGetStatements(entity);
        System.err.println("   all get stmts: " + allGetStmts.size());
        for(Statement getStmt : allGetStmts) {
            Collection<Statement> slicedStmts = this.performSlicing(getStmt);
            allStmts.addAll(slicedStmts);
        }

        return allStmts;
    }
    public Collection<Statement> performSlicing(Statement s) throws CancelException {
        this.CheckCG();
        Utils.checkNotNull(s);

        Collection<Statement> tempCollections =new LinkedList<Statement>();
        Collection<Statement> Collections =new LinkedList<Statement>();

        tempCollections = Slicer.computeForwardSlice(s,cg , builder.getPointerAnalysis(), Slicer.DataDependenceOptions.NO_BASE_NO_HEAP, Slicer.ControlDependenceOptions.NO_EXCEPTIONAL_EDGES);

        for (Statement ss:tempCollections){
            if (ss.getKind().toString().equals("NORMAL")
                    && !ss.toString().contains("Ljava/lang/System")
                    && !ss.toString().contains("Ljava/io/PrintStream"))
                Collections.add(s);
        }

        if (Collections !=null) {
            ApplicationLoaderPolicy applicationLoaderPolicy = ApplicationLoaderPolicy.INSTANCE;

            Iterator<Statement> pruneIterator = Collections.iterator();

            Statement sliceStatement = null;

            while (pruneIterator.hasNext()) {
                sliceStatement = pruneIterator.next();
                CGNode node = sliceStatement.getNode();
                if (!applicationLoaderPolicy.check(node)) {
                    pruneIterator.remove();
                }
            }
        }

        if (Collections !=null)
            return Collections;
        else
            return null;

//        return Collections;

    }
    /*public Collection<Statement> computeContextSensitiveForwardSlice(Statement seed) throws IllegalArgumentException, CancelException{
        return computeContextSensitiveSlice(seed, false, this.dataOption, this.controlOption);
    }
    public Collection<Statement> computeContextInsensitiveForwardThinSlice(Statement seed) throws IllegalArgumentException, CancelException {
        return computeContextInsensitiveThinSlice(seed, false, this.dataOption, this.controlOption);
    }*/
   /* public Collection<Statement> computeContextInsensitiveThinSlice(Statement seed, boolean goBackward,
                                                                    DataDependenceOptions dOptions, ControlDependenceOptions cOptions) throws IllegalArgumentException, CancelException{
        this.CheckCG();
        System.err.println("Seed statement in context-insensitive slicing: " + seed);
        System.err.println("Data dependence option: " + dOptions);
        System.err.println("Control dependence option: " + cOptions);

        if (slicer == null){
            slicer = new CISlicer(cg, builder.getPointerAnalysis(), dOptions, cOptions);
        }

        Collection<Statement> slice = null;

        if (goBackward){
            slice = slicer.computeBackwardThinSlice(seed);
        }
        else{
            seed = getReturnStatementForCall(seed);
            slice =slicer.computeForwardThinSlice(seed);
        }
        return slice;
    }*/
    /*public Collection<Statement> computeContextSensitiveSlice(Statement seed, boolean goBackward,
                                                              DataDependenceOptions dOptions, ControlDependenceOptions cOptions) throws IllegalArgumentException, CancelException {
        this.CheckCG();
        System.err.println("Seed statement in context-sensitive slicing: " + seed);
        System.err.println("Data dependence option: " + dOptions);
        System.err.println("Control dependence option: " + cOptions);

        Collection<Statement> slice = null;
        if (goBackward){
            slice = Slicer.computeBackwardSlice(seed,cg,builder.getPointerAnalysis(),dOptions,cOptions);
        }
        else{
            seed = getReturnStatementForCall(seed);
            slice = Slicer.computeForwardSlice(seed, cg, builder.getPointerAnalysis(), dOptions, cOptions);
        }
        return slice;
    }*/
    public static Statement getReturnStatementForCall(Statement s){
        if (s.getKind() == Statement.Kind.NORMAL){
            NormalStatement n=(NormalStatement) s;
            SSAInstruction st = n.getInstruction();
            if (st instanceof SSAInvokeInstruction){
                SSAAbstractInvokeInstruction call = (SSAAbstractInvokeInstruction) st;
                if (call.getCallSite().getDeclaredTarget().getReturnType().equals(TypeReference.Void)){
                    throw new IllegalArgumentException("this driver computes forward slices from the return value of calls.\n" + ""
                            + "Method " + call.getCallSite().getDeclaredTarget().getSignature() + " returns void.");
                }
                System.err.println("Use return value as slicing seed: " + s);
                return new NormalReturnCaller(s.getNode(), n.getInstructionIndex());

            }
            else {
                return s;
            }
        }
        else {
            return s;
        }
    }
    public ClassHierarchy getClassHierarchy() {
        return this.cha;
    }
    public CallGraph getCallGraph() {
        return this.cg;
    }
    private void CheckCG(){
        if(this.cg == null) {
            throw new RuntimeException("Please call buildAnalysis() first.");
        }
    }
    public Statement extractConfStatement(ConfEntity entity) {
        String className = entity.getClassName();
        String confName = entity.getConfName();
        String assignMethod = entity.getAssignMethod(); //FIXME we may need more specific method signature   ////null


        boolean isStatic = entity.getIsStatic();
        String targetMethod = assignMethod != null
                ? assignMethod
                : (isStatic ? "<clinit>" : "<init>");
        Log.logln("target method name: " + targetMethod);

        Collection<Statement> tempCollections =new LinkedList<Statement>();
        Collection<Statement> Collections =new LinkedList<Statement>();

        boolean classFlag = false;
        boolean seedFlag = false;
        boolean flag = false;

        for (CGNode node:cg){
            if (flag)
                break;
            if (className.equals(node.getMethod().getDeclaringClass().getName().toString())){
                List<SSAInstruction> ls = getSSA(node);
                for (int i=0;i<ls.size();i++){
                    if (ls.get(i) !=null){
                        if (ls.get(i) !=null && ls.get(i).toString().contains(confName) && ls.get(i).toString().contains("getfield")){
                            int index = getInstructionIndex(node, ls.get(i));
                            Statement s = new NormalStatement(node, index);
                            System.out.println("--------------------------statement---------------------------");
                            System.out.println(s.toString());
                            //tempCollections = Slicer.computeForwardSlice(s,cg , builder.getPointerAnalysis(), Slicer.DataDependenceOptions.NO_BASE_NO_HEAP, Slicer.ControlDependenceOptions.NO_EXCEPTIONAL_EDGES);
                            flag = true;
                            return s;
                        }
                    }
                }
            }
        }
        if (classFlag == false){
            System.err.println("there is no such class named:" + className);
            return null;
        }
        else if (seedFlag == false){
            System.err.println("there is no such seed named:" + confName);
            return null;
        }

        return null;
    }
}
