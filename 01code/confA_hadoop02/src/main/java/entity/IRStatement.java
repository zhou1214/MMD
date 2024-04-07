package entity;

import Utils.Sep;
import Utils.WALAUtils;
import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.StatementWithInstructionIndex;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAInstruction;

import java.util.LinkedList;
import java.util.List;
import Utils.readHDFS;

public class IRStatement {
    public static String[] ignorePackages = new String[]{
            "java.", "javax.",
    };
    //一种带有指令索引的语句，表示一个ssainstruct在IR指令数组中的索引
    public final StatementWithInstructionIndex s;
    //转换为SSA形式的指令
    public final SSAInstruction ssa;
    ///like com.foo.bar.createLargeOrder(IILjava.lang.String;SLjava.sql.Date;)Ljava.lang.Integer;
    public final String methodSig;
    ///指令index
    public final int instructionIndex;
    //二进制index
    public final int bcIndex;
    ////行号
    public final int lineNumber;

    public int sameCount = 0;
    ////////////一个statement 可能会有很多个conf和他重复需要构造一个list
    public List<Count> eachCount;
    //    统计每个conf 有几个相同
    public  List<AnalysisCount> AnalysisCount;

    private int getByteCodeIndex(Statement stmt, int index){
        try {
            return ((ShrikeBTMethod)stmt.getNode().getMethod()).getBytecodeIndex(index);
        } catch (Throwable e) {
            System.err.println("Error in getByteCodeIndex:" + stmt);

            throw new Error(e);
        }
    };
    public IRStatement(StatementWithInstructionIndex stmt){
        this.s = stmt;
        this.ssa = stmt.getInstruction();
        this.methodSig = this.s.getNode().getMethod().getSignature();
        this.instructionIndex = stmt.getInstructionIndex();
        this.bcIndex = this.getByteCodeIndex(stmt,this.instructionIndex);
        this.lineNumber = WALAUtils.getStatementLineNumber(this.s);
        ////初始化一个list
        this.eachCount = new LinkedList<Count>();
        this.AnalysisCount = new LinkedList<AnalysisCount>();


        ///check stmt not null
        readHDFS.checkNotNull(this.s);
        readHDFS.checkNotNull(this.ssa);

        readHDFS.checkTrue(this.instructionIndex >-1);
        readHDFS.checkTrue(this.bcIndex >-1);
    }
    public Statement getStatement(){
        return this.s;
    }
    public boolean shouldIgnore(){
        String fullMethodName = WALAUtils.getFullMethodName(this.s.getNode().getMethod());
        for (String packagePrefix : ignorePackages){
            if (fullMethodName.startsWith(packagePrefix)){
                return true;
            }
        }
        return false;
    }
    public String getUniqueSignature(){
        return this.getMethodSig() + "#" + this.ssa.toString() + "#" + this.getInstructionIndex();
    }
    public String getMethodSig() {
        return this.methodSig;
    }
    public int getInstructionIndex() {
        return this.instructionIndex;
    }
    public boolean isBranch() {
        return this.isSSABranch(this.ssa);
    }
    private boolean isSSABranch(SSAInstruction ssa) {
        return ssa instanceof SSAConditionalBranchInstruction;
    }
    public int getLineNumber(){
        return this.lineNumber;
    }

    @Override
    public int hashCode() {
        return this.s.hashCode() + 13*this.ssa.hashCode()
                + 17*this.instructionIndex + 29*this.bcIndex
                + 31*this.lineNumber;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(s.toString());
        sb.append(Sep.lineSep);
        sb.append("    instructionIndex: ");
        sb.append(this.instructionIndex);
        sb.append("    bcIndex: ");
        sb.append(this.bcIndex);
        sb.append("    lineNumber: ");
        sb.append(this.lineNumber);
        sb.append("    isBranch: ");
        sb.append(this.isBranch());

        return sb.toString();
    }
    public String IRHeadString(){
        StringBuilder sb = new StringBuilder();
        sb.append(s.getNode());

        return sb.toString();
    }

    public void setEachCount(ConfEntity conf){

        Count tempEachCount = new Count(conf);
        this.eachCount.add(tempEachCount);
    }



    public void output(){
        System.out.println("statement:" +s.toString());
        System.out.println("linenumber:"+this.lineNumber);
        System.out.println("sanme count：" + this.sameCount);
        System.out.println("each count：");

        int size = this.eachCount.size();
        for (int i=0;i<size;i++){
            System.out.println(this.eachCount.get(i));
        }
    }

    public void Analysis(List<ConfEntity> confList){
//        List<ConfEntity> confList = Utils.getConfList();

        int ConfSize = confList.size();
        int CountSize = this.eachCount.size();

        for (int i=0;i<ConfSize;i++){
            AnalysisCount temp = new AnalysisCount(confList.get(i));
            this.AnalysisCount.add(temp);
            for (int j=0;j<CountSize;j++){

                if (confList.get(i).equals( this.eachCount.get(j).getConfEntity() )){
                    temp.setCount();
                }

            }
        }

    }
}
