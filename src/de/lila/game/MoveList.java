package de.lila.game;

public class MoveList {
	
	private Move[] moves;
	
	private int count;
	
	private int picked;
	
	public MoveList() {
		moves = new Move[BoardConstants.MAX_POSSIBLE_MOVES];
	}
	
	public Move[] getMoves() {
		return moves;
	}
	
	public int getCount() {
		return count;
	}
	
	public void setMoves(Move[] moves) {
		this.moves = moves;
	}
	
	public void setCount(int count) {
		this.count = count;
	}
	
	public void addMove(int from, int to, int captured, int promoted, int flag) {
		addMove(new Move(from, to, captured, promoted, flag));
	}
	
	public void addMove(Move m) {
		moves[count] = m;
		count++;
	}
	
	public Move getMove(int i) {
		return moves[i];
	}
	
	public void setMove(Move move, int i) {
		moves[i] = move;
	}
	
	public boolean hasMovesLeft() {
		return picked < count;
	}
	
	public void applyMoveScore(Move move, int score) {
		int hash = move.getHash();
		
		for(int i=0; i<count; i++) {
			Move m = moves[i];
			
			if(m.getHash() == hash) {
				if(m.getScore() < score) m.setScore(score);
				
				break;
			}
		}
	}
	
	public void reset() {
		picked = 0;
		
		for(int i=0; i<count; i++) {
			Move m = moves[i];
			
			m.setPicked(false);
		}
	}
	
	public Move next() {
		picked++;
		
		Move best = null;
		
		for(int i=0; i<count; i++) {
			Move m = moves[i];
			
			if(!m.wasPicked()) {
				if(best == null || m.getScore() > best.getScore()) {
					best = m;
				}
			}
		}
		
		best.setPicked(true);
		
		return best;
	}
	
}
