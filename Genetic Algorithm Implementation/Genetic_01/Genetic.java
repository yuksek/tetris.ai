
import java.util.Arrays;
public class Genetic {

	public static int L = 22;
	public static int N = 1000;
	public static int G = 100;
	public static double Mrate = 0.1;

	public static double[][] population = new double[N][L];
	public static double[] fitness = new double[N];
	public static double[] bestWeight = new double[L];
	public static double bestFitness = 0;
	
	public static void main(String[] args) {

		for (int i = 0; i < N; i++){
			for (int j = 0; j < L; j++){
				population[i][j] = 20 * Math.random() - 10;
			} 
		}
		

		for (int g = 0; g < G; g++){

			int bestIndex = -1;
			double max = 0;
			for (int i = 0; i < N; i++){
				fitness[i] = computeFitness(population[i]);
				if (fitness[i] > max){
					max = fitness[i];
					if (fitness[i] > bestFitness){
						bestIndex = i;
						bestFitness = fitness[i];
					}
				}
			}
			if (bestIndex != -1){
				for (int i = 0; i < L; i++){
					bestWeight[i] = population[bestIndex][i];
				}
			}

			double[][] nextPop = new double[N][L];
			for (int i = 0; i < N; i++){
				int p1 = pickIndex(max);
				int p2 = pickIndex(max);
				int ranPosition = (int)(Math.random()*L);
				for (int j = 0; j < L; j++){
					if (j < ranPosition){
						nextPop[i][j] = population[p1][j];
					}
					else{
						nextPop[i][j] = population[p2][j];
					}
					if (Math.random() < Mrate){
						nextPop[i][j] = 20 * Math.random() - 10;
					}
				}	
			}
			population = nextPop;

			if ((g+1)%10 == 0){
				System.out.println("Finished "+ (g+1) +"th generation");	
				bestIndex = -1;
				for (int i = 0; i < N; i++){
					fitness[i] = computeFitness(population[i]);
					if (fitness[i] > bestFitness){
						bestIndex = i;
						bestFitness = fitness[i];
					}
				}
				if (bestIndex != -1){
					for (int i = 0; i < L; i++){
						bestWeight[i] = population[bestIndex][i];
					}
				}
				System.out.println(Arrays.toString(bestWeight));
				testWeight(bestWeight);
			}
		}
	}

	public static double computeFitness(double[] weights){
		int totalScore = 0;
		int totalMove = 0;
		for (int i = 0; i < 30; i++){
			State s = new State();
			PlayerG p = new PlayerG();
			p.setWeights(weights);
			while(!s.hasLost()) {
				s.makeMove(p.pickMove(s,s.legalMoves()));
				totalMove++;
			}
			totalScore += s.getRowsCleared();
		}
		return Math.pow(totalScore/30, 4) + totalMove/30;
	}

	public static int pickIndex(double max){
		int beSafe = 0;
		while (true){
			int index = (int)(Math.random()*N);
			double ranVal = (Math.random()*max);
			if (ranVal < fitness[index]){
				return index;
			}
			beSafe++;
			if (beSafe > 10000){
				return -1;
			}
		}
	}

	public static void testWeight(double[] weights){
		for (int i = 0; i < 10; i++){
			int totalScore = 0;
			for (int j = 0; j < 100; j++){
				State s = new State();
				PlayerG p = new PlayerG();
				p.setWeights(weights);
				while(!s.hasLost()) {
					s.makeMove(p.pickMove(s,s.legalMoves()));
				}
				totalScore += s.getRowsCleared();
			}
			System.out.println("You have completed "+ totalScore/100 +" rows.");
		}	
	}
	
}
