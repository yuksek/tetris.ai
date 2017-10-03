
public class State {
	public static final int COLS = 10;
	public static final int ROWS = 21;
	public static final int N_PIECES = 7;


	public boolean lost = false;

	
	//current turn
	private int turn = 0;
	private int cleared = 0;
	
	//each square in the grid - int means empty - other values mean the turn it was placed
	private int[][] field = new int[ROWS][COLS];
	private int[][] trialField = new int[ROWS][COLS];
	//top row+1 of each column
	//0 means empty
	private int[] top = new int[COLS];
	private int[] trialTop = new int[COLS];
	
	//number of next piece
	protected int nextPiece;
	
	
	
	//all legal moves - first index is piece type - then a list of 2-length arrays
	protected static int[][][] legalMoves = new int[N_PIECES][][];
	
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
	
	//initialize legalMoves
	{
		//for each piece type
		for(int i = 0; i < N_PIECES; i++) {
			//figure number of legal moves
			int n = 0;
			for(int j = 0; j < pOrients[i]; j++) {
				//number of locations in this orientation
				n += COLS+1-pWidth[i][j];
			}
			//allocate space
			legalMoves[i] = new int[n][2];
			//for each orientation
			n = 0;
			for(int j = 0; j < pOrients[i]; j++) {
				//for each slot
				for(int k = 0; k < COLS+1-pWidth[i][j];k++) {
					legalMoves[i][n][ORIENT] = j;
					legalMoves[i][n][SLOT] = k;
					n++;
				}
			}
		}
	
	}
	
	
	public int[][] getField() {
		return field;
	}

	public int[] getTop() {
		return top;
	}

    public static int[] getpOrients() {
        return pOrients;
    }
    
    public static int[][] getpWidth() {
        return pWidth;
    }

    public static int[][] getpHeight() {
        return pHeight;
    }

    public static int[][][] getpBottom() {
        return pBottom;
    }

    public static int[][][] getpTop() {
        return pTop;
    }


	public int getNextPiece() {
		return nextPiece;
	}
	
	public boolean hasLost() {
		return lost;
	}
	
	public int getRowsCleared() {
		return cleared;
	}
	
	public int getTurnNumber() {
		return turn;
	}
	
	
	
	//constructor
	public State() {
		nextPiece = randomPiece();

	}
	
	//random integer, returns 0-6
	private int randomPiece() {
		return (int)(Math.random()*N_PIECES);
	}
	


	
	//gives legal moves for 
	public int[][] legalMoves() {
		return legalMoves[nextPiece];
	}
	
	//make a move based on the move index - its order in the legalMoves list
	public int makeMove(int move) {
		return makeMove(legalMoves[nextPiece][move]);
	}

	public int[] tryMove(int move) {
		return tryMove(legalMoves[nextPiece][move]);
	}
	
	//make a move based on an array of orient and slot
	public int makeMove(int[] move) {
		return makeMove(move[ORIENT],move[SLOT]);
	}

	public int[] tryMove(int[] move) {
		return tryMove(move[ORIENT],move[SLOT]);
	}
	
	//returns false if you lose - true otherwise
	public int makeMove(int orient, int slot) {
		turn++;
		//height if the first column makes contact
		int height = top[slot]-pBottom[nextPiece][orient][0];
		//for each column beyond the first in the piece
		for(int c = 1; c < pWidth[nextPiece][orient];c++) {
			height = Math.max(height,top[slot+c]-pBottom[nextPiece][orient][c]);
		}
		
		//check if game ended
		if(height+pHeight[nextPiece][orient] >= ROWS) {
			lost = true;
			return 0;
		}

		
		//for each column in the piece - fill in the appropriate blocks
		for(int i = 0; i < pWidth[nextPiece][orient]; i++) {
			
			//from bottom to top of brick
			for(int h = height+pBottom[nextPiece][orient][i]; h < height+pTop[nextPiece][orient][i]; h++) {
				field[h][i+slot] = turn;
			}
		}
		
		//adjust top
		for(int c = 0; c < pWidth[nextPiece][orient]; c++) {
			top[slot+c]=height+pTop[nextPiece][orient][c];
		}
		
		int rowsCleared = 0;
		
		//check for full rows - starting at the top
		for(int r = height+pHeight[nextPiece][orient]-1; r >= height; r--) {
			//check all columns in the row
			boolean full = true;
			for(int c = 0; c < COLS; c++) {
				if(field[r][c] == 0) {
					full = false;
					break;
				}
			}
			//if the row was full - remove it and slide above stuff down
			if(full) {
				rowsCleared++;
				cleared++;
				//for each column
				for(int c = 0; c < COLS; c++) {

					//slide down all bricks
					for(int i = r; i < top[c]; i++) {
						field[i][c] = field[i+1][c];
					}
					//lower the top
					top[c]--;
					while(top[c]>=1 && field[top[c]-1][c]==0)	top[c]--;
				}
			}
		}
	

		//pick a new piece
		nextPiece = randomPiece();
		

		
		return rowsCleared;
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


