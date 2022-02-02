package com.loff.xdmscannermodule;

public class SSCCCard {
    private int mcardImage;
    private String mcardLastFour;
    private String mcardSSCC;
    private String mcardDetails;
    private boolean misHR;

    public SSCCCard(int cardImage, String cardSSCC, String cardDetails, boolean isHR){
        mcardImage = cardImage;
        mcardLastFour = cardSSCC.substring(cardSSCC.length() - 4);
        mcardSSCC = cardSSCC;
        mcardDetails = cardDetails;
        misHR = isHR;
    }

    public String getSSCC(){
        return mcardSSCC;
    }
    public int getImage(){
        return mcardImage;
    }
    public String getDetails(){
        return mcardDetails;
    }
    public String getLastFour(){
        return mcardLastFour;
    }
    public boolean getIsHR() {return misHR;}
}
