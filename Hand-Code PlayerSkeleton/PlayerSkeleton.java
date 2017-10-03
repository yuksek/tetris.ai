
import java.util.Arrays;


public class PlayerSkeleton {

	private double[] weights = {-3.2236852403345377, 
		                        -1.2965051412591784, 
		                        -7.679478343912685, 
		                        -9.848146996425509, 
		                        -9.051635848238169, 
		                        -4.860969092657845, 
		                        -1.303199700058622, 
		                         3.644186031593467, 
		                        -7.845594932631544, 
		                        -9.32712230879736};

	public int pickMove(State s, int[][] legalMoves) {

		SimulatedState ss = new SimulatedState(s);

		double maxVal = getValue(ss,0);
		int maxMove = 0;

		for (int i = 1; i < legalMoves.length; i++){
			double currVal = getValue(ss,i);
			if (currVal > maxVal){
				maxVal = currVal;
				maxMove = i;
			}

		}
		
		return maxMove;
	}

	public double getValue(SimulatedState ss, int action){
		int[] features = ss.tryMove(action);
		return getValue(features);
	}

	public double getValue(int[] features){
		if (features == null){
			return Double.NEGATIVE_INFINITY;
		}
		double result = 0;
		for (int i = 0; i < weights.length; i++){
			result += weights[i]*features[i];
		}
		return result;
	}
	
	public static void main(String[] args) {
		
		State s = new State();
		new TFrame(s);
		PlayerSkeleton p = new PlayerSkeleton();
		while(!s.hasLost()) {
			s.makeMove(p.pickMove(s,s.legalMoves()));
			s.draw();
			s.drawNext(0,0);
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("You have completed "+s.getRowsCleared()+" rows.");
	}
	
}

class SimulatedState {
	public static final int COLS = 10;
	public static final int ROWS = 21;
	public static final int N_PIECES = 7;

	public boolean lost;

	private int turn;
	private int cleared;

	private int[][] field;
	private int[][] trialField = new int[ROWS][COLS];

	private int[] top;
	private int[] trialTop = new int[COLS];
	
	protected int nextPiece;
	
	protected static int[][] legalMoves;
	
	//indices for legalMoves
	public static final int ORIENT = 0;
	public static final int SLOT = 1;
	
	//possible orientations for a given piece type
	protected static int[] pOrients = {1,2,4,4,4,2,2};
	
	//the next several arrays define the piece vocabulary in detail
	//width of the pieces [piece ID][orientation]
	protected static int[][] pWidth = {
			{2},
			{1,4},
			{2,3,2,3},
			{2,3,2,3},
			{2,3,2,3},
			{3,2},
			{3,2}
	};
	//height of the pieces [piece ID][orientation]
	private static int[][] pHeight = {
			{2},
			{4,1},
			{3,2,3,2},
			{3,2,3,2},
			{3,2,3,2},
			{2,3},
			{2,3}
	};
	private static int[][][] pBottom = {
		{{0,0}},
		{{0},{0,0,0,0}},
		{{0,0},{0,1,1},{2,0},{0,0,0}},
		{{0,0},{0,0,0},{0,2},{1,1,0}},
		{{0,1},{1,0,1},{1,0},{0,0,0}},
		{{0,0,1},{1,0}},
		{{1,0,0},{0,1}}
	};
	private static int[][][] pTop = {
		{{2,2}},
		{{4},{1,1,1,1}},
		{{3,1},{2,2,2},{3,3},{1,1,2}},
		{{1,3},{2,1,1},{3,3},{2,2,2}},
		{{3,2},{2,2,2},{2,3},{1,2,1}},
		{{1,2,2},{3,2}},
		{{2,2,1},{2,3}}
	};

	//constructor
	public SimulatedState(State s) {
		lost = s.hasLost();
		turn = s.getTurnNumber();
		cleared = s.getRowsCleared();
		field = s.getField();
		top = s.getTop();
		nextPiece = s.getNextPiece();
		legalMoves = s.legalMoves();
	}

	public int[] tryMove(int move) {
		return tryMove(legalMoves[move]);
	}

	public int[] tryMove(int[] move) {
		return tryMove(move[ORIENT],move[SLOT]);
	}

	public int[] tryMove(int orient, int slot) {

		int[] features = new int[10];

		int trialTurn = turn + 1;

		for (int i=0; i<ROWS; i++){
			for (int j=0; j<COLS; j++){
				trialField[i][j] = field[i][j];
 			}
		}

		for (int i=0; i<COLS; i++){
			trialTop[i] = top[i];
		}
		
		//height if the first column makes contact
		int contactCol = slot;
		int height = trialTop[slot]-pBottom[nextPiece][orient][0];
		//for each column beyond the first in the piece
		for(int c = 1; c < pWidth[nextPiece][orient];c++) {
			if (trialTop[slot+c]-pBottom[nextPiece][orient][c] > height){
				contactCol = slot+c;
				height = trialTop[slot+c]-pBottom[nextPiece][orient][c];
			}
		}
		features[0] = trialTop[contactCol];

		//check if game ended
		if(height+pHeight[nextPiece][orient] >= ROWS) {
			return null;
		}

		
		//for each column in the piece - fill in the appropriate blocks
		for(int i = 0; i < pWidth[nextPiece][orient]; i++) {
			
			//from bottom to top of brick
			for(int h = height+pBottom[nextPiece][orient][i]; h < height+pTop[nextPiece][orient][i]; h++) {
				trialField[h][i+slot] = trialTurn;
			}
		}
		
		//adjust top
		for(int c = 0; c < pWidth[nextPiece][orient]; c++) {
			trialTop[slot+c]=height+pTop[nextPiece][orient][c];
		}
		
		int rowsCleared = 0;
		int pieceContribution = 0;
		
		//check for full rows - starting at the top
		for(int r = height+pHeight[nextPiece][orient]-1; r >= height; r--) {
			//check all columns in the row
			boolean full = true;
			int rowContribution = 0;
			for(int c = 0; c < COLS; c++) {
				if(trialField[r][c] == 0) {
					full = false;
					break;
				}
				if (trialField[r][c] == trialTurn){
					rowContribution ++;
				}
			}
			//if the row was full - remove it and slide above stuff down
			if(full) {
				rowsCleared++;
				pieceContribution += rowContribution;
				//for each column
				for(int c = 0; c < COLS; c++) {

					//slide down all bricks
					for(int i = r; i < trialTop[c]; i++) {
						trialField[i][c] = trialField[i+1][c];
					}
					//lower the top
					trialTop[c]--;
					while(trialTop[c]>=1 && trialField[trialTop[c]-1][c]==0)	trialTop[c]--;
				}
			}
		}

		features[1] = rowsCleared * pieceContribution;

		int rowTransition = 0;
		for (int r = 0; r < ROWS; r++){
			if (trialField[r][0] == 0){
				rowTransition++;
			}
			if (trialField[r][COLS-1] == 0){
				rowTransition++;
			}
			for (int c = 1; c < COLS; c++){
				if ((trialField[r][c] * trialField[r][c-1] == 0) && (trialField[r][c] + trialField[r][c-1] > 0)){
					rowTransition++;
				}
			}
		}
		features[2] = rowTransition;

		int colTransition = 0;
		for (int c = 0; c < COLS; c++){
			if (trialField[0][c] == 0){
				colTransition++;
			}
			if (trialField[ROWS-1][c] == 0){
				colTransition++;
			}
			for (int r = 1; r < ROWS; r++){
				if ((trialField[r][c] * trialField[r-1][c] == 0) && (trialField[r][c] + trialField[r-1][c] > 0)){
					colTransition++;
				}
			}
		}
		features[3] = colTransition;

		int numHoles = 0;
		for (int c = 0; c < COLS; c++){
			for (int r = 0; r < ROWS-1; r++){
				if (trialField[r][c] == 0 && trialField[r+1][c] > 0){
					numHoles++;
				}
			}
		}
		features[4] = numHoles;

		int cumWell = 0;
		for (int i = 1; i <= trialTop[1] - trialTop[0]; i++){
			cumWell += i;
		}
		for (int i = 1; i <= trialTop[COLS-2] - trialTop[COLS-1]; i++){
			cumWell += i;
		}
		for (int c = 1; c < COLS-1; c++){
			for (int i = 1; i <= Math.min(trialTop[c-1], trialTop[c+1]) - trialTop[c]; i++){
				cumWell += i;
			}
		}
		features[5] = cumWell;
	

		int sumHeight = 0;
		for (int c = 0; c < COLS; c++){
			sumHeight += trialTop[c];
		}
		features[6] = sumHeight;

		int sumDifference = 0;
		for (int c = 1; c < COLS; c++){
			sumDifference += Math.abs(trialTop[c] - trialTop[c-1]);
		}
		features[7] = sumDifference;

		int maxHeight = 0;
		for (int c = 0; c < COLS; c++){
			if (trialTop[c] > maxHeight){
				maxHeight = trialTop[c];
			}
		}
		features[8] = maxHeight;

		int numVeritcalHoles = 0;
		for (int c = 0; c < COLS; c++){
			for (int r = 0; r < trialTop[c]; r++){
				if (trialField[r][c] == 0){
					numVeritcalHoles ++;
				}
			}
		}
		features[9] = numVeritcalHoles;			

		return features;
	}

}






