	package br.listofacil.acquirer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.MUX;
import org.jpos.util.LogEvent;
import org.jpos.util.Logger;
import org.jpos.util.NameRegistrar;
import org.jpos.util.NameRegistrar.NotFoundException;

import br.listofacil.AcquirerLogonProcess;
import br.listofacil.AcquirerSettings;
import br.listofacil.BCDataEmvAid;
import br.listofacil.BCDataPublicKeys;
import br.listofacil.CommonFunctions;
import br.listofacil.tefserver.iso.ISO87APackagerGP;

public class GlobalpaymentsMessage {
	private final  String MERCHANT = "LISTO FACIL";
	private final static String GLOBAL_PAYMENTS = "01";
	
	private final  String REQ_GP_LOGON_INIT = "0800";
	private final  String RES_GP_LOGON_INIT = "0810";
	private final  String REQ_GP_PAYMENT = "0200";
	private final  String RES_GP_PAYMENT = "0210";
	private final  String REQ_GP_ADVICE = "0220";
	private final  String RES_GP_ADVICE = "0230";
	private final  String REQ_GP_CANCELLATION = "0400";
	private final  String RES_GP_CANCELLATION = "0410";
	private final  String REQ_GP_UNMAKING = "0420";
	private final  String RES_GP_UNMAKING = "0430";
	private final  String REQ_GP_CONFIRMATION = "9820";
	private final  String RES_GP_CONFIRMATION = "9830";
	
	private final int FIELD_PAN = 2;
	private final int FIELD_PROC_CODE = 3;
	private final int FIELD_AMOUNT = 4;
	private final int FIELD_DATE_TIME = 7;
	private final int FIELD_NSU_TEF = 11;
	private final int FIELD_TIME = 12;
	private final int FIELD_DATE = 13;
	private final int FIELD_CARD_EXPIRATION_DATE = 14;
	private final int FIELD_ENTRY_MODE = 22;
	private final int FIELD_PAN_SEQUENCE = 23;
	private final int FIELD_TRACK_2 = 35;
	private final int FIELD_CARD_SECURITY_CODE = 36;
	private final int FIELD_AUTHORIZATION_CODE = 38;
	private final int FIELD_RESPONSE_CODE = 39;
	private final int FIELD_TERMINAL_CODE = 41;
	private final int FIELD_MERCHANT_CODE = 42;
	private final int FIELD_ADDITIONAL_DATA_1 = 48;
	private final int FIELD_CURRENCY_CODE = 49;
	private final int FIELD_PIN = 52;
	private final int FIELD_EMV_DATA = 55;
	private final int FIELD_ADDITIONAL_DATA_2 = 60;
	private final int FIELD_TERMINAL_DATA = 61;
	private final int FIELD_TRANSACTION_DATA_1 = 62;
	private final int FIELD_TRANSACTION_DATA_2 = 63;
	private final int FIELD_ORIGINAL_DATA = 90;
	private final int FIELD_CONFIRMATION_DATA = 120;
	private final int FIELD_SECURITY_DATA = 126;
	private final int FIELD_NSU_ACQUIRER = 127;
	
	//BIT048
	private final String TAG_AD1_TABLES_VERSION = "001";
	private final String TAG_AD1_FLAG_UPDATE = "002";
	private final String TAG_AD1_CODE_FUNCTION = "003";
	private final String TAG_AD1_VALIDITY_PERIOD_PRE_AUTH = "004";
	private final String TAG_AD1_ENCRYPTED_PAN = "005";
	private final String TAG_AD1_INSTALLMENTS = "006";
	
	//BIT061
	private final String TAG_TD_SPEC_VERSION = "001";
	private final String TAG_TD_PINPAD_SERIAL_NUMBER = "002";
	private final String TAG_TD_DATA_FORMAT = "003";
	private final String TAG_TD_PINPAD_APP_VERSION = "004";
	private final String TAG_TD_PINPAD_MAMNUFACTURER = "005";
	private final String TAG_TD_PINPAD_MODEL = "006";
	private final String TAG_TD_PINPAD_FIRMWARE = "007";
	
	//BIT120
	private final String TAG_CT_TRANSACTION_ID = "001";
	private final String TAG_CT_DATA_TRANSACTION = "002";
	
	//BIT126
	private final String TAG_ENCRYPTION_PIN_TYPE = "001";
	private final String TAG_ENCRYPTION_PIN_KSN = "002";
	private final String TAG_ENCRYPTION_CARD_TYPE = "003";
	private final String TAG_ENCRYPTION_CARD_KSN = "004";
	private final String TAG_TYPE_CARD_VERIFICATION_DATA = "005";
	private final String TAG_CARD_VERIFICATION_DATA = "006";
	private final String TAG_SECURITY_CODE_DEBIT = "010";
	
	private String PARAM_41 = "00000000"; //terminal number
	private String PARAM_42 = "012000006020001";
	
	private String DATA_POSITION_GP_PINPAD = "04";
	private String PIN_POSITION_GP_PINPAD = "317"; //DUKPT 3DES = 3-3DES 17-POSICAO
	
	//Versao do conteudo das tabelas = 0 (solicita tabelas)
	private String PARAM_48001 = "00100800000000";
	
	//Versao da especificacao da Globalpayments
	private String VERSAO_ESPEC_GP = "GP0102LI";
	private String PARAM_61001 = "001008" + VERSAO_ESPEC_GP;
	
	public final static String PROC_CODE_LOGON = "910000";
	public final static String PROC_CODE_INIT = "900000";
	public final static String PROC_CODE_CREDIT = "003000";
	public final static String PROC_CODE_DEBIT = "002000";

	//TABELAS GP
	private final String TAB_MERCHANT = "001";
	private final String TAB_BINS = "002";
	private final String TAB_PRODUCTS = "003";
	private final String TAB_ESP_BINS = "004";
	private final String TAB_ESP_PRODUCTS = "005";
	private final String TAB_FEATURES = "006";
	private final String TAB_FUNC_PRODUCT = "007";
	private final String TAB_EMV_AIDS = "008";
	private final String TAB_REQ_TAGS_1ND_GEN = "009";
	private final String TAB_OPT_TAGS_1ND_GEN = "010";
	private final String TAB_REQ_TAGS_2ND_GEN = "011";
	private final String TAB_OPT_TAGS_2ND_GEN = "012";
	private final String TAB_PUBLIC_KEYS = "013";
	private final String TAB_ENCRYPTION_PIN = "014";
	private final String TAB_ENCRYPTION_DATA = "015";
	
	public final static String FUNC_CREDIT = "01";
	public final static String FUNC_CREDIT_WITH_INTEREST = "02";
	public final static String FUNC_CREDIT_WITHOUT_INTEREST = "03";
	public final static String FUNC_DEBIT = "50";
	
	private static final String idMUXGlobalpayments = "mux.clientsimulator-globalpayments-mux";

	ListoData listoData = new ListoData();
	CommonFunctions cf = new CommonFunctions();
	
	//Map criado para verificar se existem registros duplicados no adquirente
	private ArrayList<String> tablesGlobalpayments = new ArrayList<String>();
	private HashMap<String, String> refCodesEmvAid = new HashMap<String, String>(); 
	
	int initializationId = 0;
	
	
	public boolean loadTablesInitialization(String logicalNumber, String terminalNumber, boolean forceInitialization) throws ISOException, Exception{		
		
		ISOMsg response = null;
		boolean ret = true;
		
		PARAM_41 = terminalNumber;
		tablesGlobalpayments = new ArrayList<String>();
		
		if (!forceInitialization)
			AcquirerSettings.setStatusLoadingGlobalpayments(logicalNumber);
			
		//Efetuar o logon
		response = requestLogon(logicalNumber, AcquirerSettings.getIncrementNSUGlobalpayments());

		if (response != null)
		{
			while(true)
			{
				response = requestTable(logicalNumber, AcquirerSettings.getIncrementNSUGlobalpayments());
				
				//Timeout
				if (response == null)
				{
					Logger.log(new LogEvent("Globalpayments FAIL - Connection Timed out!"));
					AcquirerSettings.removeStatusLoadingGlobalpayments(logicalNumber);
					return false;
				}
				
				setResDataInitialization(response);
								
				//Verifica se eh o ultimo registro = 90000
				if (response.hasField(3)) {
					if (response.getString(3).equals(PROC_CODE_INIT))
					break;
				}
			}
			
			//Seta inicializacao na estrutura padrao Listo
			AcquirerSettings.setInitializationTables(GLOBAL_PAYMENTS, logicalNumber, listoData); 
			
		} else {
			Logger.log(new LogEvent("Globalpayments FAIL - Connection Timed out!"));
			ret = false;
		}
		
		if (!forceInitialization)
			AcquirerSettings.removeStatusLoadingGlobalpayments(logicalNumber);
		
		AcquirerSettings.writeDataFile();
		
		return ret;
	}


	public ISOMsg requestLogon(String logicalNumber, long nsu)
	{
		ISOMsg request = new ISOMsg();
		ISOMsg response = null;
		
		try {
			
			request.setPackager(new ISO87APackagerGP());
			request.setMTI(REQ_GP_LOGON_INIT);
			request.set(3, PROC_CODE_LOGON);
			//Obtem os bits 007, 011, 012 e 013
			request = getCommonBitsFormatted(request, nsu);
			request.set(42, logicalNumber);
			request.set(48, PARAM_48001);
			request.set(61, PARAM_61001);
			
			response = requestAcquirer(request);
			
		} catch (ISOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (response != null)
			AcquirerLogonProcess.setDateLogon(ListoData.BANRISUL, cf.getCurrentDate());
		
		return response;
	}
	
	public ISOMsg requestTable(String logicalNumber, long nsu)
	{
		ISOMsg request = new ISOMsg();
		ISOMsg response = null;
		
		try {
			
			request.setPackager(new ISO87APackagerGP());
			request.setMTI(REQ_GP_LOGON_INIT);
			request.set(3, PROC_CODE_INIT);
			//Obtem os bits 007, 011, 012 e 013
			request = getCommonBitsFormatted(request, nsu);
			request.set(42, logicalNumber);
			request.set(61, PARAM_61001);
			
			response = requestAcquirer(request);
			
		} catch (ISOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return response;
	}
	
	private String getAcquirerTimestampTables(ISOMsg m)
	{	
		try {
			
			//Retira o conteudo do TLV
			HashMap<String, String> info = cf.tlvExtractData(m.getString(48));
			
			//Obtem somente a versao das tabelas
			if (info.containsKey("001"))
				return info.get("001");
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return "";
	}
	
	private ISOMsg requestAcquirer(ISOMsg request)
	{
		ISOMsg response = null;
		
		try {
			
			MUX mux = (MUX) NameRegistrar.get(idMUXGlobalpayments);
			if (!mux.isConnected()) //VERIFICAR OUTRA FORMA
			{
				//Thread.currentThread().sleep(500);
				return null;
			}
			//Request acquirer
			response = mux.request(request, ListoData.SERVER_TIMEOUT * 1000);
			
		} catch (NotFoundException | ISOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return response;
	}
	
	public void setResDataInitialization(ISOMsg m)
	{	
		String tab01 = new String();
		String tab02 = new String();
		String tab03 = new String();
		String tab04 = new String();
		String tab05 = new String();
		String tab06 = new String();
		String tab07 = new String();
		String tab08 = new String();
		String tab12 = new String();
		String tab13 = new String();
		String tab14 = new String();
		String tab15 = new String();
		String tab16 = new String();
		String tab17 = new String();
		
		try {		
			
			listoData.codigoAdquirente = ListoData.GLOBAL_PAYMENTS;
			listoData.posicaoChaveDadosPinpad = DATA_POSITION_GP_PINPAD;
			listoData.posicaoChaveSenhaPinpad = PIN_POSITION_GP_PINPAD;
			listoData.gmtDataHora = m.getString(7);
			listoData.nsuOrigem = m.getString(11);
			listoData.horaLocal = m.getString(12);
			listoData.dataLocal = m.getString(13);
			listoData.codigoResposta = m.getString(39);
			listoData.numeroLogico = m.getString(42);
			listoData.versaoTabelasGlobalpayments = getAcquirerTimestampTables(m);
			listoData.smid = "";
			listoData.workingKey = "";
			
			String code = "";
			String registro = m.getValue(62).toString();
			//Adiciona caso exista o BIT063
			if (m.hasField(63))
				registro += m.getValue(63).toString();

			while(registro.length() > 0)
			{   
				String tag = registro.substring(0,  3);
				String len = registro.substring(3,  6);
				String value = registro.substring(6, Integer.parseInt(len) + 6);
				
				if (!checkRecordDuplicated(value)) {
				    switch (tag) {
					case TAB_MERCHANT:
						tab01 += getMerchantData(value);
					    code = ListoData.REG_CODE_ESTABELECIMENTO;
						break;
					case TAB_BINS:
						tab02 += value;
						code = ListoData.REG_CODE_BINS;
						break;
					case TAB_PRODUCTS:
						tab03 += getProducts(value);
						code = ListoData.REG_CODE_PRODUTOS;
						break;
					case TAB_ESP_BINS:
						tab05 += value;
						code = ListoData.REG_CODE_BINS_ESPECIAIS;
						break;
					case TAB_ESP_PRODUCTS:
						tab06 += getProducts(value);
						code = ListoData.REG_CODE_PRODUTOS_ESPECIAIS;
						break;
					case TAB_FEATURES:
						tab08 += getTransactionParameters(value);
						code = ListoData.REG_CODE_PARAMS_TRANSACAO;
						break;
					case TAB_FUNC_PRODUCT:
						//Tabela nao utilizada no sistema - ignorada
						break;
					case TAB_EMV_AIDS:
						setEmvAid(value);
						code = ListoData.REG_CODE_EMV_AID;
						break;
					case TAB_PUBLIC_KEYS:
						setPublicKey(value);
						code = ListoData.REG_CODE_CHAVES_PUBLICAS;
						break;
					case TAB_REQ_TAGS_1ND_GEN:
						tab12 += getTagsFormatted(value);
						code = ListoData.REG_CODE_REQ_TAGS_1ND_GEN;
						break;
					case TAB_OPT_TAGS_1ND_GEN:
						tab13 += getTagsFormatted(value + "5F34");
						code = ListoData.REG_CODE_OPT_TAGS_1ND_GEN;
						break;
					case TAB_REQ_TAGS_2ND_GEN:
						tab14 += getTagsFormatted(value);
						code = ListoData.REG_CODE_REQ_TAGS_2ND_GEN;
						break;
					case TAB_OPT_TAGS_2ND_GEN:
						tab15 += getTagsFormatted(value);
						code = ListoData.REG_CODE_OPT_TAGS_2ND_GEN;
						break;
					case TAB_ENCRYPTION_PIN:
						tab16 += getEncryptionRegistry(value);
						code = ListoData.REG_CODE_CRIPTO_SENHA;
						break;
					case TAB_ENCRYPTION_DATA:
						tab17 += getEncryptionRegistry(value);
						code = ListoData.REG_CODE_CRIPTO_DADOS;
						break;
					}
				}
			    registro = registro.substring(6 + Integer.parseInt(len), registro.length());
			    
			    if (!listoData.tableSequence.contains(code))
			    	listoData.tableSequence.add(code);
			    
			}
			
			setTablesInitialization(tab01, tab02, tab03, tab04,
									tab05, tab06, tab07, tab08,
									tab12, tab13, tab14, tab15, 
									tab16, tab17);
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	private void setTablesInitialization(String tab01, String tab02, String tab03, String tab04,
										 String tab05, String tab06, String tab07, String tab08,
										 String tab12, String tab13, String tab14, String tab15, 
										 String tab16, String tab17){
		if (tab01.length() > 0) 
			listoData.L001_estabelecimento.put(listoData.L001_estabelecimento.size(), tab01);
		if (tab02.length() > 0)
			listoData.L002_bins.put(listoData.L002_bins.size(), tab02);
		if (tab03.length() > 0)
			listoData.L003_produtos.put(listoData.L003_produtos.size(), tab03);
		if (tab04.length() > 0)
			listoData.L004_paramsProdutos.put(listoData.L004_paramsProdutos.size(), tab04);
		if (tab05.length() > 0)
			listoData.L005_binsEspeciais.put(listoData.L005_binsEspeciais.size(), tab05);
		if (tab06.length() > 0)
			listoData.L006_produtosEspeciais.put(listoData.L006_produtosEspeciais.size(), tab06);
		if (tab07.length() > 0)
			listoData.L007_paramsProdutosEspeciais.put(listoData.L007_paramsProdutosEspeciais.size(), tab07);
		if (tab08.length() > 0)
			listoData.L008_parametros.put(listoData.L008_parametros.size(), tab08);
		if (tab12.length() > 0)
			listoData.L012_TagsReq1ndGenerateAC.put(listoData.L012_TagsReq1ndGenerateAC.size(), tab12);
		if (tab13.length() > 0)
			listoData.L013_TagsOpt1ndGenerateAC.put(listoData.L013_TagsOpt1ndGenerateAC.size(), tab13);
		if (tab14.length() > 0)
			listoData.L014_TagsReq2ndGenerateAC.put(listoData.L014_TagsReq2ndGenerateAC.size(), tab14);
		if (tab15.length() > 0)
			listoData.L015_TagsOpt2ndGenerateAC.put(listoData.L015_TagsOpt2ndGenerateAC.size(), tab15);
		if (tab16.length() > 0)
			listoData.L016_CriptografiaSenha.put(listoData.L016_CriptografiaSenha.size(), tab16);
		if (tab17.length() > 0)
			listoData.L017_CriptografiaDados.put(listoData.L017_CriptografiaDados.size(), tab17);
	}
	
	private String getTagsFormatted(String value) {
		//Codigo do AID
		String id = value.substring(0, 2);
		String tagsEmv = value.substring(2, value.length());
		String registry = "";
		
		if (refCodesEmvAid.containsKey(id)) {
			id = refCodesEmvAid.get(id); //Obtem o id da BC
			registry = id + cf.padLeft(String.valueOf(tagsEmv.length()), 3, "0") + tagsEmv;
		}
		
		return registry;
	}
	
	private String getEncryptionRegistry(String value) {
		String data = new String();
		if (value != null && value.length() > 0) {
			data = value.substring(0, 1); //Codigo	
			String indexMasterKey =  value.substring(value.length() - 2, value.length());
			value = value.substring(1, value.length() - 2); //masterkey
			data = cf.convertHexToInt(indexMasterKey) + data + 
				   cf.padLeft(String.valueOf(value.length()), 3, "0") + value;
		}
		return data;
	}
	
	private boolean checkRecordDuplicated(String value) {
		if (!tablesGlobalpayments.contains(value)) {
			tablesGlobalpayments.add(value);
			return false;
		}
		return true;
	}
	
	private void setEmvAid(String value) {
		String emvAid = getEmvAid(value);
		if (!listoData.L009_emv.containsValue(emvAid)) {
			listoData.L009_emv.put(listoData.L009_emv.size(), emvAid);
		}
	}
	
	private void setPublicKey(String value) {
		String publicKey = getPublicKeys(value);
		if (!listoData.L010_chavesPublicas.containsValue(publicKey)) 
			listoData.L010_chavesPublicas.put(listoData.L010_chavesPublicas.size(), publicKey);
	}
	
	private String getMerchantData(String value)
	{
		String merchant = "001038" + value.substring(0, 38); //Nome do estabelecimento
		merchant += "002076" + value.substring(38, 114); 	 //Endereço do estabelecimento
		
		//Na inicializacao da GP nao eh enviado as tags de 003 a 006 e 008 e 009
		
		listoData.merchantCategoryCode = value.substring(125, 129); 
		listoData.currencyCode = value.substring(118, 121);
		listoData.currencyExponent = value.substring(121, 122);
		listoData.terminalCountryCode = value.substring(122, 125);
		
		merchant += "007004" + listoData.merchantCategoryCode; 	 //MCC
		merchant += "010004" + value.substring(114, 118);	 	 //Codigo da moeda
		merchant += "011003" + listoData.currencyCode;	 		 //Terminal currency code 
		merchant += "012001" + listoData.currencyExponent;	 	 //Transaction currency exponent 
		merchant += "013003" + listoData.terminalCountryCode; 	 //Terminal country code
		
		merchant += "014008" + cf.stringValueToStringBinary(value.substring(129, 131)); //Byte 1
		merchant += "015008" + cf.stringValueToStringBinary(value.substring(131, 133)); //Byte 2
		merchant += "016008" + cf.stringValueToStringBinary(value.substring(133, 135)); //Byte 3
		
		return merchant;
	}
	
	private String getProducts(String value)
	{
		String product = "";
		
		while(value.length() > 0) {
			product += value.substring(0, 3); //Codigo do produto
			String type = "D";
			product += value.substring(3, 23);		//Descricao
			if (value.substring(23, 25).equals("01"))
				type = "C";
			product += type;						//Tipo credito e debito
			product += cf.stringValueToStringBinary(value.substring(25, 27)); //Byte 1
			product += cf.stringValueToStringBinary(value.substring(27, 29)); //Byte 2
			product += cf.stringValueToStringBinary(value.substring(29, 31)); //Byte 3
			
			value = value.substring(35, value.length());
		}
		
		return product;
	}
	
	
	private String getTransactionParameters(String value)
	{
		String params = "";
		String name = "";
		
		while(value.length() > 0) {
			String code = value.substring(0, 2);
			switch (code) {
			case "01":
				name = "COMPRA CREDITO ROTATIVO";
				break;
			case "02":
				name = "COMPRA CREDITO PARCELADO C/ JUROS";
				break;
			case "03":
				name = "COMPRA CREDITO PARCELADO S/ JUROS";
				break;
			//case 04 = pre-autorizacao de credito
			//case 05 = confirmacao de pre-autorizacao
			//case 09-49 = reservado para uso futuro credito
			case "50":
				name = "COMPRA DEBITO A VISTA";
				break;
			}
			//caseo 51-60 = reservado para uso futuro debito
			params += code;
			params += cf.padRight(name, 50, " ");
			//Ignora byte 1 e 2
			//Parametros gerais
			//esta sem os ultimos 3 parametros (prazo, qtde dias e limite dias)
			params += value.substring(6, 60) + "000000";
			value = value.substring(60, value.length());
		}

		return params;
	}
	
	private String getEmvAid(String value)
	{

		listoData.indiceTabEmvAid++;
		BCDataEmvAid paramEMV = getBCDataEmvAid(listoData.indiceTabEmvAid, value);

		String registry = paramEMV.IdTable +
						DATA_POSITION_GP_PINPAD + 
	                    paramEMV.TableIndex +
	                    paramEMV.AIDSize +
	                    paramEMV.AID +
	                    paramEMV.AppType +
	                    paramEMV.Label +
	                    paramEMV.AppDefault +
	                    paramEMV.AppVersionN1 +
	                    paramEMV.AppVersionN2 +
	                    paramEMV.AppVersionN3 +
	                    paramEMV.TerminalCountryCode +
	                    paramEMV.TransactionCurrencyCode +
	                    paramEMV.TransactionCurrencyExponent +
	                    listoData.numeroLogico +
	                    paramEMV.MerchantCategoryCode +
	                    PARAM_41 + //Terminal number
	                    paramEMV.TerminalCapabilities +
	                    paramEMV.AdditionalTerminalCapabilities +
	                    paramEMV.TerminalType +
	                    paramEMV.TACDefault +
	                    paramEMV.TACDenial +
	                    paramEMV.TACOnline +
	                    paramEMV.TerminalFloorLimit +
	                    paramEMV.TransactionCategoryCode +
	                    paramEMV.ActionContactless +
	                    paramEMV.TerminalCapabilitiesAID +
	                    paramEMV.TerminalContactlessLimit +
	                    paramEMV.TerminalContactlessFloorLimit +
	                    paramEMV.TerminalCVMRequiredLimit +
	                    paramEMV.PaypassMagstripeVersionNumber +
	                    paramEMV.SelectContactlessApp +
	                    paramEMV.TDOL +
	                    paramEMV.DDOL +
	                    paramEMV.AuthRespCodeOfflineApproved +
	                    paramEMV.AuthRespCodeOfflineDeclined +
	                    paramEMV.AuthRespCodeUnableToGoOnlineApproved +
	                    paramEMV.AuthRespCodeUnableToGoOnlineDeclined;
	                    /*
	                    paramEMV.TACContactlessDefault +
	                    paramEMV.TACContactlessDenial +
	                    paramEMV.TACContactlessOnline;
	                     * */

		registry = cf.padLeft(String.valueOf(registry.length() + 3), 3, "0") + registry;
		
		//Adiciona o AIDCode para ter referencia do index
		refCodesEmvAid.put(paramEMV.AIDCode, paramEMV.TableIndex);
		
		return registry;
	}
	
	private BCDataEmvAid getBCDataEmvAid(int id, String value) {
		BCDataEmvAid bcData = new BCDataEmvAid();
		
		bcData.IdTable = "1"; //Referencia biblioteca compartilhada
        bcData.AIDCode = value.substring(0, 2); //Codigo do AID
        bcData.TableIndex = cf.padLeft(String.valueOf(id), 2, "0");
        bcData.AIDSize = value.substring(2, 4);
        bcData.AID = value.substring(4, 36);

        bcData.Label = value.substring(36, 52);
        bcData.AppType = value.substring(52, 54);
        bcData.AppDefault = "03"; //Padrão da aplicação: “03” - EMV (com ou sem contato)
        bcData.TerminalFloorLimit = "00000000"; //aid.substring(54, 62); - Global solicitou colocar por seguranca 23/06/16 conversa com Lucas Argotechno
        bcData.AppVersionN1 = value.substring(62, 66);
        bcData.AppVersionN2 = value.substring(66, 70);
        bcData.AppVersionN3 = value.substring(70, 74);
        bcData.TDOL = value.substring(74, 114);
        bcData.DDOL = value.substring(114, 154);

        if (value.substring(162, 164) != "00")
            bcData.TerminalType = value.substring(162, 164);
        else
            bcData.TerminalType = "22"; //offline com capacidade online

        bcData.TerminalCapabilities = value.substring(164, 170);
        bcData.AdditionalTerminalCapabilities = value.substring(170, 180);
        bcData.TACDefault = value.substring(180, 190);
        bcData.TACDenial = value.substring(190, 200);
        bcData.TACOnline = value.substring(200, 210);

        bcData.TargetPercentage = value.substring(210, 212);
        bcData.ThresholdValue = value.substring(212, 216);
        bcData.MaximumTarget = value.substring(216, 218);

        if (value.substring(218, 219) != "0")
            bcData.TransactionCategoryCode = value.substring(218, 219);
        else
            bcData.TransactionCategoryCode = "R";

        //Este campo é desprezado pelo pinpad, dado que estes códigos
        //foram fixados a partir da norma EMV 4.0, deixando de ser parâmetros.
        bcData.AuthRespCodeOfflineApproved = "Y1";
        bcData.AuthRespCodeOfflineDeclined = "Z1";
        bcData.AuthRespCodeUnableToGoOnlineApproved = "Y3";
        bcData.AuthRespCodeUnableToGoOnlineDeclined = "Z3";

        bcData.TerminalCapabilitiesAID = "0"; //Nao suporta contactless
        bcData.TerminalContactlessFloorLimit = "00000000";
        bcData.TerminalContactlessLimit = "00000000";
        bcData.TerminalCVMRequiredLimit = "00000000";
        bcData.PaypassMagstripeVersionNumber = "0000";
        bcData.SelectContactlessApp = "0";
        bcData.TACContactlessDefault = "0000000000";
        bcData.TACContactlessDenial = "0000000000";
        bcData.TACContactlessOnline = "0000000000";

        //Dados tabela estabelecimento
        bcData.TransactionCurrencyCode = listoData.currencyCode;
        bcData.TransactionCurrencyExponent = listoData.currencyExponent;
        bcData.TerminalCountryCode = listoData.terminalCountryCode;
        bcData.MerchantCategoryCode = listoData.merchantCategoryCode;
        //bcData.MerchantId = bcDataBC.IdMerchant;
        //bcData.TerminalId = bcDataBC.TerminalId;
        //bcData.TerminalId = "00000000";

        //Na especificacao 0102D a GlobalPayments informa que o tamanho eh em bytes, porem, esta
        //enviando como qtde de caracteres decimal
        //Abaixo o valor esta sendo convertido em bytes
        int AIDSizeHex = Integer.parseInt(bcData.AIDSize);
        bcData.AIDSize = cf.padLeft(String.valueOf(AIDSizeHex / 2), 2, "0");
        
        return bcData;
	}
	
	private String getPublicKeys(String value)
	{
		String registry = "";
		
		listoData.indiceTabPublicKeys++;
		BCDataPublicKeys key = getBCDataPublicKeys(listoData.indiceTabPublicKeys, value);
		
		registry = key.IdTable +
                DATA_POSITION_GP_PINPAD +
                key.TableIndex +
                key.RID +
                key.CAPublicKeyIndex +
                key.Reserved1 +
                key.SizeCAPublicKeyExp +
                key.CAPublicKeyExp +
                key.SizeCAPublicKeyMod +
                key.CAPublicKeyMod +
                key.StatusCheckSum +
                key.CAPublicKeyCheckSum +
                key.Reserved2;
			
		registry = cf.padLeft(String.valueOf(registry.length() + 3), 3, "0") + registry;
		
		return registry;
	}
	
	
	public BCDataPublicKeys getBCDataPublicKeys(int id, String value) {
		BCDataPublicKeys data = new BCDataPublicKeys();
		
		data.IdTable = "2"; //Referencia biblioteca compartilhada
        data.TableIndex = cf.padLeft(String.valueOf(id), 2, "0");
        data.RID = value.substring(0, 10);
        data.CAPublicKeyIndex = value.substring(10, 12);
        data.SizeCAPublicKeyExp = value.substring(14, 15);//value.substring(12, 13);
        data.CAPublicKeyExp = cf.padRight(value.substring(13, 15), 6, "0");
        data.SizeCAPublicKeyMod = value.substring(15, 18);

        int sizeCAPublicKey = Integer.parseInt(data.SizeCAPublicKeyMod);
        if (sizeCAPublicKey > 0)
            data.CAPublicKeyMod = cf.padRight(value.substring(18, sizeCAPublicKey + 18), 496, "0");
        else
        	data.CAPublicKeyMod = cf.padRight(data.CAPublicKeyMod, 496, "0");

        //Tamanho - BYTES
        //Na especificacao 0102D a GlobalPayments informa que o tamanho eh em bytes, porem, esta
        //enviando como qtde de caracteres decimal	
        //Abaixo o valor esta sendo convertido em bytes
        int sizeCAPublicKeyExp = Integer.parseInt(data.SizeCAPublicKeyExp);
        data.SizeCAPublicKeyExp = String.valueOf(sizeCAPublicKeyExp / 2); //Uma posicao apenas

        //Tamanho - BYTES
        //Na especificacao 0102D a GlobalPayments informa que o tamanho eh em bytes, porem, esta
        //enviando como qtde de caracteres decimal
        //Abaixo o valor esta sendo convertido em bytes
        int sizeCAPublicKeyMod = Integer.parseInt(data.SizeCAPublicKeyMod);
        data.SizeCAPublicKeyMod = cf.padLeft(String.valueOf(sizeCAPublicKeyMod / 2), 2, "0");

        data.StatusCheckSum = value.substring(18 + sizeCAPublicKey, (1 + 18 + sizeCAPublicKey));
        data.CAPublicKeyCheckSum = value.substring(19 + sizeCAPublicKey, (40 + 19 + sizeCAPublicKey)); //BC aceita 40, os outros 8 sao zeros a esquerda

        data.Reserved1 = "00"; //Reservado uso futuro
        data.Reserved2 = "000000000000000000000000000000000000000000"; //Reservado uso futuro
        
        return data;
	}
	
	private ISOMsg getCommonBitsFormatted(ISOMsg msg, long nsu) throws ISOException {
		ISOMsg isomsg = msg;
		
		CommonFunctions commonFunctions =  new CommonFunctions();
		Calendar trsdate = commonFunctions.getCurrentDate();		
		
		if (!msg.hasField(7))
		{			
			isomsg.set(7, commonFunctions.padLeft(String.valueOf(trsdate.get(Calendar.MONTH) + 1), 2, "0") +			
					commonFunctions.padLeft(String.valueOf(trsdate.get(Calendar.DAY_OF_MONTH)), 2, "0") + 
					commonFunctions.padLeft(String.valueOf(trsdate.get(Calendar.HOUR_OF_DAY)), 2, "0") + 
					commonFunctions.padLeft(String.valueOf(trsdate.get(Calendar.MINUTE)), 2, "0") +
					commonFunctions.padLeft(String.valueOf(trsdate.get(Calendar.SECOND)), 2, "0"));								
		}		
		else
			isomsg.set(7, msg.getValue(7).toString());
		
		if (!msg.hasField(11)) //NSU TEF
			isomsg.set(11, commonFunctions.padLeft(String.valueOf(nsu), 6, "0"));
		else
			isomsg.set(11, msg.getValue(11).toString());
		
		if (!msg.hasField(12)) //hora local
		{
			isomsg.set(12, commonFunctions.padLeft(String.valueOf(trsdate.get(Calendar.HOUR_OF_DAY)), 2, "0") + 
					commonFunctions.padLeft(String.valueOf(trsdate.get(Calendar.MINUTE)), 2, "0") +
					commonFunctions.padLeft(String.valueOf(trsdate.get(Calendar.SECOND)), 2, "0"));		
		}	
		else
			isomsg.set(12, msg.getValue(12).toString());
		
		if (!msg.hasField(13)) //data local
		{
			isomsg.set(13, commonFunctions.padLeft(String.valueOf(trsdate.get(Calendar.MONTH) + 1), 2, "0") +			
					commonFunctions.padLeft(String.valueOf(trsdate.get(Calendar.DAY_OF_MONTH)), 2, "0"));
		}
		else
			isomsg.set(13, msg.getValue(13).toString());
		
		return isomsg;
	}
	
	public TransactionData getResponseData(ISOMsg message) {
		TransactionData data = new TransactionData();
		
		if (message == null)
			return null;
		
		if (message.hasField(FIELD_PAN))
			data.pan = message.getString(FIELD_PAN);
		if (message.hasField(FIELD_PROC_CODE))
			data.processingCode = message.getString(FIELD_PROC_CODE);
		if (message.hasField(FIELD_AMOUNT))
			data.amount = message.getString(FIELD_AMOUNT);
		if (message.hasField(FIELD_DATE_TIME))
			data.dateTime = message.getString(FIELD_DATE_TIME);
		if (message.hasField(FIELD_NSU_TEF))
			data.nsuTef = message.getString(FIELD_NSU_TEF);
		if (message.hasField(FIELD_DATE))
			data.date = message.getString(FIELD_DATE);
		if (message.hasField(FIELD_TIME))
			data.time = message.getString(FIELD_TIME);
		if (message.hasField(FIELD_ENTRY_MODE))
			data.entryMode = message.getString(FIELD_ENTRY_MODE);
		if (message.hasField(FIELD_PAN_SEQUENCE))
			data.panSequence = message.getString(FIELD_PAN_SEQUENCE);
		if (message.hasField(FIELD_TRACK_2))
			data.cardTrack2 = message.getString(FIELD_TRACK_2);
		if (message.hasField(FIELD_TERMINAL_CODE))
			data.terminalCode = message.getString(FIELD_TERMINAL_CODE);
		if (message.hasField(FIELD_MERCHANT_CODE))
			data.merchantCode = message.getString(FIELD_MERCHANT_CODE);
		if (message.hasField(FIELD_ADDITIONAL_DATA_1)) {
			HashMap<String, String> map = cf.tlvExtractData(message.getString(FIELD_ADDITIONAL_DATA_1));
			if (map.containsKey(TAG_AD1_TABLES_VERSION))
				data.tablesVersion = map.get(TAG_AD1_TABLES_VERSION);
			if (map.containsKey(TAG_AD1_ENCRYPTED_PAN))
				data.pan = map.get(TAG_AD1_ENCRYPTED_PAN);
			if (map.containsKey(TAG_AD1_INSTALLMENTS))
				data.installments = map.get(TAG_AD1_INSTALLMENTS);
		}
		if (message.hasField(FIELD_AUTHORIZATION_CODE))
			data.authorizationCode = message.getString(FIELD_AUTHORIZATION_CODE);
		if (message.hasField(FIELD_RESPONSE_CODE))
			data.responseCode = message.getString(FIELD_RESPONSE_CODE);		
		if (message.hasField(FIELD_CURRENCY_CODE))
			data.currencyCode = message.getString(FIELD_CURRENCY_CODE);
		if (message.hasField(FIELD_PIN))
			data.pin = message.getString(FIELD_PIN);
		if (message.hasField(FIELD_EMV_DATA)) {
			data.emvData = message.getString(FIELD_EMV_DATA);
			//Remove os 6 primeiros digitos para o finishchip
			data.emvData = data.emvData.substring(6,  data.emvData.length());
		}
		if (message.hasField(FIELD_TERMINAL_DATA)) {
			HashMap<String, String> map = cf.tlvExtractData(message.getString(FIELD_TERMINAL_DATA));
			if (map.containsKey(TAG_TD_PINPAD_SERIAL_NUMBER))
				data.pinpadSerialNumber = map.get(TAG_TD_PINPAD_SERIAL_NUMBER);
			if (map.containsKey(TAG_TD_PINPAD_APP_VERSION))
				data.pinpadSerialNumber = map.get(TAG_TD_PINPAD_APP_VERSION);
			if (map.containsKey(TAG_TD_PINPAD_MAMNUFACTURER))
				data.pinpadSerialNumber = map.get(TAG_TD_PINPAD_MAMNUFACTURER);
			if (map.containsKey(TAG_TD_PINPAD_MODEL))
				data.pinpadSerialNumber = map.get(TAG_TD_PINPAD_MODEL);
			if (map.containsKey(TAG_TD_PINPAD_FIRMWARE))
				data.pinpadSerialNumber = map.get(TAG_TD_PINPAD_FIRMWARE);
		}
		if (message.hasField(FIELD_SECURITY_DATA)) {
			HashMap<String, String> map = cf.tlvExtractData(message.getString(FIELD_SECURITY_DATA));
			if (map.containsKey(TAG_ENCRYPTION_PIN_TYPE))
				data.encryptionPinType = map.get(TAG_ENCRYPTION_PIN_TYPE);	
			if (map.containsKey(TAG_ENCRYPTION_PIN_KSN))
				data.ksnPin = map.get(TAG_ENCRYPTION_PIN_KSN);
			if (map.containsKey(TAG_ENCRYPTION_CARD_TYPE))
				data.encryptionCardType = map.get(TAG_ENCRYPTION_CARD_TYPE);	
			if (map.containsKey(TAG_ENCRYPTION_CARD_KSN))
				data.ksnCard = map.get(TAG_ENCRYPTION_CARD_KSN);	
			if (map.containsKey(TAG_TYPE_CARD_VERIFICATION_DATA))
				data.typeCardVerificationData = map.get(TAG_TYPE_CARD_VERIFICATION_DATA);
			if (map.containsKey(TAG_CARD_VERIFICATION_DATA))
				data.cardVerificationData = map.get(TAG_CARD_VERIFICATION_DATA);
		}
		
		if (message.hasField(FIELD_ADDITIONAL_DATA_2))
			data.cardholderReceipt = message.getString(FIELD_ADDITIONAL_DATA_2);
		
		if (message.hasField(FIELD_TRANSACTION_DATA_1)) 
			data.merchantReceipt = message.getString(FIELD_TRANSACTION_DATA_1);
		
		if (message.hasField(FIELD_NSU_ACQUIRER))
			data.nsuAcquirer = message.getString(FIELD_NSU_ACQUIRER);
		
		return data;
	}
	
	public TransactionData requestPayment(TransactionData requestData) {
		TransactionData responseData = null;
		
		try {
			
			ISOMsg request = getMessage0200(requestData);
			ISOMsg response = requestAcquirer(request);	
			responseData = getResponseData(response);
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
		return responseData;
	}
	
	public TransactionData requestConfirmation(TransactionData requestData) {
		TransactionData responseData = null;
		
		try {
			
			ISOMsg request = getMessage9820(requestData);
			ISOMsg response = requestAcquirer(request);	
			responseData = getResponseData(response);
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
		return responseData;
	}
	
	public TransactionData requestAdvice(TransactionData requestData) {
		TransactionData responseData = null;
		
		try {
			
			ISOMsg request = getMessage0220(requestData);
			ISOMsg response = requestAcquirer(request);	
			responseData = getResponseData(response);
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
		return responseData;
	}
	
	public TransactionData requestCancellation(TransactionData requestData) {
		TransactionData responseData = null;
		
		try {
			
			ISOMsg request = getMessage0400(requestData);
			ISOMsg response = requestAcquirer(request);	
			responseData = getResponseData(response);
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
		return responseData;
	}
	
	public TransactionData requestUnmaking(TransactionData requestData) {
		TransactionData responseData = null;
		
		try {
			
			ISOMsg request = getMessage0420(requestData);
			ISOMsg response = requestAcquirer(request);	
			responseData = getResponseData(response);
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
		return responseData;
	}
	
	private ISOMsg getMessage0200(TransactionData requestData) throws ISOException {
		ISOMsg request = new ISOMsg();
		
		request.setPackager(new ISO87APackagerGP());
		request.setMTI(REQ_GP_PAYMENT);
		
		if ((requestData.pan.length() > 0) && 
			(requestData.typeCardRead.equals(ListoData.TRANSACTION_ENTERED))) {
			request.set(FIELD_PAN, requestData.pan);	
		}
		
		request.set(FIELD_PROC_CODE, getProcessingCode(requestData.processingCode));
		request.set(FIELD_AMOUNT, requestData.amount);
		request.set(FIELD_DATE_TIME, requestData.dateTime);
		request.set(FIELD_NSU_TEF, requestData.nsuTef);
		request.set(FIELD_DATE, requestData.date);
		request.set(FIELD_TIME, requestData.time);
		
		if ((requestData.expirationDateCard.length() > 0) &&
			(requestData.typeCardRead.equals(ListoData.MAGNETIC)))
			request.set(FIELD_CARD_EXPIRATION_DATE, requestData.expirationDateCard);
		
		String entryMode = requestData.entryMode;
		if (entryMode.equals("059"))
			entryMode = "051";
		
		if (entryMode.equals("028"))
			entryMode = "021";
		
		request.set(FIELD_ENTRY_MODE, entryMode);
		
		if (requestData.panSequence.length() > 0)
			request.set(FIELD_PAN_SEQUENCE, requestData.panSequence);
	
		if (requestData.cardTrack2.length() > 0) {
			int index = requestData.cardTrack2.indexOf("=");
			String bin = requestData.cardTrack2.substring(0, 6);
			String track2 = bin + cf.padLeft(new String(), index - 6, "0") +
				   requestData.cardTrack2.substring(index, requestData.cardTrack2.length());
			request.set(FIELD_TRACK_2, track2);
		}
		
		request.set(FIELD_TERMINAL_CODE, requestData.terminalCode);
		request.set(FIELD_MERCHANT_CODE, requestData.merchantCode);
		request.set(FIELD_ADDITIONAL_DATA_1, getTransactionAdditionalData(REQ_GP_PAYMENT, requestData));
		request.set(FIELD_CURRENCY_CODE, requestData.currencyCode);

		if (requestData.pin.length() > 0)
			request.set(FIELD_PIN, requestData.pin);
		
		if (requestData.emvData.length() > 0)
			request.set(FIELD_EMV_DATA, requestData.emvData);
		
		request.set(FIELD_TERMINAL_DATA, getTerminalData(REQ_GP_PAYMENT, requestData));
		request.set(FIELD_SECURITY_DATA, getDataEncrypted(REQ_GP_PAYMENT, requestData));
		
		return request;
	}
	
	private ISOMsg getMessage9820(TransactionData requestData) throws ISOException {
		ISOMsg request = new ISOMsg();
		
		request.setPackager(new ISO87APackagerGP());
		request.setMTI(REQ_GP_CONFIRMATION);
		
		if ((requestData.pan.length() > 0) && 
			(requestData.typeCardRead.equals(ListoData.TRANSACTION_ENTERED))) {
			String pan = requestData.pan.substring(0, 6);
			pan += cf.padLeft(new String(), requestData.pan.length() - 6, "0");
			request.set(FIELD_PAN, pan);
		}
		
		request.set(FIELD_PROC_CODE, getProcessingCode(requestData.processingCode));
		request.set(FIELD_DATE_TIME, requestData.dateTime);
		request.set(FIELD_NSU_TEF, requestData.nsuTef);
		request.set(FIELD_DATE, requestData.date);
		request.set(FIELD_TIME, requestData.time);
		
		request.set(FIELD_TERMINAL_CODE, requestData.terminalCode);
		request.set(FIELD_MERCHANT_CODE, requestData.merchantCode);
		
		request.set(FIELD_TERMINAL_DATA, getTerminalData(REQ_GP_CONFIRMATION, requestData));
		request.set(FIELD_CONFIRMATION_DATA, requestData.confirmationData);
		
		return request;
	}
	
	private ISOMsg getMessage0220(TransactionData requestData) throws ISOException {
		ISOMsg request = new ISOMsg();
		
		request.setPackager(new ISO87APackagerGP());
		request.setMTI(REQ_GP_ADVICE);
		
		request.set(FIELD_PAN, requestData.pan);	
		request.set(FIELD_PROC_CODE, getProcessingCode(requestData.processingCode));
		request.set(FIELD_AMOUNT, requestData.amount);
		request.set(FIELD_DATE_TIME, requestData.dateTime);
		request.set(FIELD_NSU_TEF, requestData.nsuTef);
		request.set(FIELD_DATE, requestData.date);
		request.set(FIELD_TIME, requestData.time);
		
		if (requestData.expirationDateCard.length() > 0)
			request.set(FIELD_CARD_EXPIRATION_DATE, requestData.expirationDateCard);
		
		request.set(FIELD_ENTRY_MODE, "010"); //Advice
		
		if (requestData.panSequence.length() > 0)
			request.set(FIELD_PAN_SEQUENCE, requestData.panSequence);
		
		//Se a transacao foi aprovada, enviar o codigo
		if (requestData.responseCode.equals("00"))
			request.set(FIELD_AUTHORIZATION_CODE, requestData.authorizationCode);
		
		if ((requestData.responseCode.length() > 0) &&
			(!requestData.responseCode.equals("00")))
			request.set(FIELD_RESPONSE_CODE, requestData.responseCode);
		
		request.set(FIELD_TERMINAL_CODE, requestData.terminalCode);
		request.set(FIELD_MERCHANT_CODE, requestData.merchantCode);
		request.set(FIELD_ADDITIONAL_DATA_1, getTransactionAdditionalData(REQ_GP_ADVICE, requestData));
		request.set(FIELD_CURRENCY_CODE, requestData.currencyCode);
		
		if (requestData.emvData.length() > 0)
			request.set(FIELD_EMV_DATA, requestData.emvData);
		
		request.set(FIELD_TERMINAL_DATA, getTerminalData(REQ_GP_ADVICE, requestData));
		request.set(FIELD_SECURITY_DATA, getDataEncrypted(REQ_GP_ADVICE, requestData));
		
		return request;
	}
	
	private ISOMsg getMessage0400(TransactionData requestData) throws ISOException {	
		ISOMsg request = new ISOMsg();
		
		request.setPackager(new ISO87APackagerGP());
		request.setMTI(REQ_GP_CANCELLATION);
		
		if ((requestData.pan.length() > 0) && 
			(requestData.typeCardRead.equals(ListoData.TRANSACTION_ENTERED))) {
			request.set(FIELD_PAN, requestData.pan);	
		}
		request.set(FIELD_PROC_CODE, getProcessingCode(requestData.processingCode));
		request.set(FIELD_AMOUNT, requestData.amount);
		request.set(FIELD_DATE_TIME, requestData.dateTime);
		request.set(FIELD_NSU_TEF, requestData.nsuTef);
		request.set(FIELD_DATE, requestData.date);
		request.set(FIELD_TIME, requestData.time);
		
		if ((requestData.expirationDateCard.length() > 0) &&
			(requestData.typeCardRead.equals(ListoData.MAGNETIC)))
			request.set(FIELD_CARD_EXPIRATION_DATE, requestData.expirationDateCard);
		
		String entryMode = requestData.entryMode;
		if (entryMode.equals("059"))
			entryMode = "051";
		request.set(FIELD_ENTRY_MODE, entryMode);
		
		if (requestData.panSequence.length() > 0)
			request.set(FIELD_PAN_SEQUENCE, requestData.panSequence);
	
		if (requestData.cardTrack2.length() > 0) {
			int index = requestData.cardTrack2.indexOf("=");
			String bin = requestData.cardTrack2.substring(0, 6);
			String track2 = bin + cf.padLeft(new String(), index - 6, "0") +
				   requestData.cardTrack2.substring(index, requestData.cardTrack2.length());
			request.set(FIELD_TRACK_2, track2);
		}
		
		request.set(FIELD_TERMINAL_CODE, requestData.terminalCode);
		request.set(FIELD_MERCHANT_CODE, requestData.merchantCode);
		request.set(FIELD_ADDITIONAL_DATA_1, getTransactionAdditionalData(REQ_GP_CANCELLATION, requestData));
		request.set(FIELD_CURRENCY_CODE, requestData.currencyCode);
		request.set(FIELD_TERMINAL_DATA, getTerminalData(REQ_GP_CANCELLATION, requestData));
		request.set(FIELD_ORIGINAL_DATA, getOriginalTransaction(requestData));
		request.set(FIELD_SECURITY_DATA, getDataEncrypted(REQ_GP_CANCELLATION, requestData));
		request.set(FIELD_NSU_ACQUIRER, requestData.nsuAcquirer);
		
		return request;
	}
	
	private ISOMsg getMessage0420(TransactionData requestData) throws ISOException {
		ISOMsg request = new ISOMsg();
		
		request.setPackager(new ISO87APackagerGP());
		request.setMTI(REQ_GP_UNMAKING);
		
		request.set(FIELD_PROC_CODE, getProcessingCode(requestData.processingCode));
		request.set(FIELD_AMOUNT, requestData.amount);
		request.set(FIELD_DATE_TIME, requestData.dateTime);
		request.set(FIELD_NSU_TEF, requestData.nsuTef);
		request.set(FIELD_DATE, requestData.date);
		request.set(FIELD_TIME, requestData.time);
		
		if ((requestData.expirationDateCard.length() > 0) &&
			(requestData.typeCardRead.equals(ListoData.MAGNETIC)))
			request.set(FIELD_CARD_EXPIRATION_DATE, requestData.expirationDateCard);
		
		String entryMode = requestData.entryMode;
		if (entryMode.equals("059"))
			entryMode = "051";
		request.set(FIELD_ENTRY_MODE, entryMode);
		
		if (requestData.panSequence.length() > 0)
			request.set(FIELD_PAN_SEQUENCE, requestData.panSequence);
		
		request.set(FIELD_TERMINAL_CODE, requestData.terminalCode);
		request.set(FIELD_MERCHANT_CODE, requestData.merchantCode);
		request.set(FIELD_CURRENCY_CODE, requestData.currencyCode);		
		request.set(FIELD_TERMINAL_DATA, getTerminalData(REQ_GP_UNMAKING, requestData));
		request.set(FIELD_ORIGINAL_DATA, getOriginalTransaction(requestData));
		request.set(FIELD_SECURITY_DATA, getDataEncrypted(REQ_GP_UNMAKING, requestData));
		
		return request;
	}
	
	private String getProcessingCode(String value){
		switch (value) {
		case ListoData.PROC_REQ_CREDIT:
		case ListoData.PROC_REQ_CREDIT_WITH_INTEREST:
		case ListoData.PROC_REQ_CREDIT_WITHOUT_INTEREST:
			return PROC_CODE_CREDIT;
		case ListoData.PROC_REQ_DEBIT:
			return PROC_CODE_DEBIT;
		}
		return null;
	}
	
	private String getFunctionCode(String value){
		String code = "";
		switch (value) {
		case ListoData.PROC_REQ_CREDIT:
			code = FUNC_CREDIT;
			break;
		case ListoData.PROC_REQ_CREDIT_WITH_INTEREST:
			code = FUNC_CREDIT_WITH_INTEREST;
			break;
		case ListoData.PROC_REQ_CREDIT_WITHOUT_INTEREST:
			code = FUNC_CREDIT_WITHOUT_INTEREST;
			break;
		case ListoData.PROC_REQ_DEBIT:
			code = FUNC_DEBIT;
			break;
		}
		return code;
	}
	
	private String getTransactionAdditionalData(String mti, TransactionData data){
		String bit048 = "";

		if ((!mti.equals(REQ_GP_CANCELLATION)) && (!mti.equals(REQ_GP_UNMAKING)))
			bit048 = "003002" + getFunctionCode(data.processingCode);

        //criptografia de pinpad habilitada - PAN criptografado pelo wkpan      
        bit048 += "005" + cf.padLeft(String.valueOf(data.panPartEncrypted.length()), 3, "0") + 
        								 data.panPartEncrypted;

        //A transacao eh parcelada - adiciona subcampo 6
        if ((data.processingCode.equals(ListoData.PROC_REQ_CREDIT_WITH_INTEREST)) ||
        	(data.processingCode.equals(ListoData.PROC_REQ_CREDIT_WITHOUT_INTEREST)))
            bit048 += "006002" + cf.padLeft(data.installments, 2, "0");
		
		return bit048;
	}
	
	private String getTerminalData(String mti, TransactionData data){
		//Formatando BIT061 TLV
        //Tag=001,Length=008,value=GP + versao_aplicacao + identificacao_listo=LI
		String bit061 = "001008" + VERSAO_ESPEC_GP;
		
		if (mti.equals(REQ_GP_CONFIRMATION) ||
			mti.equals(REQ_GP_UNMAKING))
            return bit061;
		
		 //verifica se usou o pinpad
		if (data.typeCardRead.equals(ListoData.MAGNETIC) ||
			data.typeCardRead.equals(ListoData.EMV_CONTACT))
            bit061 += "002020" + cf.padRight(data.pinpadSerialNumber, 20, " ");
        
        //Subcampo 003 - APENAS PARA CARTOES MASTERCARD              
        if (data.productDescription.toUpperCase().contains("MASTER") || 
            data.productDescription.toUpperCase().contains("MAESTRO"))
        {
        	String pointAuthLifeCycle = "00";
        	String pointOfServicePostalCode = "          ";
            bit061 += "003026" + "00000000008" + pointAuthLifeCycle +
            		  data.countryCode + pointOfServicePostalCode;
        }
        
        if ((data.typeCardRead.equals(ListoData.MAGNETIC)) || 
        	(data.typeCardRead.equals(ListoData.EMV_CONTACT))) //verifica se usou o pinpad
            bit061 += "004016" + data.pinpadVersionBasicApp;      

        bit061 += "005020" + data.pinpadManufacturer;   //Fabricante Pinpad
        bit061 += "006019" + data.pinpadModel;          //Modelo Pinpad
        bit061 += "007020" + data.pinpadFirmware;       //Firmware Pinpad
        
        return bit061;
	}
	
	private String getOriginalTransaction(TransactionData data){
        String bit090 = new String();
        
        bit090 = data.originalMessageCode;		 //Codigo da mensagem original (0100, 0200 ou 0220)
        bit090 += data.originalNSUTEF;   		 //NSU da transacao original
        bit090 += data.originalDateTime;         //Data e hora da transacao original
        bit090 += "0000000000000000000000";      //22 zeros RFU
        
        return bit090;
	}
	
	private String getDataEncrypted(String mti, TransactionData data){
		String bit126 = "";
		String especificChar = " ";

        //05 - Presente se solicitado PIN online para validação do portador do cartão.         
        if ((mti.equals(REQ_GP_PAYMENT)) && (data.pin.length() > 0)) {
            bit126 += "001001" + data.encryptionPinType;
            bit126 += "002020" + data.ksnPin; //Chave do pin
        }
        
        bit126 += "003001" + data.encryptionCardType; //2 = ID da chave 3DES

        //37 - Presente se cartão (BIT002 ou 035) criptografado com chave DUKPT
        //A chave utilizada para dados eh 3DES somente
        //bit126 += "004020" + dataBC.KeyDUKPT3DES;     

        if (mti.equals(REQ_GP_PAYMENT)) {
        	String bitAux5 = "005006"; //Subcampo 5        
            bit126 += bitAux5; //adiciona o 005 e o tamanho 006
            if (data.productDescription.toUpperCase().contains("VISA") || 
                data.productDescription.toUpperCase().contains("ELECTRON"))
                especificChar = "0"; //Somente para VISA

            if (data.typeCardRead.equals(ListoData.EMV_CONTACT) || 
               (data.typeCardRead.equals(ListoData.EMV_WITHOUT_CONTACT)))  
                bit126 += "0" + especificChar + "    "; //CVV2/CVC2/CID desconsiderado ou nao fornecido (nao eh necessario para EMV)                
            else
            {
                if (data.typeCardVerificationData.equals("1")) //inexistente
                    bit126 += "9" + especificChar + "    ";
                else if (data.typeCardVerificationData.equals("2")) //ilegivel
                    bit126 += "2" + especificChar + "    ";
                else //captura do cvv
                {
                    //Solicita codigo de seguranca
                	//Somente o código normal de resposta é esperado (BIT 039) - 
                	//atualmente o host só utiliza a opção 0. 
                    bit126 += "0";
                    //dado justificado à direita com espaço)
                    bit126 += especificChar + cf.padLeft(data.cardVerificationData, 4, " ");               
                }
                //Envia o subcampo 010
                if (data.processingCode.equals(ListoData.PROC_REQ_DEBIT))
                	bit126 += "010005" + cf.padLeft(data.cardVerificationData, 5, "0");
            }
        }
        return bit126;
	}
	
	public void requestLogonProcess(ISOMsg message) {
		
		String logicalNumber = message.getString(ListoData.FIELD_MERCHANT_CODE);
		
		ISOMsg response = requestLogon(logicalNumber, AcquirerSettings.getIncrementNSUGlobalpayments());
		
		if (response != null) {
			
			String bit48 = response.getString(FIELD_ADDITIONAL_DATA_1);
			if (bit48.trim().length() > 0) {
				String versaoTabelas = bit48.substring(6, 14);
				
				//Obtem os dados de inicializacao do adquirente
				ListoData listoData = AcquirerSettings.getInitializationTables(ListoData.GLOBAL_PAYMENTS, logicalNumber);	
				
				if (listoData != null) {
					if (!versaoTabelas.equals(listoData.versaoTabelasGlobalpayments)) {		
						
						Logger.log(new LogEvent("Diferent tables version - Process of load Globalpayments tables started!"));
						
						AcquirerSettings.loadAcquirerTables(ListoData.GLOBAL_PAYMENTS, logicalNumber, 
															message.getString(ListoData.FIELD_TERMINAL_CODE), true);
					}
				}
			}
			
		}
	}

}
