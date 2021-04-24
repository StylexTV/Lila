package de.lila.uci;

import de.lila.ai.Search;
import de.lila.game.Board;
import de.lila.main.Main;

public class GoCommand extends UCICommand {
	
	public GoCommand() {
		super("go");
	}
	
	@Override
	public void execute(String[] args) {
		Board b = Main.getController().getBoard();
		
		int i = 1;
		
		if(args.length <= i) return;
		
		String type = args[i];
		
		i++;
		
		if(type.equalsIgnoreCase("depth")) {
			int depth = Integer.parseInt(args[i]);
			
			Search.findBestMove(b, 0, depth);
		}
	}
	
}