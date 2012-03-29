/**
 * 
 */
package banker.frontend;

import banker.logic.Bankers;
import banker.logic.FIFO;
import banker.logic.Parser;
import banker.types.State;

/**
 * @author Sailee
 *
 */
public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Parser p = new Parser();		
		
		//process input from input file
		if(args.length > 0)
		{
			try 
			{				
				State currentState= p.parseInput(args[0]) ;
				State backup = p.parseInput(args[0]) ;

				if(currentState !=null)
				{					
					FIFO.perform(currentState);
					Bankers.perform(backup);
				}
				else
				{
					System.out.println("Invalid File Name. Please check the file path and re-run the program.");
				}
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}			
		}
		else
		{
			System.out.println("File name not specified.");
		}
	}

}
