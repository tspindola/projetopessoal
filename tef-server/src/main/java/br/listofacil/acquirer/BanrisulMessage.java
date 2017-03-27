package br.listofacil.acquirer;

import java.math.BigInteger;
import java.util.Calendar;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.MUX;
import org.jpos.util.NameRegistrar;
import org.jpos.util.NameRegistrar.NotFoundException;

import br.listofacil.AcquirerSettings;
import br.listofacil.CommonFunctions;
import br.listofacil.tefserver.iso.ISO87APackagerGP;
import br.listofacil.tefserver.iso.ISO93EPackagerBanrisul;

public class BanrisulMessage {

	private final String MERCHANT = "LISTO FACIL";
	private final String BANRISUL = "02";
	private final String CURRENCY_SYMBOL = "R$  ";
	
	//Comprovante
	private final String RCP_ACQUIRER_NAME = "VERO";
	private final String RCP_CREDIT = "VENDA CREDITO A VISTA";
	
	private final String BYTE_1 = "11111101";
	private final String BYTE_2 = "10000000";
	private final String BYTE_3 = "00000000";
	
	private final String PROC_CODE_LOGON_A = "001";
	private final String PROC_CODE_LOGON_F = "002";
	private final String PROC_CODE_EMV = "061";
	private final String PROC_CODE_BINS = "062";
	private final String PROC_CODE_PARAMS = "063";
	private final String PROC_CODE_MESSAGES = "064";
	private final String PROC_CODE_FLAGS = "065";
	private final String PROC_CODE_END = "999";
	
	private final String REQ_BA_LOGON_INIT = "0800";
	private final String RES_BA_LOGON_INIT = "0810";
	private final String REQ_BA_PAYMENT = "0200";
	private final String RES_BA_PAYMENT = "0210";
	
	private final int FIELD_PAN = 2;
	private final int FIELD_PROC_CODE = 3;
	private final int FIELD_AMOUNT = 4;
	private final int FIELD_DATE_TIME = 7;
	private final int FIELD_NSU_TEF = 11;
	private final int FIELD_DATE = 12;
	private final int FIELD_TIME = 13;
	private final int FIELD_CARD_EXPIRATION_DATE = 14;
	private final int FIELD_ENTRY_MODE = 22;
	private final int FIELD_PAN_SEQUENCE = 23;
	private final int FIELD_FINANCIAL_INSTITUTION = 32;
	private final int FIELD_TRACK_2 = 35;
	private final int FIELD_AUTHORIZATION_CODE = 38;
	private final int FIELD_RESPONSE_CODE = 39;
	private final int FIELD_TERMINAL_CODE = 41;
	private final int FIELD_MERCHANT_CODE = 42;
	private final int FIELD_TRACK_1 = 45;
	private final int FIELD_CURRENCY_CODE = 49;
	private final int FIELD_PIN = 52;
	private final int FIELD_SMID = 53;
	private final int FIELD_EMV_DATA = 55;
	private final int FIELD_TERMINAL_TYPE = 61;
	private final int FIELD_GENERIC_DATA_62 = 62;
	private final int FIELD_GENERIC_DATA_63 = 63;
	private final int FIELD_INSTALLMENTS = 67;
	private final int FIELD_SECURITY_CODE = 122;
	private final int FIELD_LAST_TRANSACTION = 125;
	private final int FIELD_NSU_ACQUIRER = 127;
	
	private String BANRISUL_PINPAD_DATA = "14";					//Posicao da chave de criptografia de dados
	private String BANRISUL_PINPAD_PIN = "14";					//Posicao da chave de criptografia de senha
	private String PARAM_42 = "041003500000100";				//Numero logico Banrisul
	
	private final String PARAM_62a = "00000000"; 				//timestamp-1-Data Tabela EMV - �00000000� se 1a. vez
	private final String PARAM_62b = "00"; 						//num. sequencial da versao em uso - �00000000� se 1a. vez
	
	private final String PARAM_63a = "11";						//DUKPT e Master Key Banrisul
	private final String PARAM_63b = "50";						//forma de comunicacao (50 - TCP/IP)
	private final String PARAM_63c = "001";						//tipo do equipamento - DEVERA VIR DO CLIENT		
	private final String PARAM_63d = "00003";					//versao do buffer
	private final String PARAM_63e = "LISTO_TEF_v_2.00aaaa";	//versao da especificacao do tef
	private final String PARAM_63f = "0010";					//codigo da Listo no cadastro com o BANRISUL
	
	private final String FINANCIAL_INSTITUTION_CODE = "00410035000"; //Codigo fornecido pelo BANRISUL (testes)
	private final String TAGS_EMV_MASTERCARD_VISA = "9F269F279F109F379F36959A9C9F029F035F2A829F1AF345F249F159F335F2884";
	private final String TAGS_EMV_BANRISUL = "9F1A959C829F109F269F279F369F3784"; 
	
	/*
	003000 � compra credito a vista 
	003100 � compra credito parcelado lojista 
	003800 � compra credito parcelado emissor 
	002000 � compra debito a vista 
	*/
	private final String PROC_CODE_CREDIT = "003000";
	private final String PROC_CODE_CREDIT_WITHOUT_INTEREST = "003100"; //Parcelado lojista
	private final String PROC_CODE_CREDIT_WITH_INTEREST = "003800";	   //Parcelado emissor
	private final String PROC_CODE_DEBIT = "002000";
	
	private static final String idMUXBanrisul = "mux.clientsimulator-banrisul-mux";
	
	ListoData listoData = new ListoData();
	CommonFunctions cf = new CommonFunctions();
	
	boolean firstEmvReg = false;	
	
	public boolean loadTablesInitialization(String logicalNumber, String terminalNumber) throws ISOException, Exception{		
		
		ISOMsg response = null;
		String timestampTables;
		boolean ret = true;
		
		//Nao utiliza o numero do terminal
		//PARAM_41 = terminalNumber;
		
		AcquirerSettings.setStatusLoadingBanrisul(logicalNumber);
			
		//Efetuar o logon
		response = requestLogon(logicalNumber, AcquirerSettings.getIncrementNSUBanrisul());

		if (response != null) {
			
			if (setResDataLogon(response)) {
				
				//Configura algumas tabelas que nao existem na 
				//inicializacao do Banrisul
				setMerchantData(response);
				
				while(true) {
					
					response = requestTable(response, AcquirerSettings.getIncrementNSUBanrisul());
					
					//Timeout
					if (response == null)
					{
						System.out.println("Banrisul - Connection Timed out!");
						AcquirerSettings.removeStatusLoadingBanrisul(logicalNumber);
						return false;
					}
												
					if (!setResDataInitialization(response)) 
						break;				
				}
				
				//Seta os dados de criptografia
				setEspecificData();
				
				//Seta inicializacao na estrutura padrao Listo
				AcquirerSettings.setInitializationTables(BANRISUL, logicalNumber, listoData); 
				
			} else {
				System.out.println("Banrisul - Falha nos dados do logon!");
				ret = false;
			}
		} else {
			System.out.println("Banrisul - Connection Timed out!");
			ret = false;
		}
		
		AcquirerSettings.removeStatusLoadingBanrisul(logicalNumber);
		
		return ret;
	}
	
	public ISOMsg requestEMV(ISOMsg request, ISOMsg response)
	{	
		ISOMsg isomsg = request;
		
		try {
			
			//Caso o response seja 999 - ultimo registro, carrega tabela de BINS
			if ((response.getValue(62).toString().trim().equals("")) || 
				(response.getValue(62).toString().substring(0,  3).equals("999")))
				return requestBINS(request, response);
			
			isomsg.setPackager(request.getPackager());
			String tableCode = response.getValue(70).toString();
			
			isomsg.setMTI(REQ_BA_LOGON_INIT);
			
			String bit62 = PARAM_62a;						//Timestamp-1-Data Tabela EMV - �00000000� se 1a. vez
			bit62 += PARAM_62b; 							//Num. sequencial da versao em uso - �00� se 1a. vez
			
			//Indice de controle EMV - numero da ultima tabela enviada
			if (tableCode.equals("001"))		//Caso o response anterior seja inicializacao
			{
				bit62 += "000";
				tableCode = PROC_CODE_EMV;
			}
			else 
				bit62 += response.getValue(62).toString().substring(0,  3); 
			isomsg.set(62, bit62);							//bit62
			
			String bit63 = getTerminalData();
			isomsg.set(63, bit63);							//bit63
			
			isomsg.set(70, tableCode);		//Codigo de gerenciamento = 001 Abertura
			
		} catch (ISOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return request;
		}
		
		return isomsg;
	}
	
	public ISOMsg requestBINS(ISOMsg request, ISOMsg response)
	{	
		ISOMsg isomsg = request;
		
		try {
			
			//Caso o response seja S - ultimo registro
			if (!response.getValue(70).toString().substring(0, 3).equals(PROC_CODE_EMV))
			{
				if ((response.getValue(62).toString().trim().equals("")) || 
					(response.getValue(62).toString().substring(10, 11).equals("N")))
					return requestParameters(request, response);
			}
			
			isomsg.setPackager(request.getPackager());
			String tableCode = response.getValue(70).toString();
			
			isomsg.setMTI(REQ_BA_LOGON_INIT);
			
			String bit62 = PARAM_62a;						//Timestamp-1-Data Tabela EMV - �00000000� se 1a. vez
			bit62 += PARAM_62b; 							//Num. sequencial da versao em uso - �00� se 1a. vez
			
			//Bin inicial
			if (response.getValue(70).toString().substring(0,  3).equals(PROC_CODE_EMV))	
			{
				bit62 += "000000"; //Bin inicial
				bit62 += "000000"; //Bin final
				bit62 += " "; 	   //Flag credito/debito
				bit62 += " "; 	   //Flag servico
				tableCode = PROC_CODE_BINS;
			}
			else 
			{
				bit62 += response.getValue(62).toString().substring(14,  28);
			}
			isomsg.set(62, bit62);							//bit62
			
			String bit63 = getTerminalData();
			isomsg.set(63, bit63);							//bit63
			
			isomsg.set(70, tableCode);		//Codigo de gerenciamento = 001 Abertura
			
		} catch (ISOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return request;
		}
		
		return isomsg;
	}
	
	public ISOMsg requestParameters(ISOMsg request, ISOMsg response)
	{	
		ISOMsg isomsg = request;
		
		try {
			
			//Caso o response seja S - ultimo registro
			if (!response.getValue(70).toString().substring(0,  3).equals(PROC_CODE_BINS))
			{
				if ((response.getValue(62).toString().trim().equals("")) || 
					(response.getValue(62).toString().substring(10,  11).equals("N")))
					return requestMessages(request, response);
			}
			
			isomsg.setPackager(request.getPackager());
			String tableCode = response.getValue(70).toString();
			
			isomsg.setMTI(REQ_BA_LOGON_INIT);
			
			String bit62 = PARAM_62a;						//Timestamp-1-Data Tabela EMV - �00000000� se 1a. vez
			bit62 += PARAM_62b; 							//Num. sequencial da versao em uso - �00� se 1a. vez
			
			if (response.getValue(70).toString().substring(0,  3).equals(PROC_CODE_BINS))	
			{
				bit62 += "000"; //Codigo da bandeira
				tableCode = PROC_CODE_PARAMS;
			}
			else 
			{
				bit62 += response.getValue(62).toString().substring(10,  13);
			}
			isomsg.set(62, bit62);							//bit62
			
			String bit63 = getTerminalData();
			isomsg.set(63, bit63);							//bit63
			
			isomsg.set(70, tableCode);		//Codigo de gerenciamento = 001 Abertura
			
		} catch (ISOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return request;
		}
		
		return isomsg;
	}
	
	public ISOMsg requestMessages(ISOMsg request, ISOMsg response)
	{	
		ISOMsg isomsg = request;
		
		try {
			
			//Caso o response seja S - ultimo registro
			if (!response.getValue(70).toString().substring(0,  3).equals(PROC_CODE_PARAMS))
			{
				if ((response.getValue(62).toString().trim().equals("")) || 
					(response.getValue(62).toString().substring(10,  11).equals("N")))
					return requestFlags(request, response);
			}
			
			isomsg.setPackager(request.getPackager());
			String tableCode = response.getValue(70).toString();
			
			isomsg.setMTI(REQ_BA_LOGON_INIT);
			
			String bit62 = PARAM_62a;						//Timestamp-1-Data Tabela EMV - �00000000� se 1a. vez
			bit62 += PARAM_62b; 							//Num. sequencial da versao em uso - �00� se 1a. vez
			
			if (response.getValue(70).toString().substring(0,  3).equals(PROC_CODE_PARAMS))	
			{
				bit62 += "00"; //Codigo da bandeira
				tableCode = PROC_CODE_MESSAGES;
			}
			else 
			{
				//obtem o codigo da ultima mensagem
				String messageCode = response.getValue(62).toString();
				bit62 += messageCode.substring(messageCode.length() - 42,  (messageCode.length() - 40));
				//40 = tamanho da descricao da mensagem
				//42 = 40 +2 do codigo da mensagem (2 posicoes)
			}
			isomsg.set(62, bit62);							//bit62
			
			String bit63 = getTerminalData();
			isomsg.set(63, bit63);							//bit63
			
			isomsg.set(70, tableCode);		//Codigo de gerenciamento = 001 Abertura
			
		} catch (ISOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return request;
		}
		
		return isomsg;
	}
	
	public ISOMsg requestFlags(ISOMsg request, ISOMsg response)
	{	
		ISOMsg isomsg = request;
		
		try {
			
			isomsg.setPackager(request.getPackager());
			String tableCode = response.getValue(70).toString();
			
			isomsg.setMTI(REQ_BA_LOGON_INIT);
			
			String bit62 = PARAM_62a;						//Timestamp-1-Data Tabela EMV - �00000000� se 1a. vez
			bit62 += PARAM_62b; 							//Num. sequencial da versao em uso - �00� se 1a. vez
			
			if (response.getValue(70).toString().substring(0,  3).equals(PROC_CODE_MESSAGES))	
			{
				bit62 += "000"; //Codigo da bandeira
				tableCode = PROC_CODE_FLAGS;
			}
			else 
			{
				//Obtem o ultimo codigo da bandeira recebido
				bit62 += response.getValue(62).toString().substring(13,  16);
			}
			isomsg.set(62, bit62);							//bit62
			
			String bit63 = getTerminalData();
			isomsg.set(63, bit63);							//bit63
			
			isomsg.set(70, tableCode);		//Codigo de gerenciamento = 001 Abertura
			
		} catch (ISOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return request;
		}
		
		return isomsg;
	}
	
	public ISOMsg requestTable(ISOMsg response, int nsu)
	{
		String tableCode = PROC_CODE_LOGON_A;
		ISOMsg request = new ISOMsg();
		PARAM_42 = response.getString(42);
		
		try {
			
			request.setPackager(new ISO93EPackagerBanrisul());
			//Obtem os bits 007, 011, 012 e 013
			request = getCommonBitsFormatted(request, AcquirerSettings.getIncrementNSUBanrisul());	
			request.set(42, PARAM_42);
			
			tableCode = response.getValue(70).toString();
			if (response.getValue(70).toString().equals(PROC_CODE_LOGON_A)) {	
				tableCode = PROC_CODE_EMV;
			}
		
			switch (tableCode) {
				
			case PROC_CODE_EMV:
				request = requestEMV(request, response);
				break;
				
			case PROC_CODE_BINS:
				request = requestBINS(request, response);
				break;
				
			case PROC_CODE_PARAMS:
				request = requestParameters(request, response);
				break;
				
			case PROC_CODE_MESSAGES:
				request = requestMessages(request, response);
				break;

			case PROC_CODE_FLAGS:
				request = requestFlags(request, response);
				break;
			}
			
			response = requestAcquirer(request);
			
		} catch (ISOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return request;
		}
		
		return response;
	}
	
	public ISOMsg getFormattedPaymentMsg(ISOMsg m) {
		ISOMsg isomsg = null;
		
		try {
			
			
			
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		return isomsg;
	}
	
	public ISOMsg requestLogon(String logicalNumber, int nsu)
	{
		ISOMsg request = new ISOMsg();
		ISOMsg response = null;
		try {
			
			request.setPackager(new ISO93EPackagerBanrisul());
			request.setMTI(REQ_BA_LOGON_INIT);
			//Obtem os bits 007, 011, 012 e 013
			request = getCommonBitsFormatted(request, nsu);
			request.set(42, logicalNumber);
			
			String bit63 = getTerminalData();
			request.set(63, bit63);							//bit63
			request.set(70, PROC_CODE_LOGON_A);				//Codigo de gerenciamento = 001 Abertura
			
			response = requestAcquirer(request);
			
		} catch (ISOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return response;
	}
	
	public ListoData getResDataInitialization()
	{
		try {
			//listoData.estabelecimento = 
			
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return listoData;
	}
	
	private boolean setResDataLogon(ISOMsg response)
	{
		try {
			
			//Registra os dados retornados no logon
			listoData.smid = response.getValue(53).toString().trim();	
			String bit62 = response.getString(62);
			if (bit62.trim().length() > 0) {
				listoData.workingKey = bit62.substring(0, 32);
				listoData.versaoTabelas = bit62.substring(32, bit62.length());	
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	private void setMerchantData(ISOMsg response) 
	{
		try {
		
			String merchant = new String();
			if (response.hasField(64))
				merchant = response.getString(64);
			
			merchant += "010004" + CURRENCY_SYMBOL;

			merchant += "014008" + BYTE_1;
			merchant += "015008" + BYTE_2;
			merchant += "016008" + BYTE_3;
			
			listoData.tableSequence.add(ListoData.REG_CODE_ESTABELECIMENTO);
			listoData.L001_estabelecimento.put(listoData.L001_estabelecimento.size(), merchant);
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}

	private void setEspecificData() {
		
		//Seta as tags emv obrigatorias referentes ao aid		
		String tab12 = new String();
		for (Entry<Integer, String> entry : listoData.L009_emv.entrySet())		
		    tab12 += getTagsEmv(entry.getValue());		
		
		if (tab12.length() > 0) {
			listoData.L012_TagsReq1ndGenerateAC.put(listoData.L012_TagsReq1ndGenerateAC.size(), tab12);
		    if (!listoData.tableSequence.contains(ListoData.REG_CODE_REQ_TAGS_1ND_GEN))
		    	listoData.tableSequence.add(ListoData.REG_CODE_REQ_TAGS_1ND_GEN);
		}
		
		//Criptografia
		listoData.tableSequence.add(ListoData.REG_CODE_CRIPTO_SENHA);
		String data = BANRISUL_PINPAD_PIN + ListoData.DUKPT3DES_CODE + "000";
		listoData.L016_CriptografiaSenha.put(listoData.L016_CriptografiaSenha.size(), data);
		
		listoData.tableSequence.add(ListoData.REG_CODE_CRIPTO_DADOS);
		String wkLength = cf.padLeft(String.valueOf(listoData.workingKey.length()) , 3, "0");

		data = BANRISUL_PINPAD_DATA + ListoData.ID_VERIFONE + wkLength + listoData.workingKey;
		data += BANRISUL_PINPAD_DATA + ListoData.ID_INGENICO + wkLength + listoData.workingKey;		
		data += BANRISUL_PINPAD_DATA + ListoData.ID_GERTEC + wkLength + listoData.workingKey;			
		data += BANRISUL_PINPAD_DATA + ListoData.ID_PAX + wkLength + listoData.workingKey;
		data += BANRISUL_PINPAD_DATA + ListoData.ID_DATECS + wkLength + listoData.workingKey;
		listoData.L017_CriptografiaDados.put(listoData.L017_CriptografiaDados.size(), data);
	}
	
	public boolean setResDataInitialization(ISOMsg response)
	{	
		int ini = 0;
		int end = 42;
		String messages;
		String flags;
		
		String tab02 = new String();
		String tab04 = new String();
		String tab09 = new String();
		String tab10 = new String();
		String tab11 = new String();
		
		try {
			
			CommonFunctions common = new CommonFunctions();
			
			listoData.codigoAdquirente = ListoData.BANRISUL;
			listoData.gmtDataHora = response.getString(7);
			listoData.nsuOrigem = response.getString(11);
			listoData.horaLocal = response.getString(12);
			listoData.dataLocal = response.getString(13);
			listoData.codigoResposta = response.getString(39);
			listoData.numeroLogico = response.getString(42);
			
			String registro = response.getValue(62).toString();
		
			switch (response.getString(70)) {

			case PROC_CODE_EMV:
				while(registro.length() > 0) {   			
					int size = Integer.parseInt(registro.substring(3, 6));
					int bcCode = Integer.parseInt(registro.substring(6, 7));
					switch (bcCode) {
					case 1: //Tabela EMV AID formato BC
						tab09 += registro.substring(3,  size + 3); 
					    if (!listoData.tableSequence.contains(ListoData.REG_CODE_EMV_AID))
					    	listoData.tableSequence.add(ListoData.REG_CODE_EMV_AID);
						break;
					case 2: //Tabela chaves publicas formato BC
						tab10 += registro.substring(3,  size + 3); 
					    if (!listoData.tableSequence.contains(ListoData.REG_CODE_CHAVES_PUBLICAS))
					    	listoData.tableSequence.add(ListoData.REG_CODE_CHAVES_PUBLICAS);
						break;
					case 3:  //Tabela de certificados revogados formato BC
						tab11 += registro.substring(3,  size + 3);
					    if (!listoData.tableSequence.contains(ListoData.REG_CODE_CERTIFICADOS_REVOGADOS))
					    	listoData.tableSequence.add(ListoData.REG_CODE_CERTIFICADOS_REVOGADOS);
						break;
					}
					registro = registro.substring(size + 3, registro.length());
				}
				break;
				
			case PROC_CODE_BINS:
				tab02 += getBins(registro);
				break;
				
			case PROC_CODE_PARAMS:
				tab04 += getProductDataParams(registro);
				break;
				
			case PROC_CODE_MESSAGES:
				setAcquirerMessages(registro);
				break;
				
			case PROC_CODE_FLAGS:
				setAcquirerFlags(registro);
				//Caso o response seja S - ultimo registro
				if (!response.getValue(70).toString().substring(0,  3).equals(PROC_CODE_MESSAGES))
				{
					if ((response.getValue(62).toString().trim().equals("")) || 
						(response.getValue(62).toString().substring(10,  11).equals("N")))
						return false;
				}
				break;
			}
	
			setTablesInitialization(tab02, tab04, tab09, tab10, tab11);
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	private String getTagsEmv(String emvAid) {
		//Mastercard e visa
		//9F269F279F109F379F36959A9C9F029F035F2A829F1AF345F249F159F335F2884
		//Banrisul
		//9F1A959C829F109F269F279F369F3784
		String index = emvAid.substring(6, 8);
		String length = cf.padLeft(String.valueOf(TAGS_EMV_BANRISUL.length()), 3, "0");
		
		if (emvAid.contains("VISA") || emvAid.contains("ELECTRON") ||
		    emvAid.contains("MASTER") || emvAid.contains("MAESTRO")) {
			length = cf.padLeft(String.valueOf(TAGS_EMV_MASTERCARD_VISA.length()), 3, "0");
			return index + length + TAGS_EMV_MASTERCARD_VISA;
		}
		
		length = cf.padLeft(String.valueOf(TAGS_EMV_MASTERCARD_VISA.length()), 3, "0");
		return index + length + TAGS_EMV_MASTERCARD_VISA;
		//return index + length + TAGS_EMV_BANRISUL;
	}

	private void setTablesInitialization(String tab02, String tab04, String tab09, 
										 String tab10, String tab11){
		//if (tab01.length() > 0) 
			//listoData.L001_estabelecimento.put(listoData.L001_estabelecimento.size(), tab01);
		if (tab02.length() > 0)
			listoData.L002_bins.put(listoData.L002_bins.size(), tab02);
		//if (tab03.length() > 0)
			//listoData.L003_produtos.put(listoData.L003_produtos.size(), tab03);
		//if (tab04.length() > 0)
			//listoData.L004_paramsProdutos.put(listoData.L004_paramsProdutos.size(), tab04);
		//if (tab05.length() > 0)
			//listoData.L005_binsEspeciais.put(listoData.L005_binsEspeciais.size(), tab05);
		//if (tab06.length() > 0)
			//listoData.L006_produtosEspeciais.put(listoData.L006_produtosEspeciais.size(), tab06);
		//if (tab07.length() > 0)
			//listoData.L007_paramsProdutosEspeciais.put(listoData.L007_paramsProdutosEspeciais.size(), tab07);
		//if (tab08.length() > 0)
			//listoData.L008_parametros.put(listoData.L008_parametros.size(), tab08);
		if (tab09.length() > 0)
			listoData.L009_emv.put(listoData.L009_emv.size(), tab09);
		if (tab10.length() > 0)
			listoData.L010_chavesPublicas.put(listoData.L010_chavesPublicas.size(), tab10);
		if (tab11.length() > 0)
			listoData.L011_certificadosRevogados.put(listoData.L011_certificadosRevogados.size(), tab11);
		//if (tab12.length() > 0)
		//	listoData.L012_TagsReq1ndGenerateAC.put(listoData.L012_TagsReq1ndGenerateAC.size(), tab12);
		//if (tab13.length() > 0)
			//listoData.L013_TagsOpt1ndGenerateAC.put(listoData.L013_TagsOpt1ndGenerateAC.size(), tab13);
		//if (tab14.length() > 0)
			//listoData.L014_TagsReq2ndGenerateAC.put(listoData.L014_TagsReq2ndGenerateAC.size(), tab14);
		//if (tab15.length() > 0)
			//listoData.L015_TagsOpt2ndGenerateAC.put(listoData.L015_TagsOpt2ndGenerateAC.size(), tab15);
		//if (tab16.length() > 0)
			//listoData.L016_CriptografiaDados.put(listoData.L016_CriptografiaDados.size(), tab16);
		//if (tab17.length() > 0)
			//listoData.L017_CriptografiaSenha.put(listoData.L017_CriptografiaSenha.size(), tab17);
	}
	
	private void setAcquirerMessages(String value) {
		value = value.substring(14, value.length());
		int ini = 0;
		int end = 42;
		while(value.length() > end){
			String aux = value.substring(ini, end);
			listoData.messages.put(aux.substring(0, 2), aux.substring(2, end));
			value = value.substring(end, value.length());
		}
	}
	
	private void setAcquirerFlags(String value) {
		value = value.substring(14, value.length());
		int ini = 0;
		int end = 32;
		while(value.length() > end){
			String aux = value.substring(ini, end);
			listoData.flags.put(aux.substring(0, 2), aux.substring(2, end));
			value = value.substring(end, value.length());
		}
	}
	
	private String getBins(String value) {
		//Obtem a qtde de registros e a lista de BINS
		String bins = value.substring(14,  value.length());
		String registry = "";
		while(bins.length() > 0) {
			registry += bins.substring(0,  6) + "0000";
			registry += bins.substring(6,  12) + "0000";
			registry += bins.substring(14, 17);
			bins = bins.substring(17, bins.length());
		}
	    if ((bins.length() > 0) && (!listoData.tableSequence.contains(ListoData.REG_CODE_BINS)))
	    	listoData.tableSequence.add(ListoData.REG_CODE_BINS);
		return registry;
	}
	
	private String getProductDataParams(String value) {
		String params = "";// value.substring(11, value.length());
		String registry = "";
		String code = "";
		int count = 0;
		/* ADICIONAR NA TABELA DE PRODUTOS E NA DE PARAMETROS POR PRODUTOS
		while(params.length() > 0) {
			code = params.substring(0,  3);
			count = Integer.parseInt(params.substring(3, 2));
			String sub = params.substring(5, params.length());
			
			while(count != 0) {
				registry += code;
				registry += "000000000000"; //Valor minimo da transacao
				registry += "000000000000"; //Valor maximo da transacao
				registry += common.padLeft(sub.substring(52, 54), 2, "0"); //Qtde minima parcelas
				registry += common.padLeft(sub.substring(54, 56), 2, "0"); //Qtde maxima parcelas
				registry += "000000000000"; //Valor minimo da parcela
				registry += "000000000000"; //Valor maximo da parcela
				registry += "00"; //Taxa maxima de servico
				registry += common.padLeft(sub.substring(56, 58), 2, "0"); //Prazo default entre parcelas
				registry += common.padLeft(sub.substring(58, 60), 2, "0"); //Qtde de dias pre-datado
				registry += common.padLeft(sub.substring(60, 62), 2, "0"); //Limite dias pre-datado
				count--;
				
			}
			params = params.substring(67, params.length());
		}
	    if ((params.length() > 0) && (!listoData.tableSequence.contains(ListoData.REG_CODE_PRODUTOS)))
	    	listoData.tableSequence.add(ListoData.ListoData.REG_CODE_PRODUTOS);		
		*/
		
		return registry;	
	}
	
	private ISOMsg requestAcquirer(ISOMsg request)
	{
		ISOMsg response = null;
		
		try {
			
			MUX mux = (MUX) NameRegistrar.get(idMUXBanrisul);
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
	
	private ISOMsg getCommonBitsFormatted(ISOMsg msg, int nsu) throws ISOException {
		ISOMsg isomsg = msg;
		Calendar trsdate = cf.getCurrentDate();		
		
		if (!msg.hasField(7))
		{			
			isomsg.set(7, cf.padLeft(String.valueOf(trsdate.get(Calendar.MONTH) + 1), 2, "0") +			
						cf.padLeft(String.valueOf(trsdate.get(Calendar.DAY_OF_MONTH)), 2, "0") + 
						cf.padLeft(String.valueOf(trsdate.get(Calendar.HOUR_OF_DAY)), 2, "0") + 
						cf.padLeft(String.valueOf(trsdate.get(Calendar.MINUTE)), 2, "0") +
						cf.padLeft(String.valueOf(trsdate.get(Calendar.SECOND)), 2, "0"));								
		}		
		else
			isomsg.set(7, msg.getValue(7).toString());
		
		if (!msg.hasField(11)) //NSU TEF
			isomsg.set(11, cf.padLeft(String.valueOf(nsu), 6, "0"));
		else
			isomsg.set(11, msg.getValue(11).toString());
		
		if (!msg.hasField(12)) //hora local
		{
			isomsg.set(12, cf.padLeft(String.valueOf(trsdate.get(Calendar.HOUR_OF_DAY)), 2, "0") + 
					cf.padLeft(String.valueOf(trsdate.get(Calendar.MINUTE)), 2, "0") +
					cf.padLeft(String.valueOf(trsdate.get(Calendar.SECOND)), 2, "0"));		
		}	
		else
			isomsg.set(12, msg.getValue(12).toString());
		
		if (!msg.hasField(13)) //data local
		{
			isomsg.set(13, cf.padLeft(String.valueOf(trsdate.get(Calendar.MONTH) + 1), 2, "0") +			
						   cf.padLeft(String.valueOf(trsdate.get(Calendar.DAY_OF_MONTH)), 2, "0"));
		}
		else
			isomsg.set(13, msg.getValue(13).toString());
		
		return isomsg;
	}
	
	private String getProcessingCode(String value){
		switch (value) {
		case ListoData.PROC_REQ_CREDIT:
			return PROC_CODE_CREDIT;
		case ListoData.PROC_REQ_CREDIT_WITH_INTEREST:
			return PROC_CODE_CREDIT_WITH_INTEREST;
		case ListoData.PROC_REQ_CREDIT_WITHOUT_INTEREST:
			return PROC_CODE_CREDIT_WITHOUT_INTEREST;
		case ListoData.PROC_REQ_DEBIT:
			return PROC_CODE_DEBIT;
		}
		return null;
	}
	
	private String getMerchantData(TransactionData data) {
		if (data.merchantName.length() > 22)
			data.merchantName = data.merchantName.substring(0, 22);
		
		String bit062 = cf.padRight(data.merchantName, 22, " ");
		if (data.address.length() > 40)
			data.address = data.merchantName.substring(0, 40);
		bit062 += cf.padRight(data.address, 40, " ");
		
		if (data.city.length() > 13)
			data.city = data.city.substring(0, 13);
		bit062 += cf.padRight(data.city, 13, " ");
		
		if (data.state.length() > 2)
			data.state = data.state.substring(0, 2);
		bit062 += cf.padRight(data.state, 2, " ");
		
		if (data.country.length() > 3)
			data.country = data.country.substring(0, 3);
		bit062 += cf.padRight(data.country, 3, " ");
		
		if (data.zipCode.length() > 8)
			data.zipCode = data.zipCode.substring(0, 8);
		bit062 += cf.padRight(data.zipCode, 8, " ");
		
		if (data.mcc.length() > 4)
			data.mcc = data.mcc.substring(0, 4);
		bit062 += cf.padRight(data.phone, 4, " ");
		
		if (data.cnpjcpf.length() > 14)
			data.cnpjcpf = data.cnpjcpf.substring(0, 14);
		bit062 += cf.padRight(data.cnpjcpf, 14, " ");
		
		if (data.phone.length() > 11)
			data.phone = data.phone.substring(0, 11);
		bit062 += cf.padRight(data.phone, 11, " ");
		
		return bit062;
	}
	
	private String getTerminalData() {
		String bit63 = PARAM_63a; 						//Master key Banrisul
		bit63 += PARAM_63b;       						//Forma de comunicacao (50 - TCP/IP)
		bit63 += PARAM_63c; 							//Tipo de equipamento (001 - POSes terminal type 21)
		bit63 += PARAM_63d;								//versao do buffer
		bit63 += PARAM_63e; 							//versao da especificacao do tef
		bit63 += PARAM_63f; 							//codigo da Listo no cadastro com o BANRISUL
		return bit63;
	}
	
	private ISOMsg getTransactionFormatted(TransactionData requestData) throws ISOException {
		ISOMsg request = new ISOMsg();
		
		request.setPackager(new ISO87APackagerGP());
		request.setMTI(REQ_BA_PAYMENT);
		/*
		if (requestData.pan.length() > 0)
			request.set(FIELD_PAN, cf.padLeft(String.valueOf(requestData.pan.length()), 2, "0") + 
					    requestData.pan);
		*/
		//if (requestData.pan.length() > 0)
			//request.set(FIELD_PAN, requestData.pan);
		
		request.set(FIELD_PROC_CODE, getProcessingCode(requestData.processingCode));
		request.set(FIELD_AMOUNT, requestData.amount);
		request.set(FIELD_DATE_TIME, requestData.dateTime);
		request.set(FIELD_NSU_TEF, requestData.nsuTef);
		request.set(FIELD_DATE, requestData.date);
		request.set(FIELD_TIME, requestData.time);
		
		//if (requestData.expirationDateCard.length() > 0)
			//request.set(FIELD_CARD_EXPIRATION_DATE, requestData.expirationDateCard);
		
		request.set(FIELD_ENTRY_MODE, requestData.entryMode);
		
		if (requestData.panSequence.length() > 0)
			request.set(FIELD_PAN_SEQUENCE, requestData.panSequence);
		
		//request.set(FIELD_FINANCIAL_INSTITUTION, FINANCIAL_INSTITUTION_CODE);
		request.set(FIELD_FINANCIAL_INSTITUTION, "0800000000");
		
		//if (requestData.cardTrack2.length() > 0)
			//request.set(FIELD_TRACK_2, requestData.cardTrack2);		
		
		request.set(FIELD_TERMINAL_CODE, requestData.terminalCode);		
		request.set(FIELD_MERCHANT_CODE, "041003500000100");
		
		if (requestData.cardTrack1.length() > 0)
			request.set(FIELD_TRACK_1, 
				    cf.padLeft(String.valueOf(requestData.cardTrack1.length()), 2, "0") + 
				    requestData.cardTrack1);

		request.set(FIELD_CURRENCY_CODE, requestData.currencyCode);

		if (requestData.pin.length() > 0)
			request.set(FIELD_PIN, requestData.pin);
		
		if (requestData.smid.length() > 0)
			request.set(FIELD_SMID, requestData.smid);
		
		if (requestData.emvData.length() > 0) {
			String bit055 = requestData.emvData.substring(6, requestData.emvData.length());
			/*request.set(FIELD_EMV_DATA, 
				    cf.padLeft(String.valueOf(bit055.length()), 3, "0") + bit055);	*/
			request.set(FIELD_EMV_DATA, bit055);
		}
		
		
		request.set(FIELD_INSTALLMENTS, "01");
		
		//BIT061 - Tipo de terminal - opcional
		//request.set(FIELD_TERMINAL_TYPE, "00811111111");	
		
		//Adiciona o zero no inicio - pre-autorizacao (0 = nao e pre-autorizacao)
		String merchantData = "0" + getMerchantData(requestData);
		String securityData = requestData.ksnPin;
		securityData += "00000000000000000000"; //ksn dados
		securityData += requestData.encryptedCardData;
		
		//TESTE
		//merchantData = "0XXXXXXXXXXXX*JOJOAOZIN                                    SAOPAULO          0145200258120000000616123400000000000";
		
		request.set(FIELD_GENERIC_DATA_62, merchantData + securityData);
		
		//TESTE
		//terminalData = "014000100003***XXXXXXXXX TEF****0001";
		request.set(FIELD_GENERIC_DATA_63, getTerminalData());
		
		if (requestData.installments.length() > 0)
			request.set(FIELD_INSTALLMENTS, cf.padLeft(requestData.installments, 2, "0"));
		
		if (requestData.typeCardRead.equals(ListoData.MAGNETIC)) {
			securityData = "0";
			if (!requestData.typeCardVerificationData.equals(ListoData.SECURITY_CODE_INFORMED)) {
				if (requestData.typeCardVerificationData.equals(ListoData.SECURITY_CODE_UNREADABLE))
					securityData = "1";
			}
			else
				securityData = requestData.cardVerificationData;
			request.set(FIELD_SECURITY_CODE, securityData);
		}
		
		
		
		//Prencher com os dados da ultima transacao valida
		//salvar em memoria a data e o NSU do banrisul
		//Somente enviar valores da data e NSU banrisul da transacao 
		//realizada e confirmada com o host
		request.set(FIELD_LAST_TRANSACTION, "0000000000000000");		
		
		return request;
	}
	
	public TransactionData getResponseData(TransactionData requestData, ISOMsg message) {
		TransactionData data = new TransactionData();
		
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
		if (message.hasField(FIELD_RESPONSE_CODE))
			data.responseCode = message.getString(FIELD_RESPONSE_CODE);		
		if (message.hasField(FIELD_AUTHORIZATION_CODE))
			data.authorizationCode = message.getString(FIELD_AUTHORIZATION_CODE);		
		if (message.hasField(FIELD_TERMINAL_CODE))
			data.terminalCode = message.getString(FIELD_TERMINAL_CODE);
		if (message.hasField(FIELD_MERCHANT_CODE))
			data.merchantCode = message.getString(FIELD_MERCHANT_CODE);		
		if (message.hasField(FIELD_CURRENCY_CODE))
			data.currencyCode = message.getString(FIELD_CURRENCY_CODE);
		if (message.hasField(FIELD_EMV_DATA))
			data.emvData = message.getString(FIELD_EMV_DATA);
		
		if (!data.responseCode.equals("00")) {
			String msg = "TRANSACAO NEGADA"; 
			if ((listoData.messages != null) && 
				(listoData.messages.containsKey(data.responseCode)))
				msg = listoData.messages.get(data.responseCode).trim();
			data.cardholderReceipt = msg;
		} else {
			data.merchantReceipt = getMerchantReceipt(requestData, message);
			data.cardholderReceipt = getCardholderReceipt(requestData, message);
		}

		if (message.hasField(FIELD_NSU_ACQUIRER))
			data.nsuAcquirer = message.getString(FIELD_NSU_ACQUIRER);
		
		
		return data;
	}
	
	private String getMerchantReceipt(TransactionData requestData, ISOMsg message) {
		String receipt = new String();
		try {
	
			//Formatar comprovante
			if (message.hasField(FIELD_GENERIC_DATA_62)) { 
				String flag = message.getString(FIELD_GENERIC_DATA_62).substring(0, 15).trim();
				receipt += "@------------ 1� via � loja -----------"; 
				receipt += "@" + cf.padRight(RCP_ACQUIRER_NAME + " - " + flag, 39, " ");
				receipt += "@"; //quebra linha
				receipt += "@" + cf.padRight(getTypePaymentDescription(requestData.processingCode), 39, " ");
				
				String merchant = requestData.merchantName.toUpperCase();
				if (merchant.length() > 38) merchant = merchant.substring(0,  38);
				
				receipt += "@" + cf.padRight(merchant, 39, " ");
				receipt += "@CNPJ: " + cf.padRight(requestData.cnpjcpf, 39, " ");
				receipt += "@" + cf.padRight(requestData.city.toUpperCase(), 39, " ");
				receipt += "@"; //quebra linha
				receipt += "@" + cf.padRight(requestData.merchantCode + " " + 
											 requestData.equipmentType, 39, " ");
				receipt += "@";
				String date = 
				receipt += "@DATA: " + requestData.brazilianDate + "        HORA: " +
						   requestData.time.substring(0, 2) + ":" +
						   requestData.time.substring(2, 4) + ":" +
						   requestData.time.substring(4, 6);
				receipt += "@NSU BERGS:" + requestData.nsuAcquirer + " NSU BANDEIRA:" + requestData.authorizationCode;
				receipt += "CARTAO: 9999   VALOR: " + (Long.valueOf(requestData.amount) / 100);
				
				//TERMINAR FORMATACAO DO COMPROVANTE
				/*
				if (message.hasField(FIELD_GENERIC_DATA_63)) 
					data.merchantReceipt = message.getString(FIELD_GENERIC_DATA_63); */
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	
		return receipt;
	}
	
	private String getCardholderReceipt(TransactionData requestData, ISOMsg message) {
		String receipt = new String();
		try {
	
			//Formatar comprovante
			if (message.hasField(FIELD_GENERIC_DATA_62)) { 
				String flag = message.getString(FIELD_GENERIC_DATA_62).substring(0, 15).trim();
				receipt += "@----------- 2� via � cliente ---------"; 
				receipt += "@" + cf.padRight(RCP_ACQUIRER_NAME + " - " + flag, 39, " ");
				receipt += "@"; //quebra linha
				receipt += "@" + cf.padRight(getTypePaymentDescription(requestData.processingCode), 39, " ");
				
				String merchant = requestData.merchantName.toUpperCase();
				if (merchant.length() > 38) merchant = merchant.substring(0,  38);
				
				receipt += "@" + cf.padRight(merchant, 39, " ");
				receipt += "@CNPJ: " + cf.padRight(requestData.cnpjcpf, 39, " ");
				receipt += "@" + cf.padRight(requestData.city.toUpperCase(), 39, " ");
				receipt += "@"; //quebra linha
				receipt += "@" + cf.padRight(requestData.merchantCode + " " + 
											 requestData.equipmentType, 39, " ");
				receipt += "@";
				String date = 
				receipt += "@DATA: " + requestData.brazilianDate + "        HORA: " +
						   requestData.time.substring(0, 2) + ":" +
						   requestData.time.substring(2, 4) + ":" +
						   requestData.time.substring(4, 6);
				receipt += "@NSU BERGS:" + requestData.nsuAcquirer + " NSU BANDEIRA:" + requestData.authorizationCode;
				receipt += "CARTAO: 9999   VALOR: " + (Long.valueOf(requestData.amount) / 100);
				
				//TERMINAR FORMATACAO DO COMPROVANTE
				/*
				if (message.hasField(FIELD_GENERIC_DATA_63)) 
					data.merchantReceipt = message.getString(FIELD_GENERIC_DATA_63); */
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	
		return receipt;
	}
	
	private String getTypePaymentDescription(String type){
		switch (type) {
		case ListoData.PROC_REQ_CREDIT:
			return "VENDA CREDITO A VISTA";
		case ListoData.PROC_REQ_CREDIT_WITHOUT_INTEREST:
			return "VENDA CREDITO PARCELADO LOJISTA";
		case ListoData.PROC_REQ_CREDIT_WITH_INTEREST:
			return "VENDA CREDITO PARCELADO EMISSOR";
		}
		return "VENDA DEBITO A VISTA";
	}
	
	public TransactionData requestPayment(TransactionData requestData){
		TransactionData responseData = null;
		
		try {
			//Seta os dados de inicializacao do adquirente
			listoData = AcquirerSettings.getInitializationTables(BANRISUL, requestData.merchantCode);
			
			ISOMsg request = getTransactionFormatted(requestData);
			ISOMsg response = requestAcquirer(request);	
			responseData = getResponseData(requestData, response);
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
		return responseData;
	}
}
