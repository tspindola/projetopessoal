package br.listofacil.acquirer;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.XMLPackager;

import com.bravado.util.RabbitMQ;

import br.listofacil.CommonFunctions;
import br.listofacil.AcquirerLogonProcess;
import br.listofacil.AcquirerSettings;

public class ListoMessage {

	CommonFunctions cf = new CommonFunctions();

	public ISOMsg getResponseMessage(ISOMsg m) {
		ISOMsg isomsg = new ISOMsg();

		try {

			String logicalNumber = m.getString(ListoData.FIELD_MERCHANT_CODE);
			String acquirer = m.getString(ListoData.FIELD_ACQUIRER_CODE);

			if ((acquirer == null) || (logicalNumber == null))
				return getErrorMessage(m);

			// Formata NSU e horario transacao
			if (acquirer.equals(ListoData.BANRISUL)) {
				isomsg = getCommonBitsFormatted(m, AcquirerSettings.getIncrementNSUBanrisul());
			} else
				isomsg = getCommonBitsFormatted(m, AcquirerSettings.getIncrementNSUGlobalpayments());

			// Obtem os dados de inicializacao do adquirente
			ListoData listoData = AcquirerSettings.getInitializationTables(acquirer, logicalNumber);

			if (listoData == null) {
				isomsg = getWaitLogon(isomsg);
				if (!AcquirerSettings.loadAcquirerTables(acquirer, logicalNumber,
						m.getString(ListoData.FIELD_TERMINAL_CODE), false))
					isomsg = getErrorMessage(isomsg);
				return isomsg;
			}

			switch (m.getMTI()) {
			case ListoData.REQ_LOGON_INIT: {
				switch (m.getValue(3).toString()) {
				case ListoData.PROC_REQ_LOGON:
					isomsg = getLogon(listoData, m);
					break;
				case ListoData.PROC_REQ_INIT:
					isomsg = getInitialize(listoData, m);
					break;
				case ListoData.PROC_REQ_FORCE_INIT: // Forcar inicializacao com
													// o adquirente
					isomsg = getInitialize(listoData, m);
					AcquirerSettings.loadAcquirerTables(acquirer, logicalNumber,
							m.getString(ListoData.FIELD_TERMINAL_CODE), true);
					break;
				default:
					isomsg = getErrorMessage(m);
					break;
				}
			}
				break;

			case ListoData.REQ_PAYMENT:
				isomsg = getPayment(m);
				break;

			case ListoData.REQ_CONFIRMATION:
				isomsg = getConfirmation(m);
				break;

			case ListoData.REQ_ADVICE:
				isomsg = getAdvice(m);
				break;

			case ListoData.REQ_UNMAKING:
				isomsg = getUnmaking(m);
				break;

			case ListoData.REQ_CANCELLATION:
				isomsg = getCancellation(m);
				break;

			default:
				break;
			}

		} catch (ISOException e) {
			// TODO Auto-generated catch block
			isomsg = getErrorMessage(m);
		}
		return isomsg;
	}

	public ISOMsg getCommonBitsFormatted(ISOMsg msg, long nsu) throws ISOException {
		ISOMsg isomsg = msg;
		Calendar trsdate = cf.getCurrentDate();

		if (!msg.hasField(7)) {
			isomsg.set(7,
					cf.padLeft(String.valueOf(trsdate.get(Calendar.MONTH) + 1), 2, "0")
							+ cf.padLeft(String.valueOf(trsdate.get(Calendar.DAY_OF_MONTH)), 2, "0")
							+ cf.padLeft(String.valueOf(trsdate.get(Calendar.HOUR_OF_DAY)), 2, "0")
							+ cf.padLeft(String.valueOf(trsdate.get(Calendar.MINUTE)), 2, "0")
							+ cf.padLeft(String.valueOf(trsdate.get(Calendar.SECOND)), 2, "0"));
		} else
			isomsg.set(7, msg.getValue(7).toString());

		if (!msg.hasField(11)) // NSU TEF
			isomsg.set(11, cf.padLeft(String.valueOf(nsu), 6, "0"));
		else
			isomsg.set(11, msg.getValue(11).toString());

		if (!msg.hasField(12)) // hora local
		{
			isomsg.set(12,
					cf.padLeft(String.valueOf(trsdate.get(Calendar.HOUR_OF_DAY)), 2, "0")
							+ cf.padLeft(String.valueOf(trsdate.get(Calendar.MINUTE)), 2, "0")
							+ cf.padLeft(String.valueOf(trsdate.get(Calendar.SECOND)), 2, "0"));
		} else
			isomsg.set(12, msg.getValue(12).toString());

		if (!msg.hasField(13)) // data local
		{
			isomsg.set(13, cf.padLeft(String.valueOf(trsdate.get(Calendar.MONTH) + 1), 2, "0")
					+ cf.padLeft(String.valueOf(trsdate.get(Calendar.DAY_OF_MONTH)), 2, "0"));
		} else
			isomsg.set(13, msg.getValue(13).toString());

		return isomsg;
	}

	public ISOMsg getErrorMessage(ISOMsg m) {
		ISOMsg isomsg = null;

		try {

			String procCode = null;
			String mti = ListoData.getMTI(m.getMTI());
			isomsg = new ISOMsg();
			isomsg.setPackager(m.getPackager());
			isomsg.setMTI(mti);

			if (m.hasField(ListoData.FIELD_PROCESSING_CODE))
				procCode = m.getString(ListoData.FIELD_PROCESSING_CODE);

			isomsg.set(ListoData.FIELD_PROCESSING_CODE, ListoData.getProcessCode(mti, procCode));
			isomsg.set(ListoData.FIELD_RESPONSE_CODE, ListoData.RES_CODE_ERROR);

			// Responde os mesmos valores dos campos enviados (eco)
			isomsg.set(ListoData.FIELD_TERMINAL_CODE, m.getString(ListoData.FIELD_TERMINAL_CODE)); // Codigo
																									// do
																									// terminal
			isomsg.set(ListoData.FIELD_MERCHANT_CODE, m.getString(ListoData.FIELD_MERCHANT_CODE)); // Codigo
																									// do
																									// estabelecimento
			isomsg.set(ListoData.FIELD_SHOP_CODE, m.getString(ListoData.FIELD_SHOP_CODE)); // Codigo
																							// da
																							// loja

		} catch (Exception e) {
			// TODO: handle exception
		}

		return isomsg;
	}

	public ISOMsg getWaitLogon(ISOMsg m) {
		ISOMsg isomsg = null;

		try {
			String procCode = null;
			String mti = ListoData.getMTI(m.getMTI());

			isomsg = new ISOMsg();
			isomsg.setPackager(m.getPackager());
			isomsg.setMTI(mti);

			if (m.hasField(ListoData.FIELD_PROCESSING_CODE))
				procCode = m.getString(ListoData.FIELD_PROCESSING_CODE);

			isomsg.set(ListoData.FIELD_PROCESSING_CODE, ListoData.getProcessCode(mti, procCode));
			isomsg.set(ListoData.FIELD_DATE_TIME, m.getString(ListoData.FIELD_DATE_TIME));
			isomsg.set(ListoData.FIELD_NSU_TEF, m.getString(ListoData.FIELD_NSU_TEF));
			isomsg.set(ListoData.FIELD_TIME, m.getString(ListoData.FIELD_TIME));
			isomsg.set(ListoData.FIELD_DATE, m.getString(ListoData.FIELD_DATE));

			// Autorizado a transacionar
			isomsg.set(ListoData.FIELD_RESPONSE_CODE, ListoData.RES_CODE_WAIT_TABLES);
			// Responde os mesmos valores dos campos enviados (eco)
			isomsg.set(ListoData.FIELD_TERMINAL_CODE, m.getString(ListoData.FIELD_TERMINAL_CODE)); // Codigo
																									// do
																									// terminal
			isomsg.set(ListoData.FIELD_MERCHANT_CODE, m.getString(ListoData.FIELD_MERCHANT_CODE)); // Codigo
																									// do
																									// estabelecimento
			isomsg.set(ListoData.FIELD_SHOP_CODE, m.getString(ListoData.FIELD_SHOP_CODE)); // Codigo
																							// da
																							// loja

		} catch (Exception e) {
			// TODO: handle exception
		}

		return isomsg;
	}

	public ISOMsg getLogon(ListoData listoData, ISOMsg m) {
		ISOMsg isomsg = new ISOMsg();

		try {

			// Seta o mesmo packager
			isomsg.setPackager(m.getPackager());
			isomsg.setMTI(ListoData.RES_LOGON_INIT);
			isomsg.set(ListoData.FIELD_PROCESSING_CODE, ListoData.PROC_RES_LOGON);
			isomsg.set(ListoData.FIELD_DATE_TIME, m.getString(ListoData.FIELD_DATE_TIME));
			isomsg.set(ListoData.FIELD_NSU_TEF, m.getString(ListoData.FIELD_NSU_TEF));
			isomsg.set(ListoData.FIELD_TIME, m.getString(ListoData.FIELD_TIME));
			isomsg.set(ListoData.FIELD_DATE, m.getString(ListoData.FIELD_DATE));

			// Autorizado a transacionar
			isomsg.set(ListoData.FIELD_RESPONSE_CODE, ListoData.RES_CODE_AUTHORIZED);

			// Responde os mesmos valores dos campos enviados (eco)
			isomsg.set(ListoData.FIELD_PINPAD_ACQUIRER_ID,
					ListoData.TAG_POSITION_DATA_PINPAD + "002" + listoData.posicaoChaveDadosPinpad
							+ ListoData.TAG_POSITION_PIN_PINPAD + "003" + listoData.posicaoChaveSenhaPinpad);

			isomsg.set(ListoData.FIELD_TERMINAL_CODE, m.getString(ListoData.FIELD_TERMINAL_CODE)); // Codigo
																									// do
																									// terminal
			isomsg.set(ListoData.FIELD_MERCHANT_CODE, m.getString(ListoData.FIELD_MERCHANT_CODE)); // Codigo
																									// do
																									// estabelecimento
			isomsg.set(ListoData.FIELD_SHOP_CODE, m.getString(ListoData.FIELD_SHOP_CODE)); // Codigo
																							// da
																							// loja
			isomsg.set(ListoData.FIELD_ACQUIRER_CODE, m.getString(ListoData.FIELD_ACQUIRER_CODE)); // Codigo
																									// do
																									// adquirente

			if (!listoData.smid.equals(""))
				isomsg.set(ListoData.FIELD_SMID, listoData.smid);

			isomsg.set(ListoData.FIELD_TABLES_VERSION, "00000000");

			isomsg.set(ListoData.FIELD_MERCHANT_DATA, m.getString(ListoData.FIELD_MERCHANT_DATA)); // Dados
																									// do
																									// adquirente

			if (m.getValue(ListoData.FIELD_ACQUIRER_CODE).equals(ListoData.GLOBAL_PAYMENTS)) {
				if (!listoData.versaoTabelasGlobalpayments.equals(""))
					isomsg.set(ListoData.FIELD_TABLES_VERSION, listoData.versaoTabelasGlobalpayments);
			} else {

				if (!listoData.versaoTabelasBanrisul.equals(""))
					isomsg.set(ListoData.FIELD_TABLES_VERSION, listoData.versaoTabelasBanrisul);
			}

			// Verifica se eh necessario realizar o logon/logoff
			// e carga de tabelas de inicializacao
			AcquirerLogonProcess.setValidationConnection(isomsg);

			// Envia para o sistema que grava no banco de dados
			byte[] messageData = m.pack();
			RabbitMQ.Send(new String(messageData));

			// if (!listoData.workingKey.equals(""))
			// isomsg.set(ListoData.FIELD_WORKING_KEY, listoData.workingKey);

			// Envia para o sistema que grava no banco de dados
			messageData = isomsg.pack();
			RabbitMQ.Send(new String(messageData));

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
			// Seta o mesmo packager
			isomsg.setPackager(m.getPackager());
			isomsg.setMTI(ListoData.RES_LOGON_INIT);
			isomsg.set(ListoData.FIELD_PROCESSING_CODE, ListoData.PROC_RES_INIT);
			isomsg.set(ListoData.FIELD_DATE_TIME, m.getString(ListoData.FIELD_DATE_TIME));
			isomsg.set(ListoData.FIELD_NSU_TEF, m.getString(ListoData.FIELD_NSU_TEF));
			isomsg.set(ListoData.FIELD_TIME, m.getString(ListoData.FIELD_TIME));
			isomsg.set(ListoData.FIELD_DATE, m.getString(ListoData.FIELD_DATE));
			isomsg.set(ListoData.FIELD_RESPONSE_CODE, ListoData.RES_CODE_AUTHORIZED);

			// Responde os mesmos valores dos campos enviados (eco)
			isomsg.set(ListoData.FIELD_TERMINAL_CODE, m.getString(ListoData.FIELD_TERMINAL_CODE)); // Codigo
																									// do
																									// terminal
			isomsg.set(ListoData.FIELD_MERCHANT_CODE, m.getString(ListoData.FIELD_MERCHANT_CODE)); // Codigo
																									// do
																									// estabelecimento
			isomsg.set(ListoData.FIELD_SHOP_CODE, m.getString(ListoData.FIELD_SHOP_CODE)); // Codigo
																							// da
																							// loja
			isomsg.set(ListoData.FIELD_ACQUIRER_CODE, m.getString(ListoData.FIELD_ACQUIRER_CODE)); // Codigo
																									// do
																									// adquirente

			// Caso nao tenha realizado a inicializacao
			if (listoData == null) {
				isomsg = getErrorMessage(m);
				return isomsg;
			}

			int index = Integer.valueOf(m.getString(ListoData.FIELD_REGISTRY_INDEX));
			String registryCode = m.getString(ListoData.FIELD_REGISTRY_CODE);

			// Primeira mensagem
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

			// Verifica se e a ultima tabela
			if (index == Integer.valueOf(ListoData.REG_CODE_END) || (registryBIT62.equals("0"))) {
				int nextTable = listoData.tableSequence.indexOf(registryCode) + 1;
				if ((nextTable <= listoData.tableSequence.size() - 1)) {
					registryCode = listoData.tableSequence.get(nextTable);
					index = 0; // Reinicia o index
				}
			}

			isomsg.set(ListoData.FIELD_MERCHANT_DATA, m.getString(ListoData.FIELD_MERCHANT_DATA));
			isomsg.set(ListoData.FIELD_REGISTRY_INDEX, cf.padLeft(String.valueOf(index), 3, "0"));
			isomsg.set(ListoData.FIELD_REGISTRY_CODE, registryCode);
			isomsg.set(62, registryBIT62);
			registryBIT62 = "0";

			// Envia para o sistema que grava no banco de dados
			byte[] messageData = m.pack();
			RabbitMQ.Send(new String(messageData));

			// if (!listoData.workingKey.equals(""))
			// isomsg.set(ListoData.FIELD_WORKING_KEY, listoData.workingKey);

			// Envia para o sistema que grava no banco de dados
			messageData = isomsg.pack();
			RabbitMQ.Send(new String(messageData));

		} catch (Exception e) {
			e.printStackTrace();
			isomsg = getErrorMessage(m);
		}

		return isomsg;
	}

	private ISOMsg getPayment(ISOMsg request) {
		ISOMsg response = new ISOMsg();

		try {
			String acquirer = request.getString(ListoData.FIELD_ACQUIRER_CODE);
			TransactionData dataResponse = null;
			TransactionData dataRequest = null;

			switch (acquirer) {
			case ListoData.GLOBAL_PAYMENTS:
				dataRequest = getTransactionData(
						getCommonBitsFormatted(request, AcquirerSettings.getIncrementNSUGlobalpayments()));
				GlobalpaymentsMessage globalPayments = new GlobalpaymentsMessage();
				dataResponse = globalPayments.requestPayment(dataRequest);
				break;

			case ListoData.BANRISUL:
				dataRequest = getTransactionData(
						getCommonBitsFormatted(request, AcquirerSettings.getIncrementNSUBanrisul()));
				BanrisulMessage banrisul = new BanrisulMessage();
				dataResponse = banrisul.requestPayment(dataRequest);
				break;

			default:
				response = getErrorMessage(request);
				break;
			}

			response = getResponseFormatted(ListoData.RES_PAYMENT, request, dataRequest, dataResponse);

		} catch (Exception e) {
			e.printStackTrace();
			response = getErrorMessage(request);
		}

		return response;
	}

	private ISOMsg getConfirmation(ISOMsg request) {
		ISOMsg response = new ISOMsg();

		try {
			String acquirer = request.getString(ListoData.FIELD_ACQUIRER_CODE);
			TransactionData dataResponse = null;
			TransactionData dataRequest = null;

			switch (acquirer) {
			case ListoData.GLOBAL_PAYMENTS:
				dataRequest = getTransactionData(
						getCommonBitsFormatted(request, AcquirerSettings.getIncrementNSUGlobalpayments()));
				GlobalpaymentsMessage globalPayments = new GlobalpaymentsMessage();
				dataResponse = globalPayments.requestConfirmation(dataRequest);
				break;

			case ListoData.BANRISUL:
				dataRequest = getTransactionData(
						getCommonBitsFormatted(request, AcquirerSettings.getIncrementNSUBanrisul()));
				BanrisulMessage banrisul = new BanrisulMessage();
				dataResponse = banrisul.requestConfirmation(dataRequest);
				break;

			default:
				response = getErrorMessage(request);
				break;
			}

			response = getResponseFormatted(ListoData.RES_CONFIRMATION, request, dataRequest, dataResponse);

		} catch (Exception e) {
			e.printStackTrace();
			response = getErrorMessage(request);
		}

		return response;
	}

	private ISOMsg getAdvice(ISOMsg request) {
		ISOMsg response = new ISOMsg();

		try {
			String acquirer = request.getString(ListoData.FIELD_ACQUIRER_CODE);
			TransactionData dataResponse = null;
			TransactionData dataRequest = null;

			switch (acquirer) {
			case ListoData.GLOBAL_PAYMENTS:
				dataRequest = getTransactionData(
						getCommonBitsFormatted(request, AcquirerSettings.getIncrementNSUGlobalpayments()));
				GlobalpaymentsMessage globalPayments = new GlobalpaymentsMessage();
				dataResponse = globalPayments.requestAdvice(dataRequest);
				break;

			case ListoData.BANRISUL:
				// Nao ha mensagem de Advice para o Banrisul
				dataRequest = getTransactionData(
						getCommonBitsFormatted(request, AcquirerSettings.getIncrementNSUBanrisul()));
				dataResponse = dataRequest;
				break;

			default:
				response = getErrorMessage(request);
				break;
			}

			response = getResponseFormatted(ListoData.RES_ADVICE, request, dataRequest, dataResponse);

		} catch (Exception e) {
			e.printStackTrace();
			response = getErrorMessage(request);
		}

		return response;
	}

	private ISOMsg getCancellation(ISOMsg request) {
		ISOMsg response = new ISOMsg();

		try {
			String acquirer = request.getString(ListoData.FIELD_ACQUIRER_CODE);
			TransactionData dataResponse = null;
			TransactionData dataRequest = null;

			switch (acquirer) {
			case ListoData.GLOBAL_PAYMENTS:
				dataRequest = getTransactionData(
						getCommonBitsFormatted(request, AcquirerSettings.getIncrementNSUGlobalpayments()));
				GlobalpaymentsMessage globalPayments = new GlobalpaymentsMessage();
				dataResponse = globalPayments.requestCancellation(dataRequest);
				break;

			case ListoData.BANRISUL:
				dataRequest = getTransactionData(
						getCommonBitsFormatted(request, AcquirerSettings.getIncrementNSUBanrisul()));
				BanrisulMessage banrisul = new BanrisulMessage();
				dataResponse = banrisul.requestCancellation(dataRequest);
				break;

			default:
				response = getErrorMessage(request);
				break;
			}

			response = getResponseFormatted(ListoData.RES_CANCELLATION, request, dataRequest, dataResponse);

		} catch (Exception e) {
			e.printStackTrace();
			response = getErrorMessage(request);
		}

		return response;
	}

	private ISOMsg getUnmaking(ISOMsg request) {
		ISOMsg response = new ISOMsg();

		try {
			String acquirer = request.getString(ListoData.FIELD_ACQUIRER_CODE);
			TransactionData dataResponse = null;
			TransactionData dataRequest = null;

			switch (acquirer) {
			case ListoData.GLOBAL_PAYMENTS:
				dataRequest = getTransactionData(
						getCommonBitsFormatted(request, AcquirerSettings.getIncrementNSUGlobalpayments()));
				GlobalpaymentsMessage globalPayments = new GlobalpaymentsMessage();
				dataResponse = globalPayments.requestUnmaking(dataRequest);
				break;

			case ListoData.BANRISUL:
				dataRequest = getTransactionData(
						getCommonBitsFormatted(request, AcquirerSettings.getIncrementNSUBanrisul()));
				BanrisulMessage banrisul = new BanrisulMessage();
				dataResponse = banrisul.requestUnmaking(dataRequest);
				break;

			default:
				response = getErrorMessage(request);
				break;
			}

			response = getResponseFormatted(ListoData.RES_UNMAKING, request, dataRequest, dataResponse);

		} catch (Exception e) {
			e.printStackTrace();
			response = getErrorMessage(request);
		}

		return response;
	}

	private TransactionData getTransactionData(ISOMsg message) throws IOException, TimeoutException, ISOException {
		TransactionData data = new TransactionData();

		if (message.hasField(ListoData.FIELD_PAN)) {
			data.pan = message.getString(ListoData.FIELD_PAN);
			// Formato do PAN para globalpayments (ultimos 4 digitos com zero)
			if (message.getValue(ListoData.FIELD_ACQUIRER_CODE).equals(ListoData.GLOBAL_PAYMENTS)) {
				data.pan = data.pan.substring(0, data.pan.length() - 4);
				data.pan += "0000";
			}
		}
		if (message.hasField(ListoData.FIELD_PROCESSING_CODE))
			data.processingCode = message.getString(ListoData.FIELD_PROCESSING_CODE);
		if (message.hasField(ListoData.FIELD_AMOUNT))
			data.amount = message.getString(ListoData.FIELD_AMOUNT);
		if (message.hasField(ListoData.FIELD_DATE_TIME))
			data.dateTime = message.getString(ListoData.FIELD_DATE_TIME);
		if (message.hasField(ListoData.FIELD_NSU_TEF))
			data.nsuTef = message.getString(ListoData.FIELD_NSU_TEF);
		if (message.hasField(ListoData.FIELD_TIME))
			data.time = message.getString(ListoData.FIELD_TIME);
		if (message.hasField(ListoData.FIELD_DATE))
			data.date = message.getString(ListoData.FIELD_DATE);
		if (message.hasField(ListoData.FIELD_CARD_EXP_DATE))
			data.expirationDateCard = message.getString(ListoData.FIELD_CARD_EXP_DATE);
		if (message.hasField(ListoData.FIELD_RELEASE_DATE))
			data.releaseDate = message.getString(ListoData.FIELD_RELEASE_DATE);
		if (message.hasField(ListoData.FIELD_ENTRY_MODE))
			data.entryMode = message.getString(ListoData.FIELD_ENTRY_MODE);
		if (message.hasField(ListoData.FIELD_PAN_SEQUENCE))
			data.panSequence = message.getString(ListoData.FIELD_PAN_SEQUENCE);
		if (message.hasField(ListoData.FIELD_PRODUCT_DESCRIPTION))
			data.productDescription = message.getString(ListoData.FIELD_PRODUCT_DESCRIPTION);
		if (message.hasField(ListoData.FIELD_TRACK_1))
			data.cardTrack1 = message.getString(ListoData.FIELD_TRACK_1);
		if (message.hasField(ListoData.FIELD_TRACK_2))
			data.cardTrack2 = message.getString(ListoData.FIELD_TRACK_2);
		if (message.hasField(ListoData.FIELD_BC_DATA)) {
			HashMap<String, String> map = cf.tlvExtractData(message.getString(ListoData.FIELD_BC_DATA));
			if (map.containsKey(ListoData.TAG_BC_GOONCHIP))
				data.goOnChip = map.get(ListoData.TAG_BC_GOONCHIP);
			if (map.containsKey(ListoData.TAG_BC_AID))
				data.emvAID = map.get(ListoData.TAG_BC_AID);
		}
		if (message.hasField(ListoData.FIELD_TERMINAL_CODE))
			data.terminalCode = message.getString(ListoData.FIELD_TERMINAL_CODE);
		if (message.hasField(ListoData.FIELD_MERCHANT_CODE))
			data.merchantCode = message.getString(ListoData.FIELD_MERCHANT_CODE);
		if (message.hasField(ListoData.FIELD_SHOP_CODE))
			data.shopCode = message.getString(ListoData.FIELD_SHOP_CODE);
		if (message.hasField(ListoData.FIELD_ACQUIRER_CODE))
			data.acquirerCode = message.getString(ListoData.FIELD_ACQUIRER_CODE);
		if (message.hasField(ListoData.FIELD_EQUIPMENT_TYPE))
			data.equipmentType = message.getString(ListoData.FIELD_EQUIPMENT_TYPE);
		if (message.hasField(ListoData.FIELD_SMID))
			data.smid = message.getString(ListoData.FIELD_SMID);
		if (message.hasField(ListoData.FIELD_CURRENCY_CODE))
			data.currencyCode = message.getString(ListoData.FIELD_CURRENCY_CODE);
		if (message.hasField(ListoData.FIELD_COUNTRY_CODE))
			data.countryCode = message.getString(ListoData.FIELD_COUNTRY_CODE);
		if (message.hasField(ListoData.FIELD_AUTHORIZATION_CODE))
			data.authorizationCode = message.getString(ListoData.FIELD_AUTHORIZATION_CODE);
		if (message.hasField(ListoData.FIELD_RESPONSE_CODE))
			data.responseCode = message.getString(ListoData.FIELD_RESPONSE_CODE);
		if (message.hasField(ListoData.FIELD_PIN))
			data.pin = message.getString(ListoData.FIELD_PIN);
		if (message.hasField(ListoData.FIELD_EMV_DATA)) {
			data.emvData = message.getString(ListoData.FIELD_EMV_DATA);
			data = setDataTAGs(data);
		}
		if (message.hasField(ListoData.FIELD_TYPE_CARD_READ))
			data.typeCardRead = message.getString(ListoData.FIELD_TYPE_CARD_READ);
		if (message.hasField(ListoData.FIELD_TRANSACTION_DATA)) {
			HashMap<String, String> map = cf.tlvExtractData(message.getString(ListoData.FIELD_TRANSACTION_DATA));
			if (map.containsKey(ListoData.TAG_PAN_PART_ENCRYPTED))
				data.panPartEncrypted = map.get(ListoData.TAG_PAN_PART_ENCRYPTED);
		}
		if (message.hasField(ListoData.FIELD_ENCRYPTED_CARD_DATA))
			data.encryptedCardData = message.getString(ListoData.FIELD_ENCRYPTED_CARD_DATA);
		if (message.hasField(ListoData.FIELD_TERMINAL_DATA)) {
			HashMap<String, String> map = cf.tlvExtractData(message.getString(ListoData.FIELD_TERMINAL_DATA));
			if (map.containsKey(ListoData.TAG_PINPAD_SERIAL_NUMBER))
				data.pinpadSerialNumber = map.get(ListoData.TAG_PINPAD_SERIAL_NUMBER);
			if (map.containsKey(ListoData.TAG_PINPAD_BC_VERSION))
				data.pinpadBCVersion = map.get(ListoData.TAG_PINPAD_BC_VERSION);
			if (map.containsKey(ListoData.TAG_PINPAD_MANUFACTURER))
				data.pinpadManufacturer = map.get(ListoData.TAG_PINPAD_MANUFACTURER);
			if (map.containsKey(ListoData.TAG_PINPAD_MODEL))
				data.pinpadModel = map.get(ListoData.TAG_PINPAD_MODEL);
			if (map.containsKey(ListoData.TAG_PINPAD_FIRMWARE))
				data.pinpadFirmware = map.get(ListoData.TAG_PINPAD_FIRMWARE);
			if (map.containsKey(ListoData.TAG_PINPAD_TABLES_VERSION))
				data.tablesVersion = map.get(ListoData.TAG_PINPAD_TABLES_VERSION);
			if (map.containsKey(ListoData.TAG_PINPAD_VERSION_BASIC_APP))
				data.pinpadVersionBasicApp = map.get(ListoData.TAG_PINPAD_VERSION_BASIC_APP);
		}
		if (message.hasField(ListoData.FIELD_MERCHANT_DATA)) {
			HashMap<String, String> map = cf.tlvExtractData(message.getString(ListoData.FIELD_MERCHANT_DATA));
			if (map.containsKey(ListoData.TAG_MERCHANT_NAME))
				data.merchantName = map.get(ListoData.TAG_MERCHANT_NAME);
			if (map.containsKey(ListoData.TAG_MERCHANT_ADDRESS))
				data.address = map.get(ListoData.TAG_MERCHANT_ADDRESS);
			if (map.containsKey(ListoData.TAG_MERCHANT_CITY))
				data.city = map.get(ListoData.TAG_MERCHANT_CITY);
			if (map.containsKey(ListoData.TAG_MERCHANT_STATE))
				data.state = map.get(ListoData.TAG_MERCHANT_STATE);
			if (map.containsKey(ListoData.TAG_MERCHANT_COUNTRY))
				data.country = map.get(ListoData.TAG_MERCHANT_COUNTRY);
			if (map.containsKey(ListoData.TAG_MERCHANT_ZIPCODE))
				data.zipCode = map.get(ListoData.TAG_MERCHANT_ZIPCODE);
			if (map.containsKey(ListoData.TAG_MERCHANT_MCC))
				data.mcc = map.get(ListoData.TAG_MERCHANT_MCC);
			if (map.containsKey(ListoData.TAG_MERCHANT_CPFCNPJ))
				data.cnpjcpf = map.get(ListoData.TAG_MERCHANT_CPFCNPJ);
			if (map.containsKey(ListoData.TAG_MERCHANT_PHONE))
				data.phone = map.get(ListoData.TAG_MERCHANT_PHONE);
		}
		if (message.hasField(ListoData.FIELD_SUGGEST_DATE))
			data.suggestedDate = message.getString(ListoData.FIELD_SUGGEST_DATE);
		if (message.hasField(ListoData.FIELD_INSTALLMENT_VALUE))
			data.installmentValue = message.getString(ListoData.FIELD_INSTALLMENT_VALUE);
		if (message.hasField(ListoData.FIELD_INSTALLMENTS))
			data.installments = message.getString(ListoData.FIELD_INSTALLMENTS);
		if (message.hasField(ListoData.FIELD_ORIGINAL_TRANSACTION)) {
			HashMap<String, String> map = cf.tlvExtractData(message.getString(ListoData.FIELD_ORIGINAL_TRANSACTION));
			if (map.containsKey(ListoData.TAG_ORIGINAL_MESSAGE_CODE))
				data.originalMessageCode = map.get(ListoData.TAG_ORIGINAL_MESSAGE_CODE);
			if (map.containsKey(ListoData.TAG_ORIGINAL_NSU_TEF))
				data.originalNSUTEF = map.get(ListoData.TAG_ORIGINAL_NSU_TEF);
			if (map.containsKey(ListoData.TAG_ORIGINAL_NSU_ACQUIRER))
				data.originalNSUAcquirer = map.get(ListoData.TAG_ORIGINAL_NSU_ACQUIRER);
			if (map.containsKey(ListoData.TAG_ORIGINAL_TRANSACTION_DATE))
				data.originalDateTime = map.get(ListoData.TAG_ORIGINAL_TRANSACTION_DATE);
			if (map.containsKey(ListoData.TAG_ORIGINAL_CV))
				data.originalCV = map.get(ListoData.TAG_ORIGINAL_CV);
		}
		if (message.hasField(ListoData.FIELD_CONFIRMATION_DATA))
			data.confirmationData = message.getString(ListoData.FIELD_CONFIRMATION_DATA);
		if (message.hasField(ListoData.FIELD_ENCRYPTION_DATA)) {
			HashMap<String, String> map = cf.tlvExtractData(message.getString(ListoData.FIELD_ENCRYPTION_DATA));
			if (map.containsKey(ListoData.TAG_ENCRYPTION_PIN))
				data.encryptionPinType = map.get(ListoData.TAG_ENCRYPTION_PIN);
			if (map.containsKey(ListoData.TAG_ENCRYPTION_KSN_PIN))
				data.ksnPin = map.get(ListoData.TAG_ENCRYPTION_KSN_PIN);
			if (map.containsKey(ListoData.TAG_ENCRYPTION_CARD))
				data.encryptionCardType = map.get(ListoData.TAG_ENCRYPTION_CARD);
			if (map.containsKey(ListoData.TAG_ENCRYPTION_KSN_CARD))
				data.ksnCard = map.get(ListoData.TAG_ENCRYPTION_KSN_CARD);
			if (map.containsKey(ListoData.TAG_ENCRYPTION_TYPE_CVD))
				data.typeCardVerificationData = map.get(ListoData.TAG_ENCRYPTION_TYPE_CVD);
			if (map.containsKey(ListoData.TAG_ENCRYPTION_CVD))
				data.cardVerificationData = map.get(ListoData.TAG_ENCRYPTION_CVD);
		}
		if (message.hasField(ListoData.FIELD_NSU_ACQUIRER))
			data.nsuAcquirer = message.getString(ListoData.FIELD_NSU_ACQUIRER);

		Calendar trsdate = cf.getCurrentDate();
		data.brazilianDate = cf.padLeft(String.valueOf(trsdate.get(Calendar.DAY_OF_MONTH)), 2, "0") + "/"
				+ cf.padLeft(String.valueOf(trsdate.get(Calendar.MONTH) + 1), 2, "0") + "/"
				+ trsdate.get(Calendar.YEAR);

		// Envia para o sistema que grava no banco de dados
		byte[] messageData = message.pack();
		RabbitMQ.Send(new String(messageData));

		return data;
	}

	private TransactionData setDataTAGs(TransactionData data) {
		String bit055 = new String();
		if (data.emvData.contains("9F12")) {
			int index = data.emvData.indexOf("9F12");
			data.cardPreferredName = data.emvData.substring(index + 4, data.emvData.length());
			int size = Integer.parseInt(cf.convertHexToInt(data.cardPreferredName.substring(0, 2))) * 2;
			data.cardPreferredName = cf.convertHexString(data.cardPreferredName.substring(2, size + 2));
			if (cf.isASCII(data.cardPreferredName)) {
				data.cardPreferredName = data.productDescription;
				// Remove dos dados EMV
				bit055 = data.emvData.substring(0, index);
				data.emvData = bit055 + data.emvData.substring(index + (size + 6), data.emvData.length());
			}
		}
		if (data.emvData.contains("9F26")) {
			int index = data.emvData.indexOf("9F26");
			data.cardApplicationCryptogram = data.emvData.substring(index + 4, data.emvData.length());
			int size = Integer.parseInt(cf.convertHexToInt(data.cardApplicationCryptogram.substring(0, 2))) * 2;
			data.cardApplicationCryptogram = data.cardApplicationCryptogram.substring(2, size + 2);
		}
		if (data.emvData.contains("9F36")) {
			int index = data.emvData.indexOf("9F36");
			data.cardApplicationTransactionCounter = data.emvData.substring(index + 4, data.emvData.length());
			int size = Integer.parseInt(cf.convertHexToInt(data.cardApplicationTransactionCounter.substring(0, 2))) * 2;
			data.cardApplicationTransactionCounter = data.cardApplicationTransactionCounter.substring(2, size + 2);
		}
		return data;
	}

	private ISOMsg getResponseFormatted(String mti, ISOMsg request, TransactionData dataRequest,
			TransactionData dataResponse) throws ISOException, IOException, TimeoutException {
		ISOMsg response = new ISOMsg();

		if ((dataResponse == null) || (dataRequest == null))
			return null;

		if (dataResponse.dateTime.trim().equals(""))
			dataResponse.dateTime = dataRequest.dateTime;

		if (dataResponse.time.trim().equals(""))
			dataResponse.time = dataRequest.time;

		if (dataResponse.date.trim().equals(""))
			dataResponse.date = dataRequest.date;

		response.setPackager(new XMLPackager());

		response.setMTI(mti);
		response.set(ListoData.FIELD_PROCESSING_CODE, dataRequest.processingCode);
		response.set(ListoData.FIELD_AMOUNT, dataRequest.amount);
		response.set(ListoData.FIELD_DATE_TIME, dataResponse.dateTime);
		response.set(ListoData.FIELD_NSU_TEF, dataResponse.nsuTef);
		response.set(ListoData.FIELD_TIME, dataResponse.time);
		response.set(ListoData.FIELD_DATE, dataResponse.date);

		if (dataResponse.authorizationCode.trim().length() > 0)
			response.set(ListoData.FIELD_AUTHORIZATION_CODE, dataResponse.authorizationCode);

		if (dataResponse.responseCode.trim().length() > 0)
			response.set(ListoData.FIELD_RESPONSE_CODE, dataResponse.responseCode);

		response.set(ListoData.FIELD_TERMINAL_CODE, dataRequest.terminalCode);
		response.set(ListoData.FIELD_MERCHANT_CODE, dataRequest.merchantCode);
		response.set(ListoData.FIELD_SHOP_CODE, dataRequest.shopCode);
		response.set(ListoData.FIELD_ACQUIRER_CODE, dataRequest.acquirerCode);
		response.set(ListoData.FIELD_EQUIPMENT_TYPE, dataRequest.equipmentType);

		if (dataResponse.emvData.trim().length() > 0) {

			if (dataResponse.emvData.contains("9F26")) {
				dataResponse.emvData = dataResponse.emvData.substring(0, dataResponse.emvData.length());
			}
			response.set(ListoData.FIELD_EMV_DATA, dataResponse.emvData);

		}

		if (dataResponse.merchantReceipt.trim().length() > 0) {
			if (!dataResponse.responseCode.equals("00")) {
				dataResponse.merchantReceipt = dataResponse.merchantReceipt.trim();

				if (dataRequest.acquirerCode.equals(ListoData.GLOBAL_PAYMENTS))
					dataResponse.merchantReceipt = dataResponse.merchantReceipt.substring(1,
							dataResponse.merchantReceipt.length());

				dataResponse.merchantReceipt = dataResponse.merchantReceipt.replace("'", "");
				dataResponse.merchantReceipt = dataResponse.merchantReceipt.replace("#", "");
			}
			response.set(ListoData.FIELD_GENERIC_DATA_1, dataResponse.merchantReceipt);
		}

		if (dataResponse.cardholderReceipt.trim().length() > 0)
			response.set(ListoData.FIELD_GENERIC_DATA_2, dataResponse.cardholderReceipt);

		if (request.hasField(ListoData.FIELD_MERCHANT_DATA))
			response.set(ListoData.FIELD_MERCHANT_DATA, request.getString(ListoData.FIELD_MERCHANT_DATA));

		if (dataResponse.nsuAcquirer.trim().length() > 0) {
			response.set(ListoData.FIELD_NSU_ACQUIRER, dataResponse.nsuAcquirer);
			/*
			 * if (dataRequest.acquirerCode.equals(ListoData.BANRISUL)) { String
			 * nsuAcquirer = dataResponse.nsuAcquirer.substring(3,
			 * dataResponse.nsuAcquirer.length());
			 * response.set(ListoData.FIELD_NSU_ACQUIRER, nsuAcquirer); } else {
			 * response.set(ListoData.FIELD_NSU_ACQUIRER,
			 * dataResponse.nsuAcquirer); }
			 */
		}

		// Envia para o sistema que grava no banco de dados
		byte[] messageData = response.pack();
		RabbitMQ.Send(new String(messageData));

		return response;
	}

	public void SendUnmakingMessage(ISOMsg request, ISOMsg response) {

		try {

			if ((response == null) || (!(response.getMTI().equals(ListoData.RES_PAYMENT))))
				return;

			if (response.hasField(ListoData.FIELD_RESPONSE_CODE)
					&& response.getString(ListoData.FIELD_RESPONSE_CODE).equals("00")) {
				String acquirer = response.getString(ListoData.FIELD_ACQUIRER_CODE);

				TransactionData dataRequest = new TransactionData();

				switch (acquirer) {
				case ListoData.GLOBAL_PAYMENTS:
					dataRequest = getTransactionData(
							getCommonBitsFormatted(request, AcquirerSettings.getIncrementNSUGlobalpayments()));

					dataRequest.originalMessageCode = request.getMTI();
					dataRequest.originalNSUAcquirer = response.getString(ListoData.FIELD_NSU_ACQUIRER);
					dataRequest.originalNSUTEF = response.getString(ListoData.FIELD_NSU_TEF);
					dataRequest.originalDateTime = response.getString(ListoData.FIELD_DATE);

					GlobalpaymentsMessage globalPayments = new GlobalpaymentsMessage();
					globalPayments.requestUnmaking(dataRequest);
					break;

				case ListoData.BANRISUL:
					dataRequest = getTransactionData(
							getCommonBitsFormatted(request, AcquirerSettings.getIncrementNSUBanrisul()));

					dataRequest.originalMessageCode = request.getMTI();
					dataRequest.originalNSUAcquirer = response.getString(ListoData.FIELD_NSU_ACQUIRER);
					dataRequest.originalNSUTEF = response.getString(ListoData.FIELD_NSU_TEF);
					dataRequest.originalDateTime = response.getString(ListoData.FIELD_DATE);

					BanrisulMessage banrisul = new BanrisulMessage();

					// 86 - transacao desfeita (timeout)
					dataRequest.responseCode = ListoData.CODE_TRANSACTION_TIMEOUT;

					// Para o Banrisul enviar uma confirmacao negativa da
					// transacao
					banrisul.requestConfirmation(dataRequest);
					break;

				default:
					break;
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
