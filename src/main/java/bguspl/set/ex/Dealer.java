package bguspl.set.ex;

import bguspl.set.Env;
import bguspl.set.UtilImpl;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    private final Player[] players;

    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;

    /**
     * True iff game should be terminated due to an external event.
     */
    private volatile boolean terminate;

    /**
     * The UserInterface
     */
    private UtilImpl userInterface;

    /**
     * The max score.
     */
    private int maxScore;

    /**
     * True iff the dealer is done placing the cards.
     */
    protected boolean donePlacing;
    /**
     * The id  of players that their sets need to be checked, by the checking order.
     */
    protected Vector<Integer> playersToCheck;
    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = Long.MAX_VALUE;

    private final Object lock = new Object();




    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        userInterface = new UtilImpl(env.config);
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
        maxScore = 0;
        playersToCheck = new Vector<>();
        donePlacing = false;
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " starting.");
        for (int i=0; i< players.length; i++){
            Thread PlayerThread = new Thread(players[i], "Player"+players[i].id);
            PlayerThread.start();
        }
        while (!shouldFinish()) {
        	shuffleDeck(deck);
            placeCardsOnTable();
            timerLoop();
            updateTimerDisplay(false);
            removeAllCardsFromTable();
        }
        announceWinners();
        terminate();
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void timerLoop(){
        reshuffleTime = System.currentTimeMillis() + env.config.turnTimeoutMillis;
        while (!terminate && System.currentTimeMillis() < reshuffleTime) {

            sleepUntilWokenOrTimeout();
            if(!playersToCheck.isEmpty()) {
                int playerId = playersToCheck.remove(playersToCheck.size()-1);
                if (players[playerId].chosenCards.size()==3) {
                    checkSet(playerId, players[playerId].chosenCards);
                }
                players[playerId].wakePlayer();
            }
            updateTimerDisplay(false);
            removeCardsFromTable();
            placeCardsOnTable();
        }
    }

    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {
        // TODO implement
        for (int i=players.length-1; i>=0; i--){
            players[i].terminate();
        }
        terminate = true;
    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0;
    }

    /**
     * Checks cards should be removed from the table and removes them.
     */
    private void removeCardsFromTable() {
        // TODO implement
    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        // TODO implement
        donePlacing = false;
    	ArrayList<Integer> emptySlots = table.emptySlots;
    	Collections.shuffle(emptySlots);
    	for (int i = 0; deck.size()>0 && i< emptySlots.size(); i++)
    	{
    		table.placeCard(deck.remove(0), emptySlots.get(i));
    	}
    donePlacing = true;
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     */
    private void sleepUntilWokenOrTimeout() {
        // TODO implement
        int sleepTimeMillis = 99;
        if (reshuffleTime - System.currentTimeMillis() <= env.config.turnTimeoutWarningMillis){
            sleepTimeMillis = 5;
        }
        try {
            Thread.currentThread().sleep(sleepTimeMillis);
        } catch (InterruptedException e){}
    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        // TODO implement
        if (reset){
            env.ui.setCountdown(env.config.turnTimeoutMillis, false);
            reshuffleTime = System.currentTimeMillis() + env.config.turnTimeoutMillis;
        }
        else if(reshuffleTime - System.currentTimeMillis() <= env.config.turnTimeoutWarningMillis){
            env.ui.setCountdown(reshuffleTime - System.currentTimeMillis(), true);
        }
        else {
            env.ui.setCountdown(reshuffleTime - System.currentTimeMillis(), false);
        }
    }

    /**
     * Returns all the cards from the table to the deck.
     */
    public void removeAllCardsFromTable() {
        // TODO implement
        for(int i=0; i<players.length; i++){
            players[i].chosenCards.clear();
        }
        for (int slot=0; slot<12; slot++){
            table.removeCard(slot);

        }

    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        // TODO implement
        Vector<Player> winnersVector = new Vector<>();
        for (int i=0; i<players.length; i++){
            if (players[i].score() == maxScore){
                winnersVector.add(players[i]);
            }
        }
        int[] winnersArray = new int[winnersVector.size()];
        for (int i=0; i<winnersArray.length; i++){
            winnersArray[i] = winnersVector.elementAt(i).id;
        }
        env.ui.announceWinner(winnersArray);
    }
    
    
    /**
     * shuffle the deck.
     * @param deck
     */
    private void shuffleDeck(List<Integer> deck)
    {
    	Collections.shuffle(deck);
    }

    /**
     * Checks if input cards form a  legal set.
     * @param cardsVec - cards to check.
     * @param playerId - the id of the player that sent the cards.
     * @return true if the cards form a legal set, false otherwise.
     */
    public synchronized boolean checkSet(int playerId, Vector<Integer> cardsVec){
        int[] cards = new int[cardsVec.size()];
        for (int i = 0; i < cardsVec.size(); i++) {
            cards[i] = cardsVec.elementAt(i);
        }
        boolean ans = userInterface.testSet(cards);
        Player currPlayer = players[playerId];
        if (ans){
            for (int i=0; i<players.length; i++){
                players[i].burnCards(cards);
            }
            for (int i=0; i<cards.length; i++){
                int currSlot = table.cardToSlot[cards[i]];
                table.removeTokens(currSlot);
                table.removeCard(currSlot);
                deck.remove((Integer)cards[i]);
                table.burnedCards.add((Integer)cards[i]);

            }
            placeCardsOnTable();
            updateTimerDisplay(true);
            currPlayer.isLegalSet = 0;
        }
        else {
            currPlayer.isLegalSet = 1;
        }
        currPlayer.check=false;
        return ans;
    }

    public void wakeDealer(){
        synchronized (lock){
            lock.notifyAll();
        }
    }

    public void updateMaxScore(int score){
        if (score>maxScore){
            maxScore = score;
        }
    }

}
