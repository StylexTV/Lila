package de.chess.ai;

import de.chess.game.Move;

public class TranspositionEntry {
	
	public static final int TYPE_EXACT = 0;
	public static final int TYPE_LOWER_BOUND = 1;
	public static final int TYPE_UPPER_BOUND = 2;
	
	private long key;
	
	private int depth;
	
	private Move m;
	
	private int type;
	
	private int score;
	
	private int age;
	
	public TranspositionEntry(long key, int depth, int plyFromRoot, Move m, int type, int score, int age) {
		this.key = key;
		this.depth = depth;
		this.m = m;
		this.type = type;
		this.score = correctMateScore(score, plyFromRoot);
		this.age = age;
	}
	
	public long getPositionKey() {
		return key;
	}
	
	public int getDepth() {
		return depth;
	}
	
	public Move getMove() {
		return m;
	}
	
	public int getType() {
		return type;
	}
	
	public int getScore(int plyFromRoot) {
		return readjustMateScore(score, plyFromRoot);
	}
	
	public int getAge() {
		return age;
	}
	
	private static int correctMateScore(int score, int plyFromRoot) {
		if(Search.isMateScore(score)) {
			int sign = 0;
			
			if(score > 0) sign = 1;
			else if(score < 0) sign = -1;
			
			return (score * sign + plyFromRoot) * sign;
		}
		
		return score;
	}
	
	private static int readjustMateScore(int score, int plyFromRoot) {
		if(Search.isMateScore(score)) {
			int sign = 0;
			
			if(score > 0) sign = 1;
			else if(score < 0) sign = -1;
			
			return (score * sign - plyFromRoot) * sign;
		}
		
		return score;
	}
	
}
