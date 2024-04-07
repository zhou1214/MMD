package Utils;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.TypeName;

import java.util.LinkedList;
import java.util.List;

public class WALAUtils {
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
    public static String getFullMethodName(IMethod method){
        String className = getJavaFullClassName(method.getDeclaringClass());
        return className + "." + method.getName().toString();
    }

    public static String getJavaFullClassName(IClass declaringClass) {
        TypeName tn = declaringClass.getName();
        String packageNanme = (tn.getPackage() == null ?"":tn.getPackage().toString()+".");
        String className = tn.getClassName().toString();
        return readHDFS.translateSlashToDot(packageNanme) + className;
    }
}
