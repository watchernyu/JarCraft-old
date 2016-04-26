/**
* This file is based on and translated from the open source project: Sparcraft
* https://code.google.com/p/sparcraft/
* author of the source: David Churchill
**/
package bwmcts.sparcraft.players;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import bwmcts.sparcraft.Constants;
import bwmcts.sparcraft.EvaluationMethods;
import bwmcts.sparcraft.Game;
import bwmcts.sparcraft.GameState;
import bwmcts.sparcraft.Players;
import bwmcts.sparcraft.Position;
import bwmcts.sparcraft.StateEvalScore;
import bwmcts.sparcraft.Unit;
import bwmcts.sparcraft.UnitAction;
import bwmcts.sparcraft.UnitActionTypes;

import java.util.Random;

public class Player_Watcher5 extends Player {
	// this one we can now control multiple units!!
	private int _id = 0;
	private int enemy;
	Random ran;
	ArrayList<Player> scripts;

	public Player_Watcher5(int playerID) {
		_id = playerID;
		setID(playerID);
		enemy = GameState.getEnemy(_id);
		ran = new Random();
		
		//initialize script players
		scripts = new ArrayList<Player>();
		//scripts.add(new Player_AttackClosest(playerID));
		scripts.add(new Player_NoOverKillAttackValue(playerID));
		scripts.add(new Player_KiteDPS(playerID));
		//scripts.add(new Player_AttackWeakest(playerID));
		//scripts.add(new Player_Retreat(playerID));
		//scripts.add(new Player_Defense(playerID));
		//currently only support playerID = 0;
	}

	public void getMoves(GameState state, HashMap<Integer, List<UnitAction>> moves, List<UnitAction> moveVec) {
		moveVec.clear();
		List<UnitAction> actions;
		int bestMoveIndex = 0;
		UnitAction move;
		int futureSteps = 6;
		int numOfMutations = 30;
		int numOfUnits = 16;
		int numOfScripts = scripts.size();
		//DNA initialization
		ArrayList<ArrayList<Integer>> DNA = new ArrayList<ArrayList<Integer>>();
		for(int k=0;k<futureSteps;k++){
			DNA.add(new ArrayList<Integer>());
			for(int i=0;i<numOfUnits;i++){
				DNA.get(k).add(ran.nextInt(numOfScripts));
			}
		}
		
		double dnaScore=0;
		double dnaBestScore=-9999;
		ArrayList<Integer> bestDna = new ArrayList<Integer>();
		
		//first try all the scripts individually
		for(int s=0;s<numOfScripts;s++){
			for(int k=0;k<futureSteps;k++){
				for(int i=0;i<numOfUnits;i++){
					DNA.get(k).set(i, s);
				}
			}
			dnaScore = this.dnaEvalGroup(state, DNA);
			if(dnaScore>dnaBestScore){
				dnaBestScore = dnaScore;
				bestDna = copy(DNA.get(0));
			}
			//System.out.println("BestScore: "+dnaBestScore+" DNA: "+bestDna);
			for(int i=0;i<15;i++){//15 mutations
				this.mutateGroup(DNA);
				dnaScore = this.dnaEvalGroup(state, DNA);
				if(dnaScore>dnaBestScore){
					dnaBestScore = dnaScore;
					bestDna = copy(DNA.get(0));
				}
			}
		}
		//System.out.println("Best Score: "+dnaBestScore +" DNA: "+bestDna);
		
		for(int k=0;k<futureSteps;k++){
			for(int i=0;i<numOfUnits;i++){
				DNA.get(k).set(i,ran.nextInt(numOfScripts));
			}
		}
		
		//mutation started
		for(int i=0;i<numOfMutations;i++){//100 mutations
			this.mutateGroup(DNA);
			dnaScore = this.dnaEvalGroup(state, DNA);
			if(dnaScore>dnaBestScore){
				dnaBestScore = dnaScore;
				bestDna = copy(DNA.get(0));
			}
		}
		
		//mutation ended
		//System.out.println("BestScore: "+dnaBestScore+" DNA: "+bestDna);
		dnaMoves(state,bestDna,moves,moveVec);
	}
	
	public ArrayList<Integer> copy(ArrayList<Integer> A){
		ArrayList<Integer> toReturn = new ArrayList<Integer>();
		for(int i=0;i<A.size();i++){
			toReturn.add(A.get(i));
		}
		return toReturn;
	}

	public void dnaMoves(GameState state, ArrayList<Integer> DNAi,
			HashMap<Integer,List<UnitAction>> moves, List<UnitAction> moveVec){
		for (Integer u : moves.keySet()){
			//this u is a unit index!!
			int scriptN = DNAi.get(u);
			Player scriptToUse = this.scripts.get(scriptN); //
			HashMap<Integer,List<UnitAction>> oneUnitMap = new HashMap<Integer,List<UnitAction>>();
			oneUnitMap.put(u, moves.get(u));
			scriptToUse.getMoves(state, oneUnitMap, moveVec);
		}
	}
	
	public void mutate(ArrayList<Integer> DNA) {
		// helper function to mutate a DNA, according to some rate.
		Double rate = 0.8;
		for(int i=0;i<DNA.size();i++){
			Double mut = ran.nextDouble();
			if (mut < rate) {
				int newGene = ran.nextInt(this.scripts.size());
				DNA.set(i, newGene);
			}
		}
	}
	
	public void mutateGroup(ArrayList<ArrayList<Integer>> DNA) {
		// helper function to mutate a DNA, according to some rate.
		Double rate = 0.2;
		for(int i=0;i<DNA.size();i++){
			for(int j=0;j<DNA.get(0).size();j++){
				Double mut = ran.nextDouble();
				ArrayList<Integer> gene = DNA.get(i);
				if (mut < rate) {
					int newGene = ran.nextInt(this.scripts.size());
					gene.set(j, newGene);
				}
			}
		}
	}
	
	public double dnaEval(GameState currentState,ArrayList<Integer> DNA){
		GameState sc = currentState.clone(); // sc for state clone
		Game gc = new Game(sc, new Player_NoOverKillAttackValue(this.ID()),
				new Player_NoOverKillAttackValue(this.enemy), 200, false, scripts); //send scripts to game...
		return gc.dnaEval(DNA);
	}
	
	public double dnaEvalGroup(GameState currentState,ArrayList<ArrayList<Integer>> DNA){
		GameState sc = currentState.clone(); // sc for state clone
		Game gc = new Game(sc, new Player_NoOverKillAttackValue(this.ID()),
				new Player_NoOverKillAttackValue(this.enemy), 200, false, scripts); //send scripts to game...
		return gc.dnaEvalGroup(DNA);
	}

	public String toString() {
		return "Watcher's first state-based alg";
	}
}