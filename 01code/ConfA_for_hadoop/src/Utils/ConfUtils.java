package Utils;

import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.slicer.Statement;
import entity.IRStatement;
import entity.ConfEntity;

import java.util.*;

public class ConfUtils {

    private static Map<ConfEntity, Collection<Statement>> cachedStmts = new LinkedHashMap<ConfEntity, Collection<Statement>>();

    public static Set<IRStatement> removeSameStmtsInDiffContexts(Set<IRStatement> stmts){
        Set<String> existed = new LinkedHashSet<String>();
        Set<IRStatement> filered = new LinkedHashSet<IRStatement>();
        for (IRStatement stmt:stmts){
            String sig = stmt.getUniqueSignature();
            if (existed.contains(sig)){
                continue;
            }
            existed.add(sig);
            filered.add(stmt);
        }
        return filered;
    }
    public static Collection<Statement> getextractAllGetStatements(ConfEntity entity, CallGraph cg) {
        if(cachedStmts.containsKey(entity)) {
            return cachedStmts.get(entity);
        }
        throw new Error("Not built: " + entity);
    }
}
