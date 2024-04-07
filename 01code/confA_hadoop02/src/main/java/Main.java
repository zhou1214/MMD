import Utils.readHDFS;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.util.CancelException;
import entity.ConfEntity;
import entity.ConfPropOutput;

import java.io.IOException;
import java.util.*;

import entity.Helper;
import entity.IRStatement;

public class Main {
//    private String jarPath = "./lib/2.9.2/hadoop-common-2.9.2.jar;./lib/2.9.2/hadoop-hdfs-2.9.2.jar";
//    private String jarPath = "./lib/2.3.2/hbase-client-2.3.2.jar;./lib/2.3.2/hbase-common-2.3.2.jar";   ./lib/2.10.2/hadoop-common-2.10.2.jar;
//    private String jarPath = "./lib/2.10.2/hadoop-hdfs-2.10.2.jar";
    private String jarPath = "./lib/2.10.2/hadoop-common-2.10.2.jar";
    private String scope = "common";
    private List<ConfEntity> confEntityList = null ;
    private Collection<ConfPropOutput> slices;
    private String exclusive = "JavaAllExclusions.txt";

    public String getJarPath() {
        return jarPath;
    }

    public void setJarPath(String jarPath) {
        this.jarPath = jarPath;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public List<ConfEntity> getConfEntityList() {
        return confEntityList;
    }

    public void setConfEntityList(List<ConfEntity> confEntityList) {
        this.confEntityList = confEntityList;
    }

    public Collection<ConfPropOutput> getSlices() {
        return slices;
    }

    public void setSlices(Collection<ConfPropOutput> slices) {
        this.slices = slices;
    }

    public String getExclusive() {
        return exclusive;
    }

    public void setExclusive(String exclusive) {
        this.exclusive = exclusive;
    }

    public static void main(String[] args) throws ClassHierarchyException, IOException, CancelException {
        Main s = new Main();
        s.setConfEntityList(s.getscope());
        s.setSlices(s.slice(s.getJarPath(),s.getConfEntityList()));
        s.showSlices();

        s.execution();

    }
    public void showSlices(){
        Iterator<ConfPropOutput> it = this.slices.iterator();
        while (it.hasNext()){
            Iterator<IRStatement> ss = it.next().statements.iterator();
            while(ss.hasNext()){
                System.out.println(ss.next());
            }
        }

    }
    public List<ConfEntity> getscope(){
//        return readHDFS.getHDFSList();
//        return readHDFS.getHBASEList();
//        return readHDFS.getHDFSBalanceList();
        return readHDFS.getSingle2();
    }
    public Collection<ConfPropOutput> slice(String Path,List<ConfEntity> confList) throws ClassHierarchyException, IOException, CancelException {
        return paraSlice(Path, confList, getExclusive(), Slicer.DataDependenceOptions.NO_BASE_NO_HEAP, Slicer.ControlDependenceOptions.NO_EXCEPTIONAL_EDGES);
    }
    public Collection<ConfPropOutput> paraSlice(String path, List<ConfEntity> confList, String exclusionFile,Slicer.DataDependenceOptions dataDep, Slicer.ControlDependenceOptions controlDep) throws ClassHierarchyException, IOException, CancelException {
        Helper helper = new Helper(path,exclusionFile,dataDep,controlDep);
//        构建调用图
        helper.Build();
        Collection<ConfPropOutput> outputs =new LinkedList<ConfPropOutput>();
        for (ConfEntity entity:confList){
            ConfPropOutput output = helper.outputSliceConfOption(entity);
            outputs.add(output);
        }
        readHDFS.checkTrue(confList.size() == outputs.size());

        long startTime = System.currentTimeMillis();
        for (ConfPropOutput output :outputs){
            output.setConfigurationSlicer(helper);
        }
        long endTime =System.currentTimeMillis();
        System.out.println("切片计时： " + (endTime - startTime)/1000 + "s");
        return outputs;
    }

    public void execution() {
        long start = System.currentTimeMillis();
        Iterator<ConfPropOutput> it = slices.iterator();

        int forCount = 0;
        while (it.hasNext()) {
//            System.out.println("test");

            forCount++;

            ConfPropOutput head = it.next();
            Iterator<IRStatement> IRit = head.statements.iterator();
            while (IRit.hasNext()) {
                IRStatement IRstmt = IRit.next();
                String node = IRstmt.IRHeadString();
                int linenumber = IRstmt.getLineNumber();

                Iterator<ConfPropOutput> forit = slices.iterator();
                for (int i = 1; i <= forCount; i++) {
                    forit.next();
                }

                while (forit.hasNext()) {
                    ConfPropOutput next = forit.next();
                    Iterator<IRStatement> nextIRit = next.statements.iterator();
                    while (nextIRit.hasNext()) {

                        IRStatement nextIRstmt = nextIRit.next();
                        String nextNode = nextIRstmt.IRHeadString();
                        int nextLinenumber = nextIRstmt.getLineNumber();

                        if (node.equals(nextNode) && linenumber == nextLinenumber && linenumber != -1) {
                            IRstmt.sameCount++;
                            nextIRstmt.sameCount++;

                            IRstmt.setEachCount(next.conf);
                            nextIRstmt.setEachCount(head.conf);
                        }
                    }
                }
            }
        }

        /////3.看一下samecount

        Iterator<ConfPropOutput> it2 = slices.iterator();
        while (it2.hasNext()) {
            ConfPropOutput body = it2.next();
            Iterator<IRStatement> Stmt = body.statements.iterator();

            while (Stmt.hasNext()) {

                IRStatement stmt = Stmt.next();
                if (stmt.sameCount != 0) {
                    stmt.Analysis(this.confEntityList);
//                    stmt.output();
                }

            }
            System.out.println("-----------------------每个ConfPropOutput samecount-------------------------------------------");
            System.out.println(body.conf);
            body.OutputAnalysis(this.confEntityList);
            body.RatioRank();
        }
        long end = System.currentTimeMillis();
        System.out.println(" 迭代时间：  " + (end - start) /1000 + "s");
    }
}
