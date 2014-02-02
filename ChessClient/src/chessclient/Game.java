/* -1: Empty space
 *  0: White pawn
 *  1: White knight
 *  2: White bishop
 *  3: White rook
 *  4: White queen
 *  5: White king
 *  6: [unused]
 *  7: [unused]
 *  8: Black pawn
 *  9: Black knight
 * 10: Black bishop
 * 11: Black rook
 * 12: Black queen
 * 13: Black king
 * 
 * This method is convenient for several reasons.  
 * To check if a space is unoccupied, do space != -1.  
 * To check if it's black, do space / 8 == 1.  
 * To check what piece it is, do space % 8.
 * 
 */
package chessclient;

//import java.io.*;
//import java.net.*;
//import java.util.*;
public class Game {

    private boolean turn; //False means it's white's turn, true is black.
	/*
     * If in the last turn, a pawn moved two spaces forward, then this contains 
     * the file where that pawn is.  Otherwise, it contains -1.
     */
    private int enPassantFile;
    private boolean whiteQueenCastle; //Whether the white king can queenside castle
    private boolean whiteKingCastle; //etc
    private boolean blackQueenCastle;
    private boolean blackKingCastle;
    /*
     * The game keeps track of where each king is so it can easily tell if a
     * color is in check, or if a move would put it in check.
     */
    private int whiteKingRank;
    private int whiteKingFile;
    private int blackKingRank;
    private int blackKingFile;
    //Something like this?
    //The 8 different coordinate modifiers for how a knight moves.
    private final int[] knightRank = {-1, -2, -2, -1, 1, 2, 2, 1};
    private final int[] knightFile = {2, 1, -1, -2, -2, -1, 1, 2};
    private final String PIECES = "pnbrqk";
    private final String STARTING_BOARD = "RNBQKBNRPPPPPPPP................"
            + "................pppppppprnbqkbnr";
    private int[][] grid;

    //Constructs a new game
    public Game() {
        grid = new int[8][8];
        turn = false;
        enPassantFile = -1;
        whiteQueenCastle = true;
        whiteKingCastle = true;
        blackQueenCastle = true;
        blackKingCastle = true;
        whiteKingRank = 7;
        whiteKingFile = 4;
        blackKingRank = 0;
        blackKingFile = 4;
        setBoard(STARTING_BOARD);
    }

    //Constructs a game in progress
    public Game(String data) {
        grid = new int[8][8];
        setBoard(data.substring(0, 64));
        turn = data.charAt(64) == '1';
        whiteQueenCastle = data.charAt(65) == '1';
        whiteKingCastle = data.charAt(66) == '1';
        blackQueenCastle = data.charAt(67) == '1';
        blackKingCastle = data.charAt(68) == '1';
        enPassantFile = data.charAt(69) - '0';
        //Let's find the kings
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (grid[i][j] == 5) {
                    whiteKingRank = i;
                    whiteKingFile = j;

                } else if (grid[i][j] == 13) {
                    blackKingRank = i;
                    blackKingFile = j;
                }
            }
        }//No exit condition because it would probably just make it less efficient.
    }

    private void setBoard(String board) {
        for (int i = 0; i < 64; i++) {
            char piece = board.charAt(i);
            int space = 0;
            if (Character.isUpperCase(piece)) {
                space += 8;
            }
            space += PIECES.indexOf(Character.toLowerCase(piece));
            grid[i / 8][i % 8] = space;
        }
    }

    public boolean getTurn() {
        return turn;
    }

    public int getPiece(int rank, int file) {
        return grid[rank][file];
    }

    /*
     * Server function only, check if the piece at this rank and file is a piece
     * that this color is allowed to move
     */
    public boolean getValidSelection(int rank, int file, boolean color) {
        return grid[rank][file] != -1 && (grid[rank][file] / 8 == 1) == color;
    }

    //Returns a 70-char string representing the entire game state.
    public String gameState() {
        String state = "";
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                int space = grid[i][j];
                if (space == -1) {
                    state += '.';
                } else {
                    char piece = PIECES.charAt(space % 8);
                    if (space >= 8) {
                        piece = Character.toUpperCase(piece);
                    }
                    state += piece;
                }
            }
        }
        state += (turn) ? '1' : '0';
        state += (whiteQueenCastle) ? '1' : '0';
        state += (whiteKingCastle) ? '1' : '0';
        state += (blackQueenCastle) ? '1' : '0';
        state += (blackKingCastle) ? '1' : '0';
        state += (char) (enPassantFile + '0');
        return state;
    }

    //These two functions are for testing.
    public void print() {
        print(grid);
    }

    public void print(int[][] board) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                int space = board[i][j];
                if (space == -1) {
                    System.out.print('.');
                } else if (space / 8 == 1) {
                    System.out.print(Character.toUpperCase(PIECES.charAt(space % 8)));
                } else {
                    System.out.print(PIECES.charAt(space % 8));
                }
            }
            System.out.println();
        }
    }

    //Overloaded to make checking for threats on theoretical grids easier.
    public boolean threatened(int rank, int file, boolean color) {
        return threatened(grid, rank, file, color);
    }

    //My favorite function
    public boolean threatened(int[][] board, int rank, int file, boolean color) {
        int checkRank, checkFile, threatener;
        //Checking for rooks, bishops, queens, and kings
        //These two for loops check in each of the 8 direcions for the line-moving pieces.
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0) {
                    continue;
                } //Continue if both directional differences are 0 (on original space)
                for (int dist = 1; dist < 8; dist++) {
                    checkRank = rank + dist * i;
                    checkFile = file + dist * j;
                    if (checkFile < 8 && checkFile >= 0 && checkRank < 8 && checkRank >= 0) { // Test for out of bounds
                        int space = board[checkRank][checkFile];
                        if (space != -1) { //If the space is not empty
                            int type = space % 8; //Get the type of piece
                            if ((space / 8 == 1) != color //If they are of different colors
                                    && (((i == 0 || j == 0) && (type == 4 || type == 3))
                                    //One directional difference is 0, so only a queen or rook can threaten
                                    || (i != 0 && j != 0 && (type == 4 || type == 2))
                                    //Both directional differences are not 0, so only a queen or bishop can threaten.
                                    || (dist == 1 && type == 5))) {
                                //Distance is 1, so a king can threaten.
                                return true;
                            } else {
                                break; //Break if in this direction a piece is encountered, but not one that can threaten.
                            }
                        }
                    } else {
                        break; //Break if this direction is out of bounds
                    }
                }
            }
        }
        //Checking for knights
        threatener = color ? 1 : 9;
        for (int i = 0; i < 8; i++) {
            checkFile = file + knightFile[i];
            checkRank = rank + knightRank[i];
            if (checkFile < 8 && checkFile >= 0 && checkRank < 8 && checkRank >= 0
                    && board[checkRank][checkFile] == threatener) {
                return true;
            }
        }
        //Checking for pawns
        threatener = color ? 0 : 8;
        int dir = color ? 1 : -1; //Look 1 space forward if the piece is black, 1 space behind if white.
        checkRank = rank + dir;
        if (checkRank < 8 && checkRank >= 0) {
            for (int i = -1; i <= 1; i += 2) {
                checkFile = file + i;
                if (checkFile < 8 && checkFile >= 0
                        && board[checkRank][checkFile] == threatener) {
                    return true;
                }
            }
        }
        return false;
    }

    //This function can take an optional 5th paramter, representing a promotion.
    public void move(int startRank, int startFile, int endRank, int endFile) {
        move(startRank, startFile, endRank, endFile, '\0');
    }

    public void move(int startRank, int startFile, int endRank, int endFile, char promotion) {
        //This function assumes the move is legal. Calling it with illegal arguments will have unintended results.

        int type = grid[startRank][startFile] % 8;
        //En passant capturing
        if (type == 0 && startFile != endFile && grid[endRank][endFile] == -1) { //Pawn is moving diagonally to empty space
            int enPassantRank = grid[startRank][startFile] / 8 == 1 ? 4 : 3;
            grid[enPassantRank][enPassantFile] = -1;
        }

        if (type == 0 && Math.abs(endRank - startRank) == 2) { //If a pawn has advanced 2 spaces
            enPassantFile = startFile; //Mark that space for en passant capturing.
        } else {
            enPassantFile = -1;
        }

        //Castling
        if (type == 5 && Math.abs(endFile - startFile) == 2) {
            boolean side = endFile - 4 == 2; //true means kingside
            int rookStartFile = side ? 7 : 0;
            int rookEndFile = side ? 5 : 3;
            grid[startRank][rookEndFile] = grid[startRank][rookStartFile]; //Moving the rook
            grid[startRank][rookStartFile] = -1;
        }

        if (whiteQueenCastle && ((startRank == 7 && (startFile == 0 || startFile == 4)) //If the white king or queenside rook moves
                || endRank == 7 && endFile == 0)) { //or the white queenside rook is captured
            whiteQueenCastle = false; //no queenside castling for white
        }

        if (blackQueenCastle && ((startRank == 0 && (startFile == 0 || startFile == 4))
                || endRank == 0 && endFile == 0)) {
            blackQueenCastle = false;
        }

        if (whiteKingCastle && ((startRank == 7 && (startFile == 7 || startFile == 4))
                || endRank == 7 && endFile == 7)) {
            whiteKingCastle = false;
        }

        if (blackKingCastle && ((startRank == 0 && (startFile == 7 || startFile == 4))
                || endRank == 0 && endFile == 7)) {
            blackKingCastle = false;
        }

        if (promotion == '\0') {
            grid[endRank][endFile] = grid[startRank][startFile]; //End space becomes contents of start space
        } else {
            int piece = PIECES.indexOf(Character.toLowerCase(promotion));
            grid[endRank][endFile] = (grid[startRank][startFile] / 8 == 1) ? piece + 8 : piece;
        }
        grid[startRank][startFile] = -1; //Start space becomes empty

        //Update the location of the kings if they have moved.
        if (startRank == whiteKingRank && startFile == whiteKingFile) {
            whiteKingRank = endRank;
            whiteKingFile = endFile;
        } else if (startRank == blackKingRank && startFile == blackKingFile) {
            blackKingRank = endRank;
            blackKingFile = endFile;
        }

        turn = !turn; //Other player's turn
    }

    //Promotion?
    public int[][] theoreticalMove(int startRank, int startFile, int endRank, int endFile) {
        //This function is similar to move(), but doesn't update the board or any of the data.
        //It simply returns a grid of what the board would look like if this move would happen.
        //Again, it assumes the move is legal.

        int[][] board = new int[8][];
        for (int i = 0; i < 8; i++) {
            board[i] = grid[i].clone();
        }

        int type = board[startRank][startFile] % 8;
        //En passant
        if (type == 0 && startFile != endFile && board[endRank][endFile] == -1) {
            int enPassantRank = board[startRank][startFile] / 8 == 1 ? 4 : 3;
            board[enPassantRank][enPassantFile] = -1;
        }

        //Castling
        if (type == 5 && Math.abs(endFile - startFile) == 2) {
            boolean side = endFile - 4 == 2; //true means kingside
            int rookStartFile = side ? 7 : 0;
            int rookEndFile = side ? 5 : 3;
            board[startRank][rookEndFile] = board[startRank][rookStartFile]; //Moving the rook
            board[startRank][rookStartFile] = -1;
        }

        //No need to implement promotion because of how this function is used.

        board[endRank][endFile] = board[startRank][startFile];
        board[startRank][startFile] = -1;
        return board;
    }

    public boolean hasLegalMove(boolean color) {
        /*
         * This function is weird.  It just uses legalMove on sets of coordinates
         * that the piece could theoretically go to, without checking if they 
         * are legal, occupied, out of bounds, or anything, and passes them to 
         * legalMove. If it finds any move that is legal, it stops and returns true.
         * For the pieces that move in lines, it differentiates between moves 
         * that are valid (that are legal movements) and those that are legal 
         */

        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {

                //Pawns
                if (grid[rank][file] != -1 && (grid[rank][file] / 8 == 1) == color) {
                    int type = grid[rank][file] % 8;
                    if (type == 0) {
                        int dir = color ? 1 : -1;
                        //Up one space
                        if (legalMove(rank, file, rank + dir, file)) {
                            return true;
                        }
                        //Up two spaces
                        if (legalMove(rank, file, rank + 2 * dir, file)) {
                            return true;
                        }
                        //Diagonally one way
                        if (legalMove(rank, file, rank + dir, file + 1)) {
                            return true;
                        }
                        //Diagonally the other way
                        if (legalMove(rank, file, rank + dir, file - 1)) {
                            return true;
                        }
                    } else if (type == 1) { //Knights
                        for (int i = 0; i < 8; i++) {
                            if (legalMove(rank, file, rank + knightRank[i], file + knightFile[i])) {
                                return true;
                            }
                        }
                    } else if (type == 5) { //King (No need to check for castling)
                        for (int i = -1; i <= 1; i++) {
                            for (int j = -1; j <= 1; j++) {
                                if (legalMove(rank, file, rank + i, file + j)) {
                                    return true;
                                }
                            }
                        }
                    } else { //Line-moving pieces
                        for (int i = -1; i <= 1; i++) {
                            for (int j = -1; j <= 1; j++) {
                                for (int dist = 1; dist < 8; dist++) {
                                    if (validMove(rank, file, rank + (i * dist), file + (j * dist))) {
                                        if (!putInCheck(rank, file, rank + (i * dist), file + (j * dist))) {
                                            return true;
                                        }
                                    } else {
                                        break;
                                        /*Break out of searching in this direction 
                                         * if it was out of bounds, invalid movement,
                                         * or a piece was in the way.  If the only 
                                         * reason it was illegal was because it put
                                         * you in check, though, keep going in that 
                                         * direction, because there might still
                                         * be a legal one.
                                         */
                                    } //End of legalmove check
                                } //End of directional search
                            } //End of file direction switching 
                        } //End of rank direction switching
                    } //End of line-traveling piece move generation
                } //End of is-color check
            } //End of file search
        } //End of rank search
        //This is really rough.
        return false;
    }

    //Just a combination of validMove and putInCheck
    public boolean legalMove(int startRank, int startFile, int endRank, int endFile) {
        return validMove(startRank, startFile, endRank, endFile)
                && !putInCheck(startRank, startFile, endRank, endFile);
    }

    //Check if this piece is allowed to move here (don't test for putting in check)
    //This is the complicated and boring part
    public boolean validMove(int startRank, int startFile, int endRank, int endFile) {

        if (startRank >= 8 || startRank < 0 || startFile >= 8 || startFile < 0
                || endRank >= 8 || endRank < 0 || endFile >= 8 || endFile < 0) {
            return false;
        } //Check for out of bounds

        int rankDist = Math.abs(endRank - startRank);
        int fileDist = Math.abs(endFile - startFile);

        if (rankDist == 0 && fileDist == 0) {
            return false;
        } //No matter what, a piece has to move at least one space
        int type = grid[startRank][startFile] % 8;

        if (type == -1) {
            return false;
        } //Make sure there is a piece here
        boolean color = grid[startRank][startFile] / 8 == 1;

        if (type == 0) { //Type is pawn
            int dir = color ? 1 : -1;
            //DOUBLE FORWARD MOVEMENT
            if (!((startRank == (color ? 1 : 6) //Pawn is at starting rank
                    && startRank + dir * 2 == endRank //Pawn is moving 2 spaces forward
                    && startFile == endFile //Pawn is not moving across files
                    && grid[startRank + dir][startFile] == -1 //Intermediate space is free
                    && grid[endRank][endFile] == -1) //Ending space is free
                    //NORMAL FORWARD MOVEMENT
                    || (startRank + dir == endRank //OR Pawn is moving 1 space forward
                    && fileDist == 0 //Pawn is not moving across ranks
                    && grid[endRank][endFile] == -1) //Ending space is free
                    //NORMAL CAPTURING
                    || (startRank + dir == endRank //Pawn is moving 1 space forward
                    && fileDist == 1 //Pawn is moving 1 space left or right
                    && (grid[endRank][endFile] != -1 //Ending space is occupied
                    && (grid[endRank][endFile] / 8 == 1) != color)) //Ending space is occupied by enemy
                    //EN PASSANT CAPTURING
                    || (grid[endRank][endFile] == -1 //OR ending space is empty
                    && enPassantFile != -1 // and last move was a double pawn move
                    && endFile == enPassantFile // and this pawn is moving to the same file
                    && endRank == (color ? 5 : 2)))) { //and it's moving to the rank that's two spaces from the opponent's side
                return false;
            }
        } else if (type == 1) {
            if (!(((rankDist == 2 && fileDist == 1) || (rankDist == 1 && fileDist == 2))
                    && (grid[endRank][endFile] == -1 || (grid[endRank][endFile] / 8 == 1) != color))) {
                return false;
            }
        } else if (type == 5) {

            //CASTLING
            if (startFile == 4 //King's file
                    && ((startRank == 0 && endRank == 0 && color //Black king's rank
                    && ((blackQueenCastle && endFile == 2) || (blackKingCastle && endFile == 6))) //And that side is okay
                    || (startRank == 7 && endRank == 7 && !color //White king's rank
                    && ((whiteQueenCastle && endFile == 2) || (whiteKingCastle && endFile == 6))))) { //And that side is okay
                int dir = Integer.signum(endFile - 4); //Direction king will move to castle
                int rookDist = (dir == 1) ? 3 : 4; // 3 spaces between the king and rook kingside, 4 queenside
                for (int i = 1; i < rookDist; i++) { //Checking every space between king and rook
                    if (grid[startRank][startFile + i * dir] != -1) {
                        return false;
                    }
                }
                for (int i = 0; i < 3; i++) { //Checks all spaces the king moves on for threats
                    if (threatened(startRank, startFile + i * dir, color)) {
                        return false;
                    }
                }

                return true;
            }

            //NORMAL KING MOVEMENT
            if (!(fileDist <= 1 && rankDist <= 1
                    && (grid[endRank][endFile] == -1
                    || (grid[endRank][endFile] / 8 == 1) != color))) {
                return false;
            }
        } else { //Type is bishop, rook, or queen
            if (!((rankDist == fileDist && (type == 2 || type == 4)) //Rank and file movement are equal and it's a bishop or queen
                    || ((rankDist == 0 || fileDist == 0) && (type == 3 || type == 4))
                    && (grid[endRank][endFile] == -1 //Ending space is empty
                    || (grid[endRank][endFile] / 8 == 1) != color))) { //Or one is 0 and it's a rook or queen
                return false;
            }
            int rankSign = Integer.signum(endRank - startRank);
            int fileSign = Integer.signum(endFile - startFile);
            int dist = Math.max(rankDist, fileDist);
            for (int i = 1; i < dist; i++) {
                if (grid[startRank + rankSign * i][startFile + fileSign * i] != -1) {
                    return false; //Check each space between starting and ending (exclusive) for emptiness
                }
            }
        }
        return true;
    }

    //Uses theoreticalMove to see if a move would put you in check.
    public boolean putInCheck(int startRank, int startFile, int endRank, int endFile) {
        int[][] theoreticalGrid = theoreticalMove(startRank, startFile, endRank, endFile);
        boolean color = grid[startRank][startFile] / 8 == 1;

        int kingRank = color ? blackKingRank : whiteKingRank;
        int kingFile = color ? blackKingFile : whiteKingFile;

        //Reassign the king position if we're moving the king
        if (kingRank == startRank && kingFile == startFile) {
            kingRank = endRank;
            kingFile = endFile;
        }

        if (threatened(theoreticalGrid, kingRank, kingFile, color)) {
            return true;
        }
        return false;
    }

    public int checkmate(boolean color) {
        if (!hasLegalMove(color)) {
            if (inCheck(color)) {
                return 1; //Checkmated
            } else {
                return -1; //Stalemated
            }
        } else {
            return 0; //Neither
        }
    }

    public boolean inCheck(boolean color) {
        int kingRank = color ? blackKingRank : whiteKingRank;
        int kingFile = color ? blackKingFile : whiteKingFile;
        return threatened(kingRank, kingFile, color);
    }
}
