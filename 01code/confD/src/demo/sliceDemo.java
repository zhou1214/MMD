package demo;


import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.callgraph.impl.AllApplicationEntrypoints;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.modref.ModRef;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.*;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.strings.Atom;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class sliceDemo {
//    class path to analyze, delimited by File.pathSeparator
    static String jarPath ="./lib/test2.jar";
    static String exclusionsFilePath = "JavaAllExclusions.txt";
    static File exclusionsFile = new File(exclusionsFilePath);
//    static String methodName = "main";
//    static String MethodClass = "Lmain/main";
    static String seed = "getA";
    static List<IMethod> methodList = null;
    static List<Statement> statements = null;

    public static void main(String[] args) throws IOException, ClassHierarchyException, CancelException, InvalidClassFileException {

//      build scope
        AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(jarPath,exclusionsFile);
//        Hierarchy
        ClassHierarchy cha = ClassHierarchyFactory.makeWithRoot(scope);

//        AllApplicationEntrypoints entrypoints = new AllApplicationEntrypoints(scope,cha);
        Iterable<Entrypoint> Mainentrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha);
//      options
        AnalysisOptions options =new AnalysisOptions(scope,Mainentrypoints);

        AnalysisCache cache = new AnalysisCacheImpl();
        CallGraphBuilder builder = Util.makeVanillaZeroOneCFABuilder(Language.JAVA,options,cache,cha,scope);

        CallGraph cg = builder.makeCallGraph(options,null);

        Collection<Statement> collections =null;


        methodList = new LinkedList<IMethod>();
        statements =new LinkedList<Statement>();
        Statement temp = null;
        for (CGNode node:cg){
            if (node.getMethod().getName().toString().equals(seed)){
//                System.out.println(node);
                List<SSAInstruction> ls = getSSA(node);
                int size = ls.size();
                for (int i=0;i<size;i++){
//                    System.out.println(ls.get(i));
                    if (ls.get(i) != null){
                        int index = getInstructionIndex(node, ls.get(i));
                        Statement s = new NormalStatement(node, index);
                        statements.add(s);
                        temp =s;
                    }
                }
            }
        }

        collections = Slicer.computeForwardSlice(temp ,cg , builder.getPointerAnalysis(), Slicer.DataDependenceOptions.NO_BASE_NO_HEAP, Slicer.ControlDependenceOptions.NO_EXCEPTIONAL_EDGES);
        outputSlice(collections);


    }
    public static AnalysisScope addScope(String classPath, File exclusionsFile) throws IOException {
        if (classPath == null) {
            throw new IllegalArgumentException("classPath null");
        }
        AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(jarPath,exclusionsFile);
        ClassLoaderReference loader = scope.getLoader(AnalysisScope.APPLICATION);
        AnalysisScopeReader.addClassPathToScope(classPath, scope, loader);
        return scope;
    }
//    method name seed 函数名
//    找到函数名在那个node中
//    name 方法名 add
//    methodClass 类名 Lutil/function
    static public CGNode findMethod(CallGraph cg,String Name,String methodClass){
        if (Name.equals(null) && methodClass.equals(null))
            return null;
        Atom name = Atom.findOrCreateUnicodeAtom(Name);

        for (Iterator<CGNode> it = cg.iterator(); it.hasNext();){
            CGNode n =it.next();

            if (n.getMethod().getName().equals(name)  && n.getMethod().getDeclaringClass().getName().toString().equals(methodClass)){
                return n;
            }
        }
        Assertions.UNREACHABLE("faild to find method" + name);
        return null;
    }
//    node 中找到 seed语句
    static public Statement findCallTo(CGNode n,String methodName){
        IR ir = n.getIR();

        for (Iterator<SSAInstruction> it = ir.iterateAllInstructions(); it.hasNext();){
            SSAInstruction s = it.next();
            if (s instanceof SSAInvokeInstruction){
                SSAInvokeInstruction call =(SSAInvokeInstruction) s;
                if (call.getCallSite().getDeclaredTarget().getName().toString().equals(methodName)){
                    IntSet indics = ir.getCallInstructionIndices(call.getCallSite());
                    Assertions.productionAssertion(indics.size() == 1,"expected 1 but got:" + indics.size());
                    return new NormalStatement(n,indics.intIterator().next());
                }
            }
        }
        Assertions.UNREACHABLE("faild to find call to :" + methodName +"in" + n);
        return null;
    }
//    sout
    static public void outputSlice(Collection<Statement> slices){
        for (Statement s:slices){
            System.out.println(s);
        }
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

}
