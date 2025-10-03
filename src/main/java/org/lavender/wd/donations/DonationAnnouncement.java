package org.lavender.wd.donations;

/**
 *
 * @author Studios TKOH!
 */
public record DonationAnnouncement(String player, String item, String amount) {

    public String amountOrEmpty() {
        return amount == null ? "" : amount;
    }
}
