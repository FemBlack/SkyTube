package free.rm.skytube.iap;

import android.content.res.Resources;

import java.util.Arrays;
import java.util.List;

import ind.riaz.iap.Donation;

public class DonationItems {
	
	public static final String SKU = "u2a_iap"; // Replace this with your item ID;
    public static final String SKU1 = "vibe_iap_silver";
    public static final String SKU2 = "vibe_iap_gold";
    public static final String SKU3 = "vibe_iap_platinum";
    public static final String SKU4 = "vibe_iap_priceless";
    
    public static final String SKU_VALUE = "2 USD"; // Replace this with your item ID;
    public static final String SKU1_VALUE = "3 USD";
    public static final String SKU2_VALUE = "5 USD";
    public static final String SKU3_VALUE = "10 USD";
    
    public static List<String> items = Arrays.asList(
    		SKU1, SKU2, SKU3, SKU4
    		);
    
    public static Donation[] get(Resources res) {
        String[] data = new String[]{
        		SKU_VALUE, SKU1,
        		SKU1_VALUE, SKU2,
        		SKU2_VALUE, SKU3,
        		SKU3_VALUE, SKU4
        		
        }; 

        Donation[] donation = new Donation[data.length / 2];

        int length = donation.length;
        for (int i = 0; i < length; i++) {
            donation[i] = new Donation(data[i * 2],
                    (data[i * 2 + 1]));
        }
        return donation;
    }

}