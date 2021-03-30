package data;

import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReference;

/** GameTreeNode represents a Move and the value that move has for the player making it
 * We track:
 *  - past moves
 *  - future moves
 *  -
 *
 * */

public class GameTreeNode {
    private final SynchronizedArrayList<GameTreeNode> super_nodes = new SynchronizedArrayList<>();
    private final SynchronizedArrayList<GameTreeNode> sub_nodes = new SynchronizedArrayList<>(); //note: that there is no way to remove nodes! this is by design!
    public final Heuristic heuristic = new Heuristic();
    final public AtomicReference<Move> move = new AtomicReference<>();
    final public AtomicReference<GameState> state_after_move = new AtomicReference<>();

    public GameTreeNode(Move move, GameTreeNode parent, GameState state_after_move){
        this.move.set(move);
        this.state_after_move.set(state_after_move);
        if(parent != null){
            parent.adopt(this);
        }
    }

    public GameTreeNode get(int index){
        return sub_nodes.get(index);
    }
    public int edges(){
        return sub_nodes.size();
    }

    private void add_parent(GameTreeNode parent){
        super_nodes.add(parent);
        if(heuristic.has_propagated.get()){
            force_propagate();
        }
    }
    public void adopt(GameTreeNode node){
        //we don't do anything with heuristics because they won't exist yet when this method is used (RunSim/PruneMoves)
        if(this != node) { //no idea why node == this (other than it happens in the MonteCarlo else)
            if (!sub_nodes.contains(node)) {
                node.add_parent(this);
                sub_nodes.add(node);
            }
        }
    }
    public void disown_children(){
        for(int i = 0; i < sub_nodes.size(); ++i){
            sub_nodes.get(i).super_nodes.clear(); // we're only going to be running this during memory cleanup
            // unless that changes this incorrect looking function is actually correct
        }
    }

    public void propagate(){
        if(!heuristic.has_propagated.get()){
            force_propagate();
        }
    }
    private void force_propagate(){
        heuristic.has_propagated.set(true);
        double h = heuristic.value.get();
        for (int i = 0; i < super_nodes.size(); ++i) {
            GameTreeNode parent = super_nodes.get(i);
            if(parent.heuristic.maximum_sub.get() < h){
                parent.heuristic.maximum_sub.set(h);
            }
            if(parent.heuristic.minimum_sub.get() > h){
                parent.heuristic.minimum_sub.set(h);
            }
        }
    }

    // used in PruneMoves to sort moves according to best for us and least beneficial to the enemy
    public static class NodeComparator implements Comparator<GameTreeNode> {
        @Override
        public int compare(GameTreeNode o1, GameTreeNode o2) {
            int c1 = Double.compare(o1.heuristic.value.get(),o2.heuristic.value.get());
            if(c1 == 0){
                return Double.compare(o1.heuristic.maximum_sub.get(), o2.heuristic.maximum_sub.get());
            }
            return c1;
        }
    }
}