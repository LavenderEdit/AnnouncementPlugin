package org.lavender.wd.donations;

/**
 *
 * @author Studios TKOH!
 */
public record RankPurchase(String player, String rank, String amount) {

    public String amountOrEmpty() {
        return amount == null ? "" : amount;
    }
}