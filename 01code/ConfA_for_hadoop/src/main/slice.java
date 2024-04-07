package main;

import Utils.Utils;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.pruned.ApplicationLoaderPolicy;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.util.CancelException;
import entity.ConfEntity;
import entity.ConfPropOutput;

import java.util.*;

import Utils.CommonUtils;
import entity.Count;
import entity.IRStatement;

public class slice {
    ///切片jar包路径
    static  String path = "./lib/hadoop-hdfs-2.6.5.jar;./lib/hadoop-common-2.6.5.jar";
    ///入口类
    static String mainClass = "Lorg/apache/hadoop/hdfs/DFSClient$Conf";


    public static void main(String[] args) throws CancelException {
        ///获取配置选项列表
        List<ConfEntity> confList = Utils.getConfList();

//       获得切片集合
        Collection<ConfPropOutput> slices = CommonUtils.getConfPropOutputs(path, mainClass, confList, false);
        ///对切片集合进行处理
        System.out.println("切片结束");
        Iterator<ConfPropOutput> it = slices.iterator();

        int forCount = 0;
        while (it.hasNext()){


            forCount ++ ;

            ConfPropOutput head = it.next();
            Iterator<IRStatement> IRit = head.statements.iterator();
            while (IRit.hasNext()){
                IRStatement IRstmt = IRit.next();
                String node = IRstmt.IRHeadString();
                int linenumber = IRstmt.getLineNumber();

                Iterator<ConfPropOutput> forit = slices.iterator();
                for (int i=1;i<=forCount;i++){
                    forit.next();
                }

                while (forit.hasNext()){
                    ConfPropOutput next = forit.next();
                    Iterator<IRStatement> nextIRit = next.statements.iterator();
                    while (nextIRit.hasNext()){

                        IRStatement nextIRstmt = nextIRit.next();
                        String nextNode = nextIRstmt.IRHeadString();
                        int nextLinenumber = nextIRstmt.getLineNumber();

//                        if (node.equals(nextNode) && linenumber == nextLinenumber && linenumber !=-1){
                        if (node.equals(nextNode)){
                            IRstmt.sameCount ++;
                            nextIRstmt.sameCount ++;

                            IRstmt.setEachCount(next.conf);
                            nextIRstmt.setEachCount(head.conf);
                        }
                    }
                }
            }
        }

        /////3.看一下samecount

        Iterator<ConfPropOutput> it2 = slices.iterator();
        while (it2.hasNext()){
            ConfPropOutput body = it2.next();
            Iterator<IRStatement> Stmt = body.statements.iterator();

            while (Stmt.hasNext()){

                IRStatement stmt = Stmt.next();
                if (stmt.sameCount !=0){
                    stmt.Analysis(confList);
                    stmt.output();
                }

            }
            System.out.println("-----------------------每个ConfPropOutput samecount-------------------------------------------");
            System.out.println(body.conf);
            body.OutputAnalysis(confList);
            body.RatioRank();
        }
    }

}


