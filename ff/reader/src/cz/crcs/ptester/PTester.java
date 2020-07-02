package cz.crcs.ptester;

import javax.smartcardio.*;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Collectors;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.math.BigInteger;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;

public class PTester {
	public static final byte[] AID_BASE = {(byte)0x2e, (byte)0x2e, (byte)0x50,
										   (byte)0x54, (byte)0x65, (byte)0x73,
										   (byte)0x74, (byte)0x65, (byte)0x72};
	public static final byte[] AID_DSA_SUFFIX = {(byte)0x44, (byte)0x53,
												 (byte)0x41};
	public static final byte[] AID_FULL_SUFFIX = {(byte)0x46, (byte)0x75,
												  (byte)0x6c, (byte)0x6c};
	public static final int CLA_APPLET = 0xb0;
	public static final byte INS_PREPARE = (byte)0x5a;
	public static final byte INS_TEST_PRIME = (byte)0x5b;
	public static final byte INS_CLEAR = (byte)0x5c;

	public static final byte KEYPAIR_DSA = (byte)0x01;
	public static final byte KEYPAIR_DH = (byte)0x02;
	public static final byte KEYPAIR_ALL = KEYPAIR_DSA | KEYPAIR_DH;

	public static final byte METHOD_SET_DSA_PQG = (byte)0x01;
	public static final byte METHOD_SET_DH_PG = (byte)0x02;
	public static final byte METHOD_ALL =
		METHOD_SET_DSA_PQG | METHOD_SET_DH_PG;
	public static final byte[] METHODS =
		new byte[] {METHOD_SET_DSA_PQG, METHOD_SET_DH_PG};

	public static final short SW_Exception = (short)0xff01;
	public static final short SW_ArrayIndexOutOfBoundsException = (short)0xff02;
	public static final short SW_ArithmeticException = (short)0xff03;
	public static final short SW_ArrayStoreException = (short)0xff04;
	public static final short SW_NullPointerException = (short)0xff05;
	public static final short SW_NegativeArraySizeException = (short)0xff06;
	public static final short SW_CryptoException_prefix = (short)0xf100;
	public static final short SW_SystemException_prefix = (short)0xf200;
	public static final short SW_PINException_prefix = (short)0xf300;
	public static final short SW_TransactionException_prefix = (short)0xf400;
	public static final short SW_CardRuntimeException_prefix = (short)0xf500;

	public static class CryptoException {
		public static final short ILLEGAL_VALUE = 1;
		public static final short UNINITIALIZED_KEY = 2;
		public static final short NO_SUCH_ALGORITHM = 3;
		public static final short INVALID_INIT = 4;
		public static final short ILLEGAL_USE = 5;
	}

	public static class SW {
		public static final short SW_FILE_FULL = 0x6A84;
		public static final short SW_UNKNOWN = 0x6F00;
		public static final short SW_CLA_NOT_SUPPORTED = 0x6E00;
		public static final short SW_INS_NOT_SUPPORTED = 0x6D00;
		public static final short SW_CORRECT_LENGTH_00 = 0x6C00;
		public static final short SW_WRONG_P1P2 = 0x6B00;
		public static final short SW_INCORRECT_P1P2 = 0x6A86;
		public static final short SW_RECORD_NOT_FOUND = 0x6A83;
		public static final short SW_FILE_NOT_FOUND = 0x6A82;
		public static final short SW_FUNC_NOT_SUPPORTED = 0x6A81;
		public static final short SW_WRONG_DATA = 0x6A80;
		public static final short SW_APPLET_SELECT_FAILED = 0x6999;
		public static final short SW_COMMAND_NOT_ALLOWED = 0x6986;
		public static final short SW_CONDITIONS_NOT_SATISFIED = 0x6985;
		public static final short SW_LOGICAL_CHANNEL_NOT_SUPPORTED = 0x6881;
		public static final short SW_SECURE_MESSAGING_NOT_SUPPORTED = 0x6882;
		public static final short SW_WARNING_STATE_UNCHANGED = 0x6200;
		public static final short SW_DATA_INVALID = 0x6984;
		public static final short SW_FILE_INVALID = 0x6983;
		public static final short SW_SECURITY_STATUS_NOT_SATISFIED = 0x6982;
		public static final short SW_WRONG_LENGTH = 0x6700;
		public static final short SW_BYTES_REMAINING_00 = 0x6100;
		public static final short SW_NO_ERROR = (short)0x9000;
	}

	public static final short SW_Key_not_initialized = (short)0x0ee1;
	public static final short SW_Unknown_method = (short)0x0ee2;
	public static final short SW_Key_not_available = (short)0x0ee3;

	public static byte[] toByteArray(BigInteger what, int bits) {
		byte[] raw = what.toByteArray();
		int bytes = (bits + 7) / 8;
		if (raw.length < bytes) {
			byte[] result = new byte[bytes];
			System.arraycopy(raw, 0, result, bytes - raw.length, raw.length);
			return result;
		}
		if (bytes < raw.length) {
			byte[] result = new byte[bytes];
			System.arraycopy(raw, raw.length - bytes, result, 0, bytes);
			return result;
		}
		return raw;
	}

	public static byte[] concatenate(byte[]... arrays) {
		int len = 0;
		for (byte[] array : arrays) {
			if (array == null)
				continue;
			len += array.length;
		}
		byte[] out = new byte[len];
		int offset = 0;
		for (byte[] array : arrays) {
			if (array == null || array.length == 0)
				continue;
			System.arraycopy(array, 0, out, offset, array.length);
			offset += array.length;
		}
		return out;
	}
	
	
	public static byte[] concatenate(BigInteger... bigInts) {
		byte[][] arrays = new byte[bigInts.length * 2][];
		for (int i = 0; i < bigInts.length; ++i) {
			arrays[i*2+1] = toByteArray(bigInts[i],  bigInts[i].bitLength());
			arrays[i*2] = new byte[] {(byte)(arrays[i*2+1].length >> 8), (byte) arrays[i*2+1].length};
		}
		return concatenate(arrays);
	}

	public static String getSW(short sw) {
		int upper = (sw & 0xff00) >> 8;
		int lower = (sw & 0xff);
		switch (upper) {
			case 0xf1:
				return String.format("CryptoException(%d)", lower);
			case 0xf2:
				return String.format("SystemException(%d)", lower);
			case 0xf3:
				return String.format("PINException(%d)", lower);
			case 0xf4:
				return String.format("TransactionException(%d)", lower);
			case 0xf5:
				return String.format("CardRuntimeException(%d)", lower);
			default:
				switch (sw) {
					case SW.SW_APPLET_SELECT_FAILED:
						return "APPLET_SELECT_FAILED";
					case SW.SW_BYTES_REMAINING_00:
						return "BYTES_REMAINING";
					case SW.SW_CLA_NOT_SUPPORTED:
						return "CLA_NOT_SUPPORTED";
					case SW.SW_COMMAND_NOT_ALLOWED:
						return "COMMAND_NOT_ALLOWED";
					case SW.SW_CONDITIONS_NOT_SATISFIED:
						return "CONDITIONS_NOT_SATISFIED";
					case SW.SW_CORRECT_LENGTH_00:
						return "CORRECT_LENGTH";
					case SW.SW_DATA_INVALID:
						return "DATA_INVALID";
					case SW.SW_FILE_FULL:
						return "FILE_FULL";
					case SW.SW_FILE_INVALID:
						return "FILE_INVALID";
					case SW.SW_FILE_NOT_FOUND:
						return "FILE_NOT_FOUND";
					case SW.SW_FUNC_NOT_SUPPORTED:
						return "FUNC_NOT_SUPPORTED";
					case SW.SW_INCORRECT_P1P2:
						return "INCORRECT_P1P2";
					case SW.SW_INS_NOT_SUPPORTED:
						return "INS_NOT_SUPPORTED";
					case SW.SW_LOGICAL_CHANNEL_NOT_SUPPORTED:
						return "LOGICAL_CHANNEL_NOT_SUPPORTED";
					case SW.SW_RECORD_NOT_FOUND:
						return "RECORD_NOT_FOUND";
					case SW.SW_SECURE_MESSAGING_NOT_SUPPORTED:
						return "SECURE_MESSAGING_NOT_SUPPORTED";
					case SW.SW_SECURITY_STATUS_NOT_SATISFIED:
						return "SECURITY_STATUS_NOT_SATISFIED";
					case SW.SW_UNKNOWN:
						return "UNKNOWN";
					case SW.SW_WARNING_STATE_UNCHANGED:
						return "WARNING_STATE_UNCHANGED";
					case SW.SW_WRONG_DATA:
						return "WRONG_DATA";
					case SW.SW_WRONG_LENGTH:
						return "WRONG_LENGTH";
					case SW.SW_WRONG_P1P2:
						return "WRONG_P1P2";
					case CryptoException.ILLEGAL_VALUE:
						return "ILLEGAL_VALUE";
					case CryptoException.UNINITIALIZED_KEY:
						return "UNINITIALIZED_KEY";
					case CryptoException.NO_SUCH_ALGORITHM:
						return "NO_SUCH_ALG";
					case CryptoException.INVALID_INIT:
						return "INVALID_INIT";
					case CryptoException.ILLEGAL_USE:
						return "ILLEGAL_USE";
					case SW_Exception:
						return "Exception";
					case SW_ArrayIndexOutOfBoundsException:
						return "ArrayIndexOutOfBoundsException";
					case SW_ArithmeticException:
						return "ArithmeticException";
					case SW_ArrayStoreException:
						return "ArrayStoreException";
					case SW_NullPointerException:
						return "NullPointerException";
					case SW_NegativeArraySizeException:
						return "NegativeArraySizeException";
					case SW_Key_not_initialized:
						return "Key not initialized";
					case SW_Unknown_method:
						return "Unknown method";
					case SW_Key_not_available:
						return "Key not available";
					default:
						return "unknown";
				}
		}
	}

	public static String getSWString(short sw) {
		if (sw == (short)0x9000) {
			return "OK   (0x9000)";
		} else {
			String str = getSW(sw);
			return String.format("fail (%s, 0x%04x)", str, sw);
		}
	}

	public static String getMethodName(byte method) {
		switch (method) {
			case METHOD_SET_DSA_PQG:
				return "DSA.PQG";
			case METHOD_SET_DH_PG:
				return "DH.PG";
			default:
				return "";
		}
	}

	public static String getKeypairName(byte keyPairType) {
		switch (keyPairType) {
			case KEYPAIR_DSA:
				return "DSA";
			case KEYPAIR_DH:
				return "DH";
			default:
				return "";
		}
	}

	public static byte keypairToMethods(byte keyPairType) {
		switch (keyPairType) {
			case KEYPAIR_DSA:
				return METHOD_SET_DSA_PQG;
			case KEYPAIR_DH:
				return METHOD_SET_DH_PG;
			default:
				return 0;
		}
	}

	public static BigInteger loadInt(String str) {
		if (str.startsWith("0x")) {
			return new BigInteger(str, 16);
		} else {
			return new BigInteger(str);
		}
	}
	
	public static Card connect() throws CardException {
		TerminalFactory factory = TerminalFactory.getDefault();
		List<CardTerminal> terminals =
			factory.terminals().list(CardTerminals.State.CARD_PRESENT);
		if (terminals.isEmpty()) {
			System.err.println("No terminal with connected card.");
			System.exit(1);
		}
		CardTerminal terminal = terminals.get(0);
		return terminal.connect("*");
	}
	
	public static String selectApplet(CardChannel channel) throws CardException {
		System.out.println("[ ] Selecting applet...");
		String appletVersion = "Full";
		CommandAPDU selectAPDU = new CommandAPDU(
			0x00, 0xa4, 0x04, 0x00, concatenate(AID_BASE, AID_FULL_SUFFIX));
		ResponseAPDU selectResp = channel.transmit(selectAPDU);
		if (selectResp.getSW() != 0x9000) {
			appletVersion = "DSA";
			selectAPDU = new CommandAPDU(0x00, 0xa4, 0x04, 0x00,
										 concatenate(AID_BASE, AID_DSA_SUFFIX));
			selectResp = channel.transmit(selectAPDU);
			if (selectResp.getSW() != 0x9000) {
				System.err.println(
					"[*] Could not select applet, is it installed?");
				channel.getCard().disconnect(true);
				System.exit(1);
			}
		}
		System.out.println(
			String.format("[*] Selected applet.(%s)", appletVersion));
		System.out.println(
			"*****************************************************");
		return appletVersion;
	}
	
	public static boolean allocateKeyPair(CardChannel channel, byte keypairType, int keypairLength) throws CardException {
		System.out.println("[ ] Allocating: " + getKeypairName(keypairType) + " of " + keypairLength);
		byte[] data = new byte[6];
		byte keypairMask = 1;
		while (keypairMask < KEYPAIR_ALL) {
			byte masked = (byte)(keypairMask & keypairType);
			if (masked != 0) {
				data[(masked >> 1) * 2] = (byte)(keypairLength >> 8);
				data[(masked >> 1) * 2 + 1] =
					(byte)(keypairLength & 0xff);
			}
			keypairMask = (byte)(keypairMask << 1);
		}
		CommandAPDU prep = new CommandAPDU(CLA_APPLET, INS_PREPARE,
										   keypairType, 0, data);
		ResponseAPDU resp = channel.transmit(prep);
		byte[] respData = resp.getData();
		if (resp.getSW() != 0x9000) {
			System.err.println("[x] Error allocating: " +
							   getSWString((short)resp.getSW()));
			return false;
		} else {
			keypairMask = 1;
			int count = 0;
			String suffix = "";
			while (keypairMask < KEYPAIR_ALL) {
				byte masked = (byte)(keypairMask & keypairType);
				if (masked != 0) {
					int sw = ((respData[count * 2] << 8) & 0xffff) |
							 (respData[count * 2 + 1] & 0xff);
					if (sw != 0x9000) {
						System.err.println("[x] Error on alloc: " + getSWString((short)sw));
						return false;
					}
					count++;
					suffix += ", " + getKeypairName(masked) + ": " +
							  getSWString((short)sw);
				}
				keypairMask = (byte)(keypairMask << 1);
			}
			System.out.println("[*] Allocated" + suffix);
		}
		return true;
	}

	public static void main(String[] args) throws CardException {
		File f = null;
		if (args.length != 1) {
			System.err.println("Usage: reader.jar <prime_file.txt>");
			System.exit(1);
		} else {
			f = new File(args[0]);
			if (!f.isFile()) {
				System.err.println("Pseudoprime file not found:" + args[0]);
				System.exit(1);
			}
		}

		Card card = connect();
		CardChannel channel = card.getBasicChannel();

		String appletVersion = selectApplet(channel);

		try (BufferedReader br = new BufferedReader(new FileReader(f))) {
			String line;
			int lastLength = 0;
			byte fileType = 0;
			byte fileMethod = 0;
			int fileParams = 0;
			int fileSWs = 0;
			int lineNum = 0;
			while ((line = br.readLine()) != null) {
				if (line.equals("p,q,g")) {
					fileType = KEYPAIR_DSA;
					fileMethod = METHOD_SET_DSA_PQG;
					fileParams = 3;
					continue;
				}
				if (line.equals("p,g")) {
					if (!appletVersion.equals("Full")) {
						System.err.println("[x] Cannot test DH parameters in DSA only version of applet.");
						break;
					}
					fileType = KEYPAIR_DH;
					fileMethod = METHOD_SET_DH_PG;
					fileParams = 2;
					continue;
				}
				lineNum++;
				String[] lineSplit = line.split(",");
				short upperLen = 0;
				short bitLen = 0;

				if (lineSplit.length != fileParams) {
					System.err.println("[x] Bad format, line: " + line);
					continue;
				}
				
				byte[] testData;
				if (fileType == KEYPAIR_DSA) {
					BigInteger p = loadInt(lineSplit[0]);
					BigInteger q = loadInt(lineSplit[1]);
					BigInteger g = loadInt(lineSplit[2]);
					upperLen = (short)p.bitLength();
					bitLen = (short)q.bitLength();
					testData = concatenate(p, q, g);
				} else {
					BigInteger p = loadInt(lineSplit[0]);
					BigInteger g = loadInt(lineSplit[1]);
					upperLen = (short)p.bitLength();
					bitLen = upperLen;
					testData = concatenate(p, g);
				}

				if (lastLength == 0 || lastLength != ((upperLen + 7) / 8) * 8) {
					lastLength = ((upperLen + 7) / 8) * 8;
					if (!allocateKeyPair(channel, fileType, lastLength)) {
						break;
					}
				}

				int pass = 0;
				List<Long> passes = new LinkedList<>();
				List<Long> fails = new LinkedList<>();
				int total = 100;
				System.out.println("[ ] Doing " + total + " tries with fixed values, line = " + lineNum + ".");
				for (int j = 0; j < total; j++) {
					long elapsed = -System.nanoTime();
					CommandAPDU test = new CommandAPDU(
						CLA_APPLET, INS_TEST_PRIME, fileMethod, 0, testData);
					elapsed += System.nanoTime();
					ResponseAPDU resp = channel.transmit(test);
					if (resp.getSW() == 0x9000) {
						byte[] respData = resp.getData();
						String suffix = ", " + getMethodName(fileMethod);
						boolean allOk = true;
						for (int i = 0; i < respData.length/2; ++i) {
							int sw = (((respData[i * 2] << 8) & 0xffff) |
								   (respData[i * 2 + 1] & 0xff));
							suffix += ", " + getSWString((short)sw);
							if (sw != 0x9000) {
								allOk = false;
							}
						}
						if (allOk) {
							pass++;
							passes.add(elapsed);
						} else {
							fails.add(elapsed);
						}
						System.out.println("[*]   Values(" + bitLen + "/" +
										   upperLen + " bits): " + suffix +
										   " time: " + elapsed);
					} else {
						System.out.println("[ ]   Values(" + bitLen + "/" +
										   upperLen + " bits), FAIL, SW: " +
										   getSWString((short)resp.getSW()));
					}
				}
				double passAvg =
					passes.stream().mapToDouble(a -> a).average().orElse(0);
				double passVar = 
					passes.stream().mapToDouble(a -> (a - passAvg) * (a - passAvg)).average().orElse(0);
				double failAvg =
					fails.stream().mapToDouble(a -> a).average().orElse(0);
				double failVar = 
					fails.stream().mapToDouble(a -> (a - failAvg) * (a - failAvg)).average().orElse(0);
				System.out.println("[*] Passed " + pass + "/" +
								   (passes.size() + fails.size()) +
								   ", pass time avg/var: " + passAvg + "/" + passVar +
								   ", fail time avg/var: " + failAvg + "/" + failVar);

				CommandAPDU clear =
					new CommandAPDU(CLA_APPLET, INS_CLEAR, 0, 0);
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		card.disconnect(true);
	}
}
