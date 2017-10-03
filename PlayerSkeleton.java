import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

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

		// To run Learning Genetic Algorithm
		Genetic g = new Genetic();
		g.run();
		
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

//Genetic 
class Genetic {

	public static int L = 10;
	public static int N = 100;
	public static int G = 10;
	public static double Mrate = 0.1;

	public static double[][] population = new double[N][L];
	public static ArrayList<Results> fitness = new ArrayList<Results>();
	public static double[] bestWeight = new double[L];
	public static double bestFitness = 0;
	
	public static int cores;
	public static int runJFP = 0;
	
	public static double fitnessToReach =0;
	
	public static void run() {
		//run genetic algorithm and compare two methods
//		compareJFPandNormOverall();
		runJFP = 1;
		runGenetic();
		
	}
	
	private static void compareJFPandNormOverall() {
		int numRuns = 10;
//		fitnessToReach = 1E16;//10E16;
		
		
		
		long totalTimeNorm = 0;
		long totalTimeJFP = 0;
		double totalFitnessNorm = 0;//[] = new double[numRuns];
		double totalFitnessJFP = 0;//[] = new double[numRuns];
		
		
		//Normal
		runJFP =0;
		for(int i =0;i<numRuns;i++){
			System.out.println("norm run: "+i);
			clearVar();
			totalTimeNorm += runGenetic();
			
		}
		
		runJFP = 1;
		for(int i =0;i<numRuns;i++){
			System.out.println("JFP run: "+i);
			clearVar();
			totalTimeJFP += runGenetic();
		}
		
		System.out.println("totatTimeNorm: "+totalTimeNorm);
		double avgNorm = totalTimeNorm/numRuns;
		System.out.println("totalTimeNorm average: "+avgNorm);
		
		double avgJFP = totalTimeJFP/numRuns;
		System.out.println("totalTimeJFP: "+totalTimeJFP);
		System.out.println("totalTimeJFP average: "+avgJFP);
		
		double compTotal = totalTimeNorm/(double)totalTimeJFP;
		double avg = avgNorm/avgJFP;
		System.out.println("totalTimeJFP ran "+compTotal+ " times faster than Norm");
		System.out.println("avgJFP ran "+avg+ " times faster than avgNorm");
	}
	private static void clearVar(){
		population = new double[N][L];
//		fitness = new double[N];
		fitness = new ArrayList<Results>();
		bestWeight = new double[L];
		bestFitness = 0;
	}
	
	
	//if you specify a fitness, then it will run till it hits the fitness
	private static long runGenetic() {
		long start = System.currentTimeMillis();
		if(runJFP == 1){
			cores = Runtime.getRuntime().availableProcessors();
			System.out.println("cores: "+cores);
		}
		for (int i = 0; i < N; i++){
			for (int j = 0; j < L; j++){
				population[i][j] = 20 * Math.random() - 10;
			} 
		}
		
		if(fitnessToReach ==0){
			for (int g = 0; g < G; g++){
				runGAPopulation(g);
			}
		}else{
			int count =0;
			while(true){
				runGAPopulation(count);
				if(bestFitness>fitnessToReach){
					System.out.println("bestFitness reached: "+bestFitness+" fitnessToreach: "+fitnessToReach);
					break;
				}
				count++;
			}
		}

		long elapsedTime = System.currentTimeMillis() - start;
		System.out.println("Elapsed time:"+ elapsedTime);
		return elapsedTime;
	}

	private static void runGAPopulation(int g) {
		if(runJFP == 1){
//					fitness[i] = computeFitnessJPF(population[i]);
			fitness = runJPF(population);
		}else{
			fitness = new ArrayList<Results>();
			for (int i = 0; i < N; i++){
				fitness.add(computeFitness(population[i]));
			}
		}
		
		double max = 0;
		for(int i =0;i<fitness.size();i++){
			double tempFitness = fitness.get(i).fitness;
			if(tempFitness > max){
				max = tempFitness;
				if(tempFitness>bestFitness){
					bestFitness = tempFitness;
					bestWeight = fitness.get(i).weights;//population[bestIndex][i];		
				}
			}
		}
		System.out.println(Arrays.toString(bestWeight));
		System.out.println("bestFiness for this gen is "+bestFitness);

		double[][] nextPop = new double[N][L];
		for (int i = 0; i < N; i++){
			int p1 = pickIndex(max);
			int p2 = pickIndex(max);
			int ranPosition = (int)(Math.random()*L);
			for (int j = 0; j < L; j++){
				if (j < ranPosition){
					nextPop[i][j] =  fitness.get(p1).weights[j];//population[p1][j];
				}
				else{
					nextPop[i][j] = fitness.get(p2).weights[j];//population[p2][j];
				}
				if (Math.random() < Mrate){
					nextPop[i][j] = 20 * Math.random() - 10;
				}
			}	
		}
		population = nextPop;

		System.out.println("Finished "+ (g+1) +"th generation");
		
		
		if ((g+1)== G){

			
			
			
		if(runJFP == 1){
				fitness = runJPF(population);
//					fitness[i] = computeFitnessJPF(population[i]);
		}else{
			fitness = new ArrayList<Results>();
			for (int i = 0; i < N; i++){
				fitness.add(computeFitness(population[i]));
			}
		}
									

		for(int i =0;i<fitness.size();i++){
			double tempFitness = fitness.get(i).fitness;
			if(tempFitness>bestFitness){
				bestFitness = tempFitness;
				bestWeight = fitness.get(i).weights;//population[bestIndex][i];		
			}
			
		}
		System.out.println("bestFiness for this gen is "+bestFitness);
		System.out.println(Arrays.toString(bestWeight));

		}
	}

	private static ArrayList<Results> runJPF(double[][] inputPopulation) {
		ForkJoinPool pool = new ForkJoinPool(cores);
		ArrayList<ForkJoinTask<Results>> output = new ArrayList<ForkJoinTask<Results>>();
		for (int i = 0; i < N; i++){
			output.add(pool.submit(new Task(inputPopulation[i])));
		}
		ArrayList<Results> res = new ArrayList<Results>();
		
		for(int i =0;i<N; i++){
			
			try {
				res.add(output.get(i).get());
//				System.out.println("output res is "+ res.get(i).fitness);
//				System.out.println("output w is "+ Arrays.toString(res.get(i).weights));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return res;
	}

	public static Results computeFitness(double[] weights){
		int totalScore = 0;
		int totalMove = 0;
		for (int i = 0; i < 10; i++){
			AugmentedState s = new AugmentedState();
			PlayerG p = new PlayerG();
			p.setWeights(weights);
			while(!s.hasLost()) {
				s.makeMove(p.pickMove(s,s.legalMoves()));
				totalMove++;
			}
			totalScore += s.getRowsCleared();
		}
		return new Results((Math.pow(totalScore/10, 4) + totalMove/10),weights);
	}

	public static int pickIndex(double max){
		int beSafe = 0;
		while (true){
			int index = (int)(Math.random()*N);
			double ranVal = (Math.random()*max);
			if (ranVal < fitness.get(index).fitness){
				return index;
			}
			beSafe++;
			if (beSafe > 5000){
				return -1;
			}
		}
	}
	
}

class Task implements Callable<Results>{
	private double[] weights;
	Task(double[] inputWeights){
		this.weights = inputWeights;
	}
	public Results call(){
		int totalScore = 0;
		int totalMove = 0;
		for (int i = 0; i < 10; i++){
			AugmentedState s = new AugmentedState();
			PlayerG p = new PlayerG();
			p.setWeights(weights);
			while(!s.hasLost()) {
				s.makeMove(p.pickMove(s,s.legalMoves()));
				totalMove++;
			}
			totalScore += s.getRowsCleared();
		}
		return new Results((Math.pow(totalScore/10, 4) + totalMove/10), weights);
	}
}
class Results{
	public double fitness;
	double[] weights;
	Results(double inputFitness,double[] inputWeights){
		this.fitness = inputFitness;
		this.weights = inputWeights;
	}
}
class AugmentedState {
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
	public AugmentedState() {
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

class PlayerG {

	private double[] weights;

	public int pickMove(AugmentedState s, int[][] legalMoves) {

		double maxVal = getValue(s,0);
		int maxMove = 0;

		for (int i = 1; i < legalMoves.length; i++){
			double currVal = getValue(s,i);
			if (currVal > maxVal){
				maxVal = currVal;
				maxMove = i;
			}

		}
		
		return maxMove;
	}

	public int[] getFeatures(AugmentedState s, int action){
		return s.tryMove(action);
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

	public double getValue(AugmentedState s, int action){
		int[] features = s.tryMove(action);
		return getValue(features);
	}

	public void setWeights(double[] w){
		weights = new double[w.length];
		for (int i = 0; i < w.length; i++){
			weights[i] = w[i];
		}
	}

}




