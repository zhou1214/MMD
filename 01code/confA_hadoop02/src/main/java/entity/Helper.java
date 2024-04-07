package entity;

import Utils.WALAUtils;
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
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.StatementWithInstructionIndex;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.io.FileProvider;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import Utils.readHDFS;

public class Helper {
    private enum CG {OneCFA,ZeroCFA,RTA};
    private String classPath;
    private CG type = CG.ZeroCFA;
    private String exclusionFile = CallGraphTestUtil.REGRESSION_EXCLUSIONS;
    private AnalysisScope scope = null;
    private ClassHierarchy cha = null;
    private Iterable<Entrypoint> entryPoints = null;
    private AnalysisOptions options = null;
    private CallGraphBuilder builder = null;
    private CallGraph cg = null;
    private Slicer.DataDependenceOptions dataOption = null;
    private Slicer.ControlDependenceOptions controlOption = null;
    private static AnalysisCache cache = null;
    private String targetPackageName = null;

    public String getClassPath() {
        return classPath;
    }

    public void setClassPath(String classPath) {
        this.classPath = classPath;
    }

    public CG getType() {
        return type;
    }

    public void setType(CG type) {
        this.type = type;
    }

    public String getExclusionFile() {
        return exclusionFile;
    }

    public void setExclusionFile(String exclusionFile) {
        this.exclusionFile = exclusionFile;
    }

    public AnalysisScope getScope() {
        return scope;
    }

    public void setScope(AnalysisScope scope) {
        this.scope = scope;
    }

    public ClassHierarchy getCha() {
        return cha;
    }

    public void setCha(ClassHierarchy cha) {
        this.cha = cha;
    }

    public Iterable<Entrypoint> getEntryPoints() {
        return entryPoints;
    }

    public void setEntryPoints(Iterable<Entrypoint> entryPoints) {
        this.entryPoints = entryPoints;
    }

    public AnalysisOptions getOptions() {
        return options;
    }

    public void setOptions(AnalysisOptions options) {
        this.options = options;
    }

    public CallGraphBuilder getBuilder() {
        return builder;
    }

    public void setBuilder(CallGraphBuilder builder) {
        this.builder = builder;
    }

    public CallGraph getCg() {
        return cg;
    }

    public void setCg(CallGraph cg) {
        this.cg = cg;
    }

    public Slicer.DataDependenceOptions getDataOption() {
        return dataOption;
    }

    public void setDataOption(Slicer.DataDependenceOptions dataOption) {
        this.dataOption = dataOption;
    }

    public Slicer.ControlDependenceOptions getControlOption() {
        return controlOption;
    }

    public void setControlOption(Slicer.ControlDependenceOptions controlOption) {
        this.controlOption = controlOption;
    }

    public Helper(String classPath, String exclusionFile, Slicer.DataDependenceOptions dataOption, Slicer.ControlDependenceOptions controlOption) {
        this.classPath = classPath;
        this.exclusionFile = exclusionFile;
        this.dataOption = dataOption;
        this.controlOption = controlOption;
    }
    static public CallGraphBuilder buildCallGraph(AnalysisOptions options,AnalysisCache cache,AnalysisScope scope,ClassHierarchy cha){
        return Util.makeVanillaZeroOneCFABuilder(Language.JAVA,options,cache,cha,scope);
    }
    public void Build() throws IOException, ClassHierarchyException {
        long startTime = System.currentTimeMillis();
        System.err.println("......................wala......................");
        this.cache = new AnalysisCacheImpl();
//        构建结构层次
        this.scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(this.classPath,(new FileProvider()).getFile(this.exclusionFile));
        this.cha = ClassHierarchyFactory.makeWithRoot(this.scope);
//        entry points
        if (this.entryPoints ==null){
            this.entryPoints = new AllApplicationEntrypoints(this.scope, this.cha);
        }
        System.err.println("entry points list : " + this.entryPoints.toString());
//
        this.options = new AnalysisOptions(this.scope,this.entryPoints);
        this.builder = buildCallGraph(this.options,this.cache,this.scope,this.cha);
        System.out.println("Building call graph...");
        try {
            this.cg =builder.makeCallGraph(this.options,null);
            System.err.println(CallGraphStats.getStats(this.cg));
            long endTime = System.currentTimeMillis();
            System.out.println("调用图构建时间：" + (endTime - startTime)/1000 + "s");
        } catch (CallGraphBuilderCancelException e) {
            e.printStackTrace();
            System.err.println("graph is error");
        }
    }

    public ConfPropOutput outputSliceConfOption(ConfEntity entity) throws CancelException {
        long startT = System.currentTimeMillis();
//        获得切片 seed 所在的statement
        Collection<Statement> stmts = sliceConfOption(entity);

        if (stmts !=null){
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
        System.out.println("the slice size of " + entity + " is :" + output.statements.size());
        return output;
    }

    public Collection<Statement> sliceConfOption(ConfEntity entity) throws CancelException {    ///zhou
        CheckCG();
        Statement s = this.extractConfStatement(entity);

        readHDFS.checkNotNull(s, "statement is null? " + entity);
        Collection<Statement> slice = this.performSlicing(s);

        return slice;

    }
    private void CheckCG(){
        if(this.cg == null) {
            throw new RuntimeException("Please call buildAnalysis() first.");
        }
    }

    public Statement extractConfStatement(ConfEntity entity) {   /////zhou
        String className = entity.getClassName();
        String confName = entity.getConfName();

        boolean flag =false;

        for (CGNode node:cg){
            if (flag)
                break;
            if (className.equals(node.getMethod().getDeclaringClass().getName().toString())){
                List<SSAInstruction> ls = WALAUtils.getSSA(node);
                int size = ls.size();
                for (int i = 0; i < size; i++) {
                    if (ls.get(i) !=null && ls.get(i).toString().contains(confName) && ls.get(i).toString().contains("getfield")){
                        int index = WALAUtils.getInstructionIndex(node, ls.get(i));
                        Statement s = new NormalStatement(node, index);
                        System.out.println("--------------------------seed statement---------------------------");
                        System.out.println(s.toString());
                        flag = true;
                        return s;
                    }
                }
            }
        }
        return null;
    }
    public Collection<Statement> performSlicing(Statement s) throws CancelException {    ////zhou
        this.CheckCG();
        Collection<Statement> tempCollections =new LinkedList<Statement>();
        Collection<Statement> Collections =new LinkedList<Statement>();
        tempCollections = Slicer.computeForwardSlice(s,cg , builder.getPointerAnalysis(), Slicer.DataDependenceOptions.NO_BASE_NO_HEAP, Slicer.ControlDependenceOptions.NO_EXCEPTIONAL_EDGES);

        for (Statement statements:tempCollections){
            if (statements.getKind().toString().equals("NORMAL")
                    && !statements.toString().contains("Ljava/")
            )
//                && statements.toString().contains("conditional branch")
//                    && statements.toString().contains("binaryop")
                if (statements.toString().contains("conditional branch") || statements.toString().contains("binaryop"))
                    Collections.add(statements);
        }
        return Collections;
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
}
