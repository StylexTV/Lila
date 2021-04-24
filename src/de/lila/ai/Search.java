package de.lila.ai;

import de.lila.game.Board;
import de.lila.game.Move;
import de.lila.game.MoveGenerator;
import de.lila.game.MoveList;
import de.lila.game.Winner;

public class Search {
	
	private static final int INFINITY = 1000000;
	
	private static final int MATE_SCORE = 100000;
	
	private static final int WINDOW_SIZE = Evaluator.GENERIC_PAWN_VALUE;
	
	private static final int NULL_MOVE_REDUCTION = 2;
	
	private static int allocatedTime;
	
	private static int minSearchDepth;
	
	private static Move responseMove;
	
	private static int maxDepth;
	
	private static long visitedNodes;
	
	public static void findBestMove(Board b, int time, int depth) {
		long before = System.currentTimeMillis();
		
		allocatedTime = time;
		minSearchDepth = depth;
		
		maxDepth = 0;
		visitedNodes = 0;
		
		int score = 0;
		
		int currentDepth = 1;
		
		while(currentDepth <= minSearchDepth || System.currentTimeMillis() - before < allocatedTime) {
			score = startSearch(b, currentDepth, score);
			
			String scoreString = convertScoreToString(score);
			
			int ms = (int) (System.currentTimeMillis() - before);
			
			int nps = (int) (visitedNodes / (ms / 1000f));
			
			System.out.println("info depth " + currentDepth + " seldepth " + maxDepth + " score " + scoreString + " nodes " + visitedNodes + " nps " + nps + " time " + ms);
			
			currentDepth++;
		}
		
		System.out.println("bestmove " + responseMove.getAlgebraicNotation());
	}
	
	private static int startSearch(Board b, int depth, int lastScore) {
		// Aspiration windows
		
		if(depth != 1) {
			int alpha = lastScore - WINDOW_SIZE;
			int beta = lastScore + WINDOW_SIZE;
			
			int score = runAlphaBeta(b, alpha, beta, depth);
			
			if(score > alpha && score < beta) {
				return score;
			}
		}
		
		int score = runAlphaBeta(b, -INFINITY, INFINITY, depth);
		
		return score;
	}
	
	private static int runAlphaBeta(Board b, int alpha, int beta, int depth) {
		visitedNodes++;
		
		int type = TranspositionEntry.TYPE_UPPER_BOUND;
		
		MoveList list = new MoveList();
		
		MoveGenerator.generateAllMoves(b, list);
		
		MoveEvaluator.eval(list, b);
		
		TranspositionEntry entry = TranspositionTable.getEntry(b.getPositionKey());
		
		if(entry != null && entry.getMove() != null) {
			list.applyMoveScore(entry.getMove(), MoveEvaluator.HASH_MOVE_SCORE);
		}
		
		applyKillerMoves(list, b.getHistoryPly());
		
		Move bestMove = null;
		
		while(list.hasMovesLeft()) {
			Move m = list.next();
			
			b.makeMove(m);
			
			if(!b.isOpponentInCheck()) {
				int score = -alphaBeta(b, 1, -beta, -alpha, depth - 1, true);
				
				if(score > alpha) {
					bestMove = m;
					alpha = score;
					
					type = TranspositionEntry.TYPE_EXACT;
				}
			}
			
			b.undoMove(m);
		}
		
		if(alpha >= beta) type = TranspositionEntry.TYPE_LOWER_BOUND;
		
		TranspositionTable.putEntry(b.getPositionKey(), depth, 0, bestMove, type, alpha, b.getHistoryPly());
		
		responseMove = bestMove;
		
		return alpha;
	}
	
	private static int alphaBeta(Board b, int plyFromRoot, int alpha, int beta, int depth, boolean allowNullMove) {
		if(depth <= 0) {
			return quiesce(b, plyFromRoot, alpha, beta);
		}
		
		if(plyFromRoot + 1 > maxDepth) {
			maxDepth = plyFromRoot + 1;
		}
		
		visitedNodes++;
		
		if(b.getFiftyMoveCounter() == 100 || b.hasThreefoldRepetition()) return 0;
		
		// TT lookup
		
		TranspositionEntry entry = TranspositionTable.getEntry(b.getPositionKey());
		
		if(entry != null && entry.getDepth() >= depth) {
			int entryScore = entry.getScore(plyFromRoot);
			
			if(entry.getType() == TranspositionEntry.TYPE_EXACT) return entryScore;
			else if(entry.getType() == TranspositionEntry.TYPE_LOWER_BOUND) alpha = Math.max(alpha, entryScore);
			else beta = Math.min(beta, entryScore);
			
			if(alpha >= beta) return entryScore;
		}
		
		int type = TranspositionEntry.TYPE_UPPER_BOUND;
		
		int newDepth = depth - 1;
		
		// Null move
		
		boolean inCheck = b.isSideInCheck();
		
		if(allowNullMove && !inCheck) {
			b.makeNullMove();
			
			int score = -alphaBeta(b, plyFromRoot + 1, -beta, -beta + 1, newDepth - NULL_MOVE_REDUCTION, false);
			
			b.undoNullMove();
			
			if(score >= beta) {
				return score;
			}
		}
		
		// Ordinary search
		
		MoveList list = new MoveList();
		
		MoveGenerator.generateAllMoves(b, list);
		
		MoveEvaluator.eval(list, b);
		
		if(entry != null && entry.getMove() != null) {
			list.applyMoveScore(entry.getMove(), MoveEvaluator.HASH_MOVE_SCORE);
		}
		
		applyKillerMoves(list, b.getHistoryPly());
		
		boolean hasLegalMove = false;
		
		Move bestMove = null;
		int bestScore = Integer.MIN_VALUE;
		
		int moveCount = 0;
		
		while(list.hasMovesLeft()) {
			Move m = list.next();
			
			b.makeMove(m);
			
			if(!b.isOpponentInCheck()) {
				hasLegalMove = true;
				
				moveCount++;
				
				int score = 0;
				
				boolean doFullDepthSearch = true;
				
				boolean captureOrPromotion = m.getCaptured() != 0 || m.getPromoted() != 0;
				
				// Late move reduction
				
				if(moveCount > 1) {
					
					if(depth > 2 && !inCheck && !captureOrPromotion && !b.isSideInCheck()) {
						int r = 1;
						
						int d = newDepth - r;
						
						if(d < 1) d = 1;
						else if(d > newDepth) d = newDepth;
						
						score = -alphaBeta(b, plyFromRoot + 1, -beta, -alpha, d, true);
						
						doFullDepthSearch = score > alpha && d < newDepth;
					}
				}
				
				if(doFullDepthSearch) {
					score = -alphaBeta(b, plyFromRoot + 1, -beta, -alpha, newDepth, true);
				}
				
				if(score > bestScore) {
					bestMove = m;
					bestScore = score;
				}
				
				if(score > alpha) {
					alpha = score;
					
					type = TranspositionEntry.TYPE_EXACT;
				}
			}
			
			b.undoMove(m);
			
			if(alpha >= beta) {
				KillerTable.storeMove(m, b.getHistoryPly());
				
				TranspositionTable.putEntry(b.getPositionKey(), depth, plyFromRoot, bestMove, TranspositionEntry.TYPE_LOWER_BOUND, beta, b.getHistoryPly());
				
				return alpha;
			}
		}
		
		if(!hasLegalMove) {
			int winner = b.findWinner(false);
			
			int score;
			
			if(winner == Winner.DRAW) {
				score = 0;
			} else {
				int i = MATE_SCORE - plyFromRoot;
				
				score = b.getSide() == winner ? i : -i;
			}
			
			TranspositionTable.putEntry(b.getPositionKey(), depth, plyFromRoot, null, TranspositionEntry.TYPE_EXACT, score, b.getHistoryPly());
			
			return score;
		}
		
		TranspositionTable.putEntry(b.getPositionKey(), depth, plyFromRoot, bestMove, type, alpha, b.getHistoryPly());
		
		return alpha;
	}
	
	private static int quiesce(Board b, int plyFromRoot, int alpha, int beta) {
		if(plyFromRoot + 1 > maxDepth) {
			maxDepth = plyFromRoot + 1;
		}
		
		visitedNodes++;
		
		if(b.getFiftyMoveCounter() == 100 || b.hasThreefoldRepetition()) return 0;
		
		boolean inCheck = b.isSideInCheck();
		
		if(!inCheck) {
			int evalScore = Evaluator.eval(b, b.getSide());
			
			if(evalScore >= beta) return beta;
			
			if(evalScore > alpha) alpha = evalScore;
		}
		
		MoveList list = new MoveList();
		
		MoveGenerator.generateAllMoves(b, list);
		
		MoveEvaluator.eval(list, b);
		
		applyKillerMoves(list, b.getHistoryPly());
		
		boolean hasLegalMove = false;
		
		while(list.hasMovesLeft()) {
			Move m = list.next();
			
			b.makeMove(m);
			
			int score = 0;
			boolean hasDoneMove = false;
			
			if(!b.isOpponentInCheck()) {
				hasLegalMove = true;
				
				if(inCheck || m.getCaptured() != 0 || m.getPromoted() != 0) {
					hasDoneMove = true;
					
					score = -quiesce(b, plyFromRoot + 1, -beta, -alpha);
					
					if(score > alpha) {
						alpha = score;
					}
				}
			}
			
			b.undoMove(m);
			
			if(hasDoneMove && score >= beta) {
				KillerTable.storeMove(m, b.getHistoryPly());
				
				return beta;
			}
		}
		
		if(!hasLegalMove) {
			int winner = b.findWinner(false);
			
			int score;
			
			if(winner == Winner.DRAW) {
				score = 0;
			} else {
				int i = MATE_SCORE - plyFromRoot;
				
				score = b.getSide() == winner ? i : -i;
			}
			
			return score;
		}
		
		return alpha;
	}
	
	private static void applyKillerMoves(MoveList list, int ply) {
		for(int i=0; i<KillerTable.SIZE; i++) {
			Move killer = KillerTable.getMove(ply, i);
			
			if(killer != null) list.applyMoveScore(killer, MoveEvaluator.KILLER_MOVE_SCORE);
		}
	}
	
	public static boolean isMateScore(int score) {
		int maxDepth = 1000;
		
		return Math.abs(score) > MATE_SCORE - maxDepth;
	}
	
	private static String convertScoreToString(int score) {
		if(isMateScore(score)) {
			int moves;
			
			if(score > 0) {
				moves = (99999 - score) / 2 + 1;
			} else {
				moves = (score + 99998) / 2 + 1;
				
				moves = -moves;
			}
			
			return "mate " + moves;
		}
		
		float pawns = (float) score / Evaluator.GENERIC_PAWN_VALUE;
		
		return "cp " + Math.round(pawns * 100);
	}
	
}