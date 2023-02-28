package bguspl.set.ex;

import bguspl.set.Env;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class contains the data that is visible to the player.
 *
 * @inv slotToCard[x] == y iff cardToSlot[y] == x
 */
public class Table {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Mapping between a slot and the card placed in it (null if none).
     */
    protected final Integer[] slotToCard; // card per slot (if any)

    /**
     * Mapping between a card and the slot it is in (null if none).
     */
    protected final Integer[] cardToSlot; // slot per card (if any)
    
    
    /**
     * Mapping between a token and the slot it is in .
     */
    protected ArrayList<Integer>[] tokens = new ArrayList[12];
    
    /**
     * The current empty slots .
     */
    protected ArrayList<Integer> emptySlots;
    /**
     * All the card that have been taken out of the game .
     */
    protected Vector<Integer> burnedCards;
    /**
     * Constructor for testing.
     *
     * @param env        - the game environment objects.
     * @param slotToCard - mapping between a slot and the card placed in it (null if none).
     * @param cardToSlot - mapping between a card and the slot it is in (null if none).
     */
    public Table(Env env, Integer[] slotToCard, Integer[] cardToSlot) {

        this.env = env;
        this.slotToCard = slotToCard;
        this.cardToSlot = cardToSlot;
        emptySlots = new ArrayList<Integer>();
        for(int i = 0;i<tokens.length;i++)
        {
        	tokens[i] = new ArrayList<Integer>(0);
        	emptySlots.add(i);
        }
        burnedCards = new Vector<>();
    }

    /**
     * Constructor for actual usage.
     *
     * @param env - the game environment objects.
     */
    public Table(Env env) {

        this(env, new Integer[env.config.tableSize], new Integer[env.config.deckSize]);
    }

    /**
     * This method prints all possible legal sets of cards that are currently on the table.
     */
    public void hints() {
        List<Integer> deck = Arrays.stream(slotToCard).filter(Objects::nonNull).collect(Collectors.toList());
        env.util.findSets(deck, Integer.MAX_VALUE).forEach(set -> {
            StringBuilder sb = new StringBuilder().append("Hint: Set found: ");
            List<Integer> slots = Arrays.stream(set).mapToObj(card -> cardToSlot[card]).sorted().collect(Collectors.toList());
            int[][] features = env.util.cardsToFeatures(set);
            System.out.println(sb.append("slots: ").append(slots).append(" features: ").append(Arrays.deepToString(features)));
        });
    }

    /**
     * Count the number of cards currently on the table.
     *
     * @return - the number of cards on the table.
     */
    public int countCards() {
        int cards = 0;
        for (Integer card : slotToCard)
            if (card != null)
                ++cards;
        return cards;
    }

    /**
     * Places a card on the table in a grid slot.
     * @param card - the card id to place in the slot.
     * @param slot - the slot in which the card should be placed.
     *
     * @post - the card placed is on the table, in the assigned slot.
     */
    public void placeCard(int card, int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}

        cardToSlot[card] = slot;
        slotToCard[slot] = card;
        
        // TODO implement
        env.ui.placeCard(card, slot);
        emptySlots.remove((Integer)slot);
    }

    /**
     * Removes a card from a grid slot on the table.
     * @param slot - the slot from which to remove the card.
     */
    public void removeCard(int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}

        // TODO implement
        emptySlots.add(slot);
        removeTokens(slot);
        env.ui.removeCard(slot);
        int card = slotToCard[slot];
        cardToSlot[card] = null;
        slotToCard[slot] = null;
        
    }

    /**
     * Places a player token on a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot on which to place the token.
     */
    public void placeToken(int player, int slot) {
        // TODO implement
        ArrayList<Integer> currSlot = tokens[slot];
        tokens[slot].add(player);
        env.ui.placeToken(player, slot);

    }
    

    /**
     * Removes a token of a player from a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot from which to remove the token.
     * @return       - true iff a token was successfully removed.
     */
    public boolean removeToken(int player, int slot) {
        // TODO implement

    	ArrayList<Integer> currSlot = tokens[slot];

        for (int currPlayer:currSlot)
        {
            if (currPlayer == player){
                env.ui.removeToken(player, slot);
                currSlot.remove((Integer) currPlayer);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Remove all the tokens from a grid slot.
     * @param slot   - the slot from which to remove the tokens.
     */
    public void removeTokens(int slot) {
    	ArrayList<Integer> currSlot = tokens[slot];
		for(int currPlayer:currSlot) {
		    env.ui.removeToken(currPlayer, slot);
		}
		currSlot.clear();
    	
    }

    
}
