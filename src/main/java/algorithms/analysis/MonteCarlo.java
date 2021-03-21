package algorithms.analysis;

import org.apache.commons.math3.util.Pair;
import structures.GameTree;
import structures.GameTreeNode;
import structures.LocalState;
import structures.Move;
import tools.RandomGen;

import java.util.*;

public class MonteCarlo {
    public static class SimPolicy{
        public enum policy_type{
            BREADTH_FIRST,
            MONTE_CARLO
        }
        public int branches = 1;
        public int depth = 1;
        public policy_type type;
        public SimPolicy(int branches, int depth, policy_type sim_type){
            this.branches = branches;
            this.depth = depth;
            this.type = sim_type;
        }
    }

    public static boolean RunSimulation(LocalState board, SimPolicy sim_policy) {
        RandomGen rng = new RandomGen();
        GameTreeNode sim_root = GameTree.get(board);
        if(sim_root == null){
            // This should only be the case if this is the beginning of the game. todo: ensure this holds true?
            sim_root = new GameTreeNode(new Move(),null);
            GameTree.put(board,sim_root); //in the off chance our two threads run this line at the same time, the reference should be the same.. so it should not matter which gets there first
        }
        RunSimulation(rng, board, sim_root, sim_policy.branches, sim_policy.depth, sim_policy.type);
        return !Thread.interrupted(); //Assuming execution was interrupted then we need to clear that flag, and restart from the current LocalState
    }

    private static void RunSimulation(RandomGen rng, LocalState board, GameTreeNode parent, int branches, int depth, SimPolicy.policy_type type) {
        /* Simulate X branches at Y depths
         * simulate X branches
         ** On each branch simulate X branches
         * repeat until at Y depth
         * */
        if (depth > 0 && !board.IsGameOver() && !Thread.currentThread().isInterrupted()) {
            ArrayList<Move> moves = MoveCompiler.GetMoveList(board, board.GetTurnPieces(), true);
            if (moves == null || moves.size() == 0) {
                return;
            }
            switch (type) {
                case BREADTH_FIRST:
                    moves = PruneMoves(board, parent, moves, new TreePolicy(0, 0, TreePolicy.policy_type.DO_NOTHING));
                    break;
                case MONTE_CARLO:
                    // todo (debug): this is probably going to cause a problem.. we'll see
                    int sample_size = moves.size() >> 1;
                    int branches2 = branches << 1;
                    int bound = Math.max(branches2, sample_size - branches2);
                    if(bound > 0) {
                        moves = PruneMoves(board, parent, moves, new TreePolicy(rng.nextInt(bound) + branches2, branches, rng.get_random_policy()));
                    } else {
                        moves = PruneMoves(board, parent, moves, new TreePolicy(branches << 1, branches, rng.get_random_policy()));
                    }
                    break;
            }
            if (moves == null || moves.size() == 0) {
                return;
            }
            List<Integer> rng_set = rng.GetDistinctSequenceShuffled(0, moves.size()-1, Math.min(branches, moves.size()));
            Queue<Pair<LocalState,GameTreeNode>> branch_jobs = new LinkedList<>();
            for (int b = 0; b < branches && b < moves.size(); ++b) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                /* Copy board
                 * Perform move on board's copy
                 * Check if GameTree has this board state already
                 * + retrieve existing node
                 * - make new node
                 * - update GameTree
                 * Perform simulation on this board state
                 * Update node according to simulations run under it
                 * */
                LocalState new_state = new LocalState(board);
                Move m = moves.get(rng_set.get(b));
                new_state.MakeMove(m, true);

                GameTreeNode node = GameTree.get(new_state); // GameTreeNode might already exist for this state [original_state + move]
                if (node == null) {
                    // LocalState is a new position
                    node = new GameTreeNode(m, parent);
                    parent.adopt(node);
                    Heuristics.enqueue(new Pair<>(new_state, node));
                    GameTree.put(new_state, node);
                } else {
                    // This LocalState + Node have already been seen once.
                    // This might represent branches merging so..
                    // run the adoption procedure to ensure linkage and propagation of the heuristic (only one link, and only propagates if node's heuristic is non-zero)
                    parent.adopt(node);
                }
                branch_jobs.add(new Pair<>(new_state,node));
            }
            while(!branch_jobs.isEmpty()){
                var job = branch_jobs.poll();
                RunSimulation(rng, job.getFirst(), job.getSecond(), branches, depth - 1, type);
            }
        }
    }

    public static class TreePolicy{
        public enum policy_type{
            FIRST_DEGREE_MOVES,
            COUNT_HEURISTIC,
            TERRITORY,
            ALL_HEURISTICS,
            DO_NOTHING
        }
        policy_type type;
        int sample_size;
        int max_return;
        TreePolicy(int N,int M, policy_type T){
            sample_size = N;
            max_return = M;
            type = T;
        }
    }

    private static ArrayList<Move> PruneMoves(LocalState board, GameTreeNode parent, ArrayList<Move> moves, TreePolicy tree_policy){
        if(moves == null){
            return null;
        }
        tree_policy.sample_size = Math.min(tree_policy.sample_size, moves.size());
        tree_policy.max_return = Math.min(tree_policy.max_return, tree_policy.sample_size);
        if(tree_policy.max_return == moves.size() || tree_policy.sample_size == 0 || tree_policy.type == TreePolicy.policy_type.DO_NOTHING){
            return moves;
        }
        RandomGen rng = new RandomGen();
        TreeSet<GameTreeNode> sample = new TreeSet<>(new GameTreeNode.NodeComparator());
        List<Integer> selection = rng.GetDistinctSequenceShuffled(0, moves.size()-1, tree_policy.sample_size);
        for(int i = 0; i < tree_policy.sample_size; ++i){
            LocalState copy = new LocalState(board);
            Move move = moves.get(selection.get(i));
            copy.MakeMove(move,true);
            GameTreeNode node = GameTree.get(copy);
            if(node == null){
                // todo (10): implement exploration consideration
                node = new GameTreeNode(move,parent);
                parent.adopt(node);
                GameTree.put(copy,node);
            }
            sample.add(node);
            boolean enqueue = true;
            switch(tree_policy.type){
                case FIRST_DEGREE_MOVES:
                    Heuristics.CalculateHeuristicFirstDegree(copy,node);
                    Heuristics.enqueue(new Pair<>(copy,node));
                    break;
                case COUNT_HEURISTIC:
                    Heuristics.CalculateHeuristicCount(copy,node);
                    Heuristics.enqueue(new Pair<>(copy,node));
                    break;
                case TERRITORY:
                    Heuristics.CalculateHeuristicTerritory(copy,node);
                    Heuristics.enqueue(new Pair<>(copy,node));
                    break;
                case ALL_HEURISTICS:
                    // all of the above combined
                    Heuristics.CalculateHeuristicsAll(copy,node);
                    break;
            }
        }
        moves = new ArrayList<>(tree_policy.max_return);
        int i = 0;
        for(GameTreeNode n : sample.descendingSet()){
            if(i++ < tree_policy.max_return) {
                moves.add(n.move.get());
            } else {
                return moves;
            }
        }
        return moves;
    }
}
