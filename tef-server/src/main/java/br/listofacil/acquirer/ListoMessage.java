package br.listofacil.acquirer;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.XMLPackager;

import com.bravado.bsh.InfoTransaction;

import br.listofacil.CommonFunctions;
import br.listofacil.AcquirerProcess;
import br.listofacil.AcquirerSettings;

public class ListoMessage {
	
	private final int FIELD_PAN = 2;
	private final int FIELD_PROCESSING_CODE = 3;
	private final int FIELD_AMOUNT = 4;
	private final int FIELD_DATE_TIME = 7;
	private final int FIELD_NSU_TEF = 11;
	private final int FIELD_DATE = 12;
	private final int FIELD_TIME = 13;
	private final int FIELD_CARD_EXP_DATE = 14;
	private final int FIELD_RELEASE_DATE = 15;
	private final int FIELD_ENTRY_MODE = 22;
	private final int FIELD_PAN_SEQUENCE = 23;
	private final int FIELD_PRODUCT_DESCRIPTION = 33;
	private final int FIELD_TRACK_1 = 34;
	private final int FIELD_TRACK_2 = 35;
	private final int FIELD_AUTHORIZATION_CODE = 38;
	private final int FIELD_RESPONSE_CODE = 39;
	private final int FIELD_TERMINAL_CODE = 41;
	private final int FIELD_MERCHANT_CODE = 42;
	private final int FIELD_SHOP_CODE = 43;
	private final int FIELD_ACQUIRER_CODE = 44;
	private final int FIELD_EQUIPMENT_TYPE = 45;
	private final int FIELD_SMID = 46;
	private final int FIELD_TABLES_VERSION = 47;
	private final int FIELD_TRANSACTION_DATA = 48;
	private final int FIELD_CURRENCY_CODE = 49;
	private final int FIELD_COUNTRY_CODE = 50;
	private final int FIELD_TYPE_CARD_READ = 51;
	private final int FIELD_PIN = 52;
	private final int FIELD_EMV_DATA = 55;
	private final int FIELD_REGISTRY_INDEX = 56;
	private final int FIELD_REGISTRY_CODE = 57;
	private final int FIELD_TERMINAL_DATA = 61;
	private final int FIELD_GENERIC_DATA_1 = 62;
	private final int FIELD_GENERIC_DATA_2 = 63;
	private final int FIELD_MERCHANT_DATA = 64;
	private final int FIELD_SUGGEST_DATE = 65;
	private final int FIELD_INSTALLMENT_VALUE = 66;
	private final int FIELD_INSTALLMENTS = 67;
	private final int FIELD_ENCRYPTION_DATA = 126;
	private final int FIELD_NSU_ACQUIRER = 127;
	
	private final String TAG_PAN_PART_ENCRYPTED = "005";
	
	private final String TAG_PINPAD_SERIAL_NUMBER = "001";
	private final String TAG_PINPAD_BC_VERSION = "002";
	private final String TAG_PINPAD_MANUFACTURER = "003";
	private final String TAG_PINPAD_MODEL = "004";
	private final String TAG_PINPAD_FIRMWARE = "005";
	private final String TAG_PINPAD_TABLES_VERSION = "006";
	private final String TAG_PINPAD_VERSION_BASIC_APP = "007";
	
	private final String TAG_MERCHANT_NAME = "001";
	private final String TAG_MERCHANT_ADDRESS = "002";
	private final String TAG_MERCHANT_CITY = "003";
	private final String TAG_MERCHANT_STATE = "004";
	private final String TAG_MERCHANT_COUNTRY = "005";
	private final String TAG_MERCHANT_ZIPCODE = "006";
	private final String TAG_MERCHANT_MCC = "007";
	private final String TAG_MERCHANT_CPFCNPJ = "008";
	private final String TAG_MERCHANT_PHONE = "009";
	
	private final String TAG_ENCRYPTION_PIN = "001";
	private final String TAG_ENCRYPTION_KSN_PIN = "002";
	private final String TAG_ENCRYPTION_CARD = "003";
	private final String TAG_ENCRYPTION_KSN_CARD = "004";
	private final String TAG_ENCRYPTION_TYPE_CVD = "005";
	private final String TAG_ENCRYPTION_CVD = "006";
	
	CommonFunctions cf =  new CommonFunctions();
	
	public ISOMsg getResponseMessage(ISOMsg m)
	{
		ISOMsg isomsg = new ISOMsg();
		
		try {
		
			String logicalNumber = m.getString(FIELD_MERCHANT_CODE);
			String acquirer = m.getString(FIELD_ACQUIRER_CODE);
			
			if ((acquirer == null) || (logicalNumber == null))
				return getErrorMessage(m);
			
			//Formata NSU e horario transacao
			isomsg = getCommonBitsFormatted(isomsg, AcquirerSettings.getIncrementNSU());
			
			switch (m.getMTI()) {
			case ListoData.REQ_LOGON_INIT:
				{
					ListoData listoData = AcquirerSettings.getInitializationTables(acquirer, logicalNumber);		
					
					switch (m.getValue(3).toString()) {
					case ListoData.PROC_REQ_LOGON:
						isomsg = getLogon(listoData, m);
						break;
					case ListoData.PROC_REQ_INIT:
						isomsg = getInitialize(listoData, m);
						break;
					
					default:
						isomsg = getErrorMessage(m);
						break;
					}		
				}
				break;
			
			case ListoData.REQ_PAYMENT:
				isomsg = getPayment(getCommonBitsFormatted(m, AcquirerSettings.getIncrementNSU()));
				break;

			default:
				break;
			}
			
		} catch (ISOException e) {
			// TODO Auto-generated catch block
			String erro = e.getMessage();
			isomsg = getErrorMessage(m);
		}
		return isomsg;
	}
	
	public ISOMsg getCommonBitsFormatted(ISOMsg msg, int nsu) throws ISOException {
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
	
	public ISOMsg getErrorMessage(ISOMsg m)
	{
		ISOMsg isomsg = null;
		
		try {
			
			String procCode = null;
			String mti = ListoData.getMTI(m.getMTI());
			isomsg = new ISOMsg();
			isomsg.setPackager(m.getPackager());
			isomsg.setMTI(mti);
			
			if (m.hasField(FIELD_PROCESSING_CODE))
				procCode = m.getString(FIELD_PROCESSING_CODE);
			
			isomsg.set(FIELD_PROCESSING_CODE, ListoData.getProcessCode(mti, procCode)); 
			isomsg.set(FIELD_RESPONSE_CODE, ListoData.RES_CODE_ERROR); 
			
			//Responde os mesmos valores dos campos enviados (eco)
			isomsg.set(FIELD_TERMINAL_CODE, m.getString(FIELD_TERMINAL_CODE)); //Codigo do terminal
			isomsg.set(FIELD_MERCHANT_CODE, m.getString(FIELD_MERCHANT_CODE)); //Codigo do estabelecimento
			isomsg.set(FIELD_SHOP_CODE, m.getString(FIELD_SHOP_CODE)); 	   //Codigo da loja
			
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		return isomsg;
	}
	
	public ISOMsg getWaitLogon(ISOMsg m)
	{
		ISOMsg isomsg = null;
		
		try {
			String procCode = null;
			String mti = ListoData.getMTI(m.getMTI());
			
			isomsg = new ISOMsg();
			isomsg.setPackager(m.getPackager());
			isomsg.setMTI(mti);
			
			if (m.hasField(FIELD_PROCESSING_CODE))
				procCode = m.getString(FIELD_PROCESSING_CODE);
			
			isomsg.set(FIELD_PROCESSING_CODE, ListoData.getProcessCode(mti, procCode)); 
			isomsg.set(FIELD_DATE_TIME, m.getString(FIELD_DATE_TIME));
			isomsg.set(FIELD_NSU_TEF, m.getString(FIELD_NSU_TEF));
			isomsg.set(FIELD_DATE, m.getString(FIELD_DATE));
			isomsg.set(FIELD_TIME, m.getString(FIELD_TIME));
			
			//Autorizado a transacionar
			isomsg.set(FIELD_RESPONSE_CODE, ListoData.RES_CODE_WAIT_TABLES); 
			//Responde os mesmos valores dos campos enviados (eco)
			isomsg.set(FIELD_TERMINAL_CODE, m.getString(FIELD_TERMINAL_CODE)); //Codigo do terminal
			isomsg.set(FIELD_MERCHANT_CODE, m.getString(FIELD_MERCHANT_CODE)); //Codigo do estabelecimento
			isomsg.set(FIELD_SHOP_CODE, m.getString(FIELD_SHOP_CODE)); 	   	   //Codigo da loja
			
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		return isomsg;
	}
	
	public ISOMsg getLogon(ListoData listoData, ISOMsg m) {
		ISOMsg isomsg = new ISOMsg();
		
		try {
			//Seta o mesmo packager
			isomsg.setPackager(m.getPackager());
			isomsg.setMTI(ListoData.RES_LOGON_INIT);
			isomsg.set(FIELD_PROCESSING_CODE, ListoData.PROC_RES_LOGON);
			isomsg.set(FIELD_DATE_TIME, m.getString(FIELD_DATE_TIME));
			isomsg.set(FIELD_NSU_TEF, m.getString(FIELD_NSU_TEF));
			isomsg.set(FIELD_DATE, m.getString(FIELD_DATE));
			isomsg.set(FIELD_TIME, m.getString(FIELD_TIME));
			
			//Autorizado a transacionar
			isomsg.set(FIELD_RESPONSE_CODE, ListoData.RES_CODE_AUTHORIZED); 
			//Responde os mesmos valores dos campos enviados (eco)
			isomsg.set(FIELD_TERMINAL_CODE, m.getString(FIELD_TERMINAL_CODE)); //Codigo do terminal
			isomsg.set(FIELD_MERCHANT_CODE, m.getString(FIELD_MERCHANT_CODE)); //Codigo do estabelecimento
			isomsg.set(FIELD_SHOP_CODE, m.getString(FIELD_SHOP_CODE)); 	   	   //Codigo da loja
			isomsg.set(FIELD_ACQUIRER_CODE, m.getString(FIELD_ACQUIRER_CODE)); //Codigo do adquirente
			
			if (listoData != null)
			{
				if (!listoData.smid.equals("")) 
					isomsg.set(FIELD_SMID, listoData.smid);
				if (!listoData.versaoTabelas.equals("")) 
					isomsg.set(FIELD_TABLES_VERSION, listoData.versaoTabelas);
				//if (!listoData.workingKey.equals("")) 
				//	isomsg.set(FIELD_WORKING_KEY, listoData.workingKey);
			}
			else
			{
				String acquirer = m.getString(FIELD_ACQUIRER_CODE);
				String logicalNumber = m.getString(FIELD_MERCHANT_CODE);
				
				isomsg = getWaitLogon(m);
				
				if (!AcquirerSettings.loadAcquirerTables(acquirer, logicalNumber, m.getString(FIELD_TERMINAL_CODE)))
					isomsg = getErrorMessage(m);
			}

		} catch (Exception e) {
			e.printStackTrace();
			isomsg = getErrorMessage(m);
		}
		
		return isomsg;
	}	

	public ISOMsg getInitialize(ListoData listoData, ISOMsg m) {
		ISOMsg isomsg = new ISOMsg();
		
		try {
			
			isomsg = new ISOMsg();
			//Seta o mesmo packager
			isomsg.setPackager(m.getPackager());
			isomsg.setMTI(ListoData.RES_LOGON_INIT);
			isomsg.set(FIELD_PROCESSING_CODE, ListoData.PROC_RES_INIT);
			isomsg.set(FIELD_DATE_TIME, m.getString(FIELD_DATE_TIME));
			isomsg.set(FIELD_NSU_TEF, m.getString(FIELD_NSU_TEF));
			isomsg.set(FIELD_DATE, m.getString(FIELD_DATE));
			isomsg.set(FIELD_TIME, m.getString(FIELD_TIME));
			isomsg.set(FIELD_RESPONSE_CODE, ListoData.RES_CODE_AUTHORIZED); 
			
			//Responde os mesmos valores dos campos enviados (eco)
			isomsg.set(FIELD_TERMINAL_CODE, m.getString(FIELD_TERMINAL_CODE)); //Codigo do terminal
			isomsg.set(FIELD_MERCHANT_CODE, m.getString(FIELD_MERCHANT_CODE)); //Codigo do estabelecimento
			isomsg.set(FIELD_SHOP_CODE, m.getString(FIELD_SHOP_CODE)); 	   	   //Codigo da loja
			isomsg.set(FIELD_ACQUIRER_CODE, m.getString(FIELD_ACQUIRER_CODE)); //Codigo do adquirente
			
			//Caso nao tenha realizado a inicializacao
			if (listoData == null) {
				isomsg = getErrorMessage(m);
				return isomsg;
			}
			
			int index = Integer.valueOf(m.getString(FIELD_REGISTRY_INDEX));
			String registryCode = m.getString(FIELD_REGISTRY_CODE);
			
			//Primeira mensagem
			if (registryCode.equals(ListoData.REG_CODE_REQUEST))
				registryCode = listoData.tableSequence.get(0);
			
			String registryBIT62 = "0";
			
			switch (registryCode) {				
			case ListoData.REG_CODE_ESTABELECIMENTO:
				if (listoData.L001_estabelecimento.containsKey(index)) {
					registryBIT62 = listoData.L001_estabelecimento.get(index);
					index = Integer.valueOf(ListoData.REG_CODE_END);
				}
				break;
			case ListoData.REG_CODE_BINS:
				if (listoData.L002_bins.containsKey(index)) {
					registryBIT62 = listoData.L002_bins.get(index);
					index++;
					if (!listoData.L002_bins.containsKey(index))
						index = Integer.valueOf(ListoData.REG_CODE_END);
				}
				break;
			case ListoData.REG_CODE_PRODUTOS:
				if (listoData.L003_produtos.containsKey(index)) {
					registryBIT62 = listoData.L003_produtos.get(index);
					index++;
					if (!listoData.L003_produtos.containsKey(index))
						index = Integer.valueOf(ListoData.REG_CODE_END);
				}
				break;
			case ListoData.REG_CODE_PARAMS_PRODUTOS:
				if (listoData.L004_paramsProdutos.containsKey(index)) {
					registryBIT62 = listoData.L004_paramsProdutos.get(index);
					index++;
					if (!listoData.L004_paramsProdutos.containsKey(index))
						index = Integer.valueOf(ListoData.REG_CODE_END);
				}
				break;
			case ListoData.REG_CODE_BINS_ESPECIAIS:
				if (listoData.L005_binsEspeciais.containsKey(index)) {
					registryBIT62 = listoData.L005_binsEspeciais.get(index);
					index++;
					if (!listoData.L005_binsEspeciais.containsKey(index))
						index = Integer.valueOf(ListoData.REG_CODE_END);
				}
				break;
			case ListoData.REG_CODE_PRODUTOS_ESPECIAIS:
				if (listoData.L006_produtosEspeciais.containsKey(index)) {
					if (listoData.L006_produtosEspeciais.containsKey(index)) {
						registryBIT62 = listoData.L006_produtosEspeciais.get(index);
						index++;
						if (!listoData.L006_produtosEspeciais.containsKey(index))
							index = Integer.valueOf(ListoData.REG_CODE_END);
					}
				}
				break;
			case ListoData.REG_CODE_PARAMS_PRODUTOS_ESPECIAIS:
				if (listoData.L007_paramsProdutosEspeciais.containsKey(index)) {
					registryBIT62 = listoData.L007_paramsProdutosEspeciais.get(index);
					index++;
					if (!listoData.L007_paramsProdutosEspeciais.containsKey(index))
						index = Integer.valueOf(ListoData.REG_CODE_END);
				}
				break;
			case ListoData.REG_CODE_PARAMS_TRANSACAO:
				if (listoData.L008_parametros.containsKey(index)) {
					registryBIT62 = listoData.L008_parametros.get(index);
					index++;
					if (!listoData.L008_parametros.containsKey(index))
						index = Integer.valueOf(ListoData.REG_CODE_END);
				}
				break;
			case ListoData.REG_CODE_EMV_AID:
				if (listoData.L009_emv.containsKey(index)) {
					registryBIT62 = listoData.L009_emv.get(index);
					index++;
					if (!listoData.L009_emv.containsKey(index))
						index = Integer.valueOf(ListoData.REG_CODE_END);
				}
				break;
			case ListoData.REG_CODE_CHAVES_PUBLICAS:
				if (listoData.L010_chavesPublicas.containsKey(index)) {
					registryBIT62 = listoData.L010_chavesPublicas.get(index);
					index++;
					if (!listoData.L010_chavesPublicas.containsKey(index))
						index = Integer.valueOf(ListoData.REG_CODE_END);
				}
				break;
			case ListoData.REG_CODE_CERTIFICADOS_REVOGADOS:
				if (listoData.L011_certificadosRevogados.containsKey(index)) {
					registryBIT62 = listoData.L011_certificadosRevogados.get(index);
					index++;
					if (!listoData.L011_certificadosRevogados.containsKey(index))
						index = Integer.valueOf(ListoData.REG_CODE_END);
				}
				break;
			case ListoData.REG_CODE_REQ_TAGS_1ND_GEN:
				if (listoData.L012_TagsReq1ndGenerateAC.containsKey(index)) {
					registryBIT62 = listoData.L012_TagsReq1ndGenerateAC.get(index);
					index++;
					if (!listoData.L012_TagsReq1ndGenerateAC.containsKey(index))
						index = Integer.valueOf(ListoData.REG_CODE_END);
				}
				break;
			case ListoData.REG_CODE_OPT_TAGS_1ND_GEN:
				if (listoData.L013_TagsOpt1ndGenerateAC.containsKey(index)) {
					registryBIT62 = listoData.L013_TagsOpt1ndGenerateAC.get(index);
					index++;
					if (!listoData.L013_TagsOpt1ndGenerateAC.containsKey(index))
						index = Integer.valueOf(ListoData.REG_CODE_END);
				}
				break;
			case ListoData.REG_CODE_REQ_TAGS_2ND_GEN:
				if (listoData.L014_TagsReq2ndGenerateAC.containsKey(index)) {
					registryBIT62 = listoData.L014_TagsReq2ndGenerateAC.get(index);
					index++;
					if (!listoData.L014_TagsReq2ndGenerateAC.containsKey(index))
						index = Integer.valueOf(ListoData.REG_CODE_END);
				}
				break;
			case ListoData.REG_CODE_OPT_TAGS_2ND_GEN:
				if (listoData.L015_TagsOpt2ndGenerateAC.containsKey(index)) {
					registryBIT62 = listoData.L015_TagsOpt2ndGenerateAC.get(index);
					index++;
					if (!listoData.L015_TagsOpt2ndGenerateAC.containsKey(index))
						index = Integer.valueOf(ListoData.REG_CODE_END);
				}
				break;
			case ListoData.REG_CODE_CRIPTO_SENHA:			
				if (listoData.L016_CriptografiaSenha.containsKey(index)) {
					registryBIT62 = listoData.L016_CriptografiaSenha.get(index);
					index++;
					if (!listoData.L016_CriptografiaSenha.containsKey(index))
						index = Integer.valueOf(ListoData.REG_CODE_END);
				}
				break;
			case ListoData.REG_CODE_CRIPTO_DADOS:
				if (listoData.L017_CriptografiaDados.containsKey(index)) {
					registryBIT62 = listoData.L017_CriptografiaDados.get(index);
					index++;
					if (!listoData.L017_CriptografiaDados.containsKey(index))
						index = Integer.valueOf(ListoData.REG_CODE_END);
				}
				break;
				
			default:
				isomsg = getErrorMessage(m);
				return isomsg;
			}
			
			//Verifica se e a ultima tabela
			if (index == Integer.valueOf(ListoData.REG_CODE_END) || (registryBIT62.equals("0"))) {
				int nextTable = listoData.tableSequence.indexOf(registryCode) + 1;
				if ((nextTable <= listoData.tableSequence.size() - 1)) {
					registryCode = listoData.tableSequence.get(nextTable);
					index = 0; //Reinicia o index
				}
			}
			
			isomsg.set(FIELD_REGISTRY_INDEX, cf.padLeft(String.valueOf(index), 3, "0"));
			isomsg.set(FIELD_REGISTRY_CODE, registryCode);
			isomsg.set(62, registryBIT62); 
			registryBIT62 = "0";

		} catch (Exception e) {
			e.printStackTrace();
			isomsg = getErrorMessage(m);
		}
		
		return isomsg;
	}
	
	private ISOMsg getPayment(ISOMsg request) {
		ISOMsg response = new ISOMsg();
		
		try {
			String acquirer = request.getString(FIELD_ACQUIRER_CODE);
			
			request = getCommonBitsFormatted(request, AcquirerSettings.getIncrementNSU());
			
			TransactionData dataResponse = null;
			TransactionData dataRequest = new TransactionData();
			dataRequest = getTransactionData(request);
			
			switch (acquirer) {
			case ListoData.GLOBAL_PAYMENTS:
				GlobalpaymentsMessage globalPayments = new GlobalpaymentsMessage();
				dataResponse = globalPayments.requestPayment(dataRequest);
				break;

			case ListoData.BANRISUL:
				BanrisulMessage banrisul = new BanrisulMessage();
				dataResponse = banrisul.requestPayment(dataRequest);
				break;
			default:
				response = getErrorMessage(request);
				break;
			}
			
			response = getResponsePaymentFormatted(dataRequest, dataResponse);
			
		} catch (Exception e) {
			e.printStackTrace();
			response = getErrorMessage(request);
		}
	
		return response;
	}
	
	private TransactionData getTransactionData(ISOMsg message) {
		TransactionData data = new TransactionData();
		
		if (message.hasField(FIELD_PAN))
			data.pan = message.getString(FIELD_PAN);
		if (message.hasField(FIELD_PROCESSING_CODE))
			data.processingCode = message.getString(FIELD_PROCESSING_CODE);
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
		if (message.hasField(FIELD_CARD_EXP_DATE))
			data.expirationDateCard = message.getString(FIELD_CARD_EXP_DATE);
		if (message.hasField(FIELD_RELEASE_DATE))
			data.releaseDate = message.getString(FIELD_RELEASE_DATE);
		if (message.hasField(FIELD_ENTRY_MODE))
			data.entryMode = message.getString(FIELD_ENTRY_MODE);
		if (message.hasField(FIELD_PAN_SEQUENCE))
			data.panSequence = message.getString(FIELD_PAN_SEQUENCE);
		if (message.hasField(FIELD_PRODUCT_DESCRIPTION))
			data.productDescription = message.getString(FIELD_PRODUCT_DESCRIPTION);
		if (message.hasField(FIELD_TRACK_1))
			data.cardTrack1 = message.getString(FIELD_TRACK_1);
		if (message.hasField(FIELD_TRACK_2))
			data.cardTrack2 = message.getString(FIELD_TRACK_2);
		if (message.hasField(FIELD_TERMINAL_CODE))
			data.terminalCode = message.getString(FIELD_TERMINAL_CODE);
		if (message.hasField(FIELD_MERCHANT_CODE))
			data.merchantCode = message.getString(FIELD_MERCHANT_CODE);
		if (message.hasField(FIELD_SHOP_CODE))
			data.shopCode = message.getString(FIELD_SHOP_CODE);
		if (message.hasField(FIELD_ACQUIRER_CODE))
			data.acquirerCode = message.getString(FIELD_ACQUIRER_CODE);
		if (message.hasField(FIELD_EQUIPMENT_TYPE))
			data.equipmentType = message.getString(FIELD_EQUIPMENT_TYPE);
		if (message.hasField(FIELD_SMID))
			data.smid = message.getString(FIELD_SMID);
		if (message.hasField(FIELD_CURRENCY_CODE))
			data.currencyCode = message.getString(FIELD_CURRENCY_CODE);
		if (message.hasField(FIELD_COUNTRY_CODE))
			data.countryCode = message.getString(FIELD_COUNTRY_CODE);
		if (message.hasField(FIELD_PIN))
			data.pin = message.getString(FIELD_PIN);
		if (message.hasField(FIELD_EMV_DATA))
			data.emvData = message.getString(FIELD_EMV_DATA);
		if (message.hasField(FIELD_TYPE_CARD_READ))
			data.typeCardRead = message.getString(FIELD_TYPE_CARD_READ);
		if (message.hasField(FIELD_TRANSACTION_DATA)) {
			HashMap<String, String> map = cf.tlvExtractData(message.getString(FIELD_TRANSACTION_DATA));
			if (map.containsKey(TAG_PAN_PART_ENCRYPTED))
				data.panPartEncrypted = map.get(TAG_PAN_PART_ENCRYPTED);
		}
		if (message.hasField(FIELD_TERMINAL_DATA)) {
			HashMap<String, String> map = cf.tlvExtractData(message.getString(FIELD_TERMINAL_DATA));
			if (map.containsKey(TAG_PINPAD_SERIAL_NUMBER))
				data.pinpadSerialNumber = map.get(TAG_PINPAD_SERIAL_NUMBER);
			if (map.containsKey(TAG_PINPAD_BC_VERSION))
				data.pinpadBCVersion = map.get(TAG_PINPAD_BC_VERSION);
			if (map.containsKey(TAG_PINPAD_MANUFACTURER))
				data.pinpadManufacturer = map.get(TAG_PINPAD_MANUFACTURER);
			if (map.containsKey(TAG_PINPAD_MODEL))
				data.pinpadModel = map.get(TAG_PINPAD_MODEL);
			if (map.containsKey(TAG_PINPAD_FIRMWARE))
				data.pinpadFirmware = map.get(TAG_PINPAD_FIRMWARE);
			if (map.containsKey(TAG_PINPAD_TABLES_VERSION))
				data.tablesVersion = map.get(TAG_PINPAD_TABLES_VERSION);
			if (map.containsKey(TAG_PINPAD_VERSION_BASIC_APP))
				data.pinpadVersionBasicApp = map.get(TAG_PINPAD_VERSION_BASIC_APP);
		}
		if (message.hasField(FIELD_MERCHANT_DATA)) {
			HashMap<String, String> map = cf.tlvExtractData(message.getString(FIELD_MERCHANT_DATA));
			if (map.containsKey(TAG_MERCHANT_NAME))
				data.merchantName = map.get(TAG_MERCHANT_NAME);
			if (map.containsKey(TAG_MERCHANT_ADDRESS))
				data.address = map.get(TAG_MERCHANT_ADDRESS);
			if (map.containsKey(TAG_MERCHANT_CITY))
				data.city = map.get(TAG_MERCHANT_CITY);
			if (map.containsKey(TAG_MERCHANT_STATE))
				data.state = map.get(TAG_MERCHANT_STATE);
			if (map.containsKey(TAG_MERCHANT_COUNTRY))
				data.country = map.get(TAG_MERCHANT_COUNTRY);
			if (map.containsKey(TAG_MERCHANT_ZIPCODE))
				data.zipCode = map.get(TAG_MERCHANT_ZIPCODE);
			if (map.containsKey(TAG_MERCHANT_MCC))
				data.mcc = map.get(TAG_MERCHANT_MCC);
			if (map.containsKey(TAG_MERCHANT_CPFCNPJ))
				data.cnpjcpf = map.get(TAG_MERCHANT_CPFCNPJ);
			if (map.containsKey(TAG_MERCHANT_PHONE))
				data.phone = map.get(TAG_MERCHANT_PHONE);
		}
		if (message.hasField(FIELD_SUGGEST_DATE))
			data.suggestedDate = message.getString(FIELD_SUGGEST_DATE);
		if (message.hasField(FIELD_INSTALLMENT_VALUE))
			data.installmentValue = message.getString(FIELD_INSTALLMENT_VALUE);
		if (message.hasField(FIELD_INSTALLMENTS))
			data.installments = message.getString(FIELD_INSTALLMENTS);
		if (message.hasField(FIELD_ENCRYPTION_DATA)) {
			HashMap<String, String> map = cf.tlvExtractData(message.getString(FIELD_ENCRYPTION_DATA));
			if (map.containsKey(TAG_ENCRYPTION_PIN))
				data.encryptionPinType = map.get(TAG_ENCRYPTION_PIN);
			if (map.containsKey(TAG_ENCRYPTION_KSN_PIN))
				data.ksnPin = map.get(TAG_ENCRYPTION_KSN_PIN);
			if (map.containsKey(TAG_ENCRYPTION_CARD))
				data.encryptionCardType = map.get(TAG_ENCRYPTION_CARD);
			if (map.containsKey(TAG_ENCRYPTION_KSN_CARD))
				data.ksnCard = map.get(TAG_ENCRYPTION_KSN_CARD);
			if (map.containsKey(TAG_ENCRYPTION_TYPE_CVD))
				data.typeCardVerificationData = map.get(TAG_ENCRYPTION_TYPE_CVD);
			if (map.containsKey(TAG_ENCRYPTION_CVD))
				data.cardVerificationData = map.get(TAG_ENCRYPTION_CVD);
		}
		return data;
	}
	
	private ISOMsg getResponsePaymentFormatted(TransactionData dataRequest, TransactionData dataResponse) throws ISOException {
		ISOMsg response = new ISOMsg();

		response.setPackager(new XMLPackager());
		response.setMTI(ListoData.RES_PAYMENT);
		response.set(FIELD_PROCESSING_CODE, ListoData.PROC_RES_PAYMENT);
		response.set(FIELD_AMOUNT, dataResponse.amount);
		response.set(FIELD_DATE_TIME, dataResponse.dateTime);
		response.set(FIELD_NSU_TEF, dataResponse.nsuTef);
		response.set(FIELD_DATE, dataResponse.date);
		response.set(FIELD_TIME, dataResponse.time);
		response.set(FIELD_AUTHORIZATION_CODE, dataResponse.authorizationCode);
		response.set(FIELD_RESPONSE_CODE, dataResponse.responseCode);
		
		response.set(FIELD_TERMINAL_CODE, dataRequest.terminalCode);
		response.set(FIELD_MERCHANT_CODE, dataRequest.merchantCode);
		response.set(FIELD_SHOP_CODE, dataRequest.shopCode);
		response.set(FIELD_ACQUIRER_CODE, dataRequest.acquirerCode);
		response.set(FIELD_EQUIPMENT_TYPE, dataRequest.equipmentType);
		
		if (dataResponse.emvData.length() > 0)
			response.set(FIELD_EMV_DATA, dataResponse.emvData);
		response.set(FIELD_GENERIC_DATA_1, dataResponse.cardholderReceipt);
		if (dataResponse.merchantReceipt.length() > 0)
			response.set(FIELD_GENERIC_DATA_2, dataResponse.merchantReceipt);
		response.set(FIELD_NSU_ACQUIRER, dataResponse.nsuAcquirer);

		return response;
	}
}