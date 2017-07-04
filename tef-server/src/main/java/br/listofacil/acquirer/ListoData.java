package br.listofacil.acquirer;

import java.util.ArrayList;
import java.util.HashMap;

public class ListoData {
	public final static String GLOBAL_PAYMENTS = "01";
	public final static String BANRISUL = "02";

	public final static String RES_CODE_AUTHORIZED = "00";
	public final static String RES_CODE_ERROR = "05";
	public final static String RES_CODE_WAIT_TABLES = "10";
	
	public final static String ENTRY_MODE_MAGNETIC_WITH_PIN = "021";
	public final static String ENTRY_MODE_MAGNETIC_WITHOUT_PIN = "028";
	public final static String ENTRY_MODE_CHIP_WITH_PIN = "051";
	public final static String ENTRY_MODE_CHIP_VALIDATED_PIN = "059";
	public final static String ENTRY_MODE_FALLBACK_WITH_PIN = "801";

	public final static String REQ_LOGON_INIT = "0800";
	public final static String RES_LOGON_INIT = "0810";
	public final static String REQ_PAYMENT = "0200";
	public final static String RES_PAYMENT = "0210";
	public final static String REQ_ADVICE = "0220";
	public final static String RES_ADVICE = "0230";
	public final static String REQ_CANCELLATION = "0400";
	public final static String RES_CANCELLATION = "0410";
	public final static String REQ_UNMAKING = "0420";
	public final static String RES_UNMAKING = "0430";
	public final static String REQ_CONFIRMATION = "9820";
	public final static String RES_CONFIRMATION = "9830";
	
	public final static String PROC_REQ_LOGON = "10000";
	public final static String PROC_RES_LOGON = "11000";
	public final static String PROC_REQ_INIT = "20000";
	public final static String PROC_RES_INIT = "21000";
	public final static String PROC_REQ_CREDIT = "30000";
	public final static String PROC_REQ_CREDIT_WITHOUT_INTEREST = "30001";
	public final static String PROC_REQ_CREDIT_WITH_INTEREST = "30002";
	public final static String PROC_REQ_DEBIT = "30003";
	public final static String PROC_RES_PAYMENT = "31000";
	
	public final static String CODE_TRANSACTION_TIMEOUT = "86";
	
	public final static int SERVER_TIMEOUT = 65; //segundos
	/*
	public final static int FIELD_PROCESSING_CODE = 3;
	public final static int FIELD_AMOUNT = 4;
	public final static int FIELD_DATE_TIME = 7;
	public final static int FIELD_NSU_TEF = 11;
	public final static int FIELD_DATE = 12;
	public final static int FIELD_TIME = 13;
	public final static int FIELD_AUTHORIZATION_CODE = 38;
	public final static int FIELD_RESPONSE_CODE = 39;
	public final static int FIELD_TERMINAL_CODE = 41;
	public final static int FIELD_MERCHANT_CODE = 42;
	public final static int FIELD_SHOP_CODE = 43;
	public final static int FIELD_ACQUIRER_CODE = 44;
	public final static int FIELD_EQUIPMENT_TYPE = 45;
	public final static int FIELD_SMID = 46;
	public final static int FIELD_TABLES_VERSION = 47;
	public final static int FIELD_WORKING_KEY = 48;
	public final static int FIELD_REGISTRY_INDEX = 56;
	public final static int FIELD_REGISTRY_CODE = 57;
	
	public final static int FIELD_TERMINAL_DATA = 61;
	public final static int FIELD_REGISTRY_VALUE_1 = 62;
	public final static int FIELD_REGISTRY_VALUE_2 = 63;
	*/
	public static final String REG_CODE_REQUEST = "000";
	public static final String REG_CODE_LOGON = "001";
	
	public final static String REG_CODE_ESTABELECIMENTO = "001";
	public static final String REG_CODE_BINS = "002";
	public static final String REG_CODE_PRODUTOS = "003";
	public static final String REG_CODE_PARAMS_PRODUTOS = "004";
	public static final String REG_CODE_BINS_ESPECIAIS = "005";
	public static final String REG_CODE_PRODUTOS_ESPECIAIS = "006";
	public static final String REG_CODE_PARAMS_PRODUTOS_ESPECIAIS = "007";
	public static final String REG_CODE_PARAMS_TRANSACAO = "008";
	public static final String REG_CODE_EMV_AID = "009";
	public static final String REG_CODE_CHAVES_PUBLICAS = "010";
	public static final String REG_CODE_CERTIFICADOS_REVOGADOS = "011";
	public static final String REG_CODE_REQ_TAGS_1ND_GEN = "012";
	public static final String REG_CODE_OPT_TAGS_1ND_GEN = "013";
	public static final String REG_CODE_REQ_TAGS_2ND_GEN = "014";
	public static final String REG_CODE_OPT_TAGS_2ND_GEN = "015";;
	public static final String REG_CODE_CRIPTO_SENHA = "016";
	public static final String REG_CODE_CRIPTO_DADOS = "017";
	
	public static final String REG_CODE_MESSAGES = "006";
	public static final String REG_CODE_FLAGS = "007";
	public static final String REG_CODE_CRIPTOGRAFIA = "008";
	public static final String REG_CODE_END = "999";
	
	public static final String MAGNETIC = "00";
	public static final String TIBCV1 = "01";
	public static final String TIBCV3 = "02";
	public static final String EMV_CONTACT = "03";
	public static final String EASY_ENTRY_TIBCV1 = "04";
	public static final String CHIP_SIMULATE_CARD_STRIPE = "05";
	public static final String EMV_WITHOUT_CONTACT = "06";
	public static final String TRANSACTION_ENTERED = "07";
	
	public static final String SECURITY_CODE_NOT_FOUND = "0";
	public static final String SECURITY_CODE_INFORMED = "1";
	public static final String SECURITY_CODE_UNREADABLE = "2";
	public static final String SECURITY_CODE_UNINFORMED = "9";
	
    /*
    1 - Verifone
    2 - Ingenico
    3 - Gertec
    4 - Pax
    5 - Datecs */
	
	public final static String ID_VERIFONE = "1";
	public final static String ID_INGENICO = "2";
	public final static String ID_GERTEC = "3";
	public final static String ID_PAX = "4";
	public final static String ID_DATECS = "5";
	
	public final static String DUKPT3DES_CODE = "4";
	
	public int indiceTabEmvAid = 0;
	public int indiceTabPublicKeys = 0;
	
	public String codigoAdquirente = "";
	public String gmtDataHora = "";
	public String nsuOrigem = "";
	public String horaLocal = "";
	public String dataLocal = "";
	public String codigoResposta = "";
	public String numeroLogico = "";
	public String smid = "";
	public String workingKey = "";
	public String versaoTabelasGlobalpayments = "";
	public String versaoTabelasBanrisul = "";
	public String identificacaoRede = "";
	public String currencyCode = "";
	public String currencyExponent = "";
	public String terminalCountryCode = "";
	public String merchantCategoryCode = "";
	public String posicaoChaveDadosPinpad = "";
	public String posicaoChaveSenhaPinpad = "";
	
	
	public ArrayList<String> tableSequence = new ArrayList<String>();
	
	//Tabelas padrao LISTO
	//L001 - Dados de cadastro do estabelecimento
	public HashMap<Integer, String> L001_estabelecimento = new HashMap<Integer, String>();
	
	//L002 - Lista de Bins
	public HashMap<Integer, String> L002_bins = new HashMap<Integer, String>();
	
	//L003 - Produtos habilitados
	public HashMap<Integer, String> L003_produtos = new HashMap<Integer, String>();
	
	//L004 - Paramentros da transacao por produto
	public HashMap<Integer, String> L004_paramsProdutos = new HashMap<Integer, String>();
	
	//L005 - Lista de Bins Especiais
	public HashMap<Integer, String> L005_binsEspeciais = new HashMap<Integer, String>();
	
	//L006 - Produtos habilitados
	public HashMap<Integer, String> L006_produtosEspeciais = new HashMap<Integer, String>();
	
	//L007 - Paramentros da transacao por produto especial
	public HashMap<Integer, String> L007_paramsProdutosEspeciais = new HashMap<Integer, String>();
	
	//L008 - Parametros da transacao
	public HashMap<Integer, String> L008_parametros = new HashMap<Integer, String>();
	
	//L009 - Parametros EMV da transacao
	public HashMap<Integer, String> L009_emv = new HashMap<Integer, String>();
	
	//L010 - Chaves publicas
	public HashMap<Integer, String> L010_chavesPublicas = new HashMap<Integer, String>();	
	
	//L011 - Certificados revogados
	public HashMap<Integer, String> L011_certificadosRevogados = new HashMap<Integer, String>();
	
	//L012 - Tags EMV Obrigatorias 1nd Generate AC
	public HashMap<Integer, String> L012_TagsReq1ndGenerateAC = new HashMap<Integer, String>();	
	
	//L013 - Tags EMV Opcionais 1nd Generate AC
	public HashMap<Integer, String> L013_TagsOpt1ndGenerateAC = new HashMap<Integer, String>();	
	
	//L014 - Tags EMV Obrigatorias 2nd Generate AC
	public HashMap<Integer, String> L014_TagsReq2ndGenerateAC = new HashMap<Integer, String>();	
	
	//L015 - Tags EMV Opcionais 2nd Generate AC
	public HashMap<Integer, String> L015_TagsOpt2ndGenerateAC = new HashMap<Integer, String>();	
	
	//L016 - Dados criptografia de dados - 3DES
	public HashMap<Integer, String> L016_CriptografiaSenha = new HashMap<Integer, String>();	
	
	//L017 - Dados criptografia de senha - DUKPT
	public HashMap<Integer, String> L017_CriptografiaDados = new HashMap<Integer, String>();	
	
	//Mensagens do Banrisul
	public HashMap<String, String> messages = new HashMap<String, String>();
	public HashMap<String, String> flags = new HashMap<String, String>();

	public static String getMTI(String mti)
	{
		String resMti = mti;
		
		switch (mti) {
		case REQ_LOGON_INIT:
			return RES_LOGON_INIT;
		case REQ_PAYMENT:
			return RES_PAYMENT;
		}
		return resMti;
	}
	
	public static String getProcessCode(String mti, String procCode)
	{	
		switch (mti) {
		case REQ_LOGON_INIT:
		case RES_LOGON_INIT:
			if ((procCode != null) && (procCode == PROC_REQ_LOGON))
				return PROC_RES_LOGON;
			return PROC_RES_INIT;
		}

		return PROC_RES_PAYMENT;
	}
}
