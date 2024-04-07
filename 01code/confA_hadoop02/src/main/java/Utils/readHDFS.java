package Utils;

import entity.ConfEntity;

import java.util.LinkedList;
import java.util.List;

public class readHDFS {
    public static List<ConfEntity> getHDFSList(){
        ConfEntity c1 = new ConfEntity("Lorg/apache/hadoop/hdfs/client/impl/DfsClientConf", "uMask");
        ConfEntity c2 = new ConfEntity("Lorg/apache/hadoop/hdfs/client/impl/DfsClientConf", "maxRetryAttempts");
        ConfEntity c3 = new ConfEntity("Lorg/apache/hadoop/hdfs/client/impl/DfsClientConf", "blockWriteLocateFollowingInitialDelayMs");
        ConfEntity c4 = new ConfEntity("Lorg/apache/hadoop/hdfs/client/impl/DfsClientConf", "hedgedReadThreadpoolSize");

        List<ConfEntity> confEntityList = new LinkedList<ConfEntity>();
        confEntityList.add(c1);
        confEntityList.add(c2);
        confEntityList.add(c3);
        confEntityList.add(c4);

        return confEntityList;
    }
    public static List<ConfEntity> getHDFSBalanceList(){
        ConfEntity c1 = new ConfEntity("Lorg/apache/hadoop/hdfs/server/balancer/Balancer","threshold");
        ConfEntity c2 = new ConfEntity("Lorg/apache/hadoop/hdfs/server/balancer/Balancer","defaultBlockSize");

        List<ConfEntity> confEntityList = new LinkedList<ConfEntity>();
        confEntityList.add(c1);
        confEntityList.add(c2);

        return confEntityList;

    }
    public static List<ConfEntity> getHDFSClient(){
        ConfEntity c1 = new ConfEntity("Lorg/apache/hadoop/hdfs/qjournal/client/QuorumJournalManager","maxTxnsPerRpc");
        ConfEntity c2 = new ConfEntity("Lorg/apache/hadoop/hdfs/qjournal/client/QuorumJournalManager","inProgressTailingEnabled");
        ConfEntity c3 = new ConfEntity("Lorg/apache/hadoop/hdfs/server/balancer/Balancer","threshold");
        ConfEntity c4 = new ConfEntity("Lorg/apache/hadoop/hdfs/server/balancer/Balancer","defaultBlockSize");
        ConfEntity c5 = new ConfEntity("Lorg/apache/hadoop/hdfs/server/namenode/NamenodeFsck","staleInterval");

        List<ConfEntity> confEntityList = new LinkedList<ConfEntity>();
        confEntityList.add(c1);
        confEntityList.add(c2);
        confEntityList.add(c3);
        confEntityList.add(c4);
        confEntityList.add(c5);

        return confEntityList;
    }
    public static List<ConfEntity> getSingle(){
        ConfEntity c1 = new ConfEntity("Lorg/apache/hadoop/hdfs/server/namenode/NNStorageRetentionManager", "numCheckpointsToRetain");
        ConfEntity c2 = new ConfEntity("Lorg/apache/hadoop/hdfs/server/namenode/NNStorageRetentionManager", "numExtraEditsToRetain");
        ConfEntity c3 = new ConfEntity("Lorg/apache/hadoop/hdfs/server/balancer/Balancer","threshold");
        ConfEntity c4 = new ConfEntity("Lorg/apache/hadoop/hdfs/server/balancer/Balancer","defaultBlockSize");

        List<ConfEntity> confEntityList = new LinkedList<ConfEntity>();
        confEntityList.add(c1);
        confEntityList.add(c2);
        confEntityList.add(c3);
        confEntityList.add(c4);

        return confEntityList;
    }
    public static List<ConfEntity> getSingle2(){
        ConfEntity c1 = new ConfEntity("Lorg/apache/hadoop/ha/HealthMonitor", "lastServiceState");
        ConfEntity c2 = new ConfEntity("Lorg/apache/hadoop/ha/HealthMonitor", "callbacks");
        ConfEntity c3 = new ConfEntity("Lorg/apache/hadoop/fs/FSDataOutputStreamBuilder","bufferSize");
//        ConfEntity c4 = new ConfEntity("Lorg/apache/hadoop/hdfs/server/balancer/Balancer","defaultBlockSize");

        List<ConfEntity> confEntityList = new LinkedList<ConfEntity>();
        confEntityList.add(c1);
        confEntityList.add(c2);
        confEntityList.add(c3);
//        confEntityList.add(c4);

        return confEntityList;
    }
    public static List<ConfEntity> getHBASEList(){
        ConfEntity c1 = new ConfEntity("Lorg/apache/hadoop/hbase/client/AsyncConnectionConfiguration", "rpcTimeoutNs");
        ConfEntity c2 = new ConfEntity("Lorg/apache/hadoop/hbase/client/AsyncConnectionConfiguration", "readRpcTimeoutNs");
        ConfEntity c3 = new ConfEntity("Lorg/apache/hadoop/hbase/client/AsyncConnectionConfiguration", "writeRpcTimeoutNs");
        ConfEntity c4 = new ConfEntity("Lorg/apache/hadoop/hbase/client/AsyncConnectionConfiguration", "writeBufferSize");
        ConfEntity c5 = new ConfEntity("Lorg/apache/hadoop/hbase/client/AsyncConnectionConfiguration", "scannerMaxResultSize");

        List<ConfEntity> confEntityList = new LinkedList<ConfEntity>();
        confEntityList.add(c1);
        confEntityList.add(c2);
        confEntityList.add(c3);
        confEntityList.add(c4);
        confEntityList.add(c5);


        return confEntityList;
    }

    public static void checkNotNull(Object o,String msg){
        if (o == null){
            System.err.println(msg);
            throw new RuntimeException(msg);
        }
    }
    public static void checkTrue(boolean b, String s) {
        if (!b){
            System.err.println(s);
            throw new RuntimeException(s);
        }
    }
    public static void checkNotNull(Object o){
        checkNotNull(o,null);
    }
    public static void checkTrue(boolean b){
        checkTrue(b,"");
    }
    public static String translateSlashToDot(String str) {
        assert str != null;
        return str.replace('/', '.');
    }
}
