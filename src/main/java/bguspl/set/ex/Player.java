package bguspl.set.ex;

import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.logging.Level;
import java.util.Random;

import bguspl.set.Env;

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */
public class Player implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;

    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */
    private Thread playerThread;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    private final boolean human;

    /**
     * True iff game should be terminated due to an external event.
     */
    private volatile boolean terminate;

    /**
     * The current score of the player.
     */
    private int score;
    /**
     * -1 default, 0 if the player chose a legal set, 1 if the player chose an illegal set.
     */
    protected int isLegalSet;
    /**
     * True iff the set of cards is ready to be checked.
     */
    private boolean isReadyForCheck;

    /**
     * The cards the player placed tokens on.
     */
    protected Vector<Integer> chosenCards;
    /**
     * The cards the player placed tokens on.
     */
    protected int[] cardsForCheck;

    /**
     * The Dealer.
     */
    private Dealer dealer;
    /**
     * Queue of incoming actions.
     */
    public Vector<Integer> incomingActions;
    /**
     * True iff a player submitted cards for check.
     */
    protected static boolean check = false;

    protected final Object lock = new Object();




    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.table = table;
        this.id = id;
        this.human = human;
        this.dealer = dealer;
        isReadyForCheck = true;
        chosenCards = new Vector<>();
        incomingActions = new Vector<>(3);
        cardsForCheck = new int[3];
        isLegalSet = -1;

    }

    /**
     * The main player thread of each player starts here (main loop for the player thread).
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + "starting.");
        if (!human) createArtificialIntelligence();
        while (!terminate) {
            // TODO implement main player loop
            if (!incomingActions.isEmpty()) {
                press(incomingActions.lastElement());
                incomingActions.remove(incomingActions.size()-1);
            }
            if (!check && chosenCards.size()==3) {
                if (isReadyForCheck) {
                    check = true;
                    dealer.playersToCheck.add((Integer) id);
                    try {
                        synchronized (lock) {
                            dealer.wakeDealer();
                            lock.wait();
                        }
                    } catch (InterruptedException e) {
                    }
                }
                if (isLegalSet == 1) {
                    penalty();
                    isReadyForCheck = false;
                }
                if (isLegalSet == 0) {
                    point();
                }
                isLegalSet = -1;
            }
        }
        if (!human) try { aiThread.join(); } catch (InterruptedException ignored) {}
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
    }



    /**
     * Creates an additional thread for an AI (computer) player. The main loop of this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it is not full.
     */
        private void createArtificialIntelligence() {
        // note: this is a very very smart AI (!)
        aiThread = new Thread(() -> {
            env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " starting.");
            while (!terminate) {
                // TODO implement player key press simulator
                if (incomingActions.size()<3){
                    int slot = randomSlot();
                    keyPressed(slot);
                }
//                try {
//                    synchronized (this) { wait(); }
//                } catch (InterruptedException ignored) {}
            }
            env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Creates random press for the ai players.
     */
    public int randomSlot(){
        Random rand = new Random();
        int numOfSlots = env.config.columns*env.config.rows;
        int slot = rand.nextInt(numOfSlots);
        return slot;
    }
    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {
        // TODO implement
        terminate = true;
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot) {
        // TODO implement
        incomingActions.add(slot);
    }

    /**
     * This method is called by the player thread to press a key.
     * @param slot - the slot corresponding to the key pressed.
     */
    public void press(int slot){

        if(!table.emptySlots.contains(slot)&&dealer.donePlacing) {
            int card = table.slotToCard[slot];
            if (table.removeToken(id, slot)) {
                chosenCards.remove((Integer) card);
                if (!isReadyForCheck) {
                    isReadyForCheck = true;
                }
            }else if (chosenCards.size() < 3) {
                table.placeToken(id, slot);
                chosenCards.add(card);
            }
        }
    }

    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {
        // TODO implement

        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
        env.ui.setScore(id, ++score);
        dealer.updateMaxScore(score);
        long sleepTime = env.config.pointFreezeMillis;
        try {
    		playerThread.sleep(sleepTime);
    	}
    	catch(InterruptedException e){ }
        incomingActions.clear();
    }
    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {
        // TODO implement
        long penaltyTime = env.config.penaltyFreezeMillis;
    	try {
    		playerThread.sleep(penaltyTime);
    	}
    	catch(InterruptedException e){}
        incomingActions.clear();
    }



    public int score() {
        return score;
    }

    /**
     * Return the current thread.
     */
    public Thread getPlayerThread() {
        return playerThread;
    }

    /**
     * Remove the burned cards from the chosen cards and perform other related actions.
     * * @param cards - the cards in the legal set we found.
     */
    public void burnCards(int[] cards){
        for (int i=0; i<cards.length; i++){
        	int slot = table.cardToSlot[cards[i]];
        	if(table.removeToken(id, slot)){
                chosenCards.remove((Integer)cards[i]);

            }


        }
        incomingActions.clear();
    }
    
    /**
     * Check if 3 cards perform a legal set.
     * * @param cards - the cards suspected to be a legal set.
     */

    public void wakePlayer(){
        synchronized (lock){
            lock.notifyAll();
        }
    }
    
}
