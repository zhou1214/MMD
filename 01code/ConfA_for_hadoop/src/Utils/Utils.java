package Utils;

import entity.ConfEntity;

import java.io.File;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedList;
import java.util.List;
import Utils.Sep;


public class Utils {
    ///获得配置选项列表
    public static List<ConfEntity> getConfList(){

        ConfEntity entity1 = new ConfEntity("Lorg/apache/hadoop/hdfs/DFSClient$Conf", "shortCircuitLocalReads", true);
        ConfEntity entity2 = new ConfEntity("Lorg/apache/hadoop/hdfs/DFSClient$Conf", "domainSocketDataTraffic", true);
        ConfEntity entity3 = new ConfEntity("Lorg/apache/hadoop/hdfs/DFSClient$Conf", "socketCacheCapacity", true);
        ConfEntity entity4 = new ConfEntity("Lorg/apache/hadoop/hdfs/DFSClient$Conf", "socketCacheExpiry", true);
        ConfEntity entity5 = new ConfEntity("Lorg/apache/hadoop/hdfs/DFSClient$Conf", "timeWindow, true",true);


        List<ConfEntity> list =new LinkedList<ConfEntity>();
        list.add(entity1);
        list.add(entity2);
        list.add(entity3);
        list.add(entity4);
        list.add(entity5);



        return list;
    }

    public static void checkNotNull(Object o){
        checkNotNull(o,null);
    }
    public static void checkTrue(boolean b){
        checkTrue(b,"");
    }

    public static void checkTrue(boolean b, String s) {
        if (!b){
            System.err.println(s);
            throw new RuntimeException(s);
        }
    }

    public static void checkNotNull(Object o,String msg){
        if (o == null){
            System.err.println(msg);
            throw new RuntimeException(msg);
        }
    }
    ///////format
    public static String translateSlashToDot(String str) {
        assert str != null;
        return str.replace('/', '.');
    }
    ///entryPoint  计数
    public static <T> int countIterable(Iterable<T> c) {
        int count = 0;
        for(T t: c) {
            count++;
        }
        return count;
    }
    ///loadclass
    public static Class<?> loeadClass(String classPath,String className){
        String[] paths = classPath.split(Sep.pathSep);
        File[] files = new File[paths.length];
        for (int i =0;i<paths.length;i++){
            files[i] = new File(paths[i]);
        }

        try{
            URL[] urls = new URL[files.length];
            for (int i=0;i< files.length;i++){
                urls[i]=files[i].toURL();
            }
            ClassLoader cl = new URLClassLoader(urls);
            Class<?> cls = cl.loadClass(className);
            return cls;
        }catch (MalformedURLException e){
        }catch (ClassNotFoundException e){

        }
        return  null;
    }

    public static Field lookUpField(Class<?> clz,String confName){
        try{
            Field[] fields =clz.getDeclaredFields();
            for (Field f:fields){
                if (f.getName().equals(confName)){
                    return f;
                }
            }
            throw new Error("Can not find confName: " + confName + " in " + clz.toString());
        }catch(Throwable e){
            throw new Error(e);
        }

    }

    public static List<String> getConfNameList(List<ConfEntity> confList){
        List<String> name = new LinkedList<String>();
        int size = confList.size();
        for (int i=0;i<size;i++){
            name.add(confList.get(i).getConfName());
        }
        return name;
    }


}
