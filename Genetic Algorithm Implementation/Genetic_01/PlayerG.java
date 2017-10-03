
public class PlayerG {

	private double[] weights;

	public int pickMove(State s, int[][] legalMoves) {
		
		int r = s.tryMove(0);
		double maxVal = r + getStateValue(getFeatures(s.getTrialField(), s.getTrialTop()));
		int maxMove = 0;

		for (int i=1; i<legalMoves.length; i++){
			r = s.tryMove(i);
			double currVal = r + getStateValue(getFeatures(s.getTrialField(), s.getTrialTop()));
			if (currVal > maxVal){
				maxVal = currVal;
				maxMove = i;
			}

		}
		
		return maxMove;
	}

	public int[] getFeatures(int[][] field, int[] top){

		int features[] = new int[weights.length];

		for (int i=0; i<10; i++){
			features[i+1] = top[i];
			if (i < 9) {
				features[i+11] = Math.abs(top[i+1] - top[i]);
			}
			features[20] = Math.max(features[20], top[i]);
			for (int j=0; j<top[i]; j++){
				if (field[j][i] == 0){
					features[21] ++;
				}
			}
		}

		features[0] = 1;

		return features;
	}

	public double getStateValue(int[] features){
		double product = 0;
		for (int i=0; i<weights.length; i++){
			product += weights[i]*features[i];
		}
		return product;
	}

	public void setWeights(double[] w){
		weights = w;
	}

}
