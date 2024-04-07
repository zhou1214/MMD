package Utils;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.nCFABuilder;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.util.graph.Graph;

import java.util.*;

public class WALAUtils {
    public static int getStatementLineNumber(Statement s){
        int lineNum = -1;
        if (s.getKind() == Statement.Kind.NORMAL){
            int bcIndex,instructionIndex = ((NormalStatement)s).getInstructionIndex();
            try {
                bcIndex = ((ShrikeBTMethod) s.getNode().getMethod()).getBytecodeIndex(instructionIndex);
                try {
                    int src_line_number = s.getNode().getMethod().getLineNumber(bcIndex);
                    lineNum = src_line_number;
                } catch (Exception e){
                    System.err.println("Exception in Bytecode index " +bcIndex) ;
                    System.err.println(e.getMessage());
                }
            }catch (Exception e){
                System.err.println("it's probably not a BT method (e.g. it's a fakeroot method)");
                System.err.println(e.getMessage());
            }

        }
        return lineNum;
    }
    //return a.b.c.d.MethodName
    public static String getFullMethodName(IMethod method){
        String className = getJavaFullClassName(method.getDeclaringClass());
        return className + "." + method.getName().toString();
    }

    public static String getJavaFullClassName(IClass declaringClass) {
        TypeName tn = declaringClass.getName();
        String packageNanme = (tn.getPackage() == null ?"":tn.getPackage().toString()+".");
        String className = tn.getClassName().toString();
        return Utils.translateSlashToDot(packageNanme) + className;
    }

    /*public static SSAPropagationCallGraphBuilder makeOneCFABuilder(AnalysisOptions options, AnalysisCache cache, IClassHierarchy cha,
                                                                   AnalysisScope scope){
        return makeCFABuilder(1,options,cache,cha,scope);
    }
    public static SSAPropagationCallGraphBuilder makeCFABuilder(int n,
                                                                AnalysisOptions options, AnalysisCache cache, IClassHierarchy cha,
                                                                AnalysisScope scope){
        if (options == null){
            throw new IllegalArgumentException("option is null");
        }
        Util.addDefaultSelectors(options,cha);
        Util.addDefaultBypassLogic(options,scope,Util.class.getClassLoader(),cha);

        return new nCFABuilder(n, cha, options, cache, null, null);

    }*/
    public static List<SSAInstruction> getAllIRs(CGNode node){
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
    public static IClass lookupClass(ClassHierarchy cha ,String classFullName){
        for (IClass c:cha){
            String fullName = WALAUtils.getJavaFullClassName(c);
            if (fullName.equals(classFullName)){
                return c;
            }
        }
        return null;
    }
    public static Collection<CGNode> lookupCGNode(Graph<CGNode> cg, String fullName) {
        Collection<CGNode> nodes = new LinkedHashSet<CGNode>();

        for(CGNode node : cg) {
            String fullMethodName = WALAUtils.getFullMethodName(node.getMethod());
            if(fullName.equals(fullMethodName)) {
                nodes.add(node);
            }
        }

        return nodes;
    }
    public static void printAllIRs(CGNode node){
        for (SSAInstruction ssa: WALAUtils.getAllNotNullIRs(node)){
            System.out.println(ssa + ", type: " + ssa.getClass());
        }
    }
    public static List<SSAInstruction> getAllNotNullIRs(CGNode node){
        List<SSAInstruction> list =new LinkedList<SSAInstruction>();
        SSAInstruction[] instructions =node.getIR().getInstructions();
        for (SSAInstruction ins:instructions){
            if (ins ==null){
                continue;
            }
            list.add(ins);
        }
        return list;
    }
}
