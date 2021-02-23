package ubc.cosc322;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;

public class Analysis {
    static private ExecutorService thread_manager = Executors.newCachedThreadPool();

    //320 bottom level operations -> new positions to look at
    public static ArrayList<Integer>[] GetMoveList(LocalState board, BoardPiece[] player_pieces){
        ArrayList<Integer>[] all_moves = new ArrayList[4];
        for(int i = 0; i < 4; ++i){
            all_moves[i] = ScanMoves(board,player_pieces[i].pos);
        }
        return all_moves;
    }

    //25600 bottom level operations if used on the 320 new positions from above
    public static ArrayList<Integer>[] GetOpenPositions(LocalState board, ArrayList<Integer> starting_positions){
        ArrayList<Integer>[] all_moves = new ArrayList[starting_positions.size()];
        for(int i = 0; i < starting_positions.size(); ++i){
            all_moves[i] = ScanMoves(board,starting_positions.get(i));
        }
        return all_moves;
    }

    public static ArrayList<Integer>[] GetMoveListThreaded(LocalState board, BoardPiece[] player_pieces) throws ExecutionException, InterruptedException {
        ArrayList<Integer>[] all_moves = new ArrayList[4];
        Future<ArrayList<Integer>>[] ret_values = new Future[4];
        for(int i = 0; i < 4; ++i){
            BoardPiece piece = player_pieces[i];
            ret_values[i] = thread_manager.submit(() -> ScanMoves(board,piece.pos));
        }
        for(int i = 0; i < 4; ++i){
            all_moves[i] = ret_values[i].get();
        }
        return all_moves;
    }

    ///Helper functions

    //this has been optimized to death
    protected static void ScanDirection(Integer[] moves, int start_index, LocalState board, int x, int y, int xi, int yi){
        x += xi;
        y += yi;
        int i = start_index;
        Function<Integer, Boolean> check_in_range = (v) -> {
            return (v < 11 && v > 0);
        }; //hoping the JVM is gonna inline this =)
        while(check_in_range.apply(x) && check_in_range.apply(y)){
            int index = (x*11)+y;
            if(board.ReadTile(index) != 0){
                break;
            }
            moves[i++] = index;
            x += xi;
            y += yi;
        }
        while(i < start_index + 10){
            moves[i++] = -1;
        }
    }

    protected static ArrayList<Integer> ScanMoves(LocalState board, int index){
        return ScanMoves(board,new Position(index));
    }

    //this will always be faster than a parallel version for the board size we have
    protected static ArrayList<Integer> ScanMoves(LocalState board, Position pos){
        Integer[] moves = new Integer[80];

        ScanDirection(moves,0,board,pos.x,pos.y,1,1);
        ScanDirection(moves,10,board,pos.x,pos.y,-1,-1);
        ScanDirection(moves,20,board,pos.x,pos.y,1,-1);
        ScanDirection(moves,30,board,pos.x,pos.y,-1,1);

        ScanDirection(moves,40,board,pos.x,pos.y,1,0);
        ScanDirection(moves,50,board,pos.x,pos.y,-1,0);
        ScanDirection(moves,60,board,pos.x,pos.y,0,1);
        ScanDirection(moves,70,board,pos.x,pos.y,0,-1);

        return new ArrayList<>(Arrays.asList(moves));
    }
}
