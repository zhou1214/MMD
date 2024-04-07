package slice;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.modref.DelegatingExtendedHeapModel;
import com.ibm.wala.ipa.modref.ExtendedHeapModel;
import com.ibm.wala.ipa.modref.ModRef;
import com.ibm.wala.ipa.slicer.*;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.MapUtil;
import com.ibm.wala.util.config.SetOfClasses;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.OrdinalSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class test {
    private final SlowSparseNumberedGraph<Statement> delegate = SlowSparseNumberedGraph.make();
    private final CGNode node;
    private Statement[] paramCalleeStatements;
    private Statement[] returnStatements;
    private final Map<CallSiteReference, Set<Statement>> callerParamStatements = HashMapFactory.make();
    private final Map<CallSiteReference, Set<Statement>> callerReturnStatements = HashMapFactory.make();
    private final HeapExclusions exclusions;
    private final Collection<PointerKey> locationsHandled = HashSetFactory.make();
    private final PointerAnalysis pa;
    private final ExtendedHeapModel heapModel;
    private final Map<CGNode, OrdinalSet<PointerKey>> mod;
    private final Slicer.DataDependenceOptions dOptions;
    private final CallGraph cg;
    private final ModRef modRef;
    private final Map<CGNode, OrdinalSet<PointerKey>> ref;
    private final boolean ignoreAllocHeapDefs;
    private boolean isPopulated = false;

    public test(final CGNode node, PointerAnalysis pa, Map<CGNode, OrdinalSet<PointerKey>> mod,Map<CGNode, OrdinalSet<PointerKey>> ref, Slicer.DataDependenceOptions dOptions,HeapExclusions exclusions, CallGraph cg, ModRef modRef) {
        this(node, pa, mod, ref, dOptions, exclusions, cg, modRef, false);
    }
    public test(final CGNode node, PointerAnalysis pa, Map<CGNode, OrdinalSet<PointerKey>> mod,Map<CGNode, OrdinalSet<PointerKey>> ref, Slicer.DataDependenceOptions dOptions,HeapExclusions exclusions, CallGraph cg, ModRef modRef, boolean ignoreAllocHeapDefs) {
        super();
        if (node == null){
            throw new IllegalArgumentException("node is null");
        }
        this.cg = cg;
        this.node = node;
        this.heapModel = pa ==null ? null : new DelegatingExtendedHeapModel(pa.getHeapModel());
        this.pa = pa;
        this.dOptions = dOptions;
        this.mod = mod;
        this.exclusions = exclusions;
        this.modRef =modRef;
        this.ref =ref;
        this.ignoreAllocHeapDefs = ignoreAllocHeapDefs;
    }
    private void createReturnStatements() {
        ArrayList<Statement> list = new ArrayList<Statement>();
        if (!node.getMethod().getReturnType().equals(TypeReference.Void)) {
            NormalReturnCallee n = new NormalReturnCallee(node);
            delegate.addNode(n);
            list.add(n);
        }if (!dOptions.isIgnoreExceptions()) {
            ExceptionalReturnCallee e = new ExceptionalReturnCallee(node);
            delegate.addNode(e);
            list.add(e);
        }
        if (!dOptions.isIgnoreHeap()) {
            for (PointerKey p : mod.get(node)) {
                Statement h = new HeapStatement.HeapReturnCallee(node, p);
                delegate.addNode(h);
                list.add(h);
            }
        }
        returnStatements = new Statement[list.size()];
        list.toArray(returnStatements);
    }
    private void createCalleeParams() {
        if (paramCalleeStatements == null) {
            ArrayList<Statement> list = new ArrayList<Statement>();
            for (int i = 1; i <= node.getMethod().getNumberOfParameters(); i++) {
                ParamCallee s = new ParamCallee(node, i);
                delegate.addNode(s);
                list.add(s);
            }
            if (!dOptions.isIgnoreHeap()) {
                for (PointerKey p : ref.get(node)) {
                    Statement h = new HeapStatement.HeapParamCallee(node, p);
                    delegate.addNode(h);
                    list.add(h);
                }
            }
            paramCalleeStatements = new Statement[list.size()];
            list.toArray(paramCalleeStatements);
        }
    }
    private void addParamPassingStatements(int callIndex, Map<CGNode, OrdinalSet<PointerKey>> ref, IR ir) {
        SSAAbstractInvokeInstruction call = (SSAAbstractInvokeInstruction) ir.getInstructions()[callIndex];
        Collection<Statement> params = MapUtil.findOrCreateSet(callerParamStatements, call.getCallSite());
        Collection<Statement> rets = MapUtil.findOrCreateSet(callerReturnStatements, call.getCallSite());
        for (int j = 0; j < call.getNumberOfUses(); j++) {
            Statement st = new ParamCaller(node, callIndex, call.getUse(j));
            delegate.addNode(st);
            params.add(st);
        }
        if (!call.getDeclaredResultType().equals(TypeReference.Void)) {
            Statement st = new NormalReturnCaller(node, callIndex);
            delegate.addNode(st);
            rets.add(st);
        }
        {
            if (!dOptions.isIgnoreExceptions()) {
                Statement st = new ExceptionalReturnCaller(node, callIndex);
                delegate.addNode(st);
                rets.add(st);
            }
        }
        if (!dOptions.isIgnoreHeap()) {
            OrdinalSet<PointerKey> uref = unionHeapLocations(cg, node, call, ref);
            for (PointerKey p : uref) {
                Statement st = new HeapStatement.HeapParamCaller(node, callIndex, p);
                delegate.addNode(st);
                params.add(st);
            }
            OrdinalSet<PointerKey> umod = unionHeapLocations(cg, node, call, mod);
            for (PointerKey p : umod) {
                Statement st = new HeapStatement.HeapReturnCaller(node, callIndex, p);
                delegate.addNode(st);
                rets.add(st);
            }
        }
    }
    private OrdinalSet<PointerKey> unionHeapLocations(CallGraph cg, CGNode n, SSAAbstractInvokeInstruction call, Map<CGNode, OrdinalSet<PointerKey>> loc) {
        BitVectorIntSet bv = new BitVectorIntSet();
        for (CGNode t : cg.getPossibleTargets(n, call.getCallSite())) {
            bv.addAll(loc.get(t).getBackingSet());
        }
        return new OrdinalSet<PointerKey>(bv, loc.get(n).getMapping());
//    int test =5;
//    int test2;
//    if (test > 1){
//        test2 ++;
//    }else {
//        test2 --;
//    }
    }
}
