package com.tinkerpop.gremlin.process.graph.step.sideEffect;

import com.tinkerpop.gremlin.process.Path;
import com.tinkerpop.gremlin.process.Traversal;
import com.tinkerpop.gremlin.process.computer.MapReduce;
import com.tinkerpop.gremlin.process.graph.marker.MapReducer;
import com.tinkerpop.gremlin.process.graph.marker.PathConsumer;
import com.tinkerpop.gremlin.process.graph.marker.Reversible;
import com.tinkerpop.gremlin.process.graph.marker.SideEffectCapable;
import com.tinkerpop.gremlin.process.graph.step.sideEffect.mapreduce.TreeMapReduce;
import com.tinkerpop.gremlin.process.graph.util.Tree;
import com.tinkerpop.gremlin.process.util.FunctionRing;
import com.tinkerpop.gremlin.process.util.TraversalHelper;
import com.tinkerpop.gremlin.structure.Graph;

import java.util.function.Function;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class TreeStep<S> extends SideEffectStep<S> implements Reversible, PathConsumer, SideEffectCapable, MapReducer<Object, Tree, Object, Tree, Tree> {

    public FunctionRing functionRing;
    private final String sideEffectKey;

    public TreeStep(final Traversal traversal, final String sideEffectKey, final Function... branchFunctions) {
        super(traversal);
        this.sideEffectKey = null == sideEffectKey ? this.getLabel() : sideEffectKey;
        this.functionRing = new FunctionRing(branchFunctions);
        TraversalHelper.verifySideEffectKeyIsNotAStepLabel(this.sideEffectKey, this.traversal);
        this.setConsumer(traverser -> {
            Tree depth = traversal.sideEffects().getOrCreate(this.sideEffectKey, Tree::new);
            final Path path = traverser.getPath();
            for (int i = 0; i < path.size(); i++) {
                final Object object = functionRing.next().apply(path.get(i));
                if (!depth.containsKey(object))
                    depth.put(object, new Tree<>());
                depth = (Tree) depth.get(object);
            }
            this.functionRing.reset();
        });
    }

    @Override
    public String getSideEffectKey() {
        return this.sideEffectKey;
    }

    @Override
    public MapReduce<Object, Tree, Object, Tree, Tree> getMapReduce() {
        return new TreeMapReduce(this);
    }

    @Override
    public void reset() {
        super.reset();
        this.functionRing.reset();
    }

    @Override
    public String toString() {
        return Graph.Key.isHidden(this.sideEffectKey) ? super.toString() : TraversalHelper.makeStepString(this, this.sideEffectKey);
    }
}
