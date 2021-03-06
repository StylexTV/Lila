package de.lila.command;

import java.util.HashMap;

import de.lila.game.Board;
import de.lila.main.Main;

public class DisplayCommand extends UCICommand {
	
	public DisplayCommand() {
		super("d");
	}
	
	@Override
	public void execute(HashMap<String, String> args) {
		Board b = Main.getController().getBoard();
		
		b.print();
	}
	
}
