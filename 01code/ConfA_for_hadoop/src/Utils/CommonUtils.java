package Utils;

import Repository.ConfEntityRepository;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.util.CancelException;
import entity.ConfEntity;
import entity.ConfPropOutput;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import entity.ConfigurationSlicer;
import entity.ConfigurationSlicer.CG;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import Utils.Log;
import entity.IRStatement;
import Utils.ConfUtils;

public class CommonUtils {
//    static String Prefix = "Lorg/apache/hadoop/";
//    static String key ="putfield";
//// doPruning false
    public static Collection<ConfPropOutput> getConfPropOutputs(String path, String mainClass, List<ConfEntity> confList, boolean doPruning) throws CancelException {
        return getConfPropOutputs(path,mainClass,confList,"JavaAllExclusions.txt",doPruning);
    }

    public static Collection<ConfPropOutput> getConfPropOutputs(String path, String mainClass, List<ConfEntity> confList, String exclusionFile, boolean doPruning) throws CancelException {
        return getConfPropOutputs(path, mainClass, confList, exclusionFile, CG.ZeroCFA, doPruning);
    }
    ///// 数据流 控制流无 ??????????????????
    public static Collection<ConfPropOutput> getConfPropOutputs(String path, String mainClass, List<ConfEntity> confList, String exclusionFile, CG type, boolean doPruning) throws CancelException {
//        return getConfPropOutputs(path, mainClass, confList, exclusionFile, type, doPruning, DataDependenceOptions.NO_BASE_NO_HEAP_NO_EXCEPTIONS, ControlDependenceOptions.NONE);
        return getConfPropOutputsH(path, mainClass, confList, exclusionFile, type, doPruning, DataDependenceOptions.NO_BASE_NO_HEAP, ControlDependenceOptions.NO_EXCEPTIONAL_EDGES);

    }
    public static Collection<ConfPropOutput> getConfPropOutputsH(String path, String mainClass, List<ConfEntity> confList, String exclusionFile, CG type, boolean doPruning,
                                                                 DataDependenceOptions dataDep, ControlDependenceOptions controlDep) throws CancelException {
        ConfigurationSlicer helper = new ConfigurationSlicer(path,mainClass);
        helper.setCGType(type);
        helper.setExclusiveFile(exclusionFile);
        helper.setDataDependenceOptions(dataDep);
        helper.setControlDependenceOptions(controlDep);
        helper.setContextSensitive(false);
// 构建调用图
        helper.buildAnalysis();

        ConfEntityRepository repo =new ConfEntityRepository(confList);

        //repo.initializeTypesInConfEntities(path);
        Collection<ConfPropOutput> outputs =new LinkedList<ConfPropOutput>();

        for (ConfEntity entity:confList){
            Log.logln("entity:" + entity);

            ConfPropOutput output = helper.outputSliceConfOption(entity);

            outputs.add(output);
            Log.logln("  statement in slice: " + output.statements.size());

            Set<IRStatement> filtered = ConfPropOutput.excludeIgnorableStatements(output.statements);
            System.out.println("  statements after filtering: " + filtered.size());
            ////去掉依赖包的
            Log.logln("  statements after filtering: " + filtered.size());
            //////???????????
            Set<IRStatement> sameStmts = ConfUtils.removeSameStmtsInDiffContexts(filtered);
            System.out.println("  filtered statements: " + sameStmts.size());
            Log.logln("  filtered statements: " + sameStmts.size());

            for (IRStatement s:sameStmts){
                Log.logln("       statement: " + s);
            }

            Set<IRStatement> branchStmts = ConfPropOutput.extractBranchStatements(sameStmts);
            System.out.println("  branching statements: " + branchStmts.size());
            Log.logln("  branching statements: " + branchStmts.size());

            dumpStatements(branchStmts);

        }

        Utils.checkTrue(confList.size() == outputs.size());

        for (ConfPropOutput output :outputs){
            output.setConfigurationSlicer(helper);
        }


        return outputs;
    }
    /*public static Collection<ConfPropOutput> getConfPropOutputs(String path, String mainClass, List<ConfEntity> confList, String exclusionFile, CG type, boolean doPruning,
                                                                DataDependenceOptions dataDep, ControlDependenceOptions controlDep){
        ConfigurationSlicer helper = new ConfigurationSlicer(path,mainClass);
        helper.setCGType(type);
        helper.setExclusiveFile(exclusionFile);
        helper.setDataDependenceOptions(dataDep);
        helper.setControlDependenceOptions(controlDep);
        helper.setContextSensitive(false);
//构建入口点
        helper.buildAnalysis();

        ConfEntityRepository repo =new ConfEntityRepository(confList);
        repo.initializeTypesInConfEntities(path);
        Collection<ConfPropOutput> outputs =new LinkedList<ConfPropOutput>();
        for (ConfEntity entity:confList){
            Log.logln("entity:" + entity);
            ConfPropOutput output = helper.outputSliceConfOption(entity);
            outputs.add(output);
            Log.logln("  statement in slice: " + output.statements.size());

            Set<IRStatement> filtered = ConfPropOutput.excludeIgnorableStatements(output.statements);
            System.out.println("  statements after filtering: " + filtered.size());
            ////去掉依赖包的
            Log.logln("  statements after filtering: " + filtered.size());
                //////???????????
            Set<IRStatement> sameStmts = ConfUtils.removeSameStmtsInDiffContexts(filtered);
            System.out.println("  filtered statements: " + sameStmts.size());
            Log.logln("  filtered statements: " + sameStmts.size());

            for (IRStatement s:sameStmts){
                Log.logln("       statement: " + s);
            }

            Set<IRStatement> branchStmts = ConfPropOutput.extractBranchStatements(sameStmts);
            System.out.println("  branching statements: " + branchStmts.size());
            Log.logln("  branching statements: " + branchStmts.size());

            dumpStatements(branchStmts);

        }

        Utils.checkTrue(confList.size() == outputs.size());

        if (doPruning){

        }

        for (ConfPropOutput output :outputs){
            output.setConfigurationSlicer(helper);
        }


        return outputs;

    }*/

    public static void dumpStatements(Collection<IRStatement> stmts) {
        for(IRStatement stmt : stmts) {
            Log.logln("     >> " + stmt.toString());
        }
    }
}
