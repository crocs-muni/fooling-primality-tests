package cz.crcs.ptester;

import javacard.framework.Applet;
import javacard.framework.APDU;
import javacard.framework.JCSystem;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.Util;
import javacardx.apdu.ExtendedLength;
import javacard.framework.CardRuntimeException;
import javacard.framework.SystemException;
import javacard.framework.PINException;
import javacard.framework.TransactionException;
import javacard.security.KeyPair;
import javacard.security.DSAPrivateKey;
import javacard.security.DSAPublicKey;
import javacard.security.DHPrivateKey;
import javacard.security.DHPublicKey;
import javacard.security.CryptoException;

public class PTesterFull extends Applet implements ExtendedLength {
	public static final byte CLA_APPLET = (byte)0xb0;
	public static final byte INS_PREPARE = (byte)0x5a;
	public static final byte INS_TEST_PRIME = (byte)0x5b;
	public static final byte INS_CLEAR = (byte)0x5c;

	public static final byte KEYPAIR_DSA = (byte)0x01;
	public static final byte KEYPAIR_DH = (byte)0x02;

	public static final byte METHOD_SET_DSA_PQG = (byte)0x01;
	public static final byte METHOD_SET_DH_PQG = (byte)0x02;
	public static final byte METHOD_ALL =
		METHOD_SET_DSA_PQG | METHOD_SET_DH_PQG;

	private KeyPair dsaKeyPair = null;
	private DSAPublicKey dsaPublicKey = null;
	private DSAPrivateKey dsaPrivateKey = null;
	private KeyPair dhKeyPair = null;
	private DHPublicKey dhPublicKey = null;
	private DHPrivateKey dhPrivateKey = null;

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

	public static final short SW_Key_not_initialized = (short)0x0ee1;
	public static final short SW_Unknown_method = (short)0x0ee2;
	public static final short SW_Key_not_available = (short)0x0ee3;

	protected PTesterFull(byte[] buffer, short offset, byte length) {
		register();
	}

	public void process(APDU apdu) throws ISOException {
		byte[] apduBuffer = apdu.getBuffer();
		byte cla = apduBuffer[ISO7816.OFFSET_CLA];
		byte ins = apduBuffer[ISO7816.OFFSET_INS];

		if (selectingApplet()) {
			return;
		}

		if (cla == CLA_APPLET) {
			short len = 0;
			try {
				if (ins == INS_PREPARE) {
					len = prepare(apdu);
				} else if (ins == INS_TEST_PRIME) {
					len = testPrime(apdu);
				} else if (ins == INS_CLEAR) {
					len = clear(apdu);
				} else {
					ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
				}
				apdu.setOutgoingAndSend((short)0, len);
			} catch (ArrayIndexOutOfBoundsException e) {
				ISOException.throwIt(SW_ArrayIndexOutOfBoundsException);
			} catch (ArithmeticException e) {
				ISOException.throwIt(SW_ArithmeticException);
			} catch (ArrayStoreException e) {
				ISOException.throwIt(SW_ArrayStoreException);
			} catch (NullPointerException e) {
				ISOException.throwIt(SW_NullPointerException);
			} catch (NegativeArraySizeException e) {
				ISOException.throwIt(SW_NegativeArraySizeException);
			} catch (CryptoException e) {
				ISOException.throwIt(
					(short)(SW_CryptoException_prefix | e.getReason()));
			} catch (SystemException e) {
				ISOException.throwIt(
					(short)(SW_SystemException_prefix | e.getReason()));
			} catch (PINException e) {
				ISOException.throwIt(
					(short)(SW_PINException_prefix | e.getReason()));
			} catch (TransactionException e) {
				ISOException.throwIt(
					(short)(SW_TransactionException_prefix | e.getReason()));
			} catch (CardRuntimeException e) {
				ISOException.throwIt(
					(short)(SW_CardRuntimeException_prefix | e.getReason()));
			} catch (Exception e) {
				ISOException.throwIt(SW_Exception);
			}
		} else {
			ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
		}
	}

	/**
	 * Format:
	 *   P1 = which keypair to alloc
	 *   P2 = ...
	 *   DATA = (short) lengths to alloc
	 * Output:
	 *  SWs of alloc
	 */
	private short prepare(APDU apdu) {
		byte[] apdubuf = apdu.getBuffer();
		short length = apdu.setIncomingAndReceive();
		byte keypairType = apdubuf[ISO7816.OFFSET_P1];
		short sw = ISO7816.SW_NO_ERROR;
		short offset = 0;
		if ((keypairType & KEYPAIR_DSA) != 0) {
			try {
				dsaKeyPair =
					new KeyPair(KeyPair.ALG_DSA,
								Util.getShort(apdubuf, ISO7816.OFFSET_CDATA));
				if (dsaKeyPair.getPrivate() == null || dsaKeyPair.getPublic() == null) {
					try {
						dsaKeyPair.genKeyPair();
					} catch (Exception ignored) {
					}					
				}
				dsaPrivateKey = (DSAPrivateKey)dsaKeyPair.getPrivate();
				dsaPublicKey = (DSAPublicKey)dsaKeyPair.getPublic();
			} catch (CryptoException ce) {
				sw = ce.getReason();
			}
			Util.setShort(apdubuf, offset, sw);
			offset += 2;
		}
		sw = ISO7816.SW_NO_ERROR;
		if ((keypairType & KEYPAIR_DH) != 0) {
			try {
				dhKeyPair = new KeyPair(
					KeyPair.ALG_DH,
					Util.getShort(apdubuf, (short)(ISO7816.OFFSET_CDATA + 2)));
				if (dhKeyPair.getPrivate() == null || dhKeyPair.getPublic() == null) {
					try {
						dhKeyPair.genKeyPair();
					} catch (Exception ignored) {
					}					
				}
				dhPrivateKey = (DHPrivateKey)dhKeyPair.getPrivate();
				dhPublicKey = (DHPublicKey)dhKeyPair.getPublic();
			} catch (CryptoException ce) {
				sw = ce.getReason();
			}
			Util.setShort(apdubuf, offset, sw);
			offset += 2;
		}
		return offset;
	}

	/**
	 * Format:
	 *   P1 = METHOD_*.
	 *   P2 =
	 *   DATA = value to be tested.
	 * Output:
	 *  ...
	 */
	private short testPrime(APDU apdu) {
		byte[] apdubuf = apdu.getBuffer();
		short length = apdu.setIncomingAndReceive();
		byte method = apdubuf[ISO7816.OFFSET_P1];

		short i = 0;
		byte methodMask = METHOD_SET_DSA_PQG;
		while (methodMask < METHOD_ALL) {
			byte masked = (byte)(methodMask & method);
			if (masked != 0) {
				short sw = ISO7816.SW_NO_ERROR;
				short offset = apdu.getOffsetCdata();
				if (masked == METHOD_SET_DSA_PQG) {
					if (dsaPrivateKey == null || dsaPublicKey == null) {
						Util.setShort(apdubuf, (short)(2 * (i++)),
									  SW_Key_not_available);
						Util.setShort(apdubuf, (short)(2 * (i++)),
									  SW_Key_not_available);
						Util.setShort(apdubuf, (short)(2 * (i++)),
									  SW_Key_not_available);
						Util.setShort(apdubuf, (short)(2 * (i++)),
									  SW_Key_not_available);
						methodMask = (byte)(methodMask << 1);
						continue;
					}
					short pLen = Util.getShort(apdubuf, offset);
					offset += 2;
					try {
						dsaPrivateKey.setP(apdubuf, offset, pLen);
						dsaPublicKey.setP(apdubuf, offset, pLen);
						Util.setShort(apdubuf, (short)(2 * (i++)),
										  ISO7816.SW_NO_ERROR);
					} catch (CardRuntimeException ce) {
						Util.setShort(apdubuf, (short)(2 * (i++)),
									  ce.getReason());
					}
					offset += pLen;
					short qLen = Util.getShort(apdubuf, offset);
					offset += 2;
					try {
						dsaPrivateKey.setQ(apdubuf, offset, qLen);
						dsaPublicKey.setQ(apdubuf, offset, qLen);
						Util.setShort(apdubuf, (short)(2 * (i++)),
										  ISO7816.SW_NO_ERROR);
					} catch (CardRuntimeException ce) {
						Util.setShort(apdubuf, (short)(2 * (i++)),
									  ce.getReason());
					}
					offset += qLen;
					short gLen = Util.getShort(apdubuf, offset);
					offset += 2;
					try {
						dsaPrivateKey.setG(apdubuf, offset, gLen);
						dsaPublicKey.setG(apdubuf, offset, gLen);
						Util.setShort(apdubuf, (short)(2 * (i++)),
										  ISO7816.SW_NO_ERROR);
					} catch (CardRuntimeException ce) {
						Util.setShort(apdubuf, (short)(2 * (i++)),
									  ce.getReason());
					}
					try {
						dsaKeyPair.genKeyPair();
						if (!dsaPrivateKey.isInitialized() ||
							!dsaPublicKey.isInitialized()) {
							Util.setShort(apdubuf, (short)(2 * (i++)),
										  SW_Key_not_initialized);
						} else {
							Util.setShort(apdubuf, (short)(2 * (i++)),
										  ISO7816.SW_NO_ERROR);
						}
					} catch (CardRuntimeException ce) {
						Util.setShort(apdubuf, (short)(2 * (i++)),
									  ce.getReason());
					}
				} else if (masked == METHOD_SET_DH_PQG) {
					if (dhPrivateKey == null || dhPublicKey == null) {
						Util.setShort(apdubuf, (short)(2 * (i++)),
									  SW_Key_not_available);
						Util.setShort(apdubuf, (short)(2 * (i++)),
									  SW_Key_not_available);
						Util.setShort(apdubuf, (short)(2 * (i++)),
									  SW_Key_not_available);
						methodMask = (byte)(methodMask << 1);
						continue;
					}
					// Q is optional in DH keys.
					short pLen = Util.getShort(apdubuf, offset);
					offset += 2;
					try {
						dhPrivateKey.setP(apdubuf, offset, pLen);
						dhPublicKey.setP(apdubuf, offset, pLen);
						Util.setShort(apdubuf, (short)(2 * (i++)),
										  ISO7816.SW_NO_ERROR);
					} catch (CardRuntimeException ce) {
						Util.setShort(apdubuf, (short)(2 * (i++)),
									  ce.getReason());
					}
					offset += pLen;
					short gLen = Util.getShort(apdubuf, offset);
					offset += 2;
					try {
						dhPrivateKey.setG(apdubuf, offset, gLen);
						dhPublicKey.setG(apdubuf, offset, gLen);
						Util.setShort(apdubuf, (short)(2 * (i++)),
										  ISO7816.SW_NO_ERROR);
					} catch (CardRuntimeException ce) {
						Util.setShort(apdubuf, (short)(2 * (i++)),
									  ce.getReason());
					}
					try {
						dhKeyPair.genKeyPair();
						if (!dhPrivateKey.isInitialized() ||
							!dhPublicKey.isInitialized()) {
							Util.setShort(apdubuf, (short)(2 * (i++)),
										  SW_Key_not_initialized);
						} else {
							Util.setShort(apdubuf, (short)(2 * (i++)),
										  ISO7816.SW_NO_ERROR);
						}
					} catch (CardRuntimeException ce) {
						Util.setShort(apdubuf, (short)(2 * (i++)),
									  ce.getReason());
					}
				} else {
					Util.setShort(apdubuf, (short)(2 * (i++)),
								  SW_Unknown_method);
				}
			}
			methodMask = (byte)(methodMask << 1);
		}
		return (short)(2 * i);
	}

	/**
	 * Format:
	 *  ...
	 * Output:
	 *  ...
	 */
	private short clear(APDU apdu) {
		byte[] apdubuf = apdu.getBuffer();
		short length = apdu.setIncomingAndReceive();
		if (dsaPrivateKey != null) {
			dsaPrivateKey.clearKey();
		}
		if (dsaPublicKey != null) {
			dsaPublicKey.clearKey();
		}
		if (dhPrivateKey != null) {
			dhPrivateKey.clearKey();
		}
		if (dhPublicKey != null) {
			dhPublicKey.clearKey();
		}
		if (JCSystem.isObjectDeletionSupported()) {
			JCSystem.requestObjectDeletion();
		}
		return 0;
	}

	public static void install(byte[] bArray, short bOffset, byte bLength)
		throws ISOException {
		new PTesterFull(bArray, bOffset, bLength);
	}
}
