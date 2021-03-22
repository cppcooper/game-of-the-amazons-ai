package algorithms.analysis;

import org.junit.jupiter.api.Test;
import structures.Debug;
import structures.LocalState;
import structures.Position;
import tools.RandomGen;
import static org.junit.jupiter.api.Assertions.*;

public class MiscTests {
    @Test
    void find_first_degree_range() {
        LocalState board = new LocalState();
        int[] positions = Debug.GetAllPositions();
        int[][] first_degree_territory = MoveCompiler.GetOpenPositions(board,positions);
        int max = 0;
        int min = Integer.MAX_VALUE;
        for(int i = 0; i < first_degree_territory.length; ++i) {
            int first_degree = 0;
            for (int x : first_degree_territory[i]) {
                if (x < 0) {
                    break;
                }
                first_degree++;
            }
            if(first_degree > max){
                max = first_degree;
            }
            if(first_degree < min){
                min = first_degree;
            }
        }
        System.out.printf("min: %d\nmax: %d\n",min,max);
    }

    @Test
    void find_max_first_degree_heuristic(){
        final RandomGen rng = new RandomGen();
        final int trials = 1000000;
        double max = Double.NEGATIVE_INFINITY;
        double min = Double.POSITIVE_INFINITY;
        for(int i = 0; i < trials; ++i){
            LocalState board = rng.GetRandomBoard();
            double heuristic = Heuristics.GetFirstDegreeMoveHeuristic(board);
            if(heuristic > max){
                max = heuristic;
            }
            if(heuristic < min){
                min = heuristic;
            }
        }
        System.out.printf("min: %.3f\nmax: %.3f\n",min,max);
    }

    @Test
    void find_max_count_heuristic(){
        final RandomGen rng = new RandomGen();
        final int trials = 100000;
        double max = Double.NEGATIVE_INFINITY;
        double min = Double.POSITIVE_INFINITY;
        for(int i = 0; i < trials; ++i){
            LocalState board = rng.GetRandomBoard(0.9);
            double heuristic = Heuristics.GetCountHeuristic(board);
            if(heuristic > max){
                max = heuristic;
            }
            if(heuristic < min){
                min = heuristic;
            }
        }
        System.out.printf("min: %.3f\nmax: %.3f\n",min,max);
    }

    @Test
    void find_max_territory_count(){
        final RandomGen rng = new RandomGen();
        final int trials = 100000;
        double max = Double.NEGATIVE_INFINITY;
        double min = Double.POSITIVE_INFINITY;
        for(int i = 0; i < trials; ++i){
            LocalState board = new LocalState(rng.GetRandomState(),true,true);/*new int[121]);/**/
            double heuristic = Heuristics.GetTerritoryHeuristic(board);
            boolean new_value = false;
            if(heuristic > max){
                new_value = true;
                max = heuristic;
            }
            if(heuristic < min){
                new_value = true;
                min = heuristic;
            }
            if(new_value){
                System.out.printf("--------\nnew min: %.3f\nnew max: %.3f\n",min,max);
            }
        }
        System.out.printf("min: %.3f\nmax: %.3f\n",min,max);
    }

    @Test
    void testing_infinite(){
        assertEquals(Double.NEGATIVE_INFINITY < 0, true);
    }

    @Test
    void probability_test(){
        final int trials = 1000000;
        int[] counts = new int[5];
        double[] p_values = new double[5];
        RandomGen rng = new RandomGen();
        for(int i = 0; i < trials; ++i){
            switch(rng.get_random_policy()){
                case FIRST_DEGREE_MOVES:
                    counts[0]++;
                    break;
                case COUNT_HEURISTIC:
                    counts[1]++;
                    break;
                case TERRITORY:
                    counts[2]++;
                    break;
                case ALL_HEURISTICS:
                    counts[3]++;
                    break;
                case DO_NOTHING:
                    counts[4]++;
                    break;
            }
        }
        for(int i = 0; i < counts.length; ++i){
            System.out.printf("counts[%d] = %d\n", i, counts[i]);
            p_values[i] = (double)counts[i] / trials;
        }
        for(int i = 0; i < counts.length; ++i) {
            System.out.printf("p_value[%d] = %.2f\n", i, p_values[i]);
        }
    }
}
