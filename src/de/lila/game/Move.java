package de.lila.game;

public class Move {
	
	private int from;
	private int to;
	
	private int captured;
	private int promoted;
	
	private int flag;
	
	private int hash;
	
	private String notation;
	
	private int score;
	
	private boolean picked = false;
	
	public Move(int from, int to, int captured, int promoted, int flag) {
		this.from = from;
		this.to = to;
		
		this.captured = captured;
		this.promoted = promoted;
		
		this.flag = flag;
		
		this.hash = from | (to << 7) | (promoted << 14);
		
		initAlgebraicNotation();
	}
	
	private void initAlgebraicNotation() {
		notation = BoardSquare.getNotation(from) + BoardSquare.getNotation(to);
		
		if(promoted != 0) {
			notation = notation + Piece.getFenNotation(promoted - Piece.PAWN + 6);
		}
	}
	
	public int getFrom() {
		return from;
	}
	
	public int getTo() {
		return to;
	}
	
	public int getCaptured() {
		return captured;
	}
	
	public int getPromoted() {
		return promoted;
	}
	
	public int getFlag() {
		return flag;
	}
	
	public int getHash() {
		return hash;
	}
	
	public String getAlgebraicNotation() {
		return notation;
	}
	
	public int getScore() {
		return score;
	}
	
	public void setScore(int score) {
		this.score = score;
	}
	
	public boolean wasPicked() {
		return picked;
	}
	
	public void setPicked(boolean picked) {
		this.picked = picked;
	}
	
	@Override
	public String toString() {
		return "Move[to = "+to+", from = "+from+", captured = "+captured+", promoted = "+promoted+", flag = "+flag+", hash = "+hash+", score = "+score+"]";
	}
	
}