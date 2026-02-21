package com.example.tictactoe_global.service;

import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class GameService {

    public boolean checkWin(String[] board, String symbol) {
        int[][] winPatterns = {
                { 0, 1, 2 }, { 3, 4, 5 }, { 6, 7, 8 }, // Rows
                { 0, 3, 6 }, { 1, 4, 7 }, { 2, 5, 8 }, // Cols
                { 0, 4, 8 }, { 2, 4, 6 } // Diagonals
        };

        for (int[] pattern : winPatterns) {
            if (symbol.equals(board[pattern[0]]) &&
                    symbol.equals(board[pattern[1]]) &&
                    symbol.equals(board[pattern[2]])) {
                return true;
            }
        }
        return false;
    }

    public boolean checkDraw(String[] board) {
        return Arrays.stream(board).allMatch(cell -> cell != null);
    }

    public int getBestMove(String[] board, String aiSymbol, String playerSymbol, String difficulty) {
        if ("Easy".equalsIgnoreCase(difficulty)) {
            return getRandomMove(board);
        } else if ("Medium".equalsIgnoreCase(difficulty)) {
            // Try to win or block, else random
            int move = findWinningMove(board, aiSymbol);
            if (move != -1)
                return move;
            move = findWinningMove(board, playerSymbol);
            if (move != -1)
                return move;
            return getRandomMove(board);
        } else {
            // Impossible: Minimax
            return minimax(board, aiSymbol, aiSymbol, playerSymbol).index;
        }
    }

    private int getRandomMove(String[] board) {
        java.util.List<Integer> available = new java.util.ArrayList<>();
        for (int i = 0; i < board.length; i++) {
            if (board[i] == null)
                available.add(i);
        }
        if (available.isEmpty())
            return -1;
        return available.get(new java.util.Random().nextInt(available.size()));
    }

    private int findWinningMove(String[] board, String symbol) {
        for (int i = 0; i < 9; i++) {
            if (board[i] == null) {
                board[i] = symbol;
                if (checkWin(board, symbol)) {
                    board[i] = null;
                    return i;
                }
                board[i] = null;
            }
        }
        return -1;
    }

    private Move minimax(String[] board, String currentSymbol, String aiSymbol, String playerSymbol) {
        if (checkWin(board, aiSymbol))
            return new Move(10);
        if (checkWin(board, playerSymbol))
            return new Move(-10);
        if (checkDraw(board))
            return new Move(0);

        java.util.List<Move> moves = new java.util.ArrayList<>();
        for (int i = 0; i < 9; i++) {
            if (board[i] == null) {
                Move move = new Move();
                move.index = i;
                board[i] = currentSymbol;

                if (currentSymbol.equals(aiSymbol)) {
                    move.score = minimax(board, playerSymbol, aiSymbol, playerSymbol).score;
                } else {
                    move.score = minimax(board, aiSymbol, aiSymbol, playerSymbol).score;
                }

                board[i] = null;
                moves.add(move);
            }
        }

        int bestMove = 0;
        if (currentSymbol.equals(aiSymbol)) {
            int bestScore = -10000;
            for (int i = 0; i < moves.size(); i++) {
                if (moves.get(i).score > bestScore) {
                    bestScore = moves.get(i).score;
                    bestMove = i;
                }
            }
        } else {
            int bestScore = 10000;
            for (int i = 0; i < moves.size(); i++) {
                if (moves.get(i).score < bestScore) {
                    bestScore = moves.get(i).score;
                    bestMove = i;
                }
            }
        }
        return moves.get(bestMove);
    }

    private static class Move {
        int index;
        int score;

        Move() {
        }

        Move(int score) {
            this.score = score;
        }
    }
}
