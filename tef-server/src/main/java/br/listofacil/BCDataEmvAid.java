package br.listofacil;

public class BCDataEmvAid {
	
	//Parametros EMV AID
	public String IdTable;
    public String TableIndex;
    public String AIDSize;
    public String AID;
    public String AIDCode;
    public String AppType;
    public String Label;
    public String AppDefault;
    public String AppVersionN1;
    public String AppVersionN2;
    public String AppVersionN3;
    public String TerminalCountryCode;
    public String TransactionCurrencyCode;
    public String TransactionCurrencyExponent;
    public String MerchantCategoryCode;
    public String TerminalCapabilities;
    public String AdditionalTerminalCapabilities;
    public String TerminalType;
    public String TACDefault;
    public String TACDenial;
    public String TACOnline;
    public String TerminalFloorLimit;
    public String TransactionCategoryCode;
    
    public String TargetPercentage;
    public String ThresholdValue;
    public String MaximumTarget;

    //Indica a ação para cartão com chip sem contato se o valor da transação estiver zerado:
    //“0” = Não suporta;
    //“1” = Suporta, porém somente online.
    public String ActionContactless = "0";

    public String TerminalCapabilitiesAID;
    public String TerminalContactlessLimit;
    public String TerminalContactlessFloorLimit;
    public String TerminalCVMRequiredLimit;
    public String PaypassMagstripeVersionNumber;
    public String SelectContactlessApp;
    public String TDOL;
    public String DDOL;

    public String AuthRespCodeOfflineApproved;
    public String AuthRespCodeOfflineDeclined;
    public String AuthRespCodeUnableToGoOnlineApproved;
    public String AuthRespCodeUnableToGoOnlineDeclined;

    public String TACContactlessDefault;
    public String TACContactlessDenial;
    public String TACContactlessOnline;
}
