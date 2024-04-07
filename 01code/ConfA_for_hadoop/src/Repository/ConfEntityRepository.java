package Repository;

import Utils.Utils;
import entity.ConfEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;

public class ConfEntityRepository {
    public final Collection<ConfEntity> entities = new LinkedHashSet<ConfEntity>();

    public ConfEntityRepository(ConfEntity[] entities){
        this(Arrays.asList(entities));
    }
    public ConfEntityRepository(Collection<ConfEntity> entities){
        this.entities.addAll(entities);
        if (this.entities.size() != entities.size()){
            System.err.println("Warning, size not equal. Given: "
                    + entities.size() + ", but result in: " + this.entities.size());
        }
    }
    public void initializeTypesInConfEntities(String path){
        for (ConfEntity entity :this.entities){
            String fullClassName = entity.getClassName();
            String fullConfName = entity.getConfName();
            boolean isStatic = entity.getIsStatic();

            Class<?> clz = Utils.loeadClass(path,fullClassName);

            //Utils.checkNotNull(clz,"full class name:" + fullClassName);
            Field f = Utils.lookUpField(clz,fullConfName);
            //Utils.checkNotNull(f);

            boolean isConfStatic = Modifier.isStatic(f.getModifiers());
            Utils.checkTrue(isStatic == isConfStatic,"f is:" + f);

            entity.setType(f.getType().getName());
        }
    }
}
